package Zeze.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import Zeze.Transaction.DispatchMode;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestOneByOneInlineDispatch {
	private record Engine(BiConsumer<Action0, DispatchMode> submit, IntSupplier size) {
	}

	private static Engine newEngine(boolean key2, Executor executor) {
		if (key2) {
			var queue = new TaskOneByOneByKey2(1, executor);
			return new Engine((action, mode) -> TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(0, queue),
					() -> queue.getQueueSize(0));
		}
		var queue = new TaskOneByOneByKey(1, executor);
		return new Engine((action, mode) -> TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(0, queue),
				() -> queue.getQueueSize(0));
	}

	private static void drainAlternatingModes(Engine engine) {
		var completed = new AtomicInteger();
		var ordered = new AtomicBoolean(true);
		int count = 10_000;
		assertDoesNotThrow(() -> engine.submit.accept(() -> {
			for (int i = 0; i < count; i++) {
				int expected = i;
				engine.submit.accept(() -> {
					if (completed.getAndIncrement() != expected)
						ordered.set(false);
				}, (i & 1) == 0 ? DispatchMode.Normal : DispatchMode.Critical);
			}
		}, DispatchMode.Normal));
		assertEquals(count, completed.get(), "every accepted task must complete");
		assertTrue(ordered.get(), "one bucket must preserve FIFO across mode changes");
		assertEquals(0, engine.size.getAsInt());
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void directExecutorDrainsAlternatingModes(boolean key2) {
		drainAlternatingModes(newEngine(key2, Runnable::run));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void saturatedCallerRunsDrainsAlternatingModes(boolean key2) throws InterruptedException {
		var started = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		try (var pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<>(),
				new ThreadPoolExecutor.CallerRunsPolicy())) {
			pool.execute(() -> {
				started.countDown();
				try {
					release.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
			try {
				assertTrue(started.await(5, TimeUnit.SECONDS));
				drainAlternatingModes(newEngine(key2, pool));
			} finally {
				release.countDown();
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void anotherBucketCanStillExecuteInline(boolean key2) {
		var outer = newEngine(key2, Runnable::run);
		var inner = newEngine(!key2, Runnable::run);
		var completed = new AtomicBoolean();
		var observedBeforeReturn = new AtomicBoolean();
		outer.submit.accept(() -> {
			inner.submit.accept(() -> completed.set(true), DispatchMode.Normal);
			observedBeforeReturn.set(completed.get());
		}, DispatchMode.Normal);
		assertTrue(observedBeforeReturn.get(), "a different bucket must retain its executor's inline behavior");
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void eachDispatchRetainsItsExecutorContext(boolean key2) {
		var nextContext = new AtomicInteger();
		var executorContext = new ThreadLocal<Integer>();
		Executor decorated = command -> {
			var previous = executorContext.get();
			executorContext.set(nextContext.incrementAndGet());
			try {
				command.run();
			} finally {
				if (previous == null)
					executorContext.remove();
				else
					executorContext.set(previous);
			}
		};
		var engine = newEngine(key2, decorated);
		var correctContexts = new AtomicInteger();
		engine.submit.accept(() -> {
			for (int i = 0; i < 10; i++)
				engine.submit.accept(() -> {
					if (Integer.valueOf(nextContext.get()).equals(executorContext.get()))
						correctContexts.incrementAndGet();
				}, (i & 1) == 0 ? DispatchMode.Normal : DispatchMode.Critical);
		}, DispatchMode.Normal);
		assertEquals(10, correctContexts.get(), "tasks must run inside their dispatch's executor decoration");
		assertNull(executorContext.get(), "executor context must be restored after the queue drains");
	}

	@Test
	public void cancellationCanStillSubmitAnIdleBucketInline() {
		var reject = new AtomicBoolean(true);
		Executor executor = command -> {
			if (reject.getAndSet(false))
				throw new RejectedExecutionException("initial dispatch rejected");
			command.run();
		};
		var queue = new TaskOneByOneByKey(1, executor);
		var completed = new AtomicBoolean();
		var observedBeforeReturn = new AtomicBoolean();
		assertThrows(RejectedExecutionException.class, () -> TaskSpec.ofAction(() -> fail("rejected task ran"))
				.onCancel(() -> {
					TaskSpec.ofAction(() -> completed.set(true)).executeOneByOne(0, queue);
					observedBeforeReturn.set(completed.get());
				}).executeOneByOne(0, queue));
		assertTrue(observedBeforeReturn.get());
		assertEquals(0, queue.getQueueSize(0));
	}
}
