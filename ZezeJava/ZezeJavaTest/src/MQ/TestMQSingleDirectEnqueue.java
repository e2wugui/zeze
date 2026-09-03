package Zeze.MQ;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND-G2-1 回归：MQSingle.sendMessage 低负载直入与后台 fillMessage 回填的竞态（确定性单线程复现）。
 * <p>
 * 竞态窗口：pullMessage 在 MQSingle 锁内用 calculateFill 把 highLoad 减到 0（fill 快照已算好），
 * 之后在锁外慢慢执行 fillMessage 装载盘上积压。窗口内（毫秒~秒级）到达的 sendMessage 按旧条件
 * （highLoad==0 && 队列未满）直入，新消息插到积压之前：队列变 [新,积压...]，分区内乱序；
 * ack(新) 后 increaseFirstMessageId 盲目 +1 越过未投递的最老消息，崩溃重启后消息永久丢失。
 * <p>
 * 本测试用确定性的单线程序列线性化同一竞态：在空分区上构造 MQSingle 后，直接经
 * MQFileWithIndex.appendMessage 写入积压 ids 0..4（绕开内存队列，等价于"后台回填已执行
 * calculateFill 扣减 highLoad、fillMessage 尚未装载"的窗口状态：盘上积压 5 条、队列空、
 * highLoad==0），然后依次执行：
 * ① sendMessage(id5)（窗口内的新消息，旧代码直入使队列变 [5]）；
 * ② 以窗口前算好的快照参数直接 fillMessage(0,5)（= 后台回填线程在锁外继续执行，把积压
 * 0..4 追加到队尾）；
 * ③ pullMessage()（= ack/tryStartBackgroundFill 触发的下一轮回填，装载剩余积压）。
 * 修复后 ①不得直入、②装载前队列必须为空、③从队尾之后继续装载：全程按 id 有序、无跳过、
 * 无重复。旧代码 ②时装载前队列已是 [5]，装载后变 [5,0,1,2,3,4]——分区内乱序，且后续
 * ack(5) 会使 firstMessageId 越过未投递的 id0（消息永久丢失）。
 * <p>
 * 注：文件放在 src/MQ/ 但声明 package Zeze.MQ——需要访问 MQSingle 的包内测试缝
 * （注入 MQFileWithIndex 的构造器与 pullMessage）；与 src/Zeze/Dbh2/Master 下同包测试先例一致。
 */
@Fast
public class TestMQSingleDirectEnqueue {

	/** 记录每次实际装载的区间与装载前的队列内容（消息以 Timestamp 携带 id 便于断言顺序）。 */
	static class RecordingFile extends MQFileWithIndex {
		static final class Fill {
			final long head;
			final long end;
			final List<String> queueBefore;

			Fill(long head, long end, List<String> queueBefore) {
				this.head = head;
				this.end = end;
				this.queueBefore = queueBefore;
			}
		}

		final List<Fill> fills = new ArrayList<>();
		// MQSingle 的内存队列引用（pullMessage 把队列作为参数传入 fillMessage 时捕获）。
		Queue<BMessage.Data> queueRef;

		RecordingFile(String home, RocksDatabase database) throws Exception {
			super(home, database, "topic", 0);
		}

		@Override
		public void fillMessage(Queue<BMessage.Data> messageQueue, long headMessageId, long endMessageId) {
			queueRef = messageQueue;
			if (headMessageId < endMessageId)
				fills.add(new Fill(headMessageId, endMessageId, queueIds(messageQueue)));
			super.fillMessage(messageQueue, headMessageId, endMessageId);
		}

		List<String> queueNow() {
			return queueIds(queueRef);
		}

		private static List<String> queueIds(Queue<BMessage.Data> messageQueue) {
			var ids = new ArrayList<String>();
			if (null != messageQueue)
				for (var message : messageQueue)
					ids.add(String.valueOf(message.getTimestamp()));
			return ids;
		}
	}

	private static BSendMessage.Data sendMessageOf(long id) {
		var message = new BMessage.Data();
		message.setTimestamp(id);
		var send = new BSendMessage.Data();
		send.setMessage(message);
		return send;
	}

	@Test
	public void testNoDirectEnqueueWhileBacklogNotFilled(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new RecordingFile(home, database);
		try {
			// 空分区上构造（无积压，构造时 pullMessage 不装载；fillMessage 参数捕获队列引用；未绑定网络，推送短路）。
			var single = new MQSingle(new MQPartition(null), "topic", 0, file);

			// 直接写盘制造积压 ids 0..4：等价于后台回填已执行 calculateFill（highLoad 减到 0、
			// 快照 first=0/last=5 已算好）但 fillMessage 尚未装载的竞态窗口。
			for (long id = 0; id < 5; ++id)
				file.appendMessage(sendMessageOf(id).getMessage());

			// 忠实模拟窗口：真实竞态里后台回填任务正在执行（messageFillFuture 非空）。
			// G2-2 修复后 sendMessage 的未直入分支会经 tryStartBackgroundFill 自行启动回填，
			// 不占位的话测试将有一个异步 pullMessage 与下面的同步步骤 ②③ 竞争，失去确定性。
			var fillFutureField = MQSingle.class.getDeclaredField("messageFillFuture");
			fillFutureField.setAccessible(true);
			fillFutureField.set(single, new java.util.concurrent.CompletableFuture<Void>());

			// ① 窗口内 sendMessage(id5)：不得直入（旧代码此处 offer(id5)，队列变 [5]）。
			single.sendMessage(sendMessageOf(5));

			// ② 后台回填线程在锁外继续执行：按窗口前算好的快照装载积压 0..4。
			// fillMessage 的参数契约就是"由 calculateFill 得到"（见其 javadoc），这里即 T1 的快照。
			file.fillMessage(file.queueRef, 0, 5);

			// ③ 下一轮回填（ack 触发 tryStartBackgroundFill 的路径，此处同步驱动）装载剩余积压。
			single.pullMessage();

			// 第二轮回填前队列必须恰好为第一轮装载的结果 [0..4]，且新消息 id5 没有插队。
			Assertions.assertFalse(file.fills.isEmpty(), "回填未执行");
			Assertions.assertEquals(0, file.fills.get(0).head);
			Assertions.assertEquals(5, file.fills.get(0).end);
			Assertions.assertEquals(List.of(), file.fills.get(0).queueBefore,
					"回填装载前队列必须为空：积压未装载完时sendMessage不得直入（旧代码此处已是[5]，装载后变[5,0,1,2,3,4]）");
			// 剩余积压 id5 由下一轮回填在队尾之后按序装载：全程按 id 有序、无跳过、无重复。
			Assertions.assertEquals(2, file.fills.size(), "剩余积压id5应由下一轮回填装载");
			Assertions.assertEquals(5, file.fills.get(1).head);
			Assertions.assertEquals(6, file.fills.get(1).end);
			Assertions.assertEquals(List.of("0", "1", "2", "3", "4"), file.fills.get(1).queueBefore);
			Assertions.assertEquals(List.of("0", "1", "2", "3", "4", "5"), file.queueNow());
			// 没有 ack 发生，位点不得推进；id5 已落盘。
			Assertions.assertEquals(0, file.getFirstMessageId());
			Assertions.assertEquals(6, file.getNextMessageId());
		} finally {
			database.close();
			file.close();
		}
	}
}
