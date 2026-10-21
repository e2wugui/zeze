package Zeze.Util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestAsyncLockDispatchReject {
	@Test
	void rejectionFallsBackAndLaterDispatchUsesTheSameExecutor() {
		var reject = new AtomicBoolean(true);
		var tasks = new ConcurrentLinkedQueue<Runnable>();
		var submissions = new AtomicInteger();
		var count = new AtomicInteger();
		var lock = new AsyncLock(task -> {
			submissions.incrementAndGet();
			if (reject.get())
				throw new RejectedExecutionException("test rejection");
			tasks.add(task);
		});
		assertDoesNotThrow(() -> lock.enter(() -> lock.enter(count::incrementAndGet)));
		assertEquals(1, count.get());
		assertEquals(1, submissions.get());
		assertFalse(lock.isLocked());

		reject.set(false);
		lock.enter(() -> lock.enter(count::incrementAndGet));
		assertEquals(1, count.get(), "排队回调必须投递到注入的执行器");
		assertEquals(1, tasks.size());
		assertTrue(lock.isLocked());
		var task = tasks.poll();
		assertNotNull(task);
		task.run();
		assertEquals(2, count.get());
		assertEquals(2, submissions.get());
		assertFalse(lock.isLocked());
	}
}
