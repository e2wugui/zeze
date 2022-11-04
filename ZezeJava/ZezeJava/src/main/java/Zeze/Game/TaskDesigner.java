package Zeze.Game;

import Zeze.Application;
import Zeze.Builtin.Game.Task.BTaskPhase;

/**
 * Factory性质
 * 提供新Task的设计和创建。
 */

public class TaskDesigner {
	public final Application zeze;
	Task.Module taskModule;

	public TaskDesigner(Application zeze, Task.Module taskModule) {
		this.zeze = zeze;
		this.taskModule = taskModule;
	}

	public Task create(String taskName) {
		Task newTask = taskModule.open(taskName);
		BTaskPhase phase1 = new BTaskPhase();
		BTaskPhase phase2 = new BTaskPhase();
		newTask.addPhase(phase1);
		newTask.addPhase(phase2);
		newTask.linkPhase(phase1, phase2);
		return newTask;
	}
}
