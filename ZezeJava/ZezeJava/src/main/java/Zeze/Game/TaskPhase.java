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
	public BTaskPhase getBean() { return bean; }
	private BTaskPhase bean;
	public TaskBase<?> getTask() { return task; }
	private final TaskBase<?> task;
	public final ConcurrentHashMap<Long, SubPhase> subPhases = new ConcurrentHashMap<>();
	public SubPhase currentSubPhase;
	public Func0<Boolean> isAbleToStartCheckCallback;
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
		}
	}

	/**
	 * Runtime方法：accept
	 * - 用于接收事件，改变数据库的数据
	 * - 当满足任务Phase推进情况时，会自动推进任务Phase
	 */
	public boolean accept(Bean eventBean) throws Throwable {
//
//		// 如果是Phase的bean，那就在Phase这儿截断。
//		if (eventBean instanceof BTPhaseCommitNPCTalkEvent) {
//			var e = (BTPhaseCommitNPCTalkEvent)eventBean;
//			if (e.getPhaseId() != getPhaseId())
//				return false;
//
//			if (getCommitType() == CommitNPCTalk) {
//				var commitBean = (BTPhaseCommitNPCTalk)bean.getExtendedData().getBean();
//				commitBean.setCommitted(true);
//				return true;
//			}
//			return false;
//		}
//
//		if (getConditionsCompleteType() == ConditionCompleteSequence) {
//			var condition = conditions.get(currentConditionId);
//			if (!condition.accept(eventBean))
//				return false;
//
//			if(condition.isCompleted()) {
//				condition.onComplete();
//				++currentConditionId;
//			}
//			return true;
//		}
//
		boolean res = false;
//		for (var condition : conditions.values()) {
//			res = res || condition.accept(eventBean);
//			if(condition.isCompleted())
//				condition.onComplete();
//		}
		return res;
	}

	/**
	 * 非Runtime方法：用于加载配置。
	 */
	public void addSubPhase(SubPhase subPhase) {
		bean.getSubPhases().put(subPhase.getBean().getSubPhaseId(), subPhase.getBean());
		subPhases.put(subPhase.getBean().getSubPhaseId(), subPhase);
	}

	public static class SubPhase {
		public static int COMPLETE_ALL = 0;
		public static int COMPLETE_ANY = 1;
		private BSubPhase bean;
		public BSubPhase getBean() { return bean; }
		private final TaskPhase phase;
		public SubPhase(TaskPhase phase) {
			this.phase = phase;
		}
		public final ConcurrentHashSet<TaskConditionBase<?,?>> conditions = new ConcurrentHashSet<>();

		/**
		 * 非Runtime方法：用于加载json配置。
		 */
		public void loadJson(JsonObject json) {
			bean = new BSubPhase();
			bean.setSubPhaseId(json.getInt("subPhaseId"));
			bean.setNextSubPhaseId(json.getInt("nextSubPhaseId"));
			bean.setCompleteType(json.getInt("completeType"));
		}

		/**
		 * 非Runtime方法：用于加载配置。
		 */
		public void addCondition(TaskConditionBase<?,?> condition) {
			bean.getConditions().add(condition.getBean());
			conditions.add(condition);
		}
	}

	/**
	 * Runtime方法：reset
	 * - 用于重置任务Phase
	 */
	public void reset() {
//		for (var condition : conditions.values())
//			condition.reset();
	}

	// ======================================== 任务Phase初始化阶段的方法 ========================================
	public void loadBean(BTaskPhase bean) { this.bean = bean; }
	public boolean isStartPhase() { return bean.getNextPhaseId() == bean.getPhaseId() || bean.getNextPhaseId() == -1; } // 也许可以这么用，但暂时没有这么用。
	public boolean isEndPhase() { return bean.getPrePhaseIds().isEmpty(); }
//	public boolean isCompleted() { return bean.isCompleted(); }

	// @formatter:on
}
