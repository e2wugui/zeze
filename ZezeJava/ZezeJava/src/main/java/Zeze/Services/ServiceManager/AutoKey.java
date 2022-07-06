package Zeze.Services.ServiceManager;

public final class AutoKey {
	private final String Name;
	private final Agent Agent;
	private int Count;
	private long Current;

	public AutoKey(String name, Agent agent) {
		Name = name;
		Agent = agent;
	}

	public String getName() {
		return Name;
	}

	public Agent getAgent() {
		return Agent;
	}

	public int getCount() {
		return Count;
	}

	public long getCurrent() {
		return Current;
	}

	public synchronized long Next() {
		if (Count <= 0) {
			Allocate();
			if (Count <= 0)
				throw new IllegalStateException("AllocateId failed for " + Name);
		}

		Count--;
		return Current++;
	}

	private void Allocate() {
		var r = new AllocateId();
		r.Argument.setName(Name);
		r.Argument.setCount(1024);
		r.SendAndWaitCheckResultCode(Agent.getClient().getSocket());
		Current = r.Result.getStartId();
		Count = r.Result.getCount();
	}
}
