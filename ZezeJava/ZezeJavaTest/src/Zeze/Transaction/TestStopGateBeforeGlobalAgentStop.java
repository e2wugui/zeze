package Zeze.Transaction;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Application;
import Zeze.Config;
import Zeze.Net.Binary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 停机关闭提交闸门必须先于globalAgent.stop的权限撤销。
 * <p>
 * NormalClose会让GCM立即释放本server全部记录锁，他进程随后可Acquire并从后台库读旧值
 * 改写提交。若闸门在撤权后才关闭，撤权后到关闸前的在途事务仍能取得提交使用权并登记
 * 脏数据，其后的终检点flush发生在权限释放之后——旧脏值覆盖他进程新值（跨进程丢更新）。
 * 修复前顺序：checkpointRun冲刷→globalAgent.stop撤权→checkpoint卸载关闸；修复后：
 * checkpointRun冲刷→checkpoint卸载关闸（排空在途提交+终检点flush，全程权限仍有效）
 * →globalAgent.stop撤权。
 * <p>
 * 用可控IGlobalAgent替身钉死时序：在途事务阻塞在action内，替身stop进入时放行并等待
 * 其提交收尾——断言此刻闸门已关（checkpoint已卸载），事务得到Closed，而非在撤权过程
 * 中成功提交（修复前：checkpoint仍在，事务在globalAgent.stop内返回Success并触发
 * whileCommit）。仅验证本地停机窗口顺序；跨节点丢更新后果是源码推演（独立内存库，
 * 无真实GCM/第二应用）。
 */
@Fast
public class TestStopGateBeforeGlobalAgentStop {
	// 独立serverId+url：@Fast类并行时避免本地RocksCache目录互撞。
	private static final int SERVER_ID = FastServerIds.TEST_STOP_GATE_BEFORE_GLOBAL_AGENT;

	private Application app;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("stop_gate_order_" + SERVER_ID);
		conf.getDatabaseConfMap().put("", dbConf);
		app = new Application("TestStopGateBeforeGlobalAgentStop", conf);
		app.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app.getStartState() != Application.StartState.eStopped)
			app.stop(); // 已停实例幂等
	}

	@Test
	public void testCommitGateClosedBeforeGlobalAgentStop() throws Exception {
		var commits = new AtomicInteger();
		var rollbacks = new AtomicInteger();
		var inAction = new CountDownLatch(1);
		var gateRelease = new CountDownLatch(1);
		var result = new AtomicLong(Long.MIN_VALUE);
		var error = new AtomicReference<Throwable>();

		var worker = new Thread(() -> {
			try {
				result.set(app.newProcedure(() -> {
					Transaction.getCurrent().runWhileCommit(commits::incrementAndGet);
					Transaction.getCurrent().runWhileRollback(rollbacks::incrementAndGet);
					inAction.countDown();
					// 受控阻塞：等globalAgent.stop进入后才继续走到提交点。
					gateRelease.await();
					return Procedure.Success;
				}, "StopGateOrder.BlockedCommit").call());
			} catch (Throwable e) {
				error.set(e);
			}
		}, "stop-gate-order-txn");
		worker.setDaemon(true);
		worker.start();
		assertTrue(inAction.await(10, TimeUnit.SECONDS), "事务必须先进入action（perform入口检查已通过）");

		// 可控替身：stop进入时记录闸门状态、放行在途事务并等其提交收尾。
		var checkpointNullAtGlobalStop = new AtomicBoolean(true);
		var fake = new IGlobalAgent() {
			@Override
			public AcquireResult acquire(Binary gkey, int state, boolean fresh, boolean noWait) {
				return AcquireResult.getSuccessResult(state);
			}

			@Override
			public int getGlobalCacheManagerHashIndex(Binary gkey) {
				return 0;
			}

			@Override
			public GlobalAgentBase getAgent(int index) {
				throw new UnsupportedOperationException();
			}

			@Override
			public int getAgentCount() {
				return 0;
			}

			@Override
			public void stop() throws Exception {
				checkpointNullAtGlobalStop.set(app.getCheckpoint() == null);
				gateRelease.countDown();
				worker.join(10_000);
			}
		};
		var gaField = Application.class.getDeclaredField("globalAgent");
		gaField.setAccessible(true);
		gaField.set(app, fake);

		app.stop(); // 阻塞至替身stop返回（含在途事务收尾）后继续完成停机

		assertFalse(worker.isAlive(), "事务线程必须已在globalAgent.stop内收尾");
		assertNull(error.get(), "事务线程不得抛异常");
		assertTrue(checkpointNullAtGlobalStop.get(), "globalAgent.stop进入时提交闸门必须已关闭（checkpoint已卸载）");
		assertEquals(Procedure.Closed, result.get(), "撤权过程中的在途提交必须被拒（Closed），不得成功提交");
		assertEquals(0, commits.get(), "未真正提交，whileCommit不得触发");
		assertEquals(1, rollbacks.get(), "显式失败的终局回滚回调必须触发");
	}
}
