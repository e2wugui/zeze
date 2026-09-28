package Zeze.Services.ZokerImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FND24 zoker-13 守卫：parseLaunchSpec 吸收 UTF-8 BOM（U+FEFF）。
 * 带 BOM 的首行 key="\uFEFFcommand"：indexOf('=')&gt;0 不被跳过、trim() 只剥 &lt;=U+0020 剥不掉
 * U+FEFF——switch 失配解析不到 command，误报 eNoServiceProperties（错误码语义"缺少部署描述"
 * 对真因 BOM 误导；Windows 记事本类工具常见输出）。修复=读入后剥开头 BOM 再进既有行式解析。
 * 纯文件直构（parseLaunchSpec 包内静态），全平台确定性；修复前红=IOException(missing command)。
 */
@Fast
public class TestE07LaunchSpecBom {

	private static ServiceManager.LaunchSpec parse(Path versionDir, String content) throws IOException {
		Files.createDirectories(versionDir);
		Files.writeString(versionDir.resolve(ServiceManager.SERVICE_PROPERTIES_NAME), content);
		return ServiceManager.parseLaunchSpec(versionDir.toFile());
	}

	/** 核心红点：BOM+CRLF（记事本保存形态）的 command/args/env 全部正常解析。 */
	@Test
	public void testBomHeadAbsorbed(@TempDir Path tempDir) throws Exception {
		var spec = parse(tempDir.resolve("v1"), "\uFEFFcommand=ping\r\nargs=-n 60 127.0.0.1\r\nenv=A=1\r\n");
		assertEquals(List.of("ping", "-n", "60", "127.0.0.1"), spec.command,
				"BOM 不得粘在首行 key 上使 command 失配");
		assertEquals(Map.of("A", "1"), spec.env);
	}

	/** BOM+无 CRLF（LF 形态）与 BOM 后紧跟空白行的形态同样闭合。 */
	@Test
	public void testBomWithLfAndBlankLine(@TempDir Path tempDir) throws Exception {
		var spec = parse(tempDir.resolve("v1"), "\uFEFFcommand=cmd\n\nargs=/c app.exe\n");
		assertEquals(List.of("cmd", "/c", "app.exe"), spec.command);
	}

	/** 无 BOM 既有语义不受影响（回归钉，同 TestE04LaunchSpecBackslash 的最小形态）。 */
	@Test
	public void testNoBomUnchanged(@TempDir Path tempDir) throws Exception {
		var spec = parse(tempDir.resolve("v1"), "command=ping\n");
		assertEquals(List.of("ping"), spec.command);
	}
}
