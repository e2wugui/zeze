package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BTConditionReachNPC;
import Zeze.Builtin.Game.TaskBase.BTConditionSubmitItem;
import Zeze.Builtin.Game.TaskBase.BTConditionSubmitItemEvent;
import Zeze.Builtin.Game.TaskBase.BTaskEvent;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;
import Zeze.Transaction.Bean;

public class ConditionSubmitItem extends TaskConditionBase<BTConditionSubmitItem, BTConditionSubmitItemEvent> {

	@Override
	public boolean isCompleted() {
		return false;
	}

	@Override
	public boolean accept(Bean eventBean) throws Throwable {
		if (!(eventBean instanceof BTConditionSubmitItem))
			return false;
		if (isCompleted())
			onComplete();
		return true;
	}

	public ConditionSubmitItem(TaskPhase phase) {
		super(phase, BTConditionSubmitItem.class, BTConditionSubmitItemEvent.class);
	}
}
