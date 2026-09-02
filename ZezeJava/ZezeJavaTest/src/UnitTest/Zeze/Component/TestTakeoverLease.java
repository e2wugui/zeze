package UnitTest.Zeze.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Application;
import Zeze.Component.Takeover;
import Zeze.Config;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Takeover租约簿记回归（步骤①）：claim抢占式epoch+1、reyn续约、release墓碑、
 * 重复claim再+1、配置解析。编程式Application（SM=disable，Memory库，Takeover不依赖SM）。
 */
@Fast
public class TestTakeoverLease {

	// 读租约行 {epoch, expireAt}，事务内取值拷贝出来。
	private static long[] readLease(Application app, int serverId) {
		var out = new long[2];
		var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
			var lease = app.getTakeover().getTable().get(serverId);
			if (lease != null) {
				out[0] = lease.getEpoch();
				out[1] = lease.getExpireAt();
			}
			return 0L;
		}, "TestTakeoverLease.readLease")).call();
		Assertions.assertEquals(0L, rc);
		return out;
	}

	@Test
	public void testClaimRenewRelease() throws Exception {
		// serverId必须经TakeoverTestEnv唯一分配：默认0会与demo.App（zeze.xml ServerId=0）
		// 撞 zeze_cache_0 的RocksDB锁（IDEA全模块单JVM运行时两者同进程）。
		var conf = TakeoverTestEnv.newConf("on", 600, 600_000); // ttl=600ms，renew周期200ms；不依赖周期扫描
		var app = new Application("TestTakeoverLease1", conf);
		try {
			app.start();
			var takeover = app.getTakeover();
			Assertions.assertNotNull(takeover);
			var serverId = conf.getServerId();

			// claim：epoch>=1，expireAt≈now+TTL。
			var lease = readLease(app, serverId);
			Assertions.assertTrue(lease[0] >= 1, "claim后epoch应>=1");
			Assertions.assertEquals(takeover.getMyEpoch(), lease[0]);
			Assertions.assertTrue(lease[1] > System.currentTimeMillis() && lease[1] <= System.currentTimeMillis() + 600 + 100,
					"expireAt≈now+TTL, lease=" + lease[1]);

			// renew：睡过两个renew周期，expireAt被推后，epoch不变。
			var expireBefore = lease[1];
			Thread.sleep(500);
			var renewed = readLease(app, serverId);
			Assertions.assertEquals(lease[0], renewed[0], "renew不得改epoch");
			Assertions.assertTrue(renewed[1] > expireBefore, "renew应推后expireAt: " + renewed[1] + " vs " + expireBefore);
			Assertions.assertTrue(renewed[1] > System.currentTimeMillis(), "续约后不得过期");

			// release：写墓碑 expireAt=0，epoch保留。
			takeover.release();
			var tomb = readLease(app, serverId);
			Assertions.assertEquals(lease[0], tomb[0]);
			Assertions.assertEquals(0L, tomb[1], "release后应为墓碑expireAt=0");

			// 重复claim：epoch再+1（抢占式，不等TTL）。
			var epoch2 = takeover.claim();
			Assertions.assertEquals(lease[0] + 1, epoch2);
			var again = readLease(app, serverId);
			Assertions.assertEquals(epoch2, again[0]);
			Assertions.assertTrue(again[1] > System.currentTimeMillis(), "重新claim后租约应未过期");
		} finally {
			app.stop();
		}
	}

	@Test
	public void testConfigParse() throws Exception {
		// Config.load的文件路径按cwd解析，这里直接构造DOM元素解析（parse公开）。
		var doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new org.xml.sax.InputSource(new java.io.StringReader(
						"<zeze TakeoverTtl=\"123456\" TakeoverScanPeriod=\"65432\" TakeoverMode=\"on\"/>")));
		var conf = new Config();
		conf.parse(doc.getDocumentElement());
		Assertions.assertEquals(123456L, conf.getTakeoverTtl());
		Assertions.assertEquals(65432L, conf.getTakeoverScanPeriod());
		Assertions.assertEquals("on", conf.getTakeoverMode());

		// 默认值（步骤②起默认on全量接管）。
		var confDefault = new Config();
		Assertions.assertEquals(600_000L, confDefault.getTakeoverTtl());
		Assertions.assertEquals(30_000L, confDefault.getTakeoverScanPeriod());
		Assertions.assertEquals("on", confDefault.getTakeoverMode());
	}
}
