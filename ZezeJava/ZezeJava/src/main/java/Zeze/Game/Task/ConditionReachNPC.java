package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BTConditionReachNPC;
import Zeze.Builtin.Game.TaskBase.BTConditionReachNPCEvent;
import Zeze.Builtin.Game.TaskBase.BTaskEvent;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;

public class ConditionReachNPC extends TaskConditionBase<BTConditionReachNPC, BTConditionReachNPCEvent> {

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public boolean accept(BTaskEvent eventBean) throws Throwable {
		return false;
	}

	public ConditionReachNPC(TaskPhase phase) {
		super(phase, BTConditionReachNPC.class, BTConditionReachNPCEvent.class);
	}
}
