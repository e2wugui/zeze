package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BNPCTaskDynamics;
import Zeze.Game.TaskBase;

public class NPCTask extends TaskBase<BNPCTaskDynamics> {
	@Override
	public void loadExtendedData() {

	}

	public long getReceiveNpcId() {
		return getExtendedBean().getReceiveNpcId();
	}

	public void setReceiveNpcId(long value) {
		getExtendedBean().setReceiveNpcId(value);
	}

	public long getSubmitNpcId() {
		return getExtendedBean().getSubmitNpcId();
	}

	public void setSubmitNpcId(long value) {
		getExtendedBean().setSubmitNpcId(value);
	}

	public static class NPCTaskOpt extends TaskBase.TaskBaseOpt{
		public long ReceiveNpcId;
		public long SubmitNpcId;
	}
	public NPCTask(TaskBase.Module taskModule, NPCTaskOpt opt) {
		super(taskModule, opt, BNPCTaskDynamics.class);
	}
}