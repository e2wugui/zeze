package Zeze.Game;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.TaskBase.BNPCTaskDynamics;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Builtin.Game.TaskBase.BTaskKey;
import Zeze.Builtin.Game.TaskBase.TriggerTaskEvent;
import Zeze.Builtin.Game.TaskBase.tTask;
import Zeze.Collections.BeanFactory;
import Zeze.Game.Task.NPCTask;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

/**
 * Task类
 * Task类本质是一个配置表，把真正需要的数据放到BTask里面
 */
public class TaskBase<ExtendedBean extends Bean> {
	/**
	 * Task Module
	 */
	public static class Module extends AbstractTaskBase {
		/**
		 * 所有任务的Trigger Rpc，负责中转请求
		 */
		@Override
		protected long ProcessTriggerTaskEventRequest(TriggerTaskEvent r) throws Throwable {
			var roleId = r.Argument.getRoleId();
			var processingTasksId = _tRoleTask.get(roleId).getProcessingTasksId();
			for (long id : processingTasksId) {
				var task = tasks.get(Long.toString(id));
				if (null == task) {
					r.Result.setResultCode(TaskResultTaskNotFound);
					return Procedure.Success;
				}
				var eventBean = r.Argument.getExtendedData().getBean();
				for (var condition : task.getCurrentPhase().getCurrentConditions()){
					condition.accept(eventBean);
				}
			}
			r.Result.setResultCode(TaskResultAccepted);
			return Procedure.Success;
		}

		private final ConcurrentHashMap<String, TaskBase<?>> tasks = new ConcurrentHashMap<>();
		private final TaskGraphics taskGraphics;
		public final ProviderApp providerApp;
		public final Application zeze;

