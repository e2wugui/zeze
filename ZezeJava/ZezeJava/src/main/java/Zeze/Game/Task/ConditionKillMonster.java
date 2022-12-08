package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BTConditionKillMonster;
import Zeze.Builtin.Game.TaskBase.BTConditionKillMonsterEvent;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;
import Zeze.Transaction.Bean;

public class ConditionKillMonster extends TaskConditionBase<BTConditionKillMonster, BTConditionKillMonsterEvent> {
	public ConditionKillMonster(TaskPhase phase) {
		super(phase, BTConditionKillMonster.class, BTConditionKillMonsterEvent.class);
	}

	@Override
	public boolean accept(Bean eventBean) throws Throwable {
		return false;
	}

	@Override
	public boolean isCompleted() {
		return false;
	}
}
