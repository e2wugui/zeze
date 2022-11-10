package Zeze.Game;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Builtin.Game.Task.BTask;
import Zeze.Builtin.Game.Task.BTaskKey;
import Zeze.Builtin.Game.Task.BTaskPhase;
import Zeze.Builtin.Game.Task.TriggerTaskEvent;
import Zeze.Builtin.Game.Task.tTask;
import Zeze.Collections.BeanFactory;
import Zeze.Collections.DAG;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;

/**
 * 设计思路
 * 一个通用的Task Module，存储所有的Task
 */
public class Task {
	private final static BeanFactory beanFactory = new BeanFactory();
	public static long getSpecialTypeIdFromBean(Bean bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}
	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}
	private final Module module;
	private final String name;
	private final BTask bean;
	private final DAG<BTaskPhase> phaseDAG; // 任务的各个阶段的连接图

	protected Task(Module module, long taskId, String taskName, DAG<BTaskPhase> phaseDAG) {
		this.module = module;
		this.name = taskName;
		this.bean = this.module._tTask.getOrAdd(new BTaskKey(taskId));
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
		return bean.getTaskPhases().get(bean.getCurrentPhaseId());
	}

	public static class Module extends AbstractTask {
		private final ConcurrentHashMap<String, Task> tasks = new ConcurrentHashMap<>();
		public final Application zeze;
		public final DAG.Module moduleDAG;

		public Module(Application zeze, DAG.Module dagModule) {
			this.zeze = zeze;
			this.moduleDAG = dagModule;
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
		public Task open(String taskId) {
			var taskPhases = moduleDAG.open(taskId + ".TaskPhase", BTaskPhase.class);
			return tasks.computeIfAbsent(taskId, key -> new Task(this, 1, key, taskPhases)); // TODO: Danger!!! taskId is hard coded, use Autokey to resolve it
		}
		@Override
		protected long ProcessTriggerTaskEventRequest(TriggerTaskEvent r) throws Throwable {
			var taskId = Long.toString(r.Argument.getTaskId());
			if (tasks.containsKey(taskId))
				return Procedure.Exception;
			var task = tasks.get(taskId);
//			var currentTaskPhase = task.getCurrentPhase();
//			var currentTaskCondition = currentTaskPhase.getCurrentCondition();

//			var customBeanId = r.Argument.getDynamicData().getTypeId();

//			for (var process : task.processes)
//				if (!process.accept(r))
//					return Procedure.Exception;
			return Procedure.Success;
		}
	}

	// ==================== 开放给外部的功能模块API ====================
	public void linkPhase(TaskPhase from, TaskPhase to) throws Exception {
		long fromId = from.getPhaseId();
		long toId = to.getPhaseId();

		BTaskPhase fromBean = new BTaskPhase();
		fromBean.setTaskPhaseId(fromId);
		BTaskPhase toBean = new BTaskPhase();
		toBean.setTaskPhaseId(toId);
		phaseDAG.addNode(fromId, fromBean);
		phaseDAG.addNode(toId, toBean);
		phaseDAG.addEdge(fromId, toId);
	}

	public boolean accept(ConditionEvent event) {
		return false;
	}
	// ==================== 开放给外部的功能模块API ====================
}
