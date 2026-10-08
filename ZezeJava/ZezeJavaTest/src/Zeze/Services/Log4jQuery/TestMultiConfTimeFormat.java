package Zeze.Services.Log4jQuery;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static harness.DirCleanup.deleteBestEffort;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import Zeze.Builtin.LogService.BCondition;
import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.Log4jLog;
import Zeze.Services.Log4jQuery.Log4jSession;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.Task;

import harness.Fast;

/**
 * GD-C04回归：多LogConf时LogConf(Element)把解析格式写入全局静态Log4jLog.LogTimeFormat，
 * 最后一份覆盖一切：先配置的logName整文件解析失败、查询静默空。修复后格式随LogConf实例
 * 下沉到Log4jFileSession，各manager各自解析。两份格式选互不兼容的分隔符（-与/），
 * 确保错格式解析必失败（宽解析下相近格式可能解析成错误时间而非失败）。
 */
@Fast
@Extra
public class TestMultiConfTimeFormat {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testEachConfUsesOwnTimeFormat() throws Exception {
		var logConfs = parseConf("""
				<LogServiceConf>
					<LogConf LogActive="a.log" LogTimeFormat="yy-MM-dd HH:mm:ss.SSS"/>
					<LogConf LogActive="b.log" LogTimeFormat="yyyy/MM/dd HH:mm:ss.SSS"/>
				</LogServiceConf>
				""").getLogConfs();

		var dirA = Files.createTempDirectory("fmt-a");
		var dirB = Files.createTempDirectory("fmt-b");
		Log4jFileManager managerA = null;
		Log4jFileManager managerB = null;
		try {
			var confDash = logConfs.get("a.log");
			confDash.logDir = dirA.toString();
			var confSlash = logConfs.get("b.log");
			confSlash.logDir = dirB.toString();

			writeLogs(dirA.resolve("a.log"), "yy-MM-dd HH:mm:ss.SSS", "A");
			writeLogs(dirB.resolve("b.log"), "yyyy/MM/dd HH:mm:ss.SSS", "B");

			managerA = new Log4jFileManager(confDash);
			managerB = new Log4jFileManager(confSlash);

			// 两个manager都构造完之后再查询：修复前全局静态已是最后一份（斜杠），a.log全部行解析失败结果为空。
			assertEquals(3, searchCount(managerA), "a.log应按自己的'-'格式解析出3条（修复前0条）");
			assertEquals(3, searchCount(managerB), "b.log应按自己的'/'格式解析出3条");
		} finally {
			if (managerA != null)
				managerA.stop();
			if (managerB != null)
				managerB.stop();
			deleteBestEffort(dirA);
			deleteBestEffort(dirB);
		}
	}

	@Test
	public void testDefaultFormatWhenAttrMissing() throws Exception {
		var conf = new LogServiceConf.LogConf(); // 不设置logTimeFormat：默认"yy-MM-dd HH:mm:ss.SSS"
		var dir = Files.createTempDirectory("fmt-default");
		Log4jFileManager manager = null;
		try {
			conf.logDir = dir.toString();
			conf.logActive = "zeze.log";
			writeLogs(dir.resolve("zeze.log"), "yy-MM-dd HH:mm:ss.SSS", "D");
			manager = new Log4jFileManager(conf);
			assertEquals(3, searchCount(manager), "缺省格式应与默认LogTimeFormat行为一致");
		} finally {
			if (manager != null)
				manager.stop();
			deleteBestEffort(dir);
		}
	}

	private static LogServiceConf parseConf(String xml) throws Exception {
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml)));
		var conf = new LogServiceConf();
		conf.parse(doc.getDocumentElement());
		return conf;
	}

	private static void writeLogs(Path file, String pattern, String tag) throws IOException {
		var fmt = DateTimeFormatter.ofPattern(pattern);
		var base = LocalDateTime.now().minusMinutes(10);
		var sb = new StringBuilder();
		for (var i = 0; i < 3; ++i)
			sb.append(base.plusSeconds(30L * i).format(fmt)).append(' ').append(tag).append(i + 1).append('\n');
		Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
	}

	private static int searchCount(Log4jFileManager manager) throws Exception {
		var session = new Log4jSession(manager);
		var result = new ArrayList<Log4jLog>();
		session.searchContains(result, -1, -1, List.of("zzz"), BCondition.ContainsNone, 100);
		session.close();
		return result.size();
	}
}
