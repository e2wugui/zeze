package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BCollectCoinTask;
import Zeze.Builtin.Game.TaskBase.BCollectCoinEvent;
import Zeze.Builtin.Game.TaskBase.BTConditionNPCTalkEvent;
import Zeze.Builtin.Game.TaskBase.BTaskEvent;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;
import Zeze.Transaction.Bean;

public class ConditionNamedCount extends TaskConditionBase<BCollectCoinTask, BCollectCoinEvent> {

	@Override
	public boolean accept(Bean eventBean) throws Throwable {
		if (!(eventBean instanceof BCollectCoinEvent e))
			return false;
		if (isCompleted())
			onComplete();
		return true;
	}

	@Override
	public boolean isCompleted() {
		return getCurrentCount() >= getTargetCount();
	}

	public ConditionNamedCount(TaskPhase phase, String name, long currentCount, long targetCount) {
		super(phase, BCollectCoinTask.class, BCollectCoinEvent.class);
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
