package Zeze.Game;

import Zeze.Application;
import Zeze.Builtin.Game.Task.BTaskPhase;

/**
 * Factory性质
 * 提供新Task的设计和创建。
 */

public class TaskDesigner {
	public final Application zeze;
	final Task.Module taskModule;

	public TaskDesigner(Application zeze, Task.Module taskModule) {
		this.zeze = zeze;
		this.taskModule = taskModule;
	}

	public Task createExampleTask(String taskName) {
		Task newTask = taskModule.open(taskName);
		return newTask;
	}
}
