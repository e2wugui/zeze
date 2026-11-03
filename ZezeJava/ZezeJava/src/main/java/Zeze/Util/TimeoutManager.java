package Zeze.Util;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class TimeoutManager {
	private final Concurrent[] concurrents = new Concurrent[16];
	private final AtomicInteger concurrentIdx = new AtomicInteger();
	private @Nullable volatile Worker worker;

	static class Concurrent extends ReentrantLock {
		private final LongHashMap<TimeoutSet> indexedMap = new LongHashMap<>();
		private final PriorityQueue<TimeoutSet> bucketQueue = new PriorityQueue<>();

		Timeout schedule(Worker worker, long timeoutMs, Action0 action0, Func2<TimeoutSet, Action0, Timeout> timeoutFactory) {
			if (timeoutMs < 0)
				throw new IllegalArgumentException("timeoutMs < 0");
			lock();
			try {
				var indexId = Math.ceilDiv(System.currentTimeMillis() + timeoutMs, 1000) * 1000;
				var bucketSet = indexedMap.computeIfAbsent(indexId, (key) -> {
					var set = new TimeoutSet(indexId);
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
				lock();
				TimeoutSet head = null;
				var nowMs = System.currentTimeMillis();
				try {
					head = bucketQueue.peek();
					if (null == head)
						return Long.MAX_VALUE;
					if (head.id > nowMs)
						return head.id - nowMs;
					bucketQueue.poll();
					indexedMap.remove(head.id);
				} finally {
					unlock();
				}
				head.run();
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

	static class TimeoutSet implements Comparable<TimeoutSet> {
		private final long id;
		private final HashSet<Timeout> timeouts = new HashSet<>();
		private final ReentrantLock lock = new ReentrantLock();
		private boolean polled = false;

		TimeoutSet(long id) {
			this.id = id;
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
				TaskSpec.ofAction(timeout).executeSystemOneByOne();
			}
		}

		@Override
		public int compareTo(TimeoutSet o) {
			return Long.compare(id, o.id);
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
		Worker w = worker;
		if (null == w)
			throw new IllegalStateException("worker has not been set");
		// unsigned mod % 16
		return concurrents[concurrentIdx.getAndIncrement() & 15].schedule(w, timeoutMs, action0, timeoutFactory);
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
				var idleMs = Long.MAX_VALUE;
				for (var c : TimeoutManager.this.concurrents) {
					var delay = c.tick();
					if (delay < idleMs) {
						idleMs = delay;
					}
				}
				sleepIdle(idleMs);
			}
		}
	}
}
