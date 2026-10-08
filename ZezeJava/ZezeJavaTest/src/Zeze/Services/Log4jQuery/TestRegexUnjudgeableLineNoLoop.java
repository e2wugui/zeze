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
import java.util.ArrayDeque;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.Log4jLog;
import Zeze.Services.Log4jQuery.Log4jSession;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.Task;

import harness.Fast;

/**
 * FND26 log4j-03 回归：单条日志在整页正则预算内不可判定时，翻页不得死循环。
 * 两个触发面同治：
 * 1. 病态回溯pattern（短行引爆64M charAt预算）——暂存重判每页得到同一结局，
 *    修复前：中止→暂存→重判→再中止，游标永久卡死，客户端续页恒"空结果+Remain=true"；
 * 2. 超长单行（&gt;64M字符）+通读型pattern——同机理（截断修复后此面不再触预算中止）。
 * 修复=行级正则输入截断（{@link Log4jSession#MAX_REGEX_LOG_CHARS}）+ 整页预算独占
 * 仍判不完的行弃置前进（warn留痕，匹配语义=不可判定即不命中）。
 */
@Fast
@Extra
public class TestRegexUnjudgeableLineNoLoop {
	private static final String Active = "zeze.log";
	private static final LocalDateTime Base = LocalDateTime.of(2026, 9, 28, 10, 0);
	/** 20 个 (a+) 组+尾 b：对 60 个 a 产生组合级回溯（实测依据见 TestRegexBudgetPendingNextHandoff）。 */
	private static final String PathologicalPattern = "(a+)".repeat(20) + "b";

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	private static long millis(LocalDateTime time) {
		return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	private static String timestamped(LocalDateTime time, String suffix) {
		return time.format(DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS")) + " " + suffix;
	}

	private static Log4jFileManager newManager(Path logDir, String content) throws Exception {
		Files.write(logDir.resolve(Active), content.getBytes(StandardCharsets.UTF_8));
		var conf = new LogServiceConf.LogConf();
		conf.logActive = Active;
		conf.logDir = logDir.toString();
		return new Log4jFileManager(conf);
	}

	/** 两行：首行 keyword1+60个a（回溯载体），次行 keyword2。 */
	private static String twoLineFixture() {
		return timestamped(Base, "keyword1 " + "a".repeat(60)) + "\n"
				+ timestamped(Base.plusSeconds(30), "keyword2") + "\n";
	}

	// 病态回溯面：同pattern翻页必须在有界页数内收敛（修复前每页都暂存重判同一行，恒Remain=true）。
	// 收敛形态：页1首判中止暂存（既有语义）；页2重判仍中止→弃置该行、同页后续行交下页
	//（弃置行烧满本页正则预算）；页3判定剩余行并读尽。
	@Test
	public void testSearchRegexSamePathologicalPatternPagesConverge() throws Exception {
		var logDir = Files.createTempDirectory("log4j-regex-backtrack");
		var manager = newManager(logDir, twoLineFixture());
		try {
			manager.stop(); // 冻结监视与定时器：行为只由会话扫描决定
			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();

			// 页1：首行判定的charAt预算引爆，暂存首行，Remain=true（既有语义不变）。
			var remain = session.searchRegex(result, millis(Base), -1, PathologicalPattern, 100);
			assertTrue(remain, "首判预算中止应置Remain=true");
			assertTrue(result.isEmpty());

			// 同pattern续页：必须推进收敛（修复前恒Remain=true+空结果=死循环）。
			var pages = 0;
			while (remain && ++pages <= 4)
				remain = session.searchRegex(result, millis(Base), -1, PathologicalPattern, 100);
			assertFalse(remain, "同pattern翻页必须在有界页数内读尽（修复前永不收敛）");
			assertTrue(result.isEmpty());

			// 稳态：无暂存残留，立即false。
			remain = session.searchRegex(result, millis(Base), -1, PathologicalPattern, 100);
			assertFalse(remain);
			assertTrue(result.isEmpty());
			session.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	// browse路径同治：同pattern续页有界收敛。
	@Test
	public void testBrowseRegexSamePathologicalPatternPagesConverge() throws Exception {
		var logDir = Files.createTempDirectory("log4j-regex-browse");
		var manager = newManager(logDir, twoLineFixture());
		try {
			manager.stop();
			var session = new Log4jSession(manager);
			var result = new ArrayDeque<Log4jLog>();

			var remain = session.browseRegex(result, millis(Base), -1, PathologicalPattern, 100, 0.0f);
			assertTrue(remain, "页1首判预算中止Remain=true");

			var pages = 0;
			while (remain && ++pages <= 4)
				remain = session.browseRegex(result, millis(Base), -1, PathologicalPattern, 100, 0.0f);
			assertFalse(remain, "同pattern翻页必须在有界页数内收敛（修复前永不收敛）");
			session.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	// 超长行面（>64M字符+通读型pattern）：截断后单页即可判定读尽——修复前页1即预算中止。
	@Test
	public void testOversizeLineJudgedViaTruncation() throws Exception {
		var logDir = Files.createTempDirectory("log4j-regex-oversize");
		// 首行=合法日志头；第二行无时间戳前缀=续行，聚合出 >64M 字符的单条日志。
		var content = timestamped(Base, "keyword1") + "\n"
				+ "y".repeat(64 * 1024 * 1024 + 64) + "\n";
		var manager = newManager(logDir, content);
		try {
			manager.stop();
			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();

			// 通读型pattern（不存在的字面量）：截断为8M前缀参与匹配，单页判定完成并读尽。
			// 修复前：64M+字符全文扫描耗尽64M预算→中止暂存→Remain=true（重判死循环同上）。
			var remain = session.searchRegex(result, millis(Base), -1, "zzz_absent_never_match", 100);
			assertFalse(remain, "超长行截断判定后单页读尽（修复前Remain=true）");
			assertTrue(result.isEmpty());
			session.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	// 正向锁定：超长行（>8M字符，触发截断）+命中前缀的pattern同样单页收敛不循环；命中行因
	// 超页字节预算（PAGE_RESULT_BYTES_BUDGET）不可送达而被跳过（warn）——截断保留的是
	// "判定参与"语义（可判定=可推进），送达仍受页预算约束，两者正交。
	@Test
	public void testOversizeLineWithPrefixMatchConverges() throws Exception {
		var logDir = Files.createTempDirectory("log4j-regex-prefix");
		// 聚合单条=首行(keyword1)+>8M字符续行；keyword1位于前缀内，截断后判定命中。
		var content = timestamped(Base, "keyword1") + "\n" + "z".repeat(9 * 1024 * 1024) + "\n";
		var manager = newManager(logDir, content);
		try {
			manager.stop();
			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();

			var remain = session.searchRegex(result, millis(Base), -1, "keyword1", 100);
			assertTrue(result.isEmpty(), "命中行超页字节预算不可送达：跳过（warn），不进结果");
			assertFalse(remain, "判定完成即推进读尽，不循环");
			session.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}
}
