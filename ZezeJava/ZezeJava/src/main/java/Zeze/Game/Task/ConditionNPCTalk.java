package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BTConditionNPCTalk;
import Zeze.Builtin.Game.TaskBase.BTConditionNPCTalkEvent;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;
import Zeze.Transaction.Bean;

public class ConditionNPCTalk extends TaskConditionBase<BTConditionNPCTalk, BTConditionNPCTalkEvent> {
	// @formatter:off
	protected static class Opt extends TaskConditionBase.Opt {}
	public ConditionNPCTalk(TaskPhase phase, Opt opt) {
		super(phase, opt);
		var extendedBean = getExtendedBean();
	}

	public void addSelectableDialog(long dialogId, int optionsCount) {
		var extendedBean = getExtendedBean();
		extendedBean.getDialogOptions().put(dialogId, optionsCount);
		extendedBean.getDialogSelected().put(dialogId, -1); // 初始化为-1，表示还没有选
	}

	@Override
	public boolean accept(Bean eventBean) throws Throwable {
		if (!(eventBean instanceof BTConditionNPCTalkEvent))
			return false;

		if (isCompleted())
			onComplete();
		return true;
	}

	@Override
	public boolean isCompleted() {
		for (var option : getExtendedBean().getDialogSelected())
			if (option.getValue() == -1)
				return false;
		return true;
	}
	// @formatter:on
}
