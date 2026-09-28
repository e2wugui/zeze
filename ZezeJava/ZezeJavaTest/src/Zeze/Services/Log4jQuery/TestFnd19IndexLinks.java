package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static harness.DirCleanup.deleteBestEffort;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.Task;

import harness.Fast;

/**
 * GD-C06回归：indexLinks目录出现非数字名文件（运维残留/desktop.ini等）时
 * removeOldLinkFiles/nextLinkFile的Long.parseLong抛NumberFormatException：
 * 启动路径构造失败；运行路径该次轮转登记被catch吞掉且不再重试。
 * GD-C08回归：removeOldLinkFiles只在构造期执行一次，运行期每次轮转+1个硬链接只在下次启动清理；
 * 修复后onFileCreated登记新active后同步清理。
 * 注：旧链接可能被rotate条目LogIndex的mmap钉住（Windows）删不掉，故只断言未被钉住的
 * 残留条目被清理，不断言目录总条数（钉句柄问题属GD-D05记录范畴）。
 */
@Fast
public class TestFnd19IndexLinks {
	private static final String Active = "zeze.log";
	private static final String Rotated = "zeze.2026-09-03.log";

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testJunkEntryToleratedAtStartup() throws Exception {
		var logDir = Files.createTempDirectory("fnd19-links-junk-startup");
		Files.createFile(logDir.resolve(Active));
		var indexLinks = Files.createDirectory(logDir.resolve("indexLinks"));
		Files.createFile(indexLinks.resolve("desktop.ini"));
		Files.createFile(indexLinks.resolve("1"));

		// 修复前：构造器removeOldLinkFiles对desktop.ini parseLong抛NumberFormatException。
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = Active;
		logConf.logDir = logDir.toString();
		var manager = assertDoesNotThrow(() -> new Log4jFileManager(logConf));
		try {
			assertEquals(1, manager.size());
			// 构造期清理按"非max即删"清掉污染条目。
			assertFalse(Files.exists(indexLinks.resolve("desktop.ini")));
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	@Test
	public void testJunkEntryToleratedDuringRotation() throws Exception {
		var logDir = Files.createTempDirectory("fnd19-links-junk-rotate");
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop(); // 冻结监视，事件受控递交
			Files.createFile(logDir.resolve("indexLinks").resolve("desktop.ini")); // 运行期污染
			freezeAndRotate(manager, logDir);

			invokeOnFileCreated(manager, Path.of(Rotated));
			invokeOnFileCreated(manager, Path.of(Active));

			// 修复前：补登loadIndex→nextLinkFile对desktop.ini parseLong抛出，被onFileCreated的catch吞掉，登记失败。
			assertEquals(2, manager.size(), "轮转登记不应被indexLinks污染条目打断（修复前保持1）");
		} finally {
			manager.stop(); // 幂等
			deleteBestEffort(logDir);
		}
	}

	@Test
	public void testRotationCleansStaleLinks() throws Exception {
		var logDir = Files.createTempDirectory("fnd19-links-cleanup");
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop();
			// 模拟上次运行残留的stale链接条目（普通文件，未被mmap钉住，可删除）。
			Files.createFile(logDir.resolve("indexLinks").resolve("0"));
			freezeAndRotate(manager, logDir);

			invokeOnFileCreated(manager, Path.of(Rotated));
			invokeOnFileCreated(manager, Path.of(Active));

			assertEquals(2, manager.size());
			// 修复前removeOldLinkFiles只在构造期跑：运行期轮转后"0"残留（累积直至重启）。
			assertFalse(Files.exists(logDir.resolve("indexLinks").resolve("0")),
					"轮转登记后应同步清理旧链接条目（GD-C08）");
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	private static Log4jFileManager newManager(Path logDir) throws Exception {
		Files.createFile(logDir.resolve(Active));
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = Active;
		logConf.logDir = logDir.toString();
		return new Log4jFileManager(logConf);
	}

	private static void freezeAndRotate(Log4jFileManager manager, Path logDir) throws IOException {
		Files.move(logDir.resolve(Active), logDir.resolve(Rotated));
		Files.createFile(logDir.resolve(Active));
	}

	private static void invokeOnFileCreated(Log4jFileManager manager, Path path) throws Exception {
		Method method = Log4jFileManager.class.getDeclaredMethod("onFileCreated", Path.class);
		method.setAccessible(true);
		method.invoke(manager, path);
	}
}
