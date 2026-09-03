package UnitTest.Zeze.Collections;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import UnitTest.Zeze.BMyBean;
import Zeze.Application;
import Zeze.Builtin.Collections.Queue.BQueue;
import Zeze.Builtin.Collections.Queue.BQueueNode;
import Zeze.Builtin.Collections.Queue.BQueueNodeKey;
import Zeze.Builtin.Collections.Queue.BQueueNodeValue;
import Zeze.Collections.CsQueue;
import Zeze.Collections.Queue;
import Zeze.Component.Takeover;
import Zeze.Config;
import Zeze.Transaction.TableX;
import Zeze.Util.FuncLong;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND-C0-1回归：compatible机制引入之前的【旧格式根行】（只写废弃long字段
 * HeadNodeId/TailNodeId，不写HeadNodeKey/TailNodeKey）在"迁移→排空/被接管清空→再访问"
 * 序列下不得楔死：poll不得永远null、add不得不可达、walk必须终止；
 * 被接管清空的死者队列重启后头指针不得复活为指向已接管的节点（双服持链、互相偷数据）。
 * 现有TestQueue/TestCsQueue/TestTakeoverTransfer全部用新格式数据，不覆盖本路径。
 */
@Fast
public class TestQueueCompatible {

	// 与TakeoverTestEnv的100+、伪造死者777+错开：@Fast类并行时Application的本地缓存按serverId一份。
	private static final AtomicInteger NextServerId = new AtomicInteger(500);

	private static Config newConf(String takeoverMode) {
		var conf = new Config();
		conf.setServiceManager("disable");
		int serverId = NextServerId.getAndIncrement();
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf());
		// Memory库按url分桶，独占url=独占存储（同TakeoverTestEnv）。
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("queue_compatible_test_" + serverId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		conf.setTakeoverMode(takeoverMode);
		conf.setTakeoverTtl(600_000);
		conf.setTakeoverScanPeriod(600_000);
		return conf;
	}

	@SuppressWarnings("unchecked")
	private static TableX<String, BQueue> tQueues(Application app) {
		var t = app.getTable("Zeze_Builtin_Collections_Queue_tQueues");
		Assertions.assertNotNull(t);
		return (TableX<String, BQueue>)t;
	}

	@SuppressWarnings("unchecked")
	private static TableX<BQueueNodeKey, BQueueNode> tQueueNodes(Application app) {
		var t = app.getTable("Zeze_Builtin_Collections_Queue_tQueueNodes");
		Assertions.assertNotNull(t);
		return (TableX<BQueueNodeKey, BQueueNode>)t;
	}

	private static void run(Application app, String name, FuncLong action) {
		var rc = TaskSpec.ofProcedure(app.newProcedure(action, name)).call();
		Assertions.assertEquals(0L, rc);
	}

	private static List<Integer> walk(Queue<BMyBean> queue) throws Exception {
		var out = new ArrayList<Integer>();
		queue.walk((k, v) -> {
			out.add(v.getI());
			return true;
		});
		return out;
	}

	private static List<Integer> walk(CsQueue<BMyBean> csq) throws Exception {
		var out = new ArrayList<Integer>();
		csq.walk((k, v) -> {
			out.add(v.getI());
			return true;
		});
		return out;
	}

	/**
	 * 伪造旧格式队列（compatible引入前的存量数据）：根行只设废弃long字段，
	 * 不设HeadNodeKey/TailNodeKey（保持构造默认的空名key）；节点行同样只设Values。
	 * 调用前必须已构造对应的Queue/CsQueue（值bean需要先注册）。
	 */
	private static void forgeLegacyQueue(Application app, String queueName, int value, long loadSerialNo) {
		run(app, "TestQueueCompatible.forge@" + queueName, () -> {
			var root = tQueues(app).getOrAdd(queueName);
			root.setHeadNodeId(1);
			root.setTailNodeId(1);
			root.setCount(1);
			root.setLastNodeId(1);
			root.setLoadSerialNo(loadSerialNo);

			var node = tQueueNodes(app).getOrAdd(new BQueueNodeKey(queueName, 1));
			var nodeValue = new BQueueNodeValue();
			nodeValue.setTimestamp(System.currentTimeMillis());
			var bean = new BMyBean();
			bean.setI(value);
			nodeValue.getValue().setBean(bean);
			node.getValues().add(nodeValue);
			return 0L;
		});
	}

