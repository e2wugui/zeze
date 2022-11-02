package Zeze.Game;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Builtin.Game.Task.BTask;
import Zeze.Builtin.Game.Task.BTaskKey;
import Zeze.Builtin.Game.Task.BTaskPhase;
import Zeze.Builtin.Game.Task.tTask;
import Zeze.Collections.BeanFactory;
import Zeze.Collections.DAG;

/**
 * 设计思路
 * 一个通用的Task Module，存储所有的Task
 */
public class Task {
	private final static BeanFactory beanFactory = new BeanFactory();
	private final Module module;
	private final String name;
	private final BTask bean;
	DAG<BTaskPhase> phaseDAG; // 任务的各个阶段的连接图
	private final ConcurrentHashMap<String, TaskPhase> taskPhases = new ConcurrentHashMap<>(); // 任务的各个阶段

	private Task(Module module, String taskId, String taskName, DAG<BTaskPhase> phaseDAG) {
		this.module = module;
		this.name = taskName;
		this.bean = this.module._tTask.getOrAdd(new BTaskKey(taskId, taskName));
		this.phaseDAG = phaseDAG;
	}

	public Module getModule() {
		return module;
	}

	public String getName() {
		return name;
	}

	public BTask getBean() {
		return bean;
	}

	public BTaskPhase getCurrentPhase() {
		return bean.getCurrentPhase();
	}

	// ==================== 功能模块 ====================
	public void addPhase(BTaskPhase phase) {
		phaseDAG.addNode(phase.getPhaseId(), phase);
	}

	public void linkPhase(BTaskPhase from, BTaskPhase to) {
		phaseDAG.addEdge(from.getPhaseId(), to.getPhaseId());
	}

	public boolean accept(ConditionEvent event) {
		return false;
	}
	// ==================== 功能模块 ====================

	public static class Module extends AbstractTask {
		private final ConcurrentHashMap<String, Task> tasks = new ConcurrentHashMap<>();
		public final Application zeze;
		public final DAG.Module DAGs;

		Module(Application zeze, DAG.Module dagModule) {
			this.zeze = zeze;
			this.DAGs = dagModule;
			RegisterZezeTables(zeze);
		}

		@Override
		public void UnRegister() {
			if (null != zeze) {
				UnRegisterZezeTables(zeze);
			}
		}

		public tTask getTable() {
			return _tTask;
		}

		public Task open(String taskName) {
			var taskPhases = DAGs.open(taskName + ".TaskPhase", BTaskPhase.class);
			return tasks.computeIfAbsent(taskName, key -> new Task(this, "1", key, taskPhases)); // TODO: Danger!!! taskId is hard coded, use Autokey to solve it
		}
	}
}