package Zeze.MQ;

import harness.Extra;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * mq-02 回归（模式D1：单一终结原语+迭代器租约）的第一层：RocksDatabase.destroyColumnFamily
 * 的三段协议（置毁标记→等在飞归零→drop）与 fillMessage 的批量租约检查点。
 * <p>
 * 修复前：dropTable 无任何迭代器/在飞排空（登记只服务 close()），删除路径与逃逸 fill 的
 * 段索引迭代器并发是 native use-after-free——Manager 进程崩溃。
 * <p>
 * 判别：①在飞非零时销毁必须阻塞（线程存活）且表已除名+置毁标记（线性化点已过）；归零后
 * drop 真正完成（重开库按 listColumnFamilies 验证列族已不存在）。②段索引表置毁标记后
 * fillMessage 必须响亮放弃（抛错走失败-复位-重试路径），不得获取迭代器；标记撤销后装载
 * 照常（检查点不误伤）。
 * <p>
 * 红判据：destroyColumnFamily/InFlight/isDestroyPending 为新 API（baseline 编译红）；
 * Table.destroyPending 反射置位在 baseline 为 NoSuchField（判红）。native UAF 本身无法在
 * JVM 内注入断言（SIGSEGV 不可捕获），以"drop 严格后于在飞归零"的线性化时序为可测代理。
 */
@Fast
@Extra
public class TestMQDestroyColumnFamilyWaitsInFlightFill {

	/** fillMessage 外层 catch(Exception) 会包一层 RuntimeException，取最深层消息做断言。 */
	private static String deepestMessage(Throwable e) {
		var msg = String.valueOf(e.getMessage());
		for (var cause = e.getCause(); cause != null; cause = cause.getCause())
			if (cause.getMessage() != null)
				msg = cause.getMessage();
		return msg;
	}

	/** 反射置 Table.destroyPending（baseline 无此字段判红：终结原语不存在）。 */
	private static void setDestroyPending(RocksDatabase.Table table, boolean value) throws Exception {
		try {
			Field f = RocksDatabase.Table.class.getDeclaredField("destroyPending");
			f.setAccessible(true);
			f.setBoolean(table, value);
		} catch (NoSuchFieldException e) {
			throw new AssertionError("Table.destroyPending 缺失（mq-02 终结原语的租约标记不存在）", e);
		}
	}

	/**
	 * ①终结原语三段时序：在飞非零→阻塞等待（表已除名、标记已置）；归零→drop 完成。
	 * drop 的真实性以重开库验证（tableMap 按列族清单装载，列族已 drop 即缺席）。
	 */
	@Test
	public void testDestroyWaitsForInFlightThenDrops(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		try {
			var table = database.getOrAddTable("cf");
			var key = "k".getBytes();
			table.put(key, "v".getBytes());
			var inFlight = new AtomicInteger(1);

			var failure = new AtomicInteger();
			var destroyer = new Thread(() -> {
				try {
					database.destroyColumnFamily("cf", inFlight::get);
				} catch (Throwable e) {
					failure.incrementAndGet();
				}
			}, "mq02-destroyColumnFamily");
			destroyer.start();

			Thread.sleep(500);
			Assertions.assertTrue(destroyer.isAlive(),
					"在飞未归零时销毁必须等待（mq-02：dropTable 无排空与逃逸 fill 迭代器并发是"
							+ " native use-after-free）");
			Assertions.assertTrue(table.isDestroyPending(), "等待期间毁标记已置（租约检查点依据）");
			Assertions.assertNull(database.getTable("cf"), "线性化点已过：表已除名");

			inFlight.set(0); // 在飞归零（fill 退出）
			destroyer.join(30_000);
			Assertions.assertFalse(destroyer.isAlive(), "归零后销毁必须完成（等待有限收敛）");
			Assertions.assertEquals(0, failure.get());
		} finally {
			database.close();
		}
		try (var reopened = new RocksDatabase(home)) {
			Assertions.assertNull(reopened.getTable("cf"), "归零后列族真正被 drop（重开库列族清单缺席）");
		}
	}

	/**
	 * ②fillMessage 租约检查点：段索引表置毁标记后必须放弃本轮（响亮抛错、不装载），
	 * 标记撤销后装载照常（检查点不误伤正常路径）。
	 */
	@Test
	public void testFillAbortsOnDestroyPendingSegment(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var home = tempDir.resolve("db2").toString();
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		try {
			file.appendMessage(MqTestSupport.messageOf(0));
			file.appendMessage(MqTestSupport.messageOf(1));
			var indexTable = database.getTable("topic.0.0");
			Assertions.assertNotNull(indexTable);

			setDestroyPending(indexTable, true); // 终结原语①段已过的形态
			Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
			var e = Assertions.assertThrows(RuntimeException.class, () -> file.fillMessage(queue, 0, 1),
					"毁标记已置的段上 fill 必须放弃（不得获取该列族迭代器）");
			Assertions.assertTrue(deepestMessage(e).contains("destroy pending"),
					"message=" + deepestMessage(e));
			Assertions.assertTrue(queue.isEmpty(), "放弃路径不得装载任何消息");

			setDestroyPending(indexTable, false); // 标记撤销（销毁回滚/误置形态）
			file.fillMessage(queue, 0, 2);
			Assertions.assertEquals(2, queue.size(), "标记撤销后装载照常（检查点不误伤）");
		} finally {
			database.close();
			file.close();
		}
	}
}
