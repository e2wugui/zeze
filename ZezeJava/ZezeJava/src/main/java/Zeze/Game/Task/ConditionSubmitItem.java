package Zeze.Game.Task;

import javax.json.JsonObject;
import Zeze.Builtin.Game.TaskBase.BTConditionSubmitItem;
import Zeze.Builtin.Game.TaskBase.BTConditionSubmitItemEvent;
import Zeze.Builtin.Game.TaskBase.BTaskCondition;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;
import Zeze.Transaction.Bean;

public class ConditionSubmitItem extends TaskConditionBase<BTConditionSubmitItem, BTConditionSubmitItemEvent> {
	// @formatter:off
	public ConditionSubmitItem(TaskPhase phase) { super(phase); }

	@Override
	public boolean accept(Bean eventBean) throws Throwable {
		if (!(eventBean instanceof BTConditionSubmitItemEvent))
			return false;

		var bean = getExtendedBean();
		var submitItemEventBean = (BTConditionSubmitItemEvent)eventBean;
		for (var item : submitItemEventBean.getItems()) {
			bean.getItemsSubmitted().put(item.getKey(), item.getValue());
		}
		return true;
	}

	@Override
	public boolean isCompleted() {
		var bean = getExtendedBean();
		for (var item : bean.getItems())
			if (bean.getItemsSubmitted().get(item.getKey()) < item.getValue())
				return false;
		return true;
	}
	@Override
	protected void loadBeanExtended(BTaskCondition bean) {

	}
	@Override
	public String getType() {
		return "SubmitItem";
	}
	@Override
	protected void loadJsonExtended(JsonObject json) {

	}
	// @formatter:on
}
