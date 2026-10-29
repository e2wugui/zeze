package Zeze.Component;

import java.nio.file.Path;
import Zeze.BMyBean;
import Zeze.Application;
import Zeze.Builtin.Collections.Queue.BQueue;
import Zeze.Collections.CsQueue;
import Zeze.Transaction.TableX;
import Zeze.Util.ShutdownHook;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/**
 * 真实子JVM验证fencing固定致命退出，以及shutdown时release不动租约。
 * Queue、Timer、scope登记和续租各自退出，不能在第一次致命退出后继续同进程测试。
 * 墓碑复活场景应正常完成，也隔离到子JVM，防止误判fence时杀掉JUnit worker。
 */
@Fast
@Timeout(45)
public class TestTakeoverFence {

	public static final class NoopHandle implements TimerHandle {
		public NoopHandle() {
		}

		@Override
		public void onTimer(TimerContext context) {
		}
	}

	@SuppressWarnings("unchecked")
	private static TableX<String, BQueue> tQueues(Application app) {
		var t = app.getTable("Zeze_Builtin_Collections_Queue_tQueues");
		Assertions.assertNotNull(t);
		return (TableX<String, BQueue>)t;
	}

	@SuppressWarnings("unchecked")
	private static TableX<Integer, Zeze.Builtin.Timer.BNodeRoot> tNodeRoot(Application app) {
		var t = app.getTable("Zeze_Builtin_Timer_tNodeRoot");
		Assertions.assertNotNull(t);
		return (TableX<Integer, Zeze.Builtin.Timer.BNodeRoot>)t;
	}

	@Test
	public void testQueueFenceExitsAndPreservesLease(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path dir) throws Exception {
		TakeoverTestProcess.run(dir, TestTakeoverFence.class, "queue-fence", "checkFence: fenced!");
	}

	@Test
	public void testTimerFenceExitsAndPreservesLease(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path dir) throws Exception {
		TakeoverTestProcess.run(dir, TestTakeoverFence.class, "timer-fence", "checkFence: fenced!");
	}

	@Test
	public void testStampLeaseLossExitsAndPreservesLease(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path dir) throws Exception {
		TakeoverTestProcess.run(dir, TestTakeoverFence.class, "stamp-lease-loss", "stampScope: lease lost");
	}

	@Test
	public void testRenewLeaseLossExitsAndPreservesLease(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path dir) throws Exception {
		TakeoverTestProcess.run(dir, TestTakeoverFence.class, "renew-lease-loss", "renew: lease lost");
	}

	@Test
	public void testTombstoneRevivalNotFenced(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path dir) throws Exception {
		TakeoverTestProcess.run(dir, TestTakeoverFence.class, "tombstone-revival", null);
	}

	public static void main(String[] args) {
		var scenario = args[0];
		try {
			if (scenario.equals("tombstone-revival")) {
				tombstoneRevival();
				TakeoverTestProcess.marker("PASS", scenario);
				System.exit(0);
			} else
				fenceAndExit(scenario);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(2); // 与生产fence的-1区分，断言失败或启动失败不能算通过。
		}
	}

	private static void fenceAndExit(String scenario) throws Exception {
		var conf = TakeoverTestEnv.newConf("on", 600_000, 600_000);
		var app = new Application("TestTakeoverFence", conf);
		// Timer必须在app.start()前构造以注册表，由测试手动启动与停止。
		var timer = new TakeoverTestEnv.AccessibleTimer(new TakeoverTestEnv.TestAppBase(app));
		var takeover = app.getTakeover();
		try {
			app.start();
			var myId = conf.getServerId();
			var csq = new CsQueue<>(app.getQueueModule(), "TestTakeoverFenceQ", myId, BMyBean.class, 10);
			timer.loadCustomClassAnd();
			timer.start();

			var rc0 = TaskSpec.ofProcedure(app.newProcedure(() -> {
				csq.add(new BMyBean());
				return 0L;
			}, "TestTakeoverFence.normalAdd")).call();
			Assertions.assertEquals(0L, rc0);

			var foreignEpoch = takeover.getMyEpoch() + 7;
			boolean leaseLost = scenario.equals("stamp-lease-loss") || scenario.equals("renew-lease-loss");
			// 写路径场景保留自己的租约epoch，只有root被改成foreign；这样删除fenceFatal保护
			// 会错误刷新到期时间，不能仅靠release的epoch不符检查掩盖回归。
			TakeoverTestEnv.forgeLease(app, myId, leaseLost ? foreignEpoch : takeover.getMyEpoch(),
					System.currentTimeMillis() + 1_234_567);
			if (!leaseLost) {
				Assertions.assertEquals(0L, TaskSpec.ofProcedure(app.newProcedure(() -> {
					if (scenario.equals("queue-fence"))
						tQueues(app).getOrAdd("TestTakeoverFenceQ@" + myId).setLoadSerialNo(foreignEpoch);
					else if (scenario.equals("timer-fence"))
						tNodeRoot(app).getOrAdd(myId).setLoadSerialNo(foreignEpoch);
					else
						throw new IllegalArgumentException(scenario);
					return 0L;
				}, "TestTakeoverFence.forgeForeignStamp")).call());
			}

			var before = TakeoverTestEnv.readLease(app, myId);
			// ShutdownHook为LIFO，此测试观察器在Application自己的停机动作之前运行。
			// 不拦截System.exit：真实退出时检查真实release，再完成应用停机。
			ShutdownHook.add(() -> {
				takeover.release();
				Assertions.assertArrayEquals(before, TakeoverTestEnv.readLease(app, myId),
						"fence后release不得改epoch或expireAt");
				TakeoverTestProcess.marker("LEASE_PRESERVED", scenario);
				timer.stop();
				app.stop();
				TakeoverTestProcess.marker("STOPPED", scenario);
			});
			TakeoverTestProcess.marker("READY", scenario);
			switch (scenario) {
			case "queue-fence", "timer-fence" -> TaskSpec.ofProcedure(app.newProcedure(() -> {
				if (scenario.equals("queue-fence"))
					csq.add(new BMyBean());
				else
					timer.schedule(TimerSpec.ofDelay(60_000), NoopHandle.class, null);
				TakeoverTestProcess.marker("UNEXPECTED_RETURN", scenario);
				return 0L;
			}, "TestTakeoverFence.fencedWrite")).call();
			case "stamp-lease-loss" -> new CsQueue<>(app.getQueueModule(), "TestTakeoverFenceNewQ",
					myId, BMyBean.class, 10);
			case "renew-lease-loss" -> {
				// 只在测试工程直接触发生产续租轮次，不等周期到点，也不替换其实现。
				var renew = Takeover.class.getDeclaredMethod("renewOnce");
				renew.setAccessible(true);
				renew.invoke(takeover);
			}
			default -> throw new IllegalArgumentException(scenario);
			}
			TakeoverTestProcess.marker("UNEXPECTED_RETURN", scenario);
			throw new AssertionError("fence必须终止子JVM");
		} finally {
			timer.stop();
			app.stop();
		}
	}

