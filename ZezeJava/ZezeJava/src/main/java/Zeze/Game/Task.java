package Zeze.Game;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.Task.BTask;
import Zeze.Builtin.Game.Task.BTaskKey;
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
	/**
	 * Task Module
	 */
	public static class Module extends AbstractTask {
		/**
		 * 所有任务的Trigger Rpc，负责中转请求
		 */
		@Override
		protected long ProcessTriggerTaskEventRequest(TriggerTaskEvent r) throws Throwable {
			var name = r.Argument.getTaskName();
			var eventBean = r.Argument.getDynamicData().getBean();

			var task = open(name);
			var phase = task.getCurrentPhase();
			var conditions = phase.getCurrentConditions();
			for (var condition : conditions) {
				condition.accept(eventBean);
			}
			return Procedure.Success;
		}

		private final ConcurrentHashMap<String, Task> tasks = new ConcurrentHashMap<>();
		public final ProviderApp providerApp;
		public final Application zeze;

		public Module(Application zeze) {
			this.zeze = zeze;
			this.providerApp = zeze.redirect.providerApp;
			RegisterZezeTables(zeze);
			RegisterProtocols(this.providerApp.providerService);
			providerApp.builtinModules.put(this.getFullName(), this);
		}

		public void register(Class<? extends Bean> cls) {
			beanFactory.register(cls);
			_tEventClasses.getOrAdd(1).getEventClasses().add(cls.getName());
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

		// 需要在事务内使用。 使用完不要保存。
		public Task newTask(String taskName) {
			return open(taskName);
		}

		private Task open(String taskName) {
			return tasks.computeIfAbsent(taskName, key -> new Task(this, key));
		}
	}

	/**
	 * Runtime方法：accept
	 * - 用于接收事件，改变数据库的数据
	 * - 当满足任务推进情况时，会自动推进任务
	 */
	public void accept(Bean eventBean) {
		if (currentPhase.accept(eventBean))
			tryToProceedToNextPhase();
	}

	// @formatter:off
	/**
	 * BeanFactory
	 */
	private final static BeanFactory beanFactory = new BeanFactory();
	public static long getSpecialTypeIdFromBean(Bean bean) { return BeanFactory.getSpecialTypeIdFromBean(bean); }
	public static Bean createBeanFromSpecialTypeId(long typeId) { return beanFactory.createBeanFromSpecialTypeId(typeId); }

	/**
	 * Module
	 */
	public Module getModule() { return module; }
	private final Module module;

	/**
	 * Task Bean
	 */
	public BTask getBean() { return bean; }
	private final BTask bean;

	/**
	 * Task Info:
	 * 1. Task Name
	 * 2. Task State
	 */
	public String getName() { return bean.getTaskName(); }
	public int getTaskState() { return bean.getState(); }

	/**
	 * Task Phases
	 */
	public TaskPhase getCurrentPhase() { return currentPhase; }
	private TaskPhase startPhase;
	private TaskPhase currentPhase;
	private TaskPhase endPhase;
	private final DirectedAcyclicGraph<TaskPhase, DefaultEdge> phases; // 任务的各个阶段的连接图

	/**
	 * Protected Task Constructor
	 * - DO NOT DIRECTLY USE THIS CONSTRUCTOR TO CREATE A NEW TASK
	 */
	protected Task(Module module, String name) {
		this.module = module;
		this.bean = this.module._tTask.getOrAdd(new BTaskKey(name));
		bean.setTaskName(name);
		bean.setState(Module.Invalid);
		phases= new DirectedAcyclicGraph<>(DefaultEdge.class);
		startPhase = null;
		currentPhase = null;
		endPhase = null;
	}
	// @formatter:on

	// ======================================== 任务初始化阶段的方法 ========================================
	public TaskPhase newPhase(String name) throws Throwable {
		TaskPhase phase = new TaskPhase(this, name);
		phases.addVertex(phase);
		return phase;
	}

	public void linkPhase(TaskPhase from, TaskPhase to) throws Exception {
		phases.addEdge(from, to);
	}

	public void setupTask() {
		// Debug Info
		var vertexCount = phases.vertexSet().size();
		var edgeCount = phases.edgeSet().size();
		// 找任务开始的节点
		Supplier<Stream<TaskPhase>> zeroInDegreeNodeSupplier = () -> phases.vertexSet().stream().filter(p -> phases.inDegreeOf(p) == 0);
		if (zeroInDegreeNodeSupplier.get().count() != 1) {
			bean.setState(Module.Invalid);
			System.out.println("Task has more than one Start Phase node.");
			return;
		}
		if (zeroInDegreeNodeSupplier.get().findAny().isEmpty()) {
			bean.setState(Module.Invalid);
			System.out.println("Task has no Start Phase node.");
			return;
		}
		startPhase = zeroInDegreeNodeSupplier.get().findAny().get();
		currentPhase = startPhase;

		// 找任务结束的节点
		Supplier<Stream<TaskPhase>> zeroOutDegreeNodeSupplier = () -> phases.vertexSet().stream().filter(p -> phases.outDegreeOf(p) == 0);
		if (zeroOutDegreeNodeSupplier.get().count() != 1) {
			bean.setState(Module.Invalid);
			System.out.println("Task has more than one End Phase node.");
			return;
		}
		if (zeroOutDegreeNodeSupplier.get().findAny().isEmpty()) {
			bean.setState(Module.Invalid);
			System.out.println("Task has no End Phase node.");
			return;
		}
		endPhase = zeroOutDegreeNodeSupplier.get().findAny().get();

		for (var phase : phases.vertexSet()) {
			phase.setupPhase();
			if (!phase.isValid()) {
				bean.setState(Module.Invalid);
				return;
			}
		}
	}

	// ======================================== Private方法 ========================================

	/**
	 * 内部方法：proceedToNextPhase
	 * - 当accept成功后（即对数据库数据进行了修改之后），会自动调用此方法
	 */
	private void tryToProceedToNextPhase() {
		if (currentPhase.isCompleted()) {
			var nextPhase = phases.getDescendants(currentPhase);
		}
	}
}
