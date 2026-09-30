package Zeze.Net;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** A pending add must not recreate selector threads after the manager has finished closing. */
@Fast
public class TestSelectorsRejectAddAfterClose {
	@Test
	@Timeout(20)
	public void queuedAddIsRejectedAfterManagerCloses() throws Exception {
		var selectors = new Selectors("test.selectors.closedAdd", 1);
		var original = selectors.choice();
		var finished = new CountDownLatch(1);
		var addFailure = new AtomicReference<Throwable>();
		var adder = new Thread(() -> {
			try {
				selectors.add(1);
			} catch (Throwable e) {
				addFailure.set(e);
			} finally {
				finished.countDown();
			}
		}, "test-selector-pending-add");
		adder.setDaemon(true);
		try {
			selectors.lock();
			try {
				adder.start();
				long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
				while (!selectors.hasQueuedThread(adder) && finished.getCount() != 0
						&& System.nanoTime() < deadline)
					Thread.sleep(1);
				Assertions.assertTrue(selectors.hasQueuedThread(adder),
						"add must be queued behind the manager lock before shutdown starts");

				// Reentrant ownership lets this thread finish close while add is still waiting.
				// The old implementation has already read closed=false before joining the lock queue.
				selectors.close();
				Assertions.assertFalse(original.isAlive());
			} finally {
				selectors.unlock();
			}

			Assertions.assertTrue(finished.await(5, TimeUnit.SECONDS));
			Assertions.assertAll(
					() -> Assertions.assertInstanceOf(IllegalStateException.class, addFailure.get(),
							"an add that acquired the lock after close must fail"),
					() -> Assertions.assertThrows(IllegalStateException.class, selectors::choice,
							"a closed manager must not regain a selectable thread"));
		} finally {
			adder.join(5_000);
			// In the original implementation the late add created a new array of threads. A second
			// close sees that array and joins it, so the failing reproduction leaves no selector alive.
			selectors.close();
			Assertions.assertFalse(adder.isAlive(), "the add worker must not leak");
			Assertions.assertFalse(original.isAlive(), "the original selector must remain stopped");
		}
	}
}
