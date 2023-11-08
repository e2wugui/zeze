package UnitTest.Zeze.Component;

import java.nio.charset.StandardCharsets;
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
		}
	}
}
