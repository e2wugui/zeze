package Zeze.Game;

import java.util.List;
import Zeze.Builtin.Game.TaskBase.BTaskEvent;
import Zeze.Builtin.Game.TaskBase.BTaskPhase;

public class TaskPhase { // TODO 使用Action绑定来引导Condition切换NextPhase

	public static final int CommitAuto = 11;
	public static final int CommitNPCTalk = 12;

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
	}
	public TaskPhase(final TaskBase<?> task, TaskPhaseOpt opt) {
		this.task = task;
		this.bean =new BTaskPhase();
		this.bean.setPhaseId(opt.id);
		this.bean.setPhaseType(opt.commitType);
		this.bean.setPhaseName(opt.name);
		this.bean.setPhaseDescription(opt.description);
		for (var afterPhaseId : opt.afterPhaseIds)
			this.bean.getAfterPhasesId().add(afterPhaseId);
		if (opt.afterPhaseIds.isEmpty())
			setNextPhaseId(-1);
		else
			setNextPhaseId(opt.afterPhaseIds.get(0)); // 默认推进到第一个加入的Phase （如果不特别指定）
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
	public void setNextPhaseId(long id) { bean.setNextPhaseId(id); }

	/**
	 * Runtime方法：accept
	 * - 用于接收事件，改变数据库的数据
	 * - 当满足任务Phase推进情况时，会自动推进任务Phase
	 */
	public boolean accept(BTaskEvent eventBean) throws Throwable {
		boolean res = false;
		for (var condition : conditions)
			res = res || condition.accept(eventBean);
		return res;
	}

	/**
	 * Runtime方法：reset
	 * - 用于重置任务Phase
	 */
	public void reset() {
//		for (var condition : currentConditions)
//			condition.reset();
	}

	/**
	 * Runtime方法：isCompleted
	 * - 用于判断任务Phase是否完成
	 */
	public boolean isCompleted() {
//		if (getConditionsCompleteType() == TaskBase.Module.ConditionCompleteAll) {
//
//		}
//		else if (getConditionsCompleteType() == TaskBase.Module.ConditionCompleteAny) {
//		}
//		else {
//			throw new RuntimeException("unknown complete type");
//		}
		return false;
	}

	/**
	 * Task Conditions
	 */
	public List<TaskConditionBase<?,?>> conditions;



	// ======================================== 任务Phase初始化阶段的方法 ========================================

	public <T extends TaskConditionBase<?,?>> T addCondition(T condition) {
		// 不能添加不是这个任务的condition
		if (condition.getPhase() == this)
			conditions.add(condition);
		return condition;
	}

//	public ArrayList<TaskConditionBase<?,?>> getCurrentConditions() {
//		return currentConditions;
//	}
//	private final DirectedAcyclicGraph<TaskConditionBase<?,?>, DefaultEdge> conditions = new DirectedAcyclicGraph<>(DefaultEdge.class); // 任务的各个阶段的连接图
//	private final ArrayList<TaskConditionBase<?,?>> currentConditions = new ArrayList<>(); // 当前的任务Phase条件（允许不止一个条件）

	// ======================================== 任务Phase初始化阶段的方法 ========================================
//	public void addCondition(TaskConditionBase<?, ?> condition) throws Throwable {
//		conditions.addVertex(condition);
//		bean.getConditions().put(condition.getBean().getConditionId(), condition.getBean());
//
//		// 自动注册加入的Condition自己的Bean和Event Bean的class。
//		task.getModule().register(condition.getConditionBeanClass());
//		task.getModule().register(condition.getEventBeanClass());
//	}

//	public void linkCondition(TaskConditionBase<?, ?> from, TaskConditionBase<?, ?> to) throws Exception {
//		conditions.addEdge(from, to);
//	}
//
//	public void setupPhase() {
//		Supplier<Stream<TaskConditionBase<?, ?>>> zeroInDegreeNodeSupplier = () -> conditions.vertexSet().stream().filter(p -> conditions.inDegreeOf(p) == 0);
//		if (zeroInDegreeNodeSupplier.get().findAny().isEmpty()) {
//			System.out.println("Task has no Start Condition node.");
//			return;
//		}
//		Supplier<Stream<TaskConditionBase<?, ?>>> zeroOutDegreeNodeSupplier = () -> conditions.vertexSet().stream().filter(p -> conditions.outDegreeOf(p) == 0);
//		if (zeroInDegreeNodeSupplier.get().findAny().isEmpty()) {
//			System.out.println("Task has no End Condition node.");
//			return;
//		}
//
//		// set current conditions with start conditions
//		zeroInDegreeNodeSupplier.get().forEach(currentConditions::add);
//	}

	public TaskBase<?> getTask() { return task; }
	public BTaskPhase getBean() { return bean; }
	private final BTaskPhase bean;
	private final TaskBase<?> task;

	// @formatter:on
}
