package TestLog4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static harness.DirCleanup.deleteBestEffort;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Builtin.LogService.BCondition;
import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.Log4jLog;
import Zeze.Services.Log4jQuery.Log4jSession;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.OutInt;
import Zeze.Util.Task;

import harness.Fast;

/**
 * GD-D01回归：轮转日志被外部删除（logrotate压缩/保留期清理）后files条目悬空：
 * 修复前manager.seek/walker.get两处new Log4jFileSession对不存在文件抛FileNotFoundException，
 * 异常穿透到查询RPC整体报错；修复后跳过该条目继续、持锁摘除条目并warn，files最终收敛到与磁盘一致。
 * 两个打开点（seek按index选中 / walker顺序遍历）分别验证。
 */
@Fast
public class TestFnd19GdD01 {
	private static final String Active = "zeze.log";
	private static final String Rotated1 = "zeze.2026-09-01.log";
	private static final String Rotated2 = "zeze.2026-09-02.log";

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * walker顺序遍历路径：被删条目由manager.get持锁摘除后继续，查询不炸且能搜到其余文件内容。
	 */
	@Test
	public void testWalkerSkipsDeletedEntry() throws Exception {
		var logDir = Files.createTempDirectory("fnd19-gdd01-walker");
		var base = LocalDateTime.now();
		Files.write(logDir.resolve(Rotated1),
				logLine(base.minusDays(2), "rotate-old-log").getBytes(StandardCharsets.UTF_8));
		Files.write(logDir.resolve(Active),
				(logLine(base, "active-new-log")).getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(2, manager.size());
			manager.stop(); // 冻结监视与索引定时器，删除只由查询路径发现（不是对账GD-D02发现）
			Files.delete(logDir.resolve(Rotated1)); // 外部清理：保留期删除

			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();
			// 修复前：walker.hasNext→get→new Log4jFileSession抛FileNotFoundException穿透到调用方。
			var remain = session.searchContains(result, -1, -1,
					List.of("active-new-log"), BCondition.ContainsAll, 10);
			assertFalse(remain);
			assertEquals(1, result.size(), "被删条目跳过后，active文件内容仍可查");
			assertTrue(result.get(0).getLog().contains("active-new-log"));
			assertEquals(1, manager.size(), "悬空条目应被持锁摘除");
			session.close();
		} finally {
			manager.stop(); // 幂等
			deleteBestEffort(logDir);
		}
	}

	/**
	 * seek按index.beginTime选中条目路径：命中的条目已被外部删除时摘除并降级到更旧文件。
	 * active为空文件（index空→beginTime=Long.MAX_VALUE）保证seek先命中被删的rotate。
	 */
	@Test
	public void testSeekSkipsDeletedEntry() throws Exception {
		var logDir = Files.createTempDirectory("fnd19-gdd01-seek");
		Files.write(logDir.resolve(Rotated1),
				logLine(LocalDateTime.of(2026, 9, 1, 0, 0), "oldest").getBytes(StandardCharsets.UTF_8));
		Files.write(logDir.resolve(Rotated2),
				logLine(LocalDateTime.of(2026, 9, 2, 0, 0), "newer").getBytes(StandardCharsets.UTF_8));
		Files.createFile(logDir.resolve(Active)); // 空active：仅占位，seek不会先命中它
		var manager = newManager(logDir);
		try {
			assertEquals(3, manager.size());
			manager.stop();
			Files.delete(logDir.resolve(Rotated2)); // 外部清理

			var out = new OutInt();
			var seekTime = millis(LocalDateTime.of(2026, 9, 2, 12, 0));
			// 修复前：seek对被删文件new Log4jFileSession抛FileNotFoundException穿透。
			var logFileSession = manager.seek(seekTime, out);
			assertTrue(null != logFileSession, "被删条目跳过后应降级到更旧的文件");
			assertEquals(Rotated1, logFileSession.getFile().getName());
			assertEquals(0, out.value);
			assertEquals(2, manager.size(), "悬空条目应被持锁摘除");
			logFileSession.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 残余条目全部打不开：walker按遍历耗尽处理（hasNext收尾false），不抛异常。
	 */
	@Test
	public void testAllEntriesMissingExhausts() throws Exception {
		var logDir = Files.createTempDirectory("fnd19-gdd01-all-missing");
		Files.write(logDir.resolve(Rotated1),
				logLine(LocalDateTime.now().minusDays(1), "only-log").getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop();
			Files.delete(logDir.resolve(Rotated1));

			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();
			var remain = session.searchContains(result, -1, -1,
					List.of("only-log"), BCondition.ContainsAll, 10);
			// 修复前：FileNotFoundException穿透；修复后：条目摘除、遍历耗尽、安静返回。
			assertFalse(remain);
			assertTrue(result.isEmpty());
			assertEquals(0, manager.size());
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

	private static String logLine(LocalDateTime time, String message) {
		return time.format(DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS")) + " " + message + "\n";
	}

	private static long millis(LocalDateTime time) {
		return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
}
