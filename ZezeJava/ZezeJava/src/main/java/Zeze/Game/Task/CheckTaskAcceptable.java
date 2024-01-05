package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskModule.BTaskConfig;

@FunctionalInterface
public interface CheckTaskAcceptable {
	boolean check(BTaskConfig.Data task, long roleId);
}
