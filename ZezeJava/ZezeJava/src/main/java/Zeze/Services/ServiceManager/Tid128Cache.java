package Zeze.Services.ServiceManager;

import Zeze.Util.FastLock;
import Zeze.Util.Id128;
import org.jetbrains.annotations.NotNull;

public class Tid128Cache extends FastLock {
	public static final int ALLOCATE_COUNT_MIN = 16;
	public static final int ALLOCATE_COUNT_MAX = 1024 * 1024;

	private final @NotNull String name;
	private final @NotNull AbstractAgent agent;
	private final @NotNull Id128 start;
	private @NotNull Id128 current;
	private final @NotNull Id128 end;
	private final int count;
	private volatile int allocated;

	public Tid128Cache(@NotNull String name, @NotNull AbstractAgent agent, @NotNull Id128 start, int count) {
		this.name = name;
		this.agent = agent;
		this.start = start;
		this.current = start;
		this.end = start.add(count);
		this.count = count;
	}

	public @NotNull String getName() {
		return name;
	}

	public @NotNull AbstractAgent getAgent() {
		return agent;
	}

	public @NotNull Id128 get() {
		return current;
	}

	public @NotNull Id128 getStart() {
		return start;
	}

	public int allocateCount() {
		var half = count >> 1;
		var tmp = allocated;
		if (tmp < half)
			return Math.max(ALLOCATE_COUNT_MIN, half);
		return Math.min(ALLOCATE_COUNT_MAX, count * 2);
	}

	public @NotNull Id128 next() {
		lock();
		try {
			if (current.compareTo(end) < 0) {
				current = current.add(1);
				//noinspection NonAtomicOperationOnVolatileField
				allocated++; // 这个在锁内了,还警告啊.
				return current;
			}
		} finally {
			unlock();
		}
		// 递归！
		return agent.allocateTid128CacheFuture(name).get().next();
	}
}
