package Zeze.Services.Log4jQuery;

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
 * rotate内容时间晚于active首条时，被遮蔽时间窗必须仍可查（FND29 log4jquery-02）。
 * 旧列表不变式"按内容时间有序"+"active恒last"在该形态下不可兼得：时钟前跳后步进回拨校正
 * +两次轮转（或拷入新内容rotate名文件），第二次轮转补登的active首条时间最小却恒居末位，
 * 旧seek从尾按beginTime恒选中active、walker只向前推进，其前rotate条目承载的时间窗
 * 整窗静默漏读（返回空且remain=false），装载路径原样重建无自愈。
 * 修复后列表不变式=轮转序（结构序，不依赖墙钟），active按名显式锚定；seek双锚（尾锚
 * beginTime+头锚endTime窗口覆盖）取早选条目、walker后继文件按查询下界锚定位——正确性
 * 不再落在列表时间序上，冲突形态不可表达。
 */
@Fast
public class TestRotateNewerThanActiveStillQueryable {
	private static final String Active = "zeze.log";
	private static final String Rotate1 = "zeze.2026-09-28.log";
	private static final String Rotate2 = "zeze.2026-09-29.log";
	private static final String Copied = "zeze.2026-09-27.log";
	// 宿主时钟快2小时：日志以未来stamp写入（真实13:00-14:00，stamp 15:00-15:59）。
	private static final LocalDateTime FastBase = LocalDateTime.of(2026, 9, 28, 15, 0);
	// NTP步进回拨校正后的真实时刻。
	private static final LocalDateTime RealBase = LocalDateTime.of(2026, 9, 28, 14, 0);

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * 主链（时钟回拨+两次轮转，运行期事件路径）：世代1全在未来stamp；轮转1后新active以
	 * 未来stamp起头、随即回拨继续写（文件内时间倒退）；轮转2把该内容封进R2（begin=15:59:30），
	 * 补登的新active首条=回拨后时刻（begin=14:01:00）且恒居末位。查未来窗口[15:05,15:55]：
	 * 修复前seek尾锚选中active扫至EOF、walker不回读，整窗漏读返回空；修复后双锚取早命中R1，
	 * 窗口内容有序返回。跨重启装载（rotates内容序+active补末）重建同一轮转序，同窗仍可查。
	 */
	@Test
	public void testClockRollbackRotationWindowSearchable() throws Exception {
		var logDir = Files.createTempDirectory("log4j-rotate-newer-than-active");
		// 世代1（快时钟）：15:00..15:50每10分钟一条+末条15:59。
		AtomicFileWriter.replace(logDir.resolve(Active), buildLines(FastBase, "g1-",
				0, 600, 1200, 1800, 2400, 3000, 3540).getBytes(java.nio.charset.StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop(); // 冻结watch与索引定时器，轮转事件由测试直调（递交顺序受控）

			// 第一次轮转：旧内容rename成R1；新active先以快时钟写一条（15:59:30），
			// NTP回拨校正后继续写（14:00:1x）——active文件内时间倒退，头部stamp不变。
			Files.move(logDir.resolve(Active), logDir.resolve(Rotate1));
			AtomicFileWriter.replace(logDir.resolve(Active), buildLines(FastBase.plusSeconds(3570), "g2-",
					0, -(3570 - 10), -(3570 - 40)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
			invokeOnFileCreated(manager, logDir.resolve(Rotate1)); // case-1：条目改指R1+补登新active
			assertEquals(List.of(Rotate1, Active), fileNamesOf(manager));

			// 第二次轮转：g2内容（横跨回拨点，begin=15:59:30）封进R2；新active首条=14:01:00。
			Files.move(logDir.resolve(Active), logDir.resolve(Rotate2));
			AtomicFileWriter.replace(logDir.resolve(Active), buildLines(RealBase.plusSeconds(60), "g3-",
					0, 30).getBytes(java.nio.charset.StandardCharsets.UTF_8));
			invokeOnFileCreated(manager, logDir.resolve(Rotate2)); // case-1：条目改指R2+补登新active

			// 终态=轮转序：R1(15:00)、R2(15:59:30)、active(14:01:00,末位)——active首条时间
			// 最小却居末位（旧不变式下的"违序"形态，在轮转序下是正典形态）。
			var entries = entriesOf(manager);
			assertEquals(List.of(Rotate1, Rotate2, Active), fileNamesOf(manager));
			assertEquals(millis(FastBase), entries.get(0).index.getBeginTime());
			assertEquals(millis(FastBase.plusSeconds(3570)), entries.get(1).index.getBeginTime());
			assertEquals(millis(RealBase.plusSeconds(60)), entries.get(2).index.getBeginTime());
			assertFutureWindowReturnsGeneration1(manager);

			// 跨重启装载重建：loadRotates（rotates间按内容时间）+active补末——同一轮转序，
			// 指针语义（active按名锚定）与运行期等价，同窗仍可查。
			var manager2 = newManager(logDir);
			try {
				assertEquals(List.of(Rotate1, Rotate2, Active), fileNamesOf(manager2));
				assertFutureWindowReturnsGeneration1(manager2);
			} finally {
				manager2.stop();
			}
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 等价触发向量（拷入，不需时钟事件）：向logDir拷入符合rotate命名、内容时间晚于active首条
	 * 的文件（从另一台时钟超前的服务器拷日志来排查）。reconcile补登（头部采样+归位插入）落进
	 * 同一列表形态，索引续建通道（buildIndex）收敛补登条目余量后，该内容的时间窗必须可查——
	 * 修复前尾锚恒选中active，即便索引完整也永久漏读（拷贝来排查的内容反而不可查）。
	 */
	@Test
	public void testCopiedFutureRotateSearchableAfterReconcile() throws Exception {
		var logDir = Files.createTempDirectory("log4j-copied-future-rotate");
		// active：回拨校正后的正常内容（14:01起3条）。
		AtomicFileWriter.replace(logDir.resolve(Active), buildLines(RealBase.plusSeconds(60), "a-",
				0, 30, 60).getBytes(java.nio.charset.StandardCharsets.UTF_8));
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop();

			// 拷入另一台时钟超前服务器的rotate名文件（内容15:00-15:59）。
			AtomicFileWriter.replace(logDir.resolve(Copied), buildLines(FastBase, "r-",
					0, 600, 1200, 1800, 2400, 3000, 3540).getBytes(java.nio.charset.StandardCharsets.UTF_8));
			invokeReconcile(manager); // 补登：sampleIndexHead采样+addByContentTime归位（active锚位之前）
			invokeBuildIndex(manager); // 索引续建收敛补登条目余量（生产由5分钟周期承担）

			// 列表形态：拷入条目内容时间晚于active首条，归位在active锚位之前。
			var entries = entriesOf(manager);
			assertEquals(List.of(Copied, Active), fileNamesOf(manager));
			assertEquals(millis(FastBase), entries.get(0).index.getBeginTime());
			assertEquals(millis(RealBase.plusSeconds(60)), entries.get(1).index.getBeginTime());

			// 查未来窗口[15:05,15:55]：返回拷入内容的r-1..r-5（修复前返回空且remain=false）。
			var session = new Log4jSession(manager);
			try {
				var result = new ArrayList<Log4jLog>();
				assertFalse(session.searchContains(result, millis(FastBase.plusSeconds(300)),
						millis(FastBase.plusSeconds(3300)), List.of("r-"), BCondition.ContainsAll, 100),
						"窗口内全部命中后应无剩余");
				assertEquals(5, result.size(), "被拷入遮蔽窗口应返回r-1..r-5（修复前整窗静默漏读）");
				for (var i = 0; i < 5; ++i) {
					assertTrue(result.get(i).getLog().contains("r-" + (i + 1)),
							"顺序必须有序：第" + i + "条应为r-" + (i + 1));
					assertEquals(millis(FastBase.plusSeconds(600L * (i + 1))), result.get(i).getTime());
				}
			} finally {
				session.close();
			}
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/** 未来窗口[15:05,15:55]必须返回世代1的g1-1..g1-5（时间序与内容序一致），且查完无剩余。 */
	private static void assertFutureWindowReturnsGeneration1(Log4jFileManager manager) throws Exception {
		var session = new Log4jSession(manager);
		try {
			var result = new ArrayList<Log4jLog>();
			assertFalse(session.searchContains(result, millis(FastBase.plusSeconds(300)),
					millis(FastBase.plusSeconds(3300)), List.of("g1-"), BCondition.ContainsAll, 100),
					"窗口内全部命中后应无剩余（修复前空结果且remain=false，客户端误判查完无数据）");
			assertEquals(5, result.size(), "未来时间窗应返回g1-1..g1-5（修复前整窗静默漏读）");
			for (var i = 0; i < 5; ++i) {
				assertTrue(result.get(i).getLog().contains("g1-" + (i + 1)),
						"顺序必须有序：第" + i + "条应为g1-" + (i + 1));
				assertEquals(millis(FastBase.plusSeconds(600L * (i + 1))), result.get(i).getTime(),
						"时间序与内容序一致");
			}
		} finally {
			session.close();
		}
	}

	private static Log4jFileManager newManager(Path logDir) throws Exception {
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = Active;
		logConf.logDir = logDir.toString();
		return new Log4jFileManager(logConf);
	}

	/** 指定偏移（秒）序列生成日志行——时钟快进/回拨由偏移值的正负与跨度表达。 */
	private static String buildLines(LocalDateTime base, String prefix, long... offsetsSeconds) {
		var fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
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

	private static void invokeBuildIndex(Log4jFileManager manager) throws Exception {
		Method method = Log4jFileManager.class.getDeclaredMethod("buildIndex");
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
