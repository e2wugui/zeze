package Zeze.Services.ServiceManager;

import Zeze.Util.FastLock;
import Zeze.Util.Id128;

public class Tid128Cache extends FastLock {
	private final String name;
	private final AbstractAgent agent;
	private final Id128 start;
	private Id128 current;
	private final Id128 end;
	private final int count;
	private volatile int allocated;

	public Tid128Cache(String name, AbstractAgent agent, Id128 start, int count) {
		this.name = name;
		this.agent = agent;
		this.start = start;
		this.current = start;
		this.end = start.add(count);
		this.count = count;
	}

	public String getName() {
		return name;
	}

	public AbstractAgent getAgent() {
		return agent;
	}

	public Id128 get() {
		return current;
	}

	public Id128 getStart() {
		return start;
	}

	public static final int ALLOCATE_COUNT_MIN = 16;
	public static final int ALLOCATE_COUNT_MAX = 1024 * 1024;

	public int allocateCount() {
		var half = count >> 1;
		var tmp = allocated;
		if (tmp < half)
			return Math.max(ALLOCATE_COUNT_MIN, half);
		return Math.min(ALLOCATE_COUNT_MAX, count * 2);
	}

	public Id128 next() {
		lock();
		try {
			if (current.compareTo(end) < 0) {
				current = current.add(1);
				allocated += 1; // 这个在锁内了,还警告啊.
				return current;
			}
		} finally {
			unlock();
		}
		// 递归！
		return agent.allocateTid128CacheFuture(name).get().next();
	}
}
