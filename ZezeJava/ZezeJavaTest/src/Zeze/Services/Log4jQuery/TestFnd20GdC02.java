package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static harness.DirCleanup.deleteBestEffort;

import java.io.IOException;
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
import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.Log4jFileWalker;
import Zeze.Services.Log4jQuery.Log4jLog;
import Zeze.Services.Log4jQuery.Log4jSession;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.Task;

import harness.Fast;

/**
 * GD-C02回归：walker/session的close无终态，GD-D03惰性清理（cleanIdleLogSessions先查后锁的两段式）
 * 与并发查询的取锁竞速可"复活"已关会话——迟到的hasNext重开files.get(0)，句柄泄漏+从列表头重扫
 * 返回错窗重复数据。修复后walker加closed终态（对齐Session.closed"先立墓碑再动作"惯例）：
 * close置位，hasNext/next/seek快速失败IllegalStateException。LogService进锁复核map身份属案外#1，
 * 由E组随GE-C03处理，此处只验证walker终态本身。
 */
@Fast
public class TestFnd20GdC02 {
	private static final String Active = "zeze.log";

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * walker直测：close后hasNext不得复活重开文件（修复前重开files.get(0)返回true），
	 * next/seek快速失败，close幂等。
	 */
	@Test
	public void testWalkerClosedFastFails() throws Exception {
		var logDir = Files.createTempDirectory("fnd20-gdc02-walker");
		writeLogs(logDir.resolve(Active), "log0", "log1");
		var manager = newManager(logDir);
		try {
			manager.stop();
			var walker = new Log4jFileWalker(manager);
			assertTrue(walker.hasNext(), "关闭前正常可遍历");

			walker.close();
			// 修复前：hasNext重新打开files.get(0)复活（返回true），next/seek对复活态照常工作。
			assertThrows(IllegalStateException.class, walker::hasNext, "close后hasNext不得复活重开文件");
			assertThrows(IllegalStateException.class, walker::next);
			assertThrows(IllegalStateException.class, () -> walker.seek(0));
			assertDoesNotThrow(walker::close, "close应幂等（重复关闭无害）");
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 会话级（GD-C02触发链的可测投影）：close后的会话再查询必须快速失败而非从列表头重扫返回旧窗
	 * 重复数据。同beginTime路径走hasNext触发；不同beginTime路径走seek触发，两条都验证。
	 */
	@Test
	public void testSessionClosedFastFails() throws Exception {
		var logDir = Files.createTempDirectory("fnd20-gdc02-session");
		writeLogs(logDir.resolve(Active), "log0", "log1");
		var manager = newManager(logDir);
		try {
			manager.stop();
			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();
			assertTrue(session.searchContains(result, 1000, -1, List.of("zzz"), BCondition.ContainsNone, 1));
			assertEquals(1, result.size(), "关闭前会话可用");

			session.close();
			// 同beginTime：trySetBeginTime早退，hasNext触发终态（修复前复活重扫返回旧窗重复数据）。
			assertThrows(IllegalStateException.class, () -> session.searchContains(
					new ArrayList<Log4jLog>(), 1000, -1, List.of("zzz"), BCondition.ContainsNone, 10));
			// 不同beginTime：seek触发终态。
			assertThrows(IllegalStateException.class, () -> session.searchContains(
					new ArrayList<Log4jLog>(), 2000, -1, List.of("zzz"), BCondition.ContainsNone, 10));
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

	private static void writeLogs(Path file, String... messages) throws IOException {
		var fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
		var base = LocalDateTime.now();
		var sb = new StringBuilder();
		for (var i = 0; i < messages.length; ++i)
			sb.append(base.plusSeconds(30L * i).format(fmt)).append(' ').append(messages[i]).append('\n');
		AtomicFileWriter.replace(file, sb.toString().getBytes(StandardCharsets.UTF_8));
	}
}
