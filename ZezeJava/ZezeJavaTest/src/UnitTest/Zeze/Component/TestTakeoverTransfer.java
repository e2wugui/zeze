package UnitTest.Zeze.Component;

import UnitTest.Zeze.BMyBean;
import Zeze.Application;
import Zeze.Builtin.Collections.Queue.BQueue;
import Zeze.Builtin.Timer.BIndex;
import Zeze.Builtin.Timer.BNode;
import Zeze.Builtin.Timer.BNodeRoot;
import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Builtin.Timer.BTimer;
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
 * 步骤②全量接管回归：伪造死者（租约+数据行），tryTransfer单事务内【搬运+立墓碑】。
 * 场景：CsQueue链搬运/幂等/serial守卫/双命名队列、Timer链搬运+版本veto不立碑、
 * 未过期租约放弃接管并到点精确重试。死者serverId用777/778/779/888/889避开真实id。
 */
@Fast
public class TestTakeoverTransfer {

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
	private static TableX<Integer, BNodeRoot> tNodeRoot(Application app) {
		var t = app.getTable("Zeze_Builtin_Timer_tNodeRoot");
		Assertions.assertNotNull(t);
		return (TableX<Integer, BNodeRoot>)t;
	}

	@SuppressWarnings("unchecked")
	private static TableX<Long, BNode> tNodes(Application app) {
		var t = app.getTable("Zeze_Builtin_Timer_tNodes");
		Assertions.assertNotNull(t);
		return (TableX<Long, BNode>)t;
	}

	@SuppressWarnings("unchecked")
	private static TableX<String, BIndex> tIndexs(Application app) {
		var t = app.getTable("Zeze_Builtin_Timer_tIndexs");
		Assertions.assertNotNull(t);
		return (TableX<String, BIndex>)t;
	}

