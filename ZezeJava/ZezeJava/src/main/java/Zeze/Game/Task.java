package Zeze.Game;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Builtin.Game.Task.BTask;
import Zeze.Builtin.Game.Task.BTaskEvent;
import Zeze.Builtin.Game.Task.BTaskKey;
import Zeze.Builtin.Game.Task.BTaskPhase;
import Zeze.Builtin.Game.Task.TriggerTaskEvent;
import Zeze.Builtin.Game.Task.tTask;
import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

/**
 * Task类
 * Task类本质是一个配置表，把真正需要的数据放到BTask里面
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
	private final BTask bean;
	private final DirectedAcyclicGraph<TaskPhase, DefaultEdge> phases = new DirectedAcyclicGraph<>(DefaultEdge.class); // 任务的各个阶段的连接图
	private int taskState;
	private TaskPhase startPhase;
	private TaskPhase currentPhase;
	private TaskPhase endPhase;

	protected Task(Module module, String name) {
		this.module = module;
		long taskId = 1; // TODO: Danger!!! taskId is hard coded, use Autokey to resolve it
		this.bean = this.module._tTask.getOrAdd(new BTaskKey(taskId));
		startPhase = null;
		currentPhase = null;
		endPhase = null;
	}

	public Module getModule() {
		return module;
	}

	public BTask getBean() {
		return bean;
	}

	public TaskPhase getCurrentPhase() {
		return currentPhase;
	}

	public static class Module extends AbstractTask {
		private final ConcurrentHashMap<String, Task> tasks = new ConcurrentHashMap<>();
		public final Application zeze;

		public Module(Application zeze) {
			this.zeze = zeze;
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
			return tasks.computeIfAbsent(taskName, key -> new Task(this, key));
		}

		@Override
		protected long ProcessTriggerTaskEventRequest(TriggerTaskEvent r) throws Throwable {
			var taskName = r.Argument.getTaskName();
			var phaseId = r.Argument.getTaskPhaseId();
			var conditionId = r.Argument.getTaskConditionId();
			var eventBean = r.Argument.getDynamicData().getBean();

			var task = open(taskName);
			var phase = task.getCurrentPhase();
			if (phase.getPhaseId() != phaseId)
				return Procedure.Exception;
			var conditions = phase.getCurrentConditions();
			for (var condition : conditions) {
//				condition.accept(eventBean);
			}
			return Procedure.Success;
		}
	}

	// ==================== 任务初始化阶段的方法 ====================
	public TaskPhase newPhase() {
		TaskPhase phase = new TaskPhase(this, 1); // TODO: Danger!!! phaseId is hard coded, use Autokey to resolve it
		phases.addVertex(phase);
		bean.getTaskPhases().put(phase.getPhaseId(), phase.getBean());
		return phase;
	}

	public void linkPhase(TaskPhase from, TaskPhase to) throws Exception {
		phases.addEdge(from, to);
	}

	public void setupTask() throws Exception {
		var ps = phases.vertexSet().stream();
		// 找任务开始的节点
		var zeroInDegreeNode = ps.filter(p -> phases.inDegreeOf(p) == 0);
		if (zeroInDegreeNode.count() != 1)
			throw new Exception("Task has more than one Start Phase node.");
		if (zeroInDegreeNode.findAny().isEmpty())
			throw new Exception("Task has no Start Phase node.");
		startPhase = zeroInDegreeNode.findAny().get();
		currentPhase = startPhase;

		// 找任务结束的节点
		var zeroOutDegreeNode = ps.filter(p -> phases.outDegreeOf(p) == 0);
		if (zeroOutDegreeNode.count() != 1)
			throw new Exception("Task has more than one End Phase node.");
		if (zeroOutDegreeNode.findAny().isEmpty())
			throw new Exception("Task has no End Phase node.");
		endPhase = zeroOutDegreeNode.findAny().get();

		taskState = Module.Disabled;

		ps.forEach(TaskPhase::setupPhase);
	}
	// ==================== 任务初始化阶段的方法 ====================

	// ==================== 任务进行阶段的方法 ====================
	public void accept(Bean eventBean) {
		currentPhase.accept(eventBean);
		proceedToNextPhase();
	}

	private void proceedToNextPhase() {
	}
	// ==================== 任务进行阶段的方法 ====================
}
