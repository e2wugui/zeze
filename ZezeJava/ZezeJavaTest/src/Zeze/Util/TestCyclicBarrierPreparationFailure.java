package Zeze.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import Zeze.Transaction.Procedure;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestCyclicBarrierPreparationFailure {

	private static final class FailingLookup extends TaskOneByOneBase {
		final TaskOneByOneQueue queue = new TaskOneByOneQueue(Runnable::run);
		final Throwable failure;

		FailingLookup(Throwable failure) {
			this.failure = failure;
		}

		@Override
		protected TaskOneByOneQueue getAndLockQueue(Object key) {
			if (key.equals(1))
				throw Task.forceThrow(failure);
			queue.lock();
			return queue;
		}
	}

	@Test
	public void actionBarrierReleasesEveryPreparedQueueWhenLookupFails() throws Exception {
		verifyPreparationFailure(false);
	}

	@Test
	public void procedureBarrierReleasesEveryPreparedQueueWhenLookupFails() throws Exception {
		verifyPreparationFailure(true);
	}

	@SuppressWarnings("DataFlowIssue") // Preparation fails before the Procedure can execute.
	private static void verifyPreparationFailure(boolean procedureMode) throws Exception {
		for (var failure : new Throwable[]{new IllegalArgumentException("lookup failed"), new AssertionError("lookup failed")}) {
			var engine = new FailingLookup(failure);
			var verifier = Executors.newSingleThreadExecutor();
			try {
				var keys = new ArrayList<>(List.of(0, 0, 1)); // Two holds on the same queue before lookup fails.
				var observed = assertThrows(failure.getClass(), () -> {
					if (procedureMode)
						engine.executeCyclicBarrier(keys, new Procedure(null, () -> 0, "prepare", null), null, null);
					else
						engine.executeCyclicBarrier(keys, "prepare", () -> {}, null, null);
				});
				assertSame(failure, observed);
				var accessible = verifier.submit(() -> {
					if (!engine.queue.tryLock(1, TimeUnit.SECONDS))
						return false;
					try {
						return true;
					} finally {
						engine.queue.unlock();
					}
				});
				assertTrue(accessible.get(5, TimeUnit.SECONDS), "lookup failure must not strand earlier queue locks");
			} finally {
				while (engine.queue.isHeldByCurrentThread())
					engine.queue.unlock();
				verifier.shutdownNow();
			}
		}
	}
}
