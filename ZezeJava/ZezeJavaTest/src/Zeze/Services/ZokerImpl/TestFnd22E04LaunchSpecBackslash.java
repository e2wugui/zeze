package Zeze.Services.ZokerImpl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND22 GE-C04：parseLaunchSpec 用 Properties.load 解析 service.properties——值内反斜杠按
 * 转义还原（{@code C:\srv\app.exe}→{@code C:srvapp.exe}，未识别转义直接丢反斜杠，Java 规范
 * 行为），Windows 路径形态的 command/env/args 静默损坏且 eStartFail 日志显示损坏后命令。
 * run.pid 的自写解析（RunPidRecord.parse）当时已为此明确弃用 Properties——部署描述对齐同一
 * 标准：行式 key=value（首个'='分隔，值原样保留），反斜杠无任何转义语义。
 * 纯文件直构（parseLaunchSpec 包内静态），全平台可跑；红=现解析 mangle、绿=新解析原样。
 */
@Fast
public class TestFnd22E04LaunchSpecBackslash {

	private static ServiceManager.LaunchSpec parse(Path versionDir, String content) throws IOException {
		Files.createDirectories(versionDir);
		Files.writeString(versionDir.resolve(ServiceManager.SERVICE_PROPERTIES_NAME), content);
		return ServiceManager.parseLaunchSpec(versionDir.toFile());
	}

	/** 核心红点：Windows 路径 command 原样保留（修复前：C:\srv\app.exe → C:srvapp.exe）。 */
	@Test
	public void testWindowsPathCommandVerbatim(@TempDir Path tempDir) throws Exception {
		var spec = parse(tempDir.resolve("v1"), "command=C:\\srv\\app.exe\n");
		assertEquals(List.of("C:\\srv\\app.exe"), spec.command,
				"反斜杠不得按 Properties 转义还原/丢弃");
	}

	/** env 值内的 Windows 路径（含空格）原样保留；args 中的反斜杠路径同样原样。 */
	@Test
	public void testBackslashInEnvAndArgsVerbatim(@TempDir Path tempDir) throws Exception {
		var spec = parse(tempDir.resolve("v1"),
				"command=cmd\n"
						+ "args=/c C:\\tools\\run.bat\n"
						+ "env=JAVA_HOME=C:\\Program Files\\java,DATA=C:\\d\\x\n");
		assertEquals(List.of("cmd", "/c", "C:\\tools\\run.bat"), spec.command);
		assertEquals(Map.of("JAVA_HOME", "C:\\Program Files\\java", "DATA", "C:\\d\\x"), spec.env);
	}

	/** 转义写法不再还原（契约变更钉）：双反斜杠就是两个反斜杠（Properties 下会还原为单个）。 */
	@Test
	public void testDoubleBackslashNotUnescaped(@TempDir Path tempDir) throws Exception {
		var spec = parse(tempDir.resolve("v1"), "command=C:\\\\srv\\\\app.exe\n");
		assertEquals(List.of("C:\\\\srv\\\\app.exe"), spec.command, "双反斜杠原样保留，无转义语义");
	}

	/** 既有语义回归钉（FND19-21 契约不变）：trim/空白分隔/逗号分隔/错误形态。 */
	@Test
	public void testExistingSemanticsPreserved(@TempDir Path tempDir) throws Exception {
		var spec = parse(tempDir.resolve("v1"),
				"command=ping\nargs=-n 60 127.0.0.1\nenv=A=1,B=2\n");
		assertEquals(List.of("ping", "-n", "60", "127.0.0.1"), spec.command);
		assertEquals(Map.of("A", "1", "B", "2"), spec.env);

		// 无 args/env 的最小形态；键值等号两侧空白容忍（消费点 trim）
		var minimal = parse(tempDir.resolve("v2"), "command = ping \n");
		assertEquals(List.of("ping"), minimal.command);
		assertTrue(minimal.env.isEmpty());
	}

	/** 错误形态不变：文件缺失 FileNotFoundException；command 缺失/空白 IOException；
	 * env 条目非法 IOException。 */
	@Test
	public void testErrorShapesUnchanged(@TempDir Path tempDir) throws Exception {
		var missing = tempDir.resolve("none").toFile();
		Files.createDirectories(missing.toPath());
		assertThrows(FileNotFoundException.class, () -> ServiceManager.parseLaunchSpec(missing));

		assertThrows(IOException.class, () -> parse(tempDir.resolve("v1"), "args=x\n"));
		assertThrows(IOException.class, () -> parse(tempDir.resolve("v2"), "command=   \n"));
		assertThrows(IOException.class, () -> parse(tempDir.resolve("v3"), "command=cmd\nenv=BADENTRY\n"));
	}
}
