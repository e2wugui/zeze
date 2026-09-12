package Zeze.Services.ServiceManager;

import harness.Fast;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND4-63：verify 与服务端 isLegalServiceIdentity/BServiceInfos 排序器的数字口径必须一致（Long）——
 * 原 Integer.parseInt 把 (Integer.MAX, Long.MAX] 的数字 identity（如雪花id）在客户端误拒。
 * FND4-64：SM Server 两版的原 static 块仅类加载即重置全 JVM root logger 级别（未设 logLevel
 * 属性时也强制 INFO）——测试/工具/同 JVM 引用只要碰到该类就静默破坏宿主日志配置。
 * 注：64 的红在独立 JVM（过滤运行）下可复现；全量套件中类可能已被其他测试提前加载。
 */
@Fast
public class TestSmAgentMisc {

	@Test
	public void testVerifyLongIdentity() {
		Assertions.assertDoesNotThrow(() -> AbstractAgent.verify("9223372036854775807"), "Long.MAX identity必须通过");
		Assertions.assertDoesNotThrow(() -> AbstractAgent.verify("2147483648"), "Integer.MAX+1必须通过（原误拒）");
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