	/**
	 * 需求语义回归：被接管后醒来必须在空链上复活继续工作，不得fence。
	 * 伪造"被接管完成"后的本地状态：自己的root是墓碑（stamp=0，transferAll留下）、
	 * 租约是墓碑（expireAt=0、epoch保留）——随后CsQueue.add与timer.schedule都必须成功
	 * 并正常退出，root被认领（stamp=myEpoch），新数据可写可取。
	 */
	private static void tombstoneRevival() throws Exception {
		var conf = TakeoverTestEnv.newConf("on", 600_000, 600_000);
		var app = new Application("TestTakeoverFenceRevival", conf);
		// Timer必须先于app.start()构造（表注册要在数据库打开前完成），start后再手动启动。
		var timer = new TakeoverTestEnv.AccessibleTimer(new TakeoverTestEnv.TestAppBase(app));
		var takeover = app.getTakeover();
		try {
			app.start();
			var myId = conf.getServerId();
			var csq = new CsQueue<>(app.getQueueModule(), "TestTakeoverFenceRevivalQ", myId, BMyBean.class, 10);
			timer.loadCustomClassAnd();
			timer.start();
			TakeoverTestProcess.marker("READY", "tombstone-revival");

			// 伪造"被接管完成"后的墓碑状态：root stamp=0（ctor的stamp被覆盖）+ 租约墓碑（epoch保留）。
			var rcForge = TaskSpec.ofProcedure(app.newProcedure(() -> {
				tQueues(app).getOrAdd("TestTakeoverFenceRevivalQ@" + myId).setLoadSerialNo(0);
				var root = tNodeRoot(app).getOrAdd(myId);
				root.setHeadNodeId(0);
				root.setTailNodeId(0);
				root.setVersion(0);
				root.setLoadSerialNo(0);
				return 0L;
			}, "TestTakeoverFence.forgeTombstone")).call();
			Assertions.assertEquals(0L, rcForge);
			TakeoverTestEnv.forgeLease(app, myId, takeover.getMyEpoch(), 0);

			// 复活写：CsQueue.add + timer.schedule 都必须成功。
			var rcWrite = TaskSpec.ofProcedure(app.newProcedure(() -> {
				csq.add(new BMyBean());
				timer.schedule(TimerSpec.ofDelay(60_000), NoopHandle.class, null);
				return 0L;
			}, "TestTakeoverFence.revivalWrite")).call();
			Assertions.assertEquals(0L, rcWrite, "墓碑后复活写必须成功");

			// root被认领（stamp=myEpoch），新数据可取。
			var rcAssert = TaskSpec.ofProcedure(app.newProcedure(() -> {
				Assertions.assertEquals(takeover.getMyEpoch(),
						tQueues(app).getOrAdd("TestTakeoverFenceRevivalQ@" + myId).getLoadSerialNo(),
						"复活写应认领队列root stamp=myEpoch");
				Assertions.assertEquals(takeover.getMyEpoch(), tNodeRoot(app).getOrAdd(myId).getLoadSerialNo(),
						"复活调度应认领Timer root stamp=myEpoch");
				return 0L;
			}, "TestTakeoverFence.assertRevivalClaim")).call();
			Assertions.assertEquals(0L, rcAssert);
			var got = new int[] {-1};
			var rcPoll = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var v = csq.poll();
				got[0] = v != null ? v.getI() : -1;
				return 0L;
			}, "TestTakeoverFence.revivalPoll")).call();
			Assertions.assertEquals(0L, rcPoll);
			Assertions.assertNotEquals(-1, got[0], "复活写的新数据必须可取");
		} finally {
			timer.stop();
			app.stop();
		}
	}
}
