package Zeze.Services.ServiceManager;

public final class AutoKey {
	private final String name;
	private final AbstractAgent agent;
	private int count;
	private long current;

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

	public int getCount() {
		return count;
	}

	public long getCurrent() {
		return current;
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

	public synchronized long next() {
		adjustAllocateCount();
		if (count <= 0) {
			agent.allocate(this, allocateCount);
			if (count <= 0)
				throw new IllegalStateException("AllocateId failed for " + name);
		}

		count--;
		return current++;
	}

	void setCurrentAndCount(long current, int count) {
		this.current = current;
		this.count = count;
	}
}
