package Zeze.Util;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestAsyncLockRejectFallbackDeepQueue {
	@Test
	void deepQueueUsesConstantStackOnRejection() throws Exception {
		verifyDeepQueue(true);
	}

	@Test
	void deepQueueUsesConstantStackOnDirectExecution() throws Exception {
		verifyDeepQueue(false);
	}

	private static void verifyDeepQueue(boolean reject) throws Exception {
		var submissions = new AtomicInteger();
		var count = new AtomicInteger();
		var lock = new AsyncLock(task -> {
			submissions.incrementAndGet();
			if (reject)
				throw new RejectedExecutionException("test rejection");
			task.run();
		});
		var thrown = new AtomicReference<@Nullable Throwable>();
		int depth = 100_000;
		var holder = new Thread(null, () -> {
			try {
				lock.enter(() -> {
					for (int i = 0; i < depth; i++) {
						lock.enter(() -> {
							count.incrementAndGet();
							lock.leave();
						});
					}
				});
			} catch (Throwable e) {
				thrown.set(e);
			}
		}, "asynclock-deepqueue", 256 * 1024);
		holder.setDaemon(true);
		holder.start();
		holder.join(10_000);
		assertFalse(holder.isAlive());
		assertNull(thrown.get());
		assertEquals(depth, count.get());
		assertEquals(1, submissions.get());
		assertFalse(lock.isLocked());
		lock.enter(count::incrementAndGet);
		assertEquals(depth + 1, count.get());
	}
}
