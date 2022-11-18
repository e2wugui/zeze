package Zeze.Game;

import java.util.ArrayList;
import Zeze.Builtin.Game.Task.BTask;
import Zeze.Builtin.Game.Task.BTaskPhase;
import Zeze.Transaction.Bean;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

public class TaskPhase {

	public TaskPhase(Task task, String name) throws Throwable {
		this.task = task;
		this.bean = task.getBean().getTaskPhases().getOrAdd(name);
		this.bean.setTaskPhaseName(name);
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
	public Task getTask() {
		return task;
	}
	private final Task task;

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
	public ArrayList<TaskCondition<?,?>> getCurrentConditions() {
		return currentConditions;
	}
	private final DirectedAcyclicGraph<TaskCondition<?,?>, DefaultEdge> conditions = new DirectedAcyclicGraph<>(DefaultEdge.class); // 任务的各个阶段的连接图
	private final ArrayList<TaskCondition<?,?>> currentConditions = new ArrayList<>(); // 任务的各个阶段的连接图

	// @formatter:on

	// ======================================== 任务Phase初始化阶段的方法 ========================================
	public void addCondition(TaskCondition<?, ?> condition) throws Throwable {
		conditions.addVertex(condition);
		var beanCondition = bean.getTaskConditions().getOrAdd(condition.getName());
		beanCondition.setTaskConditionName(condition.getName());
		beanCondition.getTaskConditionCustomData().setBean(condition.getConditionBean());

		// 自动注册加入的Condition自己的Bean和Event Bean的class。
		task.getModule().register(condition.getConditionBeanClass());
		task.getModule().register(condition.getEventBeanClass());
	}

	public void linkCondition(TaskCondition<?, ?> from, TaskCondition<?, ?> to) throws Exception {
		conditions.addEdge(from, to);
	}

	public boolean isCompleted() {
		var zeroInDegreeNode = conditions.vertexSet().stream().filter(p -> conditions.inDegreeOf(p) == 0);
		return zeroInDegreeNode.allMatch(TaskCondition::isDone);
	}

	public void setupPhase() {
		var zeroInDegreeNode = conditions.vertexSet().stream().filter(p -> conditions.inDegreeOf(p) == 0);
		zeroInDegreeNode.forEach(currentConditions::add);
	}
}
