package UnitTest.Zeze.Game.MyConditions;

import javax.json.JsonObject;
import TaskTest.TaskExt.BTConditionExploreWorld;
import TaskTest.TaskExt.BTConditionExploreWorldEvent;
import Zeze.Builtin.Game.TaskBase.BTaskCondition;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;
import Zeze.Transaction.Bean;

public class ConditionExploreWorld extends TaskConditionBase<BTConditionExploreWorld, BTConditionExploreWorldEvent> {
	// @formatter:off
	public ConditionExploreWorld(TaskPhase phase) {
		super(phase);
	}

	@Override
	public boolean accept(Bean eventBean) throws Exception {
		if (!(eventBean instanceof BTConditionExploreWorld))
			return false;

		if (((BTConditionExploreWorld)eventBean).getExploreRate() >= getExtendedBean().getExploreRate()) {
			getExtendedBean().setFinished(true);
			return true;
		}

		return true;
	}

	@Override
	public boolean isCompleted() { return getExtendedBean().isFinished(); }

	@Override
	protected void loadBeanExtended(BTaskCondition bean) {

	}
	@Override
	protected void loadJsonExtended(JsonObject json) {

	}@Override
	public String getType() {
		return null;
	}

	// @formatter:on
}
