package Zeze.Game;

import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.stream.Stream;
import Zeze.Builtin.Game.TaskBase.BTaskPhase;
import Zeze.Transaction.Bean;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

public class TaskPhase {
	public static final int TASK_PHASE_STATE_INVALID = -1;
	public static final int TASK_PHASE_STATE_VALID = 0;

	public TaskPhase(TaskBase task, String name) throws Throwable {
		this.task = task;
		this.bean = task.getBean().getTaskPhases().getOrAdd(name);
		this.bean.setTaskPhaseName(name);
		this.state = TASK_PHASE_STATE_INVALID;
	}

	/**
	 * Runtime方法：accept
	 * - 用于接收事件，改变数据库的数据
	 * - 当满足任务Phase推进情况时，会自动推进任务Phase
	 */
	public boolean accept(Bean eventBean) {
		for (var condition : currentConditions)
			condition.accept(eventBean);
		return isCompleted();
	}

	// @formatter:off
	/**
	 * 当前Phase所属的Task
	 */
	public TaskBase getTask() { return task; }
	private final TaskBase task;
	public boolean isValid(){ return state!=TASK_PHASE_STATE_INVALID; }
	int state;

	/**
	 * TaskPhase Bean
	 */
	public BTaskPhase getBean() { return bean; }
	private final BTaskPhase bean;


	/**
	 * Task Info:
	 * 1. TaskPhase Name
	 */
	public String getPhaseName() {
		return bean.getTaskPhaseName();
	}

	/**
	 * Task Conditions
	 */
	public ArrayList<TaskConditionBase<?,?>> getCurrentConditions() {
		return currentConditions;
	}
	private final DirectedAcyclicGraph<TaskConditionBase<?,?>, DefaultEdge> conditions = new DirectedAcyclicGraph<>(DefaultEdge.class); // 任务的各个阶段的连接图
	private final ArrayList<TaskConditionBase<?,?>> currentConditions = new ArrayList<>(); // 当前的任务Phase条件（允许不止一个条件）

	// @formatter:on

	// ======================================== 任务Phase初始化阶段的方法 ========================================
	public void addCondition(TaskConditionBase<?, ?> condition) throws Throwable {
		conditions.addVertex(condition);
		var beanCondition = bean.getTaskConditions().getOrAdd(condition.getName());
		beanCondition.setTaskConditionName(condition.getName());
		beanCondition.getTaskConditionCustomData().setBean(condition.getExtendedBean());

		// 自动注册加入的Condition自己的Bean和Event Bean的class。
		task.getModule().register(condition.getConditionBeanClass());
		task.getModule().register(condition.getEventBeanClass());
	}

	public void linkCondition(TaskConditionBase<?, ?> from, TaskConditionBase<?, ?> to) throws Exception {
		conditions.addEdge(from, to);
	}

	public boolean isCompleted() {
		var zeroInDegreeNode = conditions.vertexSet().stream().filter(p -> conditions.inDegreeOf(p) == 0);
		return zeroInDegreeNode.allMatch(TaskConditionBase::isDone);
	}

	public void setupPhase() {
		Supplier<Stream<TaskConditionBase<?, ?>>> zeroInDegreeNodeSupplier = () -> conditions.vertexSet().stream().filter(p -> conditions.inDegreeOf(p) == 0);
		if (zeroInDegreeNodeSupplier.get().findAny().isEmpty()) {
			state = TASK_PHASE_STATE_INVALID;
			System.out.println("Task has no Start Condition node.");
			return;
		}
		Supplier<Stream<TaskConditionBase<?, ?>>> zeroOutDegreeNodeSupplier = () -> conditions.vertexSet().stream().filter(p -> conditions.outDegreeOf(p) == 0);
		if (zeroInDegreeNodeSupplier.get().findAny().isEmpty()) {
			state = TASK_PHASE_STATE_INVALID;
			System.out.println("Task has no End Condition node.");
			return;
		}

		// set current conditions with start conditions
		zeroInDegreeNodeSupplier.get().forEach(currentConditions::add);
	}
}
