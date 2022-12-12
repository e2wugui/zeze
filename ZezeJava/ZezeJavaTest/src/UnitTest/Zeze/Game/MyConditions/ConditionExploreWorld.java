package UnitTest.Zeze.Game.MyConditions;

import TaskTest.TaskExt.BTConditionExploreWorld;
import TaskTest.TaskExt.BTConditionExploreWorldEvent;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;
import Zeze.Transaction.Bean;

public class ConditionExploreWorld extends TaskConditionBase<BTConditionExploreWorld, BTConditionExploreWorldEvent> {
	// @formatter:off
	protected static class Opt extends TaskConditionBase.Opt {
		double exploreRate;
	}
	public ConditionExploreWorld(TaskPhase phase, Opt opt) {
		super(phase, opt);
		var bean = getExtendedBean();
		bean.setExploreRate(opt.exploreRate);
		bean.setFinished(false);
	}

	@Override
	public boolean accept(Bean eventBean) throws Throwable {
		if (!(eventBean instanceof BTConditionExploreWorld))
			return false;

		if (((BTConditionExploreWorld)eventBean).getExploreRate() >= getExtendedBean().getExploreRate()) {
			getExtendedBean().setFinished(true);
			return true;
		}

		if (isCompleted())
			onComplete();
		return true;
	}

	@Override
	public boolean isCompleted() { return getExtendedBean().isFinished(); }
	// @formatter:on
}
