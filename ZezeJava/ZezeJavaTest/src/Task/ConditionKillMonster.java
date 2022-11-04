package Task;

import Zeze.Game.Condition;
import Zeze.Game.ConditionEvent;

public class ConditionKillMonster extends Condition {
	public ConditionKillMonster() {}

	public int MonsterId;
	public int Count;

	@Override
	public boolean accept(ConditionEvent event) {
		return false;
	}

	@Override
	public boolean isDone() {
		return false;
	}
}
