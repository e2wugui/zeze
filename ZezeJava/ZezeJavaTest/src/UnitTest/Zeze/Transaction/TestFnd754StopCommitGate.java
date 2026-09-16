package UnitTest.Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FND7-54 回归：停机序列先杀Checkpoint后停组件且从不静默在途事务——终检点之后
 * 提交的事务修改只进内存（rrs不注册/flush跳过），perform仍返回Success，已应答
 * 的提交在关库后丢失。
 * 修复：(1)stop重排序——组件先停、checkpoint.stopAndJoin作终检点收尾并先置null；
 * (2)提交路径停机拒绝——perform重试轮次间与tryUpdateAndCheckpoint入口/落库点
 * 检查checkpoint==null时显式失败（Closed），不再静默丢弃。
 * 测试用受控CountDownLatch钉死时序：事务阻塞在perform入口之后的action内，
 * 等stop()完全结束后再放行，确定性复现"已过入口的在途事务"。
 */
@Fast
public class TestFnd754StopCommitGate {
	// 独立serverId+url：@Fast类并行时避免本地RocksCache目录互撞（对齐TestCheckpointRunThreadSentinel）。
	private static final int SERVER_ID = 7540;

	private Application app;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("fnd7_54_stop_gate_" + SERVER_ID);
		conf.getDatabaseConfMap().put("", dbConf);
		app = new Application("TestFnd754StopCommitGate", conf);
		app.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app.getStartState() != Application.StartState.eStopped)
			app.stop(); // 已停实例幂等
	}

	/**
	 * 已过perform入口的在途事务（阻塞在action内，无锁），stop()完全结束后放行：
	 * 提交点发现终检点已过，必须显式失败（Closed）+触发回滚回调，
	 * 不得返回Success并触发whileCommit（旧代码：修改静默丢弃仍假成功）。
	 */
	@Test
	public void testInFlightCommitRejectedAfterStop() throws Exception {
		var commits = new AtomicInteger();
		var rollbacks = new AtomicInteger();
		var inAction = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var result = new AtomicLong(Long.MIN_VALUE);
		var error = new AtomicReference<Throwable>();
		var txnThread = Thread.ofPlatform().daemon().start(() -> {
			try {
				result.set(app.newProcedure(() -> {
					Transaction.getCurrent().runWhileCommit(commits::incrementAndGet);
					Transaction.getCurrent().runWhileRollback(rollbacks::incrementAndGet);
					inAction.countDown();
					// 受控阻塞：事务已过perform入口，等stop()完全结束（终检点已过）再继续。
					release.await();
					return Procedure.Success;
				}, "Fnd754.BlockedCommit").call());
			} catch (Throwable e) {
				error.set(e);
			}
		});

		inAction.await(); // 事务已进入action（perform入口检查已通过）
		app.stop();       // 组件停止+终检点+checkpoint置null全部完成
		release.countDown();
		txnThread.join(10_000);
		assertNull(error.get(), "事务线程不得抛异常");
		assertFalse(txnThread.isAlive(), "事务线程必须结束");
		assertEquals(Procedure.Closed, result.get(),
				"终检点之后的提交必须显式失败（Closed），不得假成功后静默丢弃（FND7-54）");
		assertEquals(0, commits.get(), "未真正提交落库，whileCommit不得触发");
		assertEquals(1, rollbacks.get(), "显式失败的终局回滚回调必须触发");
	}

	/**
	 * redo重试环中的在途事务：轮次2受控阻塞等stop()结束，轮次3不得再执行——
	 * perform在重试轮次间检查停机状态，直接Closed退出（旧代码：继续重执行业务逻辑，
	 * 最终在提交点静默丢弃仍返回Success）。
	 */
	@Test
	public void testRedoLoopRejectedBetweenRoundsAfterStop() throws Exception {
		var calls = new AtomicInteger();
		var rollbacks = new AtomicInteger();
		var round2InAction = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var result = new AtomicLong(Long.MIN_VALUE);
		var error = new AtomicReference<Throwable>();
		var txnThread = Thread.ofPlatform().daemon().start(() -> {
			try {
				result.set(app.newProcedure(() -> {
					calls.incrementAndGet();
					Transaction.getCurrent().runWhileRollback(rollbacks::incrementAndGet);
					if (calls.get() == 1)
						Transaction.getCurrent().throwRedo(0, "Fnd754 round1 redo");
					if (calls.get() == 2) {
						// 受控阻塞：轮次2已确定进入action后才放行主线程去stop()，
						// 之后等stop()完全结束（终检点已过）再抛redo进入轮次3。
						round2InAction.countDown();
						release.await();
						Transaction.getCurrent().throwRedo(0, "Fnd754 round2 redo after stop");
					}
					return Procedure.Success; // 轮次3不可达：轮次间停机检查必须拦截
				}, "Fnd754.RedoWhileStop").call());
			} catch (Throwable e) {
				error.set(e);
			}
		});

		round2InAction.await(); // 确保轮次1已抛redo、轮次2已进入action阻塞中
		app.stop();
		release.countDown();
		txnThread.join(10_000);
		assertNull(error.get(), "事务线程不得抛异常");
		assertFalse(txnThread.isAlive(), "事务线程必须结束");
		assertEquals(Procedure.Closed, result.get(), "redo环中的在途事务在停机后必须显式失败（FND7-54）");
		assertEquals(2, calls.get(), "轮次3（stop后的重执行）不得发生");
		assertEquals(1, rollbacks.get(), "终局回滚回调必须触发一次");
	}

	/** 正常路径不受影响：运行中的事务照常Success并触发whileCommit。 */
	@Test
	public void testNormalCommitUnaffected() throws Exception {
		var commits = new AtomicInteger();
		var rc = app.newProcedure(() -> {
			Transaction.getCurrent().runWhileCommit(commits::incrementAndGet);
			return Procedure.Success;
		}, "Fnd754.NormalCommit").call();
		assertEquals(Procedure.Success, rc, "运行期正常提交必须成功");
		assertEquals(1, commits.get());
	}

	/** 停机后新事务（直接构造Procedure绕过newProcedure的isStart检查）在perform入口被拒。 */
	@Test
	public void testEntryRejectedAfterStop() throws Exception {
		var calls = new AtomicInteger();
		app.stop();
		var rc = new Procedure(app, (Zeze.Util.FuncLong)() -> {
			calls.incrementAndGet();
			return Procedure.Success;
		}, "Fnd754.AfterStop", null).call();
		assertEquals(Procedure.Closed, rc, "终检点已过，perform入口必须拒绝");
		assertEquals(0, calls.get(), "action不得执行");
	}
}
