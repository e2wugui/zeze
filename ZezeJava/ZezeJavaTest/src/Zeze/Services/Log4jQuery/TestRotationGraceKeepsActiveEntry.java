package Zeze.Services.Log4jQuery;

import harness.Extra;
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

/**
 * GD-C02回归：log4j轮转的磁盘顺序是"先rename旧内容到rotate名、后重建active"，两步之间存在
 * active路径不存在的窗口（内部毫秒级、外部mv型归档可达分钟级）。窗口内查询seek/get或reconcile
 * 摘除active条目后，case-1守卫（last==current名）落空，"索引改名+改指"整体跳过，旧索引残留
 * current名下被case-0挂到新内容上——旧时间窗查询整窗返回空，只能等下一轮reconcile自愈（≤5min）。
 * 修复后removeMissingFile与reconcile摘除循环对active名条目宽限：磁盘存在未登记rotate即
 * "轮转进行中"的证据，保留条目作为case-1/repointMissedRotation改指的载体。
 */
@Fast
@Extra
public class TestRotationGraceKeepsActiveEntry {
	private static final String Active = "zeze.log";
	private static final String Rotated = "zeze.2026-09-01.log";
	private static final LocalDateTime C1Base = LocalDateTime.of(2026, 9, 1, 10, 0);
	private static final LocalDateTime C2Base = C1Base.plusHours(1);

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * 主链（查询摘除竞速→守卫落空→错窗）：窗口内seek/get打开active失败（GD-D01路径）不摘条目
	 * （get安静降级null不自旋）；随后case-1守卫成立，索引改名+改指正常执行；case-0按新索引补登
	 * ——配对全程正确，C1时间窗查询即查即对（修复前整窗空查、≤5min才自愈）。
	 */
	@Test
	public void testWindowQueryKeepsActiveEntry() throws Exception {
		var logDir = Files.createTempDirectory("grace-window");
		AtomicFileWriter.writeAtomically(logDir.resolve(Active), buildLines(C1Base, "c1-", 40).getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop(); // 冻结watch与定时对账，事件由测试直调（生产由watch线程调用）

			// 轮转窗口：旧内容已rename进R、active尚未重建。
			Files.move(logDir.resolve(Active), logDir.resolve(Rotated));

			// 窗口内两个打开点：seek（无锁路径，案卷触发形态）与get（同index重试路径，验证不自旋）。
			assertNull(manager.seek(millis(C1Base.plusSeconds(600)), new OutInt()),
					"窗口内seek选中active打开失败应安静降级返回null");
			assertNull(manager.get(0), "窗口内get打开失败应安静降级（耗尽语义，不抛不挂）");
			assertEquals(1, manager.size(),
					"轮转进行中active条目不得摘除（修复前被摘，case-1守卫随之落空）");

			// watch处理CREATE(R)（case-1）：条目在位，守卫成立——索引改名+条目改指正常执行。
			invokeOnFileCreated(manager, logDir.resolve(Rotated));
			assertEquals(List.of(Rotated), fileNamesOf(manager), "active条目改指rotate");
			assertTrue(Files.exists(logDir.resolve(Rotated + ".index")), "current索引改名跟随rotate");
			// FND24 log4j-01 新契约：移交=rotate名下硬链接接管同inode，current.index为滞后一代的
			// 装载期链接（运行期不触碰该名，下次装载重建）——残留即语义本身，且必与R.index同inode。
			assertTrue(Files.isSameFile(logDir.resolve(Rotated + ".index"), logDir.resolve(Active + ".index")),
					"current.index应为指向被接管索引的滞后链接（与rotate索引同inode）");

			// watch处理CREATE(X)（case-0）：active重建，按新索引登记。
			AtomicFileWriter.writeAtomically(logDir.resolve(Active),
					buildLines(C2Base, "c2-", 2).getBytes(StandardCharsets.UTF_8));
			invokeOnFileCreated(manager, logDir.resolve(Active));
			assertEquals(List.of(Rotated, Active), fileNamesOf(manager));
			var entries = entriesOf(manager);
			assertEquals(millis(C1Base), entries.get(0).index.getBeginTime(), "rotate条目=旧内容索引（改名跟随）");
			assertEquals(millis(C2Base), entries.get(1).index.getBeginTime(),
					"active条目=新内容索引（修复前残留的旧C1索引被case-0挂上）");

			assertC1WindowReturns20(manager);
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 对账摘除路径的宽限：改名失败（残留占据R.index名）中止改指后，摘除循环同样保留active条目
	 * （它是当轮改指重试的载体）。FND25 log4j-04后残留占据者在同轮补登中被配对校验消费（不可配对
	 * 即删除让位、按rotate名重建索引），R即刻登记——宽限随"R已登记"解除：下一轮active仍缺失即按
	 * 常规摘除；active重建后按新内容索引补登，C1时间窗由R的重建索引承载。
	 */
	@Test
	public void testReconcileGraceDuringBlockedRename() throws Exception {
		var logDir = Files.createTempDirectory("grace-reconcile");
		AtomicFileWriter.writeAtomically(logDir.resolve(Active), buildLines(C1Base, "c1-", 40).getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop();

			// 窗口 + 改名障碍（目录占据R.index名，跨平台确定性失败）。
			Files.move(logDir.resolve(Active), logDir.resolve(Rotated));
			Files.createDirectory(logDir.resolve(Rotated + ".index"));

			// 首轮对账：repointMissedRotation改名失败中止（GD-C01回滚语义）；摘除循环对active条目
			// 宽限保留（此刻R未登记=轮转未收敛的证据，不得摘除改指载体）。同轮补登把障碍按陈旧残留
			// 消费：删除让位后R以重建索引登记（FND25前该形态是补登抛异常整轮回滚，R跨轮不登记）。
			invokeReconcile(manager);
			assertEquals(List.of(Rotated, Active), fileNamesOf(manager),
					"改名失败+轮转未收敛：active条目是改指重试的载体，不得摘除");
			assertTrue(Files.isRegularFile(logDir.resolve(Rotated + ".index")),
					"占据R.index名的陈旧残留被删除让位，按rotate名重建为索引文件");

			// 次轮对账：R已登记（轮转收敛），active仍缺失——宽限解除，条目按常规摘除（不得跨轮滞留）。
			invokeReconcile(manager);
			assertEquals(List.of(Rotated), fileNamesOf(manager), "宽限解除后缺失的active条目按常规摘除");

			// active重建，下一轮对账收敛：R携重建索引在位，active按新内容索引补登。
			AtomicFileWriter.writeAtomically(logDir.resolve(Active),
					buildLines(C2Base, "c2-", 2).getBytes(StandardCharsets.UTF_8));
			invokeReconcile(manager);
			assertEquals(List.of(Rotated, Active), fileNamesOf(manager), "收敛：active按新索引补登");
			var entries = entriesOf(manager);
			assertEquals(millis(C1Base), entries.get(0).index.getBeginTime(), "rotate条目携重建的旧内容索引");
			assertEquals(millis(C2Base), entries.get(1).index.getBeginTime(),
					"active条目挂新内容索引（不得残留旧C1索引）");
			assertC1WindowReturns20(manager);
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	private static void assertC1WindowReturns20(Log4jFileManager manager) throws Exception {
		var tMid = millis(C1Base.plusSeconds(600)); // 恰为c1-20的时间：其后20条（c1-20..c1-39）
		var tC1End = millis(C1Base.plusSeconds(30L * 39)); // c1-39的时间
		var result = new ArrayList<Log4jLog>();
		var session = new Log4jSession(manager);
		assertFalse(session.searchContains(result, tMid, tC1End, List.of("zzz"), BCondition.ContainsNone, 100));
		assertEquals(20, result.size(), "C1时间窗应返回c1-20..c1-39（错配形态下整窗丢失返回空）");
		assertTrue(result.get(0).getLog().contains("c1-20"));
		assertTrue(result.get(19).getLog().contains("c1-39"));
		for (var log : result)
			assertFalse(log.getLog().contains("c2-"), "C1窗口不得返回新内容");
		session.close();
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

	private static void invokeOnFileCreated(Log4jFileManager manager, Path path) throws Exception {
		Method method = Log4jFileManager.class.getDeclaredMethod("onFileCreated", Path.class);
		method.setAccessible(true);
		method.invoke(manager, path);
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
