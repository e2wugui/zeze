package Zeze.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.LogIndex;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.OutInt;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND24 log4j-01 新契约（轮转索引移交不再 rename 任何存活 mmap 的 inode）：
 * 轮转时 rotate 名下以硬链接接管 active 索引（同 inode 零复制，Windows 实测 createLink
 * 对经另一链接映射的 inode 可行），active 换 indexLinks/(max+1) 全新 inode；current.index
 * 只在装载期重建为链接（运行期滞后一代）。本类锁定两个行为：
 *
 * test1（移交成功路径）：内容自洽的 current 索引经装载配对校验接管后轮转——R.index 硬链接
 *   接管同 inode（记录完整保留），active 条目挂全新空索引，两条目均可查询。
 * test2（GD-C01 中止语义的移交失败形态）：rotate 目标名被既存文件占用（注入移交失败）时
 *   中止改指与补登——不登记 rotate 条目，active 条目保持可用，由后续 reconcile 收敛。
 */
@Fast
public class TestLog4jFileManagerRenameRetry {
	private static final String RotateName = "zeze.2026-09-08.log";
	private static final String Active = "zeze.log";
	// 两行内容（30s 间隔），手写索引两条记录与内容配对（首条时间=索引 beginTime，末 offset 在文件长度内）。
	private static final LocalDateTime Base = LocalDateTime.of(2026, 9, 8, 10, 0);

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void test1_RotateIndexTakeoverByHardlinkKeepsRecords() throws Exception {
		var logDir = Files.createTempDirectory("zeze-log4j-rename-retry-test");
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size()); // 启动配对接管（32字节索引，active条目持有真实映射）
			freezeAndRotate(manager, logDir);
			// 正序递交rotate目标事件与active重建事件。
			invokeOnFileCreated(manager, Path.of(RotateName));
			invokeOnFileCreated(manager, Path.of(Active));

			// 移交成功：R.index 硬链接接管同 inode，记录完整保留。
			assertTrue(Files.exists(logDir.resolve(RotateName + ".index")), "rotate索引接管应成功");
			assertEquals(32, Files.size(logDir.resolve(RotateName + ".index")), "rotate索引记录应完整保留");

			// active 条目挂全新空索引（新 inode 从零开始）；current.index 为装载期重建的滞后链接（仍在）。
			var entries = entriesOf(manager);
			assertEquals(2, entries.size());
			// 空索引哨兵：begin=Long.MAX_VALUE/end=0（无记录，任何时间窗不命中）。
			assertEquals(Long.MAX_VALUE, entries.get(1).index.getBeginTime(), "active条目应为全新空索引");
			assertEquals(0, entries.get(1).index.getEndTime(), "active条目应为全新空索引");
			assertTrue(Files.exists(logDir.resolve(Active + ".index")), "current.index为装载期链接（滞后一代，运行期不触碰）");

			// rotate条目（接管的原索引实例）可查询：seek定位（触达lowerBound）。
			var out = new OutInt();
			assertNotNull(manager.seek(millis(Base), out), "seek应命中rotate条目");
			assertEquals(0, out.value);
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	@Test
	public void test2_TransferFailAbortsRepointKeepsEntriesUsable() throws Exception {
		var logDir = Files.createTempDirectory("zeze-log4j-takeover-fail-test");
		// 既存目标名=注入移交失败（新契约下外部句柄不再钉住轮转；FileAlreadyExists 按既存中止）。
		Files.writeString(logDir.resolve(RotateName + ".index"), "occupied");
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			freezeAndRotate(manager, logDir);
			invokeOnFileCreated(manager, Path.of(RotateName));
			invokeOnFileCreated(manager, Path.of(Active));

			// GD-C01 中止语义：移交失败即中止改指与补登——不登记rotate条目（条目仍指active名，
			// 由下一轮reconcile摘除+常规补登收敛）；既存目标文件原样未被触碰。
			assertEquals(1, manager.size(), "移交失败不得登记rotate条目");
			assertEquals("occupied".length(), Files.size(logDir.resolve(RotateName + ".index")),
					"既存目标文件必须原样保留（移交未发生）");
			assertEquals(32, Files.size(logDir.resolve(Active + ".index")), "索引应留在active条目名下（中止改指）");
			try (var active = manager.get(0)) {
				assertNotNull(active, "中止后条目必须仍可查询");
			}
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/** 预置与内容配对的active日志索引（两records：time=首条/offset=0，time=次条/offset=首行长度）。 */
	private static Log4jFileManager newManager(Path logDir) throws Exception {
		AtomicFileWriter.replace(logDir.resolve(Active), buildLines().getBytes(StandardCharsets.UTF_8));
		var line0Len = buildLines().indexOf('\n') + 1;
		try (var ch = FileChannel.open(logDir.resolve(Active + ".index"),
				StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
			var buf = ByteBuffer.allocate(2 * LogIndex.eIndexRecordSize);
			buf.putLong(millis(Base)).putLong(0L);
			buf.putLong(millis(Base.plusSeconds(30))).putLong(line0Len);
			buf.flip();
			ch.write(buf);
		}
		var conf = new LogServiceConf.LogConf();
		conf.logActive = Active;
		conf.logDir = logDir.toString();
		return new Log4jFileManager(conf);
	}

	private static String buildLines() {
		var fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
		var sb = new StringBuilder();
		sb.append(Base.format(fmt)).append(" c-00\n");
		sb.append(Base.plusSeconds(30).format(fmt)).append(" c-01\n");
		return sb.toString();
	}

	private static long millis(LocalDateTime time) {
		return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	private static void freezeAndRotate(Log4jFileManager manager, Path logDir) throws Exception {
		// 冻结监视线程与索引定时器：物理rotate产生的真实CREATE事件不再递交，
		// 事件顺序完全由invokeOnFileCreated受控递交（与TestLog4jFileManagerRotateOrder同款）。
		manager.stop();
		Files.move(logDir.resolve(Active), logDir.resolve(RotateName));
		Files.createFile(logDir.resolve(Active));
	}

	private static void invokeOnFileCreated(Log4jFileManager manager, Path path) throws Exception {
		Method method = Log4jFileManager.class.getDeclaredMethod("onFileCreated", Path.class);
		method.setAccessible(true);
		method.invoke(manager, path);
	}

	@SuppressWarnings("unchecked")
	private static List<Log4jFileManager.Log4jFile> entriesOf(Log4jFileManager manager) throws Exception {
		Field field = Log4jFileManager.class.getDeclaredField("files");
		field.setAccessible(true);
		return (List<Log4jFileManager.Log4jFile>)field.get(manager);
	}

	// 尽力删除：LogIndex的mmap由GC cleaner延迟释放，Windows下可能暂时删不掉，留给系统临时目录清理。
	private static void deleteBestEffort(Path dir) {
		try (var walk = Files.walk(dir)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (Exception e) {
					// ignore
				}
			});
		} catch (Exception e) {
			// ignore
		}
	}
}
