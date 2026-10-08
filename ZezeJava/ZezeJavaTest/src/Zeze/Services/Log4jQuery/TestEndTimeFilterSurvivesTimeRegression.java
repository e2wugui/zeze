package Zeze.Services.Log4jQuery;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static harness.DirCleanup.deleteBestEffort;

import java.lang.reflect.Method;
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
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.Task;

import harness.Fast;

/**
 * 有界查询（携带endTime）遇到扫描流时间回退（时钟步进回拨/拷入乱序内容——FND29已废除
 * "列表按内容时间有序"不变式并文档化该形态）时，回归点之后仍落在[beginTime,endTime]窗口
 * 内的日志必须照常返回：窗口边界是逐条谓词，不是"扫描流时间单调"假设下的全局终止判据。
 * 修复前四个查询入口在首个超窗条目处整体终止并返回remain=false（客户端判"查完"），
 * 其后的窗口内日志静默漏读且跨重启不可恢复。
 */
@Fast
@Extra
public class TestEndTimeFilterSurvivesTimeRegression {
	private static final String Active = "zeze.log";
	// 回退前（快时钟）的最后一条：时间在endTime之外，其后文件内时间倒退回窗口内。
	private static final LocalDateTime BeforeRollback = LocalDateTime.of(2026, 9, 28, 15, 59, 30);
	// NTP步进回拨校正后继续写：前一条低于beginTime被下界过滤，后一条在窗口内。
	private static final LocalDateTime AfterRollbackA = LocalDateTime.of(2026, 9, 28, 14, 0, 10);
	private static final LocalDateTime AfterRollbackB = LocalDateTime.of(2026, 9, 28, 14, 0, 40);
	// 查询窗口[14:00:20, 15:00:00]：15:59:30被上界丢弃、14:00:10被下界丢弃、14:00:40必须返回。
	private static final LocalDateTime WindowBegin = LocalDateTime.of(2026, 9, 28, 14, 0, 20);
	private static final LocalDateTime WindowEnd = LocalDateTime.of(2026, 9, 28, 15, 0, 0);

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/** 单文件回退形态：searchContains在终止点之后必须返回窗口内的14:00:40行。 */
	@Test
	public void testSearchContainsReturnsWindowLogAfterRollback() throws Exception {
		var manager = newSingleRollbackManager();
		try {
			var session = new Log4jSession(manager);
			try {
				var result = new ArrayList<Log4jLog>();
				var remain = session.searchContains(result, millis(WindowBegin), millis(WindowEnd),
						List.of("after-rollback"), BCondition.ContainsAll, 100);
				assertFalse(remain, "文件耗尽即查完（修复前：首个超窗条目处终止并谎报查完）");
				assertEquals(List.of("after-rollback-b"), logsOf(result),
						"回归点之后的窗口内日志必须返回（修复前静默漏读）");
			} finally {
				session.close();
			}
		} finally {
			manager.stop();
		}
	}

	/** 单文件回退形态：searchRegex同型。 */
	@Test
	public void testSearchRegexReturnsWindowLogAfterRollback() throws Exception {
		var manager = newSingleRollbackManager();
		try {
			var session = new Log4jSession(manager);
			try {
				var result = new ArrayList<Log4jLog>();
				var remain = session.searchRegex(result, millis(WindowBegin), millis(WindowEnd),
						"after-rollback-.*", 100);
				assertFalse(remain, "文件耗尽即查完");
				assertEquals(List.of("after-rollback-b"), logsOf(result),
						"回归点之后的窗口内日志必须返回（修复前静默漏读）");
			} finally {
				session.close();
			}
		} finally {
			manager.stop();
		}
	}

	/** 单文件回退形态：browseContains同型。 */
	@Test
	public void testBrowseContainsReturnsWindowLogAfterRollback() throws Exception {
		var manager = newSingleRollbackManager();
		try {
			var session = new Log4jSession(manager);
			try {
				var result = new java.util.ArrayDeque<Log4jLog>();
				var remain = session.browseContains(result, millis(WindowBegin), millis(WindowEnd),
						List.of("after-rollback"), BCondition.ContainsAll, 100, 0.0f);
				assertFalse(remain, "文件耗尽即查完");
				assertEquals(List.of("after-rollback-b"), logsOf(new ArrayList<>(result)),
						"回归点之后的窗口内日志必须返回（修复前静默漏读）");
			} finally {
				session.close();
			}
		} finally {
			manager.stop();
		}
	}

	/** 单文件回退形态：browseRegex同型。 */
	@Test
	public void testBrowseRegexReturnsWindowLogAfterRollback() throws Exception {
		var manager = newSingleRollbackManager();
		try {
			var session = new Log4jSession(manager);
			try {
				var result = new java.util.ArrayDeque<Log4jLog>();
				var remain = session.browseRegex(result, millis(WindowBegin), millis(WindowEnd),
						"after-rollback-.*", 100, 0.0f);
				assertFalse(remain, "文件耗尽即查完");
				assertEquals(List.of("after-rollback-b"), logsOf(new ArrayList<>(result)),
						"回归点之后的窗口内日志必须返回（修复前静默漏读）");
			} finally {
				session.close();
			}
		} finally {
			manager.stop();
		}
	}

