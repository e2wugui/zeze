package Zeze.log;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.xml.sax.SAXParseException;
import Zeze.Netty.HttpServer;
import Zeze.Util.Task;

import harness.Fast;

/**
 * init 失败收尾的异常保真直测：init 的 catch 裸调 stop() 后 throw e——stop 声明
 * throws Exception 且三步回收（httpServer.close/adminNetty.close/logAgent.stop）
 * 均可抛，一旦回收自身抛异常，throw e 不执行，原始启动失败原因被无关的回收异常
 * 替换（连 suppressed 都没有）——双故障场景（启动失败+回收失败）正是最需要根因
 * 的时刻。同模块 MainZokerManager.start 对同一问题已有正确先例（stop 包 try/catch
 * addSuppressed）。修复后回收异常以 suppressed 附着，原始异常原样上抛。
 * 注入形态：坏 XML 使 init 在 Config.load 即确定性失败（回收点只到 httpServer），
 * 经公开静态 httpServer 换入 close 即抛的桩制造回收失败。
 *
 * <p>@Isolated：写 LogAgentManager JVM 级静态状态，独占运行。</p>
 */
@Fast
@Isolated
@Extra
public class TestInitFailureNotMaskedByCleanupException {

	/** 回收（stop）自身抛异常时，原始启动异常必须保留，回收异常只能 suppressed 附着。 */
	@Test
	public void testStopFailureDoesNotReplaceOriginalStartupException() throws Exception {
		Task.tryInitThreadPool();
		var malformedXml = writeMalformedXml();
		LogAgentManager.httpServer = new CloseThrowingHttpServer();
		try {
			var ex = assertThrows(SAXParseException.class, () -> LogAgentManager.init(malformedXml.toString()),
					"原始启动异常（XML 解析失败）必须上抛——回收失败不得替换根因");
			assertTrue(Arrays.stream(ex.getSuppressed()).anyMatch(s -> "close-boom".equals(s.getMessage())),
					"回收自身的异常必须以 suppressed 附着（对齐 MainZokerManager.start 的收尾纪律）: "
							+ Arrays.toString(ex.getSuppressed()));
		} finally {
			// stop() 在 httpServer.close() 抛出后即中断：httpServer 与 logAgentManager
			// 均残留（赋 null 未达），显式复位防污染同 JVM 后续用例。
			LogAgentManager.httpServer = null;
			setStaticManager(null);
			Files.deleteIfExists(malformedXml);
		}
	}

	// ---------------------------------------------------------------- helpers

	/** 内容非法的 XML：init 在 Config.load 即确定性失败（先于任何组件启动）。 */
	private static Path writeMalformedXml() throws Exception {
		var path = Files.createTempFile("zeze-zoker-mask", ".xml");
		Files.writeString(path, "<zeze-unclosed", StandardCharsets.UTF_8);
		return path;
	}

	private static void setStaticManager(LogAgentManager manager) throws Exception {
		Field field = LogAgentManager.class.getDeclaredField("logAgentManager");
		field.setAccessible(true);
		field.set(null, manager);
	}

	/** close 即抛的 HttpServer 桩：制造"回收自身失败"（stop 的第一步）。 */
	private static final class CloseThrowingHttpServer extends HttpServer {
		@Override
		public void close() {
			throw new RuntimeException("close-boom");
		}
	}
}
