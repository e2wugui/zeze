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
 * onz-01 回归：saga 参与方补偿上下文纯内存，参与方在"步骤已应答成功且已落库→
 * FuncSagaEnd(cancel) 到达"窗口内重启后，rollback 决策的年轻 eSagaNotFound 被
 * cancelSaga 无日志良性化（allDelivered=true → perform 删决策记录），已持久化的
 * 写入永无补偿而事务报告失败——补偿丢失零信号。修复：cancelSaga 对步骤应答形态
 * 三分类，已应答成功步骤的 NotFound（本事务未曾成功投递过 cancel 的形态）按
 * "上下文消失"嫌疑保守处理——保留决策记录（rollback 返回 false）+ 嫌疑登记
 * （OnzServer.noteSagaContextLost，error 一次；redo 对 rollback 决策年轻 NotFound
 * 据此保留，补偿可经 redo 到达上下文重新在场的参与方并收敛）。已应答业务失败的
 * 步骤（参与方失败自清理）维持良性终态，不误伤成噪声。
 */
public class TestOnzSagaAnsweredStepContextLost {
	// 过程名必须全 JVM 唯一：demo.App 单例的 Onz 注册表跨测试类持久。
	private static final AtomicBoolean registeredOnAppInstance = new AtomicBoolean();
	private static final String SagaName = "onzAnsweredCtxLost";

