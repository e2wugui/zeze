package Zeze.Util;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

public class TimeoutManager extends ReentrantLock {
	private final LongHashMap<TimeoutSet> indexedMap = new LongHashMap<>();
	private final PriorityQueue<TimeoutSet> bucketQueue = new PriorityQueue<>();
	private @Nullable Worker worker;

	static class TimeoutSet {
		private final long id;
		private final HashSet<Timeout> set = new HashSet<>();

		TimeoutSet(long id) {
			this.id = id;
		}

		void add(Timeout t) {
			set.add(t);
		}

		void remove(Timeout t) {
			set.remove(t);
		}

		void run() {
			for (var timeout : set) {
				timeout.run();
			}
		}
	}

	public Timeout schedule(long timeoutMs, Action0 action) {
		lock();
		try {
			if (worker == null)
				throw new IllegalStateException("Worker has not been set");
			var indexId = timeoutMs / 1000;
			var bucketSet = indexedMap.computeIfAbsent(indexId, (key) -> {
				var set = new TimeoutSet(indexId);
				bucketQueue.offer(set);
				if (null != worker) // why?
					worker.wakeup();
				return set;
			});
			Objects.requireNonNull(bucketSet); // bucketSet 可能null？ @路
			var timeout = new Timeout(bucketSet, action);
			bucketSet.add(timeout);
			return timeout;
		} finally {
			unlock();
		}

	}

	public void start() {
		lock();
		try {
			if (worker != null) {
				return;
			}
			worker = new Worker();
		} finally {
			unlock();
		}
	}

	public void stop() {
		lock();
		try {
			if (worker != null) {
				worker.shutdown();
				worker = null;
				indexedMap.clear();
				bucketQueue.clear();
			}
		} finally {
			unlock();
		}
	}

	long tick() {
		while (true) {
			lock();
			TimeoutSet head = null;
			var nowMs = System.currentTimeMillis(); // 循环外？
			try {
				head = bucketQueue.peek();
				if (null == head)
					return Long.MAX_VALUE;
				if (head.id > nowMs)
					return head.id - nowMs;
				bucketQueue.poll();
			} finally {
				unlock();
			}
			head.run();
		}
	}

	class Worker extends ThreadHelper {

		public Worker() {
			super("TimeoutManager.Worker");
		}

		public void run() {
			while (isRunning()) {
				var idleMs = TimeoutManager.this.tick();
				sleepIdle(idleMs);
			}
		}
	}
}
