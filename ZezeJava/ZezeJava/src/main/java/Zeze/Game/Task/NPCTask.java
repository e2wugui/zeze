package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskBase.BNPCTaskDynamics;
import Zeze.Game.TaskBase;

public class NPCTask extends TaskBase<BNPCTaskDynamics> {

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

	public NPCTask(TaskBase.Module taskModule, String taskName) {
		this(taskModule, taskName, null);
	}
	public NPCTask(TaskBase.Module taskModule, String taskName, String[] preTasks) {
		super(taskModule, taskName, BNPCTaskDynamics.class);
	}
}