package UnitTest.Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import Zeze.Util.BufferedRandomFile;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-48回归：BufferedRandomFile的公开方法（getPosition/seek/read/readLine）都以
 * lock()/unlock()串行化，唯独close()直接关RandomAccessFile——并发close与持锁读
 * 竞争，读线程的channel.read抛ClosedChannelException且buffer未消费数据静默作废。
 * 修复：close()同样持自身锁。
 * 测试钉住可观察契约：对象锁被占用期间close()不得完成（必须先获取锁），
 * 释放锁后close正常完成。确定性：ReentrantLock被他人持有时close必然阻塞。
 */
@Fast
public class TestFnd748BufferedRandomFileCloseLock {

	@Test
	public void testCloseWaitsForLock() throws Exception {
		var file = Path.of("TestFnd748BufferedRandomFileCloseLock.tmp");
		Files.writeString(file, "line1\nline2\n", StandardCharsets.UTF_8);
		try (var f = new BufferedRandomFile(file.toFile(), StandardCharsets.UTF_8)) {
			Assertions.assertEquals("line1", f.readLine()); // 正常路径

			f.lock(); // 模拟并发读持锁（readLine等公开方法内部即持此锁）
			var closed = new CountDownLatch(1);
			var closer = Thread.ofPlatform().start(() -> {
				try {
					f.close();
				} catch (Exception e) {
					// 关闭异常吞掉：本用例只观察关闭时序
				} finally {
					closed.countDown();
				}
			});
			// 修复前红：close不获取锁立即完成；修复后：必须等锁释放
			Assertions.assertFalse(closed.await(500, TimeUnit.MILLISECONDS),
					"close() must block while the object lock is held");
			f.unlock();
			Assertions.assertTrue(closed.await(5, TimeUnit.SECONDS), "close() must finish after unlock");
			closer.join(5_000);
		} finally {
			Files.deleteIfExists(file);
		}
	}
}
