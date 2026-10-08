package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static harness.DirCleanup.deleteBestEffort;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.OutLong;
import Zeze.Util.Task;

import harness.Fast;

/**
 * GD-C02回归：logActive含多个点号（如"a.b.log"）时split取错begin/end：
 * 修复前getCurrentLogFileName()=="a.b"，rotate目标与active自身的CREATE事件都判-1，
 * 轮转永久失察、索引停更。修复后end取末段、begin取其余段拼接。
 * 同时锁定GD-opinions#7：begin/end重叠名（"zezelog"）不再substring越界（修复前SIOOBE）。
 */
@Fast
public class TestMultiDotActiveName {
	private static final String Active = "a.b.log";
	private static final String Rotated = "a.b.2026-09-03.log";

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testMultiDotNameRecognition() throws Exception {
		var logDir = Files.createTempDirectory("multidot-name");
		var manager = newManager(logDir);
		try {
			assertEquals(Active, manager.getCurrentLogFileName(), "修复前返回'a.b'");
			assertEquals(Active + ".index", manager.getCurrentIndexFileName());

			assertEquals(0, manager.testFileName(Active, null), "active自身应识别为当前日志（修复前-1）");

			var date = new OutLong();
			assertEquals(1, manager.testFileName("a.b.2026-01-01.log", date), "rotate目标应被识别（修复前-1）");
			assertEquals(LocalDate.of(2026, 1, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
					.toEpochMilli(), date.value);

			assertEquals(-1, manager.testFileName("a.b.2026-01-01.txt", null), "非log结尾不识别");
			assertEquals(-1, manager.testFileName("x.b.2026-01-01.log", null), "前缀不符不识别");
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	@Test
	public void testMultiDotRotationRegisters() throws Exception {
		var logDir = Files.createTempDirectory("multidot-rotate");
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			freezeAndRotate(manager, logDir);
			// 正序递交：rotate目标先到（case 1改指+补登），新active后到（case 0守卫去重）。
			invokeOnFileCreated(manager, Path.of(Rotated));
			invokeOnFileCreated(manager, Path.of(Active));
			assertRotated(manager);
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	@Test
	public void testMultiDotRotationRegistersOutOfOrder() throws Exception {
		var logDir = Files.createTempDirectory("multidot-rotate-ooo");
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			freezeAndRotate(manager, logDir);
			// 乱序递交：新active先到（case 0同名守卫跳过），rotate目标后到（case 1兜底补登）。
			invokeOnFileCreated(manager, Path.of(Active));
			invokeOnFileCreated(manager, Path.of(Rotated));
			assertRotated(manager);
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	@Test
	public void testOverlappingNameNoThrow() throws Exception {
		var logDir = Files.createTempDirectory("overlap-name");
		Files.createFile(logDir.resolve("zeze.log"));
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = "zeze.log";
		logConf.logDir = logDir.toString();
		var manager = new Log4jFileManager(logConf);
		try {
			// 修复前：startsWith("zeze")&&endsWith("log")成立后substring(4,3)抛StringIndexOutOfBoundsException。
			assertEquals(-1, manager.testFileName("zezelog", null));
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
		manager.stop();
		Files.move(logDir.resolve(Active), logDir.resolve(Rotated));
		Files.createFile(logDir.resolve(Active));
	}

	private static void assertRotated(Log4jFileManager manager) throws Exception {
		assertEquals(2, manager.size(), "修复前两个CREATE事件都判-1，size保持1（轮转失察）");
		try (var rotate = manager.get(0); var active = manager.get(1)) {
			assertEquals(Rotated, rotate.getFile().getName());
			assertEquals(Active, active.getFile().getName());
		}
	}

	private static void invokeOnFileCreated(Log4jFileManager manager, Path path) throws Exception {
		Method method = Log4jFileManager.class.getDeclaredMethod("onFileCreated", Path.class);
		method.setAccessible(true);
		method.invoke(manager, path);
	}
}
