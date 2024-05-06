package Zeze.Services.ServiceManager;

import java.util.concurrent.atomic.AtomicLong;
import Zeze.Util.FastLock;

public final class AutoKey extends FastLock {
	private final String name;
	private final AbstractAgent agent;
	private final AtomicLong current = new AtomicLong();
	private volatile long end;

	public AutoKey(String name, AbstractAgent agent) {
		this.name = name;
		this.agent = agent;
	}

	public String getName() {
		return name;
	}

	public AbstractAgent getAgent() {
		return agent;
	}

	public long getCurrent() {
		return current.get();
	}

	private static final int ALLOCATE_COUNT_MIN = 64;
	private static final int ALLOCATE_COUNT_MAX = 1024 * 1024;

	private int allocateCount = ALLOCATE_COUNT_MIN;
	private long lastAllocateTime = System.currentTimeMillis();

	private void adjustAllocateCount() {
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

	public long next() {
		for (; ; ) {
			var id = current.get();
			var idEnd = end;
			if (id < idEnd) {
				if (current.compareAndSet(id, id + 1))
					return id;
				continue;
			}

			lock();
			try {
				if (idEnd == end) {
					adjustAllocateCount();
					agent.allocate(this, allocateCount);
				}
			} finally {
				unlock();
			}
		}
	}

	void setCurrentAndCount(long current, int count) {
		if (count <= 0)
			throw new IllegalStateException("count = " + count);
		end = Long.MIN_VALUE; // 确保赋值新的current和end之前不会并发分配
		this.current.set(current);
		end = current + count;
	}
}
