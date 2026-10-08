package Zeze.Services.Log4jQuery;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static harness.DirCleanup.deleteBestEffort;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Builtin.LogService.BCondition;
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.Task;

import harness.Fast;

/**
 * logDir中出现名字符合rotate模式的目录（运维/备份/拷贝脚本的中转目录，WatchService对目录
 * 创建同样递交ENTRY_CREATE）时，轮转事件不得劫持active条目：索引移交+条目改指会把active
 * 条目指到目录上，查询命中该条目时FileChannel.open(目录)抛非FileNotFoundException的
 * IOException（Windows AccessDenied/Linux IsADirectory），逃逸seek/open的FNFE降级链使
 * 整个search/browse请求失败；且reconcile的exists()摘除判据对目录恒假，运行期无自愈，只有
 * 重启（loadRotates的isFile过滤）收敛。修复后事件入口校验rotate目标为普通文件，非普通文件
 * 忽略+warn，交给对账的isFile过滤天然排除。
 */
@Fast
@Extra
public class TestRotateNamedDirectoryIgnored {
	private static final String Active = "zeze.log";
	private static final String RotateDir = "zeze.2026-09-29.log";
	private static final LocalDateTime Base = LocalDateTime.of(2026, 9, 29, 10, 0);

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * rotate名目录的创建事件不得触发索引移交+改指：条目仍指active、不产生目录名.index链接，
	 * 该时间窗查询正常返回；随后对账（isFile过滤）也不把目录补登入列表。
	 */
	@Test
	public void testRotateNamedDirectoryDoesNotHijackActive() throws Exception {
		var logDir = Files.createTempDirectory("log4j-rotate-named-directory");
		AtomicFileWriter.replace(logDir.resolve(Active), buildLines(Base, "a-", 0, 10, 20)
				.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop(); // 冻结watch与索引定时器，事件由测试直调

			// logDir内创建rotate名目录（如备份脚本的中转目录）。
			Files.createDirectory(logDir.resolve(RotateDir));
			invokeOnFileCreated(manager, logDir.resolve(RotateDir));

			// 条目不被劫持：仍只有active条目（修复前：active条目改指目录+补登新active成双条目）。
			assertEquals(List.of(Active), fileNamesOf(manager), "rotate名目录不得劫持active条目");
			// 索引不被移交到目录名.index（修复前：目录名上创建出硬链接）。
			assertFalse(Files.exists(logDir.resolve(RotateDir + ".index")),
					"不得为非普通文件的rotate目标创建索引链接");

			// 该时间窗查询正常返回（修复前：seek选中被劫持条目，打开目录抛AccessDenied等
			// 非FNFE的IOException逃逸降级链，整个请求失败且无自愈）。
			var session = new Log4jSession(manager);
			try {
				var result = new ArrayList<Log4jLog>();
				assertFalse(session.searchContains(result, millis(Base), millis(Base.plusSeconds(20)),
						List.of("a-"), BCondition.ContainsAll, 100));
				assertEquals(3, result.size(), "目录事件后active内容窗口必须照常可查");
			} finally {
				session.close();
			}

			// 对账不把目录补登入列表（isFile过滤既有行为，反证运行期唯一入口是事件路径）。
			invokeReconcile(manager);
			assertEquals(List.of(Active), fileNamesOf(manager), "对账不得补登非普通文件的rotate名");
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

	private static String buildLines(java.time.LocalDateTime base, String prefix, long... offsetsSeconds) {
		var fmt = java.time.format.DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
		var sb = new StringBuilder();
		var i = 0;
		for (var offset : offsetsSeconds)
			sb.append(base.plusSeconds(offset).format(fmt)).append(' ')
					.append(prefix).append(i++).append('\n');
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
	private static List<String> fileNamesOf(Log4jFileManager manager) throws Exception {
		Field field = Log4jFileManager.class.getDeclaredField("files");
		field.setAccessible(true);
		var files = (List<Log4jFileManager.Log4jFile>)field.get(manager);
		var names = new ArrayList<String>();
		for (var file : files)
			names.add(file.file.getName());
		return names;
	}
}
