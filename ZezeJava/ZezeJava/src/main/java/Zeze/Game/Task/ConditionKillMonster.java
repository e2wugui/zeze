package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BTConditionKillMonster;
import Zeze.Builtin.Game.TaskBase.BTConditionKillMonsterEvent;
import Zeze.Builtin.Game.TaskBase.BTConditionSubmitItem;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;
import Zeze.Transaction.Bean;

public class ConditionKillMonster extends TaskConditionBase<BTConditionKillMonster, BTConditionKillMonsterEvent> {
	// @formatter:off
	protected static class Opt extends TaskConditionBase.Opt {
	}
	public ConditionKillMonster(TaskPhase phase, Opt opt) {
		super(phase, opt);
	}

	public void addMonsterToBeKilled(long monsterId, int killCount) {
		var bean = getExtendedBean();
		bean.getMonsters().put(monsterId, killCount);
		bean.getMonstersKilled().put(monsterId, 0);
	}

	@Override
	public boolean accept(Bean eventBean) throws Throwable {
		if (!(eventBean instanceof BTConditionKillMonsterEvent))
			return false;

		var bean = getExtendedBean();
		var killMonsterEventBean = (BTConditionKillMonsterEvent)eventBean;
		for (var monster : killMonsterEventBean.getMonsters()) {
			bean.getMonstersKilled().put(monster.getKey(), monster.getValue());
		}

		if (isCompleted())
			onComplete();
		return true;
	}

	@Override
	public boolean isCompleted() {
		var bean = getExtendedBean();
		for (var monster : bean.getMonsters())
			if (bean.getMonstersKilled().get(monster.getKey()) < monster.getValue())
				return false;
		return true;
	}
	// @formatter:on
}
