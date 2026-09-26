package Zeze.MQ;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND19 GB-D02 回归：水位线整段回收（拍板方案A：firstMessageId 越过段尾才整段回收）。
 * <p>
 * 修复前：队列语义的"消费完成"只推进 meta 位点，被消费数据的物理回收路径完全缺失——
 * 段文件与索引列族只增不减，磁盘占用=历史消息总量（无上界）。
 * <p>
 * 直测 MQFileWithIndex.tryRecycle（loadMonitorTimer 周期触发是同一入口的挂载点，见
 * MQManager.loadMonitor）：多段+推位点到段中间不回收；推过段尾后已确认段消失（文件+索引
 * 出indexes）、未确认段与活跃末段保留；回收后 fill/append 不受影响；重启构造只面对活跃段。
 * <p>
 * 注：trunkFileSize/makeIndexPeriod 静态字段小值快滚、finally 恢复（TestFileWithIndexed 先例，
 * 并行测试对该字段的既有容忍口径见 TestMQFileWithIndexTornTail 注释；布局约定见 Fnd19MqTestSupport）。
 */
@Fast
public class TestFnd19GBD02SegmentRecycle {

	/** topic 目录下按"分区号.段基"命名的段基列表（升序）。 */
	private static java.util.List<Long> segmentBases(Path topicDir) throws Exception {
		try (var stream = Files.list(topicDir)) {
			return stream.map(p -> p.getFileName().toString())
					.filter(n -> n.matches("\\d+\\.\\d+"))
					.map(n -> Long.parseLong(n.split("\\.")[1]))
					.sorted()
					.collect(Collectors.toList());
		}
	}

	private static void assertFillInOrder(MQFileWithIndex file, long begin, long end) {
		Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
		file.fillMessage(queue, begin, end);
		Assertions.assertEquals(end - begin, queue.size(), "回填数量");
		var expect = begin;
		for (var message : queue)
			Assertions.assertEquals(expect++, message.getTimestamp(), "回填必须按 id 有序");
	}

	@Test
	public void testWatermarkSegmentRecycle(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var topicDir = Path.of(home, "topic");
		var oldTrunkFileSize = MQFileWithIndex.trunkFileSize;
		var oldMakeIndexPeriod = MQFileWithIndex.makeIndexPeriod;
		MQFileWithIndex.trunkFileSize = 1024; // 小段快滚
		MQFileWithIndex.makeIndexPeriod = 1;  // 每条建索引，保证fill定位
		try {
		java.util.List<Long> bases = null;
		try (var database = new RocksDatabase(home)) {
			var file = new MQFileWithIndex(home, database, "topic", 0);
			try {
				for (long id = 0; id < 200; ++id)
					file.appendMessage(Fnd19MqTestSupport.messageOf(id));
				bases = segmentBases(topicDir);
				Assertions.assertTrue(bases.size() >= 3, "多段前提不成立，实际段数=" + bases.size());
				Assertions.assertEquals(0L, bases.get(0), "首段基为0");

				// 水位推到第一段中间（未越过段尾 bases[1]）：不得回收。
				while (file.getFirstMessageId() < bases.get(1) - 3)
					file.increaseFirstMessageId();
				file.tryRecycle(0);
				Assertions.assertEquals(bases, segmentBases(topicDir), "水位在段中间不得整段回收（at-least-once 契约）");

				// 水位推过前两段段尾（bases[1]、bases[2]）：前两段已全部确认，整段回收。
				while (file.getFirstMessageId() < bases.get(2))
					file.increaseFirstMessageId();
				file.tryRecycle(0); // delay=0：窗口立即到期（loadMonitorTimer 默认60s窗口是同一逻辑）
				Assertions.assertEquals(bases.subList(2, bases.size()), segmentBases(topicDir),
						"已确认段（文件）消失，未确认段与活跃末段保留");

				// 回收后 fill/append 不受影响（indexes 移除后 floorEntry 定位后继段）。
				assertFillInOrder(file, file.getFirstMessageId(), file.getNextMessageId());
				file.appendMessage(Fnd19MqTestSupport.messageOf(200));
				Assertions.assertEquals(201, file.getNextMessageId());
			} finally {
				file.close();
			}
		}

		// 重启形态：目录只剩活跃段（构造时间与服役时长解耦是 GB-D02 的目标形态之一），
		// 位点保持、fill 照常（firstMessageId >= 最老存活段基，构造期不变量不被回收破坏）。
			try (var database2 = new RocksDatabase(home)) {
				var file2 = new MQFileWithIndex(home, database2, "topic", 0);
				try {
					Assertions.assertEquals(bases.get(2), file2.getFirstMessageId(), "位点跨重启保持");
					Assertions.assertEquals(201, file2.getNextMessageId());
					assertFillInOrder(file2, file2.getFirstMessageId(), file2.getNextMessageId());
				} finally {
					file2.close();
				}
			}
		} finally {
			// 静态字段恢复（注释宣称的先例形态，FND20 R2 补齐）：类级并行下残留 makeIndexPeriod=1
			// 会改写 TestMQFileWithIndexTornTail 撕裂恢复的锚点选择（其注释记录的干扰面）。
			MQFileWithIndex.trunkFileSize = oldTrunkFileSize;
			MQFileWithIndex.makeIndexPeriod = oldMakeIndexPeriod;
		}
	}

	/**
	 * 在飞计数与读路径同生共死（异常路径也必须归零）：fillMessage 异常退出后计数若未归零
	 * （前一轮实现的半成品形态），tryRecycle 将从此永久跳过——回收静默失效。用"失败的fill
	 * 之后回收仍进行"钉死 finally 归零语义。
	 */
	@Test
	public void testFillFailureMustNotLeakActiveCount(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db2").toString();
		var topicDir = Path.of(home, "topic");
		var oldTrunkFileSize = MQFileWithIndex.trunkFileSize;
		var oldMakeIndexPeriod = MQFileWithIndex.makeIndexPeriod;
		MQFileWithIndex.trunkFileSize = 1024;
		MQFileWithIndex.makeIndexPeriod = 1;
		try {
		try (var database = new RocksDatabase(home)) {
			var file = new MQFileWithIndex(home, database, "topic", 0);
			try {
				for (long id = 0; id < 200; ++id)
					file.appendMessage(Fnd19MqTestSupport.messageOf(id));
				var bases = segmentBases(topicDir);
				Assertions.assertTrue(bases.size() >= 3);

				// 越界fill必失败（headMessageId 超过 nextMessageId，定位越过文件尾）。
				var queue = new ConcurrentLinkedQueue<BMessage.Data>();
				Assertions.assertThrows(RuntimeException.class, () -> file.fillMessage(queue, 500, 600),
						"前置：越界fill按契约抛出");

				while (file.getFirstMessageId() < bases.get(2))
					file.increaseFirstMessageId();
				file.tryRecycle(0); // 若异常路径泄漏在飞计数，此处被入口检查永久跳过
				Assertions.assertEquals(bases.subList(2, bases.size()), segmentBases(topicDir),
						"失败的fill不得禁用后续回收（计数已在finally归零）");
			} finally {
				file.close();
			}
		}
		} finally {
			MQFileWithIndex.trunkFileSize = oldTrunkFileSize;
			MQFileWithIndex.makeIndexPeriod = oldMakeIndexPeriod;
		}
	}
}
