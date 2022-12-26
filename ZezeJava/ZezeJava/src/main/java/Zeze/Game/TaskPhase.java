package Zeze.Game;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import javax.json.JsonObject;
import Zeze.Builtin.Game.TaskBase.BSubPhase;
import Zeze.Builtin.Game.TaskBase.BTaskPhase;
import Zeze.Transaction.Bean;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Func0;

public class TaskPhase {
	// @formatter:off
	public TaskPhase(final TaskBase<?> task) { this.task = task; }
	public TaskBase<?> getTask() { return task; }
	private final TaskBase<?> task;
	public BTaskPhase getBean() { return bean; }
	private BTaskPhase bean;
	public final ConcurrentHashMap<Long, SubPhase> subPhases = new ConcurrentHashMap<>();
	private SubPhase currentSubPhase;
	public Func0<Boolean> isAbleToStartCallback = null; // 描述该Phase是否可以开始的回调函数，经典的情况就是：NPC占用。如果当前NPC被其他任务占用，那么当前Phase就不能开始。
	// @formatter:on

	// @formatter:off
	public void loadJson(JsonObject json)throws InvocationTargetException, InstantiationException, IllegalAccessException {
		this.bean = new BTaskPhase();

		bean.setPhaseId(json.getInt("phaseId"));
		bean.setPhaseName(json.getString("phaseName"));
		bean.setPhaseDescription(json.getString("phaseDesc"));
		bean.setNextPhaseId(json.getInt("nextPhaseId"));

		var prePhaseIds = json.getJsonArray("prePhaseIds");
		for (var id : prePhaseIds)
			this.bean.getPrePhaseIds().add(Long.parseLong(id.toString()));

		var module = task.getModule();
		var subPhases = json.getJsonArray("SubPhases");
		boolean isFirst = true;
		for (var subPhase : subPhases) {
			TaskPhase.SubPhase sub = new TaskPhase.SubPhase(this);
			sub.loadJson(subPhase.asJsonObject());
			var conditions = subPhase.asJsonObject().getJsonArray("conditions");
			for (var condition : conditions) {
				var conditionType = condition.asJsonObject().getString("conditionType");
				var conditionConstructor = module.conditionConstructors.get(conditionType);
				var con = (TaskConditionBase<?, ?>)conditionConstructor.newInstance(this);
				con.loadJson(condition.asJsonObject());
				sub.addCondition(con);
			}
			addSubPhase(sub);
			if (isFirst){
				bean.setCurrentSubPhaseId(sub.getBean().getSubPhaseId());
				currentSubPhase = sub;
				isFirst = false;
			}
		}
	}

	/**
	 * Runtime方法: 从Bean中恢复Phase
	 */
	public void loadBean(BTaskPhase bean) {
		this.bean = bean;

		currentSubPhase = subPhases.get(bean.getCurrentSubPhaseId());
		currentSubPhase.loadBean(bean.getSubPhases().get(bean.getCurrentSubPhaseId()));
	}

	/**
	 * Runtime方法：accept
	 * - 用于接收事件，改变数据库的数据
	 * - 当满足任务Phase推进情况时，会自动推进任务Phase
	 */
	public boolean accept(Bean eventBean) throws Throwable {
		if (!currentSubPhase.accept(eventBean))
			return false;

		// 如果有condition被accept了，检查是否需要推进到下一个SubPhase
		if (currentSubPhase.isCompleted()) {
			if (currentSubPhase.isEndSubPhase()) {
				// 做一些onComplete的工作
			} else {
				long nextSubPhaseId = currentSubPhase.getBean().getNextSubPhaseId();
				bean.setCurrentSubPhaseId(nextSubPhaseId);
				currentSubPhase = subPhases.get(nextSubPhaseId);
				currentSubPhase.loadBean(bean.getSubPhases().get(bean.getCurrentSubPhaseId()));
			}
		}

		return true;
	}

	/**
	 * 非Runtime方法：用于加载配置。
	 */
	public void addSubPhase(SubPhase subPhase) {
		bean.getSubPhases().put(subPhase.getBean().getSubPhaseId(), subPhase.getBean());
		subPhases.put(subPhase.getBean().getSubPhaseId(), subPhase);
	}

	public static class SubPhase {
		public static String COMPLETE_ALL = "ALL";
		public static String COMPLETE_ANY = "ANY";
		private BSubPhase bean;
		public BSubPhase getBean() { return bean; }
		private final TaskPhase phase;
		public SubPhase(TaskPhase phase) {
			this.phase = phase;
		}
		public final ConcurrentHashSet<TaskConditionBase<?,?>> conditions = new ConcurrentHashSet<>();

		/**
		 * Runtime方法：accept
		 */
		public boolean accept(Bean eventBean) throws Throwable {
			boolean res = false;
			for (var condition : conditions) {
				if (condition.accept(eventBean)) {
					res = true;
				}
			}
			return res;
		}

		/**
		 * Runtime方法：isCompleted
		 */
		public boolean isCompleted() {
			if (bean.getCompleteType() == COMPLETE_ALL) {
				for (var condition : conditions) {
					if (!condition.isCompleted())
						return false;
				}
				return true;
			} if (bean.getCompleteType() == COMPLETE_ANY) {
				for (var condition : conditions) {
					if (condition.isCompleted())
						return true;
				}
				return false;
			}
			return false;
		}

		/**
		 * Runtime方法: 从Bean中恢复SubPhase
		 */
		public void loadBean(BSubPhase bean) {
			this.bean = bean;
		}

		public boolean isEndSubPhase() {
			return bean.getNextSubPhaseId() == -1;
		}

		/**
		 * 非Runtime方法：用于加载json配置。
		 */
		public void loadJson(JsonObject json) {
			bean = new BSubPhase();
			bean.setSubPhaseId(json.getInt("subPhaseId"));
			bean.setNextSubPhaseId(json.getInt("nextSubPhaseId"));
			bean.setCompleteType(json.getString("completeType")); // 0: COMPLETE_ALL, 1: COMPLETE_ANY
		}

		/**
		 * 非Runtime方法：用于加载Condition配置。
		 */
		public void addCondition(TaskConditionBase<?,?> condition) {
			bean.getConditions().add(condition.getBean());
			conditions.add(condition);
		}
	}

	public boolean isEndPhase() { return bean.getNextPhaseId() == -1; }
	public boolean isCompleted() {
		return currentSubPhase.isEndSubPhase() && currentSubPhase.isCompleted();
	}
	// @formatter:on
}