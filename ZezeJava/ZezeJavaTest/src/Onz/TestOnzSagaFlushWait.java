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
import org.junit.jupiter.api.Timeout;

/**
 * FND3-52：纯 saga 事务（默认 eFlushImmediately）不得在 perform 尾部固定挂满
 * flushTimeout 再降级。参与方首次 flush 早于 FuncSagaEnd（setEnd），按设计不发
 * FlushReady，而 waitFlushDone 按 zezeSagas.size() 计数等待——计数永不满足，
 * 每笔 saga 事务固定阻塞 flushTimeout 并对每个参与方多一次串行 Checkpoint RPC。
 * 修复后协调者对 saga 不计数等待；本测试断言 perform 总耗时远小于 flushTimeout。
 */
public class TestOnzSagaFlushWait {
	protected static final Logger logger = LogManager.getLogger(TestOnzSagaFlushWait.class);

	private static final long ACCOUNT = 9001;

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

		Infinite.App.clearDbTable(App.Instance.demo_Module1.getKuafu());
		Infinite.App.clearDbTable(zeze2.demo_Module1.getKuafu());

		// saga写入kuafu表（产生脏记录），参与方事务走needFlushNow→Checkpoint.flush→sendFlushAndWait。
		App.Instance.Zeze.getOnz().registerSaga("sagaKuafu",
				TestOnzSagaFlushWait::sagaKuafu, TestOnzSagaFlushWait::sagaCancel,
				BKuafu.class, BKuafuResult.class, EmptyBean.class);
		zeze2.Zeze.getOnz().registerSaga("sagaKuafu",
				TestOnzSagaFlushWait::sagaKuafu, TestOnzSagaFlushWait::sagaCancel,
				BKuafu.class, BKuafuResult.class, EmptyBean.class);

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

	private static long sagaKuafu(OnzSaga saga, BKuafu argument, BKuafuResult result) {
		var app = (App)saga.getStub().getOnz().getZeze().getAppBase();
		var account = app.demo_Module1.getKuafu().getOrAdd(argument.getAccount());
		account.setMoney(account.getMoney() + argument.getMoney());
		result.setMoney(account.getMoney());
		return 0;
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

	@Test
	@Timeout(120)
	public void testSagaNotStallFlushTimeout() throws Exception {
		waitOnzReady();

		var txn = new SagaKuafuTransaction();
		txn.setOnzServer(onzServer);
		txn.setFlushTimeout(3_000); // 缩短：未修复时 perform 应挂满该值后降级

		var t0 = System.nanoTime();
		var rc = onzServer.perform(txn);
		var elapsedMs = (System.nanoTime() - t0) / 1_000_000;

		Assertions.assertEquals(0L, rc, "saga事务必须成功");
		Assertions.assertEquals(1L, (long)txn.getResult().getMoney(), "saga步骤结果必须正确");
		Assertions.assertTrue(elapsedMs < 2_500,
				"纯saga事务不得固定挂满flushTimeout再降级（实际 " + elapsedMs + " ms，flushTimeout=3000）");
		logger.info("saga perform elapsed {} ms", elapsedMs);
	}

	public static class SagaKuafuTransaction extends OnzTransaction<BKuafu.Data, BKuafuResult.Data> {
		@Override
		protected long perform() throws Exception {
			var a = new BKuafu.Data();
			a.setAccount(ACCOUNT);
			a.setMoney(1);
			var f = callSagaAsync("zeze1", "sagaKuafu", a, new BKuafuResult.Data());
			setResult(f.get());
			return 0;
		}
	}
}
