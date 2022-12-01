package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BCollectCoinTask;
import Zeze.Builtin.Game.TaskBase.BCollectCoinEvent;
import Zeze.Game.TaskConditionBase;
import Zeze.Transaction.Bean;

public class ConditionNamedCount extends TaskConditionBase<BCollectCoinTask, BCollectCoinEvent> {

	// @formatter:off
	@Override
	public boolean accept(Bean eventBean) {
		if (eventBean instanceof BCollectCoinEvent e) {
			if (e.getName().equals(getExtendedBean().getName())) {
				return e.getCoinCount() >= getExtendedBean().getTargetCoinCount();
			}
		}
		return false;
	}
	@Override
	public boolean isDone() {
		return getCurrentCount() >= getTargetCount();
	}

	// @formatter:on
	public ConditionNamedCount(String name, long currentCount, long targetCount) {
		super(BCollectCoinTask.class, BCollectCoinEvent.class);
		getExtendedBean().setName(name);
		getExtendedBean().setCurrentCoinCount(currentCount);
		getExtendedBean().setTargetCoinCount(targetCount);
	}

	public long getCurrentCount() {
		return getExtendedBean().getCurrentCoinCount();
	}

	public long getTargetCount() {
		return getExtendedBean().getTargetCoinCount();
	}
	@Override
	public String getName() {
		return getExtendedBean().getName();
	}
}
