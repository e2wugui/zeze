package Zeze.Onz;

import java.util.concurrent.CountDownLatch;
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

import static Zeze.Onz.GcOnzE2eTestSupport.*;

/**
 * onz-01 回归：补偿执行期间上下文被摘除的窗口。参与方处理 FuncSagaEnd(cancel=true)
 * 时先把上下文移出 sagas、补偿失败再放回——窗口（等于补偿全程）内重发的 FuncSagaEnd
 * （sagas.get 先于 businessLock）直接得 null 应答 eSagaNotFound，协调者对 rollback
 * 决策的年轻 NotFound 按良性终态删除决策记录；随后失败的补偿把上下文放回，但 redo
 * 通道已随记录删除而消失——eCompensateFail 落在早已超时的 rpc 上无人接收，补偿静默
 * 丢失（1 小时后 TTL 清理只剩 warn）。修复：摘除点移到补偿成功之后，补偿在条目在场、
 * businessLock 全程持有的互斥域内执行——重发先 get 命中再在锁上排队，不再产生窗口内
 * 的假 NotFound。判别形态：闸住补偿把窗口无限期打开，第二轮 redo 的重发必须仍能等到
 * 锁并触发记录保留（修复前：窗口内 NotFound→记录被删）；放行后等锁的重发必须再次
 * 补偿、第三轮 redo 的活 rpc 收到 eCompensateFail 后记录仍保留（修复前：CancelCount
 * 停在 1，补偿静默丢失）。
 */
public class TestOnzSagaCompensateInflightNotFound {
	// 过程名必须全 JVM 唯一：demo.App 单例的 Onz 注册表跨测试类持久。
	private static final AtomicBoolean registeredOnAppInstance = new AtomicBoolean();
	private static final String SagaName = "onzCompensateInflightGate";

	// 手动rpc伪造的孤儿决策tid（避开OnzServer.nextOnzTid的分配空间）
	private static final long CompensateInflightTid = 0x5CA2E5A1000003E2L;

	private static final long Account = 411;

	// 补偿记账（cancel stub在参与方worker执行；@BeforeEach重置，volatile供跨线程轮询）。
	// businessLock内串行（每时刻至多一个worker在sagaCancel内），非原子自增无竞态。
	static volatile int CancelCount;
	static volatile int CancelInFlight;

	// 补偿闸门：关闭期间把修复前的[remove, putIfAbsent)窗口（=补偿全程）无限期打开；
	// 修复后形态则是"补偿持有businessLock、重发在锁上排队"的互斥域。放行（countDown）
	// 后保持打开，后续补偿直通。
	static volatile CountDownLatch releaseGate;

	private final App zeze2 = new App();
	private OnzServer onzServer;

	@BeforeEach
	public void before() throws Exception {
		CancelCount = 0;
		CancelInFlight = 0;
		releaseGate = new CountDownLatch(1);
		var myConfig = startTwoClusters(zeze2);

		// fixture隔离（对齐TestOnzSagaPersistRedo先例）：App.Instance跨测试类进程级持久，
		// (a)前序类可能离场时未恢复sagaContextTimeoutMs=1ms——60s周期清理会在本用例窗口内
		// 清掉刚注册的上下文；(b)前序类（如TestOnzSagaCompensateFailErrorCode）在sagas留有
		// 补偿失败放回的上下文，本类用例与共享桩的startSagaContext按sagaCount绝对值等待。
		// 收割残留并恢复TTL默认。
		App.Instance.Zeze.getOnz().setSagaContextTimeoutMs(1);
		App.Instance.Zeze.getOnz().cleanupTimeoutSagas();
		App.Instance.Zeze.getOnz().setSagaContextTimeoutMs(Zeze.Onz.Onz.eDefaultSagaContextTimeoutMs);

		Infinite.App.clearDbTable(zeze2.demo_Module1.getKuafu());
		Infinite.App.clearDbTable(App.Instance.demo_Module1.getKuafu());

		if (registeredOnAppInstance.compareAndSet(false, true))
			App.Instance.Zeze.getOnz().registerSaga(SagaName,
					TestOnzSagaCompensateInflightNotFound::sagaBusiness,
					TestOnzSagaCompensateInflightNotFound::sagaCancel,
					BKuafu.class, BKuafuResult.class, Zeze.Transaction.EmptyBean.class);

		onzServer = startOnzServer(myConfig);
	}

	@AfterEach
	public void after() throws Exception {
		// 红态失败时闸门可能仍关闭：先放行滞留在闸内/锁上的补偿worker（不泄漏派发线程），
		// 等在飞补偿归零后再收尾（放回/在场的上下文收割见下，均无锁竞争）。
		if (releaseGate != null)
			releaseGate.countDown();
		waitCancelQuiet();
		stopCoordinator(onzServer, zeze2);
		// 收割本类残留（补偿恒失败的上下文滞留sagas）：后续按sagaCount绝对值断言的类
		// （TestOnzSagaCompensateFailErrorCode）不受污染。
		App.Instance.Zeze.getOnz().setSagaContextTimeoutMs(1);
		App.Instance.Zeze.getOnz().cleanupTimeoutSagas();
		App.Instance.Zeze.getOnz().setSagaContextTimeoutMs(Zeze.Onz.Onz.eDefaultSagaContextTimeoutMs);
	}

