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
public class TestManagerConstructFailCleanup {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testConstructorFailureStopsDetector() throws Exception {
		var logDir = Files.createTempDirectory("fnd19-ctor-fail");
		// 装载抛出注入：active在磁盘触发openActiveIndexAtLoad，indexLinks名被普通文件占据——
		// nextLinkFile的createDirectories对同名文件跨平台必抛。该失败面是索引通道的真IO错误，
		// 不在rotate名.index残留的配对校验消费范围内（那是FND25 log4j-04的合法降级面）。
		Files.write(logDir.resolve("zeze.log"), new byte[0]);
		Files.createFile(logDir.resolve("indexLinks"));

		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = "zeze.log";
		logConf.logDir = logDir.toString();
		assertThrows(Exception.class, () -> new Log4jFileManager(logConf));

		// 修复前后构造都抛；区分点是watch线程是否被回收。观察前先解除indexLinks占位：
		// 泄漏的线程处理CREATE(active)会在indexLinks下建编号索引文件（openFreshActiveIndex），
		// 占位不解除则该副作用被掩盖、断言空转；已回收的线程对后续创建无任何磁盘副作用。
		Files.delete(logDir.resolve("indexLinks"));
		Files.delete(logDir.resolve("zeze.log"));
		Files.createFile(logDir.resolve("zeze.log"));
		var linkIndex = logDir.resolve("indexLinks").resolve("1");
		var deadline = System.currentTimeMillis() + 1000;
		while (Files.notExists(linkIndex) && System.currentTimeMillis() < deadline)
			Thread.sleep(50);
		assertFalse(Files.exists(linkIndex), "构造失败后detector线程应已join，不得再处理文件创建事件");

		deleteBestEffort(logDir);
	}
}
