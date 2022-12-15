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

	public synchronized long next() {
		if (count <= 0) {
			agent.allocate(this);
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
