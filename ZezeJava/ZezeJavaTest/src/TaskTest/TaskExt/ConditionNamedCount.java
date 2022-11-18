package TaskTest.TaskExt;

import Zeze.Game.TaskCondition;
import Zeze.Game.ConditionEvent;
import Zeze.Transaction.Bean;

public class ConditionNamedCount extends TaskCondition<BCollectCoinTask, BCollectCoinEvent> {
	private final BCollectCoinTask bean;

	public ConditionNamedCount(String name, long currentCount, long targetCount) {
		bean = new BCollectCoinTask();
		bean.setName(name);
		bean.setCurrentCoinCount(currentCount);
		bean.setTargetCoinCount(targetCount);
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

	@Override
	public BCollectCoinTask getConditionBean() {
		return bean;
	}

	public static class Event extends ConditionEvent {
		private final String name;

		public Event(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	@Override
	public String getName() {
		return bean.getName();
	}

	public long getCount() {
		return bean.getCurrentCoinCount();
	}

	public long getTargetCount() {
		return bean.getTargetCoinCount();
	}
}
