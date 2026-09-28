package Zeze.Onz;

import java.util.concurrent.atomic.AtomicBoolean;

import Zeze.Onz.AbstractOnz;
import Zeze.Onz.OnzServer;
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
 * FND19 GC-C01 回归：redo() 对参与方应答只 await 不查结果码即删决策记录。
 * 参与方补偿失败（stub.end返回非0）按契约把上下文放回sagas等重发
 * （Onz.ProcessFuncSagaEndRequest），redo是崩溃/失败后唯一的自动重试通道——
 * 修复前该非0应答被无视、两表记录被删，补偿永久丢失（已提交saga步骤静默
 * 部分提交分歧，同文件commit()有完整对照组）。修复后对齐commit()的形态：
 * 非0码保留记录等下一轮redo幂等收敛；eSagaNotFound（上下文已清理无补偿对象）
 * 可辨识忽略。
 */
public class TestGcC01RedoResultCode {
	// 过程名必须全JVM唯一：demo.App单例的Onz注册表跨测试类持久。
	private static final AtomicBoolean registeredOnAppInstance = new AtomicBoolean();
	private static final String SagaName = "fnd19c1SagaRedo";

	// 手动rpc伪造的孤儿决策tid（避开OnzServer.nextOnzTid的分配空间）
	private static final long CancelFailTid = 0x5CA1BEEF00000201L;
	private static final long NotFoundTid = 0x5CA1BEEF00000202L;

	// 补偿失败开关与计数（cancel stub在参与方线程执行；@BeforeEach重置，volatile供跨线程读取）
	static volatile boolean CancelFail;
	static volatile int CancelCount;

	private final App zeze2 = new App();
	private OnzServer onzServer;

	@BeforeEach
	public void before() throws Exception {
		CancelFail = false;
		CancelCount = 0;
		var myConfig = startTwoClusters(zeze2);

		Infinite.App.clearDbTable(zeze2.demo_Module1.getKuafu());
		Infinite.App.clearDbTable(App.Instance.demo_Module1.getKuafu());

		if (registeredOnAppInstance.compareAndSet(false, true))
			App.Instance.Zeze.getOnz().registerSaga(SagaName,
					TestGcC01RedoResultCode::sagaBusiness, TestGcC01RedoResultCode::sagaCancel,
					BKuafu.class, BKuafuResult.class, Zeze.Transaction.EmptyBean.class);

		onzServer = startOnzServer(myConfig);
	}

	@AfterEach
	public void after() throws Exception {
		stopCoordinator(onzServer, zeze2);
	}

	private static long sagaBusiness(Zeze.Onz.OnzSaga saga, BKuafu argument, BKuafuResult result) {
		var app = (App)saga.getStub().getOnz().getZeze().getAppBase();
		var account = app.demo_Module1.getKuafu().getOrAdd(argument.getAccount());
		account.setMoney(account.getMoney() + argument.getMoney()); // 参与方本地提交（发结果即提交）
		result.setMoney(account.getMoney());
		return 0;
	}

	/** 补偿失败注入：返回非0（业务rc原样线上携带，非moduleId组合值）——参与方按契约放回上下文等重发。 */
	private static long sagaCancel(Zeze.Onz.OnzSaga saga, Zeze.Transaction.EmptyBean cancelArgument) {
		CancelCount++;
		return CancelFail ? 100 : 0;
	}

	/**
	 * 核心红测：参与方补偿失败（非0应答）后redo必须保留决策记录——修复前await后无条件
	 * removeCommitRecord，掐断唯一自动重试通道。故障恢复（补偿成功）后下一轮redo完成收敛。
	 */
	@Test
	@Timeout(120)
	public void testRedoKeepsRecordWhenCompensationFails() throws Exception {
		waitOnzReady(onzServer);
		CancelFail = true;
		startSagaContext(onzServer, SagaName, 310, CancelFailTid);
		writeOrphanRecords(onzServer, CancelFailTid, AbstractOnz.ePreparing);

		invokeRedoTimer(onzServer);

		Assertions.assertTrue(CancelCount >= 1, "参与方补偿必须被redo触发过");
		Assertions.assertEquals(1, count(tableOf(onzServer, "commitIndex")),
				"补偿失败（非0应答）必须保留决策记录等下一轮redo重试"
						+ "（修复前：await后无条件删除，已提交步骤永久未补偿）");
		Assertions.assertEquals(1, count(tableOf(onzServer, "commitPoint")), "两表同生命周期（FND4-88）：索引保留则点表保留");
		Assertions.assertEquals(1, sagaCount(App.Instance.Zeze.getOnz()),
				"参与方补偿失败后上下文必须仍在（按契约放回sagas等重发）");

		// 故障恢复：补偿成功后保留的记录驱动重试，redo完成收敛。
		CancelFail = false;
		invokeRedoTimer(onzServer);

		Assertions.assertTrue(CancelCount >= 2, "保留的记录必须驱动补偿重试");
		Assertions.assertEquals(0, count(tableOf(onzServer, "commitIndex")), "补偿成功后redo必须清理决策记录");
		Assertions.assertEquals(0, count(tableOf(onzServer, "commitPoint")), "两表同生命周期：一起清理");
		Assertions.assertEquals(0, sagaCount(App.Instance.Zeze.getOnz()), "补偿成功后参与方上下文清理");
	}

	/**
	 * eSagaNotFound契约守护（防修复矫枉过正）：参与方上下文不存在（业务失败自清理/TTL回收/
	 * 重复补发已处理）时应答eSagaNotFound——无补偿对象，redo必须忽略该码照常清理记录，
	 * 不得把NotFound当失败永久滞留。线上为moduleId组合值，解码后比较。
	 */
	@Test
	@Timeout(120)
	public void testRedoIgnoresSagaNotFound() throws Exception {
		waitOnzReady(onzServer);
		writeOrphanRecords(onzServer, NotFoundTid, AbstractOnz.ePreparing); // 不startSagaContext：参与方无上下文

		invokeRedoTimer(onzServer);

		Assertions.assertEquals(0, CancelCount, "无上下文不得触发补偿");
		Assertions.assertEquals(0, count(tableOf(onzServer, "commitIndex")), "eSagaNotFound可辨识忽略，记录照常清理");
		Assertions.assertEquals(0, count(tableOf(onzServer, "commitPoint")), "两表同生命周期：一起清理");
	}
}
