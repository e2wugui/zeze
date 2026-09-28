package Zeze.MQ;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Builtin.MQ.PushMessage;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND-G2-3 回归：推送 ack 回调中 increaseFirstMessageId 抛异常后 pendingPushMessage 永久悬挂，
 * 分区投递停摆。
 * <p>
 * ack 回调在线程池执行（dispatchRpcResponse），resultCode==0 分支中 messageQueue.poll() 已执行、
 * increaseFirstMessageId 内 meta.put 抛 RocksDBException（磁盘满/IO 错误/停机时序：MQManager.stop
 * 先关 rocks 再走 queue.close，晚到的 ack 正踩此窗）包装的 RuntimeException 越过
 * pendingPushMessage=null——该 rpc 的超时上下文已被消费、不会再有回调，tryPushMessage 永远看到
 * 非 null pending 直接返回，该分区推送永久停止。
 * <p>
 * 修复：resultCode==0 分支改为先 increaseFirstMessageId 再 poll（位点先推进：increase 抛异常时
 * 消息留队首、位点未推进，pending 复位后重推同一条=at-least-once，位点永不跳步）；回调体
 * try/finally 确保 pending 复位与续推必达；MQFileWithIndex.increaseFirstMessageId 的内存位点
 * 改为持久化成功后才前移（否则异常时内存已+1未持久化，重推后再推进会跳过一条消息）。
 * <p>
 * 注：文件放在 src/MQ/ 但声明 package Zeze.MQ——需要访问 MQSingle 的包内测试缝（注入
 * MQFileWithIndex 的构造器与 handlePushResult 回调体）；与 TestMQSingleDirectEnqueue 先例一致。
 */
@Fast
public class TestMQSingleAckCallbackStall {

	/** increaseFirstMessageId 可注入失败的 MQFileWithIndex（failIncrease=true 时抛异常）。 */
	static class FailingIncreaseFile extends MQFileWithIndex {
		volatile boolean failIncrease;
		final AtomicInteger increaseCalls = new AtomicInteger();
		// MQSingle 的内存队列引用（构造时 pullMessage 把队列作为参数传入 fillMessage 时捕获）。
		Queue<BMessage.Data> queueRef;

		FailingIncreaseFile(String home, RocksDatabase database) throws Exception {
			super(home, database, "topic", 0);
		}

		@Override
		public void fillMessage(Queue<BMessage.Data> messageQueue, long headMessageId, long endMessageId) {
			queueRef = messageQueue; // 捕获 MQSingle 的内存队列引用，供测试手动装载与断言。
			super.fillMessage(messageQueue, headMessageId, endMessageId);
		}

		@Override
		public void increaseFirstMessageId() {
			increaseCalls.incrementAndGet();
			if (failIncrease)
				throw new RuntimeException("injected increaseFirstMessageId failure");
			super.increaseFirstMessageId();
		}
	}

	private static BSendMessage.Data sendMessageOf(long id) {
		var message = new BMessage.Data();
		message.setTimestamp(id);
		var send = new BSendMessage.Data();
		send.setMessage(message);
		return send;
	}

	private static Object getField(Object obj, String name) throws Exception {
		var f = obj.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static void setPending(MQSingle single, PushMessage push) throws Exception {
		var f = MQSingle.class.getDeclaredField("pendingPushMessage");
		f.setAccessible(true);
		f.set(single, push);
	}

	private static List<String> queueIds(Queue<BMessage.Data> messageQueue) {
		var ids = new ArrayList<String>();
		if (null != messageQueue)
			for (var message : messageQueue)
				ids.add(String.valueOf(message.getTimestamp()));
		return ids;
	}

	/**
	 * ack 成功但位点持久化失败：回调异常后 pending 必须复位（否则分区推送永久停摆）、
	 * 消息必须留在队首（重推同一条，at-least-once）、位点不得推进；恢复后从原位继续，不跳步。
	 */
	@Test
	public void testAckCallbackExceptionDoesNotStallPush(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new FailingIncreaseFile(home, database);
		try {
			// 空分区构造（未绑定网络，tryPushMessage 短路）。
			var single = new MQSingle(new MQPartition(null), "topic", 0, file);

			// 盘上写 3 条积压后手动装载进内存队列：等价于"积压已装载、推送进行中"的状态。
			for (long id = 0; id < 3; ++id)
				file.appendMessage(sendMessageOf(id).getMessage());
			file.fillMessage(file.queueRef, 0, 3);
			Assertions.assertEquals(List.of("0", "1", "2"), queueIds(file.queueRef));

			// 模拟推送在途（pending 非空）+ ack 成功（resultCode==0）+ 位点持久化失败。
			file.failIncrease = true;
			var ack1 = new PushMessage();
			ack1.setResultCode(0);
			setPending(single, ack1);

			// ack 回调到达：increaseFirstMessageId 抛出，但 pending 复位与续推必须必达
			// （旧代码异常越过 pendingPushMessage=null，该分区推送永久停止）。
			Assertions.assertThrows(RuntimeException.class, single::handlePushResult);
			Assertions.assertNull(getField(single, "pendingPushMessage"),
					"回调异常后pendingPushMessage必须复位，否则分区推送永久停摆");
			Assertions.assertEquals(List.of("0", "1", "2"), queueIds(file.queueRef),
					"位点推进失败时消息必须留在队首（重推同一条，at-least-once）");
			Assertions.assertEquals(0, file.getFirstMessageId(), "位点不得推进（未持久化成功）");

			// 持续失败一轮：重推的仍是队首同一条，位点仍不动（事件驱动重试，非紧密循环）。
			var ack2 = new PushMessage();
			ack2.setResultCode(0);
			setPending(single, ack2);
			Assertions.assertThrows(RuntimeException.class, single::handlePushResult);
			Assertions.assertNull(getField(single, "pendingPushMessage"));
			Assertions.assertEquals(List.of("0", "1", "2"), queueIds(file.queueRef));
			Assertions.assertEquals(0, file.getFirstMessageId());

			// 故障恢复：同一条消息重推后再次 ack 成功，从原位继续——位点从 0 推进到 1（不跳步）。
			file.failIncrease = false;
			var ack3 = new PushMessage();
			ack3.setResultCode(0);
			setPending(single, ack3);
			single.handlePushResult();
			Assertions.assertEquals(1, file.getFirstMessageId());
			Assertions.assertEquals(List.of("1", "2"), queueIds(file.queueRef));
			Assertions.assertNull(getField(single, "pendingPushMessage"));
			Assertions.assertEquals(3, file.increaseCalls.get(), "成功轮只推进一次位点");
		} finally {
			database.close();
			file.close();
		}
	}
}
