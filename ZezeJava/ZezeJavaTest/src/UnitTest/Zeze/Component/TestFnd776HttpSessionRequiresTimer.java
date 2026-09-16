package UnitTest.Zeze.Component;

import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Netty.HttpServer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-76回归：无ProviderApp的带库应用enableHttpSession后start必失败（Application只在
 * redirect!=null即ProviderApp形态下创建Timer；HttpSession.start的scheduleNamed在null
 * timer上NPE，被Procedure转成"enableHttpSessionExpiredTimer error=..."错误码，无指向）。
 * 修复后：enableHttpSession护栏检查zeze.getTimer()==null，抛带ProviderApp指引的
 * IllegalStateException；ProviderApp形态不受影响。
 */
@Fast
public class TestFnd776HttpSessionRequiresTimer {

	// 无ProviderApp的带库形态：timer恒null，enableHttpSession必须抛ISE且消息指向ProviderApp。
	@Test
	public void testEnableHttpSessionWithoutProviderAppThrowsWithGuidance() throws Exception {
		Task.tryInitThreadPool();
		var conf = TakeoverTestEnv.newConf("dryrun", 600_000, 600_000);
		var zeze = new Application("TestFnd776NoProvider", conf);
		try {
			zeze.initialize(new TakeoverTestEnv.TestAppBase(zeze)); // redirect==null：不创建timer
			Assertions.assertNull(zeze.getTimer(), "前提：无ProviderApp的带库应用timer为null");

			var httpServer = new HttpServer(zeze);
			var ex = Assertions.assertThrows(IllegalStateException.class, httpServer::enableHttpSession,
					"缺timer时enableHttpSession必须fail-fast，不得推迟到start时以无指向错误码失败");
			Assertions.assertNotNull(ex.getMessage());
			Assertions.assertTrue(ex.getMessage().contains("ProviderApp"),
					"报错必须指向缺失ProviderApp，got: " + ex.getMessage());
		} finally {
			zeze.stop();
		}
	}

	// 兼容红线：ProviderApp形态（redirect!=null）下timer存在，enableHttpSession照常成功。
	@Test
	public void testEnableHttpSessionWithProviderAppSucceeds() throws Exception {
		Task.tryInitThreadPool();
		var conf = TakeoverTestEnv.newConf("dryrun", 600_000, 600_000);
		var zeze = new Application("TestFnd776WithProvider", conf);
		try {
			new ProviderApp(zeze); // fake ProviderApp：建立zeze.redirect，供initialize创建Timer
			zeze.initialize(new TakeoverTestEnv.TestAppBase(zeze));
			Assertions.assertNotNull(zeze.getTimer(), "前提：ProviderApp形态timer已创建");

			var httpServer = new HttpServer(zeze);
			httpServer.enableHttpSession(); // 不得误伤
			Assertions.assertNotNull(httpServer.getHttpSession());
			httpServer.getHttpSession().stop(); // removeModule清理
		} finally {
			zeze.stop();
		}
	}
}
