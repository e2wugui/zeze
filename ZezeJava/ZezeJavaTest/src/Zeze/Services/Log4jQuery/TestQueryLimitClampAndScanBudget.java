package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static harness.DirCleanup.deleteBestEffort;

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
import Zeze.Services.Log4jQuery.Log4jLog;
import Zeze.Services.Log4jQuery.Log4jSession;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.Task;

import harness.Fast;

/**
 * GD-D04回归：服务端Browse/Search无执行上限，limit客户端可控且无上限（裸int协议字段）。
 * 修复（方案A，手写层零协议依赖）：服务端入口clampLimit截到MAX_LIMIT；四个查询循环带
 * 扫描条数/字节预算，超预算置Remain=true提前返回，客户端按既有翻页协议继续（不丢不重）。
 * LogService的Browse/Search handler层clamp为代码审读验证（需活服务端），
 * 这里直测Log4jSession.clampLimit与预算截断的翻页语义。
 * 注：字节预算（MAX_SCAN_BYTES=256MB）需同量级fixture，行为不测，常量断言记档。
 */
@Fast
public class TestQueryLimitClampAndScanBudget {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * limit clamp：超上限静默截断到MAX_LIMIT，未超与边界值原样通过。
	 */
	@Test
	public void testClampLimit() {
		// 修复前：limit=Integer.MAX_VALUE直达查询循环，按匹配总数分配内存。
		assertEquals(Log4jSession.MAX_LIMIT, Log4jSession.clampLimit(Integer.MAX_VALUE));
		assertEquals(Log4jSession.MAX_LIMIT, Log4jSession.clampLimit(Log4jSession.MAX_LIMIT + 1));
		assertEquals(Log4jSession.MAX_LIMIT, Log4jSession.clampLimit(Log4jSession.MAX_LIMIT));
		assertEquals(123, Log4jSession.clampLimit(123));
		// 预算常量记档：本测试的条数预算行为依赖MAX_SCAN_LOGS=10万。
		assertEquals(100_000, Log4jSession.MAX_SCAN_LOGS);
		assertEquals(256L * 1024 * 1024, Log4jSession.MAX_SCAN_BYTES);
	}

	/**
	 * 扫描预算截断：无匹配的宽条件扫描在MAX_SCAN_LOGS条处置Remain=true提前返回
	 * （修复前扫到EOF返回false），后续翻页从下一条继续，条目不丢不重。
	 */
	@Test
	public void testScanBudgetRemainAndPagination() throws Exception {
		var total = Log4jSession.MAX_SCAN_LOGS + 10;
		var logDir = Files.createTempDirectory("fnd19-gdd04-budget");
		var base = LocalDateTime.now().minusMinutes(30);
		var sb = new StringBuilder((int)(total * 32L));
		for (var i = 1; i <= total; ++i)
			sb.append(base.plusNanos(i * 1_000_000L)
					.format(DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS")))
					.append(' ').append(String.format("msg-%06d", i)).append('\n');
		Files.write(logDir.resolve("zeze.log"), sb.toString().getBytes(StandardCharsets.UTF_8));

		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = "zeze.log";
		logConf.logDir = logDir.toString();
		var manager = new Log4jFileManager(logConf);
		try {
			manager.stop(); // 冻结监视与定时器，行为只由会话扫描决定
			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();

			// 页1：永不匹配的词，limit满额也到不了——只能靠扫描预算截断。
			var remain = session.searchContains(result, -1, -1,
					List.of("never-match-word"), BCondition.ContainsAll, Log4jSession.MAX_LIMIT);
			assertTrue(remain, "扫描条数超预算应置Remain=true提前返回（修复前扫到EOF返回false）");
			assertTrue(result.isEmpty());

			// 页2：从预算截断点继续，前5条恰为第100001..100005条（截断不丢不重）。
			remain = session.searchContains(result, -1, -1,
					List.of("zzz-none"), BCondition.ContainsNone, 5);
			assertTrue(remain, "仍有剩余条目，Remain应为true");
			assertEquals(5, result.size());
			assertTrue(result.get(0).getLog().endsWith("msg-100001"), "预算截断后翻页应从下一条继续");
			assertTrue(result.get(4).getLog().endsWith("msg-100005"));

			// 页3：余下5条取完，Remain=false，末条恰为最后一条。
			remain = session.searchContains(result, -1, -1,
					List.of("zzz-none"), BCondition.ContainsNone, 100);
			assertTrue(!remain);
			assertEquals(5, result.size());
			assertTrue(result.get(0).getLog().endsWith("msg-100006"));
			assertTrue(result.get(4).getLog().endsWith(String.format("msg-%06d", total)));
			session.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}
}
