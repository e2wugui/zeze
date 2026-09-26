package TestLog4jQuery;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.handler.ClassUtils;
import Zeze.Services.Log4jQuery.handler.QueryHandlerManager;

import harness.Fast;

/**
 * GD-C05回归：ClassUtils用未解码的url.getPath()构造File，classpath含空格/非ASCII时
 * 扫描静默为空、Query处理器全部不注册。修复为new File(url.toURI())。
 * 空格路径场景需在带空格目录下启动JVM，本类锁定的可测部分：常规classpath下
 * file协议扫描与QueryHandlerManager注册仍完整（换toURI后的行为基线）。
 */
@Fast
public class TestFnd19ClassUtilsScan {
	@Test
	public void testScanFindsImplHandlers() {
		var names = ClassUtils.getClassNames("Zeze.Services.Log4jQuery.handler.impl", true);
		assertTrue(names.contains("Zeze.Services.Log4jQuery.handler.impl.SelectCmdParamHandler"),
				"file协议扫描应找到impl包下的处理器类");
		assertTrue(names.size() >= 5, "impl包至少5个handler");

		// 类初始化触发QueryHandlerManager.init：命令应已注册可查。
		assertNotNull(QueryHandlerManager.getQueryHandleContainer("cmd_param"));
		assertNotNull(QueryHandlerManager.getQueryHandleContainer("cmd_list"));
	}
}