	/** 在飞补偿归零的有界静默等待（after()内不得用断言型waitUntil）。 */
	private static void waitCancelQuiet() throws InterruptedException {
		long deadline = System.currentTimeMillis() + 30_000;
		while (CancelInFlight != 0 && System.currentTimeMillis() < deadline)
			//noinspection BusyWait
			Thread.sleep(50);
	}

	private static long sagaBusiness(Zeze.Onz.OnzSaga saga, BKuafu argument, BKuafuResult result) {
		var app = (App)saga.getStub().getOnz().getZeze().getAppBase();
		var account = app.demo_Module1.getKuafu().getOrAdd(argument.getAccount());
		account.setMoney(account.getMoney() + argument.getMoney());
		result.setMoney(account.getMoney());
		return 0;
	}

	/** 补偿闸住（首次）/直通（放行后），恒返2：模拟慢且失败的补偿——缺陷的目标工况。 */
	private static long sagaCancel(Zeze.Onz.OnzSaga saga, Zeze.Transaction.EmptyBean cancelArgument) throws Exception {
		CancelCount++;
		CancelInFlight++;
		try {
			releaseGate.await();
		} finally {
			CancelInFlight--;
		}
		return 2;
	}

	@Test
	@Timeout(180)
	public void testResendDuringInflightCompensateWaitsNotNotFound() throws Exception {
		waitOnzReady(onzServer);
		stopRedoDaemon(); // 唯一驱动源=本测试的同步invokeRedoTimer（轮次串行的前提）

		startSagaContext(onzServer, SagaName, Account, CompensateInflightTid);
		writeOrphanRecords(onzServer, CompensateInflightTid, AbstractOnz.ePreparing);
		var commitIndex = tableOf(onzServer, "commitIndex");
		Assertions.assertEquals(1, count(commitIndex), "孤儿决策记录写入前置");

		// round1：闸住的慢补偿在途（修复前：上下文已摘除，[remove, putIfAbsent)窗口全开；
		// 修复后：上下文在场、businessLock持有）。rpc 30s 超时→记录保留（两形态一致）。
		invokeRedoTimer(onzServer);
		Assertions.assertEquals(1, CancelCount, "第一轮redo必须触发补偿（闸内滞留）");
		Assertions.assertEquals(1, count(commitIndex), "补偿慢于rpc超时（30s）时记录必须保留交下一轮重发");

		// 判别点round2：重发的FuncSagaEnd必须命中在场上下文→等锁→rpc超时→记录保留。
		// 修复前：窗口内get=null→eSagaNotFound→rollback年轻NotFound良性删除决策记录
		//（实测count=0），补偿重试通道随记录消失。
		invokeRedoTimer(onzServer);
		Assertions.assertEquals(1, count(commitIndex),
				"补偿在途时重发的FuncSagaEnd不得得到eSagaNotFound（须get命中后等锁，rpc超时保留记录）");

		// 放行：worker A失败（eCompensateFail落在已超时的round1死rpc上无人接收）→
		// 等锁的worker B醒来再次补偿。修复前：无等锁者（记录已删），补偿停在1。
		releaseGate.countDown();
		waitUntil(() -> CancelCount >= 2 && CancelInFlight == 0, 30_000,
				"等锁的重复FuncSagaEnd必须再次补偿（修复前：决策记录已被NotFound删除，补偿静默丢失）");

		// round3：worker C的补偿在活rpc上应答eCompensateFail→协调者按未知非零码保留记录。
		invokeRedoTimer(onzServer);
		Assertions.assertEquals(1, count(commitIndex), "eCompensateFail必须保留决策记录交redo重发");
		Assertions.assertTrue(CancelCount >= 3, "补偿重试链必须存活（修复前停在第1次）");
		Assertions.assertEquals(1, sagaCount(App.Instance.Zeze.getOnz()),
				"补偿失败的上下文必须仍在sagas等重发（修复前放回后永无重发，仅TTL清理）");
	}

	/** 停后台redo守护（反射）：60s周期的后台轮与本用例每轮30s的手动驱动交错会产生
	 * 不确定的额外重发，invokeRedoTimer同步反射调用保证轮次串行的前提是唯一驱动源。 */
	private void stopRedoDaemon() throws Exception {
		var field = OnzServer.class.getDeclaredField("redoDaemon");
		field.setAccessible(true);
		((Zeze.Util.DaemonTimer)field.get(onzServer)).stop();
	}
}
