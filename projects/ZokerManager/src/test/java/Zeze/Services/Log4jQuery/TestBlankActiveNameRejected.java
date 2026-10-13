package Zeze.Services.Log4jQuery;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static harness.DirCleanup.deleteBestEffort;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.Task;

import harness.Fast;

/**
 * LogActive漏配/空白/纯点号必须前置拒绝：这些形态的split("\\.")为空数组，修复前
 * Log4jFileManager构造在fulls[0]抛裸ArrayIndexOutOfBoundsException——配置错误
 * 伪装成无信息的运行时越界。修复后parse期（LogConf(Element)）与manager构造期
 * （LogConf是公共可变POJO，程序化构造绕过parse）都给出指向LogActive字段的明确异常。
 */
@Fast
@Extra
public class TestBlankActiveNameRejected {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testParseRejectsMissingBlankOrDotOnlyActive() throws Exception {
		// 漏配：DOM getAttribute对缺失属性返回""（不报错不告警）
		assertRejectedByParse("<LogConf LogDir=\"log\"/>");
		// 空串与纯空白
		assertRejectedByParse("<LogConf LogActive=\"\"/>");
		assertRejectedByParse("<LogConf LogActive=\"   \"/>");
		// 纯点号：split("\\.")同样丢弃全部分段得空数组（与空串同形态）
		assertRejectedByParse("<LogConf LogActive=\".\"/>");
		assertRejectedByParse("<LogConf LogActive=\"..\"/>");
	}

	@Test
	public void testParseAcceptsRegularActiveName() throws Exception {
		var logConfs = parseConf("""
				<LogServiceConf>
					<LogConf LogActive="zeze.log"/>
				</LogServiceConf>
				""").getLogConfs();
		assertEquals("zeze.log", logConfs.get("zeze.log").logActive, "合法名不得被误拒");
	}

	@Test
	public void testManagerConstructRejectsBlankActiveNotAioobe() throws Exception {
		// 程序化构造路径（无参LogConf+字段赋值是测试/嵌入方的合法用法，parse校验覆盖不到）：
		// 修复前split空数组使构造在fulls[0]抛ArrayIndexOutOfBoundsException。
		var dir = Files.createTempDirectory("blank-active-reject");
		try {
			assertManagerRejectedWith(dir, "");
			assertManagerRejectedWith(dir, null); // 无参构造不设字段的直接形态
		} finally {
			deleteBestEffort(dir);
		}
	}

	private static void assertManagerRejectedWith(Path logDir, String blankActive) {
		var conf = new LogServiceConf.LogConf();
		conf.logActive = blankActive;
		conf.logDir = logDir.toString();
		var ex = assertThrows(IllegalArgumentException.class, () -> new Log4jFileManager(conf),
				"修复前抛ArrayIndexOutOfBoundsException（空split取fulls[0]）");
		assertTrue(ex.getMessage().contains("LogActive"), "异常须指向LogActive字段: " + ex.getMessage());
	}

	private static void assertRejectedByParse(String logConfXml) throws Exception {
		var ex = assertThrows(IllegalStateException.class,
				() -> parseConf("<LogServiceConf>" + logConfXml + "</LogServiceConf>"),
				"LogActive漏配/空白/纯点号应在parse期fail-fast（修复前静默通过）");
		assertTrue(ex.getMessage().contains("LogActive"), "异常须指向LogActive字段: " + ex.getMessage());
	}

	private static LogServiceConf parseConf(String xml) throws Exception {
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml)));
		var conf = new LogServiceConf();
		conf.parse(doc.getDocumentElement());
		return conf;
	}
}
