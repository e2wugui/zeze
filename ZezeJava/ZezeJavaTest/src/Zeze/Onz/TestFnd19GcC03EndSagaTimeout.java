package Zeze.Onz;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import Zeze.Net.Binary;
import Zeze.Onz.AbstractOnz;
import Zeze.Onz.OnzServer;
import Zeze.Serialize.ByteBuffer;
import demo.App;
import demo.Module1.BKuafu;
import demo.Module1.BKuafuResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static Zeze.Onz.Fnd19GcOnzTestSupport.*;

/**
 * FND19 GC-C03 回归：endSaga 的 FuncSagaEnd 用 rpc 默认5s超时，与 cancelSaga
 * （显式flushTimeout）/redo（显式30s）对同一"慢参与方 businessLock"场景的既定策略
 * 不一致——end 应答超过5s（慢业务/慢flush在锁内串行）时await超时被吞，只剩噪声error
 * 日志。修复：SendForWait 补上 flushTimeout（对齐 cancelSaga 既有写法）。
 * 驱动方式：不走perform（perform会先等步骤结果，真实流的窗口只在结果发出后的
 * finalCommit/flush段），直接以注入的zezeSagas条目反射调用endSaga，令参与方业务
 * 睡8s持有businessLock——end应答必然慢于rpc默认5s、早于flushTimeout。
 */
public class TestFnd19GcC03EndSagaTimeout {
	// 过程名必须全JVM唯一：demo.App单例的Onz注册表跨测试类持久。
	private static final AtomicBoolean registeredOnAppInstance = new AtomicBoolean();
	private static final String SlowSagaName = "fnd19c3SagaEnd8s";

	private static final long AccountSlow8 = 305;

	private final App zeze2 = new App();
	private OnzServer onzServer;

	@BeforeEach
	public void before() throws Exception {
		var myConfig = startTwoClusters(zeze2);

		Infinite.App.clearDbTable(zeze2.demo_Module1.getKuafu());
		Infinite.App.clearDbTable(App.Instance.demo_Module1.getKuafu());

		if (registeredOnAppInstance.compareAndSet(false, true))
			App.Instance.Zeze.getOnz().registerSaga(SlowSagaName,
					TestFnd19GcC03EndSagaTimeout::sagaSlow8s, TestFnd19GcC03EndSagaTimeout::sagaCancel,
					BKuafu.class, BKuafuResult.class, Zeze.Transaction.EmptyBean.class);

		onzServer = startOnzServer(myConfig);
	}

	@AfterEach
	public void after() throws Exception {
		stopCoordinator(onzServer, zeze2);
	}

	/** 业务睡眠8s后写入：FuncSagaEnd的应答（与businessLock互斥串行）必然晚于rpc默认5s。 */
	private static long sagaSlow8s(Zeze.Onz.OnzSaga saga, BKuafu argument, BKuafuResult result) throws Exception {
		//noinspection BusyWait
		Thread.sleep(8_000);
		var app = (App)saga.getStub().getOnz().getZeze().getAppBase();
		var account = app.demo_Module1.getKuafu().getOrAdd(argument.getAccount());
		account.setMoney(account.getMoney() + argument.getMoney());
		result.setMoney(account.getMoney());
		return 0;
	}

	private static long sagaCancel(Zeze.Onz.OnzSaga saga, Zeze.Transaction.EmptyBean cancelArgument) {
		return 0;
	}

	@Test
	@Timeout(120)
	public void testEndSagaWaitsFlushTimeoutNotRpcDefault() throws Exception {
		waitOnzReady(onzServer);

		var txn = new NoStepTransaction();
		txn.setOnzServer(onzServer); // 分配真实onzTid
		txn.setFlushTimeout(20_000);
		// 注入saga参与方条目（endSaga只按key取socket发送；不经perform——驱动点即commit()内部的endSaga）
		Field sagasField = Zeze.Onz.OnzTransaction.class.getDeclaredField("zezeSagas");
		sagasField.setAccessible(true);
		@SuppressWarnings("unchecked")
		var sagas = (ConcurrentHashMap<String, Zeze.Util.TaskCompletionSource<?>>)sagasField.get(txn);
		sagas.put("zeze1", new Zeze.Util.TaskCompletionSource<>());

		startSlowSagaContext(txn.getOnzTid()); // 参与方业务8s持有businessLock，上下文滞留等FuncSagaEnd

		Method endSaga = Zeze.Onz.OnzTransaction.class.getDeclaredMethod("endSaga");
		endSaga.setAccessible(true);
		var t0 = System.nanoTime();
		endSaga.invoke(txn);
		var elapsedMs = (System.nanoTime() - t0) / 1_000_000;

		// 修复前：默认5s超时 → await在~5s抛RpcTimeoutException被吞，endSaga提前返回；
		// 修复后：等待沿用flushTimeout → 应答（业务8s结束后）到达才返回。
		Assertions.assertTrue(elapsedMs >= 6_500,
				"endSaga必须等满慢参与方的应答（~8s）而不是rpc默认5s超时放弃（实际 " + elapsedMs + " ms）");
		Assertions.assertTrue(elapsedMs < 18_000,
				"endSaga应在应答到达即返回，不得挂到flushTimeout量级之外（实际 " + elapsedMs + " ms）");

		waitUntil(() -> sagaCount(App.Instance.Zeze.getOnz()) == 0, 10_000, "FuncSagaEnd处理后参与方上下文必须清理");
		waitMoney(AccountSlow8, 5, 10_000, "慢业务必须正常提交（end与业务互斥串行，不影响提交）");
	}

	/** 空事务：不注册任何参与方，仅提供endSaga所需的onzServer/onzTid/zezeSagas载体。 */
	private static class NoStepTransaction extends Zeze.Onz.OnzTransaction<BKuafu.Data, BKuafuResult.Data> {
		@Override
		protected long perform() throws Exception {
			return 0;
		}
	}

	/** 手动以协调者身份向zeze1发起慢业务FuncSaga（不走perform）：上下文注册即返回，业务在途8s。 */
	private void startSlowSagaContext(long tid) throws Exception {
		var arg = new BKuafu.Data();
		arg.setAccount(AccountSlow8);
		arg.setMoney(5);
		var bb = ByteBuffer.Allocate();
		arg.encode(bb);
		var r = new Zeze.Builtin.Onz.FuncSaga();
		r.Argument.setOnzTid(tid);
		r.Argument.setFuncName(SlowSagaName);
		r.Argument.setFuncArgument(new Binary(bb.Bytes, 0, bb.WriteIndex));
		r.Argument.setFlushMode(AbstractOnz.eFlushImmediately);
		r.SendForWait(onzServer.getZezeInstance("zeze1")); // 不await：业务8s后才应答
		waitUntil(() -> sagaCount(App.Instance.Zeze.getOnz()) == 1, 30_000, "saga上下文未注册");
	}

	private static long getMoney(long account) throws Exception {
		var holder = new long[1];
		App.Instance.Zeze.newProcedure(() -> {
			holder[0] = App.Instance.demo_Module1.getKuafu().getOrAdd(account).getMoney();
			return 0L;
		}, "TestFnd19GcC03EndSagaTimeout.getMoney").call();
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
}
