package Zeze.Util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

/** 分片时间桶超时管理器：基于 System.nanoTime() 单调时钟按秒桶汇聚调度超时回调。 */
public class TimeoutManager {
	private static final Logger logger = LogManager.getLogger(TimeoutManager.class);
	// 基于 System.nanoTime() 单调时钟按秒分桶，免疫墙钟回拨
	private static final long BUCKET_NS = 1_000_000_000L;

	private final Concurrent[] concurrents = new Concurrent[16];
	private @Nullable volatile Worker worker;

	class Concurrent extends ReentrantLock {
		private final LongHashMap<TimeoutSet> indexedMap = new LongHashMap<>(1024);
		private final PriorityQueue<TimeoutSet> bucketQueue = new PriorityQueue<>();
		// Worker 锁外更新、schedule 线程读，需 volatile 保证可见性
		private volatile int timeoutSetInitialCapacity = 1024;

		@Nullable Timeout schedule(Worker expectedWorker, long timeoutMs, Action0 action0,
								   Func2<TimeoutSet, Action0, Timeout> timeoutFactory) {
			// 向上取整到秒桶边界（只晚不早）；入口已限制超时上限，算术不会溢出
			var indexId = Math.ceilDiv(System.nanoTime() + timeoutMs * 1_000_000L, BUCKET_NS) * BUCKET_NS;

			lock();
			try {
				// stop()先撤下worker，再逐片清理。旧代不得在清理完成后插入新桶。
				if (worker != expectedWorker)
					return null;
				var bucketSet = indexedMap.computeIfAbsent(indexId, (key) -> {
					var set = new TimeoutSet(indexId, timeoutSetInitialCapacity);
					bucketQueue.offer(set);
					expectedWorker.wakeup();
					return set;
				});
				var timeout = timeoutFactory.call(bucketSet, action0);
				bucketSet.add(timeout);
				return timeout;
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				unlock();
			}
		}

		long tick() {
			while (true) {
				TimeoutSet head;
				var nowNs = System.nanoTime();
				lock();
				try {
					head = bucketQueue.peek();
					if (null == head)
						return Long.MAX_VALUE;
					// 差值比较在 long 模运算下回绕安全
					var remainingNs = head.id - nowNs;
					if (remainingNs > 0)
						return Math.ceilDiv(remainingNs, 1_000_000L); // 向上取整转 ms：宁可晚醒不早醒
					bucketQueue.poll();
					indexedMap.remove(head.id);
				} finally {
					unlock();
				}
				var peak = head.run();

				// 按 0.75 负载因子由上桶峰值折算下桶初始容量，钳位到 [1024, 1M]
				timeoutSetInitialCapacity = (int)Math.clamp(peak * 4L / 3 + 1, 1024L, 1024L * 1024L);
			}
		}

		void clear() {
			lock();
			try {
				indexedMap.clear();
				bucketQueue.clear();
			} finally {
				unlock();
			}
		}

		int pendingSize() {
			lock();
			try {
				int total = 0;
				for (var set : bucketQueue)
					total += set.size();
				return total;
			} finally {
				unlock();
			}
		}
	}

	public static class TimeoutSet implements Comparable<TimeoutSet> {
		private final long id;
		private final HashSet<Timeout> timeouts;
		private final ReentrantLock lock = new ReentrantLock();
		private boolean polled = false;

		TimeoutSet(long id, int timeoutSetInitialCapacity) {
			this.id = id;
			this.timeouts = new HashSet<>(timeoutSetInitialCapacity);
		}

		private int highWaterMark; // 锁内记录在场峰值，用于估算下桶初始容量

		void add(Timeout t) {
			// add 与 poll 在 Concurrent 锁内互斥
			lock.lock();
			try {
				timeouts.add(t);
				if (timeouts.size() > highWaterMark)
					highWaterMark = timeouts.size();
			} finally {
				lock.unlock();
			}
		}

		void remove(Timeout t) {
			// remove 与 poll 可并发，需持桶锁并检查 polled
			lock.lock();
			try {
				if (!polled)
					timeouts.remove(t);
			} finally {
				lock.unlock();
			}
		}

		int size() {
			lock.lock(); // 持桶锁精确读（锁序 stripe -> set，与 schedule 一致）
			try {
				return timeouts.size();
			} finally {
				lock.unlock();
			}
		}

		int run() {
			int peak;
			lock.lock();
			try {
				polled = true;
				peak = highWaterMark;
			} finally {
				lock.unlock();
			}
			// 本桶已在 Concurrent 锁内摘除，仅可能与 remove 并发（polled=true 已拦截修改）
			for (var timeout : timeouts) {
				TaskSpec.ofAction(timeout).dispatchMode(timeout.getDispatchMode()).executeSystemOneByOne();
			}
			timeouts.clear();
			return peak;
		}

		@Override
		public int compareTo(TimeoutSet o) {
			// 差值符号比较，保证 nanoTime 回绕下的正确顺序
			return Long.signum(id - o.id);
		}
	}

	public TimeoutManager() {
		for (int i = 0; i < 16; i++) {
			concurrents[i] = new Concurrent();
		}
	}

	public Timeout schedule(long timeoutMs, Action0 action0) {
		return schedule(timeoutMs, action0, Timeout::new);
	}

	public Timeout schedule(long timeoutMs, Action0 action0, Func2<TimeoutSet, Action0, Timeout> timeoutFactory) {
		var timeout = scheduleIfRunning(timeoutMs, action0, timeoutFactory);
		if (timeout == null)
			throw new IllegalStateException("worker has not been set");
		return timeout;
	}

	@Nullable Timeout scheduleIfRunning(long timeoutMs, Action0 action0,
									  Func2<TimeoutSet, Action0, Timeout> timeoutFactory) {
		if (timeoutMs < 0 || timeoutMs > 365L * 24 * 3600 * 1000) // 最大超时允许一年
			throw new IllegalArgumentException("timeoutMs < 0 || timeoutMs > 365days");
		Worker w = worker;
		if (null == w)
			return null;
		return concurrents[(int)(Thread.currentThread().threadId() & 15)].schedule(w, timeoutMs, action0, timeoutFactory);
	}

	/** 返回当前各分片桶内的超时条目总数（非跨分片一致快照）。 */
	public int size() {
		int total = 0;
		for (var c : concurrents)
			total += c.pendingSize();
		return total;
	}

	public synchronized void start() {
		if (worker != null) {
			return;
		}
		var w = new Worker();
		w.start();
		worker = w;
	}

	public synchronized void stop() {
		var w = worker;
		worker = null;
		if (w != null) {
			w.shutdown();
			for (var c : concurrents) {
				c.clear();
			}
		}
	}

	class Worker extends ThreadHelper {

		public Worker() {
			super("TimeoutManager.Worker");
		}

		public void run() {
			while (isRunning()) {
				try {
					var idleMs = Long.MAX_VALUE;
					for (var c : TimeoutManager.this.concurrents) {
						var delay = c.tick();
						if (delay < idleMs) {
							idleMs = delay;
						}
					}
					sleepIdle(idleMs);
					continue;
				} catch (Throwable e) {
					logger.error("", e);
				}
				sleepIdle(60_000); // 异常时，固定休眠1分钟。
			}
		}
	}
}
