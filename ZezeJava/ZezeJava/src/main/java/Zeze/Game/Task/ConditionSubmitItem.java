package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BTConditionSubmitItem;
import Zeze.Builtin.Game.TaskBase.BTConditionSubmitItemEvent;
import Zeze.Builtin.Game.TaskBase.BTaskEvent;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;

public class ConditionSubmitItem extends TaskConditionBase<BTConditionSubmitItem, BTConditionSubmitItemEvent> {

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public boolean accept(BTaskEvent eventBean) throws Throwable {
		return false;
	}

	public ConditionSubmitItem(TaskPhase phase) {
		super(phase, BTConditionSubmitItem.class, BTConditionSubmitItemEvent.class);
	}
}
