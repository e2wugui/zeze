package Zeze.Services.ServiceManager;

public final class AutoKey {
	private final String name;
	private final Agent agent;
	private int count;
	private long current;

	public AutoKey(String name, Agent agent) {
		this.name = name;
		this.agent = agent;
	}

	public String getName() {
		return name;
	}

	public Agent getAgent() {
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
			allocate();
			if (count <= 0)
				throw new IllegalStateException("AllocateId failed for " + name);
		}

		count--;
		return current++;
	}

	private void allocate() {
		var r = new AllocateId();
		r.Argument.setName(name);
		r.Argument.setCount(1024);
		r.SendAndWaitCheckResultCode(agent.getClient().getSocket());
		current = r.Result.getStartId();
		count = r.Result.getCount();
	}
}
