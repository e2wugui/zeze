package Zeze.Game;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.concurrent.ConcurrentHashMap;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.TaskBase.BAcceptTaskEvent;
import Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent;
import Zeze.Builtin.Game.TaskBase.BRoleTasks;
import Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent;
import Zeze.Builtin.Game.TaskBase.BSubmitTaskEvent;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Builtin.Game.TaskBase.BTaskKey;
import Zeze.Builtin.Game.TaskBase.TriggerTaskEvent;
import Zeze.Collections.BeanFactory;
import Zeze.Game.Task.ConditionKillMonster;
import Zeze.Game.Task.ConditionNPCTalk;
import Zeze.Game.Task.ConditionReachPosition;
import Zeze.Game.Task.ConditionSubmitItem;
import Zeze.Game.Task.DailyTask;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Task;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

public abstract class TaskBase<ExtendedBean extends Bean> {

	// @formatter:off
	protected TaskBase(Module module) { this.module = module; }
	public Module getModule() { return module; }
	private final Module module;
	public BTask getBean() { return bean; }
	private BTask bean;
	private TaskPhase currentPhase;
	public abstract String getType(); // 任务类型，每个任务实例都不一样
	public abstract ExtendedBean getExtendedBean(); // 任务扩展数据，每个任务实例都不一样
	public final ConcurrentHashSet<Long> preTaskIds = new ConcurrentHashSet<>(); // 将通过Module在加载完配置后（即TaskGraphics的功能）统一初始化，与Bean无关，不需要存储在数据库
	public final ConcurrentHashSet<Long> nextTaskIds = new ConcurrentHashSet<>(); // 将通过Module在加载完配置后（即TaskGraphics的功能）统一初始化，与Bean无关，不需要存储在数据库
	public final ConcurrentHashMap<Long, TaskPhase> phases = new ConcurrentHashMap<>();
	protected abstract boolean isAbleToStartTask(); // 设置除了前置任务这个条件外的其他条件，比如等级、职业等…… 为空表示不需要额外条件自动领取
	protected abstract void onCompleteTask(); // 任务完成时的回调，比如发放奖励等……

	/**
	 * 非Runtime方法：用于加载json配置。
	 */
	public void loadJson(JsonObject json) throws InvocationTargetException, InstantiationException, IllegalAccessException {
		this.bean = new BTask();

		bean.setTaskType(getType());
		bean.setTaskId(json.getInt("taskId"));
		bean.setTaskName(json.getString("taskName"));
		bean.setTaskDescription(json.getString("taskDesc"));

		var preTaskIds = json.getJsonArray("preTaskIds");
		for (var id : preTaskIds)
			this.bean.getPreTaskIds().add(Long.parseLong(id.toString()));

		loadJsonExtended(json);
		bean.getExtendedData().setBean(getExtendedBean());

		var phases = json.getJsonArray("Phases");
		for (var phase : phases) {
			TaskPhase taskPhase = new TaskPhase(this);
			taskPhase.loadJson(phase.asJsonObject());
			if (taskPhase.getBean().getPrePhaseIds().isEmpty()) {
				bean.setCurrentPhaseId(taskPhase.getBean().getPhaseId()); // TODO: 这里可能会出问题
				currentPhase = taskPhase;
			}
			addPhase(taskPhase);
		}

		bean.setTaskState(Module.Disabled); // 初始化为不可用状态
	}
	protected abstract void loadJsonExtended(JsonObject json);

	/**
	 * Runtime方法：从Bean中恢复Task配置类
	 */
	protected final void loadBean(BTask bean) {
		this.bean = bean;
		loadBeanExtended(bean);
		currentPhase = phases.get(bean.getCurrentPhaseId());
		currentPhase.loadBean(bean.getTaskPhases().get(bean.getCurrentPhaseId()));
	}
	protected abstract void loadBeanExtended(BTask bean);

	// @formatter:on

	/**
	 * Runtime方法：accept
	 * - 用于接收事件，改变数据库的数据
	 * - 当满足任务推进情况时，会自动推进任务
	 */
	public boolean accept(Bean eventBean) throws Exception {
		if (!currentPhase.accept(eventBean))
			return false;
		tryToProceedPhase();
		return true;
	}

	public void tryToProceedPhase() {

		// 如果是Init状态，那需要玩家去自行接任务才能够改变状态
		// 如果是Finished状态，那需要玩家去自行交任务才能够改变状态
		// 如果是Committed状态，这个任务已经结束了。如果是循环任务，那就应该重新开始。

		if (bean.getTaskState() == Module.Disabled) {
			if (isAbleToStartTask()) {
				bean.setTaskState(Module.Init);
			}
		} else if (bean.getTaskState() == Module.Processing) {
			if (currentPhase.isCompleted()) {
				if (currentPhase.isEndPhase()) {
					bean.setTaskState(Module.Finished);
				} else {
					var nextPhaseId = currentPhase.getBean().getNextPhaseId();
					bean.setCurrentPhaseId(nextPhaseId);
					currentPhase = phases.get(nextPhaseId);
					currentPhase.loadBean(bean.getTaskPhases().get(bean.getCurrentPhaseId()));
				}
			}
		} else if (bean.getTaskState() == Module.Committed) {
			onCompleteTask();
		}
	}

