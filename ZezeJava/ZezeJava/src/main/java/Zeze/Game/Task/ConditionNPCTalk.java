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
	public boolean accept(BTaskEvent eventBean) throws Throwable {
		if (!(eventBean.getExtendedData().getBean() instanceof BTConditionNPCTalkEvent))
			return false;
		BTConditionNPCTalkEvent e = (BTConditionNPCTalkEvent)eventBean.getExtendedData().getBean();
		if (isDone())
			onComplete();
		return true;
	}

	@Override
	public boolean isDone() {
		for (var option : getExtendedBean().getDialogSelected())
			if (option.getValue() == -1)
				return false;
		return true;
	}
}
