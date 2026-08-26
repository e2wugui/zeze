package UnitTest.Zeze.Component;

import java.util.concurrent.TimeUnit;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Net.ServiceConf;
import Zeze.Services.Token;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * keep-alive 回归（从 TestToken 拆出）：故意睡过 KeepRecvTimeout 验证保活探测维持空闲连接，
 * 属于慢测试，不标 @Fast，由 gradle integrationTest 执行。端口 5003 与 TestToken 共用（integrationTest 串行执行，不冲突）。
 */
public class TestTokenKeepAlive {
	private static final Logger logger = LogManager.getLogger(TestTokenKeepAlive.class);

	@Test
	public void testKeepAlive() throws Exception {
		Task.tryInitThreadPool();
		var conf = new Config();
		var sconf = new ServiceConf();
		sconf.getHandshakeOptions().setKeepCheckPeriod(1);
		sconf.getHandshakeOptions().setKeepRecvTimeout(2);
		sconf.getHandshakeOptions().setKeepSendTimeout(1);
		conf.getServiceConfMap().put("TokenServer", sconf);
		sconf = new ServiceConf();
		sconf.getHandshakeOptions().setKeepCheckPeriod(1);
		sconf.getHandshakeOptions().setKeepRecvTimeout(2);
		sconf.getHandshakeOptions().setKeepSendTimeout(1);
		conf.getServiceConfMap().put("TokenClient", sconf);

		var tokenServer = new Token().start(conf, null, 5003);
		try {
			var tokenClient = new Token.TokenClient(conf).start("127.0.0.1", 5003);
			try {
				var f = new TaskCompletionSource<Boolean>();
				tokenClient.registerNotifyTopicHandler("keepAliveTopic", p -> f.setResult(true));
				tokenClient.waitReady();
				// 睡过 KeepRecvTimeout(2s)+一个检查周期（整秒截断最迟 ~3.9s 观察到 3>2 判死）：
				// keep-alive 失效的话连接已被服务端掐断；正常探测让服务端 recvTime 恒 ≤1s，不会误杀
				Thread.sleep(4_500);
				logger.info("sleep over");
				tokenClient.subTopic("keepAliveTopic").get();
				tokenClient.pubTopic("keepAliveTopic", new Binary("alive"), false);
				Assertions.assertTrue(f.get(5, TimeUnit.SECONDS));
			} finally {
				tokenClient.stop();
			}
		} finally {
			tokenServer.stop();
			tokenServer.closeDb();
		}
	}
}
