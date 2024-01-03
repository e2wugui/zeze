package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskModule.BCondition;
import Zeze.Builtin.Game.TaskModule.BRoleTasks;
import Zeze.Builtin.Game.TaskModule.BTask;
import Zeze.Game.TaskModule;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import java.util.List;
import java.util.Set;

public class TaskImpl {
	public static void dispatch(TaskModule module, long roleId, ConditionEvent event) throws Exception {
		var roleTasks = module.getRoleTasks(roleId);
		for (var task : roleTasks.getTasks().values()) {
			// 给阶段派发事件。
			if (!task.getPhases().isEmpty()) {
				var currentPhase = task.getPhases().get(0);
				if (phaseAccept(currentPhase.getConditions(), currentPhase.getIndexSet(), event)) {
					// if phase done
					if (currentPhase.getIndexSet().isEmpty()) {
						task.getPhases().remove(0); // 阶段完成删除，意味着已经完成的任务，条件状态就看不到了。
						tryTaskDone(module, roleTasks, task);
						// tryTaskDone 里面会主动调用 notifyUser
					} else {
						notifyUser(task);
					}
					return; // 事件一旦被接受就不再继续派发。
				}
			}
			// 给任务直接内含条件派发。看成一个阶段来处理。
			if (phaseAccept(task.getConditions(), task.getIndexSet(), event)) {
				if (task.getIndexSet().isEmpty()) {
					tryTaskDone(module, roleTasks, task);
					// tryTaskDone 里面会主动调用 notifyUser
				} else {
					notifyUser(task);
				}
				return;// 事件一旦被接受就不再继续派发。
			}
		}
	}

	public static void tryTaskDone(TaskModule module, BRoleTasks roleTasks, BTask task) {
		if (task.getPhases().isEmpty() && task.getIndexSet().isEmpty()) {
			task.setTaskState(TaskModule.eTaskDone);
			if (task.isAutoCompleted()) {
				task.setTaskState(TaskModule.eTaskCompleted);
				roleTasks.getTasks().remove(task.getTaskId());
				// notifyUser task removed

				return; // done;
			}
		}
		notifyUser(task);
	}

	public static void notifyUser(BTask task) {
		// todo update view;
	}

	public static boolean phaseAccept(List<BCondition> conditons, Set<Integer> indexSet, ConditionEvent event) throws Exception {
		for (var i = 0; i < conditons.size(); ++i) {
			var bean = conditons.get(i);
			var condition = Condition.construct(bean);
			if (condition.getName().equals(event.getName()) && condition.accept(event)) {
				if (condition.isDone())
					indexSet.remove(i);

				// 保存条件状态
				var bb = ByteBuffer.Allocate();
				condition.encode(bb);
				bean.setParameter(new Binary(bb));

				return true;
			}
		}
		return false;
	}
}
