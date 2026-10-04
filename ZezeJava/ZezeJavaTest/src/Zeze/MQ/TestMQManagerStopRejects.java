package Zeze.MQ;

import java.nio.file.Path;
import Zeze.Builtin.MQ.PushMessage;
import Zeze.Config;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND19 GB-C02 回归（形态测试）：stop 置位后，在飞数据面入口必须被拒绝且不触碰文件/rocksdb。
 * <p>
 * RocksDatabase.close 的契约：close 前必须静默所有数据通路（并发 get/put/delete/迭代器是
 * native use-after-free）。旧 MQManager.stop 不排空 worker 池在飞 SendMessage、后台回填
 * messageFillFuture、push 应答回调即关库。关机竞态本身无法确定性测试（取决于关机时刻在飞
 * 相交），本测试固化可确定性的契约面：stopped 置位后——
 * ① sendMessage（在飞/晚到的提交）锁内拒绝，nextMessageId 不推进、无异常逃逸之外的状态变化；
 * ② handlePushResult（Service.stop 后超时回调照常触发的应答）不推进 firstMessageId 位点；
 * ③ close 有界排空（无在飞 fill 时立即）后正常返回。
 * 端到端"活 Manager 停机不崩"由 TestMQManagerStopLive 覆盖。
 * <p>
 * 注：需要 MQSingle 的包内测试缝（handlePushResult）与 MQManager.stopped 的反射置位
 *（布局约定见 Fnd19MqTestSupport）。
 */
@Fast
public class TestMQManagerStopRejects {

	private static void setStopped(MQManager manager, boolean value) throws Exception {
		var f = MQManager.class.getDeclaredField("stopped");
		f.setAccessible(true);
		f.setBoolean(manager, value);
	}

	@Test
	public void testDataPlaneRejectedAfterStop(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		// 真实（未 start 的）manager：MQSingle 的持有链 partition→manager→mqConfig 完整。
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		try {
			var home = tempDir.resolve("db").toString();
			var database = new RocksDatabase(home);
			var file = new MQFileWithIndex(home, database, "topic", 0);
			var partition = new MQPartition(manager);
			var single = new MQSingle(partition, "topic", 0, file);
			try {
				// 基线：stop 前 sendMessage 正常落盘。
				single.sendMessage(Fnd19MqTestSupport.sendMessageOf(0));
				Assertions.assertEquals(1, file.getNextMessageId());

				// 模拟 stop() 最前置位（真实 stop 里发生在关网络之前）。
				setStopped(manager, true);

				// ① 停机后到达的提交被显式拒绝：不落盘（旧代码继续 appendMessage，与随后的
				// rocksDatabase.close 并发属 native use-after-free）。
				Assertions.assertThrows(IllegalStateException.class,
						() -> single.sendMessage(Fnd19MqTestSupport.sendMessageOf(1)),
						"stopped 后 sendMessage 必须在锁内拒绝（不发成功应答）");
				Assertions.assertEquals(1, file.getNextMessageId(), "拒绝的提交不得落盘");

				// ② 在飞推送的应答回调（socket 关闭后由 rpc 超时照常触发）被拒绝：不推进位点。
				var push = new PushMessage();
				push.Argument.setTopic("topic");
				push.Argument.setSessionId(77L);
				push.setResultCode(0);
				Fnd19MqTestSupport.setPending(single, push);
				single.handlePushResult(); // 不得抛出、不得触碰 meta
				Assertions.assertEquals(0, file.getFirstMessageId(), "stopped 后应答回调不得推进 firstMessageId");
			} finally {
				// ③ close（有界排空路径）正常返回：等待在飞回填（无→立即）、持锁关文件流。
				single.close();
				database.close();
			}

			// stopped 恢复仅在测试内使用，真实 stop 后实例即废弃；此处不再复位。
		} finally {
			// 未 start 的 MQManager.stop() 安全（DaemonTimer.stop 未启动即 return、Acceptor/Service.stop
			// 对未启动组件为 no-op）：关掉构造期打开的 rocksdb（TestMQSinglePushRpcTimeout 同款依据）。
			manager.stop();
		}
	}
}
