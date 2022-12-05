package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BCollectCoinTask;
import Zeze.Builtin.Game.TaskBase.BCollectCoinEvent;
import Zeze.Builtin.Game.TaskBase.BTaskEvent;
import Zeze.Game.TaskConditionBase;

public class ConditionNamedCount extends TaskConditionBase<BCollectCoinTask, BCollectCoinEvent> {

	@Override
	public boolean accept(BTaskEvent eventBean) {
		if (eventBean.getExtendedData().getBean() instanceof BCollectCoinEvent) {
			//noinspection PatternVariableCanBeUsed
			var e = (BCollectCoinEvent)eventBean.getExtendedData().getBean();
			if (e.getName().equals(getExtendedBean().getName())) {
				return e.getCoinCount() >= getExtendedBean().getTargetCoinCount();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean isDone() {
		return getCurrentCount() >= getTargetCount();
	}

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
}
