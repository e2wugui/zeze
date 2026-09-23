package Zeze.Raft;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.ZipFile;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action0;
import Zeze.Util.Action2;
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteOptions;

public class LogSequence {
	static final Logger logger = LogManager.getLogger(LogSequence.class);
	private static final boolean isDebugEnabled = logger.isDebugEnabled();
	public static final String snapshotFileName = "snapshot.dat";

	// legacy快照内嵌代次：代际化后不再写入，读取仅供构造器迁移引导。
	static final String snapshotManifestEntryName = "zeze.snapshot.manifest";

	// 读legacy快照内嵌代次；无entry、文件不存在或损坏返回null（迁移按N=F处理）。
	static Long readSnapshotManifest(Path snapshotPath) {
		if (!Files.isRegularFile(snapshotPath))
			return null;
		try (var zip = new ZipFile(snapshotPath.toFile())) {
			var entry = zip.getEntry(snapshotManifestEntryName);
			if (entry == null)
				return null;
			var text = new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
			return Long.parseLong(text.strip());
		} catch (Exception e) {
			logger.warn("read snapshot manifest error. path={}", snapshotPath, e);
			return null;
		}
	}

	private final Raft raft;
	private volatile long term; // getTerm()存在锁外读取（Server.trySendLeaderIs），需要可见性
	private long lastIndex;
	private long firstIndex; // 用来处理NextIndex回溯时限制搜索。snapshot需要修订这个值。
	private long commitIndex;
	private long lastApplied;

	// 这个不是日志需要的，因为持久化，所以就定义在这里吧。
	private String voteFor;
	private boolean nodeReady;
	private long lastLeaderCommitIndex;

	public volatile TaskCompletionSource<Boolean> removeLogBeforeFuture;
	public volatile boolean logsAvailable;

	private long leaderActiveTime = System.currentTimeMillis(); // Leader, Follower

	private WriteOptions writeOptions = RocksDatabase.getSyncWriteOptions();
	private RocksDatabase.Table logs;
	private RocksDatabase database;
	private final boolean sharedDatabase;
	private RocksDatabase.Table rafts;
	private final LongConcurrentHashMap<UniqueRequestSet> uniqueRequestSets = new LongConcurrentHashMap<>();

	private final byte[] raftsTermKey;
	private final byte[] raftsVoteForKey;
	private final byte[] raftsFirstIndexKey;
	private final byte[] raftsNodeReadyKey; // 只会被写一次，所以这个优化可以不做，统一形式吧。
	private final byte[] lastSnapshotIndexKey;

	public volatile TaskCompletionSource<Boolean> applyFuture; // follower background apply task
	private final LongConcurrentHashMap<RaftLog> leaderAppendLogs = new LongConcurrentHashMap<>();

	// leader侧InstallSnapshot发送会话登记表（构造仅存引用，运行时经本类延迟读取）。
	private final SendSnapshotting sendSnapshotting = new SendSnapshotting(this);
	private long lastSnapshotIndex;
	private boolean snapshotting = false; // 是否正在创建Snapshot过程中，用来阻止新的创建请求。

	// gen清扫重试名单：Windows句柄锁删除失败的旧代，onLowPrecisionTimer周期重试。
	private final ConcurrentLinkedQueue<Path> pendingDeleteGenFiles = new ConcurrentLinkedQueue<>();

	static {
		RocksDB.loadLibrary();
	}

	public Raft getRaft() {
		return raft;
	}

	public long getTerm() {
		return term;
	}

	public long getLastIndex() {
		return lastIndex;
	}

	public long getFirstIndex() {
		return firstIndex;
	}

	public long getCommitIndex() {
		return commitIndex;
	}

	public long getLastApplied() {
		return lastApplied;
	}

	String getVoteFor() {
		return voteFor;
	}

	boolean getNodeReady() {
		return nodeReady;
	}

	// 初始化的时候会加入一条日志(Index=0，不需要真正apply)，
	// 以后Snapshot时，会保留LastApplied的。
	// 所以下面方法不会返回空。
	// 除非什么例外发生。那就抛空指针异常吧。
	public RaftLog lastAppliedLogTermIndex() throws RocksDBException {
		return RaftLog.decodeTermIndex(readLogBytes(lastApplied));
	}

	private void saveFirstIndex(long newFirstIndex) throws RocksDBException {
		var firstIndexValue = ByteBuffer.Allocate(9);
		firstIndexValue.WriteLong(newFirstIndex);
		rafts.put(writeOptions, raftsFirstIndexKey, 0, raftsFirstIndexKey.length,
				firstIndexValue.Bytes, 0, firstIndexValue.WriteIndex);
		firstIndex = newFirstIndex;
	}

	public void commitSnapshot(String path, long newFirstIndex) throws IOException, RocksDBException {
		Path path1 = Paths.get(path);
		if (!raft.getRaftConfig().isSnapshotCommitDelayed()) {
			_commitSnapshot(path1, newFirstIndex);
			return;
		}
		// 延时提交（延迟一代）：held候选=已发布的gen文件（index>firstIndex，指针未翻）。
		// 新候选到达时先翻上一代held（提交），再发布新候选为held；崩溃时未翻的held即孤儿，启动清扫。
		raft.lock();
		try {
			var held = findHeldGenCandidates();
			if (!held.isEmpty()) {
				// 提交最大的held（实际上最多只有一个held，这里的写法能处理多个）。
				var biggestIndex = held.lastKey();
				_flipCommittedGen(biggestIndex);
				// 删除多余的held。一般不会发生。
				for (var file : held.headMap(biggestIndex).values())
					Files.deleteIfExists(file);
			}
			// 过期防御（与立即提交同口径）：过期候选直接丢弃。
			if (newFirstIndex < firstIndex) {
				discardStaleCandidate(path1, newFirstIndex);
				return;
			}
			publishGenCandidate(path1, newFirstIndex);
		} finally {
			raft.unlock();
		}
	}

	// 接收InstallSnapshot的提交必须立即生效：随后马上loadSnapshot并按新边界重置，
	// 走延时会导致加载旧快照、firstIndex不推进。
	void commitSnapshotNow(Path path, long newFirstIndex) throws IOException, RocksDBException {
		_commitSnapshot(path, newFirstIndex);
	}

	// 过期候选丢弃：不翻指针、删候选、顺带清扫旧代。调用方持raft锁。
	private void discardStaleCandidate(Path path, long newFirstIndex) throws IOException {
		logger.warn("discard stale snapshot: path={} newFirstIndex={} < firstIndex={}",
				path, newFirstIndex, firstIndex);
		Files.deleteIfExists(path);
		startSweepGenFiles();
	}

	// 代际提交：发布（fsync+原子改名为不可变gen文件）→ 单点指针翻转（saveFirstIndex）。
	// 崩溃穷举皆安全：发布前崩=残留由启动清扫；发布后翻转前崩=gen孤儿被清扫；翻转后=已提交。
	private void _commitSnapshot(Path path, long newFirstIndex) throws IOException, RocksDBException {
		raft.lock();
		try {
			// 防御竞态：本地snapshot生成期间可能提交了更新的InstallSnapshot，
			// 过期候选提交会回退firstIndex，必须丢弃。
			if (newFirstIndex < firstIndex) {
				discardStaleCandidate(path, newFirstIndex);
				return;
			}
			publishGenCandidate(path, newFirstIndex);
			_flipCommittedGen(newFirstIndex);
		} finally {
			raft.unlock();
		}
	}

	// 翻转指针提交已就位的gen文件（延时held的提交路径）。调用方持raft锁。
	private void _flipCommittedGen(long newFirstIndex) throws RocksDBException {
		saveFirstIndex(newFirstIndex); // RocksDB sync put —— 唯一提交动作
		startSweepGenFiles();          // 删 index < firstIndex 的旧代（失败进重试名单）
		startRemoveLogOnlyBefore(newFirstIndex);
	}

