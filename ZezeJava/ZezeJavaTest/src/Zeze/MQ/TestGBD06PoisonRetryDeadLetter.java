package Zeze.MQ;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.PushMessage;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND19 GB-D06 毒消息重投上限+死信回归（@Fast，拍板 A-lite：管理端重投计数+指数退避+上限后死信本地表）。
 * <p>
 * 修复前：非0非 eConsumerNotFound 的推送结果一律复位 pending 即时重推同一条——无计数、无退避、
 * 无终态，队头毒消息使分区吞吐降为每 RpcTimeout 一次失败尝试、永久阻塞。
 * <p>
 * 修复后：失败计数递增（BPushMessage.RetryCount 随推送携带）；未达上限按次数指数退避延迟重推
 *（退避期间分区推送整体暂停——保序代价，封顶 60s）；达上限（MQConfig.PushRetryMax，默认16）
 * 位点照常推进 + 消息转存 rocksdb "dlq" 表（可配丢弃档）+ warn 元数据摘要。
 * <p>
 * 失败应答以 TestMQSingleAckCallbackStall 同款缝驱动（反射置 pending + 直调 handlePushResult）；
 * 退避调度器注入同步执行（捕获延迟序列即退避形态断言）。
 */
@Fast
public class TestGBD06PoisonRetryDeadLetter {

	private static void failOnce(MQSingle single) throws Exception {
		var push = new PushMessage();
		push.setResultCode(1); // 非0非 eConsumerNotFound：投递失败
		Fnd19MqTestSupport.setPending(single, push);
		single.handlePushResult();
	}

	/** dlq 表中 (topic,partition,messageId) 的键编码（与 MQSingle.tryDeadLetter 一致）。 */
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

	@Test
	public void testRetryCountBackoffAndDeadLetter(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("manager").toString();
		var manager = new MQManager(home, new Config());
		try {
			manager.getMqConfig().setPushRetryMax(3);           // 首推+2次重投，第3次失败转死信
			manager.getMqConfig().setPushRetryBackoffBaseMs(200); // 退避序列 400, 800（base<<retryCount）
			manager.createPartition("poison", new HashSet<>(List.of(0)));
			var single = manager.getQueueForTest("poison").get(0);
			Assertions.assertNull(manager.getRocksDatabase().getTable(MQManager.DlqTableName),
					"死信表懒建（无死信不建表）");

			// 退避调度器注入：同步执行（清窗口位）并捕获延迟序列。
			var delays = new ArrayList<Long>();
			single.retryScheduler = (delayMs, action) -> {
				delays.add(delayMs);
				action.run();
				return CompletableFuture.completedFuture(null);
			};

			for (long id = 0; id < 3; ++id)
				single.sendMessage(Fnd19MqTestSupport.sendMessageOf(id)); // bindSocket=null：装载不推送

			// 消息0：失败计数递增 + 指数退避（400=200<<1, 800=200<<2），未达上限不推进位点。
			failOnce(single);
			Assertions.assertEquals(1, Fnd19MqTestSupport.getField(single, "headRetryCount"), "首败后计数=1");
			failOnce(single);
			Assertions.assertEquals(2, Fnd19MqTestSupport.getField(single, "headRetryCount"));
			Assertions.assertEquals(List.of(400L, 800L), delays, "按次数指数退避");
			Assertions.assertEquals(0, single.getFileForTest().getFirstMessageId(), "未达上限位点不动");
			Assertions.assertEquals(Boolean.FALSE, Fnd19MqTestSupport.getField(single, "retryPending"), "退避到期后窗口位清除");

			// 第3次失败=达上限：转死信，位点照常推进，队头放行。
			var before = System.currentTimeMillis();
			failOnce(single);
			Assertions.assertEquals(0, Fnd19MqTestSupport.getField(single, "headRetryCount"), "转死信后计数清零");
			Assertions.assertEquals(1, single.getFileForTest().getFirstMessageId(), "达上限位点照常推进");
			Assertions.assertEquals(1, dlqCount(manager), "死信表条目数（value 含 BMessage+时间戳，逐键断言见下）");

			// 死信内容：key=poison|0|0；value=BMessage 编码 + 8字节BE时间戳。
			var value = manager.getDlqTable().get(dlqKey("poison", 0, 0));
			Assertions.assertNotNull(value, "消息0的死信条目存在");
			var dead = new BMessage.Data();
			dead.decode(ByteBuffer.Wrap(java.util.Arrays.copyOfRange(value, 0, value.length - 8)));
			Assertions.assertEquals(0, dead.getTimestamp(), "死信保留原消息内容（可审计可重放）");
			var deadAt = ByteBuffer.ToLongBE(value, value.length - 8);
			Assertions.assertTrue(deadAt >= before && deadAt <= System.currentTimeMillis(), "死信时间戳");

			// 消息1：同路径再毒杀一条（位点推进到 2）。
			failOnce(single);
			failOnce(single);
			failOnce(single);
			Assertions.assertEquals(2, single.getFileForTest().getFirstMessageId());
			Assertions.assertNotNull(manager.getDlqTable().get(dlqKey("poison", 0, 1)));

			// 消息2：丢弃档（PushDeadLetterPolicy=discard）——不落死信，位点仍推进（日志兜底）。
			manager.getMqConfig().setPushDeadLetterPolicy("discard");
			failOnce(single);
			failOnce(single);
			failOnce(single);
			Assertions.assertEquals(3, single.getFileForTest().getFirstMessageId(), "丢弃档位点照常推进");
			Assertions.assertNull(manager.getDlqTable().get(dlqKey("poison", 0, 2)), "丢弃档不落死信表");
			Assertions.assertEquals(2, dlqCount(manager), "死信表总数不变（0/1两条）");
		} finally {
			manager.stop();
		}
	}

	/** 退避形态纯函数：指数增长 + 封顶（封顶理由：退避期间分区不推任何消息，不封顶则毒消息阻塞无界）。 */
	@Test
	public void testBackoffFormulaCapped() {
		var config = new MQConfig(); // 默认：base=500ms, cap=60s
		Assertions.assertEquals(500, MQSingle.retryBackoffMs(0, config));
		Assertions.assertEquals(1000, MQSingle.retryBackoffMs(1, config));
		Assertions.assertEquals(8000, MQSingle.retryBackoffMs(4, config));
		Assertions.assertEquals(60_000, MQSingle.retryBackoffMs(8, config), "2^8*500=128000 封顶 60s");
		Assertions.assertEquals(60_000, MQSingle.retryBackoffMs(16, config), "默认16次上限前早已封顶");
		Assertions.assertEquals(60_000, MQSingle.retryBackoffMs(100, config), "大次数移位钳制不溢出");
	}
}
