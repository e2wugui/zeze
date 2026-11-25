package Zeze.Transaction;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Application;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Services.GlobalCacheManagerConst;
import demo.Module1.Table3;
import harness.Fast;
import harness.FastServerIds;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 事务内调用 TableX.selectCopy 触发 GoBackZeze（RedoAndReleaseLock / Abort）时，
 * 事务状态已离开 Running，selectCopy 必须立即重抛给事务主循环，绝不能吞掉后本地重试；
 * 同时 GlobalAgent 在 Completed 状态（如 whileCommit 回调）不得调用 throwAbort 误触 IllegalStateException。
 */
@Fast
public class TestSelectCopyGoBackPropagation {
	@FunctionalInterface
	private interface AcquireHook {
		IGlobalAgent.AcquireResult acquire(Binary gkey, int state, boolean fresh, boolean noWait);
	}

	private static final class StubGlobalAgent implements IGlobalAgent {
		private final AcquireHook hook;

		StubGlobalAgent(AcquireHook hook) {
			this.hook = hook;
		}

		@Override
		public @Nullable AcquireResult acquire(Binary gkey, int state, boolean fresh, boolean noWait) {
			return hook.acquire(gkey, state, fresh, noWait);
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
		public void stop() {
		}
	}

	private static Application newApp(Table3 table) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(FastServerIds.TEST_SELECT_COPY_GO_BACK_PROPAGATION);
		conf.setCheckpointMode(CheckpointMode.Table);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("select_copy_goback_" + conf.getServerId());
		conf.getDatabaseConfMap().put("", dbConf);
		var app = new Application("TestSelectCopyGoBackPropagation", conf);
		app.addTable("", table);
		app.start();
		return app;
	}

	private static void setGlobalAgent(Application app, @Nullable IGlobalAgent agent) throws Exception {
		var f = Application.class.getDeclaredField("globalAgent");
		f.setAccessible(true);
		f.set(app, agent);
	}

