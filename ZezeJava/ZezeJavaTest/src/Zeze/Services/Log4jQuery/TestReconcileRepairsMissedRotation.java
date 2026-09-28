package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * GD-C03回归：轮转双CREATE事件丢失（OVERFLOW吞掉/watch失效）后reconcile只摘除+补登，
 * 不补case-1的"索引改名+条目改指"语义——active条目持旧内容索引配新内容文件：旧时间窗查询
 * 命中错文件返回空窗、buildIndex给旧索引续写制造混合索引、错位跨重启固化。
 * 修复后reconcile检测失配（active索引末记录offset超出active文件长度——自洽索引的offset必落在
 * 文件长度内）并按case-1重放：current索引改名跟随最早漏登rotate、条目改指、active按新索引补登。
 */
@Fast
public class TestReconcileRepairsMissedRotation {
	private static final String Active = "zeze.log";
	private static final String Rotated = "zeze.2026-09-20.log";
	// C1：40行×30s间隔（每行都够10s索引阈值），末行offset≈1.2KB；C2：2行，文件长度≈60B——失配检测条件成立。
	private static final LocalDateTime C1Base = LocalDateTime.of(2026, 9, 20, 10, 0);
	private static final LocalDateTime C2Base = C1Base.plusHours(1);

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testReconcileRepointsMissedRotation() throws Exception {
		var logDir = Files.createTempDirectory("fnd20-gdc03-repoint");
		var c1 = buildLines(C1Base, "c1-", 40);
		AtomicFileWriter.replace(logDir.resolve(Active), c1.getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop(); // 模拟轮转双事件丢失：watch已冻结，rotate改名/重建只发生在磁盘上

			// log4j轮转的磁盘形态：旧内容改名进rotate名，active名重建承载新内容（时间窗更晚）。
			Files.move(logDir.resolve(Active), logDir.resolve(Rotated));
			AtomicFileWriter.replace(logDir.resolve(Active),
					buildLines(C2Base, "c2-", 2).getBytes(StandardCharsets.UTF_8));

			invokeReconcile(manager);
			// 条目收敛：[R1(携旧索引改指), active(新索引补登)]——两lane同序；差异在索引归属。
			assertEquals(List.of(Rotated, Active), fileNamesOf(manager));

			// 索引归属（修复点）：R1条目=旧内容索引（beginTime=C1首条）；active条目=新索引（beginTime=C2首条，
			// 修复前active仍挂C1索引——beginTime=C1首条）。
			var entries = entriesOf(manager);
			assertEquals(millis(C1Base), entries.get(0).index.getBeginTime(),
					"改指后的rotate条目应携旧内容索引（改名跟随）");
			assertEquals(millis(C2Base), entries.get(1).index.getBeginTime(),
					"active条目应挂新内容索引（修复前挂旧C1索引）");
			assertTrue(Files.exists(logDir.resolve(Rotated + ".index")), "current索引应改名跟随rotate");

			// 行为（错窗查询）：查C1中段时间窗，命中R1返回C1内容；修复前seek按旧索引选中active、
			// 旧offset超出新文件长度落EOF，整窗丢失返回空。endTime收在C1末条，断言不含C2内容。
			var tMid = millis(C1Base.plusSeconds(600)); // 恰为c1-20的时间：其后20条（c1-20..c1-39）
			var tC1End = millis(C1Base.plusSeconds(30L * 39)); // c1-39的时间
			var result = new ArrayList<Log4jLog>();
			var session = new Log4jSession(manager);
			assertFalse(session.searchContains(result, tMid, tC1End, List.of("zzz"), BCondition.ContainsNone, 100));
			assertEquals(20, result.size(), "C1时间窗应返回c1-20..c1-39（修复前命中错文件整窗丢失返回空）");
			assertTrue(result.get(0).getLog().contains("c1-20"));
			assertTrue(result.get(19).getLog().contains("c1-39"));
			for (var log : result)
				assertFalse(log.getLog().contains("c2-"), "C1窗口不得返回新内容");
			session.close();

			// 跨重启固化：按磁盘布局重建manager，C1窗口仍正确（修复前错位随A.index原样装载而固化）。
			var manager2 = newManager(logDir);
			try {
				assertEquals(List.of(Rotated, Active), fileNamesOf(manager2));
				result.clear();
				session = new Log4jSession(manager2);
				assertFalse(session.searchContains(result, tMid, tC1End, List.of("zzz"), BCondition.ContainsNone, 100));
				assertEquals(20, result.size(), "重启后C1时间窗仍应命中rotate（修复前错位跨重启固化）");
				assertTrue(result.get(0).getLog().contains("c1-20"));
				session.close();
			} finally {
				manager2.stop();
			}
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 无失配不动作（防误伤）：active索引末offset未超出文件长度（自洽索引）时不改指不改名，
	 * 漏登rotate按常规全量补登。
	 */
	@Test
	public void testFreshIndexNotRepointed() throws Exception {
		var logDir = Files.createTempDirectory("fnd20-gdc03-noop");
		AtomicFileWriter.replace(logDir.resolve(Active), buildLines(C1Base, "a-", 3).getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop();
			// 磁盘出现更晚的漏登rotate（watch事件丢失），但active索引与其文件自洽（末offset<文件长度）。
			AtomicFileWriter.replace(logDir.resolve(Rotated),
					buildLines(C2Base, "r-", 2).getBytes(StandardCharsets.UTF_8));

			invokeReconcile(manager);
			assertEquals(List.of(Rotated, Active), fileNamesOf(manager), "常规补登，顺序不变");
			assertTrue(Files.exists(logDir.resolve(Active + ".index")), "自洽索引不得被改名");
			// 未失配：rotate走全量补登建自己的新索引（beginTime=自身内容首条），active保留原索引。
			var entries = entriesOf(manager);
			assertEquals(millis(C2Base), entries.get(0).index.getBeginTime(),
					"自洽active不得把current索引让给rotate（rotate全量补登新索引）");
			assertEquals(millis(C1Base), entries.get(1).index.getBeginTime(), "active保留自己的索引");
			// 两文件均可查（常规补登路径不回归）。
			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();
			assertFalse(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 100));
			assertEquals(5, result.size());
			session.close();
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
