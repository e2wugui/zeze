package Zeze.Raft;

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
		//noinspection DataFlowIssue
		raft.fatalKill();
		return null;
	}

	/** 一条日志的应用入口；持久化状态机可将业务修改与应用完成事实同批提交。调用方持有Raft锁。 */
	public void apply(RaftLog log) throws Exception {
		//noinspection DataFlowIssue
		log.getLog().apply(log, this);
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

	/**
	 * 将状态机数据序列化快照到 path 指定的文件，并返回快照包含的日志边界（lastIncludedIndex/Term）。
	 * 需保证与日志应用的原子一致性：持有 Raft 锁获取 lastAppliedLog 边界并建立一致性视图，
	 * 大数据量时可在锁外并发写出文件，完成后再持 Raft 锁调用 {@code LogSequence.commitSnapshot} 提交。
	 */
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

	/** 从指定快照文件重建状态机（InstallSnapshot 接收完成或启动恢复时调用）。 */
	public abstract void loadSnapshot(String path) throws Exception;

	/**
	 * 启动恢复入口：有已提交快照时默认全量装载，否则执行 reset；支持应用水位的子类可先校验本地库。
	 * InstallSnapshot 接收路径直接调用 {@link #loadSnapshot}，不经此方法。
	 */
	public void recover(@Nullable String snapshotFile) throws Exception {
		if (snapshotFile != null)
			loadSnapshot(snapshotFile);
		else
			reset();
	}

	/** 无快照启动时重置状态机（持久化状态机如需从空库开始可重载此方法）。 */
	public void reset() {

	}

	/** 状态机自定义观测计数，用于日志输出；默认 -1 表示未提供。 */
	public long getDebugCount() {
		return -1;
	}
}
