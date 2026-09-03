package Onz;

import Zeze.Config;
import Zeze.Onz.OnzSaga;
import Zeze.Onz.OnzServer;
import Zeze.Onz.OnzTransaction;
import Zeze.Transaction.EmptyBean;
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

/**
 * FND-G1-6：saga 步骤失败后参与方 sagas 上下文必须被清理。
 * 协调者 cancelSaga 只对成功步骤发 FuncSagaEnd（失败步骤被跳过），
 * 正常结束路径 endSaga 也只在成功时到达——失败步骤只能由参与方在
 * ProcessFuncSagaRequest 中自行清理，否则条目永久滞留（无界泄漏）。
 * 控制组：成功步骤经 FuncSagaEnd(cancel) 正常清理；冒烟：cleanupTimeoutSagas 幂等。
 */
public class TestOnzSagaCleanup {
	protected static final Logger logger = LogManager.getLogger(TestOnzSagaCleanup.class);

	private final App zeze2 = new App();
	private OnzServer onzServer;

	@BeforeEach
	public void before() throws Exception {
		// 第二对服务 SM(5011)/Global(5012) 由 TestEnvLauncherListener 在进程内自动启动
		Assumptions.assumeTrue(harness.TestEnv.portReachable("127.0.0.1", 5011)
				&& harness.TestEnv.portReachable("127.0.0.1", 5012),
				"第二对服务(5011/5012)不可用：zeze.test.env=off 时 TestEnvLauncherListener 不在进程内自动启动");

		App.Instance.Start();
		var config2 = Config.load("./zeze_cluster_2.xml");
		zeze2.Start(config2);

		// zeze1: 成功步骤；zeze2: 失败步骤。cancel 参数当前未填充，用 EmptyBean。
		App.Instance.Zeze.getOnz().registerSaga("sagaOk",
				TestOnzSagaCleanup::sagaOk, TestOnzSagaCleanup::sagaCancel,
				BKuafu.class, BKuafuResult.class, EmptyBean.class);
		zeze2.Zeze.getOnz().registerSaga("sagaFail",
				TestOnzSagaCleanup::sagaFail, TestOnzSagaCleanup::sagaCancel,
				BKuafu.class, BKuafuResult.class, EmptyBean.class);

		var myConfig = Config.load("zeze.xml");
		onzServer = new OnzServer("zeze1=zeze.xml;zeze2=zeze_cluster_2.xml", myConfig);
		onzServer.start();
	}

	@AfterEach
	public void after() throws Exception {
		if (onzServer != null)
			onzServer.stop();
		zeze2.Stop();
	}

	private static long sagaOk(OnzSaga saga, BKuafu argument, BKuafuResult result) {
		result.setMoney(argument.getMoney());
		return 0;
	}

	private static long sagaFail(OnzSaga saga, BKuafu argument, BKuafuResult result) {
		return -1; // 业务失败：本地事务回滚，协调者 cancelSaga 会跳过此步骤
	}

	private static long sagaCancel(OnzSaga saga, EmptyBean cancelArgument) {
		return 0;
	}

	// 同 TestOnz.waitOnzReady：等 OnzServer 订阅发现两侧集群并建连。
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

	// sagas 是 Zeze.Onz.Onz 的 private 字段，测试包不同，反射读取条目数。
	private static int sagaCount(Zeze.Onz.Onz onz) throws Exception {
		var field = Zeze.Onz.Onz.class.getDeclaredField("sagas");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		var map = (Zeze.Util.LongConcurrentHashMap<Object>)field.get(onz);
		return map.size();
	}

	@Test
	public void testFailSagaCleanup() throws Exception {
		waitOnzReady();

		var txn = new FailSagaTransaction();
		txn.setOnzServer(onzServer);
		var rc = onzServer.perform(txn);
		Assertions.assertNotEquals(0L, rc, "失败saga事务的perform必须返回非0");

		// 核心断言：失败步骤（zeze2）的上下文在 ProcessFuncSagaRequest 内自行清理。
		// 修复前：协调者 cancelSaga 跳过失败步骤，条目永久滞留。
		Assertions.assertEquals(0, sagaCount(zeze2.Zeze.getOnz()), "失败步骤的sagas上下文必须清理");

		// 控制组：成功步骤（zeze1）经 FuncSagaEnd(cancel) 正常清理。
		Assertions.assertEquals(0, sagaCount(App.Instance.Zeze.getOnz()), "成功步骤收到cancel后必须清理");

		// 冒烟：超时清理入口对空表幂等（正常路径已清空，这里只验证可调用、无异常）。
		App.Instance.Zeze.getOnz().setSagaContextTimeoutMs(1);
		zeze2.Zeze.getOnz().setSagaContextTimeoutMs(1);
		App.Instance.Zeze.getOnz().cleanupTimeoutSagas();
		zeze2.Zeze.getOnz().cleanupTimeoutSagas();
		Assertions.assertEquals(0, sagaCount(App.Instance.Zeze.getOnz()));
		Assertions.assertEquals(0, sagaCount(zeze2.Zeze.getOnz()));
	}

	/**
	 * zeze1 成功 + zeze2 失败的 saga 事务：perform 中 await 失败步骤的结果抛异常，
	 * OnzServer.perform 走 rollback -> cancelSaga（只对成功步骤发 FuncSagaEnd(cancel)）。
	 */
	public static class FailSagaTransaction extends OnzTransaction<BKuafu.Data, BKuafuResult.Data> {
		@Override
		protected long perform() throws Exception {
			var a1 = new BKuafu.Data();
			a1.setAccount(1);
			a1.setMoney(1);
			var f1 = callSagaAsync("zeze1", "sagaOk", a1, new BKuafuResult.Data());

			var a2 = new BKuafu.Data();
			a2.setAccount(2);
			a2.setMoney(-1);
			var f2 = callSagaAsync("zeze2", "sagaFail", a2, new BKuafuResult.Data());

			f1.get();
			f2.get(); // 失败：抛 RuntimeException -> perform 异常 -> rollback
			return 0;
		}
	}
}
