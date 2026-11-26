package Zeze.Util;

import harness.Fast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import harness.Fast;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Transaction.DispatchMode;
import org.junit.jupiter.api.Test;

/**
 * soak-20261125-1807 R4 bench挂死复现试验（立案修复用）：
 * bench环境=VT-per-task执行器（Task.newFixedThreadPool在USE_UNLIMITED_VIRTUAL_THREAD下返回
 * newThreadPerTaskExecutor），BenchTaskOneByOne.testCyclicBarrier2在该执行器上出现
 * CountDownLatch永久短缺（1/10轮频率，挂死窗口零错误日志）。
 * 本测试以完全同形参数（VT executor + 20路递归cyclic + 1000角色空间 + 1024桶）反复锤击，
 * 并叠加并发压力（模拟8个bench类并行的VT风暴）。超时即取证：桶队列内容(toString)、
 * latch余量、在飞taskId集，失败时全部输出供定位丢失的完成回调。
 */
@Fast
public class TestKeyedQueueCyclicVtStress {
	private static final int Iterations = 30;
	private static final int ExeCount = 2000;
	private static final int KeyCount = 20;
	private static final int RoleCount = 1000;
	private static final int ChainConcurrency = 20;
	private static final int AwaitSeconds = 15;

	/**
	 * R4现场形态复刻：全迭代共享一个VT-per-task执行器（对齐生产ZezeTaskPool全局池），
	 * 每迭代先以testBenchmark2形态（单key队列海灌普通任务）制造VT创建风暴，再让
	 * 8个并行"bench类"（各自队列实例+20路循环屏障）同时在同一执行器上运行。
	 */
	@Test
	public void cyclicBarrierSharedExecutorStormAndParallelClasses() throws Exception {
		var factory = Thread.ofVirtual().factory();
		var executor = Executors.newThreadPerTaskExecutor(factory);
		try {
			for (int loop = 0; loop < 150; loop++) {
				final int iter = loop;
				// phase A: 单key VT风暴（testBenchmark2缩减形态，25万任务）
				var stormQueue = new TaskOneByOneByKey2(1024, executor);
				for (int i = 0; i < 250_000; i++)
					TaskSpec.ofAction(() -> { }).executeOneByOne(1, stormQueue);

				// phase B: 8个并行类，每个20路循环屏障，全部完成后才进下一迭代
				var classDone = new CountDownLatch(8);
				var firstFailure = new AtomicReference<Throwable>();
				for (int c = 0; c < 8; c++) {
					Thread.ofPlatform().start(() -> {
						try {
							oneIteration(executor, iter);
						} catch (Throwable t) {
							firstFailure.compareAndSet(null, t);
						} finally {
							classDone.countDown();
						}
					});
				}
				if (!classDone.await(30, TimeUnit.SECONDS) || firstFailure.get() != null) {
					var f = firstFailure.get();
					if (f instanceof AssertionError ae)
						throw ae;
					throw new AssertionError("iter=" + iter + " classes unfinished="
							+ classDone.getCount(), f);
				}
			}
		} finally {
			executor.shutdown();
		}
	}

	@Test
	public void cyclicBarrierUnderVtPerTaskExecutorCompletesAllCycles() throws Exception {
		var factory = Thread.ofVirtual().factory();
		for (int iter = 0; iter < Iterations; iter++) {
			var executor = Executors.newThreadPerTaskExecutor(factory);
			try {
				oneIteration(executor, iter);
			} finally {
				executor.shutdown();
			}
		}
	}

	@Test
	public void cyclicBarrierUnderVtPerTaskExecutorWithPressureCompletesAllCycles() throws Exception {
		var factory = Thread.ofVirtual().factory();
		for (int iter = 0; iter < Iterations; iter++) {
			var executor = Executors.newThreadPerTaskExecutor(factory);
			try {
				// 并发压力：6个独立队列实例模拟8个bench类并行的VT创建风暴，
				// 每个实例持续灌5万普通任务（bench的testBenchmark2量级缩减版）。
				var stop = new CountDownLatch(1);
				var pressures = new ArrayList<Thread>();
				for (int p = 0; p < 6; p++) {
					var queue = new TaskOneByOneByKey2(1024, executor);
					var t = Thread.ofVirtual().unstarted(() -> {
						try {
							for (int i = 0; i < 50_000 && stop.getCount() > 0; i++)
								TaskSpec.ofAction(() -> { }).executeOneByOne(i, queue);
						} catch (Throwable ignored) {
						}
					});
					t.start();
					pressures.add(t);
				}
				try {
					oneIteration(executor, iter);
				} finally {
					stop.countDown();
					for (var t : pressures)
						t.join(5000);
				}
			} finally {
				executor.shutdown();
			}
		}
	}

