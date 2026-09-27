package Zeze.MQ;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.List;
import Zeze.Config;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND22 GB-C01 回归：tryRecycle 的回收通路三闸（stopped/closed/managementLock）整体缺失——
 * loadMonitorTimer（120s 周期）驱动 loadMonitor → tryRecycleSegments → tryRecycle 全链无闸，
 * 与分区删除路径（removePartition→close→deletePartitionStorage 的 dropTable 从 tableMap 除名并
 * destroyColumnFamilyHandle 毁 meta/index 句柄）相交：MQSingle.close 只关文件流不触碰 tryRecycle，
 * 定时器线程在迭代中已取得的引用不因摘除失效——close 之后 tryRecycle 照常进入，
 * metaConsistent 的无锁 meta.get（Table.get 是无锁 native 调用）对已毁句柄进 JNI，
 * 正是 RocksDatabase.close/dropTable 契约明示的 native use-after-free 形态。
 * <p>
 * 修复形态（案卷处置建议）：MQFileWithIndex 加 closed 标志，close() 在自身锁内置位（流关闭一并
 * 移入锁内，锁序 MQSingle→fileWithIndex 既有方向不变），tryRecycle 入口锁内复查即返回；正确性：
 * deleteStorage 严格在 close() 返回之后执行——tryRecycle 要么在置位前完整跑完（close 阻塞于
 * fileWithIndex 锁等其退出临界区），要么在置位后被标志拒绝。belt-and-braces：MQSingle.
 * tryRecycleSegments 顺带查 managerStopped()（timer 停机方向）。
 * <p>
 * 判别：
 * ① close 后 tryRecycle 不得再动分区存储（段文件/索引列族俱在）——旧代码照常回收（判红）；
 * ② 正控（双绿守卫）：活分区的回收不被 closed 闸禁用（水位越过段尾即整段回收）；
 * ③ 停机方向（MQSingle 闸）：stopped 置位后 tryRecycleSegments 整体静默——旧代码透传回收（判红）。
 * <p>
 * 注：trunkFileSize/makeIndexPeriod 静态字段小值快滚、finally 恢复（TestFnd19GBD02SegmentRecycle
 * 先例，08bd9cbe8 教训）。
 */
@Fast
public class TestFnd22GBC01RecycleClosedGate {

	/** topic 目录下按"分区号.段基"命名的段基列表（升序）。 */
	private static List<Long> segmentBases(Path topicDir) throws Exception {
		try (var stream = Files.list(topicDir)) {
			return stream.map(p -> p.getFileName().toString())
					.filter(n -> n.matches("\\d+\\.\\d+"))
					.map(n -> Long.parseLong(n.split("\\.")[1]))
					.sorted()
					.collect(Collectors.toList());
		}
	}

	private static void setStopped(MQManager manager, boolean value) throws Exception {
		var f = MQManager.class.getDeclaredField("stopped");
		f.setAccessible(true);
		f.setBoolean(manager, value);
	}

	/**
	 * ① 核心闸：close（模拟 removePartition→close 完成，deletePartitionStorage 即将/已经毁句柄）
	 * 之后回收定时器的一轮 tryRecycle 必须被 closed 拒绝——不得再触碰该分区的任何存储。
	 * 旧代码无闸直入：metaConsistent 通过（库未关）→ recycleSegment 照常 dropTable+删文件。
	 */
	@Test
	public void testClosedFileRejectsRecycle(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var topicDir = Path.of(home, "topic");
		var oldTrunkFileSize = MQFileWithIndex.trunkFileSize;
		var oldMakeIndexPeriod = MQFileWithIndex.makeIndexPeriod;
		MQFileWithIndex.trunkFileSize = 1024; // 小段快滚
		MQFileWithIndex.makeIndexPeriod = 1;  // 每条建索引，保证fill定位
		try {
			try (var database = new RocksDatabase(home)) {
				var file = new MQFileWithIndex(home, database, "topic", 0);
				try {
					for (long id = 0; id < 200; ++id)
						file.appendMessage(Fnd19MqTestSupport.messageOf(id));
					var bases = segmentBases(topicDir);
					Assertions.assertTrue(bases.size() >= 3, "多段前提不成立，实际段数=" + bases.size());

					// 水位推过前两段段尾：前两段已全部确认，是可整段回收的稳态。
					while (file.getFirstMessageId() < bases.get(2))
						file.increaseFirstMessageId();

					// 分区删除路径先行：removePartition→close 完成（closed 在 fileWithIndex 锁内置位）。
					file.close();

					// 回收定时器的一轮（loadMonitor 的滞后/并发轮次对已关闭分区的触达形态）。
					file.tryRecycle(0);

					Assertions.assertEquals(bases, segmentBases(topicDir),
							"close 后回收不得再删段文件（FND22 GB-C01：closed 闸拒绝对已关闭分区的回收）");
					Assertions.assertTrue(database.getTableMap().containsKey("topic.0." + bases.get(0)),
							"close 后回收不得再 drop 索引列族（句柄可能已被 deletePartitionStorage 销毁——"
									+ "无锁 meta.get/dropTable 对已毁句柄的 native 调用正是本案的崩溃面）");
					Assertions.assertTrue(database.getTableMap().containsKey("topic.0"),
							"meta 列族注册不得被波及");
				} finally {
					file.close();
				}
			}
		} finally {
			// 静态字段恢复（类级并行下残留值改写他测锚点，08bd9cbe8 教训）
			MQFileWithIndex.trunkFileSize = oldTrunkFileSize;
			MQFileWithIndex.makeIndexPeriod = oldMakeIndexPeriod;
		}
	}

