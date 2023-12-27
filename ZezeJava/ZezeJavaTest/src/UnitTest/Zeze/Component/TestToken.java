package UnitTest.Zeze.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Net.ServiceConf;
import Zeze.Services.Token;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TestToken {
	private static final Logger logger = LogManager.getLogger(TestToken.class);

	@Test
	public void testToken() throws Exception {
		Task.tryInitThreadPool();
		var tokenServer = new Token().start(null, null, 5003);
		try {
			var tokenClient = new Token.TokenClient(null).start("127.0.0.1", 5003);
			try {
				tokenClient.waitReady();

				var token = tokenClient.newToken(new Binary("abc"), 5000).get().getToken();
				logger.info("token: '{}'", token);
				Assert.assertEquals(24, token.length());

				var res = tokenClient.getToken(token, 1).get();
				Assert.assertEquals("abc", res.getContext().toString(StandardCharsets.UTF_8));
				Assert.assertEquals(1, res.getCount());
				Assert.assertTrue(res.getTime() >= 0);

				res = tokenClient.getToken(token, 1).get();
				Assert.assertEquals("", res.getContext().toString(StandardCharsets.UTF_8));
				Assert.assertEquals(0, res.getCount());
				Assert.assertTrue(res.getTime() < 0);
			} finally {
				tokenClient.stop();
			}
		} finally {
			tokenServer.stop();
			tokenServer.closeDb();
		}
	}

	@Test
	public void testTopic() throws Exception {
		Task.tryInitThreadPool();
		var tokenServer = new Token().start(null, null, 5003);
		try {
			var tokenClient = new Token.TokenClient(null).start("127.0.0.1", 5003);
			try {
				var f = new TaskCompletionSource<Boolean>();
				tokenClient.registerNotifyTopicHandler("testTopic", p -> {
					Assert.assertEquals("testTopic", p.Argument.getTopic());
					Assert.assertEquals("abc", new String(p.Argument.getContent().copyIf(), StandardCharsets.UTF_8));
					Assert.assertFalse(p.Argument.isBroadcast());
					f.setResult(true);
				});
				tokenClient.waitReady();

				tokenClient.subTopic("testTopic").get();
				tokenClient.pubTopic("testTopic", new Binary("abc"), false);
				tokenClient.unsubTopic("testTopic").get();
				Assert.assertTrue(f.get(5, TimeUnit.SECONDS));
			} finally {
				tokenClient.stop();
			}
		} finally {
			tokenServer.stop();
			tokenServer.closeDb();
		}
	}

	@Ignore
	@Test
	public void testKeepAlive() throws Exception {
		Task.tryInitThreadPool();
		var conf = new Config();
		var sconf = new ServiceConf();
		sconf.getHandshakeOptions().setKeepCheckPeriod(1);
		sconf.getHandshakeOptions().setKeepRecvTimeout(5);
		sconf.getHandshakeOptions().setKeepSendTimeout(2);
		conf.getServiceConfMap().put("TokenServer", sconf);
		sconf = new ServiceConf();
		sconf.getHandshakeOptions().setKeepCheckPeriod(1);
		sconf.getHandshakeOptions().setKeepRecvTimeout(5);
		sconf.getHandshakeOptions().setKeepSendTimeout(2);
		conf.getServiceConfMap().put("TokenClient", sconf);

		var tokenServer = new Token().start(conf, null, 5003);
		try {
			var tokenClient = new Token.TokenClient(conf).start("127.0.0.1", 5003);
			try {
				Thread.sleep(10_000);
				logger.info("sleep over");
			} finally {
				tokenClient.stop();
			}
		} finally {
			tokenServer.stop();
			tokenServer.closeDb();
		}
	}

	// Token压力测试
	// -Xmx1g -XX:SoftRefLRUPolicyMSPerMB=1000
	public static void main(String[] args) throws Exception {
		final int TEST_COUNT = 10_000_000; // 申请的token总数
		final int MAX_RPC_COUNT = Runtime.getRuntime().availableProcessors() * 2; // 并发RPC请求上限
		System.setProperty("perfPeriod", "10");
		System.setProperty("noDebugMode", "true");
		Task.tryInitThreadPool();
		var tokenServer = new Token().start(null, null, 5003);
		tokenServer.getService().getConfig().getSocketOptions().setOutputBufferMaxSize(10 << 20);
		try {
			var tokenClient = new Token.TokenClient(null).start("127.0.0.1", 5003);
			try {
				tokenClient.getConfig().getSocketOptions().setOutputBufferMaxSize(10 << 20);
				tokenClient.waitReady();
				System.out.println("INFO: test begin");
				var sem = new Semaphore(MAX_RPC_COUNT);
				for (int i = 0; i < TEST_COUNT; i++) {
					if (i % (TEST_COUNT / 100) == 0)
						System.out.println("INFO: " + i);
					sem.acquire();
					if (!tokenClient.newToken(new Binary(new byte[100]), 600_000, r -> {
						if (r.getResultCode() != 0) {
							System.out.println("ERROR: rpc1.resultCode=" + r.getResultCode());
							sem.release();
						} else {
							var token = r.Result.getToken();
							if (token.length() != 24) {
								System.out.println("ERROR: token.length=" + token.length());
								sem.release();
							} else {
								if (!tokenClient.getToken(token, 100, r2 -> {
									if (r2.getResultCode() != 0)
										System.out.println("ERROR: rpc2.resultCode=" + r2.getResultCode());
									else {
										var res = r2.Result;
										if (res.getContext().size() != 100)
											System.out.println("ERROR: context.size=" + res.getContext().size());
										if (res.getCount() <= 0)
											System.out.println("ERROR: res.count=" + res.getCount());
										if (res.getTime() < 0)
											System.out.println("ERROR: res.time=" + res.getTime());
									}
									sem.release();
									return 0;
								})) {
									System.out.println("ERROR: send rpc2 failed");
									sem.release();
								}
							}
						}
						return 0;
					})) {
						System.out.println("ERROR: send rpc1 failed");
						sem.release();
					}
				}
				System.out.println("INFO: send finish, wait left res");
				sem.acquire(MAX_RPC_COUNT);
				System.out.println("INFO: test OK!");
			} finally {
				tokenClient.stop();
			}
		} finally {
			tokenServer.stop();
			tokenServer.closeDb();
			System.out.println("INFO: test end!");
		}
	}
}
