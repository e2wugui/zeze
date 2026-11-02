package Zeze.Util;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class TimeoutManager {
	private final Concurrent[] concurrents = new Concurrent[16];
	private final AtomicInteger concurrentIdx = new AtomicInteger();
	private @Nullable Worker worker;

	static class Concurrent extends ReentrantLock {
		private final LongHashMap<TimeoutSet> indexedMap = new LongHashMap<>();
		private final PriorityQueue<TimeoutSet> bucketQueue = new PriorityQueue<>();

		public Timeout schedule(Worker worker, long timeoutMs, Action0 action) {
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
				var timeout = new Timeout(bucketSet, action);
				bucketSet.add(timeout);
				return timeout;
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
		private final HashSet<Timeout> set = new HashSet<>();
		private final ReentrantLock lock = new ReentrantLock();
		private boolean triggered = false;

		TimeoutSet(long id) {
			this.id = id;
		}

		void add(Timeout t) {
			lock.lock();
			try {
				if (!triggered)
					set.add(t);
			} finally {
				lock.unlock();
			}
		}

		void remove(Timeout t) {
			lock.lock();
			try {
				if (!triggered)
					set.remove(t);
			} finally {
				lock.unlock();
			}
		}

		void run() {
			lock.lock();
			try {
				triggered = true;
			} finally {
				lock.unlock();
			}
			// 本集合已经摘除，不需要加锁。
			for (var timeout : set) {
				TaskSpec.ofAction(timeout.action0).executeSystemOneByOne();
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
		synchronized (this) {
			var w = worker;
			if (null == w)
				throw new IllegalArgumentException("worker has not been set");
		}
		// unsigned mod % 16
		return concurrents[concurrentIdx.getAndIncrement() & 15].schedule(w, timeoutMs, action0);
	}

	public synchronized void start() {
		if (worker != null) {
			return;
		}
		worker = new Worker();
	}

	public synchronized void stop() {
		var w = worker;
		if (w != null) {
			w.shutdown();
			worker = null;
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