	/**
	 * 轮转序形态（回归点落在文件边界）：终态[R1(15:00..15:59:00), R2(15:59:30,14:00:10,
	 * 14:00:40), active(14:01:00,14:01:30)]，查[14:00:20,15:59:00]——R1全部、R2的14:00:40、
	 * active两条都必须返回；修复前扫描在R2首条15:59:30处终止，R2/active的窗口内三条静默漏读。
	 */
	@Test
	public void testCrossRotateWindowSpansRegressionBoundary() throws Exception {
		var logDir = Files.createTempDirectory("log4j-endtime-cross-rotate");
		var fastBase = LocalDateTime.of(2026, 9, 28, 15, 0);
		var realBase = LocalDateTime.of(2026, 9, 28, 14, 0);
		// 世代1（快时钟15:00..15:59）。
		AtomicFileWriter.replace(logDir.resolve(Active), buildLines(fastBase, "g1-",
				0, 600, 1200, 1800, 2400, 3000, 3540).getBytes(java.nio.charset.StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop(); // 冻结watch与索引定时器，轮转事件由测试直调

			// 第一次轮转：g1封进R1；新active先写快时钟一条（15:59:30）再回拨写（14:00:1x）。
			Files.move(logDir.resolve(Active), logDir.resolve("zeze.2026-09-28.log"));
			AtomicFileWriter.replace(logDir.resolve(Active), (buildLine(fastBase.plusSeconds(3570), "g2-0")
					+ buildLine(realBase.plusSeconds(10), "g2-1")
					+ buildLine(realBase.plusSeconds(40), "g2-2"))
					.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			invokeOnFileCreated(manager, logDir.resolve("zeze.2026-09-28.log"));
			// 第二次轮转：g2封进R2；新active首条=回拨后时刻（14:01:00起）。
			Files.move(logDir.resolve(Active), logDir.resolve("zeze.2026-09-29.log"));
			AtomicFileWriter.replace(logDir.resolve(Active), buildLines(realBase.plusSeconds(60), "g3-",
					0, 30).getBytes(java.nio.charset.StandardCharsets.UTF_8));
			invokeOnFileCreated(manager, logDir.resolve("zeze.2026-09-29.log"));

			var session = new Log4jSession(manager);
			try {
				var result = new ArrayList<Log4jLog>();
				var remain = session.searchContains(result, millis(realBase.plusSeconds(20)),
						millis(fastBase.plusSeconds(3540)), List.of("g"), BCondition.ContainsAll, 100);
				assertFalse(remain, "全部条目耗尽即查完");
				// 期望：R1的g1-0..g1-6（15:00..15:59:00）+R2的g2-2（14:00:40）+active的g3-0/g3-1。
				assertEquals(10, result.size(),
						"跨回归点窗口必须完整返回（修复前R2/active的窗口内三条静默漏读）");
				assertTrue(containsLog(result, "g2-2"), "R2中回归点之后的窗口内日志必须返回");
				assertTrue(containsLog(result, "g3-0"), "active的窗口内日志必须返回");
				assertTrue(containsLog(result, "g3-1"), "active的窗口内日志必须返回");
				assertFalse(containsLog(result, "g2-1"), "低于beginTime的14:00:10行不得混入（下界逐条过滤）");
				assertFalse(containsLog(result, "g2-0"), "超出endTime的15:59:30行不得混入（上界逐条过滤）");
			} finally {
				session.close();
			}
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/** 单文件回退夹具：active内容[15:59:30(pre), 14:00:10(after-a), 14:00:40(after-b)]。 */
	private static Log4jFileManager newSingleRollbackManager() throws Exception {
		var logDir = Files.createTempDirectory("log4j-endtime-rollback");
		AtomicFileWriter.replace(logDir.resolve(Active), (buildLines(BeforeRollback, "pre-rollback", 0)
				+ buildLine(AfterRollbackA, "after-rollback-a")
				+ buildLine(AfterRollbackB, "after-rollback-b"))
				.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		return newManager(logDir);
	}

	private static Log4jFileManager newManager(Path logDir) throws Exception {
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = Active;
		logConf.logDir = logDir.toString();
		return new Log4jFileManager(logConf);
	}

	private static String buildLine(LocalDateTime time, String tag) {
		return time.format(DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS")) + ' ' + tag + '\n';
	}

	private static String buildLines(LocalDateTime base, String prefix, long... offsetsSeconds) {
		var sb = new StringBuilder();
		var i = 0;
		for (var offset : offsetsSeconds)
			sb.append(buildLine(base.plusSeconds(offset), prefix + (i++)));
		return sb.toString();
	}

	private static long millis(LocalDateTime time) {
		return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	private static List<String> logsOf(List<Log4jLog> logs) {
		var names = new ArrayList<String>();
		for (var log : logs)
			names.add(log.getLog().substring(log.getLog().lastIndexOf(' ') + 1));
		return names;
	}

	private static boolean containsLog(List<Log4jLog> logs, String tag) {
		for (var log : logs)
			if (log.getLog().contains(tag))
				return true;
		return false;
	}

	private static void invokeOnFileCreated(Log4jFileManager manager, Path path) throws Exception {
		Method method = Log4jFileManager.class.getDeclaredMethod("onFileCreated", Path.class);
		method.setAccessible(true);
		method.invoke(manager, path);
	}
}
