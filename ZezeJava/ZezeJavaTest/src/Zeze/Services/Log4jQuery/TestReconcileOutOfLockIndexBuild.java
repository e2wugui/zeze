package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
 * GD-D01回归：reconcile补登持manager锁同步全量建索引——锁持有/watch线程占用随文件体量线性放大
 * （GB级轮转文件分钟级，恢复场景对账内联在watch本尊上，期间新CREATE事件堆积再触发OVERFLOW：
 * "恢复动作自己制造下一轮丢失"）。修复为方案A两半：
 * 1. 补登两分支（rotate/active）改头部采样——首条记录入索引即登记入列（beginTime可用的最小充分集），
 *    锁内停留毫秒级；
 * 2. buildIndex扩为锁外全条目增量续建——非active条目不持manager锁逐个loadIndex续建（该通道对补登
 *    条目此前不存在：旧代码只推进last==当前名，补登rotate恰插在active之前永远轮不到——只采样不续建
 *    =永久残索引）；active条目维持锁内推进（GD-C03错位检测的串行点，不动）。
 * log4jquery-01修订：rotate补登的采样与续建闭合到同一执行面——reconcile锁内采样补登后，对本次
 * 补登集合在锁外立即执行与buildIndex锁外段相同的loadIndex续建（timer路径本就同tick闭合，watch
 * 线程路径原先隔着最多一个buildIndex周期的空窗，窗内seek双锚可双双漏选采样态条目，其承载时间窗
 * 整窗空结果且remain=false）。锁持有解耦不变：采样仍在锁内毫秒级，续建全程锁外。
 */
