package Zeze.Services.Log4jQuery;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static harness.DirCleanup.deleteBestEffort;

import java.lang.reflect.Method;
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
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.Task;

import harness.Fast;

/**
 * GD-C01回归：walker以整型currentIndex追踪files，GD-D01落地后removeMissingFile/reconcile会从
 * 列表中途摘除条目，CopyOnWriteArrayList摘除使其后元素左移——遍历中的会话游标失效：
 * 摘除点在游标之后时++currentIndex跳过整文件或提前终止（当前文件尾部丢失），静默错/缺数据。
 * 修复后walker记住打开的条目引用（currentEntry），hasNext入口indexOf重同步、耗尽按pos+1推进；
 * 摘除点在游标之前的左移形态正是TestQuerySkipsDeletedFileEntry三用例（同index重试形态）未覆盖的部分。
 */
@Fast
@Extra
public class TestWalkerCursorSurvivesEntryRemoval {
	private static final String Active = "zeze.log";
	private static final String Rotated1 = "zeze.2026-09-01.log";
	private static final String Rotated2 = "zeze.2026-09-02.log";
	private static final String Rotated3 = "zeze.2026-09-03.log";

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * 形态乙（walker不在最后，整文件跳过）：游标持R3（currentIndex=2）时摘除R1，
	 * 左移后++currentIndex越过active——修复前R3耗尽后提前终止，active整文件静默消失；
	 * 修复后按条目引用重定位续读active。
	 */
	@Test
	public void testRemovalBeforeCursorSkipsNoFile() throws Exception {
		var logDir = Files.createTempDirectory("cursor-skip");
		writeLog(logDir.resolve(Rotated1), "r1");
		writeLog(logDir.resolve(Rotated2), "r2");
		writeLog(logDir.resolve(Rotated3), "r3");
		writeLog(logDir.resolve(Active), "active");
		var manager = newManager(logDir);
		try {
			assertEquals(4, manager.size());
			manager.stop(); // 冻结watch与定时对账，摘除只由查询打开点触发（GD-D01路径）

			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();
			// 页1：limit=2停在R3已打开未读（walker状态：current=R3, currentIndex=2）。
			assertTrue(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 2));
			assertEquals(2, result.size());

			// 外部清理R1（walker持R3句柄，R1无句柄Windows可删）+任一打开点触发持锁摘除。
			Files.delete(logDir.resolve(Rotated1));
			manager.get(0).close(); // 打开R1失败→摘除→同index重试打开R2，列表左移为[R2,R3,Active]。
			assertEquals(3, manager.size());

			// 页2：修复前hasNext对current(R3)耗尽后++currentIndex=3==size提前终止，active整文件跳过；
			// 修复后indexOf(R3)=1重同步，R3耗尽后按引用取后继active。
			result.clear();
			assertFalse(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 100));
			assertEquals(2, result.size(), "页2应为R3剩余1条+active 1条（修复前只剩R3的1条，active静默消失）");
			assertTrue(result.get(0).getLog().contains("r3"));
			assertTrue(result.get(1).getLog().contains("active"), "摘除左移后紧跟的后继文件不得跳过");
			session.close();
		} finally {
			manager.stop(); // 幂等
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 形态甲（walker在最后一条目，尾部丢失+提前终止）：游标持active（currentIndex=2）且有未读内容时
	 * 摘除R1，左移后currentIndex(2)==新size(2)，hasNext循环条件直接false——修复前active未读日志全部
	 * 丢弃、会话静默"查完"；修复后重同步到active新位置续读。
	 */
	@Test
	public void testRemovalBeforeCursorDropsNoTail() throws Exception {
		var logDir = Files.createTempDirectory("cursor-tail");
		var base = LocalDateTime.now();
		writeLogs(logDir.resolve(Rotated1), base, "r1");
		writeLogs(logDir.resolve(Rotated2), base, "r2");
		writeLogs(logDir.resolve(Active), base, "active0", "active1");
		var manager = newManager(logDir);
		try {
			assertEquals(3, manager.size());
			manager.stop();

			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();
			// 页1：limit=2消费R1/R2各1条，耗尽推进打开active（walker状态：current=active, currentIndex=2，未读）。
			assertTrue(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 2));
			assertEquals(2, result.size());

			Files.delete(logDir.resolve(Rotated1));
			manager.get(0).close(); // 触发摘除R1，列表左移为[R2,Active]。
			assertEquals(2, manager.size());

			// 页2：修复前while(2<2)直接false返回空页（active尾部丢失）；修复后indexOf(active)=1续读两条。
			result.clear();
			assertFalse(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 100));
			assertEquals(2, result.size(), "当前文件的未读尾部不得因游标之前的摘除被丢弃");
			assertTrue(result.get(0).getLog().contains("active0"));
			assertTrue(result.get(1).getLog().contains("active1"));
			session.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * size回涨续读（案卷形态甲 aftermath："其后新出现的文件在size回涨后从错误的错位下标续读"）：
	 * 遍历耗尽后新文件登记（轮转新建active），修复前耗尽态current!=null且currentIndex==旧size，
	 * has-next循环里++直接越过新文件；修复后耗尽即关句柄（current==null），新条目按currentIndex正常打开。
	 */
	@Test
	public void testExhaustedThenGrownContinues() throws Exception {
		var logDir = Files.createTempDirectory("cursor-grow");
		writeLog(logDir.resolve(Rotated1), "r1");
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop(); // 模拟轮转事件延迟/丢失：新文件先登记由测试直调onFileCreated（生产由watch线程调用）

			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();
			assertFalse(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 10));
			assertEquals(1, result.size(), "页1消费唯一文件R1");

			// 轮转：旧内容已在R1（构造期登记），新建active。
			writeLog(logDir.resolve(Active), "active");
			invokeOnFileCreated(manager, logDir.resolve(Active));
			assertEquals(2, manager.size());

			// 页2：修复前hasNext对耗尽的current(R1)++currentIndex=2==size，active跳过返回空；
			// 修复后正常打开active。
			result.clear();
			assertFalse(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 10));
			assertEquals(1, result.size(), "耗尽后新登记的文件应可续读（修复前整文件跳过）");
			assertTrue(result.get(0).getLog().contains("active"));
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

	private static void invokeOnFileCreated(Log4jFileManager manager, Path path) throws Exception {
		Method method = Log4jFileManager.class.getDeclaredMethod("onFileCreated", Path.class);
		method.setAccessible(true);
		method.invoke(manager, path);
	}

	private static void writeLog(Path file, String message) throws Exception {
		writeLogs(file, LocalDateTime.now(), message);
	}

	private static void writeLogs(Path file, LocalDateTime base, String... messages) throws Exception {
		var fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
		var sb = new StringBuilder();
		for (var i = 0; i < messages.length; ++i)
			sb.append(base.plusSeconds(30L * i).format(fmt)).append(' ').append(messages[i]).append('\n');
		AtomicFileWriter.replace(file, sb.toString().getBytes(StandardCharsets.UTF_8));
	}
}