	// 候选发布：fsync补齐（zip与接收侧分块写均无fsync）→ 原子改名为不可变gen文件。
	// 发布≠提交；REPLACE回退覆盖同index旧候选。调用方持raft锁。
	private void publishGenCandidate(Path path, long newFirstIndex) throws IOException {
		AtomicFileWriter.fsync(path);
		var gen = genSnapshotPath(newFirstIndex);
		try {
			Files.move(path, gen, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(path, gen, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private File[] listDbHomeFiles() {
		return new File(raft.getRaftConfig().getDbHome()).listFiles();
	}

	// held候选：index>firstIndex的gen文件，按index排序。调用方持raft锁。
	private TreeMap<Long, Path> findHeldGenCandidates() {
		var held = new TreeMap<Long, Path>();
		var files = listDbHomeFiles();
		if (files == null)
			return held;
		for (var file : files) {
			var index = parseGenSnapshotIndex(file);
			if (index != null && index > firstIndex)
				held.put(index, file.toPath());
		}
		return held;
	}

	// gen文件名解析：snapshot.dat.<纯数字>→index；其他返回null。package-private供dump助手复用。
	static Long parseGenSnapshotIndex(File file) {
		if (!file.isFile())
			return null;
		var fileName = file.getName();
		if (!fileName.startsWith(snapshotFileName + "."))
			return null;
		var middle = fileName.substring(snapshotFileName.length() + 1);
		if (middle.isEmpty() || !middle.chars().allMatch(Character::isDigit))
			return null;
		try {
			return Long.parseLong(middle);
		} catch (NumberFormatException e) {
			return null; // 超长数字串
		}
	}

	// 运行期gen清扫：只删index<firstIndex的旧代（>firstIndex是延时held或进行中发布）；
	// 删除失败进重试名单。调用方持raft锁。
	private void startSweepGenFiles() {
		try {
			var files = listDbHomeFiles();
			if (files != null) {
				for (var file : files) {
					var index = parseGenSnapshotIndex(file);
					if (index == null || index >= firstIndex)
						continue;
					try {
						Files.deleteIfExists(file.toPath());
						logger.info("{} sweep old gen snapshot: {}", raft.getName(), file.getName());
					} catch (IOException e) {
						pendingDeleteGenFiles.add(file.toPath());
						logger.warn("sweep old gen snapshot failed, queued for retry. file={}", file, e);
					}
				}
			}
		} finally {
			drainPendingDeleteGenFiles();
		}
	}

	// 重试名单冲刷：成功或文件消失即摘除，失败留到下一轮。
	void drainPendingDeleteGenFiles() {
		for (var it = pendingDeleteGenFiles.iterator(); it.hasNext(); ) {
			var pending = it.next();
			try {
				Files.deleteIfExists(pending);
				it.remove();
			} catch (IOException e) {
				logger.warn("retry delete gen snapshot failed, keep queued. file={}", pending, e);
				return; // 本轮到此为止，下一轮再试
			}
		}
	}

	private RocksIterator newLogsIterator() {
		raft.lock();
		try {
			return logs.iterator();
		} finally {
			raft.unlock();
		}
	}

	private void startRemoveLogOnlyBefore(long index) {
		TaskCompletionSource<Boolean> future;
		raft.lock();
		try {
			if (removeLogBeforeFuture != null || !logsAvailable || raft.isShutdown)
				return;
			future = removeLogBeforeFuture = new TaskCompletionSource<>();
		} finally {
			raft.unlock();
		}

		// 直接对 RocksDb 多线程访问，这里就不做多线程保护了。
		TaskSpec.ofAction(() -> {
			try {
				try (var it = newLogsIterator()) {
					it.seekToFirst();
					while (logsAvailable && !raft.isShutdown && it.isValid()) {
						// 这里只需要log的Index，直接从key里面获取了。
						if (ByteBuffer.Wrap(it.key()).ReadLong() >= index) {
							future.setResult(true);
							return;
						}

						var key = it.key();
						logs.delete(writeOptions, key);

						// 删除快照前的日志时不删唯一请求存根（快照建立时刻稍前的请求仍需唯一保证），存根自行过期清理。
						// 注意：完全崩溃换新机后仍有小概率无法判断唯一，较好的做法是从工作节点复制unique/作为初始数据。

						//if (raftLog.Log.Unique.RequestId > 0)
						//    OpenUniqueRequests(raftLog.Log.CreateTime).Remove(raftLog);
						it.next();
					}
				}
			} finally {
				// 只操作自己创建的future：无条件置null共享字段会误杀已被并发替换的future
				// （初始化即启动本任务，收尾延迟时等待点可能已被外部重新赋值）。字段清除与创建同持raft锁配对。
				future.setResult(false);
				raft.lock();
				try {
					if (removeLogBeforeFuture == future)
						removeLogBeforeFuture = null;
				} finally {
					raft.unlock();
				}
			}
		}).name("RemoveLogBefore" + index).run();
	}

	/*
	private void removeLogReverse(long startIndex, long firstIndex)
	{
	    for (var index = startIndex; index >= firstIndex; index--)
	        RemoveLog(index);
	}
	*/

	public long getLeaderActiveTime() {
		return leaderActiveTime;
	}

	// 本轮选举超时采样值。每次重置计时（收到Leader消息/状态转换）时重新采样一次，
	// 而不是每次比较时重新随机——这才符合raft随机化选举超时的本意。
	private volatile long electionTimeoutSample;

	long getElectionTimeout() {
		var sample = electionTimeoutSample;
		return sample > 0 ? sample : raft.getRaftConfig().getElectionTimeout();
	}

	void setLeaderActiveTime(long value) {
		leaderActiveTime = value;
		electionTimeoutSample = raft.getRaftConfig().getElectionTimeout();
	}

	final class UniqueRequestSet {
		private final RocksDatabase.Table table;

		public UniqueRequestSet(String tableName) {
			try {
				table = database.getOrAddTable(tableName);
			} catch (RocksDBException e) {
				throw Task.forceThrow(e);
			}
		}

		private void put(RaftLog log, boolean isApply) throws RocksDBException {
			var key = ByteBuffer.Allocate(32);
			log.getLog().getUnique().encode(key);

			// 先读取并检查状态，减少写操作。
			var existBytes = table.get(RocksDatabase.getDefaultReadOptions(), key.Bytes, 0, key.WriteIndex);
			if (!isApply && existBytes != null)
				throw new RaftRetryException("Duplicate Request Found = " + log.getLog().getUnique());

			if (existBytes != null) {
				var existState = new UniqueRequestState();
				existState.decode(ByteBuffer.Wrap(existBytes));
				if (existState.isApplied())
					return;
			}

			var value = ByteBuffer.Allocate(32);
			new UniqueRequestState(log, isApply).encode(value);
			table.put(writeOptions, key.Bytes, 0, key.WriteIndex, value.Bytes, 0, value.WriteIndex);
		}

		public void save(RaftLog log) throws RocksDBException {
			put(log, false);
		}

		public void apply(RaftLog log) throws RocksDBException {
			put(log, true);
		}

		public void remove(RaftLog log) throws RocksDBException {
			var key = ByteBuffer.Allocate(32);
			log.getLog().getUnique().encode(key);
			table.delete(writeOptions, key.Bytes, 0, key.WriteIndex);
		}

		public UniqueRequestState getRequestState(IRaftRpc raftRpc) throws RocksDBException {
			var key = ByteBuffer.Allocate(32);
			raftRpc.getUnique().encode(key);
			var val = table.get(RocksDatabase.getDefaultReadOptions(), key.Bytes, 0, key.WriteIndex);
			if (val == null)
				return null;
			var bb = ByteBuffer.Wrap(val);
			var state = new UniqueRequestState();
			state.decode(bb);
			return state;
		}
	}

	public static void deleteDirectory(File path) {
		File[] contents = path.listFiles();
		if (contents != null)
			for (File f : contents)
				if (!Files.isSymbolicLink(f.toPath()))
					deleteDirectory(f);
		//noinspection ResultOfMethodCallIgnored
		path.delete();
	}

	public static void deletedDirectoryAndCheck(File path, int checkCount) {
		while (--checkCount >= 0) {
			deleteDirectory(path);
			if (!path.exists())
				return;
			try {
				Thread.sleep(300);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		}
		throw new IllegalStateException("delete '" + path + "' failed");
	}

	public static void deletedDirectoryAndCheck(File path) {
		deletedDirectoryAndCheck(path, 10);
	}

	void removeExpiredUniqueRequestSet() throws Exception {
		RaftConfig raftConf = raft.getRaftConfig();
		long expired = System.currentTimeMillis() - (raftConf.getUniqueRequestExpiredDays() + 1) * 86400_000L;

		// unique表名实际是 <raftName>.unique.<yyyy>.<M>.<d>（see makeUniqueRequestTableName），
		// 必须按本raft的名字过滤；多raft共享同一database时，database里还有其他raft的表，
		// 只能清理本raft的（其他raft由自己的清理任务负责）。
		for (var tableName : database.getTableMap().keySet()) {
			var date = parseUniqueRequestDate(raft.getName(), tableName);
			if (date == null || date.getTime() >= expired)
				continue;
			var opened = uniqueRequestSets.remove(toUniqueRequestKey(date));
			if (null != opened)
				opened.table.drop(); // 包含opened.close。
			else
				database.getOrAddTable(tableName).drop();
		}
	}

	void cancelPendingAppendLogFutures() throws Exception {
		for (var job : leaderAppendLogs)
			job.cancelCallback();
		leaderAppendLogs.clear();
	}

	void close() throws Exception {
		// must after set Raft.IsShutdown = false;
		cancelPendingAppendLogFutures();

		raft.lock();
		try {
			if (logs != null) {
				logger.info("close logs: {}", raft.getRaftConfig().getDbHome());
				//logs.close();
				logs = null;
			}

			rafts = null;
// 共享db，外部关闭。
			if (!sharedDatabase && database != null) {
				logger.info("close database: {}", raft.getRaftConfig().getDbHome());
				database.close();
				database = null;
			}

			uniqueRequestSets.clear();
		} finally {
			raft.unlock();
		}
	}

	public static byte[] makeRaftsKey(int key) {
		var bb = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(key));
		bb.WriteInt(key);
		return bb.CopyIf();
	}

	public LogSequence(Raft raft) throws RocksDBException {
		this(raft, null);
	}

	public LogSequence(Raft raft, RocksDatabase database) throws RocksDBException {
		this.raft = raft;
		this.sharedDatabase = database != null;
		this.database = database;
		if (this.database == null)
			this.database = new RocksDatabase(Paths.get(raft.getRaftConfig().getDbHome(), "db").toString());

		this.rafts = this.database.getOrAddTable(raft.getName() + ".rafts");
		{
			// Read Term
			raftsTermKey = makeRaftsKey(0);
			var termValue = rafts.get(RocksDatabase.getDefaultReadOptions(), raftsTermKey);
			term = termValue != null ? ByteBuffer.Wrap(termValue).ReadLong() : 0;
			// Read VoteFor
			raftsVoteForKey = makeRaftsKey(1);
			var voteForValue = rafts.get(RocksDatabase.getDefaultReadOptions(), raftsVoteForKey);
			voteFor = voteForValue != null ? ByteBuffer.Wrap(voteForValue).ReadString() : "";
			// Read FirstIndex 由于snapshot并发，Logs中的第一条记录可能不是FirstIndex了。
			raftsFirstIndexKey = makeRaftsKey(2);
			var firstIndexValue = rafts.get(RocksDatabase.getDefaultReadOptions(), raftsFirstIndexKey);
			firstIndex = firstIndexValue != null ? ByteBuffer.Wrap(firstIndexValue).ReadLong() : -1;
			// -1 no committed snapshot. will re-initialize later.
			// NodeReady
			// 节点第一次启动，包括机器毁坏后换了新机器再次启动时为 false。
			// 当满足以下条件之一：
			// 1. 成为Leader并且Ready
			// 2. 成为Follower并在处理AppendEntries时观察到LeaderCommit发生了变更
			// 满足条件以后设置 NodeReady 为 true。
			// 这个条件影响投票逻辑：NodeReady 为 true 以前，只允许给 Candidate.LastIndex == 0 的节点投票。
			raftsNodeReadyKey = makeRaftsKey(3);
			var nodeReadyValue = rafts.get(RocksDatabase.getDefaultReadOptions(), raftsNodeReadyKey);
			if (nodeReadyValue != null)
				nodeReady = ByteBuffer.Wrap(nodeReadyValue).ReadBool();
			lastSnapshotIndexKey = makeRaftsKey(4);
			var lastSnapshotIndexValue = rafts.get(RocksDatabase.getDefaultReadOptions(), lastSnapshotIndexKey);
			lastSnapshotIndex = lastSnapshotIndexValue != null ? ByteBuffer.Wrap(lastSnapshotIndexValue).ReadLong() : 0;
		}

		logs = this.database.getOrAddTable(raft.getName() + ".logs");
		{
			// Read Last Log Index
			try (var itLast = logs.iterator()) {
				itLast.seekToLast();
				if (itLast.isValid())
					lastIndex = RaftLog.decodeTermIndex(itLast.value()).getIndex();
				else {
					// empty. add one for prev.
					saveLog(new RaftLog(term, 0, new HeartbeatLog()));
					lastIndex = 0;
				}
				logger.info("{}-{} {} LastIndex={} Count={}", raft.getName(), raft.isLeader(),
						raft.getRaftConfig().getDbHome(), lastIndex, getTestStateMachineCount());

				// 崩溃自愈：endReceiveInstallSnapshot在logs.drop()之后、边界日志saveLog()之前崩溃，
				// 重启后lastIndex < firstIndex（正常截断只到firstIndex为止，该序只可能由此崩溃窗口产生），
				// 复制回退到firstIndex时readLog为null而NPE、复制楔死。重置回无快照状态，由leader重新同步。
				if (lastIndex < firstIndex) {
					logger.warn("{} crash recovery: lastIndex({}) < firstIndex({}), reset to no-snapshot state."
							+ " endReceiveInstallSnapshot crashed between logs.drop() and saveLog?",
							raft.getName(), lastIndex, firstIndex);
					saveFirstIndex(-1);
					firstIndex = -1;
				}

				// snapshot以后FirstIndex会推进，不再从-1开始。
				if (firstIndex == -1) { // no committed snapshot
					try (var itFirst = logs.iterator()) {
						itFirst.seekToFirst();
						if (itFirst.isValid()) {
							firstIndex = RaftLog.decode(new Binary(itFirst.value()),
									raft.getStateMachine()::logFactory).getIndex();
						}
					}
				}
				// legacy迁移引导：DbHome存在固定名snapshot.dat时迁入代际世界（触发只看
				// 文件存在、崩溃重入安全；须在清扫之前）。S>F先推进指针再改名；S<F防御
				// 删除；无manifest按N=F；REPLACE覆盖回滚后重升级的残留gen。
				if (firstIndex >= 0) {
					var legacyPath = Paths.get(getSnapshotFullName());
					if (Files.exists(legacyPath)) {
						var manifestIndex = readSnapshotManifest(legacyPath);
						if (manifestIndex != null && manifestIndex > firstIndex) {
							logger.warn("{} legacy migrate: snapshot manifest index({}) > firstIndex({}),"
											+ " advance firstIndex to skip replay of applied logs.",
									raft.getName(), manifestIndex, firstIndex);
							saveFirstIndex(manifestIndex);
							firstIndex = manifestIndex;
							// 附带校正lastSnapshotIndex滞后（否则trySnapshot提前触发一次新快照，无害）。
							lastSnapshotIndex = manifestIndex;
							var lsiValue = ByteBuffer.Allocate(9);
							lsiValue.WriteLong(lastSnapshotIndex);
							rafts.put(writeOptions, lastSnapshotIndexKey, 0, lastSnapshotIndexKey.length,
									lsiValue.Bytes, 0, lsiValue.WriteIndex);
						} else if (manifestIndex != null && manifestIndex < firstIndex) {
							logger.warn("{} legacy migrate: snapshot manifest index({}) < firstIndex({}),"
									+ " discard snapshot, reinstall from leader.",
									raft.getName(), manifestIndex, firstIndex);
							try {
								Files.deleteIfExists(legacyPath);
							} catch (IOException e) {
								logger.warn("discard stale snapshot error.", e);
							}
						}
						if (Files.exists(legacyPath)) {
							try {
								Files.move(legacyPath, genSnapshotPath(firstIndex),
										StandardCopyOption.REPLACE_EXISTING);
							} catch (IOException e) {
								// 迁移失败不得带病运行（磁盘布局不确定）：显性失败。
								throw Task.forceThrow(e);
							}
						}
					}
				}
				lastApplied = firstIndex;
				commitIndex = firstIndex;
			}

			// 边界日志缺失检测（lastIndex >= firstIndex方向的空洞）：完整路径在边界日志saveLog(X)
			// 之后、saveFirstIndex之前崩溃，重启后logs={X}在(F,X)区间留洞——apply从F+1读null永久
			// 楔死，且对leader的prevLog=X校验成功，不再触发InstallSnapshot。此时logs只能是半安装
			// 残留（正常截断永远保留firstIndex处边界日志）：整体丢弃，重置回无快照状态由leader重建。
			// 破坏性重置放在迭代器关闭之后（列族句柄销毁前必须先关闭其上的迭代器）。
			if (firstIndex >= 0 && readLog(firstIndex) == null) {
				logger.warn("{} crash recovery: no boundary log at firstIndex={}, discard half-installed logs({})."
								+ " endReceiveInstallSnapshot crashed after boundary saveLog?",
						raft.getName(), firstIndex, lastIndex);
				try {
					logs.drop(); // 声明throws Exception（实际只抛RocksDBException），收窄到构造器的异常签名
				} catch (RocksDBException e) {
					throw e;
				} catch (Exception e) {
					throw Task.forceThrow(e);
				}
				logs = this.database.getOrAddTable(raft.getName() + ".logs"); // this.database：构造器参数database遮蔽字段
				saveLog(new RaftLog(term, 0, new HeartbeatLog()));
				lastIndex = 0;
				saveFirstIndex(-1);
				firstIndex = 0;
				lastApplied = 0;
				commitIndex = 0;
			}
		}
		logsAvailable = true;

		// 启动清扫（在legacy迁移之后）：.installing./tmp族/.commit.delayed残留、
		// index != firstIndex的gen孤儿。删除失败仅告警（活文件不被覆写，残留不损正确性）。
		var dbHomeFiles = listDbHomeFiles();
		if (dbHomeFiles != null) {
			for (var file : dbHomeFiles) {
				if (!file.isFile() || !file.getName().startsWith(snapshotFileName + "."))
					continue;
				var fileName = file.getName();
				var delete = fileName.startsWith(snapshotFileName + ".installing.")
						|| fileName.endsWith(".tmp")
						|| fileName.endsWith(".commit.delayed");
				if (!delete) {
					var index = parseGenSnapshotIndex(file);
					delete = index != null && index != firstIndex;
				}
				if (delete) {
					try {
						Files.deleteIfExists(file.toPath());
					} catch (IOException e) {
						logger.warn("clean residual snapshot file Exception. file={}", file, e);
					}
				}
			}
		}

		// 可能有没有被清除的日志存在。启动任务。
		startRemoveLogOnlyBefore(firstIndex);
	}

	private void trySetNodeReady() throws RocksDBException {
		if (nodeReady)
			return;

		nodeReady = true;

		var value = ByteBuffer.Allocate(1);
		value.WriteBool(true);
		rafts.put(writeOptions, raftsNodeReadyKey, 0, raftsNodeReadyKey.length, value.Bytes, 0, value.WriteIndex);
	}

	/**
	 * 查询请求的状态。
	 * 1. return null 表示RaftExpired（createTime过老或过新，see isUniqueRequestCreateTimeValid），
	 *    这个错误不可忽略。
	 * 2. return state.NOT_FOUND 第一次收到请求，是合理状态的一种，外面正常处理。
	 * 3. return state 重复的请求，后面根据状态进行处理。分为RaftApplied，DuplicateRequest两种。
	 *
	 * @param p request
	 * @return state
	 * @throws RocksDBException RocksDBException
	 */
	UniqueRequestState tryGetRequestState(Protocol<?> p) throws RocksDBException {
		var raftRpc = (IRaftRpc)p;

		var create = raftRpc.getCreateTime();
		var now = System.currentTimeMillis();
		if (!isUniqueRequestCreateTimeValid(create, now, raft.getRaftConfig().getUniqueRequestExpiredDays()))
			return null;

		UniqueRequestState state = openUniqueRequests(raftRpc.getCreateTime()).getRequestState(raftRpc);
		return state != null ? state : UniqueRequestState.NOT_FOUND;
	}

	/**
	 * 唯一请求createTime合法区间判定：过老（超过expiredDays天）或过新（超过1天未来）
	 * 返回false，调用方按RaftExpired拒绝。createTime由发送方控制，时钟故障或恶意直连
	 * 可用未来日期建存根列族且其过期判定永不满足，无界增长；1天上界容忍合理时钟偏差，
	 * 未来表随时间自然过期。拒绝发生在handle之前，不产生携带未来createTime的日志。
	 */
	static boolean isUniqueRequestCreateTimeValid(long create, long now, int expiredDays) {
		if ((now - create) / 86400_000 >= expiredDays)
			return false;
		return create <= now + 86400_000L;
	}

	private UniqueRequestSet openUniqueRequests(long time) {
		return uniqueRequestSets.computeIfAbsent(toUniqueRequestKey(new Date(time)),
				k -> new UniqueRequestSet(makeUniqueRequestTableName(raft.getName(), k)));
	}

	// 唯一请求存根按天建表，表名：<raftName>.unique.<yyyy>.<M>.<d>，key由toUniqueRequestKey生成。
	static String makeUniqueRequestTableName(String raftName, long key) {
		return raftName + ".unique." + (key >> 16) + '.' + ((key >> 8) & 0xff) + '.' + (key & 0xff);
	}

	// 解析唯一请求存根表名中的日期（当天0点）；不是本raft的unique表（前缀不匹配或日期非法）返回null。
	static Date parseUniqueRequestDate(String raftName, String tableName) {
		var prefix = raftName + ".unique.";
		if (!tableName.startsWith(prefix))
			return null;
		var dateText = tableName.substring(prefix.length());
		var format = new SimpleDateFormat("yyyy.M.d");
		format.setLenient(false);
		try {
			var date = format.parse(dateText);
			// parse允许尾随垃圾，这里要求全量匹配，防止误认其他raft的表或非法日期（如2026.2.30）。
			return format.format(date).equals(dateText) ? date : null;
		} catch (ParseException e) {
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	static long toUniqueRequestKey(Date date) {
		return ((date.getYear() + 1900L) << 16) + ((date.getMonth() + 1) << 8) + date.getDate();
	}

	public WriteOptions getWriteOptions() {
		return writeOptions;
	}

	public void setWriteOptions(WriteOptions value) {
		writeOptions = value;
	}

	// package-private：headless单测直接落日志后驱动应用循环。
	void saveLog(RaftLog log) throws RocksDBException {
		var key = ByteBuffer.Allocate(9);
		key.WriteLong(log.getIndex());
		var value = log.encode();
		logs.put(writeOptions, key.Bytes, 0, key.WriteIndex, value.Bytes, 0, value.WriteIndex);

		if (isDebugEnabled)
			logger.debug("{}-{} RequestId={} Index={} Count={}", raft.getName(), raft.isLeader(),
					log.getLog().getUnique().getRequestId(), log.getIndex(), getTestStateMachineCount());
	}

	private void saveLogRaw(long index, Binary rawValue) throws RocksDBException {
		var key = ByteBuffer.Allocate(9);
		key.WriteLong(index);
		logs.put(writeOptions, key.Bytes, 0, key.WriteIndex,
				rawValue.bytesUnsafe(), rawValue.getOffset(), rawValue.size());

		if (isDebugEnabled)
			logger.debug("{}-{} RequestId=? Index={} Count={}",
					raft.getName(), raft.isLeader(), index, getTestStateMachineCount());
	}

	private byte[] readLogBytes(long index) throws RocksDBException {
		var key = ByteBuffer.Allocate(9);
		key.WriteLong(index);
		var logs = this.logs;
		return logs != null ? logs.get(RocksDatabase.getDefaultReadOptions(), key.Bytes, 0, key.WriteIndex) : null;
	}

	RaftLog readLog(long index) throws RocksDBException { // package-private：SendSnapshotting读边界日志
		var value = readLogBytes(index);
		return value != null ? RaftLog.decode(new Binary(value), raft.getStateMachine()::logFactory) : null;
	}

	/**
	 * 读取并解码一条待应用的日志。decode失败是永久性错误（本节点永远无法应用）：静默吞掉
	 * 会让lastApplied停滞而复制应答一切正常，"健康"的落后者仍计入多数派、日志完整还可当选，
	 * 当选后集群写入全卡在此——宁死不糊，fatalKill。RocksDBException（瞬时IO错误）向上传播
	 * 保持重试语义；apply阶段异常同样重试（结构性错误由状态机内部自行fatalKill）。
	 */
	private RaftLog readLogForApply(long index, String where) throws RocksDBException {
		try {
			return readLog(index);
		} catch (RocksDBException e) {
			throw e; // 瞬时读失败：lastApplied不推进，下次触发重试
		} catch (Throwable e) {
			fatalKillDecodeError(where, index, e);
			return null; // fatalKill 不会返回；这里防御编译检查。
		}
	}

	private void fatalKillDecodeError(String where, long index, Throwable e) {
		logger.fatal("{} {} decode log({}) failed, can never apply. lastApplied={} commitIndex={}",
				raft.getName(), where, index, lastApplied, commitIndex, e);
		raft.fatalKill();
	}

	public enum SetTermResult {
		Newer,
		Same,
		Older
	}

	// Rules for Servers
	// All Servers:
	// If RPC request or response contains term T > currentTerm:
	// set currentTerm = T, convert to follower(§5.1)

	/**
	 * term合理上界。超大term（如Long.MAX_VALUE）被采纳并持久化后term+1溢出回绕为负，
	 * 选举永久冻结且重启不可恢复，还会随投票/心跳传染。超过上界按陈旧拒绝采纳。
	 * 合法集群term量级极小，上界不可自然触及；/1024即允许1024次坏term攻击，修复后继续开放下一段。
	 */
	public static final long TERM_MAX = Long.MAX_VALUE / 1024;

	private volatile long lastRejectedOversizedTerm; // 同值只记一次日志，防恶意洪泛刷日志

	public SetTermResult trySetTerm(long term) throws RocksDBException {
		if (term > TERM_MAX) {
			if (term != lastRejectedOversizedTerm) {
				lastRejectedOversizedTerm = term;
				logger.error("trySetTerm reject oversized term={} (> TERM_MAX={}), current term={}."
								+ " possible attack or corrupted peer, treat as stale.",
						term, TERM_MAX, this.term);
			}
			return SetTermResult.Older;
		}
		if (term > this.term) {
			this.term = term;
			var termValue = ByteBuffer.Allocate(9);
			termValue.WriteLong(term);
			rafts.put(writeOptions, raftsTermKey, 0, raftsTermKey.length, termValue.Bytes, 0, termValue.WriteIndex);
			raft.setLeaderId("");
			setVoteFor("");
			lastLeaderCommitIndex = 0;
			return SetTermResult.Newer;
		}
		return term == this.term ? SetTermResult.Same : SetTermResult.Older;
	}

	public boolean canVoteFor(String voteFor) {
		String thisVoteFor = this.voteFor;
		return thisVoteFor == null || thisVoteFor.isEmpty() || thisVoteFor.equals(voteFor);
	}

	public void setVoteFor(String voteFor) throws RocksDBException {
		if (!this.voteFor.equals(voteFor)) {
			this.voteFor = voteFor;
			var voteForValue = ByteBuffer.Allocate(5 + voteFor.length());
			voteForValue.WriteString(voteFor);
			rafts.put(writeOptions, raftsVoteForKey, 0, raftsVoteForKey.length,
					voteForValue.Bytes, 0, voteForValue.WriteIndex);
		}
	}

	private void tryCommit(AppendEntries rpc, Server.ConnectorEx connector) throws Exception {
		connector.setNextIndex(rpc.Argument.getLastEntryIndex() + 1);
		connector.setMatchIndex(rpc.Argument.getLastEntryIndex());

		// 旧的AppendEntries结果，不用继续处理（非必要的小优化）。
		if (rpc.Argument.getLastEntryIndex() <= commitIndex) {
			// commitIndex未推进（无新写入）也要尝试apply：leader本地apply可能因flush失败中断，
			// 空闲leader的复制应答是仅剩的周期触发点，在此重试直到追平（对齐follower侧
			// commitIndex>lastApplied的重试分支；pendingFlush命中时只幂等重试flush）。
			if (commitIndex > lastApplied)
				tryStartApplyTask(readLogForApply(commitIndex, "tryCommit"));
			return;
		}

		// find MaxMajorityLogIndex
		// Rules for Servers
		// If there exists an N such that N > commitIndex, a majority
		// of matchIndex[i] ≥ N, and log[N].term == currentTerm:
		// set commitIndex = N(§5.3, §5.4).
		var followers = new ArrayList<Server.ConnectorEx>();
		raft.getServer().getConfig().forEachConnector(c -> followers.add((Server.ConnectorEx)c));
		followers.sort((a, b) -> Long.compare(b.getMatchIndex(), a.getMatchIndex()));
		var maxMajorityLogIndex = followers.get(raft.getRaftConfig().getHalfCount() - 1).getMatchIndex();
		if (maxMajorityLogIndex > commitIndex) {
			var maxMajorityLog = readLogForApply(maxMajorityLogIndex, "tryCommit");
			if (maxMajorityLog == null || maxMajorityLog.getTerm() != term) {
				// 如果是上一个 Term 未提交的日志在这一次形成的多数派，
				// 不自动提交。
				// 总是等待当前 Term 推进时，顺便提交它。
				return;
			}
			// 推进！
			commitIndex = maxMajorityLogIndex;
			trySetNodeReady();
			tryStartApplyTask(maxMajorityLog);
		}
	}

	// under lock (Raft)
	private void tryStartApplyTask(RaftLog lastApplicableLog) throws Exception {
		if (applyFuture == null && !raft.isShutdown) {
			// 仅在没有 apply 进行中才尝试进行处理。
			if (commitIndex - lastApplied < raft.getRaftConfig().getBackgroundApplyCount()) {
				// apply immediately in current thread
				tryApply(lastApplicableLog, Long.MAX_VALUE);
				return;
			}

			applyFuture = new TaskCompletionSource<>();
			Raft.executeImportantTask(() -> {
				try {
					applyFuture.setResult(TaskSpec.ofFunc(this::backgroundApply).name("BackgroundApply").call() == 0); // 如果有人等待。
				} finally {
					applyFuture = null; // 允许再次启动，不需要等待了。
				}
			});
		}
	}

	private long backgroundApply() throws Exception {
		while (!raft.isShutdown) {
			raft.lock();
			try {
				// ReadLog Again，CommitIndex Maybe Grow.
				var lastApplicableLog = readLogForApply(commitIndex, "backgroundApply");
				tryApply(lastApplicableLog, raft.getRaftConfig().getBackgroundApplyCount());
				if (lastApplicableLog != null && lastApplied == lastApplicableLog.getIndex())
					return 0; // 本次Apply结束。
			} finally {
				raft.unlock();
			}
			Thread.yield();
		}
		return Procedure.CancelException;
	}

	// 测试钩子（一次性）：非null时在unique存根写之前执行并自动置null，注入存根写失败，
	// 验证apply成功后存根写失败的重试不重放增量。仅测试使用。
	Action0 testHookBeforeUniqueApply;

	// package-private：headless单测直接驱动应用循环，理由同saveLog。
	void tryApply(RaftLog lastApplicableLog, long count) throws Exception {
		if (lastApplicableLog == null) {
			logger.error("lastApplicableLog is null.");
			return;
		}
		for (long index = lastApplied + 1; index <= lastApplicableLog.getIndex() && count > 0; --count) {
			RaftLog raftLog = leaderAppendLogs.remove(index);
			if (raftLog == null)
				raftLog = readLogForApply(index, "tryApply");
			if (raftLog == null) {
				logger.warn("What Happened! index={} lastApplicableLog={} LastApplied={}",
						index, lastApplicableLog.getIndex(), lastApplied);
				// trySnapshot(); // 错误的时候不做这个尝试了。
				return; // end?
			}

			index = raftLog.getIndex() + 1;
			try {
				raftLog.getLog().apply(raftLog, raft.getStateMachine());
			} catch (Rocks.FlushException e) {
				// flush失败：状态机已记录pendingFlush，lastApplied不推进，重试只重试flush、
				// 不重放增量，避免双重应用。原始raftLog放回leaderAppendLogs：重试命中它时走
				// flush-only路径，成功后invokeCallback唤醒等待appendLog的业务线程。
				if (raftLog.isLeaderRequest() && leaderAppendLogs.putIfAbsent(raftLog.getIndex(), raftLog) != null) {
					logger.fatal("LeaderAppendLogs.TryAdd Fail. Index={}", raftLog.getIndex(), new Exception());
					raft.fatalKill();
				}
				throw e;
			}
			var hasUniqueRequest = raftLog.getLog().getUnique().getRequestId() > 0;
			// Rocks状态机才有pendingFlush补偿（Dbh2等自定义StateMachine的apply重试语义自成一体）。
			var smRocks = raft.getStateMachine() instanceof Rocks rocks ? rocks : null;
			if (hasUniqueRequest && smRocks != null)
				// apply已完整成功，其后到lastApplied推进之间的收尾步骤（unique存根写）失败时，
				// 登记"已应用"补偿：重试经takePendingFlush命中→no-op flush短路，不重放非幂等
				// 增量（list按索引追加等重放一次即双重应用）。正常收尾后在lastApplied推进处清除。
				smRocks.markApplied(raftLog.getIndex(), raftLog.getTerm());
			try {
				if (hasUniqueRequest) {
					var hook = testHookBeforeUniqueApply;
					if (hook != null) {
						testHookBeforeUniqueApply = null; // 一次性
						hook.run(); // 测试注入存根写失败（RocksDBException）
					}
					openUniqueRequests(raftLog.getLog().getCreateTime()).apply(raftLog);
				}
			} catch (RocksDBException e) {
				// 重试的pending路径会take消费标记：补回，保持"已应用"事实直到存根写成功；
				// raftLog放回理由同上flush失败分支（否则重试用解码的新对象，回调丢失，
				// 业务线程等满超时拿到假失败）。
				if (smRocks != null)
					smRocks.markApplied(raftLog.getIndex(), raftLog.getTerm());
				if (raftLog.isLeaderRequest() && leaderAppendLogs.putIfAbsent(raftLog.getIndex(), raftLog) != null) {
					logger.fatal("LeaderAppendLogs.TryAdd Fail. Index={}", raftLog.getIndex(), new Exception());
					raft.fatalKill();
				}
				throw e;
			}
			lastApplied = raftLog.getIndex(); // 循环可能退出，在这里修改。
			if (hasUniqueRequest && smRocks != null)
				smRocks.clearAppliedMark(raftLog.getIndex());
			//*
			if (isDebugEnabled && lastIndex - lastApplied < 10) {
				logger.debug("{}-{} {} RequestId={} LastIndex={} LastApplied={} Count={}",
						raft.getName(), raft.isLeader(), raft.getRaftConfig().getDbHome(),
						raftLog.getLog().getUnique().getRequestId(), lastIndex, lastApplied,
						getTestStateMachineCount());
			}
			// */
			raftLog.invokeCallback();
		}
		// if (isDebugEnabled)
		// logger.debug($"{Raft.Name}-{Raft.IsLeader} CommitIndex={CommitIndex} RequestId={lastApplicableLog.Log.Unique.RequestId} LastIndex={LastIndex} LastApplied={LastApplied} Count={GetTestStateMachineCount()}");
		trySnapshot();
	}

	private void trySnapshot() throws RocksDBException {
		var snapshotLogCount = raft.getRaftConfig().getSnapshotLogCount();
		if (snapshotLogCount > 0) {
			if (lastApplied - lastSnapshotIndex > snapshotLogCount) {
				lastSnapshotIndex = lastApplied;
				var bb = ByteBuffer.Allocate();
				bb.WriteLong(lastSnapshotIndex);
				rafts.put(writeOptions,
						lastSnapshotIndexKey, 0, lastSnapshotIndexKey.length,
						bb.Bytes, bb.ReadIndex, bb.size());
				TaskSpec.ofAction(this::snapshot).name("Snapshot").run();
			}
		}
		// else disable
	}

	public long getTestStateMachineCount() {
		// 状态机自己提供的观测计数（主源码不instanceof测试壳类；默认-1表示无）。
		return raft.getStateMachine().getDebugCount();
	}

	public void sendHeartbeatTo(Server.ConnectorEx connector) {
		raft.lock();
		try {
			var now = System.currentTimeMillis();
			connector.setHeartbeatTime(now);
			//connector.setAppendLogActiveTime(now);

			if (!raft.isLeader())
				return; // skip if is not a leader

			if (connector.getPending() != null)
				return;

			if (sendSnapshotting.contains(connector.getName()))
				return;

			var socket = connector.TryGetReadySocket();
			if (socket == null)
				return; // Heartbeat Will Retry

			RaftLog last;
			try {
				last = lastRaftLogTermIndex();
			} catch (RocksDBException e) {
				logger.warn("sendHeartbeatTo lastRaftLogTermIndex", e); // 下一轮心跳重试
				return;
			}
			var heartbeat = new AppendEntries();
			heartbeat.Argument.setTerm(term);
			heartbeat.Argument.setLeaderId(raft.getName());
			// 心跳携带 prevLog 和 leaderCommit（仍是空entries）：
			// 1. follower 据此推进自己的 commitIndex，否则空闲期只能等下一次写入捎带；
			// 2. follower 的 nodeReady 需要观察到 leaderCommit 推进，空闲期一直无法 ready
			//    会导致它之后无法参与投票（ready与不ready互相拒投，可能选不出Leader）；
			// 3. prevLog 不匹配说明 follower 落后或分叉，leader 在应答里主动驱动复制。
			heartbeat.Argument.setPrevLogIndex(last.getIndex());
			heartbeat.Argument.setPrevLogTerm(last.getTerm());
			heartbeat.Argument.setLeaderCommit(commitIndex);
			heartbeat.Send(socket, (p) -> {
				if (heartbeat.isTimeout())
					return 0; // skip

				raft.lock();
				try {
					if (raft.getLogSequence().trySetTerm(heartbeat.Result.getTerm()) == SetTermResult.Newer) {
						// new term found.
						raft.convertStateTo(Raft.RaftState.Follower);
						return Procedure.Success;
					}

					if (!raft.isLeader())
						return 0;

					// pending != null 说明复制正在进行，心跳的失败信息是发送时的旧现场，
					// 此时不动 nextIndex，避免干扰进行中的复制。
					if (!heartbeat.Result.getSuccess() && connector.getPending() == null) {
						// follower 日志与 leader 尾部不匹配（落后或分叉），调整 nextIndex 并驱动复制。
						// 与 processAppendEntriesResult 的失败分支类似，但心跳的 prevLogIndex==lastIndex，
						// 不使用那个分支里的 Impossible 断言。
						var ni = heartbeat.Result.getNextIndex();
						if (ni == 0)
							ni = connector.getNextIndex() - 1; // 分叉：回退一格
						if (ni > lastIndex)
							ni = lastIndex;
						if (ni < firstIndex)
							ni = firstIndex;
						connector.setNextIndex(ni);
						trySendAppendEntries(connector, null);
					}
				} finally {
					raft.unlock();
				}
				return 0;
			}, raft.getRaftConfig().getAppendEntriesTimeout());
		} finally {
			raft.unlock();
		}
	}

	public static final class AppendLogResult {
		public long term;
		public long index;
	}

	/** 超时日志条目的命运：已应用/已截断删除/未决（等待超时或读失败）。 */
	enum LogFate {
		Applied,
		Removed,
		Undetermined,
	}

	public AppendLogResult appendLog(Log log) throws Exception {
		var future = new TaskCompletionSource<RaftLog>();
		var result = appendLog(log, (raftLog, success) -> {
			if (success)
				future.setResult(raftLog);
			else
				future.cancel(false);
		});
		if (!future.await(raft.getRaftConfig().getAppendEntriesTimeout() * 2L + 1000)) {
			leaderAppendLogs.remove(result.index);
			// 超时/取消后条目仍留在日志中（lastIndex不回退），命运未定：可能稍后被确认应用，
			// 也可能被新leader截断。调用方将按失败返回并释放悲观锁；若在条目应用前放锁，
			// 后续同key事务基于旧值提交新日志、本条目随后又被应用，造成丢失更新。
			// 先等待命运确定再抛重试异常，使调用方在窗口期继续持锁。
			// 【FND11 raft-01】命运区分：已应用=提交实际成功（分区愈合/慢follower补齐确认恰好
			// 落在超时点之后）——按成功返回，让_final_commit_继续执行提交动作。原先无条件抛
			// RaftRetry：已提交事务的commit actions被跳过、rollback actions被补跑（通知类提交
			// 动作不可重放、静默丢失）；仅截断/删除/未决才值得重试。
			if (waitLogFateDetermined(result.index) == LogFate.Applied)
				return result;
			throw new RaftRetryException("timeout or canceled");
		}
		return result;
	}

	/**
	 * 等待index日志条目命运确定：已应用（lastApplied>=index）或已从日志删除（截断/丢弃）。
	 * 用于appendLog超时路径，保证调用方在条目未决期间不释放悲观锁（否则丢失更新）。
	 * 必须在Raft锁外调用：内部只在检查时短暂持锁。waitMs超时后放弃：集群长期选不出
	 * leader时条目命运无法确定，继续持锁会无限期挂住业务线程。
	 */
	LogFate waitLogFateDetermined(long index) {
		return waitLogFateDetermined(index, raft.getRaftConfig().getAppendEntriesTimeout() * 2L + 1000);
	}

	// package-private with explicit timeout for tests.
	LogFate waitLogFateDetermined(long index, long waitMs) {
		var deadline = System.nanoTime() + waitMs * 1_000_000L;
		while (!raft.isShutdown) {
			raft.lock();
			try {
				// lastApplied 不是volatile，在Raft锁内读取保证可见性。
				if (lastApplied >= index)
					return LogFate.Applied; // applied
				if (readLog(index) == null)
					return LogFate.Removed; // truncated or discarded
			} catch (RocksDBException e) {
				return LogFate.Undetermined; // 读取失败（如db已关闭），不再等待
			} finally {
				raft.unlock();
			}
			try {
				//noinspection BusyWait
				Thread.sleep(20);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return LogFate.Undetermined;
			}
			if (System.nanoTime() - deadline >= 0) {
				logger.error("{} waitLogFateDetermined({}) timeout after {}ms, release pending entry. " +
						"pessimism locks will be released while the log entry is still pending.",
						raft.getName(), index, waitMs);
				return LogFate.Undetermined;
			}
		}
		return LogFate.Undetermined;
	}

	public AppendLogResult appendLog(Log log, Action2<RaftLog, Boolean> callback) throws Exception {
		raft.lock();
		try {
			if (!raft.isLeader())
				throw new RaftRetryException("not leader"); // 快速失败

			var raftLog = new RaftLog(term, lastIndex + 1, log);
			if (raftLog.getLog().getUnique().getRequestId() > 0)
				openUniqueRequests(raftLog.getLog().getCreateTime()).save(raftLog);
			saveLog(raftLog);

			// 容易出错的放到前面。
			if (null != callback) {
				raftLog.setLeaderCallback(callback);
				if (leaderAppendLogs.putIfAbsent(raftLog.getIndex(), raftLog) != null) {
					logger.fatal("LeaderAppendLogs.TryAdd Fail. Index={}", raftLog.getIndex(), new Exception());
					raft.fatalKill();
				}
			}
			// 最后修改LastIndex。
			lastIndex = raftLog.getIndex();
			// 广播给followers并异步等待多数确认。
			// 广播中途异常不回滚lastIndex：可能已有follower持久化该entry，回滚复用同一
			// (term,index)写不同内容会破坏日志匹配不变式（冲突检查只比term），状态机静默
			// 分叉。entry留着无害：要么之后提交，要么换主后被截断。
			raft.getServer().getConfig().ForEachConnector(c -> trySendAppendEntries((Server.ConnectorEx)c, null));
			var result = new AppendLogResult();
			result.term = term;
			result.index = lastIndex;
			return result;
		} finally {
			raft.unlock();
		}
	}

	boolean getSnapshotting() { // package-private：Raft.processInstallSnapshot 的首块早期拒绝与测试使用
		return snapshotting;
	}

	void setSnapshotting(boolean value) { // package-private：测试直接合成"本地快照进行中"
		snapshotting = value;
	}

	public SendSnapshotting getSendSnapshotting() {
		return sendSnapshotting;
	}

	public String getSnapshotFullName() {
		return Paths.get(raft.getRaftConfig().getDbHome(), snapshotFileName).toString();
	}

	// 代际快照文件：写入后不可变，只整体删除。
	public Path genSnapshotPath(long lastIncludedIndex) {
		return Paths.get(raft.getRaftConfig().getDbHome(), snapshotFileName + "." + lastIncludedIndex);
	}

	// 已提交快照=firstIndex指认的gen文件；无快照时返回legacy名（不存在）。
	// 读者统一经此解析不可变文件，与后续提交/清扫不竞争。
	public String getCommittedSnapshotFile() {
		return firstIndex >= 0 ? genSnapshotPath(firstIndex).toString() : getSnapshotFullName();
	}

	long endReceiveInstallSnapshot(ReceiveSnapshotting.Entry entry, InstallSnapshot r) throws Exception {
		logsAvailable = false; // cancel RemoveLogBefore
		var removeLogBeforeFuture = this.removeLogBeforeFuture;
		if (removeLogBeforeFuture != null)
			removeLogBeforeFuture.await();
		raft.lock();
		try {
			try {
				// 上面的await与本处raft.lock()都可能长时间等待，期间term可任意变化（写点全在Raft锁内）。
				// 拿到锁后与进入时不一致即为旧term准备的重置，必须放弃：否则误触发下方fatalKill防御、
				// logs.drop()丢弃新leader已复制的日志、抹掉等待期间的自投票。应答带回当前term，
				// 旧leader自行退位/回溯重试。
				if (r.Argument.getTerm() != term) {
					logger.warn("{} InstallSnapshot stale: rpcTerm={} != currentTerm={}, leaderId={},"
									+ " LastIncludedIndex={}, term changed while waiting outside raft lock;"
									+ " discard received snapshot.",
							raft.getName(), r.Argument.getTerm(), term, r.Argument.getLeaderId(),
							r.Argument.getLastIncludedIndex());
					// 对齐另外两个放弃分支：冻结文件一并清理（句柄已关；删除失败仅告警）。
					// 唯一命名保证新安装用新路径，删本条目文件不波及任何在飞接收。
					ReceiveSnapshotting.tryDelete(entry.path, "endReceiveInstallSnapshot");
					r.Result.setTerm(term);
					return 0; // leader 发现 term 更高自行退位/回溯重试（见 InstallSnapshotState.processResult）
				}
				// 本地快照进行中（重阶段在锁外写backupDir）：重置会与快照并发操作同一backupDir，
				// 且loadSnapshot失败留下"日志已重置、状态机未恢复"的不可自愈状态。应答冲突码，
				// leader下个心跳自动重试。检查与重置全程持raft锁串行；processInstallSnapshot
				// 首块处的早期拒绝只是优化，这里是正确性兜底。
				if (getSnapshotting()) {
					logger.warn("{} InstallSnapshot LastIncludedIndex={} conflicts with local snapshotting;"
									+ " discard received snapshot and reply SnapshottingConflict.",
							raft.getName(), r.Argument.getLastIncludedIndex());
					// 条目由外层finally摘除，这里只清文件（失败仅告警，启动清扫兜底）。
					ReceiveSnapshotting.tryDelete(entry.path, "endReceiveInstallSnapshot");
					return InstallSnapshot.ResultCodeSnapshottingConflict;
				}
				// 所有权复核：条目仍在登记表中且已finalizing。不满足即所有权被撤销或不变量破坏：
				// 丢弃文件应答冲突码，leader中断、下个心跳重装，不做任何重置。
				if (raft.receiveSnapshotting.get(r.Argument.getLastIncludedIndex()) != entry
						|| !entry.finalizing) {
					logger.warn("{} InstallSnapshot finalize ownership broken: LastIncludedIndex={},"
									+ " discard file and reply FinalizingConflict.",
							raft.getName(), r.Argument.getLastIncludedIndex());
					ReceiveSnapshotting.tryDelete(entry.path, "endReceiveInstallSnapshot");
					return InstallSnapshot.ResultCodeFinalizingConflict;
				}
				// 尺寸复核：文件长度必须等于done时记录的应收总长度。唯一命名+finalizing占位
				// 已让重装截断结构上无对象，此处拦理论外路径（磁盘异常/外部改动）：尺寸不符的
				// 文件一旦提交，loadSnapshot失败fatalKill、重启加载损坏快照起不来。
				long fileSize;
				try {
					fileSize = Files.size(entry.path);
				} catch (IOException e) {
					fileSize = -1;
				}
				if (fileSize != entry.expectedLength) {
					logger.warn("{} InstallSnapshot finalize size mismatch: fileSize={} expectedLength={}"
									+ " LastIncludedIndex={}, discard file and reply FinalizingConflict.",
							raft.getName(), fileSize, entry.expectedLength, r.Argument.getLastIncludedIndex());
					ReceiveSnapshotting.tryDelete(entry.path, "endReceiveInstallSnapshot");
					return InstallSnapshot.ResultCodeFinalizingConflict;
				}
				// 6. If existing log entry has same index and term as snapshot's
				// last included entry, retain log entries following it and reply
				var last = readLog(r.Argument.getLastIncludedIndex());
				if (last != null && last.getTerm() == r.Argument.getLastIncludedTerm()) {
					logger.warn("Exist Local Log. Do It Like A Local Snapshot!");
					// 防御：边界低于commitIndex时复位会回退已apply的数据，正常不会发生
					// （完好日志会被AppendEntries先行补齐），一旦发生说明别处有bug。
					if (r.Argument.getLastIncludedIndex() < commitIndex) {
						logger.fatal("{} InstallSnapshot(ExistLog) LastIncludedIndex={} < commitIndex={},"
										+ " there must be a bug.",
								raft.getName(), r.Argument.getLastIncludedIndex(), commitIndex, new Exception());
						raft.fatalKill();
					}
					commitSnapshotNow(entry.path, r.Argument.getLastIncludedIndex());
					// 恢复语义补全：本分支由"上次同边界收尾中途失败后的重装"进入——收尾在
					// commitSnapshotNow处崩溃后，logs只含边界日志X而内存索引与状态机停在旧边界。
					// 只执行commitSnapshotNow会留下firstIndex=X但lastApplied=旧值的空洞，apply永久
					// 楔死、集群级死锁。对齐完整路径：复位内存索引并装载快照（重装即恢复）。
					lastIndex = r.Argument.getLastIncludedIndex();
					commitIndex = firstIndex; // commitSnapshotNow已把firstIndex推进为X
					lastApplied = firstIndex;
					setVoteFor(raft.getLeaderId()); // 放弃当前Term的投票（对齐完整路径）
					long t = System.nanoTime();
					try {
						raft.getStateMachine().loadSnapshot(getCommittedSnapshotFile());
					} catch (Throwable e) {
						// 没有原地恢复路径：fatalKill把静默分歧变成crash，重启从已提交gen快照恢复自愈。
						logger.fatal("{} EndReceiveInstallSnapshot(ExistLog) loadSnapshot failed, fatalKill. Path={}",
								raft.getName(), entry.path, e);
						raft.fatalKill();
						throw Task.forceThrow(e); // fatalKill不会返回（halt）；测试注入钩子时到达这里
					}
					logger.info("{} EndReceiveInstallSnapshot(ExistLog) Path={} time={}ms",
							raft.getName(), entry.path, (System.nanoTime() - t) / 1_000_000);
					return 0;
				}
				// 防御：边界低于commitIndex时丢弃日志会回退已apply的数据，正常不会发生，
				// 一旦发生说明别处有bug。
				if (r.Argument.getLastIncludedIndex() < commitIndex) {
					logger.fatal("{} InstallSnapshot LastIncludedIndex={} < commitIndex={}, there must be a bug.",
							raft.getName(), r.Argument.getLastIncludedIndex(), commitIndex, new Exception());
					raft.fatalKill();
				}
				// 7. Discard the entire log：整个删除后下一次AppendEntries找不到prev，
				// 所以最后一个trunk带上LastIncludedLog，接收者清除log后插入这条边界日志。
				logger.info("endReceiveInstallSnapshot: close logs: {}", raft.getRaftConfig().getDbHome());
				//logs.close();
				//logs = null;
				cancelPendingAppendLogFutures();
				//var logsDir = Paths.get(raft.getRaftConfig().getDbHome(), "logs").toString();
				//deletedDirectoryAndCheck(new File(logsDir), 10000);
				logs.drop();
				logs = database.getOrAddTable(raft.getName() + ".logs");
				var lastIncludedLog = RaftLog.decode(r.Argument.getLastIncludedLog(),
						raft.getStateMachine()::logFactory);
				saveLog(lastIncludedLog);
				commitSnapshotNow(entry.path, lastIncludedLog.getIndex());

				lastIndex = lastIncludedLog.getIndex();
				commitIndex = firstIndex;
				lastApplied = firstIndex;

				// 【关键】记录这个，放弃当前Term的投票。
				setVoteFor(raft.getLeaderId());

				// 8. Reset state machine using snapshot contents (and load
				// snapshot's cluster configuration)
				long t = System.nanoTime();
				try {
					raft.getStateMachine().loadSnapshot(getCommittedSnapshotFile());
				} catch (Throwable e) {
					// loadSnapshot失败时日志已drop、firstIndex已推进而状态机仍旧内容；继续运行则
					// 重试走ExistLog分支不再loadSnapshot，永久脏状态。无原地恢复路径：fatalKill
					// 变静默分歧为crash，重启从已提交gen快照恢复自愈。
					logger.fatal("{} EndReceiveInstallSnapshot loadSnapshot failed, fatalKill. Path={}",
							raft.getName(), entry.path, e);
					raft.fatalKill();
					throw Task.forceThrow(e); // fatalKill 不会返回（halt）；测试注入钩子时到达这里
				}
				logger.info("{} EndReceiveInstallSnapshot Path={} time={}ms",
						raft.getName(), entry.path, (System.nanoTime() - t) / 1_000_000);
				return 0;
			} finally {
				logsAvailable = true;
			}
		} finally {
			// finalizing条目生命周期唯一收口：同一性摘除（不误摘同key接任的新条目），
			// 持raft锁执行；此后重装首块才能走"无条目→全新安装"路径。
			raft.receiveSnapshotting.removeIdentity(r.Argument.getLastIncludedIndex(), entry);
			raft.unlock();
		}
	}

	public void snapshot() throws Exception {
		raft.lock();
		try {
			// 正在接收InstallSnapshot时不启动本地snapshot：
			// 接收完成会重置状态机并推进firstIndex，进行中的本地snapshot将作废
			// （其commit由_commitSnapshot的过期检查兜底丢弃）。
			if (getSnapshotting() || !sendSnapshotting.isEmpty() || raft.isReceivingSnapshot())
				return;

			setSnapshotting(true);
		} finally {
			raft.unlock();
		}
		try {
			// 忽略Snapshot返回结果。肯定是重复调用导致的。
			// out 结果这里没有使用，定义在参数里面用来表示这个很重要。
			var path = getSnapshotFullName() + ".tmp";
			var result = raft.getStateMachine().snapshot(path);
			logger.info("{} Snapshot Path={} LastIndex={} LastTerm={} time={}ms({}+{}+{})",
					raft.getName(), path, result.lastIncludedIndex, result.lastIncludedTerm,
					result.totalNanoTime / 1_000_000,
					result.checkPointNanoTime / 1_000_000,
					result.backupNanoTime / 1_000_000,
					result.zipNanoTime / 1_000_000);
		} finally {
			raft.lock();
			try {
				setSnapshotting(false);
			} finally {
				raft.unlock();
			}
		}
	}

	@SuppressWarnings("SameReturnValue")
	private long processAppendEntriesResult(Server.ConnectorEx connector, Protocol<?> p) throws Exception {
		// 这个rpc处理流程总是返回 Success，需要统计观察不同的分支的发生情况，再来定义不同的返回值。
		var r = (AppendEntries)p;
		raft.lock();
		try {
			if (r.isTimeout() && raft.isLeader()) {
				trySendAppendEntries(connector, r); // timeout and resend
				return Procedure.Success;
			}

			if (raft.getLogSequence().trySetTerm(r.Result.getTerm()) == SetTermResult.Newer) {
				// new term found.
				raft.convertStateTo(Raft.RaftState.Follower);
				// 发现新的 Term，已经不是Leader，不能继续处理了。
				// 直接返回。
				connector.setPending(null);
				return Procedure.Success;
			}

			if (!raft.isLeader()) {
				connector.setPending(null);
				return Procedure.Success;
			}

			if (r.Result.getSuccess()) {
				tryCommit(r, connector);
				// TryCommit 推进了NextIndex，
				// 可能日志没有复制完或者有新的AppendLog。
				// 尝试继续复制日志。
				// see TrySendAppendEntries 内的
				// "限制一次发送的日志数量”
				trySendAppendEntries(connector, r);
				return Procedure.Success;
			}

			// 日志同步失败，调整NextIndex，再次尝试。
			if (r.Result.getNextIndex() == 0)
				connector.setNextIndex(connector.getNextIndex() - 1); // 默认的回退模式。
			else if (r.Result.getNextIndex() <= firstIndex) {
				// leader snapshot，follower 完全没法匹配了，后续的 TrySendAppendEntries 将启动 InstallSnapshot。
				connector.setNextIndex(firstIndex);
			} else if (r.Result.getNextIndex() >= lastIndex) {
				logger.fatal("Impossible r.Result.NextIndex({}) >= LastIndex({}) there must be a bug.",
						r.Result.getNextIndex(), lastIndex, new Exception());
				raft.fatalKill();
			} else
				connector.setNextIndex(r.Result.getNextIndex()); // fast locate
			trySendAppendEntries(connector, r); //resend. use new NextIndex。
			return Procedure.Success;
		} finally {
			raft.unlock();
		}
	}

	void trySendAppendEntries(Server.ConnectorEx connector, AppendEntries pending) throws Exception {
		// Pending 处理必须完成。
		connector.setAppendLogActiveTime(System.currentTimeMillis());
		if (connector.getPending() != pending)
			return;
		// 先清除，下面中断(return)不用每次自己清除。
		connector.setPending(null);

		if (!raft.isLeader())
			return; // skip if is not a leader

		// 【注意】
		// 正在安装Snapshot，此时不复制日志，肯定失败。
		// 不做这个判断也是可以工作的，算是优化。
		if (sendSnapshotting.contains(connector.getName()))
			return;

		var socket = connector.TryGetReadySocket();
		if (socket == null)
			return;

		if (connector.getNextIndex() > lastIndex)
			return; // copy end.

		if (connector.getNextIndex() == firstIndex) {
			// 已经到了日志开头，此时不会有prev-log，无法复制日志了。
			// 这一般发生在Leader进行了Snapshot，但是Follower的日志还更老。
			// 新起的Follower也一样。
			sendSnapshotting.start(connector);
			return;
		}

		var nextLog = readLog(connector.getNextIndex());
		if (nextLog == null) // Logs可能已经变成null了, 小概率事件
			return;
		var prevLog = readLog(nextLog.getIndex() - 1);
		if (prevLog == null) // Logs可能已经变成null了, 小概率事件
			return;

		connector.setPending(new AppendEntries());
		connector.getPending().Argument.setTerm(term);
		connector.getPending().Argument.setLeaderId(raft.getName());
		connector.getPending().Argument.setLeaderCommit(commitIndex);

		connector.getPending().Argument.setPrevLogIndex(prevLog.getIndex());
		connector.getPending().Argument.setPrevLogTerm(prevLog.getTerm());

		// 限制一次发送的日志数量，【注意】这个不是raft要求的。
		int maxCount = raft.getRaftConfig().getMaxAppendEntriesCount();
		RaftLog lastCopyLog = nextLog;
		for (var copyLog = nextLog;
			 maxCount > 0 && copyLog != null && copyLog.getIndex() <= lastIndex;
			 copyLog = readLog(copyLog.getIndex() + 1), --maxCount) {
			lastCopyLog = copyLog;
			connector.getPending().Argument.getEntries().add(new Binary(copyLog.encode()));
		}
		connector.getPending().Argument.setLastEntryIndex(lastCopyLog.getIndex());
		if (!connector.getPending().Send(socket, (p) ->
				processAppendEntriesResult(connector, p), raft.getRaftConfig().getAppendEntriesTimeout())) {
			connector.setPending(null);
			// Heartbeat Will Retry
		}
	}

	public RaftLog lastRaftLogTermIndex() throws RocksDBException {
		return RaftLog.decodeTermIndex(readLogBytes(lastIndex));
	}

	private void removeLogAndCancelStart(long startIndex, long endIndex) throws Exception {
		for (long index = startIndex; index <= endIndex; index++) {
			RaftLog raftLog;
			if (index > lastApplied && (raftLog = leaderAppendLogs.remove(index)) != null) {
				// 还没有applied的日志被删除，
				// 当发生在重新选举，但是旧的leader上还有一些没有提交的请求时，
				// 需要取消。
				// 其中判断：index > LastApplied 不是必要的。
				// Apply的时候已经TryRemove了，仅会成功一次。
				raftLog.cancelCallback();
			}
			removeLog(index);
		}
	}

	private void removeLog(long index) throws RocksDBException {
		var raftLog = readLog(index);
		if (raftLog != null) {
			var key = ByteBuffer.Allocate(9);
			key.WriteLong(index);
			logs.delete(writeOptions, key.Bytes, 0, key.WriteIndex);
			if (raftLog.getLog().getUnique().getRequestId() > 0)
				openUniqueRequests(raftLog.getLog().getCreateTime()).remove(raftLog);
		}
	}

	long followerOnAppendEntries(AppendEntries r) throws Exception {
		r.Result.setTerm(term); // maybe rewrite later
		r.Result.setSuccess(false); // set default false

		if (r.Argument.getTerm() < term || r.Argument.getTerm() > TERM_MAX) {
			// 1. Reply false if term < currentTerm (§5.1)
			// 过期term不重置选举计时，否则分区后重现的旧Leader的过期心跳会压制新选举。
			// 超过TERM_MAX的term同样按陈旧提前返回：下面switch无Older分支，
			// 落穿会setLeaderId接受非法Leader。
			r.SendResult();
			logger.info("this={} Leader={} PrevLogIndex={} invalid term({})",
					raft.getName(), r.Argument.getLeaderId(), r.Argument.getPrevLogIndex(), r.Argument.getTerm());
			return Procedure.Success;
		}

		// term合法才重置选举计时（raft要求只对有效Leader的消息重置）。
		setLeaderActiveTime(System.currentTimeMillis());

		switch (trySetTerm(r.Argument.getTerm())) {
		case Newer:
			raft.convertStateTo(Raft.RaftState.Follower);
			r.Result.setTerm(term); // new term
			break;

		case Same:
			switch (raft.getState()) {
			case Candidate:
				// 同term已存在合法Leader，仅Candidate让位转Follower。
				raft.convertStateTo(Raft.RaftState.Follower);
				break;
			case Leader:
				logger.fatal("Receive AppendEntries from another leader={} with same term={}, there must be a bug. this={}",
						r.Argument.getLeaderId(), term, raft.getLeaderId(), new Exception());
				raft.fatalKill();
				return 0;
			}
			break;
		}

		raft.setLeaderId(r.Argument.getLeaderId());

		// 心跳也是正常的AppendEntries（空entries带prevLog/leaderCommit），
		// 统一走下面的prevLog校验与commit推进，不再特殊提前返回。

		// check and copy log ...
		var prevLog = readLog(r.Argument.getPrevLogIndex());
		if (prevLog == null || prevLog.getTerm() != r.Argument.getPrevLogTerm()) {
			// 2. Reply false if log doesn't contain an entry
			// at prevLogIndex whose term matches prevLogTerm(§5.3)

			// fast locate when mismatch
			r.Result.setNextIndex(r.Argument.getPrevLogIndex() > lastIndex ? lastIndex + 1 : 0);

			r.SendResult();
			if (isDebugEnabled)
				logger.debug("this={} Leader={} Index={} prevLog mismatch",
						raft.getName(), r.Argument.getLeaderId(), r.Argument.getPrevLogIndex());
			return Procedure.Success;
		}

		// NodeReady：正常复制与心跳都会携带 leaderCommit 并在此检测。
		if (lastLeaderCommitIndex == 0) {
			// Term 增加时会重置为0，see TrySetTerm。严格点？
			lastLeaderCommitIndex = r.Argument.getLeaderCommit();
		} else if (r.Argument.getLeaderCommit() > lastLeaderCommitIndex) {
			// 这里只要LeaderCommit推进就行，不需要自己的CommitIndex变更。
			// LeaderCommit推进，意味着，已经达成了多数，自己此时可能处于少数派。
			// 本结点CommitIndex是否还处于更早的时期，是没有关系的。
			trySetNodeReady();
		}

		int entryIndex = 0;
		var copyLogIndex = prevLog.getIndex() + 1;
		for (; entryIndex < r.Argument.getEntries().size(); ++entryIndex, ++copyLogIndex) {
			RaftLog copyLog;
			try {
				copyLog = RaftLog.decode(r.Argument.getEntries().get(entryIndex), raft.getStateMachine()::logFactory);
			} catch (Throwable e) {
				// 与readLogForApply同理：无法decode的复制条目是永久错误，静默传播只会让本节点
				// 对AppendEntries永远无应答（leader无限重发），宁死不糊。
				fatalKillDecodeError("followerOnAppendEntries.copyLog", copyLogIndex, e);
				return 0; // fatalKill 不会返回；这里防御编译检查。
			}
			if (copyLog.getIndex() != copyLogIndex) {
				logger.fatal("copyLog.Index({}) != copyLogIndex({}) Leader={} this={}",
						copyLog.getIndex(), copyLogIndex, r.Argument.getLeaderId(), raft.getName(), new Exception());
				raft.fatalKill();
			}
			if (copyLog.getIndex() < firstIndex)
				continue; // 快照以前的日志忽略。

			// 本地已经存在日志。
			if (copyLog.getIndex() <= lastIndex) {
				var conflictCheck = readLog(copyLog.getIndex());
				if (conflictCheck == null || conflictCheck.getTerm() == copyLog.getTerm())
					continue;

				// 3. If an existing entry conflicts
				// with a new one (same index but different terms),
				// delete the existing entry and all that follow it(§5.3)
				// raft.pdf 5.3
				if (conflictCheck.getIndex() <= commitIndex) {
					logger.fatal("{} truncate committed entries: {} <= {}", raft.getName(),
							conflictCheck.getIndex(), commitIndex, new Exception());
					raft.fatalKill();
				}
				removeLogAndCancelStart(conflictCheck.getIndex(), lastIndex);
				lastIndex = conflictCheck.getIndex() - 1;
			}
			break;
		}
		// Append this and all following entries.
		// 4. Append any new entries not already in the log
		for (; entryIndex < r.Argument.getEntries().size(); ++entryIndex, ++copyLogIndex)
			saveLogRaw(copyLogIndex, r.Argument.getEntries().get(entryIndex));

		copyLogIndex--;
		// 必须判断，防止本次AppendEntries都是旧的。
		if (copyLogIndex > lastIndex)
			lastIndex = copyLogIndex;

		// CheckDump(prevLog.Index, copyLogIndex, r.Argument.Entries);

		// 5. If leaderCommit > commitIndex,
		// set commitIndex = min(leaderCommit, index of last new entry)
		// leaderCommit未推进但commitIndex>lastApplied时也要尝试apply：上次apply可能因
		// flush失败中断，静默应答会让follower以"健康"状态一直落后；每次AppendEntries
		// （含心跳）重试直到追平（apply异常时不发应答）。
		if (r.Argument.getLeaderCommit() > commitIndex || commitIndex > lastApplied) {
			if (r.Argument.getLeaderCommit() > commitIndex)
				commitIndex = Math.min(r.Argument.getLeaderCommit(), lastRaftLogTermIndex().getIndex());
			tryStartApplyTask(readLogForApply(commitIndex, "followerOnAppendEntries"));
		}
		r.Result.setSuccess(true);
		if (isDebugEnabled)
			logger.debug("{}: {}", raft.getName(), r);
		r.SendResultCode(0);

		if (r.Argument.getEntries().isEmpty() && null != raft.onFollowerReceiveKeepAlive)
			raft.onFollowerReceiveKeepAlive.run();

		return Procedure.Success;
	}

	@SuppressWarnings("unused")
	private void checkDump(long prevLogIndex, long lastIndex, ArrayList<Binary> entries) throws RocksDBException {
		var logs = new StringBuilder();
		for (var index = prevLogIndex + 1; index <= lastIndex; index++)
			logs.append(readLog(index)).append('\n');
		var copies = new StringBuilder();
		for (var entry : entries)
			copies.append(RaftLog.decode(entry, raft.getStateMachine()::logFactory)).append('\n');

		if (logs.toString().contentEquals(copies))
			return;

		logger.info("================= logs ======================");
		logger.info("{}", logs);
		logger.info("================= copies ======================");
		logger.info("{}", copies);
		raft.fatalKill();
	}
}
