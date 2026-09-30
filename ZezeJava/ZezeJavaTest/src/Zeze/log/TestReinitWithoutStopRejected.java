package Zeze.log;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import Zeze.Util.Task;

import harness.Fast;

/**
 * LogAgentManager.init 的重入守卫直测：init 第一行即覆盖静态引用 logAgentManager，
 * 未先 stop 的二次 init 按平台 bind 语义分岔——bind"成功"（SO_REUSEADDR/REUSEPORT
 * 同口双绑）时双 HttpServer 分流、第一实例组件全部失联泄漏；bind 失败时失败收尾
 * stop() 按静态字段回收，误关第一实例的 adminNetty、停第二实例的 agent，第一
 * LogAgent（SM 订阅与全部 connector）永久泄漏且静态引用已空无法再停。修复后
 * init 入口守卫：已初始化（logAgentManager 非 null ⇔ 成功即置位、失败收尾复位）
 * 即拒绝，重启语义由显式 stop 后再 init 承担。
 * 驱动以坏 XML 使守卫外的 init 确定性快速失败（不触网络/bind），静态注入经反射
 * （先例 TestSearchBrowseUnknownServer 的 installEmptyRegistryAgent）。
 *
 * <p>@Isolated：写 LogAgentManager JVM 级静态状态，独占运行。</p>
 */
@Fast
@Isolated
public class TestReinitWithoutStopRejected {

	/** 二次 init 必须入口即拒，且不得动第一实例的静态引用（覆盖即失联）。 */
	@Test
	public void testSecondInitWithoutStopIsRejected() throws Exception {
		Task.tryInitThreadPool();
		var malformedXml = writeMalformedXml();
		var first = new LogAgentManager();
		setStaticManager(first);
		try {
			var ex = assertThrows(IllegalStateException.class, () -> LogAgentManager.init(malformedXml.toString()),
					"未先 stop 的二次 init 必须入口即拒（否则静态引用被覆盖，第一实例回收错乱/泄漏）");
			assertTrue(ex.getMessage().contains("stop"),
					"报错必须指引先 stop 再重启: " + ex.getMessage());
			assertSame(first, LogAgentManager.getInstance(),
					"被拒的 init 不得覆盖第一实例的静态引用（覆盖即失联泄漏）");
		} finally {
			setStaticManager(null);
			Files.deleteIfExists(malformedXml);
		}
	}

	// ---------------------------------------------------------------- helpers

	/** 内容非法的 XML：守卫外的 init 在 Config.load 即确定性失败（不触网络）。 */
	private static Path writeMalformedXml() throws Exception {
		var path = Files.createTempFile("zeze-zoker-reinit", ".xml");
		Files.writeString(path, "<zeze-unclosed", StandardCharsets.UTF_8);
		return path;
	}

	private static void setStaticManager(LogAgentManager manager) throws Exception {
		Field field = LogAgentManager.class.getDeclaredField("logAgentManager");
		field.setAccessible(true);
		field.set(null, manager);
	}
}
