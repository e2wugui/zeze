package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BTConditionSubmitItem;
import Zeze.Builtin.Game.TaskBase.BTConditionSubmitItemEvent;
import Zeze.Game.TaskConditionBase;

public class ConditionSubmitItem extends TaskConditionBase<BTConditionSubmitItem, BTConditionSubmitItemEvent> {

	@Override
	public String getName() {
		return "SubmitItem";
	}

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public boolean accept(Zeze.Transaction.Bean eventBean) {
		return false;
	}

	public ConditionSubmitItem() {
		super(BTConditionSubmitItem.class, BTConditionSubmitItemEvent.class);
	}
}
