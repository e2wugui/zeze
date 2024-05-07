package Zeze.Services.ServiceManager;

import java.util.concurrent.atomic.AtomicLong;

public class TidCache {
	private final String name;
	private final AbstractAgent agent;
	private final long start;
	private final AtomicLong current;
	private final long end;

	public TidCache(String name, AbstractAgent agent, long start, int count) {
		this.name = name;
		this.agent = agent;
		this.start = start;
		this.current = new AtomicLong(start);
		this.end = start + count;
	}

	public String getName() {
		return name;
	}

	public AbstractAgent getAgent() {
		return agent;
	}

	public long get() {
		return current.get();
	}

	public long getStart() {
		return start;
	}

	public static final int ALLOCATE_COUNT_MIN = 64;
	public static final int ALLOCATE_COUNT_MAX = 1024 * 1024;

	public int allocateCount() {
		var cur = current.get();
		var allocated = (int)(cur - start);
		var count = (int)(end - start);
		var half = count >> 1;
		if (allocated < half)
			return Math.max(ALLOCATE_COUNT_MIN, half);
		return Math.min(ALLOCATE_COUNT_MAX, count * 2);
	}

	public long next() {
		for (; ; ) {
			var id = current.get();
			if (id < end) {
				if (current.compareAndSet(id, id + 1))
					return id;
				continue;
			}
			break;
		}
		// 递归！
		return agent.allocateTidCacheFuture(name).get().next();
	}
}
