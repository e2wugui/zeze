package Zeze.Services.ServiceManager;

import harness.Fast;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * verify数字口径（d70f0e534，收回FND4-63）：identity只开放int(serverId)，特意Integer.parseInt——
 * (Integer.MAX, Long.MAX]的数字identity（如雪花id）客户端直接拒绝。服务端isLegalServiceIdentity
 * 仍Long.parseLong，宽于客户端（客户端能过的服务端必过，不对称只在放行方向，无功能影响）。
 * FND4-64：SM Server 两版的原 static 块仅类加载即重置全 JVM root logger 级别（未设 logLevel
 * 属性时也强制 INFO）——测试/工具/同 JVM 引用只要碰到该类就静默破坏宿主日志配置。
 * 注：64 的红在独立 JVM（过滤运行）下可复现；全量套件中类可能已被其他测试提前加载。
 */
@Fast
public class TestSmAgentMisc {

	@Test
	public void testVerifyIntOnlyIdentity() {
		Assertions.assertThrows(NumberFormatException.class, () -> AbstractAgent.verify("9223372036854775807"),
				"Long.MAX identity必须拒绝（只开放int，d70f0e534）");
		Assertions.assertThrows(NumberFormatException.class, () -> AbstractAgent.verify("2147483648"),
				"Integer.MAX+1必须拒绝（只开放int）");
		Assertions.assertDoesNotThrow(() -> AbstractAgent.verify("123"));
		Assertions.assertDoesNotThrow(() -> AbstractAgent.verify("@named"));
		Assertions.assertThrows(NumberFormatException.class, () -> AbstractAgent.verify("abc"));
	}

	@Test
	public void testClassLoadDoesNotTouchRootLogger() throws Exception {
		var ctx = (LoggerContext)LogManager.getContext(false);
		var root = ctx.getConfiguration().getRootLogger();
		var saved = root.getLevel();
		try {
			root.setLevel(Level.WARN);
			Class.forName("Zeze.Services.ServiceManagerServer");
			Class.forName("Zeze.Services.ServiceManagerWithRaft");
			Assertions.assertEquals(Level.WARN, root.getLevel(), "仅类加载不得篡改root logger级别（FND4-64）");
		} finally {
			root.setLevel(saved);
		}
	}
}
