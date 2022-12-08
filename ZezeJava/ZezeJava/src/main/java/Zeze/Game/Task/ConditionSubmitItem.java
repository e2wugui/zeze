package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BTConditionSubmitItem;
import Zeze.Builtin.Game.TaskBase.BTConditionSubmitItemEvent;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;
import Zeze.Transaction.Bean;

public class ConditionSubmitItem extends TaskConditionBase<BTConditionSubmitItem, BTConditionSubmitItemEvent> {
	// @formatter:off
	protected static class Opt extends TaskConditionBase.Opt {}
	public ConditionSubmitItem(TaskPhase phase, Opt opt) { super(phase, opt); }

	@Override
	public boolean accept(Bean eventBean) throws Throwable {
		if (!(eventBean instanceof BTConditionSubmitItemEvent))
			return false;

		var bean = getExtendedBean();
		var submitItemEventBean = (BTConditionSubmitItemEvent)eventBean;
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
