package Zeze.MQ;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Config;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * mq-02 回归（模式D1）的删除链层：deletePartition 的 drop 必须等在飞 fill 归零。
 * <p>
 * 修复前：MQSingle.close 的排空是有界等待（超预算仅告警放行），其后的
 * deletePartitionStorage.dropTable 无任何迭代器排空——close 排空一旦超预算，逃逸 fill
 * 的段索引迭代器与 destroyColumnFamilyHandle 并发是 native use-after-free（Manager 进程
 * 原生崩溃，该进程全部 topic 服务中断）。
 * <p>
 * 修复后：deletePartition 把被删分区的在飞 fill 计数（activeFills）随存储删除传入
 * RocksDatabase.destroyColumnFamily（单一终结原语），drop 前等待归零——close 排空超预算
 * 不再裸奔。
 * <p>
 * 判别（direct-construct 形态，未 start 的 MQManager 无网络直驱，TestGBD01ManagerDelete
 * 先例）：模拟 close 排空超预算逃逸的在飞 fill（activeFills 计数非零——fillMessage 已进入、
 * 迭代器在手的落点），删除线程在归零前必须仍阻塞（baseline：dropTable 立即执行，线程即刻
 * 返回，判红）；归零后删除完成且存储全清。native UAF 本身不可注入（SIGSEGV 不可捕获），
 * 以"drop 严格后于在飞归零"的线程时序为可测代理。
 */
@ResourceLock("mq-file-statics") // 旁观者READ：与改写trunkFileSize/makeIndexPeriod的类互斥——静态被并行改小期间本类append会滚出无索引段，fillMessage seekForPrev落空即messageIndexNotFound假红（2026-10-04 test40批r5实证）；旁观者彼此READ可并行
@Fast
public class TestMQDeletePartitionWaitsEscapedFill {

	private static AtomicInteger activeFillsOf(MQFileWithIndex file) throws Exception {
		Field f = MQFileWithIndex.class.getDeclaredField("activeFills");
		f.setAccessible(true);
		return (AtomicInteger)f.get(file);
	}

	@Test
	public void testDeleteWaitsEscapedFillDrain(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var home = tempDir.resolve("manager").toString();
		var manager = new MQManager(home, new Config());
		try {
			manager.createPartition("t", new HashSet<>(java.util.List.of(0)));
			var single = manager.getQueueForTest("t").get(0);
			single.sendMessage(Fnd19MqTestSupport.sendMessageOf(1)); // 建段文件/索引/meta
			Assertions.assertNotNull(manager.getRocksDatabase().getTable("t.0.0"), "索引列族存在（前置）");

			// 模拟 close 排空超预算逃逸的在飞 fill：计数非零（fillMessage 首行 increment 的落点），
			// messageFillFuture 已不可见（世代自清/超时告警放行的形态）。
			var activeFills = activeFillsOf(single.getFileForTest());
			activeFills.incrementAndGet();

			var failure = new AtomicReference<Throwable>();
			var deleter = new Thread(() -> {
				try {
					manager.deletePartition("t", new HashSet<>(java.util.List.of(0)));
				} catch (Throwable e) {
					failure.set(e);
				}
			}, "mq02-deletePartition");
			deleter.start();

			Thread.sleep(500);
			Assertions.assertTrue(deleter.isAlive(),
					"在飞 fill 未归零时删除的 drop 必须等待（mq-02：baseline dropTable 无排空，"
							+ "与逃逸 fill 迭代器并发是 native use-after-free）");
			Assertions.assertTrue(Path.of(home, "t", "0.0").toFile().exists(),
					"等待期间段文件不得被删（drop 尚未执行：在飞迭代器仍持有句柄）");

			activeFills.decrementAndGet(); // 逃逸 fill 退出（装载完成/租约检查点放弃）
			deleter.join(30_000);
			Assertions.assertFalse(deleter.isAlive(), "归零后删除必须完成");
			Assertions.assertNull(failure.get(), "删除不得抛出");
			Assertions.assertFalse(Path.of(home, "t").toFile().exists(), "topic 目录全清");
			Assertions.assertNull(manager.getRocksDatabase().getTable("t.0"), "meta 列族已 drop");
			Assertions.assertNull(manager.getRocksDatabase().getTable("t.0.0"), "索引列族已 drop");
		} finally {
			manager.stop();
		}
	}
}