	// 与Takeover.tryTransfer同key投递哨兵，等待队列中排在前面的tryTransfer执行完。
	private static void waitTryTransferQueue() throws Exception {
		var done = new TaskCompletionSource<Void>();
		TaskSpec.ofAction(() -> done.setResult(null)).name("TestQueueCompatible.sentinel")
				.executeOneByOne(Takeover.TryTransferOneByOneKey, Task.getOneByOne());
		done.get(5, TimeUnit.SECONDS);
	}

	// 旧格式队列自然排空（poll）后再访问：不楔死；且迁移必须消费（清零）废弃long字段，
	// 使迁移严格一次性——否则后续任何写回空名头指针的路径都会让compatible用陈旧id复活指针。
	@Test
	public void test1_LegacyDrainThenAccess() throws Exception {
		var app = new Application("TestQueueCompatibleDrain", newConf("off"));
		try {
			app.start();
			var module = app.getQueueModule();
			var queue = module.open("TestQueueCompatibleDrain", BMyBean.class);
			forgeLegacyQueue(app, "TestQueueCompatibleDrain", 11, 0);

			run(app, "drain.poll", () -> {
				Assertions.assertFalse(queue.isEmpty(), "旧格式非空队列迁移后应可见");
				var v = queue.poll();
				Assertions.assertNotNull(v);
				Assertions.assertEquals(11, v.getI());
				Assertions.assertTrue(queue.isEmpty());
				return 0L;
			});

			run(app, "drain.assertConsumed", () -> {
				var root = tQueues(app).get("TestQueueCompatibleDrain");
				Assertions.assertNotNull(root);
				Assertions.assertEquals(0L, root.getHeadNodeId(), "迁移应消费（清零）废弃HeadNodeId");
				Assertions.assertEquals(0L, root.getTailNodeId(), "迁移应消费（清零）废弃TailNodeId");
				return 0L;
			});

			run(app, "drain.after", () -> {
				Assertions.assertTrue(queue.isEmpty(), "排空后必须保持空");
				Assertions.assertNull(queue.poll(), "排空后poll必须返回null而非楔死");
				return 0L;
			});
			run(app, "drain.add", () -> {
				var bean = new BMyBean();
				bean.setI(22);
				queue.add(bean);
				return 0L;
			});
			Assertions.assertEquals(List.of(22), walk(queue), "排空后add的数据必须可达");
			run(app, "drain.pollAgain", () -> {
				var v = queue.poll();
				Assertions.assertNotNull(v);
				Assertions.assertEquals(22, v.getI());
				return 0L;
			});
		} finally {
			app.stop();
		}
	}

