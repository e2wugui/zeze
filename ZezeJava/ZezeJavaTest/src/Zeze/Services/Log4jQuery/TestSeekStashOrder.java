package Zeze.Services.Log4jQuery;

import harness.Extra;
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
 * FND24 log4j 审视波守卫：Log4jFileSession.seek(offset,time) 不清 nextNextMaybePartLog
 * 残留 stash——与已修的 reset() 残留（8e7d3eb86）同构，但发生在**主查询定位路径**：
 * manager.seek 每次"新构造 session（构造即预读第2条入stash）+ 立即 seek(索引offset)"，
 * offset 落在 stash 位置之前（查文件头/全量窗）时，tryNext 把 stash 旧条先返回、
 * raf 从 offset 重读的条后返回——迭代乱序+重复（两行文件实测 [L2, L1, L2]）。
 * 8e7d3eb86 修复说明中"seek(long,long) 整体换会话路径无症状"的判断漏了
 * "新会话构造自身预读填 stash"这一环。定位点晚于 stash 位置的查询由 detailSeek
 * 推进吸收错序（自愈），故既有端到端测试未暴露。
 */
@Fast
@Extra
public class TestSeekStashOrder {
	private static final String Active = "zeze.log";
	private static final LocalDateTime Base = LocalDateTime.of(2026, 9, 28, 10, 0);

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	private static String buildLines(int count) {
		var fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
		var sb = new StringBuilder();
		for (var i = 0; i < count; ++i)
			sb.append(Base.plusSeconds(30L * i).format(fmt)).append(' ').append("msg-").append(i).append('\n');
		return sb.toString();
	}

	private static Log4jFileManager newManager(Path logDir, int count) throws Exception {
		Files.write(logDir.resolve(Active), buildLines(count).getBytes(StandardCharsets.UTF_8));
		var conf = new LogServiceConf.LogConf();
		conf.logActive = Active;
		conf.logDir = logDir.toString();
		return new Log4jFileManager(conf);
	}

	private static long headMillis() {
		return Base.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	/**
	 * 带beginTime的定位查询（walker.seek→manager.seek→新session.seek）：文件头时间窗
	 * 必须从首条开始有序返回，不得乱序/重复。修复前：构造预读的msg-1（stash）先于
	 * 定位点msg-0返回，且msg-1经raf重读重复一次（[msg-1, msg-0, msg-1]形态）。
	 */
	@Test
	public void testSeekFromFileHeadKeepsOrder() throws Exception {
		var logDir = Files.createTempDirectory("log4j-seek-stash");
		var manager = newManager(logDir, 3);
		try {
			manager.stop();
			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();

			// beginTime=首条时间：走manager.seek索引定位分支（lowerBound命中首记录msg-0，
			// offset=0早于构造stash=msg-1）——修复前session.seek不清stash，msg-1先返回+重复。
			var remain = session.searchContains(result, headMillis(), -1, List.of("msg"), BCondition.ContainsAll, 100);
			assertTrue(!remain);
			assertEquals(3, result.size(), "全量窗应返回全部3条");
			for (var i = 0; i < 3; ++i)
				assertTrue(result.get(i).getLog().endsWith("msg-" + i),
						"顺序必须有序且不重复，实际: " + result.stream().map(Log4jLog::getLog).toList());
			session.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}
}