	/**
	 * Runtime方法：接任务
	 */
	public void start() {
		bean.setTaskState(Module.Processing);
	}

	/**
	 * Runtime方法：交任务
	 */
	public void commit() {
		bean.setTaskState(Module.Committed);
	}

	// ======================================== Private方法和一些不需要被注意的方法 ========================================
	// @formatter:off
	private static final BeanFactory beanFactory = new BeanFactory();
	public static long getSpecialTypeIdFromBean(Serializable bean) { return BeanFactory.getSpecialTypeIdFromBean(bean); }
	public static Bean createBeanFromSpecialTypeId(long typeId) { return beanFactory.createBeanFromSpecialTypeId(typeId); }
	@SuppressWarnings("unchecked")
	public Class<ExtendedBean> getExtendedBeanClass() {
		ParameterizedType parameterizedType = (ParameterizedType)this.getClass().getGenericSuperclass();
		return (Class<ExtendedBean>)parameterizedType.getActualTypeArguments()[0];
	}
	private void addPhase(TaskPhase phase) {
		phases.put(phase.getBean().getPhaseId(), phase);
		bean.getTaskPhases().put(phase.getBean().getPhaseId(), phase.getBean());
	}

	// @formatter:on
	// ======================================== Task Module Part ========================================

	/**
	 * Task Module：承担TaskGraphics的功能
	 */
	public static class Module extends AbstractTaskBase {
		public final ConcurrentHashMap<Long, TaskBase<?>> taskNodes = new ConcurrentHashMap<>();
		private final DirectedAcyclicGraph<Long, DefaultEdge> taskGraph = new DirectedAcyclicGraph<>(DefaultEdge.class);
		private final ConcurrentHashMap<String, Constructor<?>> constructors = new ConcurrentHashMap<>();
		public final ConcurrentHashMap<String, Constructor<?>> conditionConstructors = new ConcurrentHashMap<>();
		public final ProviderApp providerApp;
		public final Application zeze;

		public Module(Application zeze) {
			this.zeze = zeze;
			this.providerApp = zeze.redirect.providerApp;
			RegisterZezeTables(zeze);
			RegisterProtocols(this.providerApp.providerService);
			providerApp.builtinModules.put(this.getFullName(), this);

			// 注册内置Task类型
			registerTask(DailyTask.class);

			// 注册内置Condition类型
			registerCondition(ConditionNPCTalk.class);
			registerCondition(ConditionSubmitItem.class);
			registerCondition(ConditionKillMonster.class);
			registerCondition(ConditionReachPosition.class);
		}

		/**
		 * 在加载任务配表前，必须需要提前注册所有的任务类型。
		 */
		public <ExtendedBean extends Bean,
				ExtendedTask extends TaskBase<ExtendedBean>
				> void registerTask(Class<ExtendedTask> extendedTaskClass) {
			try {
				var c = extendedTaskClass.getDeclaredConstructor(Module.class);
				var task = c.newInstance(this);
				beanFactory.register(task.getExtendedBeanClass());
				constructors.put(task.getType(), c);
//				_tEventClasses.getOrAdd(1).getEventClasses().add(task.getExtendedBeanClass().getName()); // key is 1, only one record
			} catch (Exception e) {
				Task.forceThrow(e);
			}
		}

		/**
		 * 在加载任务配表前，必须需要提前注册所有的条件类型。
		 */
		public <ConditionBean extends Bean, EventBean extends Bean,
				ExtendedCondition extends TaskConditionBase<ConditionBean, EventBean>
				> void registerCondition(Class<ExtendedCondition> extendedConditionClass) {
			try {
				var c = extendedConditionClass.getDeclaredConstructor(TaskPhase.class);
				var condition = c.newInstance((Object)null);
				beanFactory.register(condition.getConditionBeanClass());
				beanFactory.register(condition.getEventBeanClass());
				conditionConstructors.put(condition.getType(), c);
			} catch (Exception e) {
				Task.forceThrow(e);
			}
		}

		/**
		 * 加载任务配置表里的所有任务（在服务器启动时，或者想要验证配表是否合法时）
		 * 需要在事务中执行。
		 */
		public void loadJson(String taskJson) throws FileNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
			JsonReader reader = Json.createReader(new FileReader(taskJson));
			var json = reader.readObject();
			reader.close();

			var taskType = json.getString("taskType");
			var constructor = constructors.get(taskType);
			var task = (TaskBase<?>)constructor.newInstance(this);
			task.loadJson(json);

			taskNodes.put(task.getBean().getTaskId(), task);
			_tTask.put(new BTaskKey(task.getBean().getTaskId()), task.getBean());
			for (var t : taskNodes.values()) {
				taskGraph.addVertex(task.getBean().getTaskId());
			}

