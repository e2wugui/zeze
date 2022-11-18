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
import Zeze.Builtin.Game.Task.BTaskKey;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

/**
 * TaskGraphics 是本地程序员配置的，因此不需要支持事务操作。
 */
public class TaskGraphics {
	Task.Module taskModule;
	DirectedAcyclicGraph<Task, DefaultEdge> graph;

	public TaskGraphics(Task.Module taskModule) {
		this.taskModule = taskModule;
		graph = new DirectedAcyclicGraph<>(DefaultEdge.class);
		buildGraph();
	}

	private void buildGraph() {
		var taskTable = taskModule.getTable();
		taskTable.walk((k, v) -> {
			Task task = new Task(taskModule, k.getTaskName());
			// TODO: 需要彻底初始化Task
			graph.addVertex(task);
			return true;
		});
		taskTable.walk((k, v) -> {
			var currentTask = getTaskByName(v.getTaskName());
			var preTaskNamesPList = v.getPreTasks();
			for (var preTaskName : preTaskNamesPList) {
				var preTask = getTaskByName(preTaskName);
				graph.addEdge(preTask, currentTask); // 会再添加过程中检查任务图结构是否合法
			}
			return true;
		});
	}

	private Task getTaskByName(String taskName) {
		Supplier<Stream<Task>> supplier = () -> graph.vertexSet().stream().filter(task -> Objects.equals(task.getName(), taskName));
		if (supplier.get().findAny().isEmpty()) {
			throw new RuntimeException("preTaskName not found: " + taskName);
		}
		return supplier.get().findAny().get();
	}
}