	// 手动rpc伪造的tid（避开OnzServer.nextOnzTid的分配空间；与其他测试类的0x...段错开）
	private static final long LostTid = 0x5CA2E5A1000004E1L;
	private static final long BenignTid = 0x5CA2E5A1000004E2L;
	private static final long Account = 412;

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
					TestOnzSagaAnsweredStepContextLost::sagaBusiness,
					TestOnzSagaAnsweredStepContextLost::sagaCancel,
					BKuafu.class, BKuafuResult.class, EmptyBean.class);

		onzServer = startOnzServer(myConfig);
		stopRedoDaemon(); // 唯一驱动源=本测试的同步invokeRedoTimer（对齐TestOnzSagaCompensateInflightNotFound）
	}

	@AfterEach
	public void after() throws Exception {
		stopCoordinator(onzServer, zeze2);
		// 收割本类残留（清 sagas 后注册的上下文），恢复TTL默认。
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

	/** 构造绑定真实OnzServer/指定tid/单步骤future的事务（flushTimeout压小控时）。 */
	private FakeTxn bindTxn(long tid, TaskCompletionSource<?> stepFuture) throws Exception {
		var txn = new FakeTxn();
		txnField("onzServer").set(txn, onzServer);
		txnField("onzTid").setLong(txn, tid);
		@SuppressWarnings("unchecked")
		var sagas = (ConcurrentHashMap<String, TaskCompletionSource<?>>)txnField("zezeSagas").get(txn);
		sagas.put("zeze1", stepFuture);
		txn.setFlushTimeout(2_000); // 控时：本地回环的NotFound应答毫秒级完成
		return txn;
	}

	@Test
	@Timeout(120)
	public void testAnsweredSuccessNotFoundKeepsRecordForRedo() throws Exception {
		waitOnzReady(onzServer);
		var commitIndex = tableOf(onzServer, "commitIndex");

		// 参与方重启形态：真实注册上下文（业务提交、滞留等FuncSagaEnd）后清空sagas——
		// 写已持久化、补偿上下文丢失（进程内模拟重启）。
		startSagaContext(onzServer, SagaName, Account, LostTid);
		clearSagaContexts();
		Assertions.assertEquals(0, sagaCount(App.Instance.Zeze.getOnz()), "模拟重启：上下文必须已清空");

		// 协调者cancel决策：该步骤曾应答成功（future正常完成），cancel命中eSagaNotFound。
		var ok = new TaskCompletionSource<EmptyBean.Data>();
		ok.setResult(EmptyBean.Data.instance);
		var txn = bindTxn(LostTid, ok);

		var allDelivered = txn.rollback();

		Assertions.assertFalse(allDelivered,
				"已应答成功步骤的NotFound必须保守保留决策记录（修复前：无日志良性化，perform即删记录，补偿丢失零信号）");
		Assertions.assertEquals(0, CancelCount, "上下文已丢：本次cancel无法补偿（写已持久化、义务丢失）");
		Assertions.assertTrue(agedNotFoundWarnedTids().contains(LostTid),
				"上下文消失嫌疑必须登记（error一次+redo对年轻NotFound的保守保留依据）");

		// 保守保留的决策记录（perform语境）+ 补偿经redo到达上下文重新在场的参与方后收敛。
		writeOrphanRecords(onzServer, LostTid, AbstractOnz.ePreparing);
		startSagaContext(onzServer, SagaName, Account, LostTid); // "恢复后的参与方"：补偿义务在场
		invokeRedoTimer(onzServer);

		Assertions.assertTrue(CancelCount >= 1, "保留的决策记录必须驱动补偿经redo到达参与方（修复前：记录已删，补偿永久失去）");
		Assertions.assertEquals(0, count(commitIndex), "补偿成功后redo必须收敛删除决策记录");
		Assertions.assertEquals(0, sagaCount(App.Instance.Zeze.getOnz()), "补偿成功后参与方上下文清理");
	}

	/** 对照（防噪声）：已应答业务失败的步骤由参与方失败自清理，其NotFound是良性终态——
	 * 不登记嫌疑、不保守保留（修复前后行为一致，钉住三分类不误伤常态良性）。 */
	@Test
	@Timeout(120)
	public void testAnsweredFailNotFoundStaysBenign() throws Exception {
		waitOnzReady(onzServer);

		// 该tid从未注册上下文（参与方失败自清理后的缺席终态），cancel必得eSagaNotFound。
		var answeredFail = new TaskCompletionSource<EmptyBean.Data>();
		answeredFail.setException(new OnzAgent.CallAnsweredException("fake business fail"));
		var txn = bindTxn(BenignTid, answeredFail);

		Assertions.assertTrue(txn.rollback(),
				"已应答业务失败步骤的NotFound是良性终态（失败自清理；不得保守保留制造噪声）");
		Assertions.assertFalse(agedNotFoundWarnedTids().contains(BenignTid), "良性终态不得登记嫌疑");
		Assertions.assertEquals(0, CancelCount);
	}

	// ---- 构造与收尾助手 ----

	private static Field txnField(String name) throws Exception {
		var field = OnzTransaction.class.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	@SuppressWarnings("unchecked")
	private java.util.Set<Long> agedNotFoundWarnedTids() throws Exception {
		var f = OnzServer.class.getDeclaredField("agedNotFoundWarnedTids");
		f.setAccessible(true);
		return (java.util.Set<Long>)f.get(onzServer);
	}

	/** 清空本参与方sagas（进程内模拟重启丢上下文）：TTL压1+收割+恢复默认，等业务完成后再清。 */
	private static void clearSagaContexts() throws Exception {
		var onz = App.Instance.Zeze.getOnz();
		// 业务在途（businessLock被占）的条目本轮跳过：给业务收尾留出窗口后重试收割。
		waitUntil(() -> {
			onz.setSagaContextTimeoutMs(1);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			onz.cleanupTimeoutSagas();
			onz.setSagaContextTimeoutMs(Zeze.Onz.Onz.eDefaultSagaContextTimeoutMs);
			return sagaCount(onz) == 0;
		}, 30_000, "模拟重启：sagas上下文未能清空（业务滞留businessLock）");
	}

	/** 停后台redo守护（反射）：手动invokeRedoTimer保证轮次串行的前提是唯一驱动源。 */
	private void stopRedoDaemon() throws Exception {
		var field = OnzServer.class.getDeclaredField("redoDaemon");
		field.setAccessible(true);
		((Zeze.Util.DaemonTimer)field.get(onzServer)).stop();
	}
}
