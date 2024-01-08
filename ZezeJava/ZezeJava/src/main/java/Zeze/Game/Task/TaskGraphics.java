package Zeze.Game.Task;

import java.util.HashSet;
import java.util.Set;
import Zeze.Builtin.Game.TaskModule.BTask;
import Zeze.Builtin.Game.TaskModule.BTaskConfig;
import Zeze.Builtin.Game.TaskModule.BTaskSet;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import org.rocksdb.RocksDBException;

public class TaskGraphics {
	private final RocksDatabase db; // db
	private final RocksDatabase.Table tasks; // 保存所有的任务配置。
	private final RocksDatabase.Table roots; // 所有的图的根。
	private final RocksDatabase.Table npcAcceptTasks; // npcId->tasks

	public TaskGraphics(String configFileName) throws RocksDBException {
		db = new RocksDatabase(configFileName);
		tasks = db.getOrAddTable("tasks");
		roots = db.getOrAddTable("roots");
		npcAcceptTasks = db.getOrAddTable("npcAcceptTasks");
	}

	public RocksDatabase.Table getRoots() {
		return roots;
	}

	public BTaskSet.Data getNpcAcceptTasks(int npcId) throws RocksDBException {
		var key = ByteBuffer.Allocate();
		key.WriteInt(npcId);
		var value = npcAcceptTasks.get(key.Bytes, key.ReadIndex, key.size());
		var taskSet = new BTaskSet.Data();
		if (null == value)
			return taskSet;
		taskSet.decode(ByteBuffer.Wrap(value));
		return taskSet;
	}

	public void putNpcAcceptTasks(int npcId, BTaskSet.Data taskSet) throws RocksDBException {
		if (npcId != 0) {
			var key = ByteBuffer.Allocate();
			key.WriteInt(npcId);
			var value = ByteBuffer.Allocate();
			taskSet.encode(value);
			npcAcceptTasks.put(key.Bytes, key.ReadIndex, key.size(), value.Bytes, value.ReadIndex, value.size());
		}
	}

	public void addNpcAcceptTask(int npcId, int taskId) throws RocksDBException {
		if (npcId != 0) {
			var taskSet = getNpcAcceptTasks(npcId);
			taskSet.getTaskIds().add(taskId);
			putNpcAcceptTasks(npcId, taskSet);
		}
	}

	public void removeNpcAcceptTask(int npcId, int taskId) throws RocksDBException {
		if (npcId != 0) {
			var taskSet = getNpcAcceptTasks(npcId);
			taskSet.getTaskIds().remove(taskId);
			putNpcAcceptTasks(npcId, taskSet);
		}
	}

	public void close() {
		db.close();
	}

	public BTaskConfig.Data getTask(int taskId) throws RocksDBException {
		var key = ByteBuffer.Allocate();
		key.WriteInt(taskId);
		var value = tasks.get(key.Bytes, key.ReadIndex, key.size());
		if (value == null)
			return null;
		var data = new BTaskConfig.Data();
		data.decode(ByteBuffer.Wrap(value));
		return data;
	}

	public Set<Integer> getAllGraphicsNode(BTaskConfig.Data hint) throws RocksDBException {
		var result = new HashSet<Integer>();
		recursiveAllGraphicsNode(hint, result);
		return result;
	}

	public void recursiveAllGraphicsNode(BTaskConfig.Data hint, Set<Integer> result) throws RocksDBException {
		if (result.contains(hint.getTaskId())) // 保险起见，实际上应该不可能发生重复的任务。
			return;
		result.add(hint.getTaskId());
		for (var prepose : hint.getPreposeTasks()) {
			recursiveAllGraphicsNode(getTask(prepose), result);
		}
		for (var follow : hint.getFollowTasks()) {
			recursiveAllGraphicsNode(getTask(follow), result);
		}
	}

	// 编辑方法。直接用于任务编辑器或者用于把任务编辑器自己的存储格式转换运行格式。
	public TaskConfig getTaskConfig(int taskId) throws Exception {
		var data = getTask(taskId);
		if (data == null)
			return new TaskConfig();
		return new TaskConfig(data);
	}

	public void putTask(TaskConfig config) throws RocksDBException {
		var task = config.prepareData();
		var key = ByteBuffer.Allocate();
		key.WriteInt(task.getTaskId());
		var value = ByteBuffer.Allocate();
		task.encode(value);

		// 保存npcAcceptTasks索引
		if (config.getAcceptNpc() != config.getOriginAcceptNpcId()) {
			removeNpcAcceptTask(config.getOriginAcceptNpcId(), task.getTaskId());
			addNpcAcceptTask(config.getAcceptNpc(), task.getTaskId());
		}

		tasks.put(key.Bytes, key.ReadIndex, key.size(), value.Bytes, value.ReadIndex, value.size());
	}

	public void removeTask(int taskId) throws RocksDBException {
		var key = ByteBuffer.Allocate();
		key.WriteInt(taskId);
		tasks.delete(key.Bytes, key.ReadIndex, key.size());
	}

	public void putRoot(int taskId) throws RocksDBException {
		var key = ByteBuffer.Allocate();
		key.WriteInt(taskId);
		roots.put(key.Bytes, key.ReadIndex, key.size(), ByteBuffer.Empty, 0, ByteBuffer.Empty.length);
	}

	public void removeRoot(int taskId) throws RocksDBException {
		var key = ByteBuffer.Allocate();
		key.WriteInt(taskId);
		roots.delete(key.Bytes, key.ReadIndex, key.size());
	}

	// todo 上面的任务编辑辅助方法还不够高级，比如删除任务，当这个任务是root时，也是需要删除root的。类似这样的编辑关联。
	//  当开始写编辑器，并且直接使用这个类作为后端存储时，再来加强。
}
