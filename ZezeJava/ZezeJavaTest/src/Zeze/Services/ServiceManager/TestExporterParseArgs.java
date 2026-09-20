package Zeze.Services.ServiceManager;

import java.util.ArrayList;
import java.util.Properties;

import Zeze.Util.KV;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * SM1-F4 回归：命令行参数边界——-e/-s为末尾token时args[++i]抛AIOOBE、-private为末尾
 * token时i+=2越界、-private后紧跟顶层开关时把开关吞为参数值，三处均背离同分支
 * "Usage+IllegalArgumentException"的既有错误形态。
 * 修复：越界/吞开关检查后统一走Usage抛IllegalArgumentException（parseArgs抽出为
 * 可测的静态方法）。
 */
@Fast
public class TestExporterParseArgs {
	private static IllegalArgumentException assertUsageRejected(String... args) {
		var shared = new Properties();
		return Assertions.assertThrows(IllegalArgumentException.class,
				() -> Exporter.parseArgs(args, shared, new ArrayList<>(), new ArrayList<>()),
				"args=" + java.util.Arrays.toString(args));
	}

	@Test
	public void testTrailingOptionTokensRejected() {
		assertUsageRejected("-e"); // -e为末尾token：原AIOOBE
		assertUsageRejected("-e", "Print", "-private"); // -private为末尾token：原peek通过后越界
		assertUsageRejected("-s"); // -s为末尾token：原AIOOBE（裁决要求顺带覆盖）
	}

	@Test
	public void testPrivateSwallowingSwitchRejected() {
		assertUsageRejected("-e", "Print", "-private", "-s", "Bar"); // 原把"-s"吞为private参数值
		assertUsageRejected("-e", "Print", "-private", "-d");
		assertUsageRejected("-e", "Print", "-private", "-e", "Print2");
	}

	@Test
	public void testValidParsingUnchanged() {
		var shared = new Properties();
		var services = new ArrayList<String>();
		var exporters = new ArrayList<KV<String, String>>();
		Exporter.parseArgs(new String[]{"-version", "2", "-e", "Print", "-private", "-file x.cfg",
				"-s", "svc1", "-s", "svc2", "-d"}, shared, services, exporters);
		Assertions.assertEquals("2", shared.getProperty("-version"));
		Assertions.assertEquals(2, services.size());
		Assertions.assertEquals("svc1", services.get(0));
		Assertions.assertEquals("svc2", services.get(1));
		Assertions.assertEquals(2, exporters.size());
		Assertions.assertEquals("Print", exporters.get(0).getKey());
		Assertions.assertEquals("-file x.cfg", exporters.get(0).getValue(), "私有选项串以单token携带的合法形态必须保留");
		Assertions.assertEquals("Print", exporters.get(1).getKey());
		Assertions.assertNull(exporters.get(1).getValue());
	}
}
