package Zeze.Util;

import harness.Fast;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Transaction.DispatchMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestKeyedQueueExecutorErrorRecovery {
	private static final class ControlledExecutor implements Executor {
		final ArrayDeque<Runnable> accepted = new ArrayDeque<>();
		boolean fail;
		@Override
		public void execute(Runnable command) {
			if (fail)
				throw new AssertionError("dispatch failed before start");
			accepted.addLast(command);
		}
		void drain() {
			for (Runnable command; (command = accepted.pollFirst()) != null; )
				command.run();
		}
	}

	@Test
	public void errorBeforeStartReturnsClaimAndPreservesTask() {
		var executor = new ControlledExecutor();
		var queue = new TaskOneByOneByKey2(1, executor);
		var runs = new AtomicInteger();
		executor.fail = true;
		assertThrows(AssertionError.class, () -> TaskSpec.ofAction(runs::incrementAndGet).executeOneByOne(0, queue));
		executor.fail = false;
		TaskSpec.ofAction(runs::incrementAndGet).executeOneByOne(0, queue);
		executor.drain();
		assertEquals(2, runs.get());
	}

	@Test
	public void barrierFollowupErrorReturnsEveryBucketClaim() {
		var executor = new ControlledExecutor();
		var queue = new TaskOneByOneByKey2(2, executor);
		queue.executeCyclicBarrier(List.of(0, 1), "fail-followup", () -> executor.fail = true, DispatchMode.Normal);
		var runs = new AtomicInteger();
		for (int key = 0; key < 2; key++)
			TaskSpec.ofAction(runs::incrementAndGet).dispatchMode(DispatchMode.Critical).executeOneByOne(key, queue);
		executor.accepted.removeFirst().run();
		assertThrows(AssertionError.class, executor.accepted.removeFirst()::run);
		executor.fail = false;
		for (int key = 0; key < 2; key++)
			TaskSpec.ofAction(runs::incrementAndGet).executeOneByOne(key, queue);
		executor.drain();
		assertEquals(4, runs.get());
	}

	@Test
	public void errorAfterInlineExecutionDoesNotRunTaskTwice() {
		var queue = new TaskOneByOneByKey2(1, command -> {
			command.run();
			throw new AssertionError("after execution");
		});
		var runs = new AtomicInteger();
		for (int i = 0; i < 2; i++)
			assertThrows(AssertionError.class, () -> TaskSpec.ofAction(runs::incrementAndGet).executeOneByOne(0, queue));
		assertEquals(2, runs.get());
	}
}
