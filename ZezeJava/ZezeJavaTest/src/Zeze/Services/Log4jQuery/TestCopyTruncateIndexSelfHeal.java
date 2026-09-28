package Zeze.Services.Log4jQuery;

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
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.Task;

import harness.Fast;

/**
 * FND25 log4j-03 回归：copy-truncate 型轮转（rename 不发生、active 被原地 truncate 重写）
 * 下，case-1 的头部采样抢在 truncate 前采到旧内容——污染索引的末 offset 超长使续建恒
 * EOF 停格，新窗空结果、旧窗被污染 beginTime 引到 EOF，双窗漏读且原状态无自愈。
 * 修复：reconcile 摘除循环后对 active 条目自检（末 offset > 文件长度，判据与装载期/
 * repoint 同源），失配弃污染索引、fresh+头部采样从当前内容重建——一个对账周期内自愈。
 * 判别：truncate 后 reconcile 一轮，active 索引 beginTime 必须归位新内容首条时间
 * （修复前保持旧内容首条时间=污染滞留）。
 */
@Fast
public class TestCopyTruncateIndexSelfHeal {
	private static final String Active = "zeze.log";
	private static final LocalDateTime C1Base = LocalDateTime.of(2026, 9, 29, 10, 0);
	private static final LocalDateTime C2Base = C1Base.plusHours(2);

	private Path logDir;
	private Log4jFileManager manager;

	@BeforeEach
	public void before() throws Exception {
		Task.tryInitThreadPool();
		logDir = Files.createTempDirectory("fnd25-copytruncate");
		AtomicFileWriter.replace(logDir.resolve(Active), buildLines(C1Base, "c1-", 40).getBytes(StandardCharsets.UTF_8));
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = Active;
		logConf.logDir = logDir.toString();
		manager = new Log4jFileManager(logConf);
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

		// copy-truncate 磁盘形态：active 原文件被重写为更短的新内容（truncate+write 等价形态），
		// 索引仍描述旧内容——末 offset（~1.2KB）超出新文件长度（~60B）=污染判据成立。
		manager.stop(); // 冻结 watch/定时器，事件与时序完全受控
		AtomicFileWriter.replace(logDir.resolve(Active), buildLines(C2Base, "c2-", 2).getBytes(StandardCharsets.UTF_8));

		invokeReconcile(manager);

		// 自愈：active 索引弃污染重建，beginTime 归位新内容首条时间（修复前保持 C1 首条=污染滞留）。
		var entries = entriesOf(manager);
		assertEquals(1, entries.size(), "无 rotate 登记，仅 active 条目");
		assertEquals(millis(C2Base), entries.get(0).index.getBeginTime(),
				"truncate 后 reconcile 必须重建污染索引（beginTime 归位新内容）");
		assertTrue(entries.get(0).index.getEndTime() <= millis(C2Base.plusSeconds(30)),
				"重建索引的时间窗必须来自新内容");
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
