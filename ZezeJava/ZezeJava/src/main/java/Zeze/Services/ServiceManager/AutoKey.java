package Zeze.Services.ServiceManager;

import java.util.concurrent.atomic.AtomicLong;
import Zeze.Util.FastLock;
import Zeze.Util.TimeAdaptedFund;

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

	private final TimeAdaptedFund fund = TimeAdaptedFund.getDefaultFund();

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
					var allocateCount = fund.next();
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
