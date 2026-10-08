package Zeze.MQ;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.PushMessage;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND20 GB-D02 回归：死信表消费侧闭合（拍板 A：本地最小闭合）。
 * <p>
 * 修复前 dlq 只有写入端（FND19 GB-D06 落地），三个缺口：
 * ① deletePartitionStorage 清段文件/索引列族/meta 但不清 dlq 中该 (topic,partition) 前缀的
 * 死信键——对账删除的分区在 dlq 成为永无人认领的死数据，且分区重建后位点从 0 重计，旧死信键
 * 与新代际同 id 键空间重叠、代际无从分辨；
 * ② dlq 无 TTL/限量/告警，毒消息持续到达时"只入不出"无界增长；
 * ③ 日志宣称"可重放"但仓内不存在任何重放入口。
 * <p>
 * 修复后：① deletePartitionStorage 联动清 dlq 前缀（getTable 非懒建，null 即跳过）；
 * ② MQConfig.DlqMaxEntries 上界（estimate 口径），写入后超限按键序淘汰至目标线并 warn；
 * ③ MQManager.replayDeadLetter 本地重放（读死信→重新投递到原分区尾，新 messageId→消费死信键）。
 * <p>
 * 毒消息路径以 TestFnd19GBD06 的失败应答缝驱动（反射置 pending + 直调 handlePushResult）。
 * 新增 API（setDlqMaxEntries/replayDeadLetter）经反射访问：测试需双车道复用（orig 基线缺失
 * 即判红），形态对齐 TestFnd20GBC02 的反射缝先例。
 */
@Fast
public class TestDeadLetterClosure {

	/** dlq 表中 (topic,partition,messageId) 的键编码（与 MQSingle.tryDeadLetter 写入端一致）。 */
	private static byte[] dlqKey(String topic, int partition, long messageId) {
		var bb = ByteBuffer.Allocate();
		bb.WriteString(topic);
		bb.WriteInt4(partition);
		bb.WriteLong8(messageId);
		return java.util.Arrays.copyOfRange(bb.Bytes, bb.ReadIndex, bb.ReadIndex + bb.size());
	}

	private static int dlqCount(MQManager manager) throws Exception {
		var table = manager.getRocksDatabase().getTable(MQManager.DlqTableName);
		if (null == table)
			return 0;
		var count = 0;
		try (var it = table.iterator()) {
			it.seekToFirst();
			while (it.isValid()) {
				++count;
				it.next();
			}
		}
		return count;
	}

	/** 反射置 MQConfig.DlqMaxEntries；旧基线无此方法判红（修复不存在）。 */
	private static void setDlqMaxEntries(MQManager manager, int value) throws Exception {
		try {
			Method m = manager.getMqConfig().getClass().getMethod("setDlqMaxEntries", int.class);
			m.invoke(manager.getMqConfig(), value);
		} catch (NoSuchMethodException e) {
			throw new AssertionError("DlqMaxEntries 保留上界缺失（FND20 GB-D02 修复不存在）", e);
		}
	}

	/** 反射调 MQManager.replayDeadLetter；旧基线无此方法判红（修复不存在）。 */
	private static void replayDeadLetter(MQManager manager, String topic, int partition, long messageId)
			throws Exception {
		try {
			Method m = MQManager.class.getMethod("replayDeadLetter", String.class, int.class, long.class);
			m.invoke(manager, topic, partition, messageId);
		} catch (NoSuchMethodException e) {
			throw new AssertionError("replayDeadLetter 本地重放入口缺失（FND20 GB-D02 修复不存在）", e);
		}
	}

