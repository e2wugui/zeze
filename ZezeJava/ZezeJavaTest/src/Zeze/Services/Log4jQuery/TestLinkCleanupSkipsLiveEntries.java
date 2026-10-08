package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import java.util.TreeSet;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.Task;

import harness.Fast;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * FND22 GD-C03回归（fix-the-fix：FND19 GD-C08）：removeOldLinkFiles"保留max删其余"不检查链接
 * 是否仍被files中条目的LogIndex mmap持有——Linux下删成功但条目索引尾部续建按链接路径重开
 * 必FNFE（每5min ERROR无限重试+该窗口索引永久缺失）；Windows下mmap钉住删除必败（GD-C08的
 * 累积未消除+逐链接warn）。修复：清理循环跳过存活条目持有的链接（LogIndex.getFile暴露句柄
 * 路径），随条目生命周期清理；未被持有的残留照删（GD-C08语义保持）。
 * 注：Windows对mmap钉住链接的delete结局呈形态相关（失败warn/静默成功且名残留交替实测到，
 * 打开流钉住亦不稳定），故反向验证锚定修复后恒出现的行为：清理对存活链接产生"skip live
 * index link"跳过日志（修复前不存在该识别）；Linux形态下修复前另致存活链接删除消失（链接
 * 清单断言命中）。
 */
@Fast
@ResourceLock("log4jquery-logger") // 同族捕获Log4jFileManager logger的测试互斥（预防性：Onz/MQ两族竞态的同款，FND19-22复盘小集）
public class TestLinkCleanupSkipsLiveEntries {
	private static final String Active = "zeze.log";
	private static final String Rotated = "zeze.2026-09-08.log";
	private static final LocalDateTime C1Base = LocalDateTime.of(2026, 9, 8, 10, 0);
	private static final LocalDateTime C2Base = C1Base.plusHours(1);

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testRotationKeepsLiveIndexLinks() throws Exception {
		var logDir = Files.createTempDirectory("linkcleanup-links");
		AtomicFileWriter.replace(logDir.resolve(Active), buildLines(C1Base, "c1-", 40).getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop(); // 冻结watch与定时对账，事件由测试直调（生产由watch线程调用）

			// 上次运行残留的stale链接（未被任何条目持有，GD-C08语义应照删）。
			Files.createFile(logDir.resolve("indexLinks").resolve("zeze.log").resolve("0"));
			// log4j轮转磁盘形态：旧内容改名进rotate名，active重建承载新内容。
			Files.move(logDir.resolve(Active), logDir.resolve(Rotated));
			AtomicFileWriter.replace(logDir.resolve(Active),
					buildLines(C2Base, "c2-", 2).getBytes(StandardCharsets.UTF_8));

			invokeOnFileCreated(manager, logDir.resolve(Rotated)); // case-1：改指+补登（补登后同步清理）
			invokeOnFileCreated(manager, logDir.resolve(Active)); // case-0：重复递交守卫跳过

			// 轮转本身不受影响：[R(旧内容索引), active(新索引)]。
			assertEquals(List.of(Rotated, Active), fileNamesOf(manager), "轮转登记不回归");

			// 修复点（Linux/POSIX删半）：存活条目mmap持有的链接不再被删除——每个存活条目保留
			// 自己的增长通道。链接序号推演：构造期active建链接1；case-1改指后补登active建链接2（max）。
			// 修复前仅max号（active的）幸存，rotate条目的链接1被删。
			assertEquals(List.of("1", "2"), numericLinks(logDir),
					"两存活条目=两链接（修复前rotate条目的链接1被删，仅剩2）");

			// 修复点（确定性锚）：直调清理，存活链接被识别并跳过（debug可观测）。修复前无识别、
			// 对存活链接发起删除——本机实测其结局形态相关（失败warn/静默成功且名残留交替出现，
			// 连打开流的钉住也不稳定），故反向验证锚定"跳过"这一修复后恒出现的行为：
			// 修复前无任何跳过日志；经典Windows形态下另伴随delete失败warn（次级断言）。
			// 测试另以输入流钉住链接1，确保"若被发起删除则必失败"（Windows无share-delete句柄）。
			try (var pin = new java.io.FileInputStream(logDir.resolve("indexLinks").resolve("zeze.log").resolve("1").toFile())) {
				try (var capture = new TestLogCapture(Log4jFileManager.class, Level.DEBUG)) {
					invokeRemoveOldLinkFiles(manager);
					assertTrue(capture.anyMessageContains("skip live index link"),
							"存活句柄链接应被识别跳过（修复前无识别，直接对存活链接发起删除）");
					assertFalse(capture.anyMessageContains("delete link error"),
							"存活句柄链接不得发起删除（修复前Windows下删除失败并warn）");
					assertEquals(List.of("1", "2"), numericLinks(logDir), "清理后存活条目链接原位保留");
				}
			}

			// 修复点：存活条目增长通道完好——rotate条目索引经链接路径扩映射可续写
			//（修复前Linux下addIndex按被删链接重开抛FileNotFoundException，续建永久失败）。
			var entries = entriesOf(manager);
			var rotateIndex = entries.get(0).index;
			var growTime = millis(C1Base.plusSeconds(30L * 50));
			assertDoesNotThrow(() -> rotateIndex.addIndex(growTime, 2048),
					"rotate条目索引可经链接续增长");
			assertEquals(2048L, rotateIndex.lowerBound(growTime), "续写记录经mmap可读");
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * GD-C08语义保持：未被存活条目持有的残留链接（stale/污染）仍被清理——held跳过不得
	 * 让"链接随轮转累积"回归。
	 */
	@Test
	public void testStaleLinksStillCleaned() throws Exception {
		var logDir = Files.createTempDirectory("linkcleanup-stale");
		AtomicFileWriter.replace(logDir.resolve(Active), buildLines(C1Base, "c1-", 2).getBytes(StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop();
			Files.createFile(logDir.resolve("indexLinks").resolve("zeze.log").resolve("0")); // stale，无人持有
			Files.move(logDir.resolve(Active), logDir.resolve(Rotated));
			AtomicFileWriter.replace(logDir.resolve(Active),
					buildLines(C2Base, "c2-", 2).getBytes(StandardCharsets.UTF_8));

			invokeOnFileCreated(manager, logDir.resolve(Rotated));
			invokeOnFileCreated(manager, logDir.resolve(Active));

			assertFalse(Files.exists(logDir.resolve("indexLinks").resolve("zeze.log").resolve("0")),
					"未被持有的stale链接仍应清理（GD-C08语义保持）");
			assertEquals(List.of("1", "2"), numericLinks(logDir), "清理后目录恰好剩存活条目的链接");
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/** indexLinks下的数字名链接集合（升序，排除污染条目）。 */
	private static List<String> numericLinks(Path logDir) {
		var names = new TreeSet<String>();
		var links = logDir.resolve("indexLinks").resolve("zeze.log").toFile().listFiles();
		if (links != null)
			for (var link : links)
				if (link.isFile())
					try {
						Long.parseLong(link.getName());
						names.add(link.getName());
					} catch (NumberFormatException e) {
						// 污染条目不计入
					}
		return new ArrayList<>(names);
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

	private static void invokeRemoveOldLinkFiles(Log4jFileManager manager) throws Exception {
		Method method = Log4jFileManager.class.getDeclaredMethod("removeOldLinkFiles");
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
