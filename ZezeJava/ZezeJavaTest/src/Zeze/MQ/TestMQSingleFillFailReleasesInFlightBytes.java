package Zeze.MQ;

import harness.Extra;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Net.Binary;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * MQSingle 运行期装载失败路径的记账归还回归：fillMessage 装载环中"预算准入已入账、
 * 消息尚未入队"的窗口内抛错（记录体越过文件尾 read message body eof / readFully
 * IO 错 / decode 失败）时，该条消息的字节权重已计入分区 queueBytes 与 Manager 级
 * 共享 totalInFlightBytes，但消息没有进入内存队列——ack/死信出账只处理"在队"形态、
 * close() 终态释放只处理"实例销毁"形态，运行期实例存续期间无任何出账方。
 * 失败自排期链（scheduleFillRetry）每轮重试都重新走 admitFillBytes（队列空时
 * 无条件放行并入账），每次重试再泄漏一条的字节权重，按退避周期持续累积，
 * 直至分区删除或进程重启；跨分区共享的全局在飞预算被单一损坏分区耗干。
 * <p>
 * 与 TestMQSingleCtorFailReleasesInFlightBytes（构造失败=实例销毁的归还）互补：
 * 本用例钉住"实例存续、反复重试"的运行期形态——注入点是装载环内部的真实失败
 * （段文件第 1 条积压记录的 size 字段运行期损坏为超大值），不是 super.fillMessage
 * 之前的注入。注：文件放 src/MQ/ 但声明 package Zeze.MQ——需要 MQSingle 的包内
 * 测试缝（pullMessage、fillRetryScheduler、queueBytes）。
 */
@Fast
@Extra
public class TestMQSingleFillFailReleasesInFlightBytes {

	private static BMessage.Data messageOf(long id) {
		var message = new BMessage.Data();
		message.setTimestamp(id);
		message.setBody(new Binary(new byte[1024]));
		return message;
	}

	@Test
	public void testMidRecordCorruptionKeepsTotalInFlightBudgetAcrossRetries(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var manager = new MQManager(home, new Zeze.Config());
		try {
			// 建分区并造 3 条盘上积压（appendMessage 直接落盘，队列保持空=全部在盘待装载）。
			manager.createPartition("topic", new HashSet<>(List.of(0)));
			var single = manager.getQueueForTest("topic").get(0);
			var file = single.getFileForTest();
			for (long id = 0; id < 3; ++id)
				file.appendMessage(messageOf(id));

			// 运行期体部损坏（recoverTornTail 只在构造期愈合，此形态要求运行期发生）：
			// 段文件 0.0 首条记录（偏移0：8字节id + 4字节size，小端，WriteInt4/ReadInt4同序）
			// 的 size 改为超大值——装载环记录头解析成功（id==headMessageId），读体路径
			// 抛 "read message body eof"，正落在"admit已入账、add未执行"的窗口内。
			try (var raf = new RandomAccessFile(java.nio.file.Path.of(home, "topic", "0.0").toFile(), "rw")) {
				raf.seek(8);
				raf.write(new byte[]{0x00, 0x00, 0x10, 0x00}); // 小端 1MB
			}

			// 捕获式退避调度器：不真正排期（测试同步驱动重试轮次，也不在收尾后留下
			// 触碰已关 rocksdb 的后台重试链）。
			single.fillRetryScheduler = (delayMs, action) -> CompletableFuture.completedFuture(null);

			// 绕开内存队列直接落盘不推进 highLoad（生产路径由 sendMessage 的 highLoad++
			// 驱动回填），置为真实积压数后同步驱动装载；后续每轮失败由 catch 的
			// recomputeHighLoad 按盘上真相自行维持。
			var highLoad = single.getClass().getDeclaredField("highLoad");
			highLoad.setAccessible(true);
			highLoad.setLong(single, 3L);

			// 多轮重试形态：每次装载都在同一条损坏记录上失败（队列空恒放行队头）。
			for (var round = 1; round <= 3; ++round) {
				var ex = Assertions.assertThrows(RuntimeException.class, single::pullMessage,
						"第" + round + "轮装载必须响亮失败（损坏记录不得静默跳过）");
				Assertions.assertTrue(ex.getCause() != null && ex.getCause().getMessage().contains("read message body eof")
								|| ex.getMessage().contains("read message body eof"),
						"必须命中读体越界失败点: " + ex);
				// 已入账未入队的字节必须归还：全局在飞预算不得被失败装载虚占，
				// 退避重试每轮也不得再泄漏（bug形态：每轮+1MB直至耗干全局预算）。
				Assertions.assertEquals(0L, single.queueBytes(),
						"第" + round + "轮装载失败后分区queueBytes必须归零（无在队消息即无在飞记账）");
				Assertions.assertEquals(0L, manager.totalInFlightBytes.get(),
						"第" + round + "轮装载失败后全局在飞字节必须归零（重试不得累积泄漏）");
			}
			// 收尾对齐生产路径：分区 close 后全局预算仍为 0（无残留可释放）。
			manager.getQueueForTest("topic").removePartition(0);
			Assertions.assertEquals(0L, manager.totalInFlightBytes.get());
		} finally {
			manager.stop();
		}
	}
}
