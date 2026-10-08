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
 * FND24 onz-03 回归：补偿失败的线上结果码不得为用户裸 rc——用户补偿函数返回值与协议
 * 错误码共用低 32位命名空间（协调者统一 getErrorCode 解码），rc 恰为 2 时被误判
 * eSagaNotFound：redo 走 NotFound 分支不再重发补偿，且决策记录按超龄路径被删——参与方
 * 放回 sagas 等待重发的补偿上下文永无重试，补偿永久丢失。修复后失败路径统一回
 * eCompensateFail（用户 rc 记录在参与方日志），协调者按未知非零码保留记录交 redo
 * 幂等重发。判别形态：补偿恒返 rc=2，第二轮 redo 必须继续触发补偿（修复前 NotFound
 * 分支跳过重发，计数停在 1）且记录保留。
 */
public class TestOnzSagaCompensateFailErrorCode {
	// 过程名必须全 JVM 唯一：demo.App 单例的 Onz 注册表跨测试类持久。
	private static final AtomicBoolean registeredOnAppInstance = new AtomicBoolean();
	private static final String SagaName = "OnzSagaCompensateRc";

	// 手动rpc伪造的孤儿决策tid（避开OnzServer.nextOnzTid的分配空间）
	private static final long CompensateRcTid = 0x5CA2E5A1000003E1L;

	static volatile int CancelCount;

	private final App zeze2 = new App();
	private OnzServer onzServer;

	@BeforeEach
	public void before() throws Exception {
		CancelCount = 0;
		var myConfig = startTwoClusters(zeze2);

		Infinite.App.clearDbTable(zeze2.demo_Module1.getKuafu());
		Infinite.App.clearDbTable(App.Instance.demo_Module1.getKuafu());

		if (registeredOnAppInstance.compareAndSet(false, true))
			App.Instance.Zeze.getOnz().registerSaga(SagaName,
					TestOnzSagaCompensateFailErrorCode::sagaBusiness, TestOnzSagaCompensateFailErrorCode::sagaCancel,
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
		account.setMoney(account.getMoney() + argument.getMoney());
		result.setMoney(account.getMoney());
		return 0;
	}

	/** 补偿恒返 2：恰与 eSagaNotFound 同值的用户结果码（原缺陷的碰撞形态）。 */
	private static long sagaCancel(Zeze.Onz.OnzSaga saga, Zeze.Transaction.EmptyBean cancelArgument) {
		CancelCount++;
		return 2;
	}

	@Test
	@Timeout(120)
	public void testCompensateRcTwoNotSwallowedAsNotFound() throws Exception {
		waitOnzReady(onzServer);
		startSagaContext(onzServer, SagaName, 410, CompensateRcTid);
		writeOrphanRecords(onzServer, CompensateRcTid, AbstractOnz.ePreparing);

		invokeRedoTimer(onzServer);
		Assertions.assertTrue(CancelCount >= 1, "参与方补偿必须被redo触发");
		Assertions.assertEquals(1, count(tableOf(onzServer, "commitIndex")),
				"补偿失败必须保留决策记录（上下文已放回sagas等重发）");
		Assertions.assertEquals(1, sagaCount(App.Instance.Zeze.getOnz()), "参与方补偿失败后上下文必须仍在");

		// 判别点：rc=2 的失败不得被解码为 eSagaNotFound——第二轮 redo 必须继续重发补偿
		//（修复前：NotFound 分支跳过重发且按超龄删记录，放回的上下文永无重试）。
		invokeRedoTimer(onzServer);
		Assertions.assertTrue(CancelCount >= 2, "rc=2 的补偿失败必须继续重试（修复前被误判 eSagaNotFound 吞掉）");
		Assertions.assertEquals(1, count(tableOf(onzServer, "commitIndex")),
				"重试通道必须保留（修复前按 NotFound 超龄删除，补偿永久丢失）");
	}
}
