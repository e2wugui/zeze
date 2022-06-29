package Zeze.Raft;

import java.util.function.Supplier;
import Zeze.Util.LongConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StateMachine {
	private static final Logger logger = LogManager.getLogger(StateMachine.class);

	private Raft Raft;
	private final LongConcurrentHashMap<Supplier<Log>> LogFactorys = new LongConcurrentHashMap<>();

	public StateMachine() {
		AddFactory(new HeartbeatLog().getTypeId(), HeartbeatLog::new);
	}

	public Raft getRaft() {
		return Raft;
	}

	protected void setRaft(Raft value) {
		Raft = value;
	}

	// 建议在继承类的构造里面注册LogFactory。
	protected void AddFactory(int logTypeId, Supplier<Log> factory) {
		if (LogFactorys.putIfAbsent(logTypeId, factory) != null)
			throw new IllegalStateException("Duplicate Log Id");
	}

	public Log LogFactory(int logTypeId) {
		Supplier<Log> factory = LogFactorys.get(logTypeId);
		if (factory != null)
			return factory.get();
		logger.fatal("Unknown Log: " + logTypeId, new Exception());
		Raft.FatalKill();
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
	public abstract SnapshotResult Snapshot(String path) throws Throwable;

	public static final class SnapshotResult {
		public boolean success;
		public long LastIncludedIndex;
		public long LastIncludedTerm;
	}

	/**
	 * 从上一个快照中重建 StateMachine。
	 * Raft 处理 InstallSnapshot 到达最后一个数据时，调用这个方法。
	 * 然后 Raft 会从 LastIncludedIndex 后面开始复制日志。进入正常的模式。
	 */
	public abstract void LoadSnapshot(String path) throws Throwable;
}
