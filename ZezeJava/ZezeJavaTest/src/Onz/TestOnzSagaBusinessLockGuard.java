package Onz;

import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Onz.AbstractOnz;
import Zeze.Onz.OnzServer;
import Zeze.Serialize.ByteBuffer;
import demo.App;
import demo.Module1.BKuafu;
import demo.Module1.BKuafuResult;
import harness.TestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * OH1-F2/OH1-F3 回归：
 * <b>OH1-F3（P2，红测）</b>：cleanupTimeoutSagas原先只看isEnd+年龄即remove，不区分在途
 * 业务——耗时超过sagaContextTimeoutMs的合法业务被清掉上下文后，阻塞等锁的FuncSagaEnd
 * 醒来remove失败应答eSagaNotFound，协调者按"无补偿对象"忽略——补偿永久丢失。
 * 修复：清理前tryLock businessLock（拿不到=业务在途，跳过本轮），拿到后复查isEnd再两参remove。
 * <b>OH1-F2（P2，功能护栏）</b>：FuncSaga注册移入businessLock之内——注册与拿锁间的
 * 停滞窗口内并发FuncSagaEnd(cancel)可抢先补偿未执行的业务。竞态窗口需派发线程停滞，
 * 不可确定性红测；护栏钉住修复后的不变量：业务在途时到达的cancel必然串行在业务完成
 * 之后执行（补偿恰一次、且补偿观察到业务已完成）。
 * 场景构造：手动以协调者身份发送FuncSaga/FuncSagaEnd（不走perform），业务用闩阻塞。
 */
public class TestOnzSagaBusinessLockGuard {
	// 过程名必须全JVM唯一：demo.App单例的Onz注册表跨测试类持久。
	private static final java.util.concurrent.atomic.AtomicBoolean registeredOnAppInstance =
			new java.util.concurrent.atomic.AtomicBoolean();
	private static final String SlowSagaName = "oh1f3SlowSaga";

	private static final long SlowTid = 0x5CA1BEEF00000201L;
	private static final long CancelTid = 0x5CA1BEEF00000202L;

	// 业务/补偿跨线程记账
	static volatile CountDownLatchLike BusinessStarted = new CountDownLatchLike();
	static volatile CountDownLatchLike BusinessRelease = new CountDownLatchLike();
	static volatile boolean BusinessDone; // 业务return前置位（补偿必须观察到它）
	static volatile int CancelCount;

	static final class CountDownLatchLike {
		private java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

		void countDown() {
			latch.countDown();
		}

