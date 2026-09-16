package UnitTest.Zeze.Util;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Transaction.DispatchMode;
import Zeze.Util.TaskOneByOneByKey2;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-42 回归：TaskOneByOneByKey2 桶的 submitted 认领（VarHandle CAS false→true）
 * 在派发 execute 抛 RuntimeException（自定义池拒绝 REE / 全局池停机 ISE）时无人回滚——
 * submitted 的唯一复位点在 run()/runNext 消费的 pollTask/peekTask 里，而 run() 没进过池：
 * 后续 submit 的 CAS 恒失败也不再派发，该桶（及其映射的所有 key）永久卡死。
 * 姊妹实现 TaskOneByOneQueue 已修（FND5-13/FND6-07），Key2 补齐：executeOrRollback
 * 回滚认领（复位 submitted、保留积压任务）、按原类型重抛、warn 可诊断。
 * 三个派发点各测一条：submit 直派、run() 内 mode 切换派发、barrier 完成回调派发。
 */
@Fast
public class TestFnd742TaskOneByOneByKey2RejectRecover {

	/** 每次派发新起线程并捕获 run() 逃逸的异常（生产中由线程池吞掉），主线程得以等待与断言。 */
	private static final class ThreadPerTaskExecutor implements java.util.concurrent.Executor {
		final AtomicBoolean rejecting = new AtomicBoolean();
		final AtomicReference<RuntimeException> caught = new AtomicReference<>();

		@Override
		public void execute(Runnable r) {
			if (rejecting.get())
				throw new RejectedExecutionException("poison");
			var t = new Thread(() -> {
				try {
					r.run();
				} catch (RuntimeException e) {
					caught.compareAndSet(null, e);
				}
			}, "fnd742-worker");
			t.setDaemon(true);
			t.start();
		}

		boolean awaitCaught(long ms) throws InterruptedException {
			var deadline = System.currentTimeMillis() + ms;
			while (caught.get() == null && System.currentTimeMillis() < deadline)
				Thread.sleep(10);
			return caught.get() != null;
		}
	}

	/** 派发点1（submit 认领后 runNext 直派）：拒绝必须原样抛回调用方，
	 * 且认领回滚后同 key 后续 submit 必须能重新认领派发（修复前 submitted 楔死恒 false CAS）。 */
	@Test
	public void testSubmitRejectThenRecover() throws Exception {
		var executor = new ThreadPerTaskExecutor();
		var queue = new TaskOneByOneByKey2(executor);

		executor.rejecting.set(true);
		Assertions.assertThrows(RejectedExecutionException.class,
				() -> TaskSpec.ofAction(() -> { }).name("t1").executeOneByOne("k1", queue),
				"注入拒绝必须按原类型抛回调用方");

		executor.rejecting.set(false);
		var t2Ran = new CountDownLatch(1);
		TaskSpec.ofAction(t2Ran::countDown).name("t2").executeOneByOne("k1", queue);
		Assertions.assertTrue(t2Ran.await(5, TimeUnit.SECONDS),
				"拒绝后桶不得永久卡死，同key后续submit必须能重新认领派发（FND7-42）");
	}

	/** 派发点2（run() 内 mode 切换派发）：t1 执行中入队不同 mode 的 t2 并开启拒绝，
	 * 驱动线程的 mode 切换派发抛 REE 逃出 run()——回滚后 t3 提交必须恢复驱动，
	 * 积压的 t2 与 t3 照常执行（修复前 submitted 楔死，t2/t3 永不运行）。 */
	@Test
	public void testRunModeChangeRejectThenRecover() throws Exception {
		var executor = new ThreadPerTaskExecutor();
		var queue = new TaskOneByOneByKey2(executor);
		var t2Ran = new CountDownLatch(1);
		var t3Ran = new CountDownLatch(1);

		// t1：执行中开启拒绝并入队 Critical 的 t2（驱动占着认领，submit 只入队），
		// t1 返回后驱动循环 peek 到 mode 不同的 t2，切换派发即被拒。
		TaskSpec.ofAction(() -> {
			executor.rejecting.set(true);
			TaskSpec.ofAction(t2Ran::countDown).name("t2")
					.dispatchMode(DispatchMode.Critical).executeOneByOne("k2", queue);
		}).name("t1").executeOneByOne("k2", queue);

		Assertions.assertTrue(executor.awaitCaught(5000), "mode切换派发失败必须逃出run()可见");
		Assertions.assertFalse(t2Ran.getCount() == 0, "派发被拒时t2不得已执行");

		executor.rejecting.set(false);
		TaskSpec.ofAction(t3Ran::countDown).name("t3").executeOneByOne("k2", queue);
		Assertions.assertTrue(t2Ran.await(5, TimeUnit.SECONDS), "回滚后积压的t2必须照常执行");
		Assertions.assertTrue(t3Ran.await(5, TimeUnit.SECONDS), "回滚后新提交的t3必须照常执行");
	}

	/** 派发点3（barrier 完成回调 reachedRunNext 派发）：单桶两参与者屏障，action 内
	 * 开启拒绝并入队 t2；reach 的 finally 派发 t2 被拒——REE 从 finally 逃逸
	 * （生产中被 executor 吞掉、无诊断）。回滚后 t3 提交恢复驱动，积压 t2/t3 执行。 */
	@Test
	public void testBarrierDispatchRejectThenRecover() throws Exception {
		var executor = new ThreadPerTaskExecutor();
		var queue = new TaskOneByOneByKey2(executor);
		var t2Ran = new CountDownLatch(1);
		var t3Ran = new CountDownLatch(1);

		queue.executeCyclicBarrier(List.of("kb", "kb"), "fnd742-barrier", () -> {
			// barrier action 运行在桶驱动线程上：此时入队的 t2 排在屏障任务之后，
			// reach 收尾的 reachedRunNext 将派发它——先开启拒绝制造派发失败。
			executor.rejecting.set(true);
			TaskSpec.ofAction(t2Ran::countDown).name("t2").executeOneByOne("kb", queue);
		}, null);

		Assertions.assertTrue(executor.awaitCaught(5000), "barrier收尾派发失败必须可见（不得静默楔死）");
		Assertions.assertFalse(t2Ran.getCount() == 0, "派发被拒时t2不得已执行");

		executor.rejecting.set(false);
		TaskSpec.ofAction(t3Ran::countDown).name("t3").executeOneByOne("kb", queue);
		Assertions.assertTrue(t2Ran.await(5, TimeUnit.SECONDS), "回滚后积压的t2必须照常执行");
		Assertions.assertTrue(t3Ran.await(5, TimeUnit.SECONDS), "回滚后新提交的t3必须照常执行");
	}
}
