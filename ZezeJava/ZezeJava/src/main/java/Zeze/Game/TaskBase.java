package Zeze.Game;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Objects;
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
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action0;
import Zeze.Util.ConcurrentHashSet;
import com.opencsv.CSVReaderHeaderAware;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

public abstract class TaskBase<ExtendedBean extends Bean> {

	// @formatter:off
	protected TaskBase(Module module) { this.module = module; }
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
		var taskType = map.get("TaskType");
		var taskId = Long.parseLong(map.get("TaskId"));
		var taskName = map.get("TaskName");
		var taskDesc = map.get("TaskDesc");
		var preTaskIds = map.get("PreTaskIds");

		if (!Objects.equals(taskType,getType())) {
			throw new RuntimeException("taskType != getType()");
		}

		bean.setTaskType(taskType);
		bean.setTaskId(taskId);
		bean.setTaskName(taskName);
		bean.setTaskDescription(taskDesc);
		bean.setTaskState(Module.Invalid);

		var res = preTaskIds.split(",");
		for (var s : res) {
			if (s.isEmpty())
				continue;
			var id = Long.parseLong(s);
			this.bean.getPreTaskIds().add(id);
		}

		loadMapExtended(map);
	}
	protected abstract void loadMapExtended(Map<String, String> map);

	public void loadJson(JsonObject json) {
		this.bean = new BTask();

		bean.setTaskId(json.getInt("taskId"));
		bean.setTaskName(json.getString("taskName"));
		bean.setTaskDescription(json.getString("taskDesc"));

		var preTaskIds = json.getJsonArray("preTaskIds");
		for (var id : preTaskIds)
			this.bean.getPreTaskIds().add(Long.parseLong(id.toString()));

		loadJsonExtended(json);
	}
	protected abstract void loadJsonExtended(JsonObject json);

	/**
	 * Task Info:
	 * 1. Task Id
	 * 2. Task Type
	 * 3. Task State
	 * 4. Task Name
	 * 5. Task Description
	 */
	public long getId() { return bean.getTaskId(); }
	public abstract String getType(); // 任务类型，每个任务实例都不一样
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
				currentPhase = phases.get(currentPhase.getBean().getNextPhaseId());
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
//		int startNodeSize = 0;
//		BTaskPhase startBean = null;
//		for (var phaseBean : bean.getTaskPhases().values()) {
//			boolean isStart = true;
//			for (var id : phaseBean.getAfterPhaseIds()) {
//				// 如果没有phase依赖这个phase，意味着这个phase是开始的phase
//				if (id == phaseBean.getPhaseId())
//					isStart = false;
//			}
//			if (isStart) {
//				++startNodeSize;
//				startBean = phaseBean;
//			}
//		}
//		// 理论上只允许一个开始节点，这里暂时不处理过多的开始节点的问题。
//		currentPhase.loadBean(startBean);
//		currentPhase.reset();
	}

	/**
	 * 在任务结束后调用的方法，比如：发放奖励。
	 */
	protected final void onComplete() throws Throwable {
		if (isCompleted() && null != onCompleteUserCallback) {
			onCompleteUserCallback.run();
		}
	}

	//	public TaskPhase addPhase(TaskPhase.Opt opt, List<Long> afterPhaseIds) {