		public Module(Application zeze) {
			this.zeze = zeze;
			this.providerApp = zeze.redirect.providerApp;
			RegisterZezeTables(zeze);
			RegisterProtocols(this.providerApp.providerService);
			providerApp.builtinModules.put(this.getFullName(), this);
			taskGraphics = new TaskGraphics(this);
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

		public <ExtendedBean extends Bean, ExtendedTask extends TaskBase<ExtendedBean>> ExtendedTask newCustomTask(TaskBaseOpt opt, Class<ExtendedTask> extendedTaskClass, Class<ExtendedBean> extendedBeanClass) {
			return open(opt, extendedTaskClass, extendedBeanClass);
		}

		public NPCTask newNPCTask(TaskBaseOpt opt) {
			return open(opt, NPCTask.class, BNPCTaskDynamics.class);
		}

		// 需要在事务内使用。 使用完不要保存。
		@SuppressWarnings("unchecked")
		private <ExtendedBean extends Bean, ExtendedTask extends TaskBase<ExtendedBean>> ExtendedTask open(TaskBaseOpt opt, Class<ExtendedTask> extendedTaskClass, Class<ExtendedBean> extendedBeanClass) {
			return (ExtendedTask)tasks.computeIfAbsent(Long.toString(opt.id), key -> {
				try {
					var c = extendedTaskClass.getDeclaredConstructor(Module.class, TaskBase.TaskBaseOpt.class, Class.class);
					return c.newInstance(this, opt, extendedBeanClass);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}

		public TaskBase<?> getTask(long taskId) {
			return tasks.get(Long.toString(taskId));
		}
	}

	// @formatter:off

	/**
	 * Task Info:
	 * 1. Task Id
	 * 2. Task Type
	 * 3. Task State
	 * 4. Task Name
	 * 5. Task Description
	 */
	public long getId() { return bean.getTaskId(); }
	public int getType() { return bean.getTaskType(); }
	public int getState() { return bean.getTaskState(); }
	public String getName() { return bean.getTaskName(); }
	public String getDescription() { return bean.getTaskDescription(); }
	public Module getModule() { return module; }
	private final Module module;

	/**
	 * Task Constructors
	 */
	public static class TaskBaseOpt{
		public long id;
		public int type;
		public String name;
		public String description;
		public long[] preTaskIds;
	}
	public TaskBase(Module module, TaskBaseOpt opt, Class<ExtendedBean> extendedBeanClass) {
		this.module = module;
		this.bean = this.module._tTask.getOrAdd(new BTaskKey(opt.id));
		this.bean.setTaskId(opt.id);
		this.bean.setTaskType(opt.type);
		this.bean.setTaskState(Module.Invalid);
		this.bean.setTaskName(opt.name);
		this.bean.setTaskDescription(opt.description);
		for (long id :opt.preTaskIds) {
			this.bean.getPreTasksId().add(id);
		};
		this.bean.getExtendedData().setBean(new EmptyBean());
		phases= new DirectedAcyclicGraph<>(DefaultEdge.class);

		MethodHandle extendedBeanConstructor = beanFactory.register(extendedBeanClass);
		bean.getExtendedData().setBean(BeanFactory.invoke(extendedBeanConstructor));

//		module.taskGraphics.addNewTask(this);
	}
	// @formatter:on

	/**
	 * Task Phases
	 */
	public TaskPhase getCurrentPhase() {
		return currentPhase;
	}

	private TaskPhase startPhase;
	private TaskPhase currentPhase;
	private TaskPhase endPhase;
	private final DirectedAcyclicGraph<TaskPhase, DefaultEdge> phases; // 任务的各个阶段的连接图

	/**
	 * Runtime方法：accept
	 * - 用于接收事件，改变数据库的数据
	 * - 当满足任务推进情况时，会自动推进任务
	 */
	public void accept(Bean eventBean) {
		if (currentPhase.accept(eventBean))
			tryToProceedToNextPhase();
	}

	/**
	 * Runtime方法：isCompleted
	 * - 用于判断任务是否完成
	 */
	public boolean isCompleted() {
		return endPhase.isCompleted() && currentPhase == endPhase;
	}

	// ======================================== 任务初始化阶段的方法 ========================================

	public void addPhase(TaskPhase phase) {
		phases.addVertex(phase);
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
			bean.setTaskState(Module.Invalid);
			System.out.println("Task has more than one Start Phase node.");
			return;
		}
		if (zeroInDegreeNodeSupplier.get().findAny().isEmpty()) {
			bean.setTaskState(Module.Invalid);
			System.out.println("Task has no Start Phase node.");
			return;
		}
		startPhase = zeroInDegreeNodeSupplier.get().findAny().get();
		currentPhase = startPhase;

		// 找任务结束的节点
		Supplier<Stream<TaskPhase>> zeroOutDegreeNodeSupplier = () -> phases.vertexSet().stream().filter(p -> phases.outDegreeOf(p) == 0);
		if (zeroOutDegreeNodeSupplier.get().count() != 1) {
			bean.setTaskState(Module.Invalid);
			System.out.println("Task has more than one End Phase node.");
			return;
		}
		if (zeroOutDegreeNodeSupplier.get().findAny().isEmpty()) {
			bean.setTaskState(Module.Invalid);
			System.out.println("Task has no End Phase node.");
			return;
		}
		endPhase = zeroOutDegreeNodeSupplier.get().findAny().get();

		for (var phase : phases.vertexSet()) {
			phase.setupPhase();
		}
	}

	// ======================================== Private方法和一些不需要被注意的方法 ========================================

	/**
	 * 内部方法：proceedToNextPhase
	 * - 当accept成功后（即对数据库数据进行了修改之后），会自动调用此方法
	 */
	private void tryToProceedToNextPhase() {
		if (currentPhase.isCompleted()) {
			var nextPhase = phases.getDescendants(currentPhase);
		}
	}

	/**
	 * Bean
	 */
	private final static BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Bean bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public BTask getBean() {
		return bean;
	}

	@SuppressWarnings("unchecked")
	public ExtendedBean getExtendedBean() {
		return (ExtendedBean)bean.getExtendedData().getBean();
	}

	private final BTask bean;
}
