package Zeze.Game;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Game.Task.BTaskCondition;
import Zeze.Builtin.Game.Task.BTaskPhase;
import Zeze.Collections.DAG;

public class TaskPhase {
	private final Task task;
	private final String phaseId;
	private final String phaseName;
	BTaskPhase bean;
	DAG<BTaskCondition> conditionDAG;
	private final ConcurrentHashMap<String, Condition> conditions = new ConcurrentHashMap<>(); // 任务阶段的各个条件

	public TaskPhase(Task task, String phaseId, String phaseName) {
		this.task = task;
		this.phaseId = phaseId;
		this.phaseName = phaseName;
	}

	public Task getTask() {
		return task;
	}

	public String getPhaseId() {
		return phaseId;
	}

	public String getPhaseName() {
		return phaseName;
	}

	public boolean accept(ConditionEvent event) {
		return false;
	}
}
