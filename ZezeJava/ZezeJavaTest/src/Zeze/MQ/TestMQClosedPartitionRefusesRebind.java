package Zeze.MQ;

import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import Zeze.Builtin.MQ.BMessage;
import Zeze.Net.Service;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND25 mq-01 回归：removePartition 与 arrangeConsumer 的交叠窗口内，晚到的 bind 会在
 * 已 close 分区上重设 bindSocket 并重推（close 不清 messageQueue，队列非空恒成立）；
 * 消费者 ack 命中 closed 早退后 finally 必达 tryPushMessage——同一条消息以 RTT 速度
 * 无限重投（无退避、无日志、位点冻结），直到消费者断连或 Manager 重启。
 * 直驱判别（免竞速）：close 后 bind(sessionId, socket) 与直调 tryPushMessage 都必须
 * 被 closed 闸拒绝——bindSocket/pendingPushMessage 恒 null、不发起任何推送。
 */
@Fast
public class TestMQClosedPartitionRefusesRebind {

	private static Object getField(MQSingle single, String name) throws Exception {
		var f = MQSingle.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(single);
	}

	@Test
	public void testBindAfterCloseRefused(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		try {
			// 先落 3 条积压再构造：构造装载即入内存队列（无绑定不推送）。
			for (long id = 0; id < 3; ++id)
				file.appendMessage(MqTestSupport.messageOf(id));
			Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
			file.fillMessage(queue, 0, 3);
			var single = new MQSingle(new MQPartition(null), "topic", 0, file);
			// 构造形态核查：积压在队（等价"积压已装载、重排在途"的交叠前提）。
			single.close(); // removePartition→close：closed 置位、bindSocket=null

			var fake = new MqTestSupport.FakeSocket(new Service("TestMQClosedPartitionRefusesRebind"));
			single.bind(9L, fake); // 交叠窗口内晚到的 arrangeConsumer 重绑

			Assertions.assertNull(getField(single, "bindSocket"),
					"close 后 bind 必须被拒绝（重设 bindSocket 即重推死循环的起点）");
			Assertions.assertNull(getField(single, "pendingPushMessage"),
					"close 后不得发起推送（修复前：bind→tryPushMessage 立即推队首）");

			// 直调推送入口同闸（private，反射直驱）：即使他路径重设了 socket 状态，推送本身也必须短路。
			var push = MQSingle.class.getDeclaredMethod("tryPushMessage");
			push.setAccessible(true);
			push.invoke(single);
			Assertions.assertNull(getField(single, "pendingPushMessage"), "closed 分区的 tryPushMessage 必须短路");
		} finally {
			database.close();
			file.close(); // single.close 已关，幂等
		}
	}
}
