package Zeze.Game;

/*
 * 所有任务的集合类
 * TODO:
 * 1. 任务的状态变化，需要通知客户端
 * 2. 任务的状态变化，需要通知其他模块
 * 3. 任务的状态变化，需要通知数据库
 * 4. 任务的状态变化，需要通知其他服务器
 * 5. 任务的状态变化，需要通知其他服务器的数据库
 * 6. 任务的状态变化，需要通知其他服务器的客户端
 * 7. 任务的状态变化，需要通知其他服务器的其他模块
 */

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

/**
 * TaskGraphics
 */
public class TaskGraphics {
	final Task.Module taskModule;
	private final DirectedAcyclicGraph<Task, DefaultEdge> graph;

	public TaskGraphics(Task.Module taskModule) {
		this.taskModule = taskModule;
		graph = new DirectedAcyclicGraph<>(DefaultEdge.class);
		buildGraph();
	}

	public void addNewTask(Task task) {
		graph.addVertex(task);
		for (var preTaskName : task.getBean().getPreTasks()) {
			var preTasks = getTaskByName(preTaskName);
			graph.addEdge(preTasks, task); // 会再添加过程中检查任务图结构是否合法
		}
	}

	public void refreshGraph() {
		var tasks = getTasksOfZeroInAndOutDegree();
		for (var task : tasks) {
			addNewTask(task);
		}
	}

	private void buildGraph() {
//		var taskTable = taskModule.getTable();
//		if (taskTable.isNew())
//			return;
//		taskTable.walk((k, v) -> {
//			Task<?> task = new Task(taskModule, k.getTaskName(), );
//			// TODO: 需要彻底初始化Task
//			graph.addVertex(task);
//			return true;
//		});
//		taskTable.walk((k, v) -> {
//			var currentTask = getTaskByName(v.getTaskName());
//			var preTaskNamesPList = v.getPreTasks();
//			for (var preTaskName : preTaskNamesPList) {
//				var preTask = getTaskByName(preTaskName);
//				graph.addEdge(preTask, currentTask); // 会再添加过程中检查任务图结构是否合法
//			}
//			return true;
//		});
	}

	private Task getTaskByName(String taskName) {
		Supplier<Stream<Task>> supplier = () -> graph.vertexSet().stream().filter(task -> Objects.equals(task.getName(), taskName));
		if (supplier.get().findAny().isEmpty()) {
			throw new RuntimeException("Task not found: " + taskName);
		}
		return supplier.get().findAny().get();
	}

	private Task[] getTasksOfZeroInAndOutDegree() {
		Supplier<Stream<Task>> supplier = () -> graph.vertexSet().stream().filter(task -> graph.inDegreeOf(task) == 0 && graph.outDegreeOf(task) == 0); // 寻找离散任务点
		return supplier.get().toArray(Task[]::new);
	}
}