	@Test
	public void testSelectCopyPropagatesRedoAndReleaseLockInActiveTransaction() throws Exception {
		var table = new Table3();
		var app = newApp(table);
		try {
			long targetKey = 10L;
			long otherKey = 20L;
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.getOrAdd(targetKey).setInt_1(10);
				table.getOrAdd(otherKey).setInt_1(20);
				return Procedure.Success;
			}, "seed").call());
			app.checkpointRun();

			// 失效 targetKey 缓存权限，确保 selectCopy -> load 走 globalAgent.acquire
			var targetRecord = Objects.requireNonNull(Objects.requireNonNull(table.getCache()).get(targetKey));
			targetRecord.setState(GlobalCacheManagerConst.StateInvalid);

			var targetGkey = table.encodeGlobalKey(targetKey);
			var targetAcquireAttempts = new AtomicInteger();
			setGlobalAgent(app, new StubGlobalAgent((gkey, state, fresh, noWait) -> {
				if (gkey.equals(targetGkey) && targetAcquireAttempts.incrementAndGet() == 1) {
					return new IGlobalAgent.AcquireResult(
							GlobalCacheManagerConst.AcquireShareDeadLockFound,
							GlobalCacheManagerConst.StateInvalid, null);
				}
				return IGlobalAgent.AcquireResult.getSuccessResult(state);
			}));

			var rounds = new AtomicInteger();
			var rc = app.newProcedure(() -> {
				rounds.incrementAndGet();
				var copy = table.selectCopy(targetKey);
				Assertions.assertNotNull(copy);
				Assertions.assertEquals(10, copy.getInt_1());
				// 若 selectCopy 吞掉 GoBackZeze 后继续返回，此时 txn.state == RedoAndReleaseLock，
				// 下面 table.get 会立即抛出 IllegalStateException("State Is Not Running")。
				var other = table.get(otherKey);
				Assertions.assertNotNull(other);
				Assertions.assertEquals(20, other.getInt_1());
				return Procedure.Success;
			}, "selectCopyRedo").call();

			Assertions.assertEquals(Procedure.Success, rc, "selectCopy 透传 RedoAndReleaseLock 后事务应干净重试成功");
			Assertions.assertEquals(2, rounds.get(), "必须由事务主循环触发第 2 轮重做，而不是在 selectCopy 内吞掉重试");
			Assertions.assertEquals(2, targetAcquireAttempts.get());
		} finally {
			setGlobalAgent(app, null);
			app.stop();
		}
	}

	@Test
	public void testSelectCopyPropagatesAbortInActiveTransaction() throws Exception {
		var table = new Table3();
		var app = newApp(table);
		try {
			long targetKey = 30L;
			long otherKey = 40L;
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.getOrAdd(targetKey).setInt_1(30);
				table.getOrAdd(otherKey).setInt_1(40);
				return Procedure.Success;
			}, "seedAbort").call());
			app.checkpointRun();

			var targetRecord = Objects.requireNonNull(Objects.requireNonNull(table.getCache()).get(targetKey));
			targetRecord.setState(GlobalCacheManagerConst.StateInvalid);

			var targetGkey = table.encodeGlobalKey(targetKey);
			var targetAcquireAttempts = new AtomicInteger();
			setGlobalAgent(app, new StubGlobalAgent((gkey, state, fresh, noWait) -> {
				if (gkey.equals(targetGkey) && targetAcquireAttempts.incrementAndGet() == 1) {
					Objects.requireNonNull(Transaction.getCurrent()).throwAbort("simulated abort", null);
				}
				return IGlobalAgent.AcquireResult.getSuccessResult(state);
			}));

			var rounds = new AtomicInteger();
			var rc = app.newProcedure(() -> {
				rounds.incrementAndGet();
				table.selectCopy(targetKey);
				table.get(otherKey);
				return Procedure.Success;
			}, "selectCopyAbort").call();

			Assertions.assertEquals(Procedure.AbortException, rc, "selectCopy 必须立即透传 Abort 给事务主循环");
			Assertions.assertEquals(1, rounds.get(), "Abort 不得重试");
			Assertions.assertEquals(1, targetAcquireAttempts.get(), "selectCopy 不得在吞掉 Abort 后本地重试 acquire");
		} finally {
			setGlobalAgent(app, null);
			app.stop();
		}
	}

	@Test
	public void testGlobalAgentInCompletedTransactionDoesNotThrowStateNotRunning() throws Exception {
		var table = new Table3();
		var app = newApp(table);
		try {
			var globalAgent = new GlobalAgent(app, new String[]{"127.0.0.1"}, 0);
			var agent = globalAgent.getAgents()[0];
			var releaserField = GlobalAgentBase.class.getDeclaredField("releaser");
			releaserField.setAccessible(true);

			var releasingErr = new AtomicReference<@Nullable Throwable>();
			var fastFailErr = new AtomicReference<@Nullable Throwable>();

			var rc = app.newProcedure(() -> {
				Transaction.whileCommit(() -> {
					var current = Transaction.getCurrent();
					Assertions.assertNotNull(current);
					Assertions.assertTrue(current.isCompleted(), "whileCommit 中事务应处于 Completed 状态");

					// 1. agent.isReleasing() 分支：Completed 事务必须直接抛出 GoBackZeze，而非调 throwAbort 抛 State Is Not Running
					try {
						var releaserCtor = GlobalAgentBase.Releaser.class
								.getDeclaredConstructor(Application.class, int.class, long.class);
						releaserCtor.setAccessible(true);
						releaserField.set(agent, releaserCtor.newInstance(app, 0, Long.MAX_VALUE));
						globalAgent.acquire(table.encodeGlobalKey(99L), GlobalCacheManagerConst.StateShare, false, false);
					} catch (Throwable t) {
						releasingErr.set(t);
					} finally {
						try {
							releaserField.set(agent, null);
						} catch (IllegalAccessException ignored) {
						}
					}

					// 2. Agent.verifyFastFail -> throwException：Completed 事务必须抛出业务 IllegalStateException，而非 Abort: State Is Not Running
					try {
						agent.setFastFail();
						agent.verifyFastFail();
					} catch (Throwable t) {
						fastFailErr.set(t);
					}
				});
				return Procedure.Success;
			}, "completedWhileCommit").call();

			Assertions.assertEquals(Procedure.Success, rc);
			Assertions.assertInstanceOf(GoBackZeze.class, releasingErr.get(),
					"Completed 事务遇到 releasing 应抛 GoBackZeze，实际=" + releasingErr.get());
			var ff = fastFailErr.get();
			Assertions.assertInstanceOf(IllegalStateException.class, ff);
			Assertions.assertEquals("GlobalAgent In FastErrorPeriod", Objects.requireNonNull(ff).getMessage());
		} finally {
			app.stop();
		}
	}
}