			for (var t : taskNodes.values()) {
				for (var preId : t.getBean().getPreTaskIds()) {
					var preTask = taskNodes.get(preId);
					if (null == preTask)
						throw new IllegalStateException("task " + task.getBean().getTaskId() + " preTask " + preId + " not found.");
					taskGraph.addEdge(preId, task.getBean().getTaskId()); // 有向无环图，如果不合法会自动抛异常
				}
			}

			for (var t : taskNodes.values()) {
				t.preTaskIds.clear();
				t.nextTaskIds.clear();
				taskGraph.getAncestors(task.getBean().getTaskId()).forEach(t.preTaskIds::add); // 从图中获取所有前置任务
				taskGraph.getDescendants(task.getBean().getTaskId()).forEach(t.nextTaskIds::add); // 从图中获取所有后置任务
			}
		}

		@Override
		public void UnRegister() {
			if (null != zeze) {
				UnRegisterZezeTables(zeze);
			}
		}

		/**
		 * 所有任务的Trigger Rpc，负责中转所有请求
		 */
		@Override
		protected long ProcessTriggerTaskEventRequest(TriggerTaskEvent r) throws Exception {
			int resultCode = 0;

			var roleId = r.Argument.getRoleId();
			// 读取角色任务表，如果没有，会自动创建一个空的。
			if (!_tRoleTask.contains(roleId)) {
				var roleTasks = new BRoleTasks();

				for (var task : taskNodes.values()) {
					roleTasks.getProcessingTasks().put(task.getBean().getTaskId(), task.getBean().copy());
				}

				// 初始化Task Bean
				for (var taskBean : roleTasks.getProcessingTasks().values()) {
					taskBean.setRoleId(roleId);
					var task = taskNodes.get(taskBean.getTaskId());
					task.loadBean(taskBean);
					task.tryToProceedPhase(); // 在这一阶段就检查这个任务是不是当前角色可接的
					r.Result.getChangedTasks().add(taskBean);
				}

				_tRoleTask.put(roleId, roleTasks);

				resultCode |= TaskResultNewRoleTasksCreated;
			}

			var taskInfo = _tRoleTask.get(roleId);
			var eventTypeBean = r.Argument.getEventType().getBean();
			var eventBean = r.Argument.getEventBean().getBean();
			if (eventTypeBean instanceof BAcceptTaskEvent) {
				var acceptEventEventBean = (BAcceptTaskEvent)eventTypeBean;
				var taskId = acceptEventEventBean.getTaskId();
				var taskBean = taskInfo.getProcessingTasks().get(taskId);
				if (null == taskBean) {
					r.Result.setResultCode(TaskResultTaskNotFound | TaskResultFailure);
					return Procedure.Success;
				}

				var task = taskNodes.get(taskId);
				task.loadBean(taskBean);
				task.start();
				r.Result.getChangedTasks().add(taskBean);
			} else if (eventTypeBean instanceof BSubmitTaskEvent) {
				var submitTaskEventBean = (BSubmitTaskEvent)eventTypeBean;
				var taskId = submitTaskEventBean.getTaskId();
				var taskBean = taskInfo.getProcessingTasks().get(taskId);
				if (null == taskBean) {
					r.Result.setResultCode(TaskResultTaskNotFound | TaskResultFailure);
					return Procedure.Success;
				}

				var task = taskNodes.get(taskId);
				task.loadBean(taskBean);
				task.commit();
				// 封存任务
				taskInfo.getProcessingTasks().remove(taskId);
				taskInfo.getFinishedTaskIds().add(taskId);
				r.Result.getChangedTasks().add(taskBean);
			} else if (eventTypeBean instanceof BSpecificTaskEvent) {
				var specificTaskEventBean = (BSpecificTaskEvent)eventTypeBean; // 兼容JDK11
				// 检查任务Id
				var taskId = specificTaskEventBean.getTaskId();
				var taskBean = taskInfo.getProcessingTasks().get(taskId);
				if (null == taskBean) {
					r.Result.setResultCode(TaskResultTaskNotFound | TaskResultFailure);
					return Procedure.Success;
				}

				var task = taskNodes.get(taskId);
				task.loadBean(taskBean);
				if (task.accept(eventBean))
					resultCode |= TaskResultAccepted | TaskResultSuccess;
				else
					resultCode |= TaskResultRejected | TaskResultSuccess;
				r.Result.getChangedTasks().add(taskBean);
			} else if (eventTypeBean instanceof BBroadcastTaskEvent) {
				var broadcastTaskEventBean = (BBroadcastTaskEvent)eventTypeBean; // 兼容JDK11
				var taskBeanList = taskInfo.getProcessingTasks().values();
				for (var taskBean : taskBeanList) {
					var id = taskBean.getTaskId();
					var task = taskNodes.get(id);
					task.loadBean(taskBean);
					if (task.accept(eventBean)) {
						r.Result.getChangedTasks().add(taskBean);
						if (broadcastTaskEventBean.isIsBreakIfAccepted())
							break;
					}
				}
				resultCode |= TaskResultAccepted | TaskResultSuccess;
			}

			r.Result.setResultCode(resultCode);
			return Procedure.Success;
		}
	}
}
