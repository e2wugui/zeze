package TestLog4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * GD-C01回归：Log4jFileSession.reset()未清空nextNextMaybePartLog残留stash。
 * 翻页在文件中部截断（stash非空，需截断点之后还有≥2条日志）后beginTime=-1从头重查：
 * 修复前stash被当作第一条返回且结尾重复（DriverReset实测L5,L1,L2,L3,L4,L5），多行日志续行丢失；
 * 修复后严格L1..L5、续行完整。
 */
@Fast
public class TestFnd19ResetStash {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testResetDropsPrefetchedStash() throws Exception {
		var logDir = Files.createTempDirectory("fnd19-reset-stash");
		var times = new long[5];
		var manager = newManager(logDir, times);
		try {
			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();

			// phase1：beginTime=第3条时间、limit=1截断在中部：nextLog=L4、stash=L5（非空是触发前提）。
			assertTrue(session.searchContains(result, times[2], -1, List.of("zzz"), BCondition.ContainsNone, 1));
			assertEquals(1, result.size());
			assertTrue(result.get(0).getLog().contains("L3"));

			// phase2：beginTime=-1从头重查，断言顺序与条数。
			result.clear();
			assertFalse(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 100));
			assertEquals(5, result.size(), "从头重查应恰有5条（修复前首条为L5且结尾重复共6条）");
			var expect = new String[] {"L1", "L2", "L3", "L4", "L5"};
			for (var i = 0; i < 5; ++i) {
				// 首行=时间戳+空格+标记（时间戳自身含空格，不能按空格split取标记）。
				var firstLine = result.get(i).getLog().split("\n")[0];
				assertTrue(firstLine.endsWith(" " + expect[i]), "第" + i + "条应为" + expect[i] + "，实际: " + firstLine);
			}
			// 多行日志续行必须完整（修复前stash直返路径丢续行）。
			assertTrue(result.get(2).getLog().contains("L3-cont"));
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/** 写5条日志（L3带续行）并返回各条时间戳，保证断言用的beginTime与文件内容同源。 */
	private static Log4jFileManager newManager(Path logDir, long[] times) throws Exception {
		var base = LocalDateTime.now().minusMinutes(10);
		var fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
		var sb = new StringBuilder();
		for (var i = 0; i < 5; ++i) {
			var time = base.plusSeconds(30L * i);
			times[i] = time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
			sb.append(time.format(fmt)).append(' ').append('L').append(i + 1).append('\n');
			if (i == 2)
				sb.append("L3-cont\n");
		}
		Files.write(logDir.resolve("zeze.log"), sb.toString().getBytes(StandardCharsets.UTF_8));

		var logConf = new LogServiceConf.LogConf();
		logConf.logDir = logDir.toString();
		logConf.logActive = "zeze.log";
		return new Log4jFileManager(logConf);
	}
}
