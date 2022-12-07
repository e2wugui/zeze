package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BCollectCoinEvent;
import Zeze.Builtin.Game.TaskBase.BTConditionReachNPC;
import Zeze.Builtin.Game.TaskBase.BTConditionReachNPCEvent;
import Zeze.Builtin.Game.TaskBase.BTaskEvent;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;
import Zeze.Transaction.Bean;

public class ConditionReachNPC extends TaskConditionBase<BTConditionReachNPC, BTConditionReachNPCEvent> {

	@Override
	public boolean isCompleted() {
		return false;
	}

	@Override
	public boolean accept(Bean eventBean) throws Throwable {
		if (!(eventBean instanceof BTConditionReachNPC e))
			return false;
		if (isCompleted())
			onComplete();
		return true;
	}

	public ConditionReachNPC(TaskPhase phase) {
		super(phase, BTConditionReachNPC.class, BTConditionReachNPCEvent.class);
	}
}
