package Zeze.Game;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.json.JsonObject;
import Zeze.Builtin.Game.TaskBase.BSubPhase;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Builtin.Game.TaskBase.BTaskPhase;
import Zeze.Transaction.Bean;

public class TaskPhase {
	// @formatter:off
	public TaskPhase(final TaskBase<?> task) { this.task = task; }
	public BTaskPhase getBean() { return bean; }
	private BTaskPhase bean;
	public TaskBase<?> getTask() { return task; }
	private final TaskBase<?> task;
	// @formatter:on

	// @formatter:off
	public void loadJson(JsonObject json) {
		this.bean = new BTaskPhase();
		var phaseId = json.getInt("phaseId");
		var phaseName = json.getString("phaseName");
		var phaseDesc = json.getString("phaseDesc");
		var nextPhaseId = json.getInt("nextPhaseId");

		bean.setPhaseId(phaseId);
		bean.setPhaseName(phaseName);
		bean.setPhaseDescription(phaseDesc);
		bean.setNextPhaseId(nextPhaseId);

		var prePhaseIds = json.getJsonArray("prePhaseIds");
		for (var id : prePhaseIds)
			this.bean.getPrePhaseIds().add(Long.parseLong(id.toString()));
	}

	public static class SubPhase {
		public static int COMPLETE_ALL = 0;
		public static int COMPLETE_ANY = 1;
		private BSubPhase bean;
		private final TaskPhase phase;
		private final ConcurrentHashMap<Long, TaskConditionBase<?,?>> conditions = new ConcurrentHashMap<>();
		public SubPhase(TaskPhase phase) {
			this.phase = phase;
		}

		public void loadJson(JsonObject json) {
			bean = new BSubPhase();
			bean.setNextSubPhaseId(json.getInt("nextSubPhaseId"));
			bean.setCompleteType(json.getInt("completeType"));
		}

		public void addCondition(TaskConditionBase<?,?> condition) {
			conditions.put(condition.getBean().getConditionId(), condition);
			bean.getConditions().put(condition.getBean().getConditionId(), condition.getBean());
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
	 * Runtime方法：reset
	 * - 用于重置任务Phase
	 */
	public void reset() {
//		for (var condition : conditions.values())
//			condition.reset();
	}

	/**
	 * Runtime方法：isCompleted
	 * - 用于判断任务Phase是否完成
	 */
	public boolean isCompleted() {
//		if (!checkCommitted())
//			return false;
//
//		if (getConditionsCompleteType() == ConditionCompleteAll) {
//			for (var condition : conditions.values())
//				if (!condition.isCompleted())
//					return false;
//			return true;
//		}
//		if (getConditionsCompleteType() == ConditionCompleteAny) {
//			for (var condition : conditions.values())
//				if (condition.isCompleted())
//					return true;
//			return false;
//		}
//		if (getConditionsCompleteType() == ConditionCompleteSequence) { // 在判断是否完成上，ConditionCompleteSequence实际上和ConditionCompleteAll是一样的
//			for (var condition : conditions.values())
//				if (!condition.isCompleted())
//					return false;
//			return true;
//		}
		return false;
	}

	public final void onComplete() throws Throwable {
	}

	// ======================================== 任务Phase初始化阶段的方法 ========================================
	public void loadBean(BTaskPhase bean) {
		this.bean = bean;
	}
	public boolean isStartPhase() { return bean.getNextPhaseId() == bean.getPhaseId() || bean.getNextPhaseId() == -1; } // 也许可以这么用，但暂时没有这么用。
	public boolean isEndPhase() { return bean.getPrePhaseIds().isEmpty(); }

	// @formatter:on
}
