package Zeze.Util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

public class TimeoutManager {
	private static final Logger logger = LogManager.getLogger(TimeoutManager.class);
	// 时基：System.nanoTime() 单调钟，免疫 NTP 步进校正与手动改钟（墙钟会带着在途桶集体漂移）。
	// 桶粒度参数化：亚秒桶只改这一个常量并重跑验证（当前秒桶，量化语义 (timeout, timeout+1s]）。
	private static final long BUCKET_NS = 1_000_000_000L;

	private final Concurrent[] concurrents = new Concurrent[16];
	private @Nullable volatile Worker worker;

	static class Concurrent extends ReentrantLock {
		private final LongHashMap<TimeoutSet> indexedMap = new LongHashMap<>(1024);
		private final PriorityQueue<TimeoutSet> bucketQueue = new PriorityQueue<>();
		private int timeoutSetInitialCapacity = 1024;

		Timeout schedule(Worker worker, long timeoutMs, Action0 action0, Func2<TimeoutSet, Action0, Timeout> timeoutFactory) {
			// 公开入口的上界校验即溢出防线：365天×1e6 与 Long.MAX 有 292× 余量，
			// 合法输入的乘法→加法→ceil 全链路不可能溢出。
			// ceil 保证实际延迟 ≥ 请求值（只晚不早）；id 是纳秒秒界，回绕安全见 tick。
			var indexId = Math.ceilDiv(System.nanoTime() + timeoutMs * 1_000_000L, BUCKET_NS) * BUCKET_NS;

			lock();
			try {
				var bucketSet = indexedMap.computeIfAbsent(indexId, (key) -> {
					var set = new TimeoutSet(indexId, timeoutSetInitialCapacity);
					bucketQueue.offer(set);
					worker.wakeup();
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
					// 只做减法不做裸比较：long 模运算下天然回绕安全（nanoTime 原点任意且 ~292 年回绕）。
					var remainingNs = head.id - nowNs;
					if (remainingNs > 0)
						return Math.ceilDiv(remainingNs, 1_000_000L); // 余量转 ms 向上取整：宁可晚醒不早醒
					bucketQueue.poll();
					indexedMap.remove(head.id);
				} finally {
					unlock();
				}
				var size = head.size(); // run里面会clear，需要先读取。
				head.run();

				// 增长或缩减
				// todo 计算时要不要考虑HashSet的因子
				if (size > timeoutSetInitialCapacity)
					timeoutSetInitialCapacity <<= 1;
				else if (size < (timeoutSetInitialCapacity >> 1))
					timeoutSetInitialCapacity >>= 1;

				// 修订
				if (timeoutSetInitialCapacity < 1024)
					timeoutSetInitialCapacity = 1024;
				else if (timeoutSetInitialCapacity > 1024 * 1024)
					timeoutSetInitialCapacity = 1024 * 1024;
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

		int size() {
			return timeouts.size();
		}

		void add(Timeout t) {
			// add和本类被poll互斥，这里不需要保护。
			lock.lock();
			try {
				timeouts.add(t);
			} finally {
				lock.unlock();
			}
		}

		void remove(Timeout t) {
			// remove和本类poll不互斥，需要加个状态保护。
			lock.lock();
			try {
				if (!polled)
					timeouts.remove(t);
			} finally {
				lock.unlock();
			}
		}

		void run() {
			lock.lock();
			try {
				polled = true;
			} finally {
				lock.unlock();
			}
			// 本集合已经摘除，而且是在Concurrent锁内摘除的，不会和add并发。remove并发是可能的。
			for (var timeout : timeouts) {
				TaskSpec.ofAction(timeout).dispatchMode(timeout.getDispatchMode()).executeSystemOneByOne();
			}
			timeouts.clear();
		}

		@Override
		public int compareTo(TimeoutSet o) {
			// 回绕减法取符号（非 Long.compare 的线性序）：对真实差 < ±146 年的值给出正确循环序，
			// 与 tick 的减法判定一致贯彻"只做减法"铁律；活桶跨度 ≤ 最大超时一年，远在有效窗内。
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
		if (timeoutMs < 0 || timeoutMs > 365L * 24 * 3600 * 1000) // 最大超时允许一年。
			throw new IllegalArgumentException("timeoutMs < 0 || timeoutMs > 365days");
		Worker w = worker;
		if (null == w)
			throw new IllegalStateException("worker has not been set");
		// unsigned mod % 16
		return concurrents[(int)(Thread.currentThread().threadId() & 15)].schedule(w, timeoutMs, action0, timeoutFactory);
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
			w.shutdown(); // shutdown 失败，但上面worker已置空，忽略这个问题。关机过程中的错误不严格处理。
			// 下面不clear，下一次start，timeout还能继续工作，但这个有点奇怪，还是clear吧。
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
