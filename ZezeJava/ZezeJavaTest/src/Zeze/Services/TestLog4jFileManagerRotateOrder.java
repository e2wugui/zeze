package Zeze.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND2-S3-5 回归：log4j rotate 产生双 CREATE 事件（rotate目标、新active），部分平台
 * WatchService 递交顺序不保证。新active事件先到时，onFileCreated case 0 的同名守卫
 * （files 末尾仍是旧 active 同名条目）跳过登记，case 1 只改指不补登，新 active 漏登
 * 一个 rotate 周期（该周期内当前日志对全部查询不可见）。
 * 修复后 case 1 完成 last.file 改指后主动补登新 active（存在性+同名守卫去重），
 * 两种递交顺序均收敛到相同终态。
 * 修复前 testRotateEventsOutOfOrder 中 size 保持 1（漏登）。
 * 先 stop() 冻结监视线程再物理 rotate，递交顺序完全受控（真实 WatchService 顺序不定）。
 */
@Fast
public class TestLog4jFileManagerRotateOrder {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testRotateEventsOutOfOrder() throws Exception {
		var logDir = Files.createTempDirectory("zeze-log4j-rotate-ooo-test");
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size()); // 启动登记旧active
			freezeAndRotate(manager, logDir);
			// 乱序递交：新active的CREATE先到——case 0同名守卫跳过（修复要兜底的就是这条路径），
			// rotate目标的CREATE后到——case 1改指后补登兜底。
			invokeOnFileCreated(manager, Path.of("zeze.log"));
			invokeOnFileCreated(manager, Path.of("zeze.2026-09-03.log"));
			assertRotated(manager);
		} finally {
			manager.stop(); // 幂等（watchService已关再close无效，线程已join立即返回）
			deleteBestEffort(logDir);
		}
	}

	@Test
	public void testRotateEventsInOrder() throws Exception {
		var logDir = Files.createTempDirectory("zeze-log4j-rotate-inorder-test");
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			freezeAndRotate(manager, logDir);
			// 正序递交：rotate目标先到（case 1改指+补登，新active已物理存在），新active后到
			// （case 0守卫去重），终态与乱序一致——顺序无关性。
			invokeOnFileCreated(manager, Path.of("zeze.2026-09-03.log"));
			invokeOnFileCreated(manager, Path.of("zeze.log"));
			assertRotated(manager);
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	private static Log4jFileManager newManager(Path logDir) throws Exception {
		Files.createFile(logDir.resolve("zeze.log")); // 启动时已存在的active（空文件）
		var conf = new LogServiceConf.LogConf();
		conf.logActive = "zeze.log";
		conf.logDir = logDir.toString();
		return new Log4jFileManager(conf);
	}

	private static void freezeAndRotate(Log4jFileManager manager, Path logDir) throws IOException {
		// 冻结监视线程与索引定时器：后续物理rotate产生的真实CREATE事件不再被递交，
		// 事件顺序完全由下面的反射调用控制。
		manager.stop();
		// 模拟log4j rotate：zeze.log -> zeze.<date>.log，新建zeze.log。
		Files.move(logDir.resolve("zeze.log"), logDir.resolve("zeze.2026-09-03.log"));
		Files.createFile(logDir.resolve("zeze.log"));
	}

	private static void assertRotated(Log4jFileManager manager) throws Exception {
		assertEquals(2, manager.size());
		try (var rotate = manager.get(0); var active = manager.get(1)) {
			assertEquals("zeze.2026-09-03.log", rotate.getFile().getName()); // 旧条目改指rotate名
			assertEquals("zeze.log", active.getFile().getName()); // 新active已登记
		}
	}

	private static void invokeOnFileCreated(Log4jFileManager manager, Path path) throws Exception {
		// 事件路径仅用其文件名（onFileCreated里path.toFile().getName()），与FileCreateDetector
		// 递交的相对路径形态一致。
		Method method = Log4jFileManager.class.getDeclaredMethod("onFileCreated", Path.class);
		method.setAccessible(true);
		method.invoke(manager, path);
	}

	// 尽力删除：LogIndex的mmap由GC cleaner延迟释放，Windows下可能暂时删不掉，留给系统临时目录清理。
	private static void deleteBestEffort(Path dir) {
		try (var walk = Files.walk(dir)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (IOException e) {
					// ignore
				}
			});
		} catch (IOException e) {
			// ignore
		}
	}
}
