package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskModule.BTask;
import Zeze.Builtin.Game.TaskModule.BTaskConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import org.rocksdb.RocksDBException;

public class TaskGraphics {
	private final RocksDatabase db; // db
	private final RocksDatabase.Table tasks; // 保存所有的任务配置。
	private final RocksDatabase.Table roots; // 所有的图的根。

	public TaskGraphics(String configFileName) throws RocksDBException {
		db = new RocksDatabase(configFileName);
		tasks = db.getOrAddTable("tasks");
		roots = db.getOrAddTable("roots");
	}

	public RocksDatabase.Table getRoots() {
		return roots;
	}

	public BTask acceptTask(int taskId) throws RocksDBException {
		var key = ByteBuffer.Allocate();
		key.WriteInt(taskId);
		var value = tasks.get(key.Bytes, key.ReadIndex, key.size());
		if (null == value)
			throw new RuntimeException("task not exist. " + taskId);
		var config = new BTaskConfig.Data();
		config.decode(ByteBuffer.Wrap(value));
		return config.getTaskConditions().toBean();
	}

	public void close() {
		db.close();
	}

	// 编辑方法。直接用于任务编辑器或者用于把任务编辑器自己的存储格式转换运行格式。
	public void putTask(TaskConfig config) throws RocksDBException {
		var task = config.prepareData();
		var key = ByteBuffer.Allocate();
		key.WriteInt(task.getTaskId());
		var value = ByteBuffer.Allocate();
		task.encode(value);
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
}