	/** 反射调 replayDeadLetter 并断言失败原因（InvocationTargetException 的 cause 形态）。 */
	private static void replayDeadLetterExpectCause(MQManager manager, String topic, int partition,
			long messageId, String expectFragment) {
		var ex = Assertions.assertThrows(InvocationTargetException.class,
				() -> replayDeadLetter(manager, topic, partition, messageId));
		Assertions.assertNotNull(ex.getCause(), "重放失败必须显式报错（不静默）");
		Assertions.assertTrue(ex.getCause() instanceof IllegalArgumentException,
				"重放入口参数错误以 IllegalArgumentException 显式报错，实际=" + ex.getCause());
		Assertions.assertTrue(ex.getCause().getMessage().contains(expectFragment),
				"错误消息须含归因片段'" + expectFragment + "'，实际=" + ex.getCause().getMessage());
	}

	/** 一次投递失败（TestFnd19GBD06 同款缝：反射置 pending + 直调 handlePushResult）。 */
	private static void failOnce(MQSingle single) throws Exception {
		var push = new PushMessage();
		push.setResultCode(1); // 非0非 eConsumerNotFound：投递失败
		MqTestSupport.setPending(single, push);
		single.handlePushResult();
	}

	/** 毒杀一条消息转死信（PushRetryMax=1：单次失败即达上限）。 */
	private static void poisonOne(MQSingle single, long id) throws Exception {
		single.sendMessage(MqTestSupport.sendMessageOf(id));
		failOnce(single);
	}

	/**
	 * ① 清理联动：deletePartitionStorage 联动清 dlq 中该 (topic,partition) 前缀死信键；
	 * 前缀圈定不跨分区（别的分区死信原样保留）；无死信时删除分区不凭空造出 dlq 表。
	 * 旧基线：删除后死信残留（判红）。
	 */
	@Test
	public void testDeletePartitionClearsDlqPrefix(@TempDir Path tempDir) throws Exception {
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		try {
			manager.getMqConfig().setPushRetryMax(1);
			manager.createPartition("t", new HashSet<>(List.of(0)));
			manager.createPartition("t2", new HashSet<>(List.of(0)));
			poisonOne(manager.getQueueForTest("t").get(0), 0);
			poisonOne(manager.getQueueForTest("t2").get(0), 0);
			Assertions.assertEquals(2, dlqCount(manager), "前置：两个分区各一条死信");

			manager.deletePartition("t", new HashSet<>(List.of(0)));
			Assertions.assertEquals(1, dlqCount(manager),
					"删除分区须联动清 dlq 中该 (topic,partition) 前缀死信键（FND20 GB-D02：死信生命周期与分区绑定）");
			Assertions.assertNull(manager.getRocksDatabase().getTable(MQManager.DlqTableName)
							.get(dlqKey("t", 0, 0)),
					"被删分区的死信键不得残留（旧键与新代际同 id 键空间重叠，代际无从分辨）");
			Assertions.assertNotNull(manager.getDlqTable().get(dlqKey("t2", 0, 0)),
					"前缀清理不得跨分区误删（WriteString 长度前缀唯一圈定该分区）");
		} finally {
			manager.stop();
		}
	}

	/** 无死信的分区删除不得凭空造出 dlq 表（getTable 非懒建细节）。 */
	@Test
	public void testDeletePartitionWithoutDeadLetterKeepsDlqTableUncreated(@TempDir Path tempDir) throws Exception {
		var manager = new MQManager(tempDir.resolve("manager2").toString(), new Config());
		try {
			manager.createPartition("t", new HashSet<>(List.of(0)));
			manager.deletePartition("t", new HashSet<>(List.of(0)));
			Assertions.assertNull(manager.getRocksDatabase().getTable(MQManager.DlqTableName),
					"清理动作不得反向制造 dlq 表（getTable 非懒建，null 即跳过）");
		} finally {
			manager.stop();
		}
	}

