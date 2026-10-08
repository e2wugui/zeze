package Zeze.Onz;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import Zeze.Onz.AbstractOnz;
import Zeze.Onz.OnzServer;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.TaskCompletionSource;
import demo.App;
import demo.Module1.BKuafu;
import demo.Module1.BKuafuResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static Zeze.Onz.GcOnzE2eTestSupport.*;

/**
 * onz-02 回归：retryCancelNotFoundOnce 单次重试仍 eSagaNotFound 即放弃并返回 true——
 * 放弃采信了不可证前提"请求确实未到达"：FuncSaga 与 FuncSagaEnd 同为 Normal 派发共享
 * 线程池（不保证同连接处理顺序，Onz 注释自证；onz-07 证明检查点可占用派发 worker 数秒
 * 以上），参与方派发停滞超 2×flushTimeout 且同池重排时，滞留未处理的 FuncSaga 使两次
 * FuncSagaEnd 都命中 NotFound，协调者按"补偿已了结"删除决策记录；随后队列恢复、
 * FuncSaga 执行并"发结果即本地提交"，其补偿永久失去。修复：give-up 不再视为了结
 * （返回 false 保留决策记录），删除推迟到 redo 轮的重发确认——滞留 FuncSaga 在
 * redo 补发窗口内执行注册后，cancel 命中在场上下文完成补偿并收敛删除记录。
 */
public class TestOnzSagaStalledStepKeepsCompensation {
	// 过程名必须全 JVM 唯一：demo.App 单例的 Onz 注册表跨测试类持久。
	private static final AtomicBoolean registeredOnAppInstance = new AtomicBoolean();
	private static final String SagaName = "onzStalledStepComp";

	// 手动rpc伪造的tid（避开OnzServer.nextOnzTid的分配空间；与其他测试类的0x...段错开）
	private static final long StalledTid = 0x5CA2E5A1000005E1L;
	private static final long Account = 413;

	// 补偿计数（cancel stub在参与方worker执行；@BeforeEach重置，volatile供跨线程轮询）。
	static volatile int CancelCount;

	private final App zeze2 = new App();
	private OnzServer onzServer;

	@BeforeEach
	public void before() throws Exception {
		CancelCount = 0;
		var myConfig = startTwoClusters(zeze2);

		// fixture隔离（对齐TestOnzSagaCompensateInflightNotFound先例）：收割前序类残留并恢复TTL默认。
		App.Instance.Zeze.getOnz().setSagaContextTimeoutMs(1);
		App.Instance.Zeze.getOnz().cleanupTimeoutSagas();
		App.Instance.Zeze.getOnz().setSagaContextTimeoutMs(Zeze.Onz.Onz.eDefaultSagaContextTimeoutMs);

		Infinite.App.clearDbTable(zeze2.demo_Module1.getKuafu());
		Infinite.App.clearDbTable(App.Instance.demo_Module1.getKuafu());

		if (registeredOnAppInstance.compareAndSet(false, true))
			App.Instance.Zeze.getOnz().registerSaga(SagaName,
					TestOnzSagaStalledStepKeepsCompensation::sagaBusiness,
					TestOnzSagaStalledStepKeepsCompensation::sagaCancel,
					BKuafu.class, BKuafuResult.class, EmptyBean.class);

		onzServer = startOnzServer(myConfig);
		stopRedoDaemon(); // 唯一驱动源=本测试的同步invokeRedoTimer
	}

	@AfterEach
	public void after() throws Exception {
		stopCoordinator(onzServer, zeze2);
		// 收割本类残留（滞留FuncSaga执行后注册的上下文），恢复TTL默认。
		App.Instance.Zeze.getOnz().setSagaContextTimeoutMs(1);
		App.Instance.Zeze.getOnz().cleanupTimeoutSagas();
		App.Instance.Zeze.getOnz().setSagaContextTimeoutMs(Zeze.Onz.Onz.eDefaultSagaContextTimeoutMs);
	}

