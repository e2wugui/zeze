package UnitTest.Zeze.Util;

import harness.Fast;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import Zeze.Util.TaskOneByOneQueue;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * TaskOneByOneQueue.shutdown(true) 补偿边界测试。
 * <p>
 * FND-U0-2：批量任务执行期间仍留在 queue 里（只有 runNext 按 processedCount 出队），
 * shutdown(true) 的 pollFirst 取走的是队头早已完成的任务而非正在执行的任务，
 * 对 oldQueue 逐个 onCancel 时把已完成/在飞任务误补偿（教程承诺"未运行的丢弃、正在运行的保留"）。
 * 修复后：保留整个在飞批量认领区，未认领任务立即补偿，认领区内未执行部分由池线程收尾补偿。
 */
@Fast
public class TestTaskOneByOneQueueShutdown {

	static final class RecordingTask extends TaskOneByOneQueue.Task {
		private final Runnable onProcess;

		RecordingTask(String name, Runnable onProcess, Runnable onCancel) {
			super(name, onCancel == null ? null : onCancel::run, null);
			this.onProcess = onProcess;
		}

		@Override
		public boolean isBarrier() {
			return false;
		}

		@Override
		public boolean process(@NotNull TaskOneByOneQueue.BatchTask batch) {
			onProcess.run();
			return true;
		}
	}

	private static void runLatch(@NotNull CountDownLatch latch) {
		try {
			Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * mid-batch shutdown(true)：第二轮批量 [t1..t4] 中 t1 已完成、t2 正在执行（hang 住）时 shutdown。
	 * 已完成和在飞的任务（t0..t2）绝不能触发 onCancel（补偿会造成重复扣款类二次处理）；
	 * 未运行的 t3、t4 必须补偿。修复前 t1、t2 会被误 onCancel，断言失败。
	 */
	@Test
	public void testShutdownCancelMidBatch() throws InterruptedException {
		var pool = Executors.newSingleThreadExecutor();
		try {
			var q = new TaskOneByOneQueue(pool);
			var processed = ConcurrentHashMap.<String>newKeySet();
			var cancelled = ConcurrentHashMap.<String>newKeySet();
			var t2Started = new CountDownLatch(1);
			var releaseT2 = new CountDownLatch(1);
			// 模拟 TaskOneByOneBase.executeAndUnlock：submit 在 queue 锁内，首个任务触发调度。
			q.lock();
			try {
				var dispatch = q.submit(new RecordingTask("t0", () -> processed.add("t0"), () -> cancelled.add("t0")));
				q.submit(new RecordingTask("t1", () -> processed.add("t1"), () -> cancelled.add("t1")));
				q.submit(new RecordingTask("t2", () -> {
					processed.add("t2");
					t2Started.countDown();
					runLatch(releaseT2);
				}, () -> cancelled.add("t2")));
				q.submit(new RecordingTask("t3", () -> processed.add("t3"), () -> cancelled.add("t3")));
				q.submit(new RecordingTask("t4", () -> processed.add("t4"), () -> cancelled.add("t4")));
				if (dispatch != null)
					dispatch.run(); // 第一轮批量只含 t0；t0 完成后 runNext 认领 [t1..t4]
			} finally {
				q.unlock();
			}
			Assertions.assertTrue(t2Started.await(10, TimeUnit.SECONDS));
			q.shutdown(true); // t1 已完成、t2 在飞
			releaseT2.countDown(); // 在飞任务正常完成
			q.waitComplete();
			Assertions.assertEquals(Set.of("t0", "t1", "t2"), processed);
			Assertions.assertEquals(Set.of("t3", "t4"), cancelled, "processed/in-flight tasks must not be cancelled");
			Assertions.assertEquals(0, q.size());
		} finally {
			pool.shutdownNow();
			Assertions.assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
		}
	}

	/**
	 * 无在飞批量时 shutdown(true)：未运行任务全部补偿（除已认领的队首任务按教程语义保留执行）。
	 */
	@Test
	public void testShutdownCancelIdle() throws InterruptedException {
		var pool = Executors.newSingleThreadExecutor();
		try {
			var q = new TaskOneByOneQueue(pool);
			var processed = ConcurrentHashMap.<String>newKeySet();
			var cancelled = ConcurrentHashMap.<String>newKeySet();
			q.lock();
			Runnable dispatch;
			try {
				dispatch = q.submit(new RecordingTask("a0", () -> processed.add("a0"), () -> cancelled.add("a0")));
				q.submit(new RecordingTask("a1", () -> processed.add("a1"), () -> cancelled.add("a1")));
				q.submit(new RecordingTask("a2", () -> processed.add("a2"), () -> cancelled.add("a2")));
			} finally {
				q.unlock();
			}
			q.shutdown(true); // batch 已认领 a0 但尚未运行
			Assertions.assertEquals(Set.of("a1", "a2"), cancelled);
			Assertions.assertNotNull(dispatch);
			dispatch.run(); // 已认领的首任务执行（教程：正在运行的首任务保留）
			q.waitComplete();
			Assertions.assertEquals(Set.of("a0"), processed);
			Assertions.assertEquals(0, q.size());
		} finally {
			pool.shutdownNow();
			Assertions.assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
		}
	}

	/**
	 * shutdown(false)：mid-batch 调用不取消任何任务，队列继续把任务执行完。
	 */
	@Test
	public void testShutdownNoCancelContinues() throws InterruptedException {
		var pool = Executors.newSingleThreadExecutor();
		try {
			var q = new TaskOneByOneQueue(pool);
			var processed = ConcurrentHashMap.<String>newKeySet();
			var cancelled = ConcurrentHashMap.<String>newKeySet();
			var c1Started = new CountDownLatch(1);
			var releaseC1 = new CountDownLatch(1);
			q.lock();
			try {
				var dispatch = q.submit(new RecordingTask("c0", () -> processed.add("c0"), () -> cancelled.add("c0")));
				q.submit(new RecordingTask("c1", () -> {
					processed.add("c1");
					c1Started.countDown();
					runLatch(releaseC1);
				}, () -> cancelled.add("c1")));
				q.submit(new RecordingTask("c2", () -> processed.add("c2"), () -> cancelled.add("c2")));
				if (dispatch != null)
					dispatch.run();
			} finally {
				q.unlock();
			}
			Assertions.assertTrue(c1Started.await(10, TimeUnit.SECONDS));
			q.shutdown(false); // c1 在飞
			releaseC1.countDown();
			q.waitComplete();
			Assertions.assertEquals(Set.of("c0", "c1", "c2"), processed);
			Assertions.assertTrue(cancelled.isEmpty());
			Assertions.assertEquals(0, q.size());
		} finally {
			pool.shutdownNow();
			Assertions.assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
		}
	}
}
