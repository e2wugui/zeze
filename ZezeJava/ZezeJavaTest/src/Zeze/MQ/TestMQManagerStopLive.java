package Zeze.MQ;

import java.nio.file.Path;
import Zeze.MQ.MQManager;
import Zeze.MQ.MQPartition;
import Zeze.MQ.Master.MasterAgent;
import Zeze.Util.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND19 GB-C02 回归（端到端形态）：繁忙 Manager（盘上积压 + 在飞后台回填）收到 stop() 必须
 * 有界排空数据通路后再关库——不崩、不挂死、关库完成。
 * <p>
 * 旧 stop 顺序在 queue.close() 之前直接 rocksDatabase.close()，且不排空 messageFillFuture
 * （fill 在锁外持索引迭代器与文件读，与 close 并发属 native use-after-free，RocksDatabase.close
 * 契约）；修复后顺序为：置位静默标志 → 停定时器 → 停网络 → 队列关闭（MQSingle.close 有界等待
 * 在飞回填、持锁关文件流）→ rocksDatabase.close 最后。
 * <p>
 * 关机竞态是否触发 native 崩溃取决于在飞相交的概率（无法确定性复现），本测试固化可确定性的
 * 契约面：stop() 在存在在飞/可再启动回填的负载下正常返回且库已关闭。全程代码构造配置自包含。
 */
public class TestMQManagerStopLive {
	private static final int masterPort = 26200;
	private static final int proxyPort = 26201;

	@Test
	public void testStopWithInFlightFillDrainsThenCloses(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();

		var masterHome = tempDir.resolve("mqmaster").toString();
		var master = new Zeze.MQ.Master.Main(masterHome, MqNetTestSupport.masterConfig(masterPort));
		var manager = new MQManager(tempDir.resolve("mqmanager").toString(), MqNetTestSupport.managerConfig(masterPort, proxyPort));
		var agent = new MasterAgent(MqNetTestSupport.clientConfig(masterPort));
		try {
			master.start();
			manager.start();
			agent.startAndWaitConnectionReady();
			agent.createMQ("topicStopLive", 1, null);

			// 直接在 Manager 侧制造负载（等价于生产者持续发送后消费者未消费的状态）：
			// 盘上写入积压且内存队列为空，随后一条 sendMessage 触发 tryStartBackgroundFill
			// 向共享 worker 池提交后台回填任务（stop 时该任务在途或排队）。
			var partition = partitionOf(manager, "topicStopLive", 0);
			assertNotNull(partition);
			appendBacklogAndTriggerFill(partition, 10);

			// 立即停机：回填任务已提交（或在途）——stop 必须有界排空后再关库。
			manager.stop();
			assertTrue(manager.getRocksDatabase().isClosed(), "stop 后 rocksDatabase 必须已关闭（最后执行）");
			manager = null; // 已停，finally 不再重复 stop
		} finally {
			agent.stop();
			if (null != manager)
				manager.stop();
			master.stop();
		}
	}

	/**
	 * 经反射在盘上直接写积压（绕开内存队列，等价"calculateFill 已扣减、fillMessage 未装载"的
	 * 回填需求态），再发一条消息触发后台回填提交：fill 任务在锁外持索引迭代器与文件读，
	 * 正是 stop 需要有界排空的在飞数据通路。
	 */
	private static void appendBacklogAndTriggerFill(Zeze.MQ.MQSingle single, int count) throws Exception {
		var fileField = Zeze.MQ.MQSingle.class.getDeclaredField("fileWithIndex");
		fileField.setAccessible(true);
		var file = (Zeze.MQ.MQFileWithIndex)fileField.get(single);
		for (int id = 0; id < count; ++id) {
			var message = new Zeze.Builtin.MQ.BMessage.Data();
			message.setTimestamp(id);
			file.appendMessage(message);
		}
		var message = new Zeze.Builtin.MQ.BMessage.Data();
		message.setTimestamp(count);
		var send = new Zeze.Builtin.MQ.BSendMessage.Data();
		send.setMessage(message);
		single.sendMessage(send); // 队列 0 != 盘上积压 count：非直入，highLoad++ 并提交回填
	}

	private static Zeze.MQ.MQSingle partitionOf(MQManager manager, String topic, int index) throws Exception {
		var queuesField = MQManager.class.getDeclaredField("queues");
		queuesField.setAccessible(true);
		@SuppressWarnings("unchecked")
		var queues = (java.util.concurrent.ConcurrentHashMap<String, MQPartition>)queuesField.get(manager);
		var partition = queues.get(topic);
		return null != partition ? partition.get(index) : null;
	}
}
