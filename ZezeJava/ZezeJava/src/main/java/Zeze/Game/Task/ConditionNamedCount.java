package Zeze.Game.Task;


import Zeze.Builtin.Game.TaskBase.BCollectCoinTask;
import Zeze.Builtin.Game.TaskBase.BCollectCoinEvent;
import Zeze.Game.TaskCondition;
import Zeze.Transaction.Bean;

public class ConditionNamedCount extends TaskCondition<BCollectCoinTask, BCollectCoinEvent> {

	// @formatter:off
	/**
	 * Override Methods
	 */
	@Override
	public BCollectCoinTask getConditionBean() {
		return bean;
	}
	private final BCollectCoinTask bean;
	@Override
	public String getName() {
		return bean.getName();
	}
	@Override
	public boolean accept(Bean eventBean) {
		if (eventBean instanceof BCollectCoinEvent e) {
			if (e.getName().equals(bean.getName())) {
				return e.getCoinCount() >= bean.getTargetCoinCount();
			}
		}
		return false;
	}
	@Override
	public boolean isDone() {
		return bean.getCurrentCoinCount() >= bean.getTargetCoinCount();
	}

	// @formatter:on
	public ConditionNamedCount(String name, long currentCount, long targetCount) {
		bean = new BCollectCoinTask();
		bean.setName(name);
		bean.setCurrentCoinCount(currentCount);
		bean.setTargetCoinCount(targetCount);
	}

	public long getCount() {
		return bean.getCurrentCoinCount();
	}

	public long getTargetCount() {
		return bean.getTargetCoinCount();
	}
}
