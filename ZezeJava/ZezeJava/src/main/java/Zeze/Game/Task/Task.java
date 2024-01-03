package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskModule.BPhase;
import Zeze.Builtin.Game.TaskModule.BTask;
import Zeze.Game.TaskModule;

public class Task {
	private TaskModule module;
	private BTask bean;
	public boolean accept(ConditionEvent event) {
		var currentPhase = bean.getPhases().get(0);
		var accepted = phaseAccept(currentPhase, event);
		if (accepted) {
			if (isPhaseDone(currentPhase)) {
				// 可能不删除阶段数据，用currentPhaseIndex来记录。看设计目标。暂定删除。
				bean.getPhases().remove(0);
				if (bean.getPhases().isEmpty()) // task is done.
					; // todo update task state;
			}
			// ...
			// todo update view;
		}
		return accepted;
	}

	public void addPhase(BPhase phase) {
		bean.getPhases().add(phase);
	}

	public boolean phaseAccept(BPhase phase, ConditionEvent event) {
		var accepted = false;
		for (var condition : phase.getConditions()) {
			var c = module.buildCondition(condition);
			if (c.getName().equals(event.getName()) && c.accept(event)) {
				if (c.isDone()) {
					phase.getConditions().remove(condition);
				}
				return true;
			}
		}
		return accepted;
	}

	public static boolean isPhaseDone(BPhase phase) {
		return phase.getConditions().isEmpty();
	}
}
