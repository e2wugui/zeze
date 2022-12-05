package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BTConditionSubmitItem;
import Zeze.Builtin.Game.TaskBase.BTConditionSubmitItemEvent;
import Zeze.Builtin.Game.TaskBase.BTaskEvent;
import Zeze.Game.TaskConditionBase;

public class ConditionSubmitItem extends TaskConditionBase<BTConditionSubmitItem, BTConditionSubmitItemEvent> {

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public boolean accept(BTaskEvent eventBean) {
		return false;
	}

	public ConditionSubmitItem() {
		super(BTConditionSubmitItem.class, BTConditionSubmitItemEvent.class);
	}
}
