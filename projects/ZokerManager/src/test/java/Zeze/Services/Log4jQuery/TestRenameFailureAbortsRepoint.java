package Zeze.Services.Log4jQuery;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
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
 * GD-C01回归：onFileCreated case-1轮转处理中current索引renameTo失败仅告警仍改指+补登——
 * rotate条目与补登的active条目经各自新硬链接mmap同一索引inode，双条目交叉读写制造混合索引
 * （旧记录offset指向新内容任意位置，静默错窗数据），且R未登记走不了repointMissedRotation、
 * 错位随磁盘X.index跨重启固化。FND20 GD-C03要求的"失败即中止改指"回滚语义当时只落在
 * repointMissedRotation，watch路径漏落实；修复后两处共用renameCurrentIndexTo：
 * 失败即return（不改指不补登），条目仍指current名，由下一轮reconcile摘除+常规补登收敛。
 */
@Fast
@Extra
public class TestRenameFailureAbortsRepoint {
	private static final String Active = "zeze.log";
	private static final String Rotated = "zeze.2026-09-01.log";
	// C1：40行×30s间隔（末行offset≈1.2KB）；C2：2行（≈62B）——失配检测条件成立的体量差。
	private static final LocalDateTime C1Base = LocalDateTime.of(2026, 9, 1, 10, 0);
	private static final LocalDateTime C2Base = C1Base.plusHours(1);

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * 中止语义：rename失败（目标名被目录占据——跨平台确定性失败，等价Windows既存目标/sharing
	 * violation形态）后case-1不得改指、不得补登。修复前：仅告警后照常执行后两步，
	 * files=[R(旧索引), X(新硬链接mmap同一索引)]双条目共享索引。
	 */
	@Test
	public void testRenameFailAbortsRepointAndRegister() throws Exception {
		var logDir = Files.createTempDirectory("rename-abort");
		AtomicFileWriter.writeAtomically(logDir.resolve(Active), buildLines(C1Base, "c1-", 40).getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop(); // 冻结watch与定时对账，case-1由测试直调（生产由watch线程调用）

			// log4j轮转的磁盘形态：旧内容改名进rotate名，active名重建承载新内容（时间窗更晚）。
			Files.move(logDir.resolve(Active), logDir.resolve(Rotated));
			AtomicFileWriter.writeAtomically(logDir.resolve(Active),
					buildLines(C2Base, "c2-", 2).getBytes(StandardCharsets.UTF_8));
			// rename失败注入：目标名被目录占据（File.renameTo对既存目录跨平台确定性失败）。
			Files.createDirectory(logDir.resolve(Rotated + ".index"));

			invokeOnFileCreated(manager, logDir.resolve(Rotated)); // case-1：rotate CREATE

			// 修复：失败即中止——条目仍指current名、无补登；修复前：[Rotated, Active]双条目共享索引。
			assertEquals(List.of(Active), fileNamesOf(manager), "rename失败即中止改指与补登（回滚语义）");
			assertTrue(Files.exists(logDir.resolve(Active + ".index")), "失败的改名不落盘，current索引原地保留");
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 中止后的收敛（比"继续错配"严格更优的证明）：障碍消除（瞬时sharing violation自愈/残留被清理）
	 * 后下一轮reconcile由repointMissedRotation检测失配（C1末offset≈1.2KB > C2长度≈62B），
	 * 改名+改指+active按新索引补登——索引与内容重新正确配对，C1时间窗查询恢复。
	 */
	@Test
	public void testAbortConvergesAfterObstacleGone() throws Exception {
		var logDir = Files.createTempDirectory("rename-converge");
		AtomicFileWriter.writeAtomically(logDir.resolve(Active), buildLines(C1Base, "c1-", 40).getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop();

			Files.move(logDir.resolve(Active), logDir.resolve(Rotated));
			AtomicFileWriter.writeAtomically(logDir.resolve(Active),
					buildLines(C2Base, "c2-", 2).getBytes(StandardCharsets.UTF_8));
			Files.createDirectory(logDir.resolve(Rotated + ".index"));
			invokeOnFileCreated(manager, logDir.resolve(Rotated)); // case-1：rename失败，中止
			assertEquals(List.of(Active), fileNamesOf(manager));

			Files.delete(logDir.resolve(Rotated + ".index")); // 障碍消除
			invokeReconcile(manager);

			assertEquals(List.of(Rotated, Active), fileNamesOf(manager), "reconcile收敛：改指+按新索引补登");
			var entries = entriesOf(manager);
			assertEquals(millis(C1Base), entries.get(0).index.getBeginTime(),
					"rotate条目应携旧内容索引（改名跟随）");
			assertEquals(millis(C2Base), entries.get(1).index.getBeginTime(),
					"active条目应挂新内容索引（不残留中止前的旧C1索引）");
			assertTrue(Files.exists(logDir.resolve(Rotated + ".index")), "current索引改名跟随rotate");
			assertC1WindowReturns20(manager);
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 重复递交形态（案卷触面：OVERFLOW重放/平台重复事件使第二次case-1把补登后的C2索引rename向
	 * 已存在的R.index，Windows必然失败）：修复后中止=no-op，配对保持；修复前warn后照常改指+补登，
	 * 出现双R条目+第三条目再mmap C2索引。File.renameTo对既存目标仅Windows确定性失败
	 * （Linux覆盖既存目标），故本用例限Windows。
	 */
	@Test
	public void testDuplicateRotateEventIsNoop() throws Exception {
		assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"),
				"File.renameTo对既存目标仅Windows确定性失败");
		var logDir = Files.createTempDirectory("rename-dup");
		AtomicFileWriter.writeAtomically(logDir.resolve(Active), buildLines(C1Base, "c1-", 40).getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop();

			Files.move(logDir.resolve(Active), logDir.resolve(Rotated));
			AtomicFileWriter.writeAtomically(logDir.resolve(Active),
					buildLines(C2Base, "c2-", 2).getBytes(StandardCharsets.UTF_8));

			invokeOnFileCreated(manager, logDir.resolve(Rotated)); // 第一次case-1：正常轮转
			assertEquals(List.of(Rotated, Active), fileNamesOf(manager));
			var entries = entriesOf(manager);
			assertEquals(millis(C1Base), entries.get(0).index.getBeginTime());
			assertEquals(millis(C2Base), entries.get(1).index.getBeginTime());

			invokeOnFileCreated(manager, logDir.resolve(Rotated)); // 重复递交：rename失败→中止
			assertEquals(List.of(Rotated, Active), fileNamesOf(manager),
					"重复case-1应为no-op（修复前出现双R条目+共享索引的第三条目）");
			entries = entriesOf(manager);
			assertEquals(millis(C1Base), entries.get(0).index.getBeginTime(), "R条目保留C1索引");
			assertEquals(millis(C2Base), entries.get(1).index.getBeginTime(), "X条目保留C2索引");
			assertTrue(Files.exists(logDir.resolve(Active + ".index")), "C2索引仍在current名（未被夺走）");
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
