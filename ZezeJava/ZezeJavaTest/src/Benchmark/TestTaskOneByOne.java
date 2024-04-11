package Benchmark;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.TaskOneByOneByKey2;
import org.junit.Test;

public class TestTaskOneByOne {
	public final static int TaskCount = 1000_0000;

	private final AtomicLong counter = new AtomicLong();
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition cond = lock.newCondition();

	@Test
	public void testBenchmark() throws InterruptedException {
		Task.tryInitThreadPool();
		var oo = new TaskOneByOneByKey();
		var b = new Zeze.Util.Benchmark();
		for (int i = 0; i < TaskCount; ++i) {
			oo.Execute(1, () -> {
				counter.incrementAndGet();
			});
		}
		oo.Execute(1, () -> {
			lock.lock();
			try {
				counter.incrementAndGet();
				cond.signal();
			} finally {
				lock.unlock();
			}
		});
		lock.lock();
		try {
			cond.await();
		} finally {
			lock.unlock();
		}
		b.report("TestTaskOneByOne.testBenchmark", TaskCount);
		System.out.println(counter.get());
	}

	@Test
	public void testBenchmark2() throws InterruptedException {
		Task.tryInitThreadPool();
		var oo = new TaskOneByOneByKey2();
		var b = new Zeze.Util.Benchmark();
		for (int i = 0; i < TaskCount; ++i) {
			oo.Execute(1, () -> {
				counter.incrementAndGet();
			});
		}
		oo.Execute(1, () -> {
			lock.lock();
			try {
				counter.incrementAndGet();
				cond.signal();
			} finally {
				lock.unlock();
			}
		});
		lock.lock();
		try {
			cond.await();
		} finally {
			lock.unlock();
		}
		b.report("TestTaskOneByOne.testBenchmark2", TaskCount);
		System.out.println(counter.get());
	}

	private static final int exeCount = 10_0000;
	private static final int keyCount = 20;
	private static final int roleCount = 10000;
	private static final int concurrency = 20;
	private static final ConcurrentSkipListSet<Integer> taskIds = new ConcurrentSkipListSet<>();

	private static void runCyclicBarrier(TaskOneByOneByKey oo, AtomicInteger taskCounter, CountDownLatch taskAwaiter) {
		int taskId = taskCounter.incrementAndGet();
		if (taskId > exeCount)
			return;
		var rand = ThreadLocalRandom.current();
		var roleIds = new ArrayList<Long>(keyCount);
		for (int i = 0; i < keyCount; i++)
			roleIds.add((long)rand.nextInt(roleCount));
		// System.out.println("+ B-" + taskId + ": " + roleIds);
		taskIds.add(taskId);
		oo.executeCyclicBarrier(roleIds, "B-" + taskId, () -> {
			// System.out.println("- B-" + taskId + ": " + roleIds);
			taskIds.remove(taskId);
			taskAwaiter.countDown();
			runCyclicBarrier(oo, taskCounter, taskAwaiter);
		}, null, DispatchMode.Normal);
	}

	@Test
	public void testCyclicBarrier() throws InterruptedException {
		Task.tryInitThreadPool();
		taskIds.clear();
		var oo = new TaskOneByOneByKey();
		var b = new Zeze.Util.Benchmark();
		var taskCounter = new AtomicInteger();
		var taskAwaiter = new CountDownLatch(exeCount);
		for (int i = 0; i < concurrency; i++)
			runCyclicBarrier(oo, taskCounter, taskAwaiter);
		var f = Task.scheduleUnsafe(60_000, () -> {
			System.out.println("taskIds: " + taskIds);
			System.out.println("dump:\n" + oo);
		});
		taskAwaiter.await();
		f.cancel(false);
		b.report("TestTaskOneByOne.testCyclicBarrier", exeCount);
	}

	private static void runCyclicBarrier(TaskOneByOneByKey2 oo, AtomicInteger taskCounter, CountDownLatch taskAwaiter) {
		int taskId = taskCounter.incrementAndGet();
		if (taskId > exeCount)
			return;
		var rand = ThreadLocalRandom.current();
		var roleIds = new ArrayList<Long>(keyCount);
		for (int i = 0; i < keyCount; i++)
			roleIds.add((long)rand.nextInt(roleCount));
		// System.out.println("+ B-" + taskId + ": " + roleIds);
		taskIds.add(taskId);
		oo.executeCyclicBarrier(roleIds, "B-" + taskId, () -> {
			// System.out.println("- B-" + taskId + ": " + roleIds);
			taskIds.remove(taskId);
			taskAwaiter.countDown();
			runCyclicBarrier(oo, taskCounter, taskAwaiter);
		}, DispatchMode.Normal);
	}

	@Test
	public void testCyclicBarrier2() throws InterruptedException {
		Task.tryInitThreadPool();
		taskIds.clear();
		var oo = new TaskOneByOneByKey2();
		var b = new Zeze.Util.Benchmark();
		var taskCounter = new AtomicInteger();
		var taskAwaiter = new CountDownLatch(exeCount);
		for (int i = 0; i < concurrency; i++)
			runCyclicBarrier(oo, taskCounter, taskAwaiter);
		var f = Task.scheduleUnsafe(60_000, () -> {
			System.out.println("taskIds: " + taskIds);
			System.out.println("dump:\n" + oo);
		});
		taskAwaiter.await();
		f.cancel(false);
		b.report("TestTaskOneByOne.testCyclicBarrier2", exeCount);
	}
}
