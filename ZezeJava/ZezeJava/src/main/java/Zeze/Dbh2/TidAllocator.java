package Zeze.Dbh2;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class TidAllocator extends ReentrantLock {
	private volatile Range range; // 只有 raft 修改，单线程。
	private static final int ALLOCATE_COUNT_MIN = 64;
	private static final int ALLOCATE_COUNT_MAX = 1024 * 1024;
	private int allocateCount = ALLOCATE_COUNT_MIN;
	private long lastAllocateTime = System.currentTimeMillis();

	private void calcAllocateCount() {
		var now = System.currentTimeMillis();
		var diff = now - lastAllocateTime;
		lastAllocateTime = now;
		long newCount = allocateCount;
		if (diff < 30 * 1000) // 30 seconds
			newCount <<= 1;
		else if (diff > 120 * 1000) // 120 seconds
			newCount >>= 1;
		else
			return;
		allocateCount = (int)Math.min(Math.max(newCount, ALLOCATE_COUNT_MIN), ALLOCATE_COUNT_MAX);
	}

	public long next(Dbh2StateMachine stateMachine) {
		while (true) {
			var localRange = range;
			if (localRange != null) {
				var next = localRange.tryNextId();
				if (next != 0)
					return next; // allocate in range success
			}
			lock();
			try {
				//noinspection NumberEquality
				if (range != localRange)
					continue; // 可能有并发的分配已经完成。

				calcAllocateCount();
				var log = new LogAllocateTid(allocateCount);
				stateMachine.getRaft().appendLog(log);
				// log.apply 里面会生成新的Range。
			} finally {
				unlock();
			}
		}
	}

	void setRange(long start, long end) {
		this.range = new Range(start, end);
	}

	private static final class Range extends AtomicLong {
		private final long max;

		public long tryNextId() {
			var nextId = incrementAndGet(); // 可能会超过max,但通常不会超出很多,更不可能溢出long最大值
			return nextId <= max ? nextId : 0;
		}

		// 分配范围: [start+1,end]
		public Range(long start, long end) {
			super(start);
			max = end;
		}
	}
}
