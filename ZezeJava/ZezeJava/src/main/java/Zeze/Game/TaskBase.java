package Zeze.Game;

import java.io.FileReader;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent;
import Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Builtin.Game.TaskBase.BTaskKey;
import Zeze.Builtin.Game.TaskBase.BTaskPhase;
import Zeze.Builtin.Game.TaskBase.TriggerTaskEvent;
import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action0;
import Zeze.Util.ConcurrentHashSet;
import com.opencsv.CSVReaderHeaderAware;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

public abstract class TaskBase<ExtendedBean extends Bean> {

	// @formatter:off
	/**
	 * Task Constructors
	 */
	@SuppressWarnings("unchecked")
	protected
	<ExtendedBean extends Bean,
	ExtendedTask extends TaskBase<ExtendedBean>
	>
	TaskBase(Module module, Class<ExtendedTask> extendedTaskClass) {
		this.module = module;
		beanFactory.register(getExtendedBeanClass()); // 注册扩展数据的BeanFactory
	}

	// ======================================== 任务初始化阶段的方法 ========================================

	/**
	 * loadBean
	 * 将Bean加载为配置表
	 */
	protected final void loadBean(BTask bean) {
		this.bean = bean;
		loadBeanExtended(bean);
		currentPhase.loadBean(bean.getTaskPhases().get(bean.getCurrentPhaseId()));
	}
	protected abstract void loadBeanExtended(BTask bean);

	public void loadMap(Map<String, String> map){
		this.bean = new BTask();
		var taskType = Integer.parseInt(map.get("TaskType"));
		var taskId = Long.parseLong(map.get("TaskId"));
		var taskName = map.get("TaskName");
		var taskDesc = map.get("TaskDesc");

		if (taskType != getType()) {
			throw new RuntimeException("taskType != getType()");
		}

		bean.setTaskType(taskType);
		bean.setTaskId(taskId);
		bean.setTaskName(taskName);
		bean.setTaskDescription(taskDesc);
		bean.setTaskState(Module.Invalid);
		loadMapExtended(map);
	}
	protected abstract void loadMapExtended(Map<String, String> map);

	/**
	 * Task Info:
	 * 1. Task Id
	 * 2. Task Type
	 * 3. Task State
	 * 4. Task Name
	 * 5. Task Description
	 */
	public long getId() { return bean.getTaskId(); }
	public abstract int getType(); // 任务类型，每个任务实例都不一样
	public int getState() { return bean.getTaskState(); }
	public String getName() { return bean.getTaskName(); }
	public String getDescription() { return bean.getTaskDescription(); }
	public ConcurrentHashSet<Long> getPreTaskIds() { return preTaskIds; } // 这里不返回PList1<Long>，只返回一个拷贝。因为我们不希望在Runtime阶段再来修改PreTask。
	public ConcurrentHashSet<Long> getNextTaskIds() { return nextTaskIds; }
	private final ConcurrentHashSet<Long> preTaskIds = new ConcurrentHashSet<>();; // 将通过Module在加载完配置后（即TaskGraphics的功能）统一初始化，与Bean无关，不需要存储在数据库
	private final ConcurrentHashSet<Long> nextTaskIds = new ConcurrentHashSet<>();; // 将通过Module在加载完配置后（即TaskGraphics的功能）统一初始化，与Bean无关，不需要存储在数据库
	public Module getModule() { return module; }
	private final Module module;
	public BTask getBean() { return bean; }
	private BTask bean;
	private final ConcurrentHashMap<Long, TaskPhase> phases = new ConcurrentHashMap<>();
	private TaskPhase currentPhase;
	public void setOnComplete(Action0 callback) { onCompleteUserCallback = callback; }
	Action0 onCompleteUserCallback;
	// @formatter:on

	/**
	 * Runtime方法：accept
	 * - 用于接收事件，改变数据库的数据
	 * - 当满足任务推进情况时，会自动推进任务
	 */
	public boolean accept(Bean eventBean) throws Throwable {
		if (!currentPhase.accept(eventBean))
			return false;

		if (currentPhase.isCompleted())
			if (currentPhase.isEndPhase())
				onComplete();
			else {
				currentPhase.onComplete();
				currentPhase = phases.get(currentPhase.getNextPhaseId());
			}
		return true;
	}

	/**
	 * Runtime方法：isCompleted
	 * - 用于判断任务是否完成
	 */
	public boolean isCompleted() {
		return currentPhase.isEndPhase() && currentPhase.isCompleted();
	}