@Fast
public class TestReconcileOutOfLockIndexBuild {
	private static final String Active = "zeze.log";
	// 大文件计时下限标定：15万行×30s间隔（全量索引15万条），旧代码锁内同步全扫 SimpleDateFormat
	// 逐行解析数百毫秒起步；采样只解析1行。阈值取两形态之间的数量级空档。
	private static final int BigLines = 150_000;
	private static final long MaxReconcileMs = 250;

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * 第一半（rotate补登分支）：大文件补登的锁内登记毫秒级（与文件体量解耦）、续建全程锁外
	 * （查询不被大文件续建阻塞）；补登返回时索引已即时续建到全量（log4jquery-01，不再有
	 * 采样态空窗），条目可被seek选中且定位精确。
	 */
	@Test
	public void testReconcileSamplesHeadFastAndSeekable() throws Exception {
		var logDir = Files.createTempDirectory("indexbuild-sample");
		var base = LocalDateTime.of(2026, 1, 1, 0, 0);
		writeLogs(logDir.resolve(Active), LocalDateTime.now(), "active-", 3);
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop(); // 模拟watch事件丢失：补登只由直调对账发生
			AtomicFileWriter.replace(logDir.resolve("zeze.2026-01-01.log"),
					buildLines(base, BigLines).getBytes(StandardCharsets.UTF_8));

			// 锁持有解耦断言（log4jquery-01起reconcile=锁内采样登记+锁外即时续建，总墙钟随文件
			// 体量增长是有意代价——watch线程IO有既有先例；要保的是锁只在采样段持有）。对账在后台
			// 线程执行，主线程轮询观察：（a）条目毫秒级登记入列（锁内段完成）；（b）对账仍在进行
			//（大文件续建中）时锁已可空闲获取（查询路径不被续建钉住）。
			var reconcileDone = new CountDownLatch(1);
			var registeredMs = new AtomicLong(-1);
			var lockFreeDuringReconcile = new AtomicBoolean(false);
			var beginNano = System.nanoTime();
			var reconciler = new Thread(() -> {
				try {
					invokeReconcile(manager);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					reconcileDone.countDown();
				}
			});
			reconciler.start();
			while (!reconcileDone.await(1, TimeUnit.MILLISECONDS)) {
				if (registeredMs.get() < 0 && manager.size() == 2)
					registeredMs.set((System.nanoTime() - beginNano) / 1_000_000);
				if (registeredMs.get() >= 0 && manager.tryLock()) {
					manager.unlock();
					lockFreeDuringReconcile.set(true);
				}
			}
			assertTrue(registeredMs.get() >= 0 && registeredMs.get() < MaxReconcileMs,
					"补登登记（锁内段）应毫秒级（与文件体量解耦），实际" + registeredMs.get() + "ms");
			assertTrue(lockFreeDuringReconcile.get(),
					"续建须在锁外执行：对账进行期间（大文件续建中）锁应可获取（修复前锁内全量扫描恒不可得）");

			assertEquals(List.of("zeze.2026-01-01.log", Active), fileNamesOf(manager), "rotate补登在active之前");

			// 即时续建断言（log4jquery-01）：补登返回时索引已收敛到全量——不再有"采样后等5分钟
			// 周期"的空窗（空窗内seek双锚可双双漏选该条目，其承载时间窗整窗空结果且remain=false）。
			var entry = entriesOf(manager).get(0);
			assertEquals(millis(base), entry.index.getBeginTime(), "首条记录入索引，beginTime可用");
			var last = millis(base.plusSeconds(30L * (BigLines - 1)));
			assertTrue(entry.index.lowerBound(last) >= 0, "补登即时续建覆盖到末条（修复前采样态只有首条）");
			assertEquals(-1L, entry.index.lowerBound(last + 5_000), "末条之后无越界记录（增量从endTime续，不重不漏）");

			// 中点查询定位精确（旧采样态走offset 0回退+detailSeek线性定位——慢而不错；全量索引下
			// 精确跳转，结果同一）。
			var midTime = millis(base.plusSeconds(30L * 1000));
			var out = new OutInt();
			var session = manager.seek(midTime, out);
			assertNotNull(session, "补登后的条目即可被seek选中");
			assertEquals("zeze.2026-01-01.log", session.getFile().getName());
			assertEquals(midTime, session.current().getTime(), "定位精确（首条≥time的记录）");
			assertTrue(session.current().getLog().contains("line-01000"), "定位到正确行内容");
			session.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 第二半（全条目续建通道）：非active条目由buildIndex锁外增量续建覆盖（该通道对补登条目此前
	 * 不存在：旧代码只推进last==当前名，补登rotate永远轮不到=永久残索引）。log4jquery-01后补登
	 * 条目在reconcile内即时续建，本通道转为兜底+边界维持：一次buildIndex幂等重扫两个rotate条目，
	 * 索引覆盖到末条且边界精确（无越界、无重复），全部条目内容可查。
	 */
	@Test
	public void testBuildIndexContinuesAllNonActiveEntries() throws Exception {
		var logDir = Files.createTempDirectory("indexbuild-continue");
		var base1 = LocalDateTime.of(2026, 1, 1, 0, 0);
		var base2 = LocalDateTime.of(2026, 2, 1, 0, 0);
		var lines = 200; // 每行间隔30s：全量索引=200条
		writeLogs(logDir.resolve(Active), LocalDateTime.now(), "active-", 3);
		var manager = newManager(logDir);
		try {
			assertEquals(1, manager.size());
			manager.stop();
			AtomicFileWriter.replace(logDir.resolve("zeze.2026-01-01.log"),
					buildLines(base1, lines).getBytes(StandardCharsets.UTF_8));
			AtomicFileWriter.replace(logDir.resolve("zeze.2026-02-01.log"),
					buildLines(base2, lines).getBytes(StandardCharsets.UTF_8));

			invokeReconcile(manager);
			assertEquals(List.of("zeze.2026-01-01.log", "zeze.2026-02-01.log", Active), fileNamesOf(manager));
			var entries = entriesOf(manager);
			// 即时续建断言（log4jquery-01）：补登返回时索引已收敛——不再有"采样态等周期续建"的
			// 空窗（修复前此处只有首条采样记录）。
			for (var base : List.of(base1, base2))
				assertTrue(entries.get(base == base1 ? 0 : 1).index.lowerBound(millis(base.plusSeconds(60))) >= 0,
						"补登即时续建：次条记录已在索引（修复前采样态只有首条）");

			// 一次buildIndex：锁外遍历全部非active条目逐个续建（新→旧序），再锁内推进active。
			// log4jquery-01后本通道对补登条目是兜底（即时续建单条目失败/进程在两半之间重启时由
			// 周期补齐），幂等重扫不重不漏。
			invokeBuildIndex(manager);

			var r1 = entries.get(0).index;
			var last1 = millis(base1.plusSeconds(30L * (lines - 1)));
			assertTrue(r1.lowerBound(last1) >= 0, "续建应覆盖到末条记录（只采样不续建=永久残索引）");
			assertEquals(-1L, r1.lowerBound(last1 + 5_000), "末条之后无越界记录（增量从endTime续，不重不漏）");
			var r2 = entries.get(1).index;
			var last2 = millis(base2.plusSeconds(30L * (lines - 1)));
			assertTrue(r2.lowerBound(last2) >= 0, "全部非active条目均被续建（不止last==当前名）");
			assertEquals(-1L, r2.lowerBound(last2 + 5_000), "续建边界精确");

			// 续建后整窗查询正确（两rotate各200行+active 3行）。
			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();
			assertFalse(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 1000));
			assertEquals(lines * 2 + 3, result.size(), "全条目内容可查");
			session.close();
		} finally {
			manager.stop();
			deleteBestEffort(logDir);
		}
	}

