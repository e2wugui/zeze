package Zeze.Raft;

import java.util.Objects;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import Zeze.Util.LongConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.rocksdb.RocksDBException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Raft 复制状态机：注册 Log 工厂、apply 应用日志、快照的保存与装载。
 */
public abstract class StateMachine extends ReentrantLock {
	private static final Logger logger = LogManager.getLogger(StateMachine.class);

	private @Nullable Raft raft;
	private final LongConcurrentHashMap<Supplier<Log>> logFactorys = new LongConcurrentHashMap<>();

	public StateMachine() {
		addFactory(HeartbeatLog.TypeId_, HeartbeatLog::new);
	}

	public @Nullable Raft getRaft() {
		return raft;
	}

	protected void setRaft(@Nullable Raft value) {
		raft = value;
	}

	// 建议在继承类的构造里面注册LogFactory。
	protected void addFactory(int logTypeId, Supplier<Log> factory) {
		if (logFactorys.putIfAbsent(logTypeId, factory) != null)
			throw new IllegalStateException("Duplicate Log Id");
	}

	public @Nullable Log logFactory(int logTypeId) {
		Supplier<Log> factory = logFactorys.get(logTypeId);
		if (factory != null)
			return factory.get();
		logger.fatal("Unknown Log: {}", logTypeId, new Exception());
		Objects.requireNonNull(raft).fatalKill();
		return null;
	}

	/** 一条日志的应用入口；持久化状态机可将业务修改与应用完成事实同批提交。调用方持有Raft锁。 */
	public void apply(RaftLog log) throws Exception {
		Objects.requireNonNull(log.getLog()).apply(log, this);
	}

	/** 内部心跳可能触发leader-ready回调，原子状态机须先提交水位再触发它。 */
	protected static boolean isHeartbeatLog(RaftLog log) {
		return log.getLog() instanceof HeartbeatLog;
	}

	/** 水位必须与该索引的业务数据、终态存根在同一批次提交，不能仅单独记录lastApplied。 */
	public record AppliedWatermark(long index, long term) {
	}

	public @Nullable AppliedWatermark readAppliedWatermark() throws RocksDBException {
		return null;
	}

	/** 为true时，apply已经将unique终态存根与业务状态原子提交到状态机库。 */
	public boolean hasAtomicAppliedState() {
		return false;
	}

	public @Nullable UniqueRequestState getUniqueAppliedStub(IRaftRpc rpc) throws RocksDBException {
		return null;
	}

	public void removeExpiredUniqueStubTables(long expired) throws Exception {
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
	 * 启动恢复钩子：snapshotFile为null表示尚无已提交快照。默认从快照全量恢复，
	 * 无快照时reset。支持应用水位的状态机先校验本地库，失败再走默认恢复。
	 * InstallSnapshot的接收路径不走这里（语义不同：必须以传入快照为准）。
	 */
	public void recover(@Nullable String snapshotFile) throws Exception {
		if (snapshotFile != null)
			loadSnapshot(snapshotFile);
		else
			reset();
	}

	/**
	 * 没有快照的时候，stateMachine可能需要重置。
	 * 需要重置的重载这个方法。
	 * 比如stateMachine使用RocksDB存储数据的，某些情况下逻辑操作和旧数据相关，需要从空的数据库开始时，重载这个方法先清空数据库。
	 */
	public void reset() {

	}

	/**
	 * 状态机自定义的观测计数，仅用于日志输出（leader ready、saveLog 等处的 Count 字段），
	 * 默认 -1 表示无。主源码不instanceof测试壳类，需要观测的状态机重载本方法。
	 */
	public long getDebugCount() {
		return -1;
	}
}