	/**
	 * Runtime方法：reset
	 * - 将任务回归到初始化状态
	 */
	public void reset() {
		int startNodeSize = 0;
		BTaskPhase startBean = null;
		for (var phaseBean : bean.getTaskPhases().values()) {
			boolean isStart = true;
			for (var id : phaseBean.getAfterPhaseIds()) {
				// 如果没有phase依赖这个phase，意味着这个phase是开始的phase
				if (id == phaseBean.getPhaseId())
					isStart = false;
			}
			if (isStart) {
				++startNodeSize;
				startBean = phaseBean;
			}
		}
		// 理论上只允许一个开始节点，这里暂时不处理过多的开始节点的问题。
		currentPhase.loadBean(startBean);
		currentPhase.reset();
	}

	/**
	 * 在任务结束后调用的方法，比如：发放奖励。
	 */
	protected final void onComplete() throws Throwable {
		if (isCompleted() && null != onCompleteUserCallback) {
			onCompleteUserCallback.run();
		}
	}

	public TaskPhase addPhase(TaskPhase.Opt opt, List<Long> afterPhaseIds) {
		return addPhase(opt, afterPhaseIds, null);
	}

	public TaskPhase addPhase(TaskPhase.Opt opt, List<Long> afterPhaseIds, Action0 onCompleteUserCallback) {
		var phase = new TaskPhase(this, opt, afterPhaseIds, onCompleteUserCallback);
		phases.put(phase.getPhaseId(), phase);
		bean.getTaskPhases().put(phase.getPhaseId(), phase.getBean());
		return phase;
	}

	public TaskPhase addPhase(TaskPhase phase) {
		// 不能添加不是这个任务的phase
		if (phase.getTask() != this)
			return null;

		phases.put(phase.getPhaseId(), phase);
		bean.getTaskPhases().put(phase.getPhaseId(), phase.getBean());
		return phase;
	}

	// ======================================== Private方法和一些不需要被注意的方法 ========================================
	// @formatter:off
	@SuppressWarnings("unchecked")
	public ExtendedBean getExtendedBean() { return (ExtendedBean)bean.getExtendedData().getBean(); }
	private final static BeanFactory beanFactory = new BeanFactory();
	public static long getSpecialTypeIdFromBean(Bean bean) { return BeanFactory.getSpecialTypeIdFromBean(bean); }
	public static Bean createBeanFromSpecialTypeId(long typeId) { return beanFactory.createBeanFromSpecialTypeId(typeId); }
	@SuppressWarnings("unchecked")
	private Class<ExtendedBean> getExtendedBeanClass() {
		ParameterizedType parameterizedType = (ParameterizedType)this.getClass().getGenericSuperclass();
		return (Class<ExtendedBean>)parameterizedType.getActualTypeArguments()[0];
	}

// @formatter:on
	// ======================================== Task Module Part ========================================

	/**
	 * Task Module：承担TaskGraphics的功能
	 */
	public static class Module extends AbstractTaskBase {
		/**
		 * 所有任务的Trigger Rpc，负责中转所有请求
		 */
		@Override
		protected long ProcessTriggerTaskEventRequest(TriggerTaskEvent r) throws Throwable {
			// 检查角色Id
			var roleId = r.Argument.getRoleId();
			var taskInfo = _tRoleTask.get(roleId);
			if (taskInfo == null) {
				r.Result.setResultCode(TaskResultInvalidRoleId);
				return Procedure.Success;
			}

			var eventTypeBean = r.Argument.getTaskEventTypeDynamic().getBean();
			var eventExtendedBean = r.Argument.getExtendedData().getBean();
			if (eventTypeBean instanceof BSpecificTaskEvent) {
				var specificTaskEventBean = (BSpecificTaskEvent)eventTypeBean; // 兼容JDK11
				// 检查任务Id
				var id = specificTaskEventBean.getTaskId();
				var taskBean = taskInfo.getProcessingTasksId().get(id);
				if (null == taskBean) {
					r.Result.setResultCode(TaskResultTaskNotFound);
					return Procedure.Success;
				}

				var task = tasks.get(id);
				task.loadBean(taskBean);
				if (task.accept(eventExtendedBean))
					r.Result.setResultCode(TaskResultAccepted);
				else
					r.Result.setResultCode(TaskResultRejected);
			} else if (eventTypeBean instanceof BBroadcastTaskEvent) {
				var broadcastTaskEventBean = (BBroadcastTaskEvent)eventTypeBean; // 兼容JDK11
				var taskBeanList = taskInfo.getProcessingTasksId().values();
				for (var taskBean : taskBeanList) {
					var id = taskBean.getTaskId();
					var task = tasks.get(id);
					task.loadBean(taskBean);
					if (task.accept(eventExtendedBean))
						if (broadcastTaskEventBean.isIsBreakIfAccepted())
							break;
				}
				r.Result.setResultCode(TaskResultAccepted);
			}
			return Procedure.Success;
		}

