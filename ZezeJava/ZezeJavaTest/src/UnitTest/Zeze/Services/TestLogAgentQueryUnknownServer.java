package UnitTest.Zeze.Services;

import Zeze.Config;
import Zeze.Services.LogAgent;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * S3-F4 回归：query对未知/已注销的日志服务器名，__getLogServer（ConcurrentHashMap.get）
 * 返回null后裸解引用GetReadySocket()——NPE无任何信息。
 * 修复：判空抛带名字的IllegalArgumentException。
 */
@Fast
public class TestLogAgentQueryUnknownServer {
	@Test
	public void testUnknownServerThrowsIllegalArgumentWithName() throws Exception {
		var conf = new Config().loadAndParse();
		conf.setServiceManager("disable"); // 不依赖部署配置的ServiceManager（无SM时createServiceManager返回null）
		var agent = new LogAgent(conf);
		var ex = Assertions.assertThrows(IllegalArgumentException.class,
				() -> agent.query("no-such-log-server", "{}"));
		Assertions.assertTrue(ex.getMessage().contains("no-such-log-server"),
				"异常信息必须携带服务器名: " + ex.getMessage());
	}
}