		void awaitQuiet() {
			try {
				latch.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		boolean await(long ms) throws InterruptedException {
			return latch.await(ms, java.util.concurrent.TimeUnit.MILLISECONDS);
		}
	}

	private final App zeze2 = new App();
	private OnzServer onzServer;

	@BeforeEach
	public void before() throws Exception {
		Assumptions.assumeTrue(TestEnv.portReachable("127.0.0.1", 5011) && TestEnv.portReachable("127.0.0.1", 5012),
				"第二对服务(5011/5012)不可用：zeze.test.env=off 时 TestEnvLauncherListener 不在进程内自动启动");

		// 每方法重置闩与记账（静态跨方法残留）
		BusinessStarted = new CountDownLatchLike();
		BusinessRelease = new CountDownLatchLike();
		BusinessDone = false;
		CancelCount = 0;

		App.Instance.Start();
		var config2 = Config.load("./zeze_cluster_2.xml");
		zeze2.Start(config2);

		if (registeredOnAppInstance.compareAndSet(false, true))
			App.Instance.Zeze.getOnz().registerSaga(SlowSagaName,
					TestOnzSagaBusinessLockGuard::slowBusiness, TestOnzSagaBusinessLockGuard::sagaCancel,
					BKuafu.class, BKuafuResult.class, Zeze.Transaction.EmptyBean.class);

		var myConfig = Config.load("zeze.xml");
		// 同serverId的CommitOnzServer rocks目录被多个Onz测试类共用，清理避免跨类残留被redoTimer处理
		deleteRecursively(java.nio.file.Path.of("CommitOnzServer" + myConfig.getServerId()));
		onzServer = new OnzServer("zeze1=zeze.xml;zeze2=zeze_cluster_2.xml", myConfig);
		onzServer.start();
	}

	@AfterEach
	public void after() throws Exception {
		BusinessRelease.countDown(); // 断言失败也放行阻塞的业务
		if (onzServer != null)
			onzServer.stop();
		zeze2.Stop();
	}

	/** 阻塞业务：置started后挂起等release，return前置BusinessDone。 */
	private static long slowBusiness(Zeze.Onz.OnzSaga saga, BKuafu argument, BKuafuResult result) {
		BusinessStarted.countDown();
		BusinessRelease.awaitQuiet();
		BusinessDone = true;
		result.setMoney(argument.getMoney());
		return 0;
	}

	private static long sagaCancel(Zeze.Onz.OnzSaga saga, Zeze.Transaction.EmptyBean cancelArgument) {
		CancelCount++;
		return 0;
	}

	/**
	 * OH1-F3（红测）：业务在途时到达的超时清理不得删除上下文。
	 * 修复前：cleanup直接remove——后续（真实或redo路径的）FuncSagaEnd只得eSagaNotFound，
	 * 补偿永久丢失；本测试用cancel型FuncSagaEnd占住锁等待以放大该窗口。
	 */
	@Test
	@Timeout(120)
	public void testCleanupSkipsInFlightBusiness() throws Exception {
		waitOnzReady();

		// 参与方注册上下文并开始执行阻塞业务
		sendFuncSaga(SlowTid);
		Assertions.assertTrue(BusinessStarted.await(30_000), "业务必须开始执行");

		// cancel型FuncSagaEnd在业务执行期间到达：处理线程阻塞在businessLock上（FND7-34设计）
		var cancelFuture = sendFuncSagaEnd(SlowTid, true);

		// 超时条件成立（构造时刻计时早已超龄），业务仍在执行：清理必须跳过
		App.Instance.Zeze.getOnz().setSagaContextTimeoutMs(1);
		App.Instance.Zeze.getOnz().cleanupTimeoutSagas();

		// 核心（红断言）：在途业务的上下文不得被清理删除
		Assertions.assertEquals(1, sagaCount(App.Instance.Zeze.getOnz()),
				"业务在途时cleanupTimeoutSagas不得删除上下文（修复前remove后FuncSagaEnd只得eSagaNotFound，补偿永久丢失）");

		// 放行业务：cancel串行在业务完成后执行，补偿恰一次
		BusinessRelease.countDown();
		waitUntil(() -> CancelCount >= 1 && sagaCount(App.Instance.Zeze.getOnz()) == 0, 30_000,
				"业务完成后等待中的cancel必须执行补偿并清理上下文");
		Assertions.assertTrue(BusinessDone, "补偿必须发生在业务完成之后（businessLock串行）");
		awaitFutureQuietly(cancelFuture); // 应答收尾，不检查结果码
	}

	/**
	 * OH1-F2（功能护栏）：注册在businessLock之内后，业务在途期间到达的FuncSagaEnd(cancel)
	 * 与业务的顺序保持串行：补偿恰一次且观察到业务已完成（无过补偿、无补偿丢失）。
	 */
	@Test
	@Timeout(120)
	public void testCancelDuringBusinessSerializedByLock() throws Exception {
		waitOnzReady();

		sendFuncSaga(CancelTid);
		Assertions.assertTrue(BusinessStarted.await(30_000), "业务必须开始执行（注册先于业务完成）");

		var cancelFuture = sendFuncSagaEnd(CancelTid, true);
		// 等锁窗口：给FuncSagaEnd线程时间到达lockBusiness（无法直接观察，短暂让步）
		//noinspection BusyWait
		for (int i = 0; i < 20 && CancelCount == 0; i++)
			Thread.sleep(50);

		BusinessRelease.countDown(); // 业务完成（上下文保留，end未置）

		waitUntil(() -> CancelCount >= 1 && sagaCount(App.Instance.Zeze.getOnz()) == 0, 30_000,
				"业务完成后的cancel必须完成补偿并清理上下文");
		Assertions.assertEquals(1, CancelCount, "补偿恰一次");
		Assertions.assertTrue(BusinessDone, "补偿必须观察到业务已完成（串行不变量）");
		awaitFutureQuietly(cancelFuture);
	}

	/** 手动发送FuncSaga（不await应答：应答要等业务+flush，这里只需上下文注册、业务开始）。 */
	private void sendFuncSaga(long tid) throws Exception {
		var arg = new BKuafu.Data();
		arg.setAccount(400);
		arg.setMoney(40);
		var bb = ByteBuffer.Allocate();
		arg.encode(bb);
		var r = new Zeze.Builtin.Onz.FuncSaga();
		r.Argument.setOnzTid(tid);
		r.Argument.setFuncName(SlowSagaName);
		r.Argument.setFuncArgument(new Binary(bb.Bytes, 0, bb.WriteIndex));
		r.Argument.setFlushMode(AbstractOnz.eFlushImmediately);
		r.SendForWait(onzServer.getZezeInstance("zeze1"));
	}

	/** 手动发送FuncSagaEnd，返回future供收尾等待。 */
	private Zeze.Util.TaskCompletionSource<?> sendFuncSagaEnd(long tid, boolean cancel) throws Exception {
		var r = new Zeze.Builtin.Onz.FuncSagaEnd();
		r.Argument.setOnzTid(tid);
		r.Argument.setCancel(cancel);
		return r.SendForWait(onzServer.getZezeInstance("zeze1"), 60_000);
	}

	/** 应答收尾等待：结果码与异常均不关心（断言已由CancelCount/sagaCount覆盖）。 */
	private static void awaitFutureQuietly(Zeze.Util.TaskCompletionSource<?> future) {
		try {
			future.get(30_000, java.util.concurrent.TimeUnit.MILLISECONDS);
		} catch (Exception ignored) {
		}
	}

	private static int sagaCount(Zeze.Onz.Onz onz) throws Exception {
		var field = Zeze.Onz.Onz.class.getDeclaredField("sagas");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		var map = (Zeze.Util.LongConcurrentHashMap<Object>)field.get(onz);
		return map.size();
	}

	private interface Condition {
		boolean test() throws Exception;
	}

	private static void waitUntil(Condition condition, long timeoutMs, String message) throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (!condition.test()) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, message);
			//noinspection BusyWait
			Thread.sleep(50);
		}
	}

	private void waitOnzReady() throws InterruptedException {
		var deadline = System.currentTimeMillis() + 60_000;
		for (;;) {
			try {
				onzServer.getZezeInstance("zeze1");
				onzServer.getZezeInstance("zeze2");
				return;
			} catch (RuntimeException e) {
				if (System.currentTimeMillis() > deadline)
					throw e;
				Thread.sleep(100);
			}
		}
	}

	private static void deleteRecursively(java.nio.file.Path root) throws Exception {
		if (!java.nio.file.Files.exists(root))
			return;
		try (var walk = java.nio.file.Files.walk(root)) {
			walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
				try {
					java.nio.file.Files.delete(p);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}
}