//		return addPhase(opt, afterPhaseIds, null);
//	}
//
//	public TaskPhase addPhase(TaskPhase.Opt opt, List<Long> afterPhaseIds, Action0 onCompleteUserCallback) {
//		var phase = new TaskPhase(this, opt, afterPhaseIds, onCompleteUserCallback);
//		phases.put(phase.getBean().getPhaseId(), phase);
//		bean.getTaskPhases().put(phase.getBean().getPhaseId(), phase.getBean());
//		return phase;
//	}
//
	public TaskPhase addPhase(TaskPhase phase) {
		// 不能添加不是这个任务的phase
		if (phase.getTask() != this)
			return null;

		phases.put(phase.getBean().getPhaseId(), phase);
		bean.getTaskPhases().put(phase.getBean().getPhaseId(), phase.getBean());
		return phase;
	}

	// ======================================== Private方法和一些不需要被注意的方法 ========================================
	// @formatter:off
	private final static BeanFactory beanFactory = new BeanFactory();
	public static long getSpecialTypeIdFromBean(Bean bean) { return BeanFactory.getSpecialTypeIdFromBean(bean); }
	public static Bean createBeanFromSpecialTypeId(long typeId) { return beanFactory.createBeanFromSpecialTypeId(typeId); }
	@SuppressWarnings("unchecked")
	public ExtendedBean getExtendedBean() { return (ExtendedBean)bean.getExtendedData().getBean(); }
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

		private final ConcurrentHashMap<Long, TaskBase<?>> taskNodes = new ConcurrentHashMap<>();
		private final ConcurrentHashMap<String, Constructor<?>> constructors = new ConcurrentHashMap<>();
		private final ConcurrentHashMap<String, Constructor<?>> conditionConstructors = new ConcurrentHashMap<>();
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
		 * 在加载任务配表前，必须需要先注册任务类型。（因为TaskBase是个绝对抽象类，不知道任何外部的扩展任何类型的信息）
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

		public <ConditionBean extends Bean, EventBean extends Bean,
				ExtendedCondition extends TaskConditionBase<ConditionBean, EventBean>
				> void registerCondition(Class<ExtendedCondition> extendedConditionClass) {
			try {
				var c = extendedConditionClass.getDeclaredConstructor(TaskPhase.class);
				var condition = c.newInstance((Object)null);
				conditionConstructors.put(condition.getType(), c);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 加载任务配置表里的所有任务（在服务器启动时，或者想要验证配表是否合法时）
		 * 需要在事务中执行。
		 */
		public void loadConfig(String taskConfigFile) throws Exception {

			// 这里解析任务配置表，然后把所有任务配置填充到task里面。
			var reader = new CSVReaderHeaderAware(new FileReader(taskConfigFile));
			while (true) {
				Map<String, String> initValues = reader.readMap();
				if (initValues == null)
					break;

				var taskType = Integer.parseInt(initValues.get("TaskType"));
				var task = (TaskBase<?>)constructors.get(taskType).newInstance(this);
				task.loadMap(initValues);
				taskNodes.put(task.getId(), task); // 将配置表存入tasks
				_tTask.put(new BTaskKey(task.getId()), task.getBean()); // 将配置表存入_tTask数据库
			}

			// 初始化所有Task的初始配置
			for (var task : taskNodes.values()) {
				task.preTaskIds.clear();
				task.nextTaskIds.clear();
				taskGraph.addVertex(task.getId());
			}

			for (var task : taskNodes.values()) {
				for (var preId : task.getBean().getPreTaskIds()) {
					var preTask = taskNodes.get(preId);
					if (null == preTask)
						throw new RuntimeException("task " + task.getId() + " preTask " + preId + " not found.");
					taskGraph.addEdge(preId, task.getId()); // 有向无环图，如果不合法会自动抛异常
				}
			}

			for (var task : taskNodes.values()) {
				taskGraph.getAncestors(task.getId()).forEach(task.preTaskIds::add); // 从图中获取所有前置任务
				taskGraph.getDescendants(task.getId()).forEach(task.nextTaskIds::add); // 从图中获取所有后置任务
			}
		}

		public void loadJson(String jsonFile) throws FileNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
			JsonReader reader = Json.createReader(new FileReader(jsonFile));
			var json = reader.readObject();
			reader.close();

			var taskType = json.getString("taskType");
			var constructor = constructors.get(taskType);
			var task = (TaskBase<?>)constructor.newInstance(this);
			task.loadJson(json);

			var phases = json.getJsonArray("Phases");
			for (var phase : phases) {
				TaskPhase taskPhase = new TaskPhase(task);
				taskPhase.loadJson(phase.asJsonObject());

				var subPhases = json.getJsonArray("SubPhases");
				for (var subPhase : subPhases) {
					var conditionType = subPhase.asJsonObject().getString("conditionType");
					var conditionConstructor = conditionConstructors.get(conditionType);
					var condition = (TaskConditionBase<?, ?>)conditionConstructor.newInstance(taskPhase);
					condition.loadJson(subPhase.asJsonObject());
//					taskPhase.getBean().getSubPhases().add();
				}

				task.getBean().getTaskPhases().put(taskPhase.getBean().getPhaseId(), taskPhase.getBean());
			}
		}

		/**
		 * 新建任务（仅供测试使用，会马上删除）
		 */
		public <ExtendedBean extends Bean, ExtendedTask extends TaskBase<ExtendedBean>>
		ExtendedTask newTask(Class<ExtendedTask> extendedTaskClass) {
			try {
				var c = extendedTaskClass.getDeclaredConstructor(Module.class);
				var task = c.newInstance(this);
				return task;
			} catch (Exception e) {
				throw new RuntimeException(e);
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

			// 检查角色Id，如果没有，那就创建整个角色的任务表。
			var roleId = r.Argument.getRoleId();

			// 如果是新角色，那就创建整个角色的任务表。
			if (!_tRoleTask.contains(roleId)) {
				var roleTasks = new BRoleTasks();
				roleTasks.getFinishedTaskIds().clear();

				for (var task : taskNodes.values()) {
					roleTasks.getProcessingTasks().put(task.getId(), task.getBean().copy());
				}

				_tRoleTask.put(roleId, roleTasks); // 读取角色任务表，如果没有，会自动创建一个空的。

				resultCode |= TaskResultNewRoleTasksCreated;
			}

			var taskInfo = _tRoleTask.get(roleId);
			var eventTypeBean = r.Argument.getTaskEventTypeDynamic().getBean();
			var eventExtendedBean = r.Argument.getExtendedData().getBean();
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
				if (task.accept(eventExtendedBean))
					r.Result.setResultCode(TaskResultAccepted);
				else
					r.Result.setResultCode(TaskResultRejected);
			} else if (eventTypeBean instanceof BBroadcastTaskEvent) {
				var broadcastTaskEventBean = (BBroadcastTaskEvent)eventTypeBean; // 兼容JDK11
				var taskBeanList = taskInfo.getProcessingTasks().values();
				for (var taskBean : taskBeanList) {
					var id = taskBean.getTaskId();
					var task = taskNodes.get(id);
					task.loadBean(taskBean);
					if (task.accept(eventExtendedBean))
						if (broadcastTaskEventBean.isIsBreakIfAccepted())
							break;
				}
				r.Result.setResultCode(TaskResultAccepted);
			}

			resultCode |= TaskResultSuccess;
			r.Result.setResultCode(resultCode);
//			r.getSender().Send(ByteBuffer.encode(r.Result));
			return Procedure.Success;
		}
	}
}
