package Zeze.Util;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestExecuteBatchDispatchError {
	@Test
	void settlesTheUnsubmittedCollectionKeysWhenTheExecutorThrowsAnError() {
		var failure = new AssertionError("thread creation failed");
		var queues = new TaskOneByOneByKey(1, task -> { throw failure; });
		var completed = new AtomicInteger();
		var executed = new AtomicInteger();
		assertSame(failure, assertThrows(AssertionError.class, () -> queues.executeBatch(
				List.of(1, 2, 3), key -> executed.incrementAndGet(), completed::incrementAndGet, null)));
		assertEquals(0, executed.get());
		assertEquals(1, completed.get());
		queues.shutdown();
		assertEquals(1, completed.get());
	}

	@Test
	void settlesTheUnsubmittedPrimitiveKeysWhenTheExecutorThrowsAnError() {
		var failure = new AssertionError("thread creation failed");
		var queues = new TaskOneByOneByKey(1, task -> { throw failure; });
		var completed = new AtomicInteger();
		var keys = LongList.wrap(new long[]{1, 2, 3});
		assertSame(failure, assertThrows(AssertionError.class, () -> queues.executeBatch(
				keys, key -> {}, completed::incrementAndGet, null)));
		assertEquals(1, completed.get());
		queues.shutdown();
		assertEquals(1, completed.get());
	}
}