	/**
	 * 伪造死者的队列：放入values并把root stamp成deadEpoch（死者生前claim的代际）。
	 * 用死者serverId直接构造CsQueue（ctor走_open，无'@'限制；module.open公开接口拒绝'@'名字）。
	 * 其注册的scope无害：本测试的live实例先构造（scope按插入序先执行，完成搬运+立墓碑stamp=0），
	 * 轮到死者实例的scope时src已清空，直接返回0；ctor晚注册stamp的myEpoch随后被deadEpoch覆盖。
	 */
	private static void forgeDeadQueue(Application app, String queueName, int deadServerId, long deadEpoch, int... values) {
		var deadName = queueName + "@" + deadServerId;
		var deadCsq = new CsQueue<>(app.getQueueModule(), queueName, deadServerId, BMyBean.class, 10);
		var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
			for (var i : values) {
				var v = new BMyBean();
				v.setI(i);
				deadCsq.add(v); // 此刻root.serial==myEpoch（ctor晚注册stamp），fence通过；下面再覆盖成deadEpoch
			}
			tQueues(app).getOrAdd(deadName).setLoadSerialNo(deadEpoch);
			return 0L;
		}, "TestTakeoverTransfer.forgeDeadQueue@" + deadServerId)).call();
		Assertions.assertEquals(0L, rc);
	}

	private static int[] pollTwo(Application app, CsQueue<BMyBean> csq) {
		var got = new int[] {-1, -1};
		var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
			var v1 = csq.poll();
			var v2 = csq.poll();
			got[0] = v1 != null ? v1.getI() : -1;
			got[1] = v2 != null ? v2.getI() : -1;
			return 0L;
		}, "TestTakeoverTransfer.pollTwo")).call();
		Assertions.assertEquals(0L, rc);
		return got;
	}

	@Test
	public void test1_CsQueueTransfer() throws Exception {
		var conf = TakeoverTestEnv.newConf("on", 600_000, 600_000); // 不依赖扫描，手动tryTransfer
		var app = new Application("TestTakeoverTransferCsQ", conf);
		try {
			app.start();
			var myId = conf.getServerId();
			var module = app.getQueueModule();
			var csq = new CsQueue<>(module, "TestTakeoverTransferQ", myId, BMyBean.class, 10);
			var csq2 = new CsQueue<>(module, "TestTakeoverTransferQ2", myId, BMyBean.class, 10);
			var csq3 = new CsQueue<>(module, "TestTakeoverTransferQ3", myId, BMyBean.class, 10);

			// 死者777：过期租约(epoch=5, root stamp=5) + 队列数据[1,2]。
			forgeDeadQueue(app, "TestTakeoverTransferQ", 777, 5, 1, 2);
			TakeoverTestEnv.forgeLease(app, 777, 5, System.currentTimeMillis() - 1_000);
			app.getTakeover().tryTransfer(777);
			TakeoverTestEnv.waitTryTransferQueue();

			// 搬运到我的队列：接管数据拼到开头，poll按原顺序取回。
			Assertions.assertArrayEquals(new int[] {1, 2}, pollTwo(app, csq), "接管数据应按原顺序可poll");
			// 死者root已清空+墓碑stamp。
			var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var dead = tQueues(app).get("TestTakeoverTransferQ@777");
				Assertions.assertNotNull(dead);
				Assertions.assertEquals(0L, dead.getHeadNodeKey().getNodeId(), "死者head应清零");
				Assertions.assertEquals(0L, dead.getCount(), "死者count应清零");
				Assertions.assertEquals(0L, dead.getLoadSerialNo(), "死者root应立墓碑stamp=0");
				return 0L;
			}, "TestTakeoverTransfer.assertDeadCleared")).call();
			Assertions.assertEquals(0L, rc);
			// 租约立墓碑。
			var lease = TakeoverTestEnv.readLease(app, 777);
			Assertions.assertEquals(5L, lease[0]);
			Assertions.assertEquals(0L, lease[1], "接管成功后租约应立墓碑");

			// 幂等：重复tryTransfer看到墓碑直接退出，不再动数据。
			app.getTakeover().tryTransfer(777);
			TakeoverTestEnv.waitTryTransferQueue();
			Assertions.assertArrayEquals(new int[] {-1, -1}, pollTwo(app, csq), "重复接管不得产生重复数据");

			// 双命名队列：另一个名字的队列各自独立接管（notifyId错位回归的替身）。
			forgeDeadQueue(app, "TestTakeoverTransferQ2", 778, 6, 42);
			TakeoverTestEnv.forgeLease(app, 778, 6, System.currentTimeMillis() - 1_000);
			app.getTakeover().tryTransfer(778);
			TakeoverTestEnv.waitTryTransferQueue();
			Assertions.assertArrayEquals(new int[] {42, -1}, pollTwo(app, csq2));
			Assertions.assertArrayEquals(new int[] {-1, -1}, pollTwo(app, csq), "别的队列的接管数据不得串到本队列");

			// serial守卫：死者root stamp(5) != 租约epoch(9)（已被搬走/活过来了）→不搬。
			forgeDeadQueue(app, "TestTakeoverTransferQ3", 779, 5, 99);
			TakeoverTestEnv.forgeLease(app, 779, 9, System.currentTimeMillis() - 1_000);
			app.getTakeover().tryTransfer(779);
			TakeoverTestEnv.waitTryTransferQueue();
			Assertions.assertArrayEquals(new int[] {-1, -1}, pollTwo(app, csq3), "epoch不匹配不得搬运");
		} finally {
			app.stop();
		}
	}

	@Test
	public void test2_TimerTransferAndVeto() throws Exception {
		var conf = TakeoverTestEnv.newConf("on", 600_000, 600_000);
		var app = new Application("TestTakeoverTransferTimer", conf);
		// Timer必须先于app.start()构造（表注册要在数据库打开前完成），start后再手动启动
		// （生产由ProviderApp.startLast负责，这里没有ProviderApp）。
		var timer = new TakeoverTestEnv.AccessibleTimer(new TakeoverTestEnv.TestAppBase(app));
		try {
			app.start();
			var myId = conf.getServerId();
			var appVer = app.getConfig().getAppVersion();
			timer.loadCustomClassAnd();
			timer.start(); // 加载本地(空)链（接管作用域已在ctor注册、stamp由Takeover.start完成）

			// 死者777：单节点循环链，1个1小时后才触发的simple timer（接管后afterTransfer重调度不会触发）。
			var nodeId = 777_001L;
			var timerId = "@TestTakeoverTransfer.deadTimer1";
			var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var root = tNodeRoot(app).getOrAdd(777);
				root.setHeadNodeId(nodeId);
				root.setTailNodeId(nodeId);
				root.setVersion(0);
				root.setLoadSerialNo(5);
				var node = tNodes(app).getOrAdd(nodeId);
				node.setPrevNodeId(nodeId); // 循环链
				node.setNextNodeId(nodeId);
				var simple = new BSimpleTimer();
				simple.setNextExpectedTime(System.currentTimeMillis() + 3_600_000);
				var bTimer = new BTimer(timerId, NoopHandle.class.getName(), 0);
				bTimer.setTimerObj(simple);
				node.getTimers().put(timerId, bTimer);
				tIndexs(app).insert(timerId, new BIndex(777, nodeId, 1, 0));
				return 0L;
			}, "TestTakeoverTransfer.forgeDeadTimer")).call();
			Assertions.assertEquals(0L, rc);
			TakeoverTestEnv.forgeLease(app, 777, 5, System.currentTimeMillis() - 1_000);

			app.getTakeover().tryTransfer(777);
			TakeoverTestEnv.waitTryTransferQueue();

			var assertRc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				try {
					assertTimerMovedBody(app, myId, nodeId, timerId);
				} catch (Throwable e) { // procedure把非assert异常吞成rc=-1（AssertionError会重抛），打印栈定位偶发失败
					e.printStackTrace();
					throw e;
				}
				return 0L;
			}, "TestTakeoverTransfer.assertTimerMoved")).call();
			Assertions.assertEquals(0L, assertRc);
			var lease = TakeoverTestEnv.readLease(app, 777);
			Assertions.assertEquals(0L, lease[1], "timer接管成功后租约应立墓碑");

			// 版本veto：死者888的root版本高于本进程appVer→不搬且不立碑（留给高版本）。
			var rcVeto = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var root = tNodeRoot(app).getOrAdd(888);
				root.setHeadNodeId(888_001L);
				root.setTailNodeId(888_001L);
				root.setVersion(appVer + 1);
				root.setLoadSerialNo(5);
				return 0L;
			}, "TestTakeoverTransfer.forgeHighVersion")).call();
			Assertions.assertEquals(0L, rcVeto);
			TakeoverTestEnv.forgeLease(app, 888, 5, System.currentTimeMillis() - 1_000);
			app.getTakeover().tryTransfer(888);
			TakeoverTestEnv.waitTryTransferQueue();

			var lease888 = TakeoverTestEnv.readLease(app, 888);
			Assertions.assertNotEquals(0L, lease888[1], "veto时不得立墓碑，租约保持过期态留给高版本");
			var rcStay = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var dead = tNodeRoot(app).get(888);
				Assertions.assertNotNull(dead);
				Assertions.assertEquals(888_001L, dead.getHeadNodeId(), "veto时不得搬运");
				return 0L;
			}, "TestTakeoverTransfer.assertVetoStay")).call();
			Assertions.assertEquals(0L, rcStay);
		} finally {
			timer.stop();
			app.stop();
		}
	}

	// 独立方法仅为给assertRc的procedure一个可catch的可读载荷（见上：打印被吞的异常栈）。
	private static void assertTimerMovedBody(Application app, int myId, long nodeId, String timerId) {
		var dead = tNodeRoot(app).get(777);
		Assertions.assertNotNull(dead);
		Assertions.assertEquals(0L, dead.getHeadNodeId(), "死者timer链头应清零");
		Assertions.assertEquals(0L, dead.getTailNodeId());
		Assertions.assertEquals(0L, dead.getLoadSerialNo(), "死者root应立墓碑stamp=0");
		var mine = tNodeRoot(app).getOrAdd(myId);
		Assertions.assertEquals(nodeId, mine.getHeadNodeId(), "接管链应拼到我的root");
		Assertions.assertEquals(nodeId, mine.getTailNodeId());
		var index = tIndexs(app).get(timerId);
		Assertions.assertNotNull(index);
		Assertions.assertEquals(myId, index.getServerId(), "afterTransfer重载应把index重指向接管者");
	}

	@Test
	public void test3_UnexpiredPreciseRetry() throws Exception {
		var conf = TakeoverTestEnv.newConf("on", 600_000, 600_000);
		var app = new Application("TestTakeoverTransferRetry", conf);
		try {
			app.start();
			var csq = new CsQueue<>(app.getQueueModule(), "TestTakeoverTransferQ4", conf.getServerId(), BMyBean.class, 10);

			// 889还活着（400ms后过期）：tryTransfer立即放弃（flap场景），不搬不立碑。
			forgeDeadQueue(app, "TestTakeoverTransferQ4", 889, 8, 7);
			var expireAt = System.currentTimeMillis() + 400;
			TakeoverTestEnv.forgeLease(app, 889, 8, expireAt);
			app.getTakeover().tryTransfer(889);
			TakeoverTestEnv.waitTryTransferQueue();

			var notYet = TakeoverTestEnv.readLease(app, 889);
			Assertions.assertEquals(expireAt, notYet[1], "未过期租约不得立碑");
			Assertions.assertArrayEquals(new int[] {-1, -1}, pollTwo(app, csq), "未过期不得搬运");

			// 精确重试：接管被安排到过期时刻，之后自动完成搬运+立碑。
			var deadline = System.currentTimeMillis() + 5_000;
			while (TakeoverTestEnv.readLease(app, 889)[1] != 0 && System.currentTimeMillis() < deadline)
				Thread.sleep(50);
			Assertions.assertEquals(0L, TakeoverTestEnv.readLease(app, 889)[1], "到点精确重试应完成接管并立碑");
			Assertions.assertArrayEquals(new int[] {7, -1}, pollTwo(app, csq), "重试后数据应可取回");
		} finally {
			app.stop();
		}
	}
}