		/**
		 * 在加载任务配表前，需要先注册任务类型。（因为TaskBase是个绝对抽象类，不知道任何外部的扩展任何类型的信息）
		 */
		public <ExtendedBean extends Bean,
				ExtendedTask extends TaskBase<ExtendedBean>
				> void registerTask(Class<ExtendedTask> extendedTaskClass) {
			try {
				var c = extendedTaskClass.getDeclaredConstructor(Module.class);
				var task = c.newInstance(this);
				constructors.put(task.getType(), c);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

//		/**
//		 * 新建非内置任务
//		 */
//		public <ExtendedBean extends Bean, ExtendedTask extends TaskBase<ExtendedBean>>
//		ExtendedTask newTask(TaskBase.Opt opt, Class<ExtendedTask> extendedTaskClass) {
//			return open(opt, extendedTaskClass);
//		}

		private final ConcurrentHashMap<Long, TaskBase<?>> tasks = new ConcurrentHashMap<>();
		private final ConcurrentHashMap<Integer, Constructor<?>> constructors = new ConcurrentHashMap<>();
		private final DirectedAcyclicGraph<Long, DefaultEdge> taskGraph = new DirectedAcyclicGraph<>(DefaultEdge.class);
		public final ProviderApp providerApp;
		public final Application zeze;

		public Module(Application zeze) {
			this.zeze = zeze;
			this.providerApp = zeze.redirect.providerApp;
			RegisterZezeTables(zeze);
			RegisterProtocols(this.providerApp.providerService);
			providerApp.builtinModules.put(this.getFullName(), this);
		}

		/**
		 * 加载任务配置表里的所有任务（在服务器启动时，或者想要验证配表是否合法时）
		 * 需要在事务中执行。
		 */
		public void loadConfig(String taskConfigFile) throws Exception {

			// 这里解析任务配置表，然后把所有任务配置填充到task里面。
			var reader = new CSVReaderHeaderAware(new FileReader(taskConfigFile));
			while (true) {
				Map<String, String> values = reader.readMap();
				if (values == null)
					break;
				var taskType = Integer.parseInt(values.get("TaskType"));
				var taskId = Long.parseLong(values.get("TaskId"));
				var taskName = values.get("TaskName");
				var taskDesc = values.get("TaskDesc");

				var taskConstructor = constructors.get(taskType);
				var task = (TaskBase<?>)taskConstructor.newInstance(this);
				task.loadMap(values);
			}
			// TODO: 解析values，然后把所有任务配置填充到task里面。

			// 初始化所有Task的初始配置
			for (var task : tasks.values()) {
				task.preTaskIds.clear();
				task.nextTaskIds.clear();
				taskGraph.addVertex(task.getId());
			}

			for (var task : tasks.values()) {
				for (var preId : task.preTaskIds) {
					var preTask = tasks.get(preId);
					if (null == preTask)
						throw new RuntimeException("task " + task.getId() + " preTask " + preId + " not found.");
					taskGraph.addEdge(preId, task.getId()); // 有向无环图，如果不合法会自动抛异常
				}
			}

			for (var task : tasks.values()) {
				taskGraph.getAncestors(task.getId()).forEach(task.preTaskIds::add); // 从图中获取所有前置任务
				taskGraph.getDescendants(task.getId()).forEach(task.nextTaskIds::add); // 从图中获取所有后置任务
			}
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

		// 需要在事务内使用。使用完不要保存。
//		@SuppressWarnings("unchecked")
//		private <ExtendedBean extends Bean, ExtendedTask extends TaskBase<ExtendedBean>> ExtendedTask open(TaskBase.Opt opt, Class<ExtendedTask> extendedTaskClass) {
//			return (ExtendedTask)tasks.computeIfAbsent(opt.id, key -> {
//				try {
////					if (extendedTaskConstructors.contains(extendedTaskClass)) {
////						var c = extendedTaskConstructors.get(extendedTaskClass);
////					}
////					if (null != c) {
////						return c.newInstance(this, opt);
////					}
//					var c = extendedTaskClass.getDeclaredConstructor(Module.class, TaskBase.Opt.class); // TODO：可以把这个的Constructor缓存起来
//					var res = c.newInstance(this, opt);
//					return res;
//				} catch (Exception e) {
//					throw new RuntimeException(e);
//				}
//			});
//		}
	}
}
