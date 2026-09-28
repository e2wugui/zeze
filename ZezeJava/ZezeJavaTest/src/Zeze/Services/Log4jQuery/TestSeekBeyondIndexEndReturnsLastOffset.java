package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static harness.DirCleanup.deleteBestEffort;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.Log4jFileSession;
import Zeze.Services.Log4jQuery.LogIndex;
import Zeze.Util.Task;

import harness.Fast;

/**
 * FND22 GD-C02回归：getIndexOffset对time>endTime（lowerBound=-1）回落offset 0——索引明明覆盖到
 * endTime，尾窗查询（时间落在buildIndex 5分钟周期留下的滞后带内，恰是监控端"最近N分钟"最高频
 * 形状）却从文件头全量线性扫描；GD-D04的扫描预算只约束seek之后的结果循环，对seek内部的
 * detailSeek零约束。修复：索引非空且time>endTime时返回末记录offset（detailSeek只线性推进未索引
 * 尾部）；空索引维持回落0。DriverFnd22SeekLink partA的反射直证复刻为标准用例。
 */
@Fast
public class TestSeekBeyondIndexEndReturnsLastOffset {
	private static final LocalDateTime Base = LocalDateTime.of(2026, 3, 1, 10, 0);
	// 定长行（ASCII，29B/行）：21B时间头 + 空格 + 6B消息("m00000") + LF，offset=i*29精确可算。
	private static final int LineBytes = 29;
	private static final int Lines = 6000;
	private static final int IndexedTo = 1000; // 模拟索引只覆盖到前1000行（endTime滞后带）

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testTailSeekStartsFromLastIndexRecord() throws Exception {
		var logDir = Files.createTempDirectory("fnd22-gdc02-tail");
		try {
			var logFile = logDir.resolve("zeze.log");
			Files.write(logFile, buildLines(Lines).getBytes(StandardCharsets.UTF_8));
			var index = buildPartialIndex(logDir.resolve("zeze.log.index"));

			try (var session = new Log4jFileSession(logFile.toFile(), index, "utf-8", "yy-MM-dd HH:mm:ss.SSS")) {
				// 覆盖内（既有行为钉子）：lowerBound命中，返回该记录offset。
				assertEquals(offsetOf(500), getIndexOffset(session, millis(Base.plusSeconds(500))),
						"覆盖内查询返回lowerBound记录offset（既有行为）");

				// 修复点：尾窗（time>endTime=第1000行时间）从末记录offset续扫，不回落0。
				// 修复前返回0（读穿全文件只为定位尾部几行）。
				assertEquals(offsetOf(IndexedTo), getIndexOffset(session, millis(Base.plusSeconds(5000))),
						"尾窗查询应从未记录offset续扫，不得回落0整读已索引区间");

				// 端到端正确性：seek定位到首条time≥查询时间的日志（第5000行），与全量扫描结果一致。
				assertTrue(session.seek(millis(Base.plusSeconds(5000))), "尾窗seek应命中");
				var current = session.current();
				assertEquals(offsetOf(5000), current.getOffset(), "定位在第5000行（首条≥查询时间）");
				assertEquals(millis(Base.plusSeconds(5000)), current.getTime());
				assertTrue(current.getLog().contains("m05000"));
			}

			// 空索引钉子：无记录时维持回落0（文件本来就要从头扫）。
			var emptyIndex = new LogIndex(logDir.resolve("empty.index").toFile());
			try (var session = new Log4jFileSession(logFile.toFile(), emptyIndex, "utf-8", "yy-MM-dd HH:mm:ss.SSS")) {
				assertEquals(0L, getIndexOffset(session, millis(Base.plusSeconds(5000))), "空索引维持回落0");
			}
		} finally {
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 末记录offset之后仍有未索引数据时，seek只推进未索引尾部：结果与全量扫描一致
	 * （detailSeek从末记录向前校验时间，与covered路径同一容忍度）。
	 */
	@Test
	public void testTailSeekResultsMatchFullScan() throws Exception {
		var logDir = Files.createTempDirectory("fnd22-gdc02-match");
		try {
			var logFile = logDir.resolve("zeze.log");
			Files.write(logFile, buildLines(Lines).getBytes(StandardCharsets.UTF_8));
			var index = buildPartialIndex(logDir.resolve("zeze.log.index"));

			var fromTail = new ArrayList<String>();
			try (var session = new Log4jFileSession(logFile.toFile(), index, "utf-8", "yy-MM-dd HH:mm:ss.SSS")) {
				assertTrue(session.seek(millis(Base.plusSeconds(5010))));
				while (session.hasNext() && session.current().getTime() <= millis(Base.plusSeconds(5019)))
					fromTail.add(session.next().getLog());
			}

			var fromHead = new ArrayList<String>();
			try (var noIndex = new Log4jFileSession(logFile.toFile(), null, "utf-8", "yy-MM-dd HH:mm:ss.SSS")) {
				noIndex.seek(millis(Base.plusSeconds(5010))); // 无索引：从头线性定位（旧行为等价路径）
				while (noIndex.hasNext() && noIndex.current().getTime() <= millis(Base.plusSeconds(5019)))
					fromHead.add(noIndex.next().getLog());
			}
			assertEquals(10, fromTail.size(), "第5010..5019行共10条");
			assertEquals(fromHead, fromTail, "尾窗seek结果与全量扫描逐字节一致");
		} finally {
			deleteBestEffort(logDir);
		}
	}

	/** 手工部分索引：第0,100,...,IndexedTo行各一条记录（模拟buildIndex周期滞后）。 */
	private static LogIndex buildPartialIndex(Path indexFile) throws Exception {
		var index = new LogIndex(indexFile.toFile());
		var records = new ArrayList<LogIndex.Record>();
		for (var line = 0; line <= IndexedTo; line += 100)
			records.add(LogIndex.Record.of(millis(Base.plusSeconds(line)), offsetOf(line)));
		index.addIndex(records);
		return index;
	}

	private static long offsetOf(int lineIndex) {
		return (long)lineIndex * LineBytes;
	}

	private static long getIndexOffset(Log4jFileSession session, long time) throws Exception {
		Method method = Log4jFileSession.class.getDeclaredMethod("getIndexOffset", long.class);
		method.setAccessible(true);
		return (Long)method.invoke(session, time);
	}

	private static String buildLines(int count) {
		var fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
		var sb = new StringBuilder();
		for (var i = 0; i < count; ++i)
			sb.append(Base.plusSeconds(i).format(fmt)).append(' ')
					.append("m").append(String.format("%05d", i)).append('\n');
		return sb.toString();
	}

	private static long millis(LocalDateTime time) {
		return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
}
