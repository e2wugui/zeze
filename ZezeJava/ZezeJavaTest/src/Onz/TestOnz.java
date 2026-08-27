package Onz;

import Zeze.Config;
import Zeze.Onz.OnzProcedure;
import Zeze.Onz.OnzServer;
import demo.App;
import demo.Module1.BKuafu;
import demo.Module1.BKuafuResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import harness.TestEnv;

public class TestOnz {
	protected static final Logger logger = LogManager.getLogger(TestOnz.class);

	private final App zeze2 = new App();
	private OnzServer onzServer;

	@BeforeEach
	public void before() throws Exception {
		// 第二对服务 SM(5011)/Global(5012) 由 TestEnvLauncherListener 在进程内自动启动
		// （GCM 支持多实例）。仅当环境被显式关闭（-Dzeze.test.env=off）或端口被占时才会跳过。
		Assumptions.assumeTrue(TestEnv.portReachable("127.0.0.1", 5011) && TestEnv.portReachable("127.0.0.1", 5012),
				"第二对服务(5011/5012)不可用：zeze.test.env=off 时 TestEnvLauncherListener 不在进程内自动启动");

		App.Instance.Start();
		var config2 = Config.load("./zeze_cluster_2.xml");
		zeze2.Start(config2);

		Infinite.App.clearDbTable(zeze2.demo_Module1.getKuafu());
		Infinite.App.clearDbTable(App.Instance.demo_Module1.getKuafu());

		// 写到App启动里面，不用写两次，这里专门用于这个测试。
		App.Instance.Zeze.getOnz().register("kuafu", TestOnz::kuaFu, BKuafu.class, BKuafuResult.class);
		zeze2.Zeze.getOnz().register("kuafu", TestOnz::kuaFu, BKuafu.class, BKuafuResult.class);

		// 随便load一个，里面的OnzServer远程调用服务没有配置，里面讲不会初始化网络。
		// 现在这个测试嵌入方式使用OnzServer。
		var myConfig = Config.load("zeze.xml");
		onzServer = new OnzServer("zeze1=zeze.xml;zeze2=zeze_cluster_2.xml", myConfig);
		onzServer.start();
	}

	@AfterEach
	public void after() throws Exception {
		// before() 被 Assumption 跳过时 onzServer 尚未创建
		if (onzServer != null)
			onzServer.stop();
		zeze2.Stop();
	}

	private static long kuaFu(OnzProcedure onzProcedure, BKuafu argument, BKuafuResult result) {
		var app = (App)onzProcedure.getStub().getOnz().getZeze().getAppBase();
		var account = app.demo_Module1.getKuafu().getOrAdd(argument.getAccount());
		account.setMoney(account.getMoney() + argument.getMoney());
		result.setMoney(account.getMoney());
		return 0;
	}

	// 等待 OnzServer 通过 ServiceManager 订阅发现两侧集群的 Onz 服务并建连，替代盲等 sleep。
	// Onz 没有暴露独立的就绪状态：getZezeInstance 成功（订阅推送到达且 socket ready）即 perform 所需的就绪，
	// 连接器缓存与 perform 走同一条路径，无额外副作用。100ms 轮询，60s 兜底。
	private void waitOnzReady() throws InterruptedException {
		var deadline = System.currentTimeMillis() + 60_000;
		for (;;) {
			try {
				onzServer.getZezeInstance("zeze1");
				onzServer.getZezeInstance("zeze2");
				return;
			} catch (RuntimeException e) {
				if (System.currentTimeMillis() > deadline)
					throw e;
				Thread.sleep(100);
			}
		}
	}

	@Test
	public void testOnz() throws Exception {
		waitOnzReady();
		var txn = new KuafuTransaction(1, 1, 1);
		txn.setOnzServer(onzServer);
		Assertions.assertEquals(0, onzServer.perform(txn)); // 这里出现过断言失败，是rollback了，有异常日志，但很奇怪，不知道哪里调了rollback。
		logger.info("after perform m1={} m2={}", txn.m1, txn.m2);
		Assertions.assertEquals(0, txn.m1 + txn.m2);
	}
}
