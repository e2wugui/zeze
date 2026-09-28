package Zeze.MQ;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.PushMessage;
import Zeze.Config;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND21 GB-C01 回归：MQSingle.handlePushResult 的唯一闸是 managerStopped——只覆盖停机路径。
 * 分区删除路径（Manager 存活，GB-D01 对账链的常态产物）上 removePartition→close 置 closed
 * 但不取消在飞 pendingPushMessage（rpc 上下文仍在 proxyServer），晚到的 ack/超时回调继续执行：
 * 成功分支 increaseFirstMessageId 的 meta.put 与其后 deletePartitionStorage 的 dropTable(meta)
 * 是同句柄竞速（native use-after-free）；失败分支 tryDeadLetter 的 dlq.put 落在 dlq 前缀
 * deleteRange 之后则复活"分区已删却永无人认领"的孤儿死信键（FND20 GB-D02 要消灭的跨代际残留）。
 * <p>
 * 修复形态（对齐既有 stopped/closed 闸）：handlePushResult 在 managerStopped 旁同点加 closed 检查
 * （closed 在 close 锁内置位，回调持同锁读即精确）。at-least-once 无损：分区正在删除，位点丢失
 * 是删除的既定语义。
 * <p>
 * 双车道判别（均行为红/绿，不踩 native 面——测试库全程打开）：
 * ① 成功 ack 面（null-manager 直构：managerStopped 恒 false，唯一可能拦住回调的是 closed）：
 * close 后回调不得触 meta（计数器+位点断言）；
 * ② 失败 ack 面（真 Manager + PushRetryMax=1 一投即转死信）：close 后回调不得写 dlq。
 * 注：文件放 src/MQ/ 但声明 package Zeze.MQ（包内缝），与 TestFnd20GB* 先例一致。
 */
@Fast
public class TestGBC01PushCallbackClosedGate {

	/** increaseFirstMessageId 计数注入（触 meta 面的观察点）与内存队列引用捕获。 */
	static class CountingFile extends MQFileWithIndex {
		final AtomicInteger increaseCalls = new AtomicInteger();
		Queue<BMessage.Data> queueRef;

		CountingFile(String home, RocksDatabase database) throws Exception {
			super(home, database, "topic", 0);
		}

		@Override
		public void fillMessage(Queue<BMessage.Data> messageQueue, long headMessageId, long endMessageId) {
			queueRef = messageQueue; // 捕获 MQSingle 的内存队列引用
			super.fillMessage(messageQueue, headMessageId, endMessageId);
		}

		@Override
		public void increaseFirstMessageId() {
			increaseCalls.incrementAndGet();
			super.increaseFirstMessageId();
		}
	}

	private static Object getField(MQSingle single, String name) throws Exception {
		var f = MQSingle.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(single);
	}

	/**
	 * ① 成功 ack 面：close（分区删除路径形态）后到达的成功回调不得触 meta——旧代码
	 * managerStopped 恒 false（null-manager 形态）直达 increaseFirstMessageId（判红：
	 * 计数与位点推进），修复后 closed 闸短路（计数 0、位点不动、pending 由 finally 复位）。
	 */
	@Test
	public void testLateSuccessAckAfterCloseDoesNotTouchMeta(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new CountingFile(home, database);
		try {
			var single = new MQSingle(new MQPartition(null), "topic", 0, file);

			// 盘上写 3 条积压后手动装载进内存队列：等价于"积压已装载、推送进行中"。
			for (long id = 0; id < 3; ++id)
				file.appendMessage(Fnd19MqTestSupport.messageOf(id));
			file.fillMessage(file.queueRef, 0, 3);

			// 模拟在飞推送的 ack 到达前分区被 close（removePartition→close：closed 置位、
			// bindSocket=null，Manager 存活未停机——本测试 null-manager 形态 managerStopped 恒 false）。
			var ack = new PushMessage();
			ack.setResultCode(0);
			Fnd19MqTestSupport.setPending(single, ack);
			single.close();

			// close 后到达的成功 ack（消费者刚处理完/20s 超时回调在途的常态形态）。
			single.handlePushResult();

			Assertions.assertEquals(0, file.increaseCalls.get(),
					"closed 后的 ack 回调不得触 meta 位点（increaseFirstMessageId 的 meta.put 与"
							+ " deletePartitionStorage 的 dropTable(meta) 同句柄竞速，FND21 GB-C01）");
			Assertions.assertEquals(0, file.getFirstMessageId(), "位点不得推进（删除路径位点丢失是既定语义）");
			Assertions.assertEquals(3, file.getNextMessageId(), "next 不受影响");
			Assertions.assertNull(getField(single, "pendingPushMessage"), "pending 由回调 finally 复位（不悬挂）");
		} finally {
			database.close();
			file.close(); // single.close 已关，幂等
		}
	}

	/**
	 * ② 失败 ack 面：真 Manager（PushRetryMax=1 一投失败即转死信）下，close 后到达的失败回调
	 * 不得写 dlq——旧代码直达 tryDeadLetter 的 dlq.put（判红：键存在），修复后短路（键不存在、
	 * 消息留队首位点不动）。
	 */
	@Test
	public void testLateFailureAckAfterCloseDoesNotWriteDlq(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		try {
			manager.getMqConfig().setPushRetryMax(1); // 一投失败即达转死信阈值（缩短失败分支路径）
			manager.createPartition("topic", new HashSet<>(java.util.List.of(0)));
			var single = manager.getQueueForTest("topic").get(0);
			single.sendMessage(Fnd19MqTestSupport.sendMessageOf(0)); // 直入队列（无积压：id0 在队首）

			var ack = new PushMessage();
			ack.setResultCode(-999); // 非0/非eConsumerNotFound(5)=投递失败分支
			Fnd19MqTestSupport.setPending(single, ack);
			single.close(); // 删除路径形态：closed 置位（Manager 存活、未停机）

			single.handlePushResult(); // 晚到的失败 ack

			Assertions.assertNull(manager.getDlqTable().get(MQManager.dlqKey("topic", 0, 0)),
					"closed 后的失败回调不得写 dlq（落在 deletePartitionStorage 的前缀清理之后="
							+ "孤儿死信键复活，FND21 GB-C01）");
			Assertions.assertEquals(0, single.getFileForTest().getFirstMessageId(),
					"位点不得推进（消息留队首，at-least-once）");
		} finally {
			manager.stop(); // 未 start 的 stop 安全（TestFnd20GBC02 同款依据）
		}
	}
}
