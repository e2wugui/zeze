package UnitTest.Zeze.Component;

import java.util.concurrent.atomic.AtomicInteger;
import UnitTest.Zeze.BMyBean;
import Zeze.Application;
import Zeze.Builtin.Collections.Queue.BQueue;
import Zeze.Collections.CsQueue;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerSpec;
import Zeze.Transaction.TableX;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 步骤②写路径fence回归：自己的root.epoch被【别人的epoch】覆盖（同serverId双进程：后启动者
 * claim epoch+1并stampScope覆盖前者的root；或外部篡改）时，CsQueue/Timer的写路径必须fence
 * 拒绝（致命动作被注入的计数器替代，finally复原）；fenceFatal后release不得写墓碑（否则会打掉
 * 新owner的租约）。
 * 注意：被接管后醒来【不是】fence场景——transferAll留下的墓碑stamp=0会被写路径认领，
 * 被接管者在空链上复活继续新写（见testTombstoneRevivalNotFenced）。
 */
@Fast
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
	public void testFenceFatalAndReleaseNoTombstone() throws Exception {
		var conf = TakeoverTestEnv.newConf("on", 600_000, 600_000);
		var app = new Application("TestTakeoverFence", conf);
		// Timer必须先于app.start()构造（表注册要在数据库打开前完成），start后再手动启动
		// （生产由ProviderApp.startLast负责，这里没有ProviderApp）。
		var timer = new TakeoverTestEnv.AccessibleTimer(new TakeoverTestEnv.TestAppBase(app));
		var takeover = app.getTakeover();
		var fatalCount = new AtomicInteger();
		takeover.setFatalAction(fatalCount::incrementAndGet); // 替代System.exit
		try {
			app.start();
			var myId = conf.getServerId();
			var csq = new CsQueue<>(app.getQueueModule(), "TestTakeoverFenceQ", myId, BMyBean.class, 10);
			timer.loadCustomClassAnd();
			timer.start(); // 加载本地(空)链（接管作用域已在ctor注册、stamp由Takeover.start完成）

			// 正常写（root.epoch==myEpoch）：不触发。
			var rc0 = TaskSpec.ofProcedure(app.newProcedure(() -> {
				csq.add(new BMyBean());
				return 0L;
			}, "TestTakeoverFence.normalAdd")).call();
			Assertions.assertEquals(0L, rc0);
			Assertions.assertEquals(0, fatalCount.get(), "正常写不得触发fence");

			// 模拟被接管：新owner的stamp覆盖了我的root.epoch。
			var foreignEpoch = takeover.getMyEpoch() + 7;
			var rcStamp = TaskSpec.ofProcedure(app.newProcedure(() -> {
				tQueues(app).getOrAdd("TestTakeoverFenceQ@" + myId).setLoadSerialNo(foreignEpoch);
				return 0L;
			}, "TestTakeoverFence.forgeForeignStamp")).call();
			Assertions.assertEquals(0L, rcStamp);

			// CsQueue写路径fence：醒来首写被拒绝。
			var rcAdd = TaskSpec.ofProcedure(app.newProcedure(() -> {
				csq.add(new BMyBean());
				return 0L;
			}, "TestTakeoverFence.fencedAdd")).call();
			Assertions.assertEquals(0L, rcAdd); // 致命动作被注入替代，事务本身继续
			Assertions.assertEquals(1, fatalCount.get(), "被接管后的CsQueue写必须fence");

			// Timer写路径fence：schedule插链前检查root.epoch。
			var rcTimerStamp = TaskSpec.ofProcedure(app.newProcedure(() -> {
				tNodeRoot(app).getOrAdd(myId).setLoadSerialNo(foreignEpoch);
				return 0L;
			}, "TestTakeoverFence.forgeTimerForeignStamp")).call();
			Assertions.assertEquals(0L, rcTimerStamp);
			var rcSchedule = TaskSpec.ofProcedure(app.newProcedure(() -> {
				timer.schedule(TimerSpec.ofDelay(60_000), NoopHandle.class, null);
				return 0L;
			}, "TestTakeoverFence.fencedSchedule")).call();
			Assertions.assertEquals(0L, rcSchedule);
			Assertions.assertEquals(2, fatalCount.get(), "被接管后的Timer插链必须fence");

			// fenceFatal置位后：release不得写墓碑（本进程租约保持原样，新owner不受影响）。
			var before = TakeoverTestEnv.readLease(app, myId);
			Assertions.assertTrue(before[1] > 0, "本进程租约应在续约中");
			takeover.release();
			var after = TakeoverTestEnv.readLease(app, myId);
			Assertions.assertEquals(before[0], after[0], "fence后release不得动epoch");
			Assertions.assertEquals(before[1], after[1], "fence后release不得写墓碑");
		} finally {
			takeover.setFatalAction(null); // 复原默认致命退出
			timer.stop();
			app.stop();
		}
	}

	/**
	 * 需求语义回归：被接管后醒来必须在空链上复活继续工作，不得fence。
	 * 伪造"被接管完成"后的本地状态：自己的root是墓碑（stamp=0，transferAll留下）、
	 * 租约是墓碑（expireAt=0、epoch保留）——随后CsQueue.add与timer.schedule都必须成功
	 * 且零致命动作，root被认领（stamp=myEpoch），新数据可写可取。
	 */
	@Test
	public void testTombstoneRevivalNotFenced() throws Exception {
		var conf = TakeoverTestEnv.newConf("on", 600_000, 600_000);
		var app = new Application("TestTakeoverFenceRevival", conf);
		// Timer必须先于app.start()构造（表注册要在数据库打开前完成），start后再手动启动。
		var timer = new TakeoverTestEnv.AccessibleTimer(new TakeoverTestEnv.TestAppBase(app));
		var takeover = app.getTakeover();
		var fatalCount = new AtomicInteger();
		takeover.setFatalAction(fatalCount::incrementAndGet);
		try {
			app.start();
			var myId = conf.getServerId();
			var csq = new CsQueue<>(app.getQueueModule(), "TestTakeoverFenceRevivalQ", myId, BMyBean.class, 10);
			timer.loadCustomClassAnd();
			timer.start();

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

			// 复活写：CsQueue.add + timer.schedule 都必须成功、零致命。
			var rcWrite = TaskSpec.ofProcedure(app.newProcedure(() -> {
				csq.add(new BMyBean());
				timer.schedule(TimerSpec.ofDelay(60_000), NoopHandle.class, null);
				return 0L;
			}, "TestTakeoverFence.revivalWrite")).call();
			Assertions.assertEquals(0L, rcWrite, "墓碑后复活写必须成功");
			Assertions.assertEquals(0, fatalCount.get(), "复活不得触发fence");

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
			takeover.setFatalAction(null); // 复原默认致命退出
			timer.stop();
			app.stop();
		}
	}
}