	// splice清空旧格式死者（nullKey清空路径）：死者root的废弃long字段必须同步清零，
	// 否则死者再访问时compatible用陈旧id把头指针重建为指向已被拼进活者链的节点。
	@Test
	public void test2_SpliceClearLegacyNoRevive() throws Exception {
		var app = new Application("TestQueueCompatibleSplice", newConf("off"));
		try {
			app.start();
			var module = app.getQueueModule();
			var name = "TestQueueCompatibleSplice";
			var myId = app.getConfig().getServerId();
			var live = new CsQueue<>(module, name, myId, BMyBean.class, 10);
			final int deadId = 750;
			forgeLegacyQueue(app, name + "@" + deadId, 33, 0);

			live.splice(deadId, 0); // 死者root LoadSerialNo=0，匹配即接管

			Assertions.assertEquals(List.of(33), walk(live), "接管数据应归活者");
			run(app, "splice.assertCleared", () -> {
				var dead = tQueues(app).get(name + "@" + deadId);
				Assertions.assertNotNull(dead);
				Assertions.assertEquals(0L, dead.getHeadNodeKey().getNodeId(), "死者head应清零");
				Assertions.assertEquals(0L, dead.getTailNodeKey().getNodeId(), "死者tail应清零");
				Assertions.assertEquals(0L, dead.getCount(), "死者count应清零");
				Assertions.assertEquals(0L, dead.getHeadNodeId(), "清空死者必须同步清零废弃HeadNodeId");
				Assertions.assertEquals(0L, dead.getTailNodeId(), "清空死者必须同步清零废弃TailNodeId");
				return 0L;
			});

			// 死者"重启"（构造CsQueue即走getOrAddRoot→compatible）：头指针不得复活。
			var deadCsq = new CsQueue<>(module, name, deadId, BMyBean.class, 10);
			run(app, "splice.deadRevive", () -> {
				Assertions.assertTrue(deadCsq.isEmpty(), "被接管清空的死者队列重启后必须为空");
				Assertions.assertNull(deadCsq.poll(), "死者poll不得偷活者的数据");
				return 0L;
			});
			run(app, "splice.deadAdd", () -> {
				var bean = new BMyBean();
				bean.setI(44);
				deadCsq.add(bean);
				return 0L;
			});
			Assertions.assertEquals(List.of(44), walk(deadCsq), "死者重启后add必须可达");
			Assertions.assertEquals(List.of(33), walk(live), "活者数据不得被动");
		} finally {
			app.stop();
		}
	}

	// transferAll（Takeover租约接管）清空旧格式死者：与splice同一条nullKey清空路径，
	// 走真实tryTransfer流程（claim/stamp/transferAll/墓碑）。
	@Test
	public void test3_TransferAllClearLegacyNoRevive() throws Exception {
		var conf = newConf("on");
		var app = new Application("TestQueueCompatibleTransfer", conf);
		try {
			app.start();
			var module = app.getQueueModule();
			var name = "TestQueueCompatibleTransfer";
			var live = new CsQueue<>(module, name, conf.getServerId(), BMyBean.class, 10);

			final int deadId = 751;
			final long deadEpoch = 5;
			forgeLegacyQueue(app, name + "@" + deadId, 55, deadEpoch); // 死者root stamp=deadEpoch
			run(app, "transfer.forgeLease", () -> {
				var lease = app.getTakeover().getTable().getOrAdd(deadId);
				lease.setEpoch(deadEpoch);
				lease.setExpireAt(System.currentTimeMillis() - 1_000); // 已过期
				return 0L;
			});

			app.getTakeover().tryTransfer(deadId);
			waitTryTransferQueue();

			Assertions.assertEquals(List.of(55), walk(live), "接管数据应归活者");
			run(app, "transfer.assertCleared", () -> {
				var dead = tQueues(app).get(name + "@" + deadId);
				Assertions.assertNotNull(dead);
				Assertions.assertEquals(0L, dead.getHeadNodeKey().getNodeId(), "死者head应清零");
				Assertions.assertEquals(0L, dead.getCount(), "死者count应清零");
				Assertions.assertEquals(0L, dead.getLoadSerialNo(), "死者root应立墓碑stamp=0");
				Assertions.assertEquals(0L, dead.getHeadNodeId(), "清空死者必须同步清零废弃HeadNodeId");
				Assertions.assertEquals(0L, dead.getTailNodeId(), "清空死者必须同步清零废弃TailNodeId");
				return 0L;
			});

			// 死者重启：stamp的getOrAddRoot会走compatible，头指针不得复活为指向已接管节点。
			var deadCsq = new CsQueue<>(module, name, deadId, BMyBean.class, 10);
			run(app, "transfer.deadRevive", () -> {
				Assertions.assertTrue(deadCsq.isEmpty(), "被接管清空的死者队列重启后必须为空");
				Assertions.assertNull(deadCsq.poll(), "死者poll不得偷活者的数据");
				return 0L;
			});
			run(app, "transfer.deadAdd", () -> {
				var bean = new BMyBean();
				bean.setI(66);
				deadCsq.add(bean);
				return 0L;
			});
			Assertions.assertEquals(List.of(66), walk(deadCsq), "死者重启后add必须可达");
			Assertions.assertEquals(List.of(55), walk(live), "活者数据不得被动");
		} finally {
			app.stop();
		}
	}
}
