package Zeze.Services.Log4jQuery;

import harness.Extra;
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
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.Task;

import harness.Fast;

/**
 * FND22 GD-C01回归：reset=true且beginTime未变时trySetBeginTime以beginTime去重短路跳过重定位——
 * reset把游标归零到最旧文件头后，同beginTime的刷新请求（监控端固定时间窗刷新是reset=true的典型
 * 用法）从文件头迭代，早于beginTime的日志混入结果（违反查询契约）且随历史增长全量线性重扫。
 * 修复：Log4jSession.reset()同时失效beginTime（-2）——去重状态必须随游标一起失效，下一查询
 * 必走reset+seek重定位。DriverFnd22Reset复刻为标准用例。
 */
@Fast
@Extra
public class TestResetSameBeginTimeRelocates {
	private static final String Active = "zeze.log";
	private static final LocalDateTime Base = LocalDateTime.of(2026, 1, 1, 10, 0);

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * 缺陷主链：两次同beginTime的reset查询结果必须一致。修复前req2从文件头迭代返回L1..L5
	 *（含早于beginTime的L1、L2），且是全历史重扫形态。
	 */
	@Test
	public void testResetRefreshKeepsBeginTimeWindow() throws Exception {
		var logDir = Files.createTempDirectory("reset-relocate");
		AtomicFileWriter.replace(logDir.resolve(Active), buildLines(Base, "L", 5).getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			var t3 = millis(Base.plusSeconds(60)); // L2的时间（30s递增）：其后L2..L4共3条
			var session = new Log4jSession(manager);
			try {
				var result = new ArrayList<Log4jLog>();
				// req1：首请求（beginTime -2→T3），LogService的isReset分支先reset()。
				session.reset();
				assertFalse(session.searchContains(result, t3, -1, List.of("zzz"), BCondition.ContainsNone, 100));
				assertLogs(result, 2, 4, "req1应返回L2..L4（T3之后的3条）");

				// req2：同条件刷新（reset=true、beginTime仍为T3）——修复前返回L0..L4。
				result.clear();
				session.reset();
				assertFalse(session.searchContains(result, t3, -1, List.of("zzz"), BCondition.ContainsNone, 100));
				assertLogs(result, 2, 4, "req2（reset刷新）应与req1一致：L2..L4，不得混入早于beginTime的L0、L1");
			} finally {
				session.close();
			}
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 兼容钉子：reset失效beginTime不破坏beginTime=-1流程（-2→-1变化，reset后不seek，从头查）
	 * 与非reset翻页的beginTime去重（续页不重扫）。
	 */
	@Test
	public void testBeginTimeMinusOneAndPaginationUnchanged() throws Exception {
		var logDir = Files.createTempDirectory("reset-compat");
		AtomicFileWriter.replace(logDir.resolve(Active), buildLines(Base, "L", 5).getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			var session = new Log4jSession(manager);
			try {
				var result = new ArrayList<Log4jLog>();
				// reset+beginTime=-1：从头查，limit=2留remainder。
				session.reset();
				assertTrue(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 2));
				assertLogs(result, 0, 1, "reset+beginTime=-1从头查（首页2条）");

				// 非reset续页（beginTime未变去重短路，不重新seek）：接续返回L2..L4。
				assertFalse(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 10));
				assertLogs(result, 2, 4, "续页从上次位置继续");

				// 再次reset+beginTime=-1：重新从头查全量。
				session.reset();
				assertFalse(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 10));
				assertLogs(result, 0, 4, "reset后重新从头查");
			} finally {
				session.close();
			}
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	private static void assertLogs(List<Log4jLog> result, int firstIndex, int lastIndex, String message) {
		assertEquals(lastIndex - firstIndex + 1, result.size(), message + "（条数）");
		for (var i = 0; i < result.size(); ++i) {
			var marker = "L" + String.format("%02d", firstIndex + i);
			assertTrue(result.get(i).getLog().contains(marker), message + "：第" + i + "条应含" + marker);
			assertEquals(millis(Base.plusSeconds(30L * (firstIndex + i))), result.get(i).getTime(),
					message + "（时间序与内容序一致）");
		}
	}

	private static Log4jFileManager newManager(Path logDir) throws Exception {
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = Active;
		logConf.logDir = logDir.toString();
		return new Log4jFileManager(logConf);
	}

	private static String buildLines(LocalDateTime base, String prefix, int count) {
		var fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
		var sb = new StringBuilder();
		for (var i = 0; i < count; ++i)
			sb.append(base.plusSeconds(30L * i).format(fmt)).append(' ')
					.append(prefix).append(String.format("%02d", i)).append('\n');
		return sb.toString();
	}

	private static long millis(LocalDateTime time) {
		return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
}
