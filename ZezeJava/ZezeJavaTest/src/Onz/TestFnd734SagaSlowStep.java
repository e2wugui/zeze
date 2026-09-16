package Onz;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

import Zeze.Config;
import Zeze.Onz.OnzSaga;
import Zeze.Onz.OnzServer;
import Zeze.Onz.OnzTransaction;
import Zeze.Transaction.EmptyBean;
import demo.App;
import demo.Module1.BKuafu;
import demo.Module1.BKuafuResult;
import harness.TestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * FND7-34 回归（集成）：saga 步骤 rpc 超时硬编码 5s 且 cancelSaga 只补偿 future 成功的步骤。
 * saga 参与方"发结果即本地提交"（OnzSaga.sendReadyAndWait），步骤业务+网络往返超过 rpc 超时
 * 时：协调者按失败回滚并跳过该步骤的补偿，参与方却在超时后完成提交——部分提交的静默分歧。
 *
 * 场景A（超时可配，方案A）：业务 6s，flushTimeout=20s。修复前 rpc 固定 5s 超时 -> perform
 * 失败；修复后 rpc 复用 flushTimeout -> perform 成功且参与方写入落库。
 *
 * 场景B（超时步骤补偿，方案B）：业务 6s，flushTimeout=1.5s。修复前超时步骤被 cancelSaga
 * 跳过 -> perform 报失败但参与方写入残留（分歧）；修复后超时步骤也收到 FuncSagaEnd(cancel)，
 * 参与方侧与业务互斥（businessLock）串行在业务完成后补偿 -> 金额回到 0 且补偿计数 +1。
 * （金额回到0不足以证明补偿执行过——业务可能没跑；补偿计数行才是指证，而金额==0 排除
 * "只补偿没执行业务"的-22形态。）
 */
public class TestFnd734SagaSlowStep {
	// 过程名全 JVM 唯一：demo.App 单例的 Onz 注册表跨测试类持久（模式同 TestOnzReadyWaitTimeout）。
	private static final AtomicBoolean registeredOnAppInstance = new AtomicBoolean();
	private static final String ProcSlow6 = "fnd734SagaSlow6";

	// 场景A/B 各用独立行键，互不干扰；CancelCountRow 是补偿执行计数行。
	private static final long AccountSlow6 = 300;
	private static final long AccountTimeout = 301;
	private static final long AccountCancelCount = 302;

	private final App zeze2 = new App();
	private OnzServer onzServer;

	@BeforeEach
	public void before() throws Exception {
		// 第二对服务 SM(5011)/Global(5012) 由 TestEnvLauncherListener 在进程内自动启动。
		Assumptions.assumeTrue(TestEnv.portReachable("127.0.0.1", 5011) && TestEnv.portReachable("127.0.0.1", 5012),
				"第二对服务(5011/5012)不可用：zeze.test.env=off 时 TestEnvLauncherListener 不在进程内自动启动");

		var myConfig = Config.load("zeze.xml");
		var dbHome = "CommitOnzServer" + myConfig.getServerId();
		deleteRecursively(Path.of(dbHome));

		App.Instance.Start();
		var config2 = Config.load("./zeze_cluster_2.xml");
		zeze2.Start(config2);

		Infinite.App.clearDbTable(zeze2.demo_Module1.getKuafu());
		Infinite.App.clearDbTable(App.Instance.demo_Module1.getKuafu());

		if (registeredOnAppInstance.compareAndSet(false, true))
			App.Instance.Zeze.getOnz().registerSaga(ProcSlow6,
					TestFnd734SagaSlowStep::sagaSlow, TestFnd734SagaSlowStep::sagaSlowCancel,
					BKuafu.class, BKuafuResult.class, EmptyBean.class);

		onzServer = new OnzServer("zeze1=zeze.xml;zeze2=zeze_cluster_2.xml", myConfig);
		onzServer.start();
	}

	@AfterEach
	public void after() throws Exception {
		// before() 被 Assumption 跳过时 onzServer 尚未创建；stop幂等
		if (onzServer != null)
			onzServer.stop();
		zeze2.Stop();
	}

