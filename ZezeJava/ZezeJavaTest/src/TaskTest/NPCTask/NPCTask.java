package TaskTest.NPCTask;

import Zeze.Game.Task;

public class NPCTask extends Task<BNPCTaskDynamics> {

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

	public NPCTask(Task.Module taskModule, String taskName) {
		this(taskModule, taskName, null);
	}
	public NPCTask(Task.Module taskModule, String taskName, String[] preTasks) {
		super(taskModule, taskName, BNPCTaskDynamics.class);
	}
}
