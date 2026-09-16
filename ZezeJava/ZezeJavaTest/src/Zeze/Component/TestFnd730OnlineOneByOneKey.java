package Zeze.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Transaction.Procedure;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-30回归：oneByOneKey是SimpleTimerSpec/CronTimerSpec的公开契约参数（随bean持久化），
 * 全局族Timer.scheduleSimple尊重（fire包executeOneByOne），online族安装路径原先从不读取
 * ——共用key的online定时器到期并发跑回调，用户按串行假设写的回调产生竞态。
 * 修复：online族安装（首发+周期重装）非空oneByOneKey时经Timer.dispatchFire包executeOneByOne。
 * <p>
 * 时序受控复现：两个共用key的定时器同刻到期，A回调阻塞占住串行队列，断言B不得进入回调
 * （修复前B并发直跑）；释放A后B执行。未设key的online定时器行为不变作护栏。
 */
@Fast
public class TestFnd730OnlineOneByOneKey {

	/** A的回调：进入即发信号，然后阻塞占住oneByOne队列。 */
	public static class BlockingHandle implements TimerHandle {
		static volatile @NotNull CountDownLatch entered = new CountDownLatch(1);
		static volatile @NotNull CountDownLatch release = new CountDownLatch(1);

		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			entered.countDown();
			release.await();
		}
	}

	/** B的回调：仅计数（times(1)语义下至多一次）。 */
	public static class CountingHandle implements TimerHandle {
		static final AtomicInteger RUNS = new AtomicInteger();

		@Override
		public void onTimer(@NotNull TimerContext context) {
			RUNS.incrementAndGet();
		}
	}

	@Test
	public void testSharedKeySerializesOnlineTimers() throws Exception {
		try (var env = new TestFnd729CrossFamilyCancel.TestEnv("TestFnd730OnlineOneByOneKey1")) {
			var stub = new TestFnd729CrossFamilyCancel.StubOnlineTimers(env.timer);
			BlockingHandle.entered = new CountDownLatch(1);
			BlockingHandle.release = new CountDownLatch(1);
			CountingHandle.RUNS.set(0);

			// 两个online定时器共用oneByOneKey；A(200ms)先到期进入回调并阻塞占住串行队列，
			// B(700ms)后到期——串行契约下B必须排在A之后，A阻塞期间B不得执行
			var specA = (SimpleTimerSpec)TimerSpec.ofDelay(200).oneByOneKey("fnd730_key");
			var specB = (SimpleTimerSpec)TimerSpec.ofDelay(700).oneByOneKey("fnd730_key");
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				stub.scheduleOnline(false, "u1", "@fnd730a", specA.build(), BlockingHandle.class, null, false);
				stub.scheduleOnline(false, "u1", "@fnd730b", specB.build(), CountingHandle.class, null, false);
				return Procedure.Success;
			}, "FND7_30.scheduleOnline").call());

			// A触发并进入回调（占住串行队列）
			Assertions.assertTrue(BlockingHandle.entered.await(10, TimeUnit.SECONDS), "timerA必须触发");
			// 越过B的到期点（700ms）后再断言：A阻塞期间B不得进入回调
			//（修复前B到点并发直跑即计1）
			Thread.sleep(1000);
			Assertions.assertEquals(0, CountingHandle.RUNS.get(),
					"共用oneByOneKey的online定时器回调必须串行（FND7-30：online族静默丢弃oneByOneKey）");

			BlockingHandle.release.countDown();
			var deadline = System.currentTimeMillis() + 10_000;
			while (CountingHandle.RUNS.get() == 0 && System.currentTimeMillis() < deadline)
				Thread.sleep(20);
			Assertions.assertEquals(1, CountingHandle.RUNS.get(), "释放A后B必须按串行队列执行");
		}
	}

	/** 护栏：未设oneByOneKey的online定时器照常触发（不进串行队列、不改变行为）。 */
	@Test
	public void testNoKeyStillFires() throws Exception {
		try (var env = new TestFnd729CrossFamilyCancel.TestEnv("TestFnd730OnlineOneByOneKey2")) {
			var stub = new TestFnd729CrossFamilyCancel.StubOnlineTimers(env.timer);
			CountingHandle.RUNS.set(0);

			var spec = (SimpleTimerSpec)TimerSpec.ofDelay(50);
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				stub.scheduleOnline(false, "u1", "@fnd730c", spec.build(), CountingHandle.class, null, false);
				return Procedure.Success;
			}, "FND7_30.scheduleOnlineNoKey").call());

			Assertions.assertTrue(awaitRuns(), "无key的online定时器必须照常触发一次");
			Assertions.assertEquals(1, CountingHandle.RUNS.get());
		}
	}

	private static boolean awaitRuns() throws InterruptedException {
		var deadline = System.currentTimeMillis() + 10_000;
		while (CountingHandle.RUNS.get() == 0 && System.currentTimeMillis() < deadline)
			Thread.sleep(20);
		return CountingHandle.RUNS.get() > 0;
	}
}
