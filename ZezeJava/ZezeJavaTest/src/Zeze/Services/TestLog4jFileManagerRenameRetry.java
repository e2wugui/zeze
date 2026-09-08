package Zeze.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.LogIndex;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.OutInt;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND2-S3-2 复核结论的锁定（review-2026-09-r2）：rotate 索引 rename 的"本进程 mmap 钉住"
 * 触发机制经 jshell 实验证伪——映射的 section 只钉住【同名目录项】的 rename（同入口
 * map存活rename=false），Log4jFileManager 实际经 indexLinks/N 硬链接映射、case 1 改名的
 * 是另一入口 zeze.log.index（跨入口 rename=true）。本类锁定这两个事实衍生的行为：
 *
 * test1（架构不变量）：active 索引必须经硬链接入口映射，rotate rename 不被自钉——若
 *   loadIndex 被改为直接映射主入口，本用例确定性变红（rename 会开始失败）。
 * test2（降级语义）：第三方占用（java.io 句柄无 FILE_SHARE_DELETE，如杀软/备份）令
 *   rename 失败时维持现行为：索引留在 active 名下被新 active 复用（已知降级），条目
 *   仍登记、仍可查询。
 */
@Fast
public class TestLog4jFileManagerRenameRetry {
	private static final String RotateName = "zeze.2026-09-08.log";

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void test1_RotateIndexRenameUnderHardlinkMapping() throws Exception {
		var logDir = Files.createTempDirectory("zeze-log4j-rename-retry-test");
		var manager = newManager(logDir, true);
		try {
			assertEquals(1, manager.size()); // 启动登记旧active（其LogIndex持有32字节索引的真实mmap）
			freezeAndRotate(manager, logDir);
			// 正序递交rotate目标事件：硬链接映射不钉住另一入口的rename，直接成功（架构不变量）。
			invokeOnFileCreated(manager, Path.of(RotateName));
			invokeOnFileCreated(manager, Path.of("zeze.log"));

			// 架构不变量：硬链接入口的映射不钉住zeze.log.index入口的rename，rotate索引改名成功。
			assertTrue(Files.exists(logDir.resolve(RotateName + ".index")), "rotate索引rename应成功（硬链接映射不自钉）");
			assertEquals(32, Files.size(logDir.resolve(RotateName + ".index")), "rotate索引内容应完整保留");
			assertEquals(0, Files.size(logDir.resolve("zeze.log.index")), "新active索引应为新建的空文件（非旧内容复用）");

			// rotate条目（沿用原索引实例）可查询：seek定位（触达lowerBound）。
			assertEquals(2, manager.size());
			var out = new OutInt();
			assertNotNull(manager.seek(1000L, out), "seek应命中rotate条目");
			assertEquals(0, out.value);
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	@Test
	public void test2_ExternalPinRenameFailKeepsEntriesUsable() throws Exception {
		var logDir = Files.createTempDirectory("zeze-log4j-rename-external-pin-test");
		var manager = newManager(logDir, true);
		// 外部句柄占用（模拟杀软/备份）：rename失败，走现状降级路径。
		try (var pin = pinIndex(logDir)) {
			assertEquals(1, manager.size());
			freezeAndRotate(manager, logDir);
			invokeOnFileCreated(manager, Path.of(RotateName));
			invokeOnFileCreated(manager, Path.of("zeze.log"));

			// 现行为维持：rename失败，索引留在active名下（旧内容被新active复用——已知降级，
			// 钉住源不在本进程，无法在此修复；新active条目因stale beginTime会被seek先命中）；
			// rotate条目必须仍然登记且映射可查询。
			assertFalse(Files.exists(logDir.resolve(RotateName + ".index")), "外部占用下rename应失败");
			assertEquals(32, Files.size(logDir.resolve("zeze.log.index")), "索引应留在active名下（现行为）");
			assertEquals(2, manager.size());
			try (var rotate = manager.get(0)) {
				// 触达条目索引的lowerBound；rotate文件为空故无命中。
				assertFalse(rotate.seek(1000L), "空rotate文件应无命中，但映射必须可查询");
			}
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/** 预置非空active日志索引（两records：time=1000/offset=0，time=2000/offset=16）。 */
	private static Log4jFileManager newManager(Path logDir, boolean prewriteIndex) throws Exception {
		Files.createFile(logDir.resolve("zeze.log")); // 启动时已存在的active（空文件）
		if (prewriteIndex) {
			try (var ch = FileChannel.open(logDir.resolve("zeze.log.index"),
					StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
				var buf = ByteBuffer.allocate(2 * LogIndex.eIndexRecordSize);
				buf.putLong(1000L).putLong(0L).putLong(2000L).putLong(16L);
				buf.flip();
				ch.write(buf);
			}
		}
		var conf = new LogServiceConf.LogConf();
		conf.logActive = "zeze.log";
		conf.logDir = logDir.toString();
		return new Log4jFileManager(conf);
	}

	/**
	 * 外部占用句柄（模拟杀软/备份）：持有期间Windows rename必失败。
	 * 必须用java.io流：不带FILE_SHARE_DELETE；NIO FileChannel带share-delete不阻塞rename
	 * （jshell实验证实：map存活/raf存活/fos存活分别rename=false/false/false，NIO channel=true）。
	 */
	private static FileOutputStream pinIndex(Path logDir) throws IOException {
		return new FileOutputStream(logDir.resolve("zeze.log.index").toFile(), true);
	}

	private static void freezeAndRotate(Log4jFileManager manager, Path logDir) throws IOException {
		// 冻结监视线程与索引定时器：物理rotate产生的真实CREATE事件不再递交，
		// 事件顺序完全由invokeOnFileCreated受控递交（与TestLog4jFileManagerRotateOrder同款）。
		manager.stop();
		Files.move(logDir.resolve("zeze.log"), logDir.resolve(RotateName));
		Files.createFile(logDir.resolve("zeze.log"));
	}

	private static void invokeOnFileCreated(Log4jFileManager manager, Path path) throws Exception {
		Method method = Log4jFileManager.class.getDeclaredMethod("onFileCreated", Path.class);
		method.setAccessible(true);
		method.invoke(manager, path);
	}

	// 尽力删除：LogIndex的mmap由GC cleaner延迟释放，Windows下可能暂时删不掉，留给系统临时目录清理。
	private static void deleteBestEffort(Path dir) {
		try (var walk = Files.walk(dir)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (IOException e) {
					// ignore
				}
			});
		} catch (IOException e) {
			// ignore
		}
	}
}
