package Onz;

import Zeze.Config;
import Zeze.Onz.OnzProcedure;
import Zeze.Onz.OnzServer;
import demo.App;
import demo.Module1.BKuafu;
import demo.Module1.BKuafuResult;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Test1 {
	private App zeze2 = new App();
	private OnzServer onzServer;

	@Before
	public void before() throws Exception {
		App.Instance.Start();
		var config2 = Config.load("./zeze_cluster_2.xml");
		zeze2.Start(config2);

		// 写到App启动里面，不用写两次，这里专门用于这个测试。
		App.Instance.Zeze.getOnz().register(App.Instance.Zeze, "kuafu", Test1::kuaFu, BKuafu.class, BKuafuResult.class);
		zeze2.Zeze.getOnz().register(zeze2.Zeze, "kuafu", Test1::kuaFu, BKuafu.class, BKuafuResult.class);

		// 随便load一个，里面的OnzServer远程调用服务没有配置，里面讲不会初始化网络。
		// 现在这个测试嵌入方式使用OnzServer。
		var myConfig = Config.load("zeze.xml");
		onzServer = new OnzServer("zeze1=zeze.xml;zeze2=zeze_cluster_2.xml", myConfig);
		onzServer.start();
	}

	@After
	public void after() throws Exception {
		onzServer.stop();
		zeze2.Stop();
	}

	private static long kuaFu(OnzProcedure onzProcedure, BKuafu argument, BKuafuResult result) {
		var app = (App)onzProcedure.getStub().getZeze().getAppBase();
		var account = app.demo_Module1.getKuafu().getOrAdd(argument.getAccount());
		account.setMoney(account.getMoney() + argument.getMoney());
		result.setMoney(account.getMoney());
		return 0;
	}

	@Test
	public void test1() throws Exception {
		Thread.sleep(2000);
		var txn = new KuafuTransaction(1, 1, 1);
		txn.setOnzServer(onzServer);
		Assert.assertEquals(0, onzServer.perform(txn));
		System.out.println("m1=" + txn.m1 + " m2=" + txn.m2);
		Assert.assertEquals(0, txn.m1 + txn.m2);
	}
}
