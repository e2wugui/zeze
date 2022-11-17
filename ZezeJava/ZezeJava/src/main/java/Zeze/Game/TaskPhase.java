package Zeze.Game;

import java.util.ArrayList;
import Zeze.Builtin.Game.Task.BTaskPhase;
import Zeze.Transaction.Bean;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

public class TaskPhase {
	private final Task task; // Phase所属的Task
	private final long phaseId; // Phase的Id（自动生成，任务内唯一）
	private BTaskPhase bean;
	private final DirectedAcyclicGraph<TaskCondition, DefaultEdge> conditions = new DirectedAcyclicGraph<>(DefaultEdge.class); // 任务的各个阶段的连接图
	private final ArrayList<TaskCondition> currentConditions = new ArrayList<>(); // 任务的各个阶段的连接图

	public TaskPhase(Task task, long phaseId) {
		this.task = task;
		this.phaseId = phaseId;
	}

	public Task getTask() {
		return task;
	}

	public long getPhaseId() {
		return phaseId;
	}

	public BTaskPhase getBean() {
		return bean;
	}

	public void setBean(BTaskPhase bean) {
		this.bean = bean;
	}

	public ArrayList<TaskCondition> getCurrentConditions() {
		return currentConditions;
	}

	public void addCondition(TaskCondition condition) {
		conditions.addVertex(condition);
		// 自动注册加入的Condition自己的Bean和Event Bean的class。
		task.getModule().register(condition.getBeanClass());
		task.getModule().register(condition.getEventBeanClass());
	}

	public void linkCondition(TaskCondition from, TaskCondition to) throws Exception {
		conditions.addEdge(from, to);
	}

	public boolean isCompleted() {
		var zeroInDegreeNode = conditions.vertexSet().stream().filter(p -> conditions.inDegreeOf(p) == 0);
		return zeroInDegreeNode.allMatch(TaskCondition::isDone);
	}

	public boolean accept(Bean eventBean) {
		for (var condition : currentConditions)
			condition.accept(eventBean);
		return isCompleted();
	}

	public void setupPhase() {
		var zeroInDegreeNode = conditions.vertexSet().stream().filter(p -> conditions.inDegreeOf(p) == 0);
		zeroInDegreeNode.forEach(currentConditions::add);
	}
}
