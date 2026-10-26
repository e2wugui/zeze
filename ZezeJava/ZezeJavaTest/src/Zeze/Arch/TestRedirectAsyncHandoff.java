package Zeze.Arch;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRedirectAsyncHandoff {
	@Test
	void rejectingAnotherResultKeepsTheOriginalResult() throws Exception {
		var future = RedirectAllFuture.<RedirectResult>async();
		var first = new RedirectResult();
		future.asyncResult(first);
		assertThrows(IllegalStateException.class, () -> future.asyncResult(new RedirectResult()));
		var delivered = new ArrayList<RedirectResult>();
		future.onResult(delivered::add);
		assertEquals(1, delivered.size());
		assertSame(first, delivered.getFirst());
	}

	@Test
	void rejectingAnotherCallbackKeepsTheRegisteredCallback() throws Exception {
		var future = RedirectAllFuture.<RedirectResult>async();
		var delivered = new ArrayList<RedirectResult>();
		future.onResult(delivered::add);
		assertThrows(IllegalStateException.class, () -> future.onResult(__ -> fail("rejected callback")));
		var first = new RedirectResult();
		future.asyncResult(first);
		assertEquals(java.util.List.of(first), delivered);
	}

	@Test
	void aCompletedHandoffCannotDeliverAgain() throws Exception {
		var future = RedirectAllFuture.<RedirectResult>async();
		future.onResult(__ -> { });
		future.asyncResult(new RedirectResult());
		assertThrows(IllegalStateException.class, () -> future.onResult(__ -> fail("duplicate delivery")));
		assertThrows(IllegalStateException.class, () -> future.asyncResult(new RedirectResult()));
	}

	@Test
	void concurrentCallbackAndResultMeetExactlyOnce() throws Exception {
		try (var executor = Executors.newFixedThreadPool(2)) {
			for (int i = 0; i < 100; i++) {
				var future = RedirectAllFuture.<RedirectResult>async();
				var expected = new RedirectResult();
				var delivered = new AtomicReference<RedirectResult>();
				var count = new AtomicInteger();
				var start = new CountDownLatch(1);
				var producer = executor.submit(() -> {
					assertTrue(start.await(10, TimeUnit.SECONDS));
					future.asyncResult(expected);
					return null;
				});
				var consumer = executor.submit(() -> {
					assertTrue(start.await(10, TimeUnit.SECONDS));
					future.onResult(result -> {
						delivered.set(result);
						count.incrementAndGet();
					});
					return null;
				});
				start.countDown();
				producer.get(10, TimeUnit.SECONDS);
				consumer.get(10, TimeUnit.SECONDS);
				assertSame(expected, delivered.get());
				assertEquals(1, count.get());
			}
		}
	}
}
