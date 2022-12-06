package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BTConditionNPCTalk;
import Zeze.Builtin.Game.TaskBase.BTConditionNPCTalkEvent;
import Zeze.Builtin.Game.TaskBase.BTaskEvent;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;

public class ConditionNPCTalk extends TaskConditionBase<BTConditionNPCTalk, BTConditionNPCTalkEvent> {
	public ConditionNPCTalk(TaskPhase phase) {
		super(phase, BTConditionNPCTalk.class, BTConditionNPCTalkEvent.class);
	}

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public boolean accept(BTaskEvent eventBean) {
		return false;
	}
}
