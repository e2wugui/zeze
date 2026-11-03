package Zeze.Util;

import harness.Fast;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestQueueDrainWakesWaiter {
	@Test
	public void normalDrainWakesWaiterWithoutShutdown() throws Exception {
		var queue = new TaskOneByOneQueue(Runnable::run);
		Runnable dispatch;
		queue.lock();
		try {
			dispatch = queue.submit(new TaskOneByOneQueue.TaskBodyTask(
					new TaskBody.OfAction(() -> { }), "drain", null, null));
		} finally {
			queue.unlock();
		}
		var done = new CountDownLatch(1);
		var waiter = Thread.ofPlatform().daemon().start(() -> {
			try {
				queue.waitComplete();
				done.countDown();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		try {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (waiter.getState() != Thread.State.WAITING && System.nanoTime() < deadline)
				Thread.yield();
			assertEquals(Thread.State.WAITING, waiter.getState());
			assertNotNull(dispatch);
			dispatch.run();
			assertTrue(done.await(5, TimeUnit.SECONDS));
			assertEquals(0, queue.size());
		} finally {
			waiter.interrupt();
			waiter.join(5_000);
		}
	}
}
