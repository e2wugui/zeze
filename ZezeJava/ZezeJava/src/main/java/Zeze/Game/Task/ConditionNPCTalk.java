package Zeze.Game.Task;

import javax.json.JsonObject;
import Zeze.Builtin.Game.TaskBase.BTConditionNPCTalk;
import Zeze.Builtin.Game.TaskBase.BTConditionNPCTalkEvent;
import Zeze.Builtin.Game.TaskBase.BTaskCondition;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;
import Zeze.Transaction.Bean;

public class ConditionNPCTalk extends TaskConditionBase<BTConditionNPCTalk, BTConditionNPCTalkEvent> {
	// @formatter:off
	public ConditionNPCTalk(TaskPhase phase) {
		super(phase);
	}
	@Override
	protected void loadJsonExtended(JsonObject json) {
		BTConditionNPCTalk bean = new BTConditionNPCTalk();
		bean.setNpcId(json.getInt("npcId"));
		var options = json.getJsonObject("dialogOptions");
		options.keySet().forEach(key -> {
			bean.getDialogOptions().put(key, options.getInt(key));
		});
		getBean().getExtendedData().setBean(bean);
	}

	@Override
	public boolean accept(Bean eventBean) throws Throwable {
		if (!(eventBean instanceof BTConditionNPCTalkEvent))
			return false;

//		if (isCompleted())
//			onComplete();
		return true;
	}

	@Override
	public boolean isCompleted() {
		for (var option : getExtendedBean().getDialogSelected())
			if (option.getValue() == -1)
				return false;
		return true;
	}
	@Override
	protected void loadBeanExtended(BTaskCondition bean) {

	}
	@Override
	public String getType() {
		return "NPCTalk";
	}
// @formatter:on
}
