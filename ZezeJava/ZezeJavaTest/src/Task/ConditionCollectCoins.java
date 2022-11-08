package Task;

import Zeze.Game.Condition;
import Zeze.Game.ConditionEvent;

public class ConditionCollectCoins extends Condition {
	@Override
	public boolean accept(ConditionEvent event) {
		if (event instanceof ConditionEventCollectCoins e) {
			return e.getCoins() >= 100;
		}
		return false;
	}

	@Override
	public boolean isDone() {
		return false;
	}
}
