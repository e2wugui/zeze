package Zeze.Onz;

import Zeze.Config;
import Zeze.Onz.OnzProcedure;
import Zeze.Onz.OnzServer;
import Zeze.Onz.OnzTransaction;
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
 * T4-F1 回归：Table 检查点模式下，只读/空记录集的 Onz 参与方（eFlushImmediately）
 * 原先永不发送 FlushReady——孤立 mergedSet 不被 Checkpoint.flush(RelativeRecordSet)
 * 下传握手、locked 为空路径连 addOnzProcedures 都没有——协调者 waitFlushDone 计数
 * 永不满足，每笔等满 flushTimeout 后降级（放行 ready + 全参与方 Checkpoint RPC）。
 * 修复：tryUpdateAndCheckpoint 在两个分支点对 onzProcedure 直接补发
 * sendFlushAndWait(Set.of(onzProcedure))，语义对齐 Immediately 模式对空记录集的无条件握手。
 * 测试：只读参与方（zeze1，走孤立 mergedSet 路径）+ 空访问参与方（zeze2，走 locked
 * 为空路径）同笔事务，缩短 flushTimeout，断言 perform 总耗时远小于 flushTimeout。
 */
public class TestOnzReadOnlyFlushReady {
	protected static final Logger logger = LogManager.getLogger(TestOnzReadOnlyFlushReady.class);

	private static final long ACCOUNT = 9101;

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

		// 只读：参与方事务只 get 不写（走孤立 mergedSet 路径，mergedSet.recordSet==null）。
		App.Instance.Zeze.getOnz().register("readOnlyKuafu",
				TestOnzReadOnlyFlushReady::readOnlyKuafu, BKuafu.class, BKuafuResult.class);
		// 空访问：参与方业务完全不触碰任何表格（走 locked 为空路径）。
		zeze2.Zeze.getOnz().register("noTouchKuafu",
				TestOnzReadOnlyFlushReady::noTouchKuafu, BKuafu.class, BKuafuResult.class);

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

	private static long readOnlyKuafu(OnzProcedure onzProcedure, BKuafu argument, BKuafuResult result) {
		var app = (App)onzProcedure.getStub().getOnz().getZeze().getAppBase();
		var account = app.demo_Module1.getKuafu().get(argument.getAccount()); // 只读
		result.setMoney(account != null ? account.getMoney() : 0);
		return 0;
	}

	private static long noTouchKuafu(OnzProcedure onzProcedure, BKuafu argument, BKuafuResult result) {
		result.setMoney(argument.getMoney()); // 不访问任何表格
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
	public void testReadOnlyParticipantNotStallFlushTimeout() throws Exception {
		waitOnzReady();

		var txn = new ReadOnlyTransaction();
		txn.setOnzServer(onzServer);
		txn.setFlushTimeout(3_000); // 缩短：未修复时 perform 应挂满该值后降级

		var t0 = System.nanoTime();
		var rc = onzServer.perform(txn);
		var elapsedMs = (System.nanoTime() - t0) / 1_000_000;

		Assertions.assertEquals(0L, rc, "只读/空访问Onz事务必须成功");
		Assertions.assertEquals(0L, txn.readOnlyMoney, "只读参与方（读不存在的key）结果必须为0");
		Assertions.assertEquals(7L, txn.noTouchMoney, "空访问参与方结果必须正确");
		Assertions.assertTrue(elapsedMs < 2_500,
				"只读/空记录集参与方必须及时发送FlushReady，不得挂满flushTimeout再降级"
						+ "（实际 " + elapsedMs + " ms，flushTimeout=3000）");
		logger.info("readonly/no-touch onz perform elapsed {} ms", elapsedMs);
	}

	public static class ReadOnlyTransaction extends OnzTransaction<BKuafu.Data, BKuafuResult.Data> {
		public long readOnlyMoney;
		public long noTouchMoney;

		@Override
		protected long perform() throws Exception {
			// 只读参与方读取【不存在】的key：记录被访问但无脏（allRead且孤立），
			// 走孤立 mergedSet 分支（mergedSet.recordSet==null）。
			var a1 = new BKuafu.Data();
			a1.setAccount(ACCOUNT);
			var f1 = callProcedureAsync("zeze1", "readOnlyKuafu", a1, new BKuafuResult.Data());

			// 空访问参与方完全不触碰表格：走 locked 为空分支。
			var a2 = new BKuafu.Data();
			a2.setAccount(ACCOUNT);
			a2.setMoney(7);
			var f2 = callProcedureAsync("zeze2", "noTouchKuafu", a2, new BKuafuResult.Data());

			readOnlyMoney = f1.get().getMoney();
			noTouchMoney = f2.get().getMoney();
			return 0;
		}
	}
}
