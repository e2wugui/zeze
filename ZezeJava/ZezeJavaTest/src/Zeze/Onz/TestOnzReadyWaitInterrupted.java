package Zeze.Onz;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Application;
import Zeze.Builtin.Onz.BFuncProcedure;
import Zeze.Builtin.Onz.FuncProcedure;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.LongConcurrentHashMap;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FND24 onz-01 回归：参与方 sendReadyAndWait 的等待 park 被任务看门狗中断时，中断必须
 * 视同超时自愈——要么留下超时哨兵（markTimeoutRolledBack CAS 成功），要么等到既成决策
 * （决策线程已取走条目并将完成 future），绝不把活条目留给迟到的 Commit 干净应答（那会：
 * 本地已回滚、协调者报成功=静默部分提交；分歧 error 仅在取到哨兵时触发，该路径零标记）。
 * <p>
 * 不搭网络（Onz 无服务配置纯内存，见 TestOnzRollbackAfterReady 先例）；以线程 interrupt
 * 复现看门狗的全部效果（ThreadDiagnosable 对 Normal 优先级线程的动作就是一次 interrupt）。
 */
@Fast
public class TestOnzReadyWaitInterrupted {
	// 独立 serverId+url：@Fast 类并行时避免本地缓存目录与内存库互撞。
	private static final int SERVER_ID = FastServerIds.TEST_ONZ_READY_WAIT_INTERRUPTED;

