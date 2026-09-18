package Zeze.Onz;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import demo.Module1.BKuafu;
import demo.Module1.BKuafuResult;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Builtin.Onz.FuncSagaEnd;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * R3-C C复审回归：OnzSaga.businessLock 串行化契约——FuncSagaEnd(cancel) 在业务执行期间到达
 * 必须等业务完成后再决策。业务失败（本地已回滚，写从未发生）→条目在锁内清理→等待的
 * FuncSagaEnd 应答 eSagaNotFound，不得补偿（过补偿=反向分歧）；业务成功→补偿执行并清理。
 * <p>
 * 连带验证 unlock-before-remove 收窄（失败清理移入锁内）：等锁方醒来必然观察到条目已删。
 * 该间隙的原乱序（解锁后、remove 前 waiter 插队拿到条目并补偿已回滚业务）无法无钩子确定性
 * 复现，本用例锁定修复后的不变量：失败业务+并发cancel=永不过补偿。
 */
@Fast
public class TestR3cSagaBusinessLockSerialization extends AppBase {
	private static final AtomicInteger NextId = new AtomicInteger(7430);
	private static final String ProcName = "r3cCBusinessLockSaga";

	private Application zeze;
	private Onz onz;

	// 业务脚本：blockLatch 卡住业务模拟长事务；fail 为 true 时业务返回失败。
	final CountDownLatch businessEntered = new CountDownLatch(1);
	final CountDownLatch releaseBusiness = new CountDownLatch(1);
	volatile boolean failBusiness;
	final AtomicInteger cancelRuns = new AtomicInteger();

	@Override
	public Application getZeze() {
		return zeze;
	}

	@BeforeEach
	public void setUp() throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextId.incrementAndGet());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("r3c_c_test_" + conf.getServerId()); // Memory库，独立url=独立存储
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		zeze = new Application("TestR3cSagaBusinessLock" + conf.getServerId(), conf);
		zeze.initialize(this);
		zeze.start();
		onz = zeze.getOnz();
		onz.registerSaga(ProcName,
				(saga, argument, result) -> {
					businessEntered.countDown();
					//noinspection ResultOfMethodCallIgnored
					releaseBusiness.await();
					return failBusiness ? 1 : 0;
				},
				(saga, cancelArgument) -> {
					cancelRuns.incrementAndGet();
					return 0;
				}, BKuafu.class, BKuafuResult.class, EmptyBean.class);
	}

	@AfterEach
	public void tearDown() throws Exception {
		onz = null;
		if (zeze != null) {
			zeze.stop();
			zeze = null;
		}
	}

	@Test
	public void testCancelWaitsForFailingBusinessNoOverCompensation() throws Exception {
		var tid = ((long)NextId.get() << 32) | 0xCC1L;
		failBusiness = true;

		var bizThread = startBusiness(tid);
		Assertions.assertTrue(businessEntered.await(5, java.util.concurrent.TimeUnit.SECONDS),
				"业务必须进入执行（持有businessLock）");

		// cancel 在业务执行期间到达：必须阻塞等待（businessLock 互斥）。
		var endHolder = submitEndAsync(tid);
		Assertions.assertFalse(endHolder.done.await(300, java.util.concurrent.TimeUnit.MILLISECONDS),
				"FuncSagaEnd在业务执行期间必须等待businessLock，不得并发决策");

		// 释放业务（失败收场）：本地回滚+锁内清理条目；等待的cancel必须得到eSagaNotFound。
		releaseBusiness.countDown();
		Assertions.assertEquals(1L, (long)bizThread.get(5, java.util.concurrent.TimeUnit.SECONDS),
				"业务失败必须以rc=1从处理器返回");
		var endRc = endHolder.result.get(5, java.util.concurrent.TimeUnit.SECONDS);
		Assertions.assertEquals((long)AbstractOnz.eSagaNotFound, (long)Zeze.IModule.getErrorCode(endRc),
				"失败已回滚的业务不得被补偿：等锁的FuncSagaEnd必须观察到条目已清理（eSagaNotFound，解码组合值）");
		Assertions.assertEquals(0, cancelRuns.get(), "过补偿防线：已回滚业务的补偿函数不得执行");
	}

	@Test
	public void testCancelWaitsForSucceedingBusinessThenCompensates() throws Exception {
		var tid = ((long)NextId.get() << 32) | 0xCC2L;
		failBusiness = false;

		var bizThread = startBusiness(tid);
		Assertions.assertTrue(businessEntered.await(5, java.util.concurrent.TimeUnit.SECONDS),
				"业务必须进入执行（持有businessLock）");

		var endHolder = submitEndAsync(tid);
		Assertions.assertFalse(endHolder.done.await(300, java.util.concurrent.TimeUnit.MILLISECONDS),
				"FuncSagaEnd在业务执行期间必须等待businessLock");

		releaseBusiness.countDown();
		Assertions.assertEquals(0L, (long)bizThread.get(5, java.util.concurrent.TimeUnit.SECONDS),
				"业务成功rc必须为0");
		var endRc = endHolder.result.get(5, java.util.concurrent.TimeUnit.SECONDS);
		Assertions.assertEquals(0L, (long)endRc, "成功业务后的cancel必须真正补偿");
		Assertions.assertEquals(1, cancelRuns.get(), "补偿函数必须执行恰好一次");
	}

	// ///////////////////////////////////////////////////////////

	private java.util.concurrent.Future<Long> startBusiness(long tid) throws Exception {
		var argumentBean = new BKuafu();
		argumentBean.setAccount(1);
		argumentBean.setMoney(1);
		var bb = ByteBuffer.Allocate();
		argumentBean.encode(bb);
		var sagaRpc = new FuncSaga();
		sagaRpc.Argument.setOnzTid(tid);
		sagaRpc.Argument.setFuncName(ProcName);
		sagaRpc.Argument.setFuncArgument(new Zeze.Net.Binary(java.util.Arrays.copyOf(bb.Bytes, bb.WriteIndex)));
		var executor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
			var t = new Thread(r, "r3c-c-biz");
			t.setDaemon(true);
			return t;
		});
		return executor.submit(() -> onz.ProcessFuncSagaRequest(sagaRpc));
	}

	private static final class EndHolder {
		final java.util.concurrent.Future<Long> result;
		final CountDownLatch done;

		EndHolder(java.util.concurrent.Future<Long> result, CountDownLatch done) {
			this.result = result;
			this.done = done;
		}
	}

	private EndHolder submitEndAsync(long tid) {
		var end = new FuncSagaEnd();
		end.Argument.setOnzTid(tid);
		end.Argument.setCancel(true);
		var executor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
			var t = new Thread(r, "r3c-c-end");
			t.setDaemon(true);
			return t;
		});
		var done = new CountDownLatch(1);
		return new EndHolder(executor.submit(() -> {
			try {
				return onz.ProcessFuncSagaEndRequest(end);
			} finally {
				done.countDown();
			}
		}), done);
	}
}
