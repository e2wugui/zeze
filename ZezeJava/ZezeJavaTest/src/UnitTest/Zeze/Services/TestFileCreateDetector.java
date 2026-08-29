package UnitTest.Zeze.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Services.Log4jQuery.FileCreateDetector;
import harness.Fast;

/**
 * S-5：FileCreateDetector.stopAndJoin 挂起。
 * <p>
 * 工作线程阻塞在 watchService.take()（无超时），stopAndJoin 仅置 running=false，
 * 不关闭 watchService，take() 永不返回，join 永久挂起。
 */
@Fast
public class TestFileCreateDetector {
	@Test
	@Timeout(15)
	public void testStopAndJoinReturnsWhenNoEvent() throws Exception {
		var dir = Files.createTempDirectory("s5_watch_test");
		try {
			var detector = new FileCreateDetector(dir.toString(), p -> {
			});
			// 没有任何文件事件，stopAndJoin 必须能返回（当前实现永久挂起，由@Timeout暴露）
			detector.stopAndJoin();
		} finally {
			deleteRecursively(dir);
		}
	}

	@Test
	@Timeout(15)
	public void testCreateEventDeliveredBeforeStop() throws Exception {
		var dir = Files.createTempDirectory("s5_watch_test2");
		try {
			var created = new ConcurrentLinkedQueue<String>();
			var detector = new FileCreateDetector(dir.toString(), p -> created.add(p.toString()));
			Files.writeString(dir.resolve("hello.log"), "hello");

			long deadline = System.currentTimeMillis() + 10_000;
			while (created.isEmpty() && System.currentTimeMillis() < deadline)
				Thread.sleep(50);
			Assertions.assertFalse(created.isEmpty(), "create event must be delivered");
			Assertions.assertEquals("hello.log", created.peek());

			detector.stopAndJoin(); // 正常工作后停止也必须返回
		} finally {
			deleteRecursively(dir);
		}
	}

	private static void deleteRecursively(Path dir) {
		try {
			try (var walk = Files.walk(dir)) {
				walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
			}
		} catch (Exception ignored) { // ignored
			// Windows句柄延迟时best-effort
		}
	}
}
