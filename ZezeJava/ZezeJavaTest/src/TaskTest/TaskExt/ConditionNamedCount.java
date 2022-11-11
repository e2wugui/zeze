package TaskTest.TaskExt;

import Zeze.Game.TaskCondition;
import Zeze.Game.ConditionEvent;
import Zeze.Transaction.Bean;

public class ConditionNamedCount extends TaskCondition {
	private final String name;
	private final int count;
	private final int targetCount;

	public ConditionNamedCount(String name, int targetCount) {
		this.name = name;
		this.count = 0;
		this.targetCount = targetCount;
	}

	public ConditionNamedCount(String name, int count, int targetCount) {
		this.name = name;
		this.count = count;
		this.targetCount = targetCount;
	}

	@Override
	public boolean accept(Bean eventBean) {
		if (eventBean instanceof BCollectCoinEvent) {
			// TODO:
			var e = (BCollectCoinEvent)eventBean;
			if (e.getName().equals(name)) {
				if (e.getCoinCount() >= targetCount) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean isDone() {
		return count >= targetCount;
	}

	public static class Event extends ConditionEvent {
		private final String name;

		public Event(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public String getName() {
		return name;
	}

	public int getCount() {
		return count;
	}

	public int getTargetCount() {
		return targetCount;
	}
}
