package UnitTest.Zeze.Trans;

import Zeze.Config;
import Zeze.Util.OutInt;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("CallToPrintStackTrace")
public class TestConcurrentStartServer {
	@Test
	public void testConcurrentStartServer() throws Exception {
		//var config = Config.load("zeze.xml");
		//config.dropMysqlOperatesProcedures();
		for (var i = 0; i < 3; ++i)
			start2();
	}

	private static void start2() throws Exception {
		// 【注意】这个测试停止的非常快，会导致启动过程其他线程任务执行失败，不用管。
		demo.App app1 = new demo.App();
		demo.App app2 = new demo.App();
		demo.App app3 = new demo.App();

		var start1 = new OutInt(0);
		var t1 = new Thread(() -> {
			try {
				var config1 = Config.load("zeze.xml");
				config1.setServerId(20);
				config1.getServiceConfMap().remove("TestServer");
				config1.getServiceConfMap().remove("Zeze.Onz.Server");
				app1.Start(config1);
				start1.value = 1;
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
		var start2 = new OutInt(0);
		var t2 = new Thread(() -> {
			try {
				var config2 = Config.load("zeze.xml");
				config2.setServerId(21);
				config2.getServiceConfMap().remove("TestServer");
				config2.getServiceConfMap().remove("Zeze.Onz.Server");
				app2.Start(config2);
				start2.value = 1;
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
		var start3 = new OutInt(0);
		var t3 = new Thread(() -> {
			try {
				var config3 = Config.load("zeze.xml");
				config3.setServerId(22);
				config3.getServiceConfMap().remove("TestServer");
				config3.getServiceConfMap().remove("Zeze.Onz.Server");
				app3.Start(config3);
				start3.value = 1;
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
		try {
			t1.start();
			t2.start();
			t3.start();
			t1.join();
			t2.join();
			t3.join();
			Assert.assertEquals(1, start1.value);
			Assert.assertEquals(1, start2.value);
			Assert.assertEquals(1, start3.value);
		} finally {
			app1.Stop();
			app2.Stop();
			app3.Stop();
		}
	}
}