	/**
	 * ② 正控（双绿守卫）：分区未 close 时同条件正常整段回收——closed 闸只拦"已关闭"，
	 * 不是回收禁用（水位越过段尾的两个老段全部回收，收敛性不回归）。
	 */
	@Test
	public void testLiveFileStillRecycles(@TempDir Path tempDir) throws Exception {
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

					while (file.getFirstMessageId() < bases.get(2))
						file.increaseFirstMessageId();

					file.tryRecycle(0);
					Assertions.assertEquals(bases.subList(2, bases.size()), segmentBases(topicDir),
							"活分区回收不回归：水位越过的老段照常整段回收");
				} finally {
					file.close();
				}
			}
		} finally {
			MQFileWithIndex.trunkFileSize = oldTrunkFileSize;
			MQFileWithIndex.makeIndexPeriod = oldMakeIndexPeriod;
		}
	}

	/**
	 * ③ 停机方向（belt-and-braces 闸，真 Manager）：stop 最前置位 stopped 后，晚到的回收轮次
	 * 在 MQSingle.tryRecycleSegments 入口静默——旧代码透传 fileWithIndex.tryRecycle 照常回收
	 *（判红：段文件已删）。主闸（fileWithIndex.closed）由 stop 的 queue.close→MQSingle.close
	 * 置位，本闸兜 timer 有界 stop 超预算逃逸的早到分支。
	 */
	@Test
	public void testStoppedManagerSkipsPartitionRecycle(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		var oldTrunkFileSize = MQFileWithIndex.trunkFileSize;
		var oldMakeIndexPeriod = MQFileWithIndex.makeIndexPeriod;
		MQFileWithIndex.trunkFileSize = 1024;
		MQFileWithIndex.makeIndexPeriod = 1;
		try {
			manager.createPartition("t", new HashSet<>(List.of(0)));
			var single = manager.getQueueForTest("t").get(0);
			for (long id = 0; id < 200; ++id)
				single.sendMessage(Fnd19MqTestSupport.sendMessageOf(id)); // 直入装载，无消费者
			var file = single.getFileForTest();
			var bases = segmentBases(Path.of(manager.getHome(), "t"));
			Assertions.assertTrue(bases.size() >= 3, "多段前提不成立，实际段数=" + bases.size());

			while (file.getFirstMessageId() < bases.get(2))
				file.increaseFirstMessageId();

			setStopped(manager, true); // stop() 的最前置位（timer 晚到轮次看到的时序输入）
			single.tryRecycleSegments(0); // loadMonitorTimer 周期驱动的透传入口

			Assertions.assertEquals(bases, segmentBases(Path.of(manager.getHome(), "t")),
					"停机后回收扫描必须静默（FND22 GB-C01：rocksDatabase.close 在途/已完成，"
							+ "回收触库通路不得再进入）");
		} finally {
			MQFileWithIndex.trunkFileSize = oldTrunkFileSize;
			MQFileWithIndex.makeIndexPeriod = oldMakeIndexPeriod;
			manager.stop();
		}
	}
}