	/** 业务睡眠 6s 后写入金额：超过修复前的固定 5s rpc 超时，小于场景A的 flushTimeout=20s。 */
	private static long sagaSlow(OnzSaga saga, BKuafu argument, BKuafuResult result) throws Exception {
		//noinspection BusyWait
		Thread.sleep(6_000);
		var app = (App)saga.getStub().getOnz().getZeze().getAppBase();
		var account = app.demo_Module1.getKuafu().getOrAdd(argument.getAccount());
		account.setMoney(account.getMoney() + argument.getMoney());
		result.setMoney(account.getMoney());
		return 0;
	}

	/** 补偿：按步骤自己的参数扣回写入的金额，并留下补偿计数（getArgument返回原始类型Bean，需cast）。 */
	private static long sagaSlowCancel(OnzSaga saga, EmptyBean cancelArgument) {
		var app = (App)saga.getStub().getOnz().getZeze().getAppBase();
		var argument = (BKuafu)saga.getArgument();
		var account = app.demo_Module1.getKuafu().getOrAdd(argument.getAccount());
		account.setMoney(account.getMoney() - argument.getMoney());
		var counter = app.demo_Module1.getKuafu().getOrAdd(AccountCancelCount);
		counter.setMoney(counter.getMoney() + 1);
		return 0;
	}

	@Test
	@Timeout(180)
	public void testSlowSagaStep() throws Exception {
		waitOnzReady();

		// 场景A：业务6s < flushTimeout(20s)。修复前：rpc固定5s超时 -> perform失败。
		var slow = new OneStepTransaction(AccountSlow6, 11);
		slow.setOnzServer(onzServer);
		slow.setFlushTimeout(20_000);
		Assertions.assertEquals(0, onzServer.perform(slow), "rpc超时复用flushTimeout后慢步骤必须整体成功（FND7-34方案A）");
		Assertions.assertEquals(11, getMoney(AccountSlow6), "成功 saga 步骤的写入必须落库");
		Assertions.assertEquals(11, slow.resultMoney, "结果必须正常返回");
		Assertions.assertEquals(0, getMoney(AccountCancelCount), "成功路径不触发补偿");

		// 场景B：业务6s > flushTimeout(1.5s) -> rpc超时 -> perform失败。
		// 修复前：超时步骤被cancelSaga跳过 -> 参与方6s后提交成功，金额22残留（静默分歧）；
		// 修复后：超时步骤也收到FuncSagaEnd(cancel)，参与方侧businessLock保证补偿串行在
		// 业务完成之后：金额+22后-22回到0，补偿计数=1。
		var timeout = new OneStepTransaction(AccountTimeout, 22);
		timeout.setOnzServer(onzServer);
		timeout.setFlushTimeout(1_500);
		Assertions.assertNotEquals(0L, onzServer.perform(timeout), "超过flushTimeout的步骤必须失败");
		waitMoney(AccountTimeout, 0, 30_000, "超时但实际已提交的步骤必须被补偿回0（FND7-34方案B）");
		waitMoney(AccountCancelCount, 1, 30_000, "补偿函数必须真正执行过（FND7-34方案B）");
	}

	private static class OneStepTransaction extends OnzTransaction<BKuafu.Data, BKuafuResult.Data> {
		private final long account;
		private final long money;
		long resultMoney;

		OneStepTransaction(long account, long money) {
			this.account = account;
			this.money = money;
		}

		@Override
		protected long perform() throws Exception {
			var argument = new BKuafu.Data();
			argument.setAccount(account);
			argument.setMoney(money);
			var future = callSagaAsync("zeze1", ProcSlow6, argument, new BKuafuResult.Data());
			resultMoney = future.get().getMoney();
			return 0;
		}
	}

	private static long getMoney(long account) throws Exception {
		var holder = new long[1];
		App.Instance.Zeze.newProcedure(() -> {
			holder[0] = App.Instance.demo_Module1.getKuafu().getOrAdd(account).getMoney();
			return 0L;
		}, "TestFnd734SagaSlowStep.getMoney").call();
		return holder[0];
	}

	private static void waitMoney(long account, long expected, long timeoutMs, String message) throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		long actual = getMoney(account);
		while (actual != expected) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, message
					+ "（账户=" + account + " 期望=" + expected + " 实际=" + actual + "）");
			//noinspection BusyWait
			Thread.sleep(50);
			actual = getMoney(account);
		}
	}

	// 同 TestOnz.waitOnzReady：等订阅发现两侧集群并建连（getZezeInstance成功即perform就绪）。
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

	private static void deleteRecursively(Path root) throws Exception {
		if (!Files.exists(root))
			return;
		try (var walk = Files.walk(root)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}
}
