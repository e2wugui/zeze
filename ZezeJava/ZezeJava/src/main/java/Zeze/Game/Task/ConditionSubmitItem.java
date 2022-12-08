package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BTConditionSubmitItem;
import Zeze.Builtin.Game.TaskBase.BTConditionSubmitItemEvent;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;
import Zeze.Transaction.Bean;

public class ConditionSubmitItem extends TaskConditionBase<BTConditionSubmitItem, BTConditionSubmitItemEvent> {
	// @formatter:off
	public ConditionSubmitItem(TaskPhase phase) { super(phase, BTConditionSubmitItem.class, BTConditionSubmitItemEvent.class); }

	@Override
	public boolean accept(Bean eventBean) throws Throwable {
		if (!(eventBean instanceof BTConditionSubmitItem))
			return false;

		var bean = getExtendedBean();
		var submitItemEventBean = (BTConditionSubmitItem)eventBean;
		for (var item : submitItemEventBean.getItems()) {
			bean.getItemsSubmitted().put(item.getKey(), item.getValue());
		}

		if (isCompleted())
			onComplete();
		return true;
	}

	@Override
	public boolean isCompleted() {
		var bean = getExtendedBean();
		for (var item : bean.getItems())
			if (bean.getItemsSubmitted().get(item.getKey()) < item.getValue())
				return false;
		return false;
	}

	// @formatter:on
}
