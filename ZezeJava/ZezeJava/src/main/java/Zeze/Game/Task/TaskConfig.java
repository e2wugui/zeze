package Zeze.Game.Task;

import java.util.ArrayList;
import java.util.HashSet;
import Zeze.Builtin.Game.TaskModule.BCondition;
import Zeze.Builtin.Game.TaskModule.BPhase;
import Zeze.Builtin.Game.TaskModule.BTaskConfig;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Data;
import java.util.List;

/**
 * 任务配置辅助类，
 * 主要提供一些编辑方法。
 * 实际应用可以继承并提供更多的CustomData的编辑方法。
 * 用于编辑器。
 */
public class TaskConfig {
	private final BTaskConfig.Data data;
	private int originAcceptNpcId;

	// 编辑中的直接条件和阶段（阶段中的条件）。
	private final List<Condition> editingConditions = new ArrayList<>();
	private final List<PhaseConfig> editingPhases = new ArrayList<>();

	public TaskConfig(BTaskConfig.Data data) throws Exception {
		this.data = data;
		originAcceptNpcId = data.getAcceptNpc(); // 先保存记住
		for (var c : data.getTaskConditions().getConditions())
			editingConditions.add(Condition.construct(c));

		for (var p : data.getTaskConditions().getPhases()) {
			var ep = new PhaseConfig(p.getDescription());
			editingPhases.add(ep);
			for (var c : p.getConditions())
				ep.getConditions().add(Condition.construct(c));
		}
	}

	public int getOriginAcceptNpcId() {
		return originAcceptNpcId;
	}

	public TaskConfig() {
		this.data = new BTaskConfig.Data();
	}

	private static BCondition.Data toData(Condition condition) {
		var cData = new BCondition.Data();
		cData.setClassName(condition.getClass().getName());
		var p = ByteBuffer.Allocate();
		condition.encode(p);
		cData.setParameter(new Binary(p));
		return cData;
	}

	// 保存前由TaskGraphics调用。
	// 做最后的数据转换准备。
	BTaskConfig.Data prepareData() {
		// 把编辑中的条件转换到data里面。
		data.getTaskConditions().getIndexSet().clear();
		data.getTaskConditions().getConditions().clear();
		for (var i = 0; i < editingConditions.size(); ++i) {
			var condition = toData(editingConditions.get(i));
			data.getTaskConditions().getConditions().add(condition);
			data.getTaskConditions().getIndexSet().add(i);
		}

		data.getTaskConditions().getPhases().clear();
		for (var editingPhase : editingPhases) {
			var phase = new BPhase.Data();
			// not need phase.getIndexSet().clear();
			for (var i = 0; i < editingPhase.getConditions().size(); ++i) {
				var condition = toData(editingPhase.getConditions().get(i));
				phase.getConditions().add(condition);
				phase.getIndexSet().add(i);
			}
		}
		return data;
	}

	public int getTaskId() {
		return data.getTaskId();
	}

	public void setTaskId(int taskId) {
		data.setTaskId(taskId);
		data.getTaskConditions().setTaskId(taskId);
	}

	public HashSet<Integer> getPreposeTasks() {
		return data.getPreposeTasks();
	}

	public HashSet<Integer> getFollowTasks() {
		return data.getFollowTasks();
	}

	public int getAcceptNpc() {
		return data.getAcceptNpc();
	}

	public void setAcceptNpc(int npcId) {
		data.setAcceptNpc(npcId);
	}

	public int getFinishNpc() {
		return data.getFinishNpc();
	}

	public void setFinishNpc(int npcId) {
		data.setFinishNpc(npcId);
	}

	public Data getExtendData() {
		return data.getExtendData().getData();
	}

	public void setExtendData(Data data) {
		this.data.getExtendData().setData(data);
	}

	public List<Condition> getConditions() {
		return editingConditions;
	}

	public List<PhaseConfig> getPhases() {
		return editingPhases;
	}
}
