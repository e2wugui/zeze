package Zeze.Services.Log4jQuery;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Builtin.LogService.BCondition;
import Zeze.Util.Task;

import harness.Fast;

/**
 * seek定位锚=前驱（floor）记录回归：索引是采样而非完备集（10s节拍+loadIndex批间
 * lastIndexTime攒满100条才推进），lowerBound锚（首条>=time的记录）使前驱与锚之间
 * 未入索引的日志（时间可>=查询time、物理位置在锚之前）永不被detailSeek读到——
 * beginTime落间隙的查询窗口头部静默漏读；SessionAll水位续扫的beginTime=已投递日志
 * 时间而非记录时间，几乎必落间隙，同漏。定位必须取前驱记录，从其数据位置起线性
 * 读、按time谓词推进（最坏多扫一个采样节拍段）。
 */
@Fast
@Extra
public class TestIndexGapBeginTimeSeeksFirstQualifyingLog {
	private static final LocalDateTime Base = LocalDateTime.of(2026, 3, 1, 10, 0);
	// 夹具行距1s（真实日志形态）：loadIndex批语义下批内每行都成记录（lastIndexTime批间
	// 固定），攒满100条flush后基线跳到批末记录时间，其后约一个10s节拍（行距1s即9行）
	// 未入索引——首条间隙=行100..108，行109是批2首条记录。行距>=10s的既有夹具索引
	// 稠密到每行一条，掩盖该形态。
	private static final int Lines = 130;
	private static final int GapFirst = 100;
	private static final int GapLast = 108;
	private static final int NextRecord = 109;

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * 四入口端到端：beginTime=间隙首行（行100，是日志时间而非记录时间——同
	 * SessionAll水位续扫形态）时，首条交付=行100、间隙9行全部可达。修复前
	 * lowerBound锚定位到行109，行100..108静默丢弃。
	 */
	@Test
	public void testFourEntriesDeliverGapLinesFromFirstQualifyingLog() throws Exception {
		var logDir = Files.createTempDirectory("gap-begintime");
		Files.write(logDir.resolve("zeze.log"), buildLines(Lines).getBytes(StandardCharsets.UTF_8));
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = "zeze.log";
		logConf.logDir = logDir.toString();
		var manager = new Log4jFileManager(logConf);
		try {
			manager.stop(); // 冻结监视与定时器：索引已由构造期loadIndex按批语义建好

			// 形态前置断言（用不随修复变化的lowerBound）：行100时间的首条>=记录
			// 是行109——即行100..108确实未入索引（间隙存在，否则本测试失去形态）。
			var index = manager.entryAt(0).index;
			assertEquals(offsetOf(NextRecord), index.lowerBound(millis(Base.plusSeconds(GapFirst))),
					"夹具索引必须存在间隙：行100..108未入索引");

			var beginTime = millis(Base.plusSeconds(GapFirst));
			var window = Lines - GapFirst; // 行100..129共30条
			var session = new Log4jSession(manager);
			try {
				// searchContains
				var search = new ArrayList<Log4jLog>();
				session.searchContains(search, beginTime, -1,
						List.of("never-match-word"), BCondition.ContainsNone, Integer.MAX_VALUE);
				assertWindowComplete("searchContains", search, window);

				// searchRegex（reset重定位到同一beginTime）
				session.reset();
				session.searchRegex(search, beginTime, -1, "msg-", Integer.MAX_VALUE);
				assertWindowComplete("searchRegex", search, window);

				// browseContains
				session.reset();
				var browse = new ArrayDeque<Log4jLog>();
				session.browseContains(browse, beginTime, -1,
						List.of("never-match-word"), BCondition.ContainsNone, Integer.MAX_VALUE, 0f);
				assertWindowComplete("browseContains", new ArrayList<>(browse), window);

				// browseRegex
				session.reset();
				session.browseRegex(browse, beginTime, -1, "msg-", Integer.MAX_VALUE, 0f);
				assertWindowComplete("browseRegex", new ArrayList<>(browse), window);
			} finally {
				session.close();
			}
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/** 首条=窗口首行（间隙首行）、间隙末行在内、条数齐全。 */
	private static void assertWindowComplete(String entry, List<Log4jLog> result, int expected) {
		assertEquals(expected, result.size(), entry + "：窗口内条数");
		assertTrue(result.get(0).getLog().endsWith("msg-" + pad(GapFirst)),
				entry + "：首条交付必须是第一条time>=beginTime的日志（间隙首行），不是首条索引记录");
		assertTrue(result.get(GapLast - GapFirst).getLog().endsWith("msg-" + pad(GapLast)),
				entry + "：间隙末行（行108）必须交付");
		assertTrue(result.get(expected - 1).getLog().endsWith("msg-" + pad(Lines - 1)),
				entry + "：窗口末行不断");
	}

	/**
	 * 定位锚边界（手工部分索引，首条记录不在文件头）：恰等于记录时间=该记录行
	 * （既有行为）；间隙内多行=从间隙内首条>=time的日志起；早于首条记录=文件头
	 * 线性扫描（首条记录之前仍有窗口内日志）；超出末端=末记录续扫（既有尾窗语义）。
	 */
	@Test
	public void testFloorAnchorBoundaries() throws Exception {
		var logDir = Files.createTempDirectory("gap-begintime-boundary");
		try {
			var logFile = logDir.resolve("zeze.log");
			Files.write(logFile, buildLines(300).getBytes(StandardCharsets.UTF_8));
			// 记录：行50、行60（首条记录前有49行数据，间隙=行51..59）。
			var index = new LogIndex(logDir.resolve("manual.index").toFile());
			index.addIndex(List.of(
					LogIndex.Record.of(millis(Base.plusSeconds(50)), offsetOf(50)),
					LogIndex.Record.of(millis(Base.plusSeconds(60)), offsetOf(60))));

			try (var session = new Log4jFileSession(logFile.toFile(), index, "utf-8", "yy-MM-dd HH:mm:ss.SSS")) {
				// 恰等于索引记录时间：定位到该记录行（锚=该记录本身）。
				assertTrue(session.seek(millis(Base.plusSeconds(60))));
				assertEquals(offsetOf(60), session.current().getOffset(), "恰等于记录时间：首条=该记录行");

				// 间隙内（多行间隙，取行55的时间）：首条=行55，不是行60。
				assertTrue(session.seek(millis(Base.plusSeconds(55))));
				assertEquals(offsetOf(55), session.current().getOffset(), "间隙内：首条=间隙内首条>=time的日志");

				// 间隙首行（行51时间）：首条=行51。
				assertTrue(session.seek(millis(Base.plusSeconds(51))));
				assertEquals(offsetOf(51), session.current().getOffset(), "间隙首行：首条=行51");

				// 早于首条记录（行10时间）：无前驱记录，从文件头扫描定位，首条=行10。
				assertTrue(session.seek(millis(Base.plusSeconds(10))));
				assertEquals(offsetOf(10), session.current().getOffset(), "早于首条记录：文件头线性定位");

				// 超出索引末端（行250时间）：末记录起续扫（既有尾窗语义不回归）。
				assertTrue(session.seek(millis(Base.plusSeconds(250))));
				assertEquals(offsetOf(250), session.current().getOffset(), "超出末端：从未记录续扫");
			}
		} finally {
			deleteBestEffort(logDir);
		}
	}

	private static String pad(int i) {
		return String.format("%04d", i);
	}

	private static long offsetOf(int lineIndex) {
		return (long)lineIndex * lineBytes();
	}

	private static int lineBytes() {
		var first = buildLine(0);
		return first.getBytes(StandardCharsets.UTF_8).length + 1; // +LF
	}

	private static String buildLine(int i) {
		return Base.plusSeconds(i).format(DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS"))
				+ " msg-" + pad(i);
	}

	private static String buildLines(int count) {
		var sb = new StringBuilder();
		for (var i = 0; i < count; ++i)
			sb.append(buildLine(i)).append('\n');
		return sb.toString();
	}

	private static long millis(LocalDateTime time) {
		return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
}
