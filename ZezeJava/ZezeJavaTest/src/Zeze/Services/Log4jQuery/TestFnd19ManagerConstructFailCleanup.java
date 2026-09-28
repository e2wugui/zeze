package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static harness.DirCleanup.deleteBestEffort;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.Task;

import harness.Fast;

/**
 * GD-C07回归：Log4jFileManager构造失败（装载抛出）不回收已启动的FileCreateDetector线程，
 * watch线程永驻并持续在半构造对象上做IO。修复后构造catch中stopAndJoin再重抛。
 * 观察：构造失败后创建active文件——泄漏的线程会处理CREATE并创建索引文件；
 * 已回收的线程不会产生任何磁盘副作用。
 */
@Fast
public class TestFnd19ManagerConstructFailCleanup {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testConstructorFailureStopsDetector() throws Exception {
		var logDir = Files.createTempDirectory("fnd19-ctor-fail");
		// rotate日志 + 同名.index是目录：loadRotates→loadIndex→LogIndex构造打开FileOutputStream必抛。
		Files.createFile(logDir.resolve("zeze.2026-01-01.log"));
		Files.createDirectory(logDir.resolve("zeze.2026-01-01.log.index"));

		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = "zeze.log";
		logConf.logDir = logDir.toString();
		assertThrows(Exception.class, () -> new Log4jFileManager(logConf));

		// 修复前后构造都抛；区分点是watch线程是否被回收：泄漏线程处理该CREATE会创建zeze.log.index。
		var activeIndex = logDir.resolve("zeze.log.index");
		Files.createFile(logDir.resolve("zeze.log"));
		var deadline = System.currentTimeMillis() + 1000;
		while (Files.notExists(activeIndex) && System.currentTimeMillis() < deadline)
			Thread.sleep(50);
		assertFalse(Files.exists(activeIndex), "构造失败后detector线程应已join，不得再处理文件创建事件");

		deleteBestEffort(logDir);
	}
}
