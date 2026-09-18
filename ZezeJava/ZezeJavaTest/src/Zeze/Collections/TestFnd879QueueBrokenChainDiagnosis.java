package Zeze.Collections;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Builtin.Collections.Queue.BQueueNodeKey;
import Zeze.Config;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import harness.Fast;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-79回归：Queue根行声明链非空但节点行缺失（外部删除/半清库等数据损坏，正常事务
 * 不会产生）时，poll/pollNode/peek/peekNode曾静默返回null——size仍>0却永远取不出，
 * 消费者空转、积压封存且无迹可寻（walk与LinkedMap同族早已fail-loud，唯读路径漏网）。
 * add对尾断曾无差别另立新尾，活链存在时新增数据彻底不可达。修复：四处读路径记error
 * 断链诊断（不抛ISE——乐观并发重试交错下的瞬时行缺失是良性的）；add仅活链存在
 * （head!=0）且尾键非0的真断链才告警（排空残尾是poll故意不清TailNodeKey的设计常态）。
 */
@Fast
public class TestFnd879QueueBrokenChainDiagnosis {

	// a6专属serverId段。
	private static final AtomicInteger NextServerId = new AtomicInteger(16221);

	private static final class CapturingAppender extends AbstractAppender {
		final List<String> messages = new CopyOnWriteArrayList<>();

		CapturingAppender(String name) {
			super(name, null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(@NotNull LogEvent event) {
			if (event.getLevel() == Level.ERROR)
				messages.add(event.getMessage().getFormattedMessage());
		}
	}

	private static Application newApp(String name) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("a6_fnd879_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application(name, conf);
	}

	private static void callInTxn(Application zeze, Zeze.Util.FuncLong action) {
		var rc = zeze.newProcedure(action, "a6.fnd879").call();
		Assertions.assertEquals(Procedure.Success, rc);
	}

	/** 头节点行被删（模拟损坏）：poll/peek/pollNode/peekNode返null且记error，size保持矛盾可诊断。 */
	@Test
	public void testBrokenHeadDiagnosedOnAllReadPaths() throws Exception {
		Task.tryInitThreadPool();
		var zeze = newApp("TestFnd879Head");
		zeze.start();
		var appender = new CapturingAppender("a6_fnd879_head");
		var log = (Logger)LogManager.getLogger(Queue.class);
		log.addAppender(appender);
		try {
			var module = zeze.getQueueModule();
			var queue = module.open("a6.fnd879.q1", EmptyBean.class, 30);
			callInTxn(zeze, () -> {
				queue.add(new EmptyBean());
				queue.add(new EmptyBean());
				return 0L;
			});
			// 模拟数据损坏：直接删除头节点行（正常事务路径不可能产生该状态）。
			var headKeyHolder = new BQueueNodeKey[1];
			callInTxn(zeze, () -> {
				headKeyHolder[0] = module._tQueues.get("a6.fnd879.q1").getHeadNodeKey();
				module._tQueueNodes.remove(headKeyHolder[0]);
				return 0L;
			});

			callInTxn(zeze, () -> {
				Assertions.assertNull(queue.poll(), "断链下poll仍按空返回（仅加诊断，不改行为）");
				Assertions.assertNull(queue.peek(), "断链下peek仍按空返回");
				Assertions.assertNull(queue.pollNode(), "断链下pollNode仍按空返回");
				Assertions.assertNull(queue.peekNode(), "断链下peekNode仍按空返回");
				Assertions.assertEquals(2, queue.size(), "size仍计数（矛盾状态的诊断线索）");
				return 0L;
			});

			var msgs = appender.messages;
			Assertions.assertTrue(msgs.stream().anyMatch(m -> m.contains("queue poll: broken chain")
					&& m.contains("a6.fnd879.q1")), "poll必须记断链error");
			Assertions.assertTrue(msgs.stream().anyMatch(m -> m.contains("queue peek: broken chain")), "peek必须记断链error");
			Assertions.assertTrue(msgs.stream().anyMatch(m -> m.contains("queue pollNode: broken chain")
					&& m.contains("count=2")), "pollNode日志必须带count");
			Assertions.assertTrue(msgs.stream().anyMatch(m -> m.contains("queue peekNode: broken chain")), "peekNode必须记断链error");
		} finally {
			log.removeAppender(appender);
			zeze.stop();
		}
	}

	/** 尾节点行被删且活链仍在：add另立新尾前记error告警（新增数据不可达）。 */
	@Test
	public void testBrokenTailDiagnosedOnAdd() throws Exception {
		Task.tryInitThreadPool();
		var zeze = newApp("TestFnd879Tail");
		zeze.start();
		var appender = new CapturingAppender("a6_fnd879_tail");
		var log = (Logger)LogManager.getLogger(Queue.class);
		log.addAppender(appender);
		try {
			var module = zeze.getQueueModule();
			var queue = module.open("a6.fnd879.q2", EmptyBean.class, 1); // nodeSize=1：每个值一个节点
			callInTxn(zeze, () -> {
				queue.add(new EmptyBean()); // node1 = head = tail
				queue.add(new EmptyBean()); // node2 = tail（node1.next=node2）
				return 0L;
			});
			// 模拟尾断：删除尾节点行，head（node1）活链仍在。
			callInTxn(zeze, () -> {
				var root = module._tQueues.get("a6.fnd879.q2");
				Assertions.assertNotEquals(root.getHeadNodeKey(), root.getTailNodeKey(), "前置：head与tail分属两节点");
				module._tQueueNodes.remove(root.getTailNodeKey());
				return 0L;
			});
			callInTxn(zeze, () -> {
				queue.add(new EmptyBean()); // 活链存在+尾断：另立新尾前必须告警
				return 0L;
			});
			Assertions.assertTrue(appender.messages.stream().anyMatch(m -> m.contains("queue add: broken tail")
					&& m.contains("a6.fnd879.q2")), "真断尾的add必须记error告警");
		} finally {
			log.removeAppender(appender);
			zeze.stop();
		}
	}

	/** 护栏：排空残尾（poll故意不清TailNodeKey）后首add是设计常态，不得误报。 */
	@Test
	public void testDrainedResidualTailNoFalseAlarm() throws Exception {
		Task.tryInitThreadPool();
		var zeze = newApp("TestFnd879Drained");
		zeze.start();
		var appender = new CapturingAppender("a6_fnd879_drained");
		var log = (Logger)LogManager.getLogger(Queue.class);
		log.addAppender(appender);
		try {
			var module = zeze.getQueueModule();
			var queue = module.open("a6.fnd879.q3", EmptyBean.class, 1);
			callInTxn(zeze, () -> {
				queue.add(new EmptyBean());
				Assertions.assertNotNull(queue.poll(), "排空前必须能取出"); // head推进为0，tail残留指向已删行
				queue.add(new EmptyBean()); // 排空残尾下重建：head==0，不得告警
				Assertions.assertNotNull(queue.peek(), "重建后必须可读");
				return 0L;
			});
			Assertions.assertTrue(appender.messages.stream().noneMatch(m -> m.contains("broken tail")),
					"排空残尾（head==0）不得误报断尾");
			Assertions.assertTrue(appender.messages.stream().noneMatch(m -> m.contains("broken chain")),
					"正常路径不得误报断链");
		} finally {
			log.removeAppender(appender);
			zeze.stop();
		}
	}
}
