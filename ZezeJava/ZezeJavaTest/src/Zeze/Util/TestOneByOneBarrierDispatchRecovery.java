package Zeze.Util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Transaction.DispatchMode;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestOneByOneBarrierDispatchRecovery {
	private static final class ControlledExecutor implements Executor {
		final ArrayDeque<Runnable> accepted = new ArrayDeque<>();
		boolean reject;

		@Override
		public void execute(Runnable command) {
			if (reject)
				throw new RejectedExecutionException("barrier follow-up rejected");
			accepted.addLast(command);
		}

		void drain() {
			for (Runnable command; (command = accepted.pollFirst()) != null; )
				command.run();
		}
	}

	@Test
	public void queueBarrierSettlesEveryBucketAfterFirstDispatchRejection() {
		var executor = new ControlledExecutor();
		var queue = new TaskOneByOneByKey(2, executor);
		queue.executeCyclicBarrier(new ArrayList<>(List.of(0, 1)), "reject-next",
				() -> executor.reject = true, null, DispatchMode.Normal);
		var cancellations = new AtomicInteger();
		var runs = new AtomicInteger();
		for (int key = 0; key < 2; key++)
			TaskSpec.ofAction(runs::incrementAndGet).dispatchMode(DispatchMode.Critical)
					.onCancel(cancellations::incrementAndGet).executeOneByOne(key, queue);
		assertEquals(2, executor.accepted.size());
		executor.accepted.removeFirst().run();
		assertThrows(RejectedExecutionException.class, executor.accepted.removeFirst()::run);
		assertEquals(0, runs.get());
		assertEquals(2, cancellations.get(), "首桶失败不能跳过另一桶的补偿");
		assertEquals(0, queue.getQueueSize(0));
		assertEquals(0, queue.getQueueSize(1));
	}

	@Test
	public void key2BarrierReturnsEveryClaimAfterFirstDispatchRejection() {
		var executor = new ControlledExecutor();
		var queue = new TaskOneByOneByKey2(2, executor);
		queue.executeCyclicBarrier(List.of(0, 1), "reject-next",
				() -> executor.reject = true, DispatchMode.Normal);
		var retainedRuns = new AtomicInteger();
		var newRuns = new AtomicInteger();
		for (int key = 0; key < 2; key++)
			TaskSpec.ofAction(retainedRuns::incrementAndGet).dispatchMode(DispatchMode.Critical)
					.executeOneByOne(key, queue);
		assertEquals(2, executor.accepted.size());
		executor.accepted.removeFirst().run();
		assertThrows(RejectedExecutionException.class, executor.accepted.removeFirst()::run);
		executor.reject = false;
		for (int key = 0; key < 2; key++)
			TaskSpec.ofAction(newRuns::incrementAndGet).executeOneByOne(key, queue);
		executor.drain();
		assertEquals(2, retainedRuns.get(), "Key2不丢任务，所有桶必须允许后续提交重新派发");
		assertEquals(2, newRuns.get());
		assertEquals(0, queue.getQueueSize(0));
		assertEquals(0, queue.getQueueSize(1));
	}
}
