package Zeze.MQ;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND21 GB-C03 回归：tryRecycle 的 activeFills"锁内双检"与 recycleSegment 的 indexes.remove
 * 之间无原子性——"复查读0 → fill increment → floorEntry 命中本段"的插入使 fill 的迭代器生命周期
 * 横跨 dropTable（计数只提供观察不提供互斥；native use-after-free，正是 FND19 GB-D02 守卫注释
 * 自认要防的形态，从"入口漏检"换型为"复查后插入"的 TOCTOU）。
 * <p>
 * 修复形态：recycleSegment 在 indexes.remove 之后、dropTable 之前终检一次 activeFills，非零
 * 放回本轮放弃（hb 论证：fill 的 increment 严格先于其 floorEntry（程序序），floor 命中本段 ⟹
 * 该 map 读先于 remove 的线性化 ⟹ remove 后的终检必见该 increment；终检读到 0 则此后进入的
 * fill 只能定位后继段——无害）。真删除留待归零后的下一轮，收敛有保障。
 * <p>
 * 判别（案卷自认 TOCTOU 窗口窄、"构造性复现需压力注入"——本测试直驱终检语义本身）：
 * ① 计数非零（=复查读0之后插入的在飞 fill 落点）直驱 recycleSegment：修复代码放回+不 drop
 * （返回 false、文件/列族俱在），旧代码 remove 后直奔 dropTable（判红：返回 null 且文件已删）；
 * ② 归零后全路径 tryRecycle 正常回收（收敛性）；③ 既有入口闸不回归（双绿守卫）。
 * <p>
 * 注：recycleSegment 反射直驱（orig 车道 void 返回 null，由断言判红）；trunkFileSize/
 * makeIndexPeriod 静态字段小值快滚、finally 恢复（TestGBD02SegmentRecycle 先例）。
 */
@Fast
@ResourceLock("mq-file-statics") // MQFileWithIndex静态字段(trunkFileSize/makeIndexPeriod)操纵的测试类互斥（FND22门禁插曲：并行改写使滚段点漂移注入失灵）
public class TestGBC03RecycleFinalCheckPutback {

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

	private static Field fieldOf(MQFileWithIndex file, String name) throws Exception {
		var f = MQFileWithIndex.class.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	private static AtomicInteger activeFillsOf(MQFileWithIndex file) throws Exception {
		return (AtomicInteger)fieldOf(file, "activeFills").get(file);
	}

	/** 反射直驱 recycleSegment：修复代码返回 Boolean，orig（void）返回 null。 */
	private static Object invokeRecycleSegment(MQFileWithIndex file, long base) throws Exception {
		Method m = MQFileWithIndex.class.getDeclaredMethod("recycleSegment", long.class);
		m.setAccessible(true);
		return m.invoke(file, base);
	}

	/**
	 * ①+② 终检-放回与收敛：计数非零时 remove 后必须放回不删（旧代码判红：段文件已删+返回
	 * null）；计数归零后的下一轮全路径回收完成（软删除窗口观察起点保持，单轮收敛）。
	 */
	@Test
	public void testPostRemoveActiveFillAbortsAndConverges(@TempDir Path tempDir) throws Exception {
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

					// 水位推过前两段段尾（bases[1]、bases[2]）：前两段已全部确认，可整段回收。
					while (file.getFirstMessageId() < bases.get(2))
						file.increaseFirstMessageId();

					// 模拟"复查读0之后、floorEntry 之前"插入的在飞 fill（fillMessage 首行
					// incrementAndGet 的落点）：直驱 recycleSegment 观察 remove 之后的终检。
					var activeFills = activeFillsOf(file);
					activeFills.incrementAndGet();
					var result = invokeRecycleSegment(file, bases.get(0));

					Assertions.assertEquals(Boolean.FALSE, result,
							"remove 后终检非零必须放回并放弃本轮（FND21 GB-C03：旧代码 remove 后直奔"
									+ " dropTable——在飞 fill 的迭代器正持该句柄，native use-after-free）");
					Assertions.assertEquals(bases, segmentBases(topicDir), "段文件不得被删（放回）");
					Assertions.assertTrue(database.getTableMap().containsKey("topic.0." + bases.get(0)),
							"索引列族不得被 drop（放回=段在 indexes 中复原，fill 可继续定位）");

					// ② 收敛：计数归零后的下一轮全路径 tryRecycle 完成回收（候选窗口单轮收敛）。
					activeFills.decrementAndGet();
					file.tryRecycle(0);
					Assertions.assertEquals(bases.subList(2, bases.size()), segmentBases(topicDir),
							"归零后下轮正常回收（终检-放回不是回收禁用，收敛有保障）");
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
	 * ③ 既有入口闸不回归（双绿守卫）：在飞计数非零时 tryRecycle 入口整体跳过——终检-放回是
	 * 入口/锁内双检之后的第三道防线，不得替代前两道（非零时不应走到 remove）。
	 */
	@Test
	public void testEntryActiveFillStillSkipsRecycle(@TempDir Path tempDir) throws Exception {
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

					var activeFills = activeFillsOf(file);
					activeFills.incrementAndGet();
					file.tryRecycle(0);
					Assertions.assertEquals(bases, segmentBases(topicDir),
							"入口闸仍在：在飞 fill 非零时本轮整体跳过（FND19 GB-D02 语义不回归）");
					activeFills.decrementAndGet();
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