	private static long sagaBusiness(Zeze.Onz.OnzSaga saga, BKuafu argument, BKuafuResult result) {
		var app = (App)saga.getStub().getOnz().getZeze().getAppBase();
		var account = app.demo_Module1.getKuafu().getOrAdd(argument.getAccount());
		account.setMoney(account.getMoney() + argument.getMoney()); // 参与方本地提交（发结果即提交）
		result.setMoney(account.getMoney());
		return 0;
	}

	private static long sagaCancel(Zeze.Onz.OnzSaga saga, EmptyBean cancelArgument) {
		CancelCount++;
		return 0;
	}

	/** FakeTxn：不走perform，直接驱动rollback()（其内部cancelSaga）；步骤future按用例注入。 */
	static class FakeTxn extends OnzTransaction<EmptyBean.Data, EmptyBean.Data> {
		@Override
		protected long perform() {
			return 0; // 不进入
		}
	}

	/**
	 * 滞留交错（同步驱动构造）：未应答失败步骤（rpc超时形态的future）的两次cancel都命中
	 * NotFound（参与方上下文未注册=FuncSaga尚未被处理），give-up后滞留的FuncSaga才执行——
	 * 修复前give-up视为了结（rollback返回true→perform删决策记录），补偿永久失去；
	 * 修复后保留决策记录，redo补发的cancel命中迟到的上下文完成补偿并收敛。
	 */
	@Test
	@Timeout(120)
	public void testStalledSagaCompensatedViaRedoAfterGiveUp() throws Exception {
		waitOnzReady(onzServer);
		var commitIndex = tableOf(onzServer, "commitIndex");

		// perform语境的决策记录（rollback()返回false时不删——补偿通道保留）。
		writeOrphanRecords(onzServer, StalledTid, AbstractOnz.ePreparing);
		Assertions.assertEquals(1, count(commitIndex), "决策记录写入前置");

		// 未应答失败步骤（rpc超时：泛型异常完成）：cancelSaga走NotFound重试路径。
		var timeout = new TaskCompletionSource<EmptyBean.Data>();
		timeout.setException(new RuntimeException("fake saga rpc timeout"));
		var txn = new FakeTxn();
		txnField("onzServer").set(txn, onzServer);
		txnField("onzTid").setLong(txn, StalledTid);
		@SuppressWarnings("unchecked")
		var sagas = (ConcurrentHashMap<String, TaskCompletionSource<?>>)txnField("zezeSagas").get(txn);
		sagas.put("zeze1", timeout);
		txn.setFlushTimeout(1_000); // 控时：重试sleep+rpc共约2s

		var allDelivered = txn.rollback();

		Assertions.assertFalse(allDelivered,
				"重试仍NotFound的give-up不得视为了结（未证缺席：滞留FuncSaga可能晚于决策记录删除才执行——"
						+ "修复前返回true，perform即删记录，补偿永久失去）");
		Assertions.assertEquals(0, CancelCount, "上下文未注册：本次cancel无法补偿");
		Assertions.assertEquals(1, count(commitIndex), "决策记录必须保留（redo是唯一自动补发通道）");

		// 队列恢复：滞留的FuncSaga执行并本地提交（上下文注册、发结果即提交）。
		startSagaContext(onzServer, SagaName, Account, StalledTid);

		// redo轮补发cancel：命中迟到的在场上下文，补偿执行并收敛删除记录。
		invokeRedoTimer(onzServer);

		Assertions.assertTrue(CancelCount >= 1, "保留的决策记录必须经redo补偿迟到的FuncSaga写入");
		Assertions.assertEquals(0, count(commitIndex), "补偿成功后redo必须收敛删除决策记录");
		Assertions.assertEquals(0, sagaCount(App.Instance.Zeze.getOnz()), "补偿成功后参与方上下文清理");
	}

	private static Field txnField(String name) throws Exception {
		var field = OnzTransaction.class.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	/** 停后台redo守护（反射）：手动invokeRedoTimer保证轮次串行的前提是唯一驱动源。 */
	private void stopRedoDaemon() throws Exception {
		var field = OnzServer.class.getDeclaredField("redoDaemon");
		field.setAccessible(true);
		((Zeze.Util.DaemonTimer)field.get(onzServer)).stop();
	}
}