	/**
	 * 双驱动poll碰撞丢失回归（soak-20261125-1807 R4 bench挂死根因）：屏障完成的续轮runNext
	 * 会在旧驱动存活期创建第二个Dispatch（Dispatch的CAS只防同票据重入），旧循环在
	 * peek→loop-update poll窗口被并发poll穿洞时丢弃真实头部、处理陈旧peek引用——
	 * 双执行peek任务+永久丢失被poll走却无人引用的任务（丢失屏障任务=计数永不归零=挂死）。
	 * 单桶+单key屏障（每次reach立即完成并续轮=最大化第二驱动创建率）+让出CPU的任务体
	 * （扩大碰撞窗口），断言普通任务与屏障任务执行数==提交数（不丢不重）。
	 */
	@Test
	public void concurrentDriverPollCollisionLosesNothing() throws Exception {
		var executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
		try {
			var queue = new TaskOneByOneByKey2(1, executor); // 单桶：全部任务强碰撞
			var plainSubmitted = new AtomicLong();
			var plainExecuted = new AtomicInteger();
			var barrierCalls = new AtomicLong();
			var barrierBodies = new AtomicInteger();
			var stop = new CountDownLatch(1);
			var submitter = Thread.ofPlatform().start(() -> {
				while (stop.getCount() > 0) {
					barrierCalls.incrementAndGet();
					queue.executeCyclicBarrier(List.of(1), "b", barrierBodies::incrementAndGet,
							DispatchMode.Normal);
					plainSubmitted.incrementAndGet();
					TaskSpec.ofAction(() -> {
						Thread.yield(); // 扩大旧循环peek→poll窗口，放大双驱动碰撞率
						plainExecuted.incrementAndGet();
					}).executeOneByOne(1, queue);
				}
			});
			stop.await(5, TimeUnit.SECONDS);
			stop.countDown();
			submitter.join(5000);
			// 排空判据=队列清空+计数双稳定（soak-20261126-0519批R3/R4/R5红教训：百万级积压下
			// 仅等计数稳定会在排空途中误断"丢失"——差距4%~63%波动即积压未排空假象）。
			// 队列非空超时=真楔死（另一失败形态）；队列空但计数不等=真丢失/重复（产品侧红）。
			long lastPlain = -1;
			int lastBarrier = -1;
			boolean drained = false;
			for (int i = 0; i < 1200; i++) {
				Thread.sleep(100);
				var qs = queue.getQueueSize(0);
				if (qs == 0 && plainExecuted.get() == lastPlain && barrierBodies.get() == lastBarrier) {
					drained = true;
					break;
				}
				lastPlain = plainExecuted.get();
				lastBarrier = barrierBodies.get();
			}
			assertEquals(0, queue.getQueueSize(0), "queue not drained (wedged): residual="
					+ queue.getQueueSize(0));
			assertTrue(drained, "drain wait timed out");
			assertEquals(plainSubmitted.get(), plainExecuted.get(), "plain tasks lost or duplicated");
			assertEquals(barrierCalls.get(), barrierBodies.get(), "barrier bodies lost or duplicated");
		} finally {
			executor.shutdown();
		}
	}

	private void oneIteration(ExecutorService executor, int iter) throws InterruptedException {
		var oo = new TaskOneByOneByKey2(1024, executor);
		var taskIds = new ConcurrentSkipListSet<Integer>();
		var taskCounter = new AtomicInteger();
		var taskAwaiter = new CountDownLatch(ExeCount);
		for (int i = 0; i < ChainConcurrency; i++)
			runCyclicBarrier(oo, taskCounter, taskIds, taskAwaiter);
		if (!taskAwaiter.await(AwaitSeconds, TimeUnit.SECONDS)) {
			var residual = taskCounter.get();
			var sb = new StringBuilder();
			sb.append("cycle hang: iter=").append(iter)
					.append(" latch=").append(taskAwaiter.getCount())
					.append(" taskCounter=").append(residual)
					.append(" inFlight=").append(taskIds.size());
			var q = oo.toString();
			if (q.length() > 4000)
				q = q.substring(0, 4000) + "...(truncated)";
			sb.append('\n').append(q);
			throw new AssertionError(sb.toString());
		}
	}

	private static void runCyclicBarrier(TaskOneByOneByKey2 oo, AtomicInteger taskCounter,
										 ConcurrentSkipListSet<Integer> taskIds, CountDownLatch taskAwaiter) {
		int taskId = taskCounter.incrementAndGet();
		if (taskId > ExeCount)
			return;
		var roleIds = new ArrayList<Long>(KeyCount);
		for (int i = 0; i < KeyCount; i++)
			roleIds.add((long) ThreadLocalRandom.current().nextInt(RoleCount));
		taskIds.add(taskId);
		oo.executeCyclicBarrier(roleIds, "B-" + taskId, () -> {
			taskIds.remove(taskId);
			taskAwaiter.countDown();
			runCyclicBarrier(oo, taskCounter, taskIds, taskAwaiter);
		}, DispatchMode.Normal);
	}
}
