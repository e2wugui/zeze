package Zeze.Services.Log4jQuery;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static harness.DirCleanup.deleteBestEffort;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import harness.Fast;

/**
 * LogTimeFormat/LogDatePattern/CharsetName 非法值不得静默通过 parse 进入运行期：
 * 修复前三项只做空白回退，非法模式要到首次轮转/首次查询才以运行期异常暴露——
 * LogTimeFormat 在 Log4jFileSession 构造的 tryNext→Log4jLog.tryParse 内 new
 * SimpleDateFormat 抛 IAE，而构造失败回收只 catch IOException，已打开的
 * FileChannel 无人关闭（每查询/每轮转泄漏一个 fd）；LogDatePattern 在
 * testFileName 内抛 IAE 使对账/轮转整轮被吞（rotate 永不登记）。修复后 parse
 * 期 fail-fast 给出指向字段的异常（对齐 LogActive 先例）；程序化构造路径
 * （绕过 parse）的会话构造失败也先回收 RAF 再重抛原异常。
 */
@Fast
@Extra
public class TestIllegalLogFormatConfRejected {
	@Test
	public void testParseRejectsIllegalTimeFormat() throws Exception {
		// 未配对引号：SimpleDateFormat 构造即抛 IllegalArgumentException
		var ex = assertThrows(IllegalStateException.class, () -> parseConf("""
				<LogServiceConf>
					<LogConf LogActive="zeze.log" LogTimeFormat="yy-MM-dd HH:mm:ss.SSS'"/>
				</LogServiceConf>
				"""), "非法LogTimeFormat应在parse期fail-fast（修复前静默通过）");
		assertTrue(ex.getMessage().contains("LogTimeFormat"), "异常须指向LogTimeFormat字段: " + ex.getMessage());
	}

	@Test
	public void testParseRejectsIllegalDatePattern() throws Exception {
		var ex = assertThrows(IllegalStateException.class, () -> parseConf("""
				<LogServiceConf>
					<LogConf LogActive="zeze.log" LogDatePattern=".yyyy-MM-dd'"/>
				</LogServiceConf>
				"""), "非法LogDatePattern应在parse期fail-fast（修复前静默通过）");
		assertTrue(ex.getMessage().contains("LogDatePattern"), "异常须指向LogDatePattern字段: " + ex.getMessage());
	}

	@Test
	public void testParseRejectsUnsupportedCharsetName() throws Exception {
		var ex = assertThrows(IllegalStateException.class, () -> parseConf("""
				<LogServiceConf>
					<LogConf LogActive="zeze.log" CharsetName="no-such-charset"/>
				</LogServiceConf>
				"""), "不受支持的CharsetName应在parse期fail-fast（修复前静默通过）");
		assertTrue(ex.getMessage().contains("CharsetName"), "异常须指向CharsetName字段: " + ex.getMessage());
	}

	@Test
	public void testParseAcceptsValidCustomFormats() throws Exception {
		// 合法自定义值不得被误拒（默认值同理经同一校验）
		var conf = parseConf("""
				<LogServiceConf>
					<LogConf LogActive="a.log" LogTimeFormat="yyyy/MM/dd HH:mm:ss,SSS"
						LogDatePattern=".yyyy-MM-dd-HH" CharsetName="utf-16"/>
					<LogConf LogActive="b.log"/>
				</LogServiceConf>
				""");
		var custom = conf.getLogConfs().get("a.log");
		assertEquals("yyyy/MM/dd HH:mm:ss,SSS", custom.logTimeFormat);
		assertEquals(".yyyy-MM-dd-HH", custom.logDatePattern);
		assertEquals("utf-16", custom.charsetName);
		assertEquals("yy-MM-dd HH:mm:ss.SSS", conf.getLogConfs().get("b.log").logTimeFormat,
				"缺省回退值不得被误拒");
	}

	@Test
	public void testSessionCtorFailureReleasesFile() throws Exception {
		// 程序化构造路径（LogConf可变POJO绕过parse校验）：非法时间格式的IAE须原样穿透
		// 构造（异常形态即配置错误的载体），且构造失败必须回收已打开的RAF——修复前
		// catch(IOException)不命中RuntimeException，FileChannel在异常穿透下泄漏。
		var dir = Files.createTempDirectory("bad-time-format");
		try {
			var log = dir.resolve("zeze.log");
			Files.write(log, "26-09-29 10:00:00.000 INFO hello\n".getBytes(StandardCharsets.UTF_8));
			var before = countProcFds();
			for (int i = 0; i < 32; ++i)
				assertThrows(IllegalArgumentException.class, () -> new Log4jFileSession(log.toFile(), null,
						"utf-8", "yy-MM-dd HH:mm:ss.SSS'"), "非法时间格式的IAE须穿透构造");
			if (before >= 0) { // /proc/self/fd可读的平台（Linux）上量化fd回收；其余平台本断言自跳过
				var after = countProcFds();
				assertTrue(after < before + 8, "32次构造失败后fd不得净增(泄漏): before=" + before + " after=" + after);
			}
		} finally {
			deleteBestEffort(dir);
		}
	}

	private static int countProcFds() {
		try (Stream<Path> s = Files.list(Path.of("/proc/self/fd"))) {
			return (int)s.count();
		} catch (Exception e) {
			return -1; // 平台不可读（Windows等）：调用方跳过fd断言
		}
	}

	private static LogServiceConf parseConf(String xml) throws Exception {
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml)));
		var conf = new LogServiceConf();
		conf.parse(doc.getDocumentElement());
		return conf;
	}
}
