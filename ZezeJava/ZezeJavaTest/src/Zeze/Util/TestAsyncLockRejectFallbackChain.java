package Zeze.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestAsyncLockRejectFallbackChain {
	@Test
	void rejectionDrainsWholeChainIncludingExplicitLeaveAndExceptions() {
		var submissions = new AtomicInteger();
		var lock = new AsyncLock(task -> {
			submissions.incrementAndGet();
			throw new RejectedExecutionException("test rejection");
		});
		var order = new ArrayList<Integer>();
		var threads = new ArrayList<Thread>();
		lock.enter(() -> {
			lock.enter(() -> {
				threads.add(Thread.currentThread());
				order.add(1);
				lock.leave();
				order.add(2); // 内联驱动必须等当前回调返回才推进。
			});
			lock.enter(() -> {
				threads.add(Thread.currentThread());
				order.add(3);
				throw new IllegalStateException("test callback failure");
			});
			lock.enter(() -> order.add(4));
		});
		assertEquals(List.of(1, 2, 3, 4), order);
		assertEquals(List.of(Thread.currentThread(), Thread.currentThread()), threads);
		assertEquals(1, submissions.get(), "拒绝后不应逐回调重新投递");
		assertFalse(lock.isLocked());
		lock.enter(() -> order.add(5));
		assertEquals(List.of(1, 2, 3, 4, 5), order);
	}
}
