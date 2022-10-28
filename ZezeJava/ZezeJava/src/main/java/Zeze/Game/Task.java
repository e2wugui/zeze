package Zeze.Game;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Builtin.Game.Task.BTask;
import Zeze.Builtin.Game.Task.BTaskKey;
import Zeze.Builtin.Game.Task.BTaskPhase;
import Zeze.Builtin.Game.Task.tTask;
import Zeze.Collections.BeanFactory;
import Zeze.Collections.DAG;

public class Task {
	private final static BeanFactory beanFactory = new BeanFactory();
	private final Module module;
	private final String name;
	private final BTask bean;
	DAG<BTaskPhase> phases; // 任务的各个阶段

	private Task(Module module, String taskId, String taskName, DAG<BTaskPhase> phases) {
		this.module = module;
		this.name = taskName;
		this.bean = this.module._tTask.getOrAdd(new BTaskKey(taskId, taskName));
		this.phases = phases;
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
			var taskPhases = DAGs.open("TaskPhase", BTaskPhase.class);
			return tasks.computeIfAbsent(taskName, key -> new Task(this, "1", key, taskPhases)); // TODO: Danger!!! taskId is hard coded, use Autokey to solve it
		}
	}
}
