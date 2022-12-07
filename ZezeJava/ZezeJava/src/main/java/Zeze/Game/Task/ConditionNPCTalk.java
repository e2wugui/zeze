package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BTConditionNPCTalk;
import Zeze.Builtin.Game.TaskBase.BTConditionNPCTalkEvent;
import Zeze.Builtin.Game.TaskBase.BTaskEvent;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;

public class ConditionNPCTalk extends TaskConditionBase<BTConditionNPCTalk, BTConditionNPCTalkEvent> {
	public ConditionNPCTalk(TaskPhase phase) {
		super(phase, BTConditionNPCTalk.class, BTConditionNPCTalkEvent.class);
		var extendedBean = getExtendedBean();
	}

	public void addSelectableDialog(long dialogId, int optionsCount) {
		var extendedBean = getExtendedBean();
		extendedBean.getDialogOptions().put(dialogId, optionsCount);
		extendedBean.getDialogSelected().put(dialogId, -1); // 初始化为-1，表示还没有选
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
