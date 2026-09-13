package UnitTest.Zeze.Component;

import java.util.concurrent.TimeUnit;
import java.nio.file.Path;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Net.ServiceConf;
import Zeze.Services.Token;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * keep-alive 回归（从 TestToken 拆出）：故意睡过 KeepRecvTimeout 验证保活探测维持空闲连接，
 * 属于慢测试，不标 @Fast，由 gradle integrationTest 执行。端口 5003 与 TestToken 共用（integrationTest 串行执行，不冲突）。
 */
@ResourceLock("token.rocksdb") // Token经全局System property定位DB目录，同族测试（含fast/integration两侧）串行
public class TestTokenKeepAlive {
	private static final Logger logger = LogManager.getLogger(TestTokenKeepAlive.class);

	@Test
	public void testKeepAlive(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		// Token 的 RocksDB 目录由系统属性 token.rocksdb 指定（默认cwd下的token_db），重定向到临时目录。
		System.setProperty("token.rocksdb", tempDir.resolve("token_db").toString());
		var conf = new Config();
		// keep-alive时序按3x放宽（第五轮round 47：1s/2s的裕度下，满负载一个>2s的处理停顿就令
		// 服务端按设计判死空闲连接，睡后subTopic即Send Fail误红。产品默认分钟级，2s本就不适合
		// 满负载套件；放宽后探测仍远密于超时，语义不变——睡过"无保活必被掐死"的窗口）。
		var sconf = new ServiceConf();
		sconf.getHandshakeOptions().setKeepCheckPeriod(2);
		sconf.getHandshakeOptions().setKeepRecvTimeout(6);
		sconf.getHandshakeOptions().setKeepSendTimeout(3);
		conf.getServiceConfMap().put("TokenServer", sconf);
		sconf = new ServiceConf();
		sconf.getHandshakeOptions().setKeepCheckPeriod(2);
		sconf.getHandshakeOptions().setKeepRecvTimeout(6);
		sconf.getHandshakeOptions().setKeepSendTimeout(3);
		conf.getServiceConfMap().put("TokenClient", sconf);

		var tokenServer = new Token().start(conf, null, 5003);
		try {
			var tokenClient = new Token.TokenClient(conf).start("127.0.0.1", 5003);
			try {
				var f = new TaskCompletionSource<Boolean>();
				tokenClient.registerNotifyTopicHandler("keepAliveTopic", p -> f.setResult(true));
				tokenClient.waitReady();
				// 睡过 KeepRecvTimeout(6s)+一个检查周期(2s)（最迟 ~8s 观察到 gap>6 判死）：
				// keep-alive 失效的话连接已被服务端掐断；正常探测让服务端 recvTime 恒 ≤3s，不会误杀
				Thread.sleep(9_000);
				logger.info("sleep over");
				tokenClient.subTopic("keepAliveTopic").get();
				tokenClient.pubTopic("keepAliveTopic", new Binary("alive"), false);
				Assertions.assertTrue(f.get(15, TimeUnit.SECONDS));
			} finally {
				tokenClient.stop();
			}
		} finally {
			tokenServer.stop();
			tokenServer.closeDb();
		}
	}
}
