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
import Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent;
import Zeze.Builtin.Game.TaskBase.BRoleTasks;
import Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Builtin.Game.TaskBase.BTaskKey;
import Zeze.Builtin.Game.TaskBase.TriggerTaskEvent;
import Zeze.Collections.BeanFactory;
import Zeze.Game.Task.ConditionKillMonster;
import Zeze.Game.Task.ConditionNPCTalk;
import Zeze.Game.Task.ConditionReachPosition;
import Zeze.Game.Task.ConditionSubmitItem;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Func0;
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
	public final ConcurrentHashSet<Long> preTaskIds = new ConcurrentHashSet<>(); // 将通过Module在加载完配置后（即TaskGraphics的功能）统一初始化，与Bean无关，不需要存储在数据库
	public final ConcurrentHashSet<Long> nextTaskIds = new ConcurrentHashSet<>(); // 将通过Module在加载完配置后（即TaskGraphics的功能）统一初始化，与Bean无关，不需要存储在数据库
	public final ConcurrentHashMap<Long, TaskPhase> phases = new ConcurrentHashMap<>();
	public Func0<Boolean> onCompleteCallBack;

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

	public void addPhase(TaskPhase phase) {
		phases.put(phase.getBean().getPhaseId(), phase);
		bean.getTaskPhases().put(phase.getBean().getPhaseId(), phase.getBean());
	}

	/**
	 * Runtime方法：accept
	 * - 用于接收事件，改变数据库的数据
	 * - 当满足任务推进情况时，会自动推进任务
	 */
	public boolean accept(Bean eventBean) throws Throwable {
		if (!currentPhase.accept(eventBean))
			return false;

		/*
		 * 当Event被接受后，意味着当前Phase有可能已经完成了。
		 * 当前一个Phase完成之后
		 */
		tryToProceedPhase();
		return true;
	}

	protected void tryToProceedPhase() {

//		if (currentPhase.isCompleted()) {
//			if (currentPhase.isEndPhase())
//				onComplete();
//			else {
//				currentPhase.onComplete();
//				currentPhase = phases.get(currentPhase.getBean().getNextPhaseId());
//			}
//		}

		currentPhase = phases.get(currentPhase.getBean().getNextPhaseId());
	}

	// ======================================== Private方法和一些不需要被注意的方法 ========================================
	// @formatter:off
	private final static BeanFactory beanFactory = new BeanFactory();
	public static long getSpecialTypeIdFromBean(Bean bean) { return BeanFactory.getSpecialTypeIdFromBean(bean); }
	public static Bean createBeanFromSpecialTypeId(long typeId) { return beanFactory.createBeanFromSpecialTypeId(typeId); }
	@SuppressWarnings("unchecked")
	public Class<ExtendedBean> getExtendedBeanClass() {
		ParameterizedType parameterizedType = (ParameterizedType)this.getClass().getGenericSuperclass();
		return (Class<ExtendedBean>)parameterizedType.getActualTypeArguments()[0];
	}

	// @formatter:on
	// ======================================== Task Module Part ========================================

	/**
	 * Task Module：承担TaskGraphics的功能
	 */
	public static class Module extends AbstractTaskBase {
		private final ConcurrentHashMap<String, Constructor<?>> constructors = new ConcurrentHashMap<>();
		public final ConcurrentHashMap<String, Constructor<?>> conditionConstructors = new ConcurrentHashMap<>();
		public final ConcurrentHashMap<Long, TaskBase<?>> taskNodes = new ConcurrentHashMap<>();
		private final DirectedAcyclicGraph<Long, DefaultEdge> taskGraph = new DirectedAcyclicGraph<>(DefaultEdge.class);
		public final ProviderApp providerApp;
		public final Application zeze;

		public Module(Application zeze) {
			this.zeze = zeze;
			this.providerApp = zeze.redirect.providerApp;
			RegisterZezeTables(zeze);
			RegisterProtocols(this.providerApp.providerService);
			providerApp.builtinModules.put(this.getFullName(), this);

			// 注册内置Condition
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
				_tEventClasses.getOrAdd(1).getEventClasses().add(task.getExtendedBeanClass().getName()); // key is 1, only one record
				constructors.put(task.getType(), c);
			} catch (Exception e) {
				throw new RuntimeException(e);
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
				throw new RuntimeException(e);
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
						throw new RuntimeException("task " + task.getBean().getTaskId() + " preTask " + preId + " not found.");
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
		protected long ProcessTriggerTaskEventRequest(TriggerTaskEvent r) throws Throwable {
			int resultCode = 0;

			var roleId = r.Argument.getRoleId();
			// 读取角色任务表，如果没有，会自动创建一个空的。
			if (!_tRoleTask.contains(roleId)) {
				var roleTasks = new BRoleTasks();

				for (var task : taskNodes.values()) {
					roleTasks.getProcessingTasks().put(task.getBean().getTaskId(), task.getBean().copy());
				}

				_tRoleTask.put(roleId, roleTasks);

				resultCode |= TaskResultNewRoleTasksCreated;
			}

			var taskInfo = _tRoleTask.get(roleId);
			var eventTypeBean = r.Argument.getTaskEventTypeDynamic().getBean();
			var eventBean = r.Argument.getExtendedData().getBean();
			if (eventTypeBean instanceof BSpecificTaskEvent) {
				var specificTaskEventBean = (BSpecificTaskEvent)eventTypeBean; // 兼容JDK11
				// 检查任务Id
				var id = specificTaskEventBean.getTaskId();
				var taskBean = taskInfo.getProcessingTasks().get(id);
				if (null == taskBean) {
					r.Result.setResultCode(TaskResultTaskNotFound);
					return Procedure.Success;
				}

				var task = taskNodes.get(id);
				task.loadBean(taskBean);
				if (task.accept(eventBean))
					resultCode |= TaskResultAccepted;
				else
					resultCode |= TaskResultRejected;
			} else if (eventTypeBean instanceof BBroadcastTaskEvent) {
				var broadcastTaskEventBean = (BBroadcastTaskEvent)eventTypeBean; // 兼容JDK11
				var taskBeanList = taskInfo.getProcessingTasks().values();
				for (var taskBean : taskBeanList) {
					var id = taskBean.getTaskId();
					var task = taskNodes.get(id);
					task.loadBean(taskBean);
					if (task.accept(eventBean))
						if (broadcastTaskEventBean.isIsBreakIfAccepted())
							break;
				}
				resultCode |= TaskResultAccepted;
			}

			r.Result.setResultCode(resultCode);
			return Procedure.Success;
		}
	}
}