	/**
	 * active补登分支对称采样（GD-C03改指后的active按新索引补登走同一形态）：条目消失再回归时
	 * active补登分支对称采样（GD-C03改指后的active按新索引补登走同一形态）：active磁盘已存在而
	 * 条目未登记（构造时缺失、watch事件丢失）时，补登采样毫秒级、线性回退查询正确；随后buildIndex
	 * 锁内段（active维持锁内推进现状，GD-C03串行点不动）补齐到全量。
	 */
	@Test
	public void testActiveReconcileSamplesThenBuildIndexCompletes() throws Exception {
		var logDir = Files.createTempDirectory("indexbuild-active");
		var base = LocalDateTime.of(2026, 3, 2, 0, 0);
		var manager = newManager(logDir); // 构造时active不存在：条目空，补登由直调对账发生
		try {
			assertEquals(0, manager.size());
			manager.stop();
			AtomicFileWriter.replace(logDir.resolve(Active),
					buildLines(base, 5).getBytes(StandardCharsets.UTF_8));

			invokeReconcile(manager);
			assertEquals(List.of(Active), fileNamesOf(manager), "active条目补登");

			// 采样断言：只有首条记录（修复前补登即锁内全量建索引）。
			var entry = entriesOf(manager).get(0);
			assertEquals(millis(base), entry.index.getBeginTime());
			assertEquals(-1L, entry.index.lowerBound(millis(base.plusSeconds(35))), "active补登同样只采样头部");

			// 补齐窗口内线性回退查询正确（慢而不错）：begin=+60s起应返回line-00002..line-00004。
			var session = new Log4jSession(manager);
			var result = new ArrayList<Log4jLog>();
			assertFalse(session.searchContains(result, millis(base.plusSeconds(60)), -1,
					List.of("zzz"), BCondition.ContainsNone, 100));
			assertEquals(3, result.size(), "未覆盖区间线性定位：base+60s起应为line-00002..line-00004");
			assertTrue(result.get(0).getLog().contains("line-00002"));
			assertTrue(result.get(2).getLog().contains("line-00004"));
			session.close();

			// active条目锁内段补齐（维持锁内推进现状）：索引覆盖到末条。
			invokeBuildIndex(manager);
			assertTrue(entry.index.lowerBound(millis(base.plusSeconds(30L * 4))) >= 0,
					"active索引应由锁内段续建补齐");
			session = new Log4jSession(manager);
			result.clear();
			assertFalse(session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 100));
			assertEquals(5, result.size(), "补齐后整窗可查");
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

	private static String buildLines(LocalDateTime base, int count) {
		var fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
		var sb = new StringBuilder();
		for (var i = 0; i < count; ++i)
			sb.append(base.plusSeconds(30L * i).format(fmt)).append(' ')
					.append("line-").append(String.format("%05d", i)).append('\n');
		return sb.toString();
	}

	private static void writeLogs(Path file, LocalDateTime base, String prefix, int count) throws Exception {
		var fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
		var sb = new StringBuilder();
		for (var i = 0; i < count; ++i)
			sb.append(base.plusSeconds(30L * i).format(fmt)).append(' ')
					.append(prefix).append(i).append('\n');
		AtomicFileWriter.replace(file, sb.toString().getBytes(StandardCharsets.UTF_8));
	}

	private static long millis(LocalDateTime time) {
		return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
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
