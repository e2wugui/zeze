package Zeze.Onz;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Application;
import Zeze.Builtin.Onz.BFuncProcedure;
import Zeze.Config;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-76 回归：Onz参与方"ready已发、Commit决策已送达"后本地因停机回滚
 * （perform的RejectWhileStopping分支）——协调者侧commit决策已持久化并按提交推进，
 * 本地写入未发生，跨集群部分提交且无分歧日志。修复：该catch分支内onzProcedure!=null
 * 时记error（回滚时刻是唯一信息完备且进程存活的确定性暴露点），并putIfAbsent回填
 * TimeoutRolledBackMarker哨兵（决策RPC已先行取走条目，replace必失败），使迟到的
 * redo Commit取到哨兵时经ProcessCommitRequest二次确认。
 * 测试1：回填的槽位语义（不覆盖存活条目；空槽回填为哨兵可被探测）。
 * 测试2：受控sendReadyAndWait（阻塞模拟"ready已发+决策已到"）钉死时序，stop完全
 * 结束后放行，事务必达finalCommit被拒——必须触发标记（修复前该分支无任何动作），
 * 并照常finalRollback+Closed（FND7-54语义不变）。
 */
@Fast
public class TestFnd876OnzRollbackAfterReady {
	// 独立serverId+url：@Fast类并行时避免本地RocksCache目录互撞。
	private static final int SERVER_ID = 12876;

	private Application app;
	private Onz onz;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("a2_fnd876_" + SERVER_ID);
		conf.getDatabaseConfMap().put("", dbConf);
		app = new Application("TestFnd876OnzRollbackAfterReady", conf);
		app.start();
		onz = new Onz(app); // 无Onz服务配置：service=null，纯内存结构，无网络
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app.getStartState() != Application.StartState.eStopped)
			app.stop(); // 已停实例幂等
	}

	private static OnzProcedure newProcedure(Onz onz, long tid) {
		var stub = new OnzProcedureStub<EmptyBean, EmptyBean>(
				onz, "Fnd876." + tid, (p, a, r) -> 0L, EmptyBean.class, EmptyBean.class);
		var funcArgument = new BFuncProcedure.Data();
		funcArgument.setOnzTid(tid);
		funcArgument.setFlushMode(AbstractOnz.eFlushAsync);
		funcArgument.setFlushTimeout(60_000);
		return new OnzProcedure(null, funcArgument, stub, new EmptyBean(), new EmptyBean());
	}

	/** 回填哨兵的槽位语义：不覆盖存活条目；条目已被决策取走（空槽）时回填成功且可探测。 */
	@Test
	public void test01BackfillMarkerSemantics() {
		var p1 = newProcedure(onz, 1001L);
		onz.markReadyProcedure(p1);
		// 决策未到达（条目仍是p1）：回填不得覆盖存活条目。
		assertFalse(onz.markRolledBackAfterReady(p1), "live entry must not be overwritten");
		assertTrue(onz.markTimeoutRolledBack(p1), "entry still holds p1 (未被回填污染)");

		// 条目已被决策取走（空槽，redo Commit重发前的状态）：回填成功。
		var p2 = newProcedure(onz, 1002L);
		assertTrue(onz.markRolledBackAfterReady(p2), "vacated slot must be backfilled with marker");
		// 槽位现在持有哨兵：replace(p2)失败（不是p2），putIfAbsent失败（被占用）——
		// 证明迟到的Commit取到的正是哨兵，会走ProcessCommitRequest的分歧error路径。
		assertFalse(onz.markTimeoutRolledBack(p2), "slot must hold the marker, not p2");
		var p2again = newProcedure(onz, 1002L);
		assertThrows(RuntimeException.class, () -> onz.markReadyProcedure(p2again),
				"slot occupied by marker must reject re-registration");
	}

	/**
	 * 参与方ready已发、决策已到（sendReadyAndWait受控阻塞后返回）而本地finalCommit被
	 * 停机拒绝：必须触发分歧标记（修复前该分支无任何动作），并保持finalRollback+Closed。
	 */
	@Test
	public void test02PerformRejectMarksOnzDivergence() throws Exception {
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var marked = new AtomicInteger();
		final int[] markedTid = {-1};

		var funcArgument = new BFuncProcedure.Data();
		funcArgument.setOnzTid(2001L);
		funcArgument.setFlushMode(AbstractOnz.eFlushAsync);
		funcArgument.setFlushTimeout(60_000);
		var stub = new OnzProcedureStub<EmptyBean, EmptyBean>(
				onz, "Fnd876.PerformReject", (p, a, r) -> 0L, EmptyBean.class, EmptyBean.class);

		final class ControlledOnz extends OnzProcedure {
			ControlledOnz() {
				super(null, funcArgument, stub, new EmptyBean(), new EmptyBean());
			}

			@Override
			public void sendReadyAndWait() {
				// 模拟"ready已发、Commit决策已送达"：阻塞期间stop()完全结束（终检点已过）。
				entered.countDown();
				try {
					release.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

			@Override
			public void markRolledBackAfterReady() {
				marked.incrementAndGet(); // 记录catch分支确实触发了分歧标记
				markedTid[0] = (int)getOnzTid();
				super.markRolledBackAfterReady();
			}
		}

		var commits = new AtomicInteger();
		var rollbacks = new AtomicInteger();
		var result = new AtomicLong(Long.MIN_VALUE);
		var error = new AtomicReference<Throwable>();
		var txnThread = Thread.ofPlatform().daemon().start(() -> {
			try {
				var onzP = new ControlledOnz();
				result.set(app.newProcedure((FuncLong)() -> {
					Transaction.getCurrent().setOnzProcedure(onzP);
					Transaction.getCurrent().runWhileCommit(commits::incrementAndGet);
					Transaction.getCurrent().runWhileRollback(rollbacks::incrementAndGet);
					return Procedure.Success;
				}, "Fnd876.OnzReject").call());
			} catch (Throwable e) {
				error.set(e);
			}
		});

		assertTrue(entered.await(10, TimeUnit.SECONDS), "事务必须到达sendReadyAndWait（ready已发）");
		app.stop(); // 终检点置null完全结束
		release.countDown(); // 决策已到，sendReadyAndWait返回 → finalCommit被拒
		txnThread.join(10_000);
		assertFalse(txnThread.isAlive(), "事务线程必须结束");
		assertEquals(Procedure.Closed, result.get(), "停机拒绝仍按FND7-54语义显式失败");
		assertEquals(1, marked.get(), "修复点：RejectWhileStopping分支必须对onz参与方标记分歧（修复前无任何动作）");
		assertEquals(2001, markedTid[0], "标记必须携带正确的onz tid");
		assertEquals(0, commits.get(), "未真正提交，whileCommit不得触发");
		assertEquals(1, rollbacks.get(), "终局回滚回调必须触发");
	}
}
