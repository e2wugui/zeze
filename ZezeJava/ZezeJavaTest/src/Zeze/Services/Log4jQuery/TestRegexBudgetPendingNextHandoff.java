package Zeze.Services.Log4jQuery;

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
import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.Log4jLog;
import Zeze.Services.Log4jQuery.Log4jSession;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.Task;

import harness.Fast;

/**
 * FND24 log4j-06 审视波守卫：正则预算中止的暂存条（pendingNext）必须被同会话的**所有**
 * 后续查询路径消费——Search/Browse 按请求内容在 words(contains)/pattern(regex) 间路由，
 * 同 sid 会话上 regex 页预算中止后紧跟 contains 续查是协议完全可达的形态。
 * 暂存机制若只接线 regex 路径（53639916e 首版形态），contains 循环直接走 walker.next()
 * 会越过已被取出的暂存条——该条日志静默漏出结果（不丢不重被破坏）。
 *
 * fixture：单文件两行，首行含 60 个连续 'a'（承载回溯）+ 独立关键词；次行 keyword2。
 * 病态 pattern：20 个 (a+) 捕获组+尾 b（对 60 个 a 的歧义分割组合爆炸；JDK 对 (a+)+ 单组
 * 嵌套有 GroupCurly 记忆化缓解，实测 60a 仅数千次 charAt，多捕获组无跨组记忆化，实测
 * jshell >70M charAt 确定性引爆 64M 预算，~300ms）。
 */
@Fast
public class TestRegexBudgetPendingNextHandoff {
	private static final String Active = "zeze.log";
	private static final LocalDateTime Base = LocalDateTime.of(2026, 9, 28, 10, 0);
	/** 20 个 (a+) 组+尾 b：对 60 个 a 产生组合级回溯（见类注释实测依据）。 */
	private static final String PathologicalPattern = "(a+)".repeat(20) + "b";

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	private static long millis(LocalDateTime time) {
		return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	/** 两行：首行 keyword1+60个a（回溯载体），次行 keyword2。 */
	private static String buildLines() {
		var fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
		return Base.format(fmt) + " keyword1 " + "a".repeat(60) + "\n"
				+ Base.plusSeconds(30).format(fmt) + " keyword2\n";
	}

	private static Log4jFileManager newManager(Path logDir) throws Exception {
		Files.write(logDir.resolve(Active), buildLines().getBytes(StandardCharsets.UTF_8));
		var conf = new LogServiceConf.LogConf();
		conf.logActive = Active;
		conf.logDir = logDir.toString();
		return new Log4jFileManager(conf);
	}

	@Test
	public void testSearchContainsConsumesPendingNext() throws Exception {
		var logDir = Files.createTempDirectory("log4j-pending-search");
		var manager = newManager(logDir);
		try {
			manager.stop(); // 冻结监视与定时器：行为只由会话扫描决定
			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();

			// 页1：regex 预算中止——首行 find() 指数回溯引爆 64M charAt 预算，暂存首行并返回部分结果。
			var remain = session.searchRegex(result, millis(Base), -1, PathologicalPattern, 100);
			assertTrue(remain, "预算中止应置Remain=true");
			assertTrue(result.isEmpty(), "无匹配行，中止前不应有结果");

			// 页2：同会话改用 words 续查（同 beginTime 去重短路、不 reset）——contains 路径必须
			// 先消费暂存的首行（含 keyword1）再前进。首版形态（contains 直走 walker.next()）跳过
			// 暂存条，keyword1 静默漏出结果。
			remain = session.searchContains(result, millis(Base), -1,
					List.of("keyword1"), BCondition.ContainsAll, 100);
			assertEquals(1, result.size(), "暂存的首行必须被 contains 重判（修复前被静默跳过=0条）");
			assertTrue(result.get(0).getLog().contains("keyword1"));
			assertTrue(!remain, "文件已读尽");

			// 次行不丢不重：contains 已消费暂存后顺序前进，keyword2 仍可查到。
			result.clear();
			session.reset();
			remain = session.searchContains(result, millis(Base), -1,
					List.of("keyword2"), BCondition.ContainsAll, 100);
			assertEquals(1, result.size());
			assertTrue(!remain);
			session.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	@Test
	public void testBrowseContainsConsumesPendingNext() throws Exception {
		var logDir = Files.createTempDirectory("log4j-pending-browse");
		var manager = newManager(logDir);
		try {
			manager.stop();
			var session = new Log4jSession(manager);
			var result = new ArrayDeque<Log4jLog>();

			// 页1：browseRegex 预算中止，暂存首行。
			var remain = session.browseRegex(result, millis(Base), -1, PathologicalPattern, 100, 0.0f);
			assertTrue(remain, "预算中止应置Remain=true");

			// 页2：同会话 browseContains 续查——暂存首行必须被重判（locate 命中 keyword1）。
			// browse语义：locate后无条件收集limit条——两行全收（首条=定位点line1）。
			// 修复前形态：暂存被跳过、locate落在line2不成立，仅剩offset窗口的1条keyword2。
			remain = session.browseContains(result, millis(Base), -1,
					List.of("keyword1"), BCondition.ContainsAll, 100, 0.0f);
			assertEquals(2, result.size(), "locate于暂存首行后应收集全部2条（修复前=1条keyword2）");
			assertTrue(result.getFirst().getLog().contains("keyword1"), "首条必须是被重判命中的暂存行");
			assertTrue(!remain, "文件已读尽");
			session.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 正向锁定（修复前后均应绿）：regex 预算中止后的**regex** 续页重判不丢不重——
	 * 换非回溯 pattern 续查，暂存的首行（不匹配 keyword2）被重判跳过、次行命中。
	 */
	@Test
	public void testSearchRegexResumeRejudgesPending() throws Exception {
		var logDir = Files.createTempDirectory("log4j-pending-regex");
		var manager = newManager(logDir);
		try {
			manager.stop();
			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();

			var remain = session.searchRegex(result, millis(Base), -1, PathologicalPattern, 100);
			assertTrue(remain && result.isEmpty());

			remain = session.searchRegex(result, millis(Base), -1, "keyword2", 100);
			assertEquals(1, result.size(), "暂存首行重判跳过、次行命中");
			assertTrue(result.get(0).getLog().contains("keyword2"));
			assertTrue(!remain, "文件已读尽");
			session.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}
}
