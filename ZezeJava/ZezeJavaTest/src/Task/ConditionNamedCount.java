package Task;

import Zeze.Game.Condition;
import Zeze.Game.ConditionEvent;
import Zeze.Transaction.Bean;

public class ConditionNamedCount extends Condition {
	private String name;
	private int count;
	private int targetCount;

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
	public boolean accept(ConditionEvent event) {
		return false;
	}
	@Override
	public boolean isDone() {
		return false;
	}

	public static class Event extends ConditionEvent {
		private String name;

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