	private Application app;
	private Onz onz;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸 Config 不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("fnd24_onz01_" + SERVER_ID);
		conf.getDatabaseConfMap().put("", dbConf);
		app = new Application("TestOnzReadyWaitInterrupted", conf);
		app.start();
		onz = new Onz(app); // 无 Onz 服务配置：service=null，纯内存结构，无网络
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app.getStartState() != Application.StartState.eStopped)
			app.stop(); // 已停实例幂等
	}

	/** 无网络替身 rpc：sendReadyAndWait 只经无参 SendResult()→SendResult(null) 发结果，
	 * 覆写一参形态使其 no-op（无参形态为 final 不可覆写）。 */
	private static FuncProcedure silentRpc() {
		return new FuncProcedure() {
			@Override
			public void SendResult(Binary result) {
			}
		};
	}

	private OnzProcedure newProcedure(long tid) {
		var stub = new OnzProcedureStub<EmptyBean, EmptyBean>(
				onz, "Fnd24Onz01." + tid, (p, a, r) -> 0L, EmptyBean.class, EmptyBean.class);
		var funcArgument = new BFuncProcedure.Data();
		funcArgument.setOnzTid(tid);
		funcArgument.setFlushMode(AbstractOnz.eFlushAsync);
		funcArgument.setFlushTimeout(60_000); // 长于测试时长：被测路径是中断而非超时
		return new OnzProcedure(silentRpc(), funcArgument, stub, new EmptyBean(), new EmptyBean());
	}

	/** 等线程进入给定 park 状态（限时 park=TIMED_WAITING、无限时 park=WAITING）。 */
	private static void awaitParked(Thread t, Thread.State expect) throws InterruptedException {
		var deadline = System.currentTimeMillis() + 10_000;
		while (t.getState() != expect) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline,
					"等待线程进入 " + expect + " 超时，当前 " + t.getState());
			//noinspection BusyWait
			Thread.sleep(10);
		}
	}

	/** 中断落在限时等待 park 内且无并发决策：CAS 占哨兵成功——以异常结束等待（触发本地
	 * 回滚）、恢复中断标志、槽位置换为哨兵（修复前：中断逃逸无哨兵，活条目留给迟到的
	 * Commit 干净应答，分歧零标记）。 */
	@Test
	public void testInterruptAloneMarksTimeoutSentinel() throws Exception {
		var p = newProcedure(3001L);
		var thrown = new AtomicReference<Throwable>();
		var t = Thread.ofPlatform().daemon().start(() -> {
			try {
				p.sendReadyAndWait();
			} catch (Throwable e) {
				thrown.set(e);
			}
		});
		try {
			awaitParked(t, Thread.State.TIMED_WAITING); // 登记（markReadyProcedure）先于 park，此处必已完成
			t.interrupt(); // 看门狗的全部效果就是这一次 interrupt
			t.join(10_000);
			Assertions.assertFalse(t.isAlive(), "被中断的等待必须结束（异常路径）");

			var e = thrown.get();
			Assertions.assertNotNull(e, "CAS 占哨兵成功必须以异常结束等待（触发本地回滚）");
			Assertions.assertTrue(e instanceof RuntimeException && String.valueOf(e.getMessage()).contains("interrupted"),
					"异常必须可归因为中断: " + e);
			Assertions.assertTrue(t.isInterrupted(), "中断标志必须恢复（任务被中断的事实不丢失）");
			// 槽位必须持有哨兵：markTimeoutRolledBack 的 CAS 对已置换槽位必失败——
			// 修复前槽位仍是活 procedure，CAS 成功（活条目泄漏=迟到 Commit 干净应答成功）。
			Assertions.assertFalse(onz.markTimeoutRolledBack(p), "槽位必须持有超时哨兵而非活条目");
			// 迟到的 Commit 取哨兵走分歧 error：同 tid 重注册被哨兵拒绝。
			Assertions.assertThrows(RuntimeException.class, () -> onz.markReadyProcedure(newProcedure(3001L)),
					"哨兵占位必须拒绝重注册");
		} finally {
			t.interrupt();
			t.join(10_000);
		}
	}

	/** 中断到达前决策线程已取走条目（Commit 的 remove 与 future 完成之间无阻塞点）：
	 * CAS 失败——不抛出、等既成决策完成，按提交正常返回并恢复中断标志（修复前：中断
	 * 直接逃逸，本地把已到达的 commit 翻成回滚=协调者与参与方分歧）。 */
	@Test
	public void testInterruptAfterDecisionTakenWaitsCommit() throws Exception {
		var p = newProcedure(3002L);
		var thrown = new AtomicReference<Throwable>();
		var t = Thread.ofPlatform().daemon().start(() -> {
			try {
				p.sendReadyAndWait();
			} catch (Throwable e) {
				thrown.set(e);
			}
		});
		try {
			awaitParked(t, Thread.State.TIMED_WAITING);

			// 模拟 ProcessCommitRequest 取条目：先 remove 使中断时的 CAS 必失败，
			// future 稍后由 p.commit() 完成（决策在途、未完成）。
			var map = readyProceduresOf(onz);
			Assertions.assertSame(p, map.remove(3002L), "决策前槽位必须是活 procedure");
			t.interrupt();
			awaitParked(t, Thread.State.WAITING); // CAS 失败路进入无超时等待（决策未完成）

			p.commit(); // 决策线程完成 future：既成决策=提交
			t.join(10_000);
			Assertions.assertFalse(t.isAlive(), "既成决策完成，等待必须结束");
			Assertions.assertNull(thrown.get(), "既成 commit 不得被中断翻成异常/回滚（修复前中断直接逃逸）");
			Assertions.assertTrue(t.isInterrupted(), "中断标志必须恢复");
		} finally {
			t.interrupt();
			p.commit(); // 兜底释放可能残留的等待
			t.join(10_000);
		}
	}

	/** 反射缝：直取 Onz 私有 readyProcedures（模拟决策线程的 remove；先例见 Fnd19MqTestSupport）。 */
	@SuppressWarnings("unchecked")
	private static LongConcurrentHashMap<OnzProcedure> readyProceduresOf(Onz onz) throws Exception {
		var f = Onz.class.getDeclaredField("readyProcedures");
		f.setAccessible(true);
		return (LongConcurrentHashMap<OnzProcedure>)f.get(onz);
	}
}
