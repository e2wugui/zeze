package Zeze.Game;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Game.TaskBase.BTaskEvent;
import Zeze.Builtin.Game.TaskBase.BTaskPhase;
import Zeze.Transaction.Bean;
import Zeze.Util.Action0;
import Zeze.Util.Action1;

public class TaskPhase { // TODO 使用Action绑定来引导Condition切换NextPhase
	public static final int CommitAuto = 11;
	public static final int CommitNPCTalk = 12;
	public static final int ConditionCompleteAll = 31;
	public static final int ConditionCompleteAny = 32;
	public static final int ConditionCompleteSequence = 33;

	/**
	 * 指定这个方法以允许任务根据不同的Condition完成情况来切换到不同的NextPhase。
	 */

	// @formatter:off
	public static class TaskPhaseOpt{
		public long id;
		public String name;
		public String description;
		public List<Long> afterPhaseIds = new java.util.ArrayList<>();
		public int commitType;
		public int commitNPCId;
		public int conditionsCompleteType;
	}
	public TaskPhase(final TaskBase<?> task, TaskPhaseOpt opt) {
		this.task = task;
		this.bean =new BTaskPhase();
		this.bean.setPhaseId(opt.id);
		this.bean.setPhaseType(opt.commitType);
		this.bean.setPhaseName(opt.name);
		this.bean.setPhaseDescription(opt.description);
		for (var afterPhaseId : opt.afterPhaseIds)
			this.bean.getAfterPhaseIds().add(afterPhaseId);
		if (opt.afterPhaseIds.isEmpty())
			setNextPhaseId(-1);
		else
			setNextPhaseId(opt.afterPhaseIds.get(0)); // 默认推进到第一个加入的Phase （如果不特别指定）
		this.bean.setConditionsCompleteType(opt.conditionsCompleteType);
		currentConditionId = 1; // TODO: 硬编码！注意错误
	}

	/**
	 * Phase Info:
	 * 1. Phase Id
	 * 2. Phase Type
	 * 3. Phase Name
	 * 4. Phase Description
	 */
	public long getPhaseId() { return bean.getPhaseId(); }
	public int getPhaseType() { return bean.getPhaseType(); }
	public String getPhaseName() { return bean.getPhaseName(); }
	public String getPhaseDescription() { return bean.getPhaseDescription(); }
	public long getNextPhaseId() { return bean.getNextPhaseId(); }
	public void setNextPhaseId(long id) { bean.setNextPhaseId(id); }
	public int getConditionsCompleteType() { return bean.getConditionsCompleteType(); }
	private final ConcurrentHashMap<Long, TaskConditionBase<?,?>> conditions = new ConcurrentHashMap<>();
	private long currentConditionId; // 只在ConditionCompleteSequence时有效
	public final void setOnComplete(Action0 callback) { onCompleteUserCallback = callback; }
	private Action0 onCompleteUserCallback;
	public TaskBase<?> getTask() { return task; }
	private final TaskBase<?> task;
	public BTaskPhase getBean() { return bean; }
	private BTaskPhase bean;

	/**
	 * Runtime方法：accept
	 * - 用于接收事件，改变数据库的数据
	 * - 当满足任务Phase推进情况时，会自动推进任务Phase
	 */
	public boolean accept(Bean eventBean) throws Throwable {
		if (getConditionsCompleteType() == ConditionCompleteSequence) {
			var condition = conditions.get(currentConditionId);
			if (!condition.accept(eventBean))
				return false;

			if(condition.isCompleted()) {
				condition.onComplete();
				++currentConditionId;
			}
			return true;
		} else {
			boolean res = false;
			for (var condition : conditions.values()) {
				res = res || condition.accept(eventBean);
				if(condition.isCompleted())
					condition.onComplete();
			}
			return res;
		}
	}

	/**
	 * Runtime方法：reset
	 * - 用于重置任务Phase
	 */
	public void reset() {
//		for (var condition : conditions.values())
//			condition.reset();
		currentConditionId = 1;
	}

	/**
	 * Runtime方法：isCompleted
	 * - 用于判断任务Phase是否完成
	 */
	public boolean isCompleted() {
		if (getConditionsCompleteType() == ConditionCompleteAll) {
			for (var condition : conditions.values())
				if (!condition.isCompleted())
					return false;
			return true;
		}
		if (getConditionsCompleteType() == ConditionCompleteAny) {
			for (var condition : conditions.values())
				if (condition.isCompleted())
					return true;
			return false;
		}
		if (getConditionsCompleteType() == ConditionCompleteSequence) { // 在判断是否完成上，ConditionCompleteSequence实际上和ConditionCompleteAll是一样的
			for (var condition : conditions.values())
				if (!condition.isCompleted())
					return false;
			return true;
		}
		return false;
	}

	public final void onComplete() throws Throwable {
		if (isCompleted() && null != onCompleteUserCallback) {
			onCompleteUserCallback.run();
		}
	}

	// ======================================== 任务Phase初始化阶段的方法 ========================================
	public void loadBean(BTaskPhase bean) {
		this.bean = bean;
	}
	public <T extends TaskConditionBase<?,?>> T addCondition(T condition) {
		// 不能添加不是这个任务的condition
		if (condition.getPhase() == this)
			conditions.put(condition.getConditionId(), condition);
		return condition;
	}
	public boolean isStartPhase() { return bean.getNextPhaseId() == getPhaseId(); } // 也许可以这么用，但暂时没有这么用。
	public boolean isEndPhase() { return bean.getAfterPhaseIds().isEmpty(); }

	// @formatter:on
}
