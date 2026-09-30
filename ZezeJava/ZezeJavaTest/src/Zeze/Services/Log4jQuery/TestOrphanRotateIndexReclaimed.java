package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static harness.DirCleanup.deleteBestEffort;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.Task;

import harness.Fast;

/**
 * 外部保留期只清 R.log 而留 R.index 时孤儿索引必须被回收：transferIndexToRotate
 * 每次轮转在 logDir 留下 rotate 名索引，其唯一删除路径 openRotateIndex 只在同名
 * 日志在场时可达——R.log 被外部清理后对账摘除只动内存列表，R.index 无任何回收
 * 路径，随轮转无界累积（每日轮转+保留N天部署随运行时长线性增长，链接接管形态
 * 下还钉住索引 inode 的数据块）。修复后装载与对账枚举识别"本 manager rotate
 * 名形态（testFileName==1）且对应日志已不在磁盘"的孤儿并 best-effort 删除；
 * 在场日志的索引、active 交接名与他方 logActive 名形态不受影响。
 */
@Fast
public class TestOrphanRotateIndexReclaimed {
	private static final String Active = "zeze.log";
	// 孤儿形态：索引在场、对应日志不在（外部保留期只清 .log）
	private static final String GoneRotate = "zeze.2026-09-01.log";
	private static final String OrphanIndex = GoneRotate + ".index";
	// 在场形态：rotate 日志与其索引俱在（配对/复用价值保留给 openRotateIndex）
	private static final String LiveRotate = "zeze.2026-09-02.log";
	private static final String LiveRotateIndex = LiveRotate + ".index";

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * 对账回收：R.log 已被外部清理后，其孤儿 R.index 在对账中被删除（修复前无界累积）。
	 */
	@Test
	public void testReconcileReclaimsOrphanRotateIndex() throws Exception {
		var logDir = Files.createTempDirectory("orphan-idx-reconcile");
		Files.write(logDir.resolve(Active), logLine(LocalDateTime.now(), "active").getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			manager.stop(); // 冻结watch与定时对账，回收由本次直调对账执行
			Files.write(logDir.resolve(OrphanIndex), new byte[]{1, 2, 3});
			assertTrue(Files.exists(logDir.resolve(OrphanIndex)), "前置：孤儿索引在场");

			invokeReconcile(manager);
			assertFalse(Files.exists(logDir.resolve(OrphanIndex)),
					"对账应回收无对应日志的rotate名孤儿索引（修复前永不删除）");
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 装载回收：进程停机期间被外部清理的遗留（R.log 删除、R.index 残留）在构造装载时清扫。
	 */
	@Test
	public void testLoadSweepsOrphanLeftFromDowntime() throws Exception {
		var logDir = Files.createTempDirectory("orphan-idx-load");
		Files.write(logDir.resolve(Active), logLine(LocalDateTime.now(), "active").getBytes(StandardCharsets.UTF_8));
		Files.write(logDir.resolve(OrphanIndex), new byte[]{1, 2, 3});
		var manager = newManager(logDir); // 构造装载即清扫
		try {
			assertFalse(Files.exists(logDir.resolve(OrphanIndex)),
					"装载应回收停机期外部清理遗留的孤儿索引（修复前跨重启永存）");
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 误伤防线：在场日志的索引、active 交接名（&lt;active&gt;.index）、他方 logActive 名形态的
	 * 索引不被回收——回收判据严格限定本 manager 的 rotate 名形态且对应日志不在磁盘。
	 */
	@Test
	public void testInUseAndForeignIndexesNotTouched() throws Exception {
		var logDir = Files.createTempDirectory("orphan-idx-keep");
		Files.write(logDir.resolve(Active), logLine(LocalDateTime.now(), "active").getBytes(StandardCharsets.UTF_8));
		Files.write(logDir.resolve(LiveRotate), logLine(LocalDateTime.of(2026, 9, 2, 0, 0), "live").getBytes(StandardCharsets.UTF_8));
		Files.write(logDir.resolve(LiveRotateIndex), new byte[]{9});
		var handover = logDir.resolve(Active + ".index"); // active交接名（openActiveIndexAtLoad候选）
		Files.write(handover, new byte[]{9});
		var foreign = logDir.resolve("other.2026-09-01.log.index"); // 他方logActive名形态
		Files.write(foreign, new byte[]{9});
		var manager = newManager(logDir);
		try {
			invokeReconcile(manager);
			assertTrue(Files.exists(logDir.resolve(LiveRotateIndex)), "在场rotate的索引不得回收（配对复用价值）");
			assertTrue(Files.exists(handover), "active交接名索引不得回收");
			assertTrue(Files.exists(foreign), "他方logActive名形态不得回收（同目录多日志合法共存）");
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

	private static String logLine(LocalDateTime time, String message) {
		return time.format(DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS")) + " " + message + "\n";
	}
}
