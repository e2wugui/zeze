package Task;

import Zeze.Game.TaskCondition;
import Zeze.Game.ConditionEvent;

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
	public boolean accept(ConditionEvent event) {
		if (event instanceof Event) {
//			var e = (Event)event;
//			if (e.getName().equals(name)) {
//				if (count + 1 >= targetCount) {
//					setDone(true);
//				}
//				return true;
//			}
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
