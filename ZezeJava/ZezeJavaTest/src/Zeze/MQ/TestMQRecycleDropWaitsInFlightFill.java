package Zeze.MQ;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * mq-02 回归（模式D1）的回收层：段回收的 drop 必须等在飞 fill 归零（原"终检-放回"语义
 * 并入终结原语，不设两套）。
 * <p>
 * 演进：FND21 GB-C03 以 indexes.remove 后终检 activeFills、非零放回本轮放弃来闭合
 * "复查读0→fill increment→floorEntry 命中本段"的 TOCTOU。FND29 mq-02 把回收与删除路径
 * 收口到 RocksDatabase.destroyColumnFamily 单一终结原语（置毁标记→等在飞归零→drop）：
 * remove 后的逃逸 fill 由原语双检序承接——其 increment 严格先于租约检查点，排空必见并
 * 等其退出；放回不再需要。
 * <p>
 * 判别（直驱 recycleSegment 语义）：①在飞非零时回收线程阻塞等待（段文件/列族俱在——
 * baseline 终检-放回立即返回，线程即刻结束，判红）；归零后 drop 完成且下轮全路径回收
 * 收敛。②既有入口闸不回归（双绿守卫）：在飞非零时 tryRecycle 入口整体跳过。
 * <p>
 * 注：recycleSegment 反射直驱；trunkFileSize/makeIndexPeriod 静态字段小值快滚、finally
 * 恢复（TestGBD02SegmentRecycle 先例）。
 */
@Fast
public class TestMQRecycleDropWaitsInFlightFill {

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

	/** 反射直驱 recycleSegment（回收临界区本体，绕过入口闸与软删除窗口）。 */
	private static void invokeRecycleSegment(MQFileWithIndex file, long base) throws Exception {
		Method m = MQFileWithIndex.class.getDeclaredMethod("recycleSegment", long.class);
		m.setAccessible(true);
		m.invoke(file, base);
	}

	/**
	 * ①原语排空与收敛：在飞非零时 drop 等待（段文件/列族不得消失）；归零后回收完成，
	 * 下轮全路径 tryRecycle 继续收敛。baseline（终检-放回）：直驱立即返回，判红于"线程存活"。
	 */
	@Test
	public void testRecycleDropWaitsInFlightFill(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var topicDir = Path.of(home, "topic");
		try {
			try (var database = new RocksDatabase(home)) {
				var file = new MQFileWithIndex(home, database, "topic", 0);
				file.trunkFileSize = 1024;
				file.makeIndexPeriod = 1;
				try {
					for (long id = 0; id < 200; ++id)
						file.appendMessage(Fnd19MqTestSupport.messageOf(id));
					var bases = segmentBases(topicDir);
					Assertions.assertTrue(bases.size() >= 3, "多段前提不成立，实际段数=" + bases.size());

					// 水位推过前两段段尾（bases[1]、bases[2]）：前两段已全部确认，可整段回收。
					while (file.getFirstMessageId() < bases.get(2))
						file.increaseFirstMessageId();

					// 模拟"复查读0之后、floorEntry 之前"插入的在飞 fill（fillMessage 首行
					// incrementAndGet 的落点）：直驱 recycleSegment 观察原语排空。
					var activeFills = activeFillsOf(file);
					activeFills.incrementAndGet();
					var failure = new AtomicReference<Throwable>();
					var recycler = new Thread(() -> {
						try {
							invokeRecycleSegment(file, bases.get(0));
						} catch (Throwable e) {
							failure.set(e);
						}
					}, "mq02-recycleSegment");
					recycler.start();

					Thread.sleep(500);
					Assertions.assertTrue(recycler.isAlive(),
							"在飞 fill 未归零时段回收的 drop 必须等待（mq-02：drop 与在飞迭代器并发"
									+ "是 native use-after-free）");
					Assertions.assertEquals(bases, segmentBases(topicDir),
							"等待期间段文件不得被删（drop 尚未执行：在飞迭代器仍持有句柄；"
									+ "表已除名是原语①段线性化点的既定形态，非已 drop）");

					activeFills.decrementAndGet(); // 在飞 fill 退出
					recycler.join(30_000);
					Assertions.assertFalse(recycler.isAlive(), "归零后回收必须完成（等待有限收敛）");
					Assertions.assertNull(failure.get());

					// 收敛：下轮全路径 tryRecycle 完成剩余可回收段。
					file.tryRecycle(0);
					Assertions.assertEquals(bases.subList(2, bases.size()), segmentBases(topicDir),
							"归零后下轮正常回收（原语排空不是回收禁用，收敛有保障）");
				} finally {
					file.close();
				}
			}
		} finally {
		}
	}

	/**
	 * ②既有入口闸不回归（双绿守卫）：在飞计数非零时 tryRecycle 入口整体跳过——
	 * 原语排空是入口/锁内双检之后的第三道防线，不得替代前两道（非零时不应走到 remove，
	 * 回收是低频周期任务，不值得持锁等 fill 批次）。
	 */
	@Test
	public void testEntryActiveFillStillSkipsRecycle(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db2").toString();
		var topicDir = Path.of(home, "topic");
		try {
			try (var database = new RocksDatabase(home)) {
				var file = new MQFileWithIndex(home, database, "topic", 0);
				file.trunkFileSize = 1024;
				file.makeIndexPeriod = 1;
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
		}
	}
}
