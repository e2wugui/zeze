package Zeze.Services.Log4jQuery;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static harness.DirCleanup.deleteBestEffort;

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

import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.Log4jSession;
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.OutInt;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import harness.Fast;

/**
 * seek 的条目选择对并发摘除必须快照一致（FND35 log4jquery-01 回归）。
 * 修复前：双锚扫描在活列表上按下标计算 pick，再以裸下标 files.get(pick) 二次取条目
 * ——扫描与 get 之间并发摘除（removeMissingFile/reconcile 摘除循环）左移列表时，
 * pick 界内却指向偏移后的另一条目（IOOBE 兜底只覆盖越界形态），偏移条目被当作锚定
 * 结果发布给 walker；walker 只向前推进，位于实际打开条目之前的原锚定条目整窗静默
 * 漏读，且 beginTime 去重哨兵使同参数翻页短路，漏读持续到会话重建。
 * 修复后：双锚扫描与取条目在同一个 COW 快照（toArray，与 buildIndex 同型）上以
 * 条目引用衔接——任何并发收缩下打开的要么是被锚点选中的条目、要么其已被摘除
 * （FNFE 走既有降级收敛），发布下标改取该引用的当前存活位置。
 */
@Fast
@Extra
public class TestSeekPickSurvivesConcurrentRemovalShift {
	private static final String Active = "zeze.log";
	private static final String Rotate1 = "zeze.2026-09-01.log";
	private static final String Rotate2 = "zeze.2026-09-02.log";
	private static final String Rotate3 = "zeze.2026-09-03.log";
	private static final LocalDateTime Day1 = LocalDateTime.of(2026, 9, 1, 10, 0);
	private static final LocalDateTime Day2 = LocalDateTime.of(2026, 9, 2, 11, 0);
	private static final LocalDateTime Day3 = LocalDateTime.of(2026, 9, 3, 12, 0);

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * 确定性交错：查询时间 T=11:15 时双锚（尾锚 beginTime、头锚 endTime）都选中 R2
	 * （列表 [R1(10:00-10:30), R2(11:00-11:30), R3(12:00-12:30), active(空)]）。
	 * 注入点（扫描完成、取条目前）并发摘除 R1：外部清理删除文件 + open 走 FNFE 持锁摘条目，
	 * 列表左移为 [R2, R3, active]。修复前 files.get(pick=1) 开出 R3——R2 的 11:20/11:30
	 * 两整窗漏读；修复后快照引用锚定仍开 R2，从首条 >=T 的 11:20 起读。
	 */
	@Test
	public void testPickStaysOnAnchorSelectedEntryWhenListShifts() throws Exception {
		var logDir = Files.createTempDirectory("log4j-seek-pick-shift");
		writeRotate(logDir, Rotate1, Day1, "r1-", 0, 600, 1200, 1800);
		writeRotate(logDir, Rotate2, Day2, "r2-", 0, 600, 1200, 1800);
		writeRotate(logDir, Rotate3, Day3, "r3-", 0, 600, 1200, 1800);
		Files.createFile(logDir.resolve(Active)); // 空active：空索引，双锚自然跳过
		var manager = newManager(logDir);
		try {
			assertEquals(4, manager.size());
			manager.stop(); // 冻结watch与索引定时器：摘除时序完全由注入点控制

			manager.setSeekBeforePickOpenHookForTest(() -> {
				manager.setSeekBeforePickOpenHookForTest(null); // 一次性注入
				try {
					var r1 = manager.entryAt(0);
					Files.delete(logDir.resolve(Rotate1)); // 外部保留期清理删除.log
					assertEquals(null, manager.open(r1), "已删文件的条目打开应FNFE摘除");
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			var out = new OutInt();
			var outEntry = new OutObject<Log4jFileManager.Log4jFile>();
			var session = manager.seek(millis(Day2.plusSeconds(900)), out, outEntry); // T=11:15
			try {
				assertNotNull(session, "双锚命中R2，seek应成功");
				assertEquals(Rotate2, outEntry.value.file.getName(),
						"打开的必须仍是双锚选中的条目（修复前：摘除左移后裸下标get开出R3）");
				assertEquals(0, out.value, "发布下标=锚定条目摘除后的存活位置（walker按引用重同步）");
				var logs = new ArrayList<String>();
				while (session.hasNext())
					logs.add(session.next().getLog());
				assertEquals(2, logs.size(), "会话应从R2首条>=T（11:20）起读尽（修复前从R3的12:00起，R2整窗漏读）");
				assertEquals(true, logs.get(0).contains("r2-2"), "首条应为R2的11:20记录");
				assertEquals(true, logs.get(1).contains("r2-3"), "末条应为R2的11:30记录");
			} finally {
				session.close();
			}
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 行为级红（同交错经完整查询会话）：窗口 [11:15, 11:35] 搜 r2- 必须返回 r2-2/r2-3。
	 * 修复前 walker 从偏移条目 R3 起只向前推进，R2 整窗漏读、查完无剩余（remain=false），
	 * 客户端误判该时段无日志；修复后窗口内容完整返回。
	 */
	@Test
	public void testSearchWindowCompleteWhenSeekPickShifts() throws Exception {
		var logDir = Files.createTempDirectory("log4j-seek-pick-shift-search");
		writeRotate(logDir, Rotate1, Day1, "r1-", 0, 600, 1200, 1800);
		writeRotate(logDir, Rotate2, Day2, "r2-", 0, 600, 1200, 1800);
		writeRotate(logDir, Rotate3, Day3, "r3-", 0, 600, 1200, 1800);
		Files.createFile(logDir.resolve(Active));
		var manager = newManager(logDir);
		try {
			assertEquals(4, manager.size());
			manager.stop();

			manager.setSeekBeforePickOpenHookForTest(() -> {
				manager.setSeekBeforePickOpenHookForTest(null);
				try {
					var r1 = manager.entryAt(0);
					Files.delete(logDir.resolve(Rotate1));
					assertEquals(null, manager.open(r1));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			var session = new Log4jSession(manager);
			try {
				var result = new ArrayList<Zeze.Services.Log4jQuery.Log4jLog>();
				var remain = session.searchContains(result, millis(Day2.plusSeconds(900)),
						millis(Day2.plusSeconds(2100)), List.of("r2-"),
						Zeze.Builtin.LogService.BCondition.ContainsAll, 100);
				assertEquals(false, remain, "窗口内全部命中后应无剩余");
				assertEquals(2, result.size(), "被偏移跳过的R2窗口必须完整返回（修复前整窗静默漏读）");
				assertEquals(true, result.get(0).getLog().contains("r2-2"));
				assertEquals(true, result.get(1).getLog().contains("r2-3"));
			} finally {
				session.close();
			}
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

	private static void writeRotate(Path logDir, String name, LocalDateTime base,
								   String prefix, long... offsetsSeconds) throws Exception {
		AtomicFileWriter.replace(logDir.resolve(name),
				buildLines(base, prefix, offsetsSeconds).getBytes(StandardCharsets.UTF_8));
	}

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
}
