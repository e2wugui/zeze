package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static harness.DirCleanup.deleteBestEffort;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Builtin.LogService.BCondition;
import Zeze.Services.Log4jQuery.FileCreateDetector;
import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.Log4jLog;
import Zeze.Services.Log4jQuery.Log4jSession;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.Task;

import harness.Fast;

/**
 * GD-D02回归：watch事件丢失无对账。修复后reconcile以磁盘为真相源把files收敛一致：
 * 消失条目摘除（.gz压缩/保留期删除无事件）、漏登的合法rotate文件补登（testFileName判定+loadIndex幂等）。
 * OVERFLOW/reset失败的告警形态：FileCreateDetector接线onOverflow/onWatchInvalid回调（warn+触发对账），
 * OVERFLOW入口节流（60s窗口内重复触发不重复执行）。
 * 注：真实OVERFLOW事件与key.reset()==false（目录删除）无确定性触发手段，告警分支为代码审读验证，
 * 这里验证的是回调接线与节流窗口；reconcile挂buildIndexTimer（5分钟）低频执行。
 */
@Fast
public class TestReconcileConvergesToDisk {
	private static final String Active = "zeze.log";
	private static final String Rotated1 = "zeze.2026-09-01.log";
	private static final String Rotated2 = "zeze.2026-09-02.log";

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * 对账摘除消失条目：磁盘上已不存在的登记条目（无事件的清理）被移除，其余条目可查。
	 */
	@Test
	public void testReconcileRemovesVanishedEntry() throws Exception {
		var logDir = Files.createTempDirectory("reconcile-remove");
		Files.write(logDir.resolve(Rotated1),
				logLine(LocalDateTime.of(2026, 9, 1, 0, 0), "rotate-log").getBytes(StandardCharsets.UTF_8));
		Files.write(logDir.resolve(Active),
				logLine(LocalDateTime.now(), "active-log").getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(2, manager.size());
			manager.stop(); // 冻结watch与定时对账，消失由本次直调对账发现
			Files.delete(logDir.resolve(Rotated1)); // 外部清理无事件

			invokeReconcile(manager);
			assertEquals(1, manager.size(), "对账应摘除磁盘上已消失的条目");

			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();
			var remain = session.searchContains(result, -1, -1,
					List.of("active-log"), BCondition.ContainsAll, 10);
			assertTrue(!remain && 1 == result.size(), "对账后剩余条目仍可查");
			session.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 对账补登漏登文件：watch双事件都丢时rotate文件未登记，对账重扫补登并建索引；
	 * 补登顺序保持rotate在active之前（buildIndex只推进last==当前名的条目，active必须last——
	 * 直接追加会把active挤到中间再触发active守卫重复登记，同一文件双条目）。
	 */
	@Test
	public void testReconcileRegistersMissedRotate() throws Exception {
		var logDir = Files.createTempDirectory("reconcile-register");
		Files.write(logDir.resolve(Active),
				logLine(LocalDateTime.now(), "active-log").getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop(); // 模拟watch事件丢失：新文件创建无事件
			Files.write(logDir.resolve(Rotated1),
					logLine(LocalDateTime.of(2026, 9, 1, 0, 0), "missed-rotate-log")
							.getBytes(StandardCharsets.UTF_8));

			invokeReconcile(manager);
			assertEquals(2, manager.size(), "对账应补登磁盘上未登记的合法rotate文件");
			// 补登顺序：rotate在active前（修复前直接追加得到[active,rotate,active]双登记active）。
			var names = fileNamesOf(manager);
			assertEquals(List.of(Rotated1, Active), names);

			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();
			var remain = session.searchContains(result, -1, -1,
					List.of("missed-rotate-log"), BCondition.ContainsAll, 10);
			assertTrue(!remain && 1 == result.size(), "补登条目应建好索引并参与查询");
			session.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * OVERFLOW告警形态（可测部分）：detector的溢出/失效补偿回调已接线；溢出入口节流——
	 * 窗口内首次触发执行对账，紧随的重复触发不重复执行（对账效果不可见），直接调reconcile不受节流。
	 */
	@Test
	public void testOverflowWiringAndThrottle() throws Exception {
		var logDir = Files.createTempDirectory("reconcile-throttle");
		Files.write(logDir.resolve(Rotated1),
				logLine(LocalDateTime.of(2026, 9, 1, 0, 0), "rotate-log").getBytes(StandardCharsets.UTF_8));
		Files.write(logDir.resolve(Active),
				logLine(LocalDateTime.now(), "active-log").getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(2, manager.size());
			// 告警+补偿的接线形态：OVERFLOW→对账、reset失败→最终对账，回调必须非空（warn在回调前的分支内）。
			assertNotNull(detectorConsumer(manager, "onOverflowConsumer"), "OVERFLOW对账回调未接线");
			assertNotNull(detectorConsumer(manager, "onWatchInvalidConsumer"), "监听失效最终对账回调未接线");

			manager.stop();
			Files.delete(logDir.resolve(Rotated1));
			// 溢出触发的节流对账：首次（lastReconcileTime=0）放行执行，条目被摘除。
			invokeReconcileThrottled(manager);
			assertEquals(1, manager.size(), "节流窗口首次触发应执行对账");

			// 紧随的再次触发（60s窗口内）：被节流，本轮漏登不补。
			Files.write(logDir.resolve(Rotated2),
					logLine(LocalDateTime.of(2026, 9, 2, 0, 0), "missed-2")
							.getBytes(StandardCharsets.UTF_8));
			invokeReconcileThrottled(manager);
			assertEquals(1, manager.size(), "节流窗口内重复触发不应重复对账");

			// 直调reconcile（buildIndexTimer挂载的同款入口，reset失败的最终对账）不受节流。
			invokeReconcile(manager);
			assertEquals(2, manager.size(), "直调对账应补登漏登文件");
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	private static Log4jFileManager newManager(Path logDir) throws Exception {
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = Active;
		logConf.logDir = logDir.toString();
		return new Log4jFileManager(logConf);
	}

	private static void invokeReconcile(Log4jFileManager manager) throws Exception {
		Method method = Log4jFileManager.class.getDeclaredMethod("reconcile");
		method.setAccessible(true);
		method.invoke(manager);
	}

	private static void invokeReconcileThrottled(Log4jFileManager manager) throws Exception {
		Method method = Log4jFileManager.class.getDeclaredMethod("reconcileThrottled");
		method.setAccessible(true);
		method.invoke(manager);
	}

	private static Object detectorConsumer(Log4jFileManager manager, String fieldName) throws Exception {
		var detectorField = Log4jFileManager.class.getDeclaredField("fileCreateDetector");
		detectorField.setAccessible(true);
		var detector = detectorField.get(manager);
		var consumerField = FileCreateDetector.class.getDeclaredField(fieldName);
		consumerField.setAccessible(true);
		return consumerField.get(detector);
	}

	@SuppressWarnings("unchecked")
	private static List<String> fileNamesOf(Log4jFileManager manager) throws Exception {
		var filesField = Log4jFileManager.class.getDeclaredField("files");
		filesField.setAccessible(true);
		var files = (List<Log4jFileManager.Log4jFile>)filesField.get(manager);
		var names = new ArrayList<String>();
		for (var file : files)
			names.add(file.file.getName());
		return names;
	}

	private static String logLine(LocalDateTime time, String message) {
		return time.format(DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS")) + " " + message + "\n";
	}
}
