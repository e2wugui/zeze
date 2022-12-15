package Zeze.Game;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

/**
 * 计划不要TaskGraphics了，直接将功能集成进TaskBase中
 */
public class TaskGraphics {
//	final TaskBase.Module taskModule;
//	private DirectedAcyclicGraph<Long /* Task Id*/, DefaultEdge> graph;
//
//	public TaskGraphics(TaskBase.Module taskModule) {
//		this.taskModule = taskModule;
//		graph = new DirectedAcyclicGraph<>(DefaultEdge.class);
//	}
//
//	public void rebuildGraph() throws Exception {
//		graph = null;
//		graph = new DirectedAcyclicGraph<>(DefaultEdge.class);
//
//		// init vertex
//		for (var task : taskModule.getTasks().values()) {
//			graph.addVertex(task.getId());
//		}
//
//		// init edge
//		for (var task : taskModule.getTasks().values()) {
//			var taskId = task.getId();
//			for (var preId : task.getBean().getPreTaskIds()) {
//				if (!graph.vertexSet().contains(preId))
//					throw new Exception("No PreTask: " + preId + "for Task: " + taskId);
//				graph.addEdge(preId, taskId);
//			}
//		}
//	}
//
//	// ======================================== Edit Mode ========================================
//	public static class EditMode {
//		private final DirectedAcyclicGraph<Long /* Task Id*/, DefaultEdge> graphToEdit;
//
//		public EditMode(DirectedAcyclicGraph<Long, DefaultEdge> graph) {
//			graphToEdit = graph;
//		}
//
//		public void addNewTask(TaskBase<?> task) throws Exception {
//			var taskId = task.getId();
//			if (checkTaskExist(taskId))
//				throw new Exception("Task: " + taskId + " already Exist");
//
//			graphToEdit.addVertex(taskId);
//
//			for (var preId : task.getBean().getPreTaskIds()) {
//				if (!checkTaskExist(preId))
//					throw new Exception("No PreTask: " + preId + "for Task: " + taskId);
//				graphToEdit.addEdge(preId, taskId);
//
//			}
//		}
//
//		public void removeTaskSafe(TaskBase<?> task) throws Exception {
//			var taskId = task.getId();
//			if (!checkTaskExist(taskId))
//				throw new Exception("Task: " + taskId + " NOT Exist");
//
//			var preTaskIds = graphToEdit.getAncestors(taskId);
//			var afterTaskIds = graphToEdit.getDescendants(taskId);
//		}
//
//		public void removeTaskLink() {
//			Supplier<Stream<Long>> zeroInAndOutNodesSupplier = () -> graphToEdit.vertexSet().stream().filter(task -> graphToEdit.inDegreeOf(task) == 0 && graphToEdit.outDegreeOf(task) == 0); // 寻找离散任务点
//			var zeroInAndOutNodes = zeroInAndOutNodesSupplier.get();
//		}
//
//		public void End() {
//
//		}
//
//		private boolean checkTaskExist(long id) {
//			return graphToEdit.vertexSet().contains(id);
//		}
//	}
//
//	public EditMode enterEditMode() {
//		return new EditMode(graph);
//	}

//	public void addNewTask(TaskBase task) {
//		graph.addVertex(task);
//		for (var preTaskName : task.getBean().getPreTasks()) {
//			var preTasks = getTaskByName(preTaskName);
//			graph.addEdge(preTasks, task); // 会再添加过程中检查任务图结构是否合法
//		}
//	}

//	private void buildGraph() {
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
//	}

//	private TaskBase getTaskByName(String taskName) {
//		Supplier<Stream<TaskBase>> supplier = () -> graph.vertexSet().stream().filter(task -> Objects.equals(task.getName(), taskName));
//		if (supplier.get().findAny().isEmpty()) {
//			throw new RuntimeException("Task not found: " + taskName);
//		}
//		return supplier.get().findAny().get();
//	}
//
//	private TaskBase[] getTasksOfZeroInAndOutDegree() {
//		Supplier<Stream<TaskBase>> supplier = () -> graph.vertexSet().stream().filter(task -> graph.inDegreeOf(task) == 0 && graph.outDegreeOf(task) == 0); // 寻找离散任务点
//		return supplier.get().toArray(TaskBase[]::new);
//	}
}
