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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
 * FND20 R2 F-1回归：walker自身条目被摘除（Linux unlink形态，indexOf得pos<0）且stale下标
 * ≥size时，hasNext的while闸先于current.hasNext()判假——当前文件未读尾部丢失，违背
 * GD-C01"读尽不丢当前文件尾部"的承诺。修复后pos<0且会话可读时先返回true。
 * Windows删不掉打开中的文件，测试以反射直摘列表条目等价模拟（生产路径removeMissingFile/
 * reconcile同样终结于files.remove——磁盘文件保留使会话fd可读性与Linux unlink后一致）。
 */
@Fast
public class TestFnd20R2F1WalkerOwnEntryRemoved {
	private static final String Active = "zeze.log";
	private static final String Rotated1 = "zeze.2026-09-01.log";
	private static final String Rotated2 = "zeze.2026-09-02.log";

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testOwnEntryRemovedKeepsUnreadTail() throws Exception {
		var logDir = Files.createTempDirectory("fnd20-r2f1-tail");
		var base = LocalDateTime.now();
		writeLogs(logDir.resolve(Rotated1), base, "r1");
		writeLogs(logDir.resolve(Rotated2), base, "r2");
		writeLogs(logDir.resolve(Active), base, "active0", "active1");
		var manager = newManager(logDir);
		try {
			assertEquals(3, manager.size());
			manager.stop(); // 冻结watch与定时对账，摘除只由测试显式触发

			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();
			// 页1：limit=2消费R1/R2各1条，耗尽推进打开active（walker：current=active, currentIndex=2，未读）。
			assertTrue(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 2));
			assertEquals(2, result.size());

			// 摘除walker自身条目（反射等价Linux unlink+reconcile摘除）：列表变[R1,R2]，stale下标2==size。
			removeEntryReflectively(manager, 2);
			assertEquals(2, manager.size());

			// 页2：修复前while(2<2)先判假，active两条未读整页丢弃；修复后pos<0先读尽会话fd。
			result.clear();
			assertFalse(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 100));
			assertEquals(2, result.size(), "自身条目被摘除后，当前文件的未读尾部不得丢失");
			assertTrue(result.get(0).getLog().contains("active0"));
			assertTrue(result.get(1).getLog().contains("active1"));
			session.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	@SuppressWarnings("unchecked")
	private static void removeEntryReflectively(Log4jFileManager manager, int index) throws Exception {
		Field field = Log4jFileManager.class.getDeclaredField("files");
		field.setAccessible(true);
		var files = (CopyOnWriteArrayList<Object>)field.get(manager);
		assertTrue(files.remove(files.get(index)), "条目应成功摘除");
	}

	private static Log4jFileManager newManager(Path logDir) throws Exception {
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = Active;
		logConf.logDir = logDir.toString();
		return new Log4jFileManager(logConf);
	}

	private static void writeLogs(Path file, LocalDateTime base, String... messages) throws Exception {
		var fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
		var sb = new StringBuilder();
		for (var i = 0; i < messages.length; ++i)
			sb.append(base.plusSeconds(30L * i).format(fmt)).append(' ').append(messages[i]).append('\n');
		AtomicFileWriter.replace(file, sb.toString().getBytes(StandardCharsets.UTF_8));
	}
}
