package Zeze.Services.Log4jQuery;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.LogIndex;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.Task;

import harness.Fast;

/**
 * FND25 log4j-03 + FND26 log4j-01 回归：copy-truncate 型轮转（rename 不发生、active 被原地
 * truncate 重写）下 case-1 的头部采样可抢在 truncate 前执行，污染索引滞留 active 条目——
 * 新窗空结果、旧窗被污染 beginTime 引到 EOF，双窗漏读且无自愈。
 * 用例1（offset 维）：采样产物含大 offset 记录，末 offset 超出 truncate 后文件长度。
 * 用例2（时间维，主路径）：采样只抢到 {旧时间, offset=0} 单记录——offset 维对 0 恒假
 * （FND26 log4j-01 立案的判据盲区），两维判据的时间窗失配（文件首条可解析时间落索引
 * [beginTime,endTime] 窗外）必须兜住并弃旧重建。
 */
@Fast
@Extra
public class TestCopyTruncateIndexSelfHeal {
	private static final String Active = "zeze.log";
	private static final LocalDateTime C1Base = LocalDateTime.of(2026, 9, 29, 10, 0);
	private static final LocalDateTime C2Base = C1Base.plusHours(2);

	private Path logDir;
	private Log4jFileManager manager;

	@BeforeEach
	public void before() throws Exception {
		Task.tryInitThreadPool();
		logDir = Files.createTempDirectory("copy-truncate-selfheal");
		AtomicFileWriter.writeAtomically(logDir.resolve(Active), buildLines(C1Base, "c1-", 40).getBytes(StandardCharsets.UTF_8));
		manager = newManager(logDir);
	}

	@AfterEach
	public void after() {
		manager.stop();
		deleteBestEffort(logDir);
	}

	@Test
	public void testTruncatedActiveIndexRebuiltOnReconcile() throws Exception {
		// 前置：装载后 active 索引描述 C1（40 行 ~1.2KB，末 offset 在文件内）。
		assertEquals(millis(C1Base), entriesOf(manager).get(0).index.getBeginTime());

		// copy-truncate 磁盘形态：active 原文件被重写为更短的新内容，索引仍描述旧内容——
		// 末 offset（~1.2KB）超出新文件长度（~60B）= offset 维判据成立。
		manager.stop();
		AtomicFileWriter.writeAtomically(logDir.resolve(Active), buildLines(C2Base, "c2-", 2).getBytes(StandardCharsets.UTF_8));

		invokeReconcile(manager);

		var entries = entriesOf(manager);
		assertEquals(1, entries.size(), "无 rotate 登记，仅 active 条目");
		assertEquals(millis(C2Base), entries.get(0).index.getBeginTime(),
				"truncate 后 reconcile 必须重建污染索引（beginTime 归位新内容）");
		assertTrue(entries.get(0).index.getEndTime() <= millis(C2Base.plusSeconds(30)),
				"重建索引的时间窗必须来自新内容");
	}

	/**
	 * 主路径形态（采样竞速抢到旧内容头）：手工把 active 条目索引替换为单记录
	 * {C1 首条时间, offset=0}、文件已是 C2 新内容——offset 维恒假，时间窗失配判据
	 * （文件首条时间 C2 首条 落索引窗 [C1首条, C1首条] 之外）必须兜住。
	 */
	@Test
	public void testOffsetZeroStaleTimeIndexRebuiltOnReconcile() throws Exception {
		manager.stop();
		AtomicFileWriter.writeAtomically(logDir.resolve(Active), buildLines(C2Base, "c2-", 2).getBytes(StandardCharsets.UTF_8));

		// 构造采样竞速产物：全新索引文件 + 单记录 {C1Base, 0}，直接装入 active 条目
		// （绕过装载配对校验，直击运行期对账自检判据）。
		var pollutedFile = Files.createTempFile("stale-time-index", ".index").toFile();
		var polluted = new LogIndex(pollutedFile);
		polluted.addIndex(List.of(LogIndex.Record.of(millis(C1Base), 0L)));
		entriesOf(manager).get(0).index = polluted;

		invokeReconcile(manager);

		var entries = entriesOf(manager);
		assertEquals(1, entries.size());
		assertEquals(millis(C2Base), entries.get(0).index.getBeginTime(),
				"offset≈0+旧时间形态必须被时间窗判据兜住并重建"
						+ "（单维 offset 判据对该形态恒假=判据盲区）");
	}

	private Log4jFileManager newManager(Path logDir) throws Exception {
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
}