	/**
	 * ② 保留上界：DlqMaxEntries（estimate 口径）超限按键序淘汰最老条目并告警（warn 即审计面），
	 * 剩余条目为键序最新者。旧基线：无 setDlqMaxEntries（反射判红）。
	 */
	@Test
	public void testDlqCapEvictsOldestByKeyOrder(@TempDir Path tempDir) throws Exception {
		var manager = new MQManager(tempDir.resolve("manager3").toString(), new Config());
		try {
			manager.getMqConfig().setPushRetryMax(1);
			setDlqMaxEntries(manager, 2); // 旧基线：NoSuchMethodException 判红
			manager.createPartition("t", new HashSet<>(List.of(0)));
			var single = manager.getQueueForTest("t").get(0);
			for (long id = 0; id < 6; ++id)
				poisonOne(single, id); // messageId 0..5 逐条转死信

			Assertions.assertEquals(2, dlqCount(manager),
					"超限后 dlq 收敛于 DlqMaxEntries（淘汰动作即告警面，'只入不出'不成立）");
			var table = manager.getDlqTable();
			for (long id = 0; id < 4; ++id)
				Assertions.assertNull(table.get(dlqKey("t", 0, id)),
						"按键序淘汰最老条目（messageId=" + id + " 应被淘汰）");
			Assertions.assertNotNull(table.get(dlqKey("t", 0, 4)), "键序最新条目保留");
			Assertions.assertNotNull(table.get(dlqKey("t", 0, 5)), "键序最新条目保留");
		} finally {
			manager.stop();
		}
	}

	/**
	 * ③ 本地重放：读死信→重新投递到原分区尾（新 messageId，内容保持）→成功后消费死信键；
	 * 死信不存在/分区不存在均显式报错。旧基线：无 replayDeadLetter（反射判红）。
	 */
	@Test
	public void testReplayDeadLetterRequeuesAndConsumesKey(@TempDir Path tempDir) throws Exception {
		var manager = new MQManager(tempDir.resolve("manager4").toString(), new Config());
		try {
			manager.getMqConfig().setPushRetryMax(1);
			manager.createPartition("t", new HashSet<>(List.of(0)));
			var single = manager.getQueueForTest("t").get(0);
			var file = single.getFileForTest();
			poisonOne(single, 42); // messageId=0，timestamp=42：唯一可辨识的重放内容
			Assertions.assertNotNull(manager.getDlqTable().get(dlqKey("t", 0, 0)), "前置：死信已落");

			replayDeadLetter(manager, "t", 0, 0); // 旧基线：NoSuchMethodException 判红

			Assertions.assertNull(manager.getDlqTable().get(dlqKey("t", 0, 0)),
					"重放成功后消费死信键（处置路径闭合）");
			Assertions.assertEquals(2, file.getNextMessageId(), "重放=重新追加到原分区尾（新 messageId=1）");
			Assertions.assertEquals(1, file.getFirstMessageId(), "原消息位点照常（毒消息处置不回滚）");
			@SuppressWarnings("unchecked")
			var queue = (Queue<BMessage.Data>)MqTestSupport.getField(single, "messageQueue");
			Assertions.assertEquals(1, queue.size(), "重放消息已入内存队列");
			Assertions.assertEquals(42, queue.peek().getTimestamp(), "重放内容保持（可审计可重放契约）");

			// 显式报错面：死信不存在 / 分区不存在（不静默丢弃也不误投别处）。
			replayDeadLetterExpectCause(manager, "t", 0, 99, "dead letter not found");
			// 分区不存在的死信（直写 dlq 构造合法 value=BMessage+时间戳，partition=9 无分区）：
			// 读取成功后必须显式报错。
			var orphan = MqTestSupport.messageOf(7);
			var orphanBb = ByteBuffer.Allocate();
			orphan.encode(orphanBb);
			var stamped = java.util.Arrays.copyOfRange(
					orphanBb.Bytes, orphanBb.ReadIndex, orphanBb.ReadIndex + orphanBb.size() + 8);
			ByteBuffer.longBeHandler.set(stamped, stamped.length - 8, System.currentTimeMillis());
			manager.getDlqTable().put(dlqKey("t", 9, 123), stamped);
			replayDeadLetterExpectCause(manager, "t", 9, 123, "partition not exists");
		} finally {
			manager.stop();
		}
	}
}
