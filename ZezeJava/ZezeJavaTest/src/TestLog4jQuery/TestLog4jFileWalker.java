package TestLog4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Builtin.LogService.BCondition;
import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.Log4jLog;
import Zeze.Services.Log4jQuery.Log4jSession;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.Task;

import harness.Fast;

@Fast
public class TestLog4jFileWalker {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	private static Log4jFileManager newManager(Path logDir) throws Exception {
		var logConf = new LogServiceConf.LogConf();
		logConf.logDir = logDir.toString();
		logConf.logActive = "zeze.log";
		return new Log4jFileManager(logConf);
	}

	/**
	 * 文件列表为空时，修改beginTime触发第二次reset()：
	 * 修复前current为null，currentIndex==0分支直接current.reset()抛NPE。
	 */
	@Test
	public void testResetTwiceOnEmptyFiles() throws Exception {
		var logDir = Files.createTempDirectory("zeze-log4jwalker-test");
		var logManager = newManager(logDir);
		try {
			assertTrue(logManager.isEmpty());

			var session = new Log4jSession(logManager);
			var result = new ArrayList<Log4jLog>();
			assertFalse(session.searchContains(result, 1000, -1, List.of("hello"), BCondition.ContainsAll, 10));
			assertTrue(result.isEmpty());

			// 修改beginTime -> trySetBeginTime再次reset()。
			assertFalse(session.searchContains(result, 2000, -1, List.of("hello"), BCondition.ContainsAll, 10));
			assertTrue(result.isEmpty());
		} finally {
			logManager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 空列表下查询后日志文件被创建（onFileCreated加入列表），后续查询在hasNext()中对null current调用：
	 * 修复前NPE且会话永久失效；修复后能继续搜到新文件内容。
	 */
	@Test
	public void testSearchAfterFileCreated() throws Exception {
		var logDir = Files.createTempDirectory("zeze-log4jwalker-test");
		var logManager = newManager(logDir);
		try {
			assertTrue(logManager.isEmpty());

			var session = new Log4jSession(logManager);
			var result = new ArrayList<Log4jLog>();
			assertFalse(session.searchContains(result, 1000, -1, List.of("hello"), BCondition.ContainsAll, 10));

			// 创建活动日志文件，等待FileCreateDetector处理（watch service异步）。
			Files.write(logDir.resolve("zeze.log"), logLine(LocalDateTime.now().minusMinutes(1), "hello world")
					.getBytes(StandardCharsets.UTF_8));
			assertTrue(waitForFiles(logManager, 10_000), "FileCreateDetector没有处理新文件");

			// beginTime不变，不触发reset，直接走hasNext()。
			assertFalse(session.searchContains(result, 1000, -1, List.of("hello"), BCondition.ContainsAll, 10));
			assertEquals(1, result.size());
			assertTrue(result.get(0).getLog().contains("hello world"));
		} finally {
			logManager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 游标停在第一个文件中间时reset()：走复用分支（current!=null && currentIndex==0），
	 * 复用已打开的文件0会话从头重扫，两条都能搜到。
	 */
	@Test
	public void testResetReuseFirstFile() throws Exception {
		var logDir = Files.createTempDirectory("zeze-log4jwalker-test");
		var logManager = newManager(logDir);
		try {
			var base = LocalDateTime.now();
			Files.write(logDir.resolve("zeze.log"),
					(logLine(base.minusMinutes(2), "alpha") + logLine(base.minusMinutes(1), "beta"))
							.getBytes(StandardCharsets.UTF_8));
			assertTrue(waitForFiles(logManager, 10_000), "FileCreateDetector没有处理新文件");

			var session = new Log4jSession(logManager);
			var result = new ArrayList<Log4jLog>();

			// limit=1：游标停在文件0中间，walker处于(0, session0)。
			assertTrue(session.searchContains(result, 0, -1, List.of("zzz"), BCondition.ContainsNone, 1));
			assertEquals(1, result.size());

			// beginTime变化触发reset：复用文件0会话从头重扫。
			assertFalse(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 10));
			assertEquals(2, result.size());
			assertTrue(result.get(0).getLog().contains("alpha"));
			assertTrue(result.get(1).getLog().contains("beta"));
		} finally {
			logManager.stop();
			deleteBestEffort(logDir);
		}
	}

	private static String logLine(LocalDateTime time, String message) {
		return time.format(DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS")) + " " + message + "\n";
	}

	private static boolean waitForFiles(Log4jFileManager logManager, long timeoutMs) throws InterruptedException {
		var deadline = System.currentTimeMillis() + timeoutMs;
		while (logManager.isEmpty()) {
			if (System.currentTimeMillis() >= deadline)
				return false;
			Thread.sleep(50);
		}
		return true;
	}

	// 不用@TempDir：LogIndex的mmap在Windows下持有索引文件句柄，目录删不掉会让JUnit清理阶段失败。
	private static void deleteBestEffort(Path dir) {
		try (var walk = Files.walk(dir)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (IOException e) {
					// 尽力删除，留給系统临时目录清理。
				}
			});
		} catch (IOException e) {
			// ignore
		}
	}
}
