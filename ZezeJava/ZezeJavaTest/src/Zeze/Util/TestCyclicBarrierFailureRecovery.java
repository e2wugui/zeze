package Zeze.Util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestCyclicBarrierFailureRecovery {

	/** 固定分组遍历顺序，明确区分先入队的桶与后失败的桶。 */
	private static class OrderedQueue extends TaskOneByOneQueue {
		private final int index;

		OrderedQueue(int index, Executor executor) {
			super(executor);
			this.index = index;
		}

		@Override
		public int hashCode() {
			return index;
		}
	}

	private static final class Engine extends TaskOneByOneBase {
		private final OrderedQueue[] queues;

		Engine(OrderedQueue... queues) {
			this.queues = queues;
		}

		@Override
		protected TaskOneByOneQueue getAndLockQueue(Object key) {
			var queue = queues[(Integer)key];
			queue.lock();
			return queue;
		}
	}

	private static final class ControlledExecutor implements Executor {
		final ArrayDeque<Runnable> accepted = new ArrayDeque<>();
		final ArrayDeque<Throwable> failures = new ArrayDeque<>();

		@Override
		public void execute(Runnable command) {
			if (!failures.isEmpty())
				throw Task.forceThrow(failures.removeFirst());
			accepted.addLast(command);
		}

		void drain() {
			for (Runnable command; (command = accepted.pollFirst()) != null; )
				command.run();
		}
	}

	@SuppressWarnings("DataFlowIssue") // Procedure 仅用于失败路径，不会执行业务。
	private static void submitFailingBarrier(Engine engine, boolean procedureMode, List<Integer> keys, Action0 cancel) {
		if (procedureMode)
			engine.executeCyclicBarrier(new ArrayList<>(keys),
					new Procedure(null, () -> 0, "failure", null), cancel, null);
		else
			engine.executeCyclicBarrier(new ArrayList<>(keys), "failure", () -> {}, cancel, null);
	}

	@Test
	public void dispatchStartsAfterEveryQueueHoldIsReleased() {
		var queues = new OrderedQueue[2];
		var lockedDuringDispatch = new AtomicBoolean();
		Executor executor = command -> {
			for (var queue : queues) {
				if (queue.isHeldByCurrentThread())
					lockedDuringDispatch.set(true);
			}
			command.run();
		};
		queues[0] = new OrderedQueue(0, executor);
		queues[1] = new OrderedQueue(1, executor);
		var runs = new AtomicInteger();
		new Engine(queues).executeCyclicBarrier(new ArrayList<>(List.of(0, 0, 1)), "ready",
				runs::incrementAndGet, null, null);
		assertFalse(lockedDuringDispatch.get(), "派发前必须释放其他桶及重复 key 的全部持锁次数");
		assertEquals(1, runs.get());
		for (var queue : queues) {
			assertFalse(queue.isLocked());
			assertEquals(0, queue.size());
		}
	}

	@Test
	public void rejectionCancellationAllowsAnotherThreadToAcquireRemainingQueue() throws Exception {
		for (var procedureMode : new boolean[]{false, true}) {
			var failure = new RejectedExecutionException("dispatch rejected");
			var first = new OrderedQueue(0, command -> { throw failure; });
			var remaining = new OrderedQueue(1, Runnable::run);
			var accessible = new AtomicBoolean();
			var cancellations = new AtomicInteger();
			var verifier = Executors.newSingleThreadExecutor();
			try {
				var observed = assertThrows(RejectedExecutionException.class, () ->
						submitFailingBarrier(new Engine(first, remaining), procedureMode, List.of(0, 1, 1), () -> {
							cancellations.incrementAndGet();
							accessible.set(verifier.submit(() -> {
								if (!remaining.tryLock(1, TimeUnit.SECONDS))
									return false;
								remaining.unlock();
								return true;
							}).get(3, TimeUnit.SECONDS));
						}));
				assertSame(failure, observed);
				assertTrue(accessible.get(), "取消回调执行期间，其他线程必须仍能取得剩余桶的锁");
				assertEquals(1, cancellations.get());
				assertEquals(0, first.size());
				assertEquals(0, remaining.size());
			} finally {
				verifier.shutdownNow();
			}
		}
	}

	@Test
	public void shutdownCancellationRunsAfterRemainingQueueIsUnlocked() {
		var stopped = new OrderedQueue(0, Runnable::run);
		var remaining = new OrderedQueue(1, Runnable::run);
		stopped.shutdown(true);
		var lockedDuringCancel = new AtomicBoolean();
		var cancellations = new AtomicInteger();
		var runs = new AtomicInteger();
		new Engine(stopped, remaining).executeCyclicBarrier(new ArrayList<>(List.of(0, 1)), "stopped",
				runs::incrementAndGet, () -> {
					lockedDuringCancel.set(remaining.isHeldByCurrentThread());
					cancellations.incrementAndGet();
				}, null);
		assertFalse(lockedDuringCancel.get());
		assertEquals(1, cancellations.get());
		assertEquals(0, runs.get());
		assertEquals(0, remaining.size());
	}

	@Test
	public void partialSubmissionFailureDrivesPreviouslyPreparedBuckets() {
		for (var procedureMode : new boolean[]{false, true}) {
			var failure = new IllegalStateException("second bucket submit failed");
			var dispatches = new AtomicInteger();
			var first = new OrderedQueue(0, command -> {
				dispatches.incrementAndGet();
				command.run();
			});
			var second = new OrderedQueue(1, Runnable::run) {
				@Override
				public Runnable submit(Task task) {
					throw failure;
				}
			};
			var engine = new Engine(first, second);
			var cancellations = new AtomicInteger();
			assertSame(failure, assertThrows(IllegalStateException.class, () ->
					submitFailingBarrier(engine, procedureMode, List.of(0, 0, 1), cancellations::incrementAndGet)));
			assertEquals(1, dispatches.get(), "已认领的派发仍须消费取消后的屏障");
			assertEquals(1, cancellations.get());
			assertEquals(0, first.size());
			assertFalse(first.isLocked());
			assertFalse(second.isLocked());
			var laterRuns = new AtomicInteger();
			TaskSpec.ofAction(laterRuns::incrementAndGet).executeOneByOne(0, engine);
			assertEquals(1, laterRuns.get());
		}
	}

	@Test
	public void partialSubmissionPreservesOriginalFailureWhenPreparedDispatchAlsoFails() {
		var primary = new IllegalStateException("submit failed");
		var secondary = new AssertionError("prepared dispatch failed");
		var first = new OrderedQueue(0, command -> { throw secondary; });
		var second = new OrderedQueue(1, Runnable::run) {
			@Override
			public Runnable submit(Task task) {
				throw primary;
			}
		};
		assertSame(primary, assertThrows(IllegalStateException.class, () ->
				submitFailingBarrier(new Engine(first, second), false, List.of(0, 1), () -> {})));
		assertEquals(1, primary.getSuppressed().length);
		assertSame(secondary, primary.getSuppressed()[0]);
		assertEquals(0, first.size());
	}

	@Test
	public void dispatchErrorReturnsClaimAndMakesLateDispatchHarmless() {
		for (var procedureMode : new boolean[]{false, true}) {
			var failure = new AssertionError("dispatch failed before running");
			var stale = new AtomicReference<Runnable>();
			var dispatches = new AtomicInteger();
			var queue = new OrderedQueue(0, command -> {
				if (dispatches.incrementAndGet() == 1) {
					stale.set(command);
					throw failure;
				}
				command.run();
			});
			var engine = new Engine(queue);
			var cancellations = new AtomicInteger();
			assertSame(failure, assertThrows(AssertionError.class, () ->
					submitFailingBarrier(engine, procedureMode, List.of(0), cancellations::incrementAndGet)));
			assertEquals(1, cancellations.get());
			assertEquals(0, queue.size());
			var laterRuns = new AtomicInteger();
			TaskSpec.ofAction(laterRuns::incrementAndGet).executeOneByOne(0, engine);
			stale.get().run();
			assertEquals(1, laterRuns.get(), "旧派发迟到不能再次驱动新批次");
			assertEquals(0, queue.size());
		}
	}

	@Test
	public void inlineDispatchErrorDoesNotCompensateCompletedBusiness() {
		var failure = new AssertionError("executor failed after running");
		var dispatches = new AtomicInteger();
		var queue = new OrderedQueue(0, command -> {
			command.run();
			if (dispatches.incrementAndGet() == 1)
				throw failure;
		});
		var engine = new Engine(queue);
		var runs = new AtomicInteger();
		var cancellations = new AtomicInteger();
		assertSame(failure, assertThrows(AssertionError.class, () ->
				engine.executeCyclicBarrier(new ArrayList<>(List.of(0)), "completed",
						runs::incrementAndGet, cancellations::incrementAndGet, null)));
		assertEquals(1, runs.get());
		assertEquals(0, cancellations.get(), "已经执行的业务不能再被失败补偿");
		assertEquals(0, queue.size());
		TaskSpec.ofAction(runs::incrementAndGet).executeOneByOne(0, engine);
		assertEquals(2, runs.get());
	}

	@Test
	public void successfulBarrierSettlesEveryReachedBucketAfterResumeErrors() {
		verifyResumeErrors(false);
	}

	@Test
	public void canceledBarrierSettlesEveryReachedBucketAfterResumeErrors() {
		verifyResumeErrors(true);
	}

	private static void verifyResumeErrors(boolean cancel) {
		var executor = new ControlledExecutor();
		var engine = new TaskOneByOneByKey(2, executor);
		var barrier = new TaskOneByOneQueue.BarrierAction("resume", () -> {}, cancel ? 3 : 2, null);
		for (int key = 0; key < 2; key++)
			engine.execute(key, new TaskOneByOneQueue.TaskBarrierAction(barrier, 1, DispatchMode.Normal));
		var cancellations = new AtomicInteger();
		var runs = new AtomicInteger();
		for (int key = 0; key < 2; key++)
			TaskSpec.ofAction(runs::incrementAndGet).onCancel(cancellations::incrementAndGet)
					.dispatchMode(DispatchMode.Critical).executeOneByOne(key, engine);
		executor.accepted.removeFirst().run();
		var last = executor.accepted.removeFirst();
		var firstFailure = new AssertionError("first resume failed");
		var secondFailure = new AssertionError("second resume failed");
		executor.failures.addLast(firstFailure);
		executor.failures.addLast(secondFailure);
		if (cancel)
			last.run(); // 两桶均已到达，但屏障计数仍差一，不执行业务。
		assertSame(firstFailure, assertThrows(AssertionError.class, cancel ? barrier::cancel : last::run));
		assertEquals(1, firstFailure.getSuppressed().length);
		assertSame(secondFailure, firstFailure.getSuppressed()[0]);
		assertEquals(2, cancellations.get());
		assertEquals(0, runs.get());
		for (int key = 0; key < 2; key++) {
			assertEquals(0, engine.getQueueSize(key));
			TaskSpec.ofAction(runs::incrementAndGet).executeOneByOne(key, engine);
		}
		executor.drain();
		assertEquals(2, runs.get());
	}
}
