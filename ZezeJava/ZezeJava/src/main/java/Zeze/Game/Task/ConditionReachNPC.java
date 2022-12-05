package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BTConditionReachNPC;
import Zeze.Builtin.Game.TaskBase.BTConditionReachNPCEvent;
import Zeze.Game.TaskConditionBase;
import Zeze.Transaction.Bean;

public class ConditionReachNPC extends TaskConditionBase<BTConditionReachNPC, BTConditionReachNPCEvent> {

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public boolean accept(Bean eventBean) {
		return false;
	}

	public ConditionReachNPC() {
		super(BTConditionReachNPC.class, BTConditionReachNPCEvent.class);
	}
}
