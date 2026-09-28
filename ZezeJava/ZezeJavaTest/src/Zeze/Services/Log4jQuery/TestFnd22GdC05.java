package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static harness.DirCleanup.deleteBestEffort;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Builtin.LogService.BCondition;
import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.Log4jLog;
import Zeze.Services.Log4jQuery.Log4jSession;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.OutInt;
import Zeze.Util.Task;

import harness.Fast;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * FND22 GD-C05回归（FND21 GD-C02修复的误改指面）：轮转宽限与repointMissedRotation在
 * "rotate存在但active真被外部删除"形态下基于未验证推断执行破坏性改名——把现存的好索引改名
 * （不可逆）到无关rotate文件名下并改指条目：错配索引入列、该rotate自身时间窗永久不可查、
 * 磁盘多出内容错配的.index跨重启经补登挂载；宽限保留本身全程零告警（不可观测的无限期滞留）。
 * 修复：repoint改名前内容配对抽查（rotate首条日志时间须落在既有索引时间窗内）；宽限continue
 * 路径补warn（含条目名），"轮转进行中"与"无限期滞留"可区分。
 */
@Fast
@ResourceLock("log4jquery-logger") // 同族捕获Log4jFileManager logger的测试互斥（预防性：Onz/MQ两族竞态的同款，FND19-22复盘小集）
public class TestFnd22GdC05 {
	private static final String Active = "zeze.log";
	// 无关rotate名文件：时间窗（09-05）与active索引内容（09-10）完全无关——误改指形态的判别点。
	private static final String Junk = "zeze.2026-09-05.log";
	private static final LocalDateTime C1Base = LocalDateTime.of(2026, 9, 10, 10, 0);
	private static final LocalDateTime JunkBase = LocalDateTime.of(2026, 9, 5, 8, 0);

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * 误改指主链：active真删（rm）+磁盘恰有无关rotate名文件——失配判据（lastOffset>length()==0）
	 * 必成立。修复前：现存C1索引被改名到Junk名下、条目改指Junk（不可逆、零告警）。修复后：
	 * 配对抽查否决（Junk首条时间在C1窗外），Junk按常规补登挂自己的全新索引，active条目宽限
	 * 一轮（有告警）后摘除——配对全程正确。
	 */
	@Test
	public void testUnrelatedRotateNotRepointed() throws Exception {
		var logDir = Files.createTempDirectory("fnd22-gdc05-mispair");
		// C1：40行×30s，构造期全量索引（beginTime=C1Base，末行offset≈1.2KB）。
		AtomicFileWriter.replace(logDir.resolve(Active), buildLines(C1Base, "c1-", 40).getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop(); // 冻结watch与定时对账，reconcile由测试直调

			// 形态构造：active真删；磁盘放入无关rotate名文件（时间窗更早、内容无关）。
			Files.delete(logDir.resolve(Active));
			AtomicFileWriter.replace(logDir.resolve(Junk), buildLines(JunkBase, "junk-", 5).getBytes(StandardCharsets.UTF_8));

			try (var capture = new TestFnd22LogCapture(Log4jFileManager.class, Level.DEBUG)) {
				// 宽限可观测（查询路径）：窗口内seek打开失败走宽限，修复前静默。
				assertNull(manager.seek(millis(C1Base.plusSeconds(600)), new OutInt()),
						"active缺失+未登记rotate：seek安静降级null");
				assertTrue(capture.anyMessageContains("rotation unconverged"),
						"宽限保留须有告警（修复前零告警，无限期滞留不可观测）");

				invokeReconcile(manager);
				// 修复前：repoint触发——条目改指Junk且携C1索引，entries=[Junk]，C1索引被改名走。
				assertEquals(List.of(Junk, Active), fileNamesOf(manager), "Junk常规补登+active条目宽限保留");
				assertTrue(Files.exists(logDir.resolve(Active + ".index")),
						"C1索引不得被改名到无关文件名下（修复前repoint不可逆错配）");
				assertFalse(capture.anyMessageContains("reconcile missed rotation"),
						"无关rotate不得触发改指");

				// 配对正确性：Junk条目挂自己的全新索引（首条=Junk内容首条），不是C1索引。
				var entries = entriesOf(manager);
				assertEquals(millis(JunkBase), entries.get(0).index.getBeginTime(),
						"Junk应挂自己的全新索引（修复前被错配挂上C1索引）");
				assertTrue(capture.anyMessageContains("log file missing (reconcile) but rotation unconverged"),
						"reconcile宽限路径同样须有告警");
			}

			// 收敛：下一轮对账摘除仍缺失的active条目（宽限自然解除），Junk可查。
			invokeReconcile(manager);
			assertEquals(List.of(Junk), fileNamesOf(manager), "宽限一轮后active条目摘除收敛");
			var result = new ArrayList<Log4jLog>();
			var session = new Log4jSession(manager);
			try {
				assertFalse(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 100));
				assertEquals(5, result.size(), "Junk自身时间窗查询可达（修复前被C1索引顶替永久不可查）");
				for (var log : result)
					assertTrue(log.getLog().contains("junk-"), "返回Junk自身内容");
			} finally {
				session.close();
			}
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 合法改指不回归（FND20 GD-C03语义钉子）：磁盘形态确为漏轮转（R首条=索引首条，同内容）时
	 * 配对抽查通过，改名+改指+补登照常执行（TestFnd20GdC03同构，此处用更早的rotate时间名钉住
	 * "首条时间在窗内"边界：R首条时间恰等于索引beginTime）。
	 */
	@Test
	public void testGenuineMissedRotationStillRepointed() throws Exception {
		var logDir = Files.createTempDirectory("fnd22-gdc05-genuine");
		AtomicFileWriter.replace(logDir.resolve(Active), buildLines(C1Base, "c1-", 40).getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop();

			// 漏轮转形态：旧内容（=C1索引描述的内容）改名进rotate名，active重建承载更晚内容。
			Files.move(logDir.resolve(Active), logDir.resolve(Junk));
			AtomicFileWriter.replace(logDir.resolve(Active),
					buildLines(C1Base.plusHours(1), "c2-", 2).getBytes(StandardCharsets.UTF_8));

			invokeReconcile(manager);
			assertEquals(List.of(Junk, Active), fileNamesOf(manager), "改指+补登收敛");
			var entries = entriesOf(manager);
			assertEquals(millis(C1Base), entries.get(0).index.getBeginTime(),
					"rotate条目携旧内容索引（改名跟随，配对抽查不得误伤合法改指）");
			assertEquals(millis(C1Base.plusHours(1)), entries.get(1).index.getBeginTime(), "active条目挂新索引");
			assertTrue(Files.exists(logDir.resolve(Junk + ".index")), "current索引改名跟随rotate");
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

	private static void invokeReconcile(Log4jFileManager manager) throws Exception {
		Method method = Log4jFileManager.class.getDeclaredMethod("reconcile");
		method.setAccessible(true);
		method.invoke(manager);
	}

	@SuppressWarnings("unchecked")
	private static List<Log4jFileManager.Log4jFile> entriesOf(Log4jFileManager manager) throws Exception {
		Field field = Log4jFileManager.class.getDeclaredField("files");
		field.setAccessible(true);
		return (List<Log4jFileManager.Log4jFile>)field.get(manager);
	}

	private static List<String> fileNamesOf(Log4jFileManager manager) throws Exception {
		var names = new ArrayList<String>();
		for (var file : entriesOf(manager))
			names.add(file.file.getName());
		return names;
	}
}
