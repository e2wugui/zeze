package Zeze.Raft;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import Zeze.Util.LongConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StateMachine extends ReentrantLock {
	private static final Logger logger = LogManager.getLogger(StateMachine.class);

	private Raft raft;
	private final LongConcurrentHashMap<Supplier<Log>> logFactorys = new LongConcurrentHashMap<>();

	public StateMachine() {
		addFactory(HeartbeatLog.TypeId_, HeartbeatLog::new);
	}

	public Raft getRaft() {
		return raft;
	}

	protected void setRaft(Raft value) {
		raft = value;
	}

	// 建议在继承类的构造里面注册LogFactory。
	protected void addFactory(int logTypeId, Supplier<Log> factory) {
		if (logFactorys.putIfAbsent(logTypeId, factory) != null)
			throw new IllegalStateException("Duplicate Log Id");
	}

	public Log logFactory(int logTypeId) {
		Supplier<Log> factory = logFactorys.get(logTypeId);
		if (factory != null)
			return factory.get();
		logger.fatal("Unknown Log: {}", logTypeId, new Exception());
		raft.fatalKill();
		return null;
	}

	// 把 StateMachine 里面的数据系列化到 path 指定的文件中。
	// 需要自己访问的并发特性。返回快照建立时的Raft.LogSequence.Index。
	// 原子性建议伪码如下：
	// lock (Raft) // 这会阻止对 StateMachine 的写请求。
	// {
	//     var lastAppliedLog = Raft.LogSequence.LastAppliedLog();
	//     LastIncludedIndex = lastAppliedLog.Index;
	//     LastIncludedTerm = lastAppliedLog.Term;
	//     MyData.SerializeToFile(path);
	//     Raft.LogSequence.CommitSnapshot(path, LastIncludedIndex);
	// }
	//
	// 上面的问题是，数据很大时，SerializeToFile时间比较长，会导致服务不可用。
	// 这时候需要自己优化并发。如下：
	// lock (Raft)
	// {
	//     var lastAppliedLog = Raft.LogSequence.LastAppliedLog();
	//     LastIncludedIndex = lastAppliedLog.Index;
	//     LastIncludedTerm = lastAppliedLog.Term;
	//     // 设置状态，如果限制只允许一个snapshot进行，
	//     // 新进的snapshot调用返回false。
	//     MyData.StartSerializeToFile();
	// }
	// MyData.ConcurrentSerializeToFile(path);
	// lock (Raft)
	// {
	//     // 清理一些状态。
	//     MyData.EndSerializeToFile();
	//     Raft.LogSequence.CommitSnapshot(path, LastIncludedIndex);
	// }
	//
	// return true;
	//
	// 这样在保存数据到文件的过程中，服务可以继续进行。
	public abstract SnapshotResult snapshot(String path) throws Exception;

	public static final class SnapshotResult {
		public boolean success;
		public long lastIncludedIndex;
		public long lastIncludedTerm;
		public long checkPointNanoTime;
		public long backupNanoTime;
		public long zipNanoTime;
		public long totalNanoTime;
	}

	/**
	 * 从上一个快照中重建 StateMachine。
	 * Raft 处理 InstallSnapshot 到达最后一个数据时，调用这个方法。
	 * 然后 Raft 会从 LastIncludedIndex 后面开始复制日志。进入正常的模式。
	 */
	public abstract void loadSnapshot(String path) throws Exception;

	/**
	 * 没有快照的时候，stateMachine可能需要重置。
	 * 需要重置的重载这个方法。
	 * 比如stateMachine使用RocksDB存储数据的，某些情况下逻辑操作和旧数据相关，需要从空的数据库开始时，重载这个方法先清空数据库。
	 */
	public void reset() {

	}
}
