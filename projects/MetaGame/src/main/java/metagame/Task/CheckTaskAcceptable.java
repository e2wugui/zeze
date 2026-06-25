package metagame.Task;

import metagame.builtin.TaskModule.BTaskConfig;

@FunctionalInterface
public interface CheckTaskAcceptable {
	boolean check(BTaskConfig.Data task, long roleId);
}
