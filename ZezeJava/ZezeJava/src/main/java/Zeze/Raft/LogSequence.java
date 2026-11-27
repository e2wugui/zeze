package Zeze.Raft;

import java.util.Objects;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.ZipFile;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action2;
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
import org.jspecify.annotations.Nullable;
import org.rocksdb.WriteOptions;
import Zeze.Util.RocksDatabase.Table;

/**
 * Raft 日志序列：日志与 term/voteFor/firstIndex 等元数据的 RocksDB 持久化、快照的代际提交与清扫、
 * 唯一请求存根，以及 leader 复制驱动与 follower/leader 两侧的 apply 循环。
 */
public class LogSequence {
	static final Logger logger = LogManager.getLogger(LogSequence.class);
	private static final boolean isDebugEnabled = logger.isDebugEnabled();
	public static final String snapshotFileName = "snapshot.dat";

	// legacy快照内嵌代次：代际化后不再写入，读取仅供构造器迁移引导。
	static final String snapshotManifestEntryName = "zeze.snapshot.manifest";

	// 读legacy快照内嵌代次；无entry、文件不存在或损坏返回null（迁移按N=F处理）。
	static @Nullable Long readSnapshotManifest(Path snapshotPath) {
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

	// voteFor 不属于日志本身，但需要持久化，故定义在这里。
	private String voteFor;
	private boolean nodeReady;
	private long lastLeaderCommitIndex;

	public volatile @Nullable TaskCompletionSource<Boolean> removeLogBeforeFuture;
	public volatile boolean logsAvailable;

	private long leaderActiveTime = System.currentTimeMillis(); // Leader 与 Follower 共用的活跃时间戳

	private final WriteOptions writeOptions;
	private @Nullable Table logs;
	private @Nullable RocksDatabase database;
	private final boolean sharedDatabase;
	private @Nullable Table rafts;
	private final LongConcurrentHashMap<UniqueRequestSet> uniqueRequestSets = new LongConcurrentHashMap<>();

	private final byte[] raftsTermKey;
	private final byte[] raftsVoteForKey;
	private final byte[] raftsFirstIndexKey;
	private final byte[] raftsNodeReadyKey; // 只写一次，与其他 key 统一形式
	private final byte[] lastSnapshotIndexKey;

	public volatile @Nullable TaskCompletionSource<Boolean> applyFuture; // 后台 apply 任务句柄
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

	// 初始化会写入 index=0 哨兵日志，快照后也会保留 lastApplied 日志，正常情况下非空。
	public RaftLog lastAppliedLogTermIndex() throws RocksDBException {
		return RaftLog.decodeTermIndex(Objects.requireNonNull(readLogBytes(lastApplied)));
	}

	private void saveFirstIndex(long newFirstIndex) throws RocksDBException {
		var firstIndexValue = ByteBuffer.Allocate(9);
		firstIndexValue.WriteLong(newFirstIndex);
		//noinspection DataFlowIssue
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

	// 代际提交：先 fsync 并原子改名为不可变 gen 文件，再通过 saveFirstIndex 单点翻转指针。
	private void _commitSnapshot(Path path, long newFirstIndex) throws IOException, RocksDBException {
		raft.lock();
		try {
			// 丢弃过期候选，避免与并发 InstallSnapshot 竞争导致 firstIndex 回退。
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

	// 翻转指针提交已就位的 gen 文件（调用方持 Raft 锁）。
	private void _flipCommittedGen(long newFirstIndex) throws RocksDBException {
		saveFirstIndex(newFirstIndex); // RocksDB 写入 —— 唯一提交动作
		startSweepGenFiles();          // 删 index < firstIndex 的旧代（失败进重试名单）
		startRemoveLogOnlyBefore(newFirstIndex);
	}

	// 候选发布：补齐 fsync 后原子改名为不可变 gen 文件（调用方持 Raft 锁）。
	private void publishGenCandidate(Path path, long newFirstIndex) throws IOException {
		try (var channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
			channel.force(true); // 必须先于rename，确保候选内容已落盘。
		}
		var gen = genSnapshotPath(newFirstIndex);
		try {
			Files.move(path, gen, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(path, gen, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private File @Nullable [] listDbHomeFiles() {
		return new File(Objects.requireNonNull(raft.getRaftConfig().getDbHome())).listFiles();
	}

	// held 候选：index > firstIndex 的 gen 文件，按 index 排序（调用方持 Raft 锁）。
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

	// 解析代际快照文件名 snapshot.dat.<index>，非快照或格式非法返回 null。
	static @Nullable Long parseGenSnapshotIndex(File file) {
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
			//noinspection DataFlowIssue
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
			boolean reachedBoundary = false;
			try {
				try (var it = newLogsIterator()) {
					it.seekToFirst();
					while (logsAvailable && !raft.isShutdown && RocksDatabase.isValidOrThrow(it)) {
						// 这里只需要log的Index，直接从key里面获取了。
						if (ByteBuffer.Wrap(it.key()).ReadLong() >= index) {
							reachedBoundary = true;
							break;
						}

						var key = it.key();
						//noinspection DataFlowIssue
						logs.delete(writeOptions, key);

						// 删除快照前的日志时不删唯一请求存根（快照建立时刻稍前的请求仍需唯一保证），存根自行过期清理。
						// 注意：完全崩溃换新机后仍有小概率无法判断唯一，较好的做法是从工作节点复制unique/作为初始数据。
						it.next();
					}
				}
			} finally {
				// 先关闭原生迭代器并发布结果，再在 Raft 锁内按同一性置空 removeLogBeforeFuture。
				future.setResult(reachedBoundary);
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
				//noinspection DataFlowIssue
				table = database.getOrAddTable(tableName);
			} catch (RocksDBException e) {
				throw Task.forceThrow(e);
			}
		}

		private void put(RaftLog log, boolean isApply) throws RocksDBException {
			put(log, isApply, null);
		}

		// batch形态——存根与日志合入同一WriteBatch原子提交（见appendLog）。
		// 查重读（默认ReadOptions）仍在组装期执行，与单写形态语义一致。
		private void put(RaftLog log, boolean isApply, RocksDatabase.@Nullable Batch batch) throws RocksDBException {
			var key = ByteBuffer.Allocate(32);
			//noinspection DataFlowIssue
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
			if (batch != null)
				table.put(batch, key.Bytes, 0, key.WriteIndex, value.Bytes, 0, value.WriteIndex);
			else
				table.put(writeOptions, key.Bytes, 0, key.WriteIndex, value.Bytes, 0, value.WriteIndex);
		}

		public void save(RaftLog log) throws RocksDBException {
			put(log, false);
		}

		public void save(RaftLog log, RocksDatabase.@Nullable Batch batch) throws RocksDBException {
			put(log, false, batch);
		}

		public void apply(RaftLog log) throws RocksDBException {
			put(log, true);
		}

		public void remove(RaftLog log) throws RocksDBException {
			remove(log, null);
		}

		public void remove(RaftLog log, RocksDatabase.@Nullable Batch batch) throws RocksDBException {
			var key = ByteBuffer.Allocate(32);
			//noinspection DataFlowIssue
			log.getLog().getUnique().encode(key);
			if (batch != null)
				table.delete(batch, key.Bytes, 0, key.WriteIndex);
			else
				table.delete(writeOptions, key.Bytes, 0, key.WriteIndex);
		}

		// 仅在日志库中对应存根仍为未应用态（!isApplied）时删除，避免误删非原子状态机的已应用存根。
		public void removeIfNotApplied(UniqueRequestId unique, RocksDatabase.@Nullable Batch batch) throws RocksDBException {
			var key = ByteBuffer.Allocate(32);
			unique.encode(key);
			var val = table.get(RocksDatabase.getDefaultReadOptions(), key.Bytes, 0, key.WriteIndex);
			if (val == null)
				return;
			var state = new UniqueRequestState();
			state.decode(ByteBuffer.Wrap(val));
			if (state.isApplied())
				return;
			if (batch != null)
				table.delete(batch, key.Bytes, 0, key.WriteIndex);
			else
				table.delete(writeOptions, key.Bytes, 0, key.WriteIndex);
		}

		public @Nullable UniqueRequestState getRequestState(IRaftRpc raftRpc) throws RocksDBException {
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
		try {
			// 根链接和子链接都只删入口；Windows junction 的 NOFOLLOW 属性为 directory+other。
			var attrs = Files.readAttributes(path.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
			if (attrs.isDirectory() && !attrs.isOther()) {
				File[] contents = path.listFiles();
				if (contents != null)
					for (File f : contents)
						deleteDirectory(f);
			}
		} catch (IOException ignored) {
			return; // 尽力删除；需要确认的调用方由 deletedDirectoryAndCheck 报告失败。
		}
		//noinspection ResultOfMethodCallIgnored
		path.delete();
	}

	public static void deletedDirectoryAndCheck(File path, int checkCount) {
		while (--checkCount >= 0) {
			deleteDirectory(path);
			if (!Files.exists(path.toPath(), LinkOption.NOFOLLOW_LINKS))
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
		//noinspection DataFlowIssue
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

		// 状态机库的终态存根随快照走，按相同日期分桶规则清理。
		raft.getStateMachine().removeExpiredUniqueStubTables(expired);
	}

	void cancelPendingAppendLogFutures() throws Exception {
		for (var job : leaderAppendLogs)
			job.cancelCallback();
		leaderAppendLogs.clear();
	}

	void close() throws Exception {
		// 必须在 Raft.isShutdown 置位（true）之后调用。
		cancelPendingAppendLogFutures();

		raft.lock();
		try {
			if (logs != null) {
				logger.info("close logs: {}", raft.getRaftConfig().getDbHome());
				logs = null;
			}

			rafts = null;
			// 共享 db 由外部关闭。
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

	public LogSequence(Raft raft, @Nullable RocksDatabase database) throws RocksDBException {
		this.raft = raft;
		// 写选项由 RaftConfig.SyncWrite 决定，构造后不可变。
		this.writeOptions = raft.getRaftConfig().isSyncWrite()
				? RocksDatabase.getSyncWriteOptions() : RocksDatabase.getDefaultWriteOptions();
		this.sharedDatabase = database != null;
		this.database = database;
		if (this.database == null)
			this.database = new RocksDatabase(Paths.get(Objects.requireNonNull(raft.getRaftConfig().getDbHome()), "db").toString());

		this.rafts = this.database.getOrAddTable(raft.getName() + ".rafts");
		{
			raftsTermKey = makeRaftsKey(0);
			var termValue = rafts.get(RocksDatabase.getDefaultReadOptions(), raftsTermKey);
			term = termValue != null ? ByteBuffer.Wrap(termValue).ReadLong() : 0;
			raftsVoteForKey = makeRaftsKey(1);
			var voteForValue = rafts.get(RocksDatabase.getDefaultReadOptions(), raftsVoteForKey);
			voteFor = voteForValue != null ? ByteBuffer.Wrap(voteForValue).ReadString() : "";
			// 并发 snapshot 下 logs 首条记录可能早于 firstIndex，故单独持久化（-1 表示尚无已提交快照）。
			raftsFirstIndexKey = makeRaftsKey(2);
			var firstIndexValue = rafts.get(RocksDatabase.getDefaultReadOptions(), raftsFirstIndexKey);
			firstIndex = firstIndexValue != null ? ByteBuffer.Wrap(firstIndexValue).ReadLong() : -1;
			// NodeReady：成为就绪 Leader 或观察到 LeaderCommit 推进后持久化置为 true；未就绪前仅对未就绪候选者授票。
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
			try (var itLast = logs.iterator()) {
				itLast.seekToLast();
				if (RocksDatabase.isValidOrThrow(itLast))
					lastIndex = RaftLog.decodeTermIndex(itLast.value()).getIndex();
				else {
					// 空日志写入 index=0、term=0 哨兵（不可填当前 term，防止空节点在选举中凭高 term 胜出）。
					saveLog(new RaftLog(0, 0, new HeartbeatLog()));
					lastIndex = 0;
				}
				logger.info("{}-{} {} LastIndex={} Count={}", raft.getName(), raft.isLeader(),
						raft.getRaftConfig().getDbHome(), lastIndex, getTestStateMachineCount());

				// 崩溃自愈：InstallSnapshot 在 logs.drop 与边界日志写入之间崩溃会导致 lastIndex < firstIndex，重置回无快照状态。
				if (lastIndex < firstIndex) {
					logger.warn("{} crash recovery: lastIndex({}) < firstIndex({}), reset to no-snapshot state."
							+ " endReceiveInstallSnapshot crashed between logs.drop() and saveLog?",
							raft.getName(), lastIndex, firstIndex);
					saveFirstIndex(-1);
					firstIndex = -1;
				}

				// 尚无已提交快照时以日志首条索引作为 firstIndex。
				if (firstIndex == -1) {
					try (var itFirst = logs.iterator()) {
						itFirst.seekToFirst();
						if (RocksDatabase.isValidOrThrow(itFirst)) {
							firstIndex = RaftLog.decodeTermIndex(itFirst.value()).getIndex();
						}
					}
				}
				// 兼容迁移旧版固定名 snapshot.dat 至代际文件名（须在启动清扫前执行）：按内嵌 manifest 校正或丢弃过期快照。
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
							// 同步推进 lastSnapshotIndex
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
								// 迁移失败直接抛出，避免在不确定磁盘布局下继续运行。
								throw Task.forceThrow(e);
							}
						}
					}
				}
				lastApplied = firstIndex;
				commitIndex = firstIndex;
			}

			// 崩溃自愈：firstIndex 边界日志缺失说明上次 InstallSnapshot 在更新 firstIndex 前崩溃留下空洞，重置回无快照状态；
			// 必须在上方迭代器关闭后再 drop 列族，且仅检查字节存在性（构造期状态机工厂尚未就绪）。
			if (firstIndex >= 0 && readLogBytes(firstIndex) == null) {
				logger.warn("{} crash recovery: no boundary log at firstIndex={}, discard half-installed logs({})."
								+ " endReceiveInstallSnapshot crashed after boundary saveLog?",
						raft.getName(), firstIndex, lastIndex);
				try {
					logs.drop();
				} catch (RocksDBException e) {
					throw e;
				} catch (Exception e) {
					throw Task.forceThrow(e);
				}
				logs = this.database.getOrAddTable(raft.getName() + ".logs");
				// index=0 哨兵 term 恒为 0
				saveLog(new RaftLog(0, 0, new HeartbeatLog()));
				lastIndex = 0;
				saveFirstIndex(-1);
				firstIndex = 0;
				lastApplied = 0;
				commitIndex = 0;
			}
		}
		logsAvailable = true;

		// 启动清扫（在 legacy 迁移之后）：清理 .installing./.tmp/.commit.delayed 及非当前 firstIndex 的孤儿快照。
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
		//noinspection DataFlowIssue
		rafts.put(writeOptions, raftsNodeReadyKey, 0, raftsNodeReadyKey.length, value.Bytes, 0, value.WriteIndex);
	}

	/**
	 * 查询唯一请求状态：
	 * null 表示 createTime 超出合法窗口（RaftExpired）；
	 * NOT_FOUND 表示首次请求；其余状态对应已应用（RaftApplied）或进行中（DuplicateRequest）。
	 */
	@Nullable UniqueRequestState tryGetRequestState(Protocol<?> p) throws RocksDBException {
		var raftRpc = (IRaftRpc)p;

		var create = raftRpc.getCreateTime();
		var now = System.currentTimeMillis();
		if (!isUniqueRequestCreateTimeValid(create, now, raft.getRaftConfig().getUniqueRequestExpiredDays()))
			return null;

		// 终态存根优先：与业务数据原子提交，恢复或日志压缩后仍可重放原应答。
		var applied = raft.getStateMachine().getUniqueAppliedStub(raftRpc);
		if (applied != null)
			return applied;
		UniqueRequestState state = openUniqueRequests(raftRpc.getCreateTime()).getRequestState(raftRpc);
		return state != null ? state : UniqueRequestState.NOT_FOUND;
	}

	/**
	 * 校验唯一请求的 createTime 窗口：过旧（超过 expiredDays 天）或过新（超过未来 1 天）返回 false，
	 * 防止时钟异常或恶意请求创建永不过期的未来日期表。
	 */
	static boolean isUniqueRequestCreateTimeValid(long create, long now, int expiredDays) {
		if (create <= now - expiredDays * 86400_000L)
			return false;
		return create <= now + 86400_000L;
	}

	private UniqueRequestSet openUniqueRequests(long time) {
		//noinspection DataFlowIssue
		return uniqueRequestSets.computeIfAbsent(toUniqueRequestKey(new Date(time)),
				k -> new UniqueRequestSet(makeUniqueRequestTableName(raft.getName(), k)));
	}

	// 唯一请求存根按天建表（<raftName>.unique.<yyyy>.<M>.<d>），日志库与状态机库共用同一命名规则。
	public static String makeUniqueRequestTableName(String raftName, long key) {
		return raftName + ".unique." + (key >> 16) + '.' + ((key >> 8) & 0xff) + '.' + (key & 0xff);
	}

	// 解析唯一请求存根表名中的日期（当天 0 点）；前缀不匹配或日期非法返回 null。
	public static @Nullable Date parseUniqueRequestDate(String raftName, String tableName) {
		var prefix = raftName + ".unique.";
		if (!tableName.startsWith(prefix))
			return null;
		var dateText = tableName.substring(prefix.length());
		var format = new SimpleDateFormat("yyyy.M.d");
		format.setLenient(false);
		try {
			var date = format.parse(dateText);
			// 严格全量匹配格式，避免尾随字符或非法日期被宽松解析。
			return format.format(date).equals(dateText) ? date : null;
		} catch (ParseException e) {
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	public static long toUniqueRequestKey(Date date) {
		return ((date.getYear() + 1900L) << 16) + ((date.getMonth() + 1) << 8) + date.getDate();
	}

	public WriteOptions getWriteOptions() {
		return writeOptions;
	}

	void saveLog(RaftLog log) throws RocksDBException {
		saveLog(log, null);
	}

	// 支持传入 WriteBatch，与 unique 存根原子提交。
	void saveLog(RaftLog log, RocksDatabase.@Nullable Batch batch) throws RocksDBException {
		var key = ByteBuffer.Allocate(9);
		key.WriteLong(log.getIndex());
		var value = log.encode();
		if (batch != null)
			//noinspection DataFlowIssue
			logs.put(batch, key.Bytes, 0, key.WriteIndex, value.Bytes, 0, value.WriteIndex);
		else
			//noinspection DataFlowIssue
			logs.put(writeOptions, key.Bytes, 0, key.WriteIndex, value.Bytes, 0, value.WriteIndex);

		if (isDebugEnabled)
			//noinspection DataFlowIssue
			logger.debug("{}-{} RequestId={} Index={} Count={}", raft.getName(), raft.isLeader(),
					log.getLog().getUnique().getRequestId(), log.getIndex(), getTestStateMachineCount());
	}

	private void saveLogRaw(long index, Binary rawValue, RocksDatabase.@Nullable Batch batch) throws RocksDBException {
		var key = ByteBuffer.Allocate(9);
		key.WriteLong(index);
		if (batch != null)
			Objects.requireNonNull(logs).put(batch, key.Bytes, 0, key.WriteIndex,
					rawValue.bytesUnsafe(), rawValue.getOffset(), rawValue.size());
		else
			Objects.requireNonNull(logs).put(writeOptions, key.Bytes, 0, key.WriteIndex,
					rawValue.bytesUnsafe(), rawValue.getOffset(), rawValue.size());

		if (isDebugEnabled)
			logger.debug("{}-{} RequestId=? Index={} Count={}",
					raft.getName(), raft.isLeader(), index, getTestStateMachineCount());
	}

	private byte @Nullable [] readLogBytes(long index) throws RocksDBException {
		var key = ByteBuffer.Allocate(9);
		key.WriteLong(index);
		var logs = this.logs;
		return logs != null ? logs.get(RocksDatabase.getDefaultReadOptions(), key.Bytes, 0, key.WriteIndex) : null;
	}

	@Nullable RaftLog readLog(long index) throws RocksDBException {
		var value = readLogBytes(index);
		return value != null ? RaftLog.decode(new Binary(value), raft.getStateMachine()::logFactory) : null;
	}

	/**
	 * 读取并解码待应用日志：decode 失败属永久错误直接 fatalKill，RocksDBException 向上抛出以保留重试语义。
	 */
	private @Nullable RaftLog readLogForApply(long index, String where) throws RocksDBException {
		try {
			return readLog(index);
		} catch (RocksDBException e) {
			throw e; // 瞬时读失败：lastApplied 不推进，等待下次重试
		} catch (Throwable e) {
			fatalKillDecodeError(where, index, e);
			return null;
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

	/** term 合法上界：防止超大 term 被持久化后导致 term+1 溢出回绕并永久冻结选举。 */
	public static final long TERM_MAX = Long.MAX_VALUE / 1024;

	private volatile long lastRejectedOversizedTerm; // 同值只记一次日志，防洪泛刷屏

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
			var termValue = ByteBuffer.Allocate(9);
			termValue.WriteLong(term);
			var voteForValue = ByteBuffer.Allocate(1);
			voteForValue.WriteString("");
			// 任期推进与清空投票须原子持久化，成功后再更新内存状态。
			//noinspection DataFlowIssue
			try (var batch = database.borrowBatch()) {
				//noinspection DataFlowIssue
				rafts.put(batch, raftsTermKey, 0, raftsTermKey.length, termValue.Bytes, 0, termValue.WriteIndex);
				rafts.put(batch, raftsVoteForKey, 0, raftsVoteForKey.length,
						voteForValue.Bytes, 0, voteForValue.WriteIndex);
				batch.commit(writeOptions);
			}
			this.term = term;
			voteFor = "";
			raft.setLeaderId("");
			lastLeaderCommitIndex = 0;
			return SetTermResult.Newer;
		}
		return term == this.term ? SetTermResult.Same : SetTermResult.Older;
	}

	// 保留从存储加载 voteFor 时的防御性判空检查
	@SuppressWarnings("ConstantConditions")
	public boolean canVoteFor(@Nullable String voteFor) {
		String thisVoteFor = this.voteFor;
		return thisVoteFor == null || thisVoteFor.isEmpty() || thisVoteFor.equals(voteFor);
	}

	public void setVoteFor(String voteFor) throws RocksDBException {
		if (!this.voteFor.equals(voteFor)) {
			var voteForValue = ByteBuffer.Allocate(5 + voteFor.length());
			voteForValue.WriteString(voteFor);
			//noinspection DataFlowIssue
			rafts.put(writeOptions, raftsVoteForKey, 0, raftsVoteForKey.length,
					voteForValue.Bytes, 0, voteForValue.WriteIndex);
			this.voteFor = voteFor;
		}
	}

	private void tryCommit(AppendEntries rpc, Server.ConnectorEx connector) throws Exception {
		connector.setNextIndex(rpc.Argument.getLastEntryIndex() + 1);
		connector.setMatchIndex(rpc.Argument.getLastEntryIndex());

		if (rpc.Argument.getLastEntryIndex() <= commitIndex) {
			// 未推进 commitIndex 时若仍有未应用日志（如上次 flush 失败），也借机触发重试 apply。
			if (commitIndex > lastApplied)
				tryStartApplyTask(readLogForApply(commitIndex, "tryCommit"));
			return;
		}

		// 计算已被多数派复制的最大日志索引，仅当该日志属于当前 term 时推进 commitIndex。
		var followers = new ArrayList<Server.ConnectorEx>();
		raft.getServer().getConfig().forEachConnector(c -> followers.add((Server.ConnectorEx)c));
		followers.sort((a, b) -> Long.compare(b.getMatchIndex(), a.getMatchIndex()));
		var maxMajorityLogIndex = followers.get(raft.getRaftConfig().getHalfCount() - 1).getMatchIndex();
		if (maxMajorityLogIndex > commitIndex) {
			var maxMajorityLog = readLogForApply(maxMajorityLogIndex, "tryCommit");
			if (maxMajorityLog == null || maxMajorityLog.getTerm() != term) {
				// 仅在当前 term 日志达成多数派时推进 commitIndex，间接提交旧 term 日志。
				return;
			}
			commitIndex = maxMajorityLogIndex;
			trySetNodeReady();
			tryStartApplyTask(maxMajorityLog);
		}
	}

	// 调用方须持有 Raft 锁
	private void tryStartApplyTask(@Nullable RaftLog lastApplicableLog) throws Exception {
		if (applyFuture == null && !raft.isShutdown) {
			if (commitIndex - lastApplied < raft.getRaftConfig().getBackgroundApplyCount()) {
				// 积压量小于阈值时在当前线程直接 apply
				tryApply(lastApplicableLog, Long.MAX_VALUE);
				return;
			}

			applyFuture = new TaskCompletionSource<>();
			Raft.executeImportantTask(() -> {
				try {
					Objects.requireNonNull(applyFuture).setResult(TaskSpec.ofFunc(this::backgroundApply).name("BackgroundApply").call() == 0);
				} finally {
					applyFuture = null; // 允许后续再次启动后台 apply
				}
			});
		}
	}

	private long backgroundApply() throws Exception {
		while (!raft.isShutdown) {
			raft.lock();
			try {
				// 重新读取 commitIndex 对应日志，期间 commitIndex 可能继续推进。
				var lastApplicableLog = readLogForApply(commitIndex, "backgroundApply");
				tryApply(lastApplicableLog, raft.getRaftConfig().getBackgroundApplyCount());
				if (lastApplicableLog != null && lastApplied == lastApplicableLog.getIndex())
					return 0;
			} finally {
				raft.unlock();
			}
			Thread.yield();
		}
		return Procedure.CancelException;
	}

	void tryApply(@Nullable RaftLog lastApplicableLog, long count) throws Exception {
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
				return;
			}

			index = raftLog.getIndex() + 1;
			try {
				raft.getStateMachine().apply(raftLog);
			} catch (Rocks.FlushException e) {
				// flush 失败时保留原始 raftLog 至 leaderAppendLogs 且不推进 lastApplied，待重试 flush 成功后唤醒回调。
				if (raftLog.isLeaderRequest() && leaderAppendLogs.putIfAbsent(raftLog.getIndex(), raftLog) != null) {
					logger.fatal("LeaderAppendLogs.TryAdd Fail. Index={}", raftLog.getIndex(), new Exception());
					raft.fatalKill();
				}
				throw e;
			}
			// 支持原子应用的状态机已同批提交存根；其他状态机沿用日志库路径。
			//noinspection DataFlowIssue
			if (!raft.getStateMachine().hasAtomicAppliedState() && raftLog.getLog().getUnique().getRequestId() > 0) {
				try {
					openUniqueRequests(raftLog.getLog().getCreateTime()).apply(raftLog);
				} catch (RocksDBException e) {
					// 保留原始 leader 请求及其回调，以支持重试 apply。
					if (raftLog.isLeaderRequest() && leaderAppendLogs.putIfAbsent(raftLog.getIndex(), raftLog) != null) {
						logger.fatal("LeaderAppendLogs.TryAdd Fail. Index={}", raftLog.getIndex(), new Exception());
						raft.fatalKill();
					}
					throw e;
				}
			}
			lastApplied = raftLog.getIndex();
			if (isDebugEnabled && lastIndex - lastApplied < 10) {
				//noinspection DataFlowIssue
				logger.debug("{}-{} {} RequestId={} LastIndex={} LastApplied={} Count={}",
						raft.getName(), raft.isLeader(), raft.getRaftConfig().getDbHome(),
						raftLog.getLog().getUnique().getRequestId(), lastIndex, lastApplied,
						getTestStateMachineCount());
			}
			raftLog.invokeCallback();
		}
		trySnapshot();
	}

	private void trySnapshot() throws RocksDBException {
		var snapshotLogCount = raft.getRaftConfig().getSnapshotLogCount();
		if (snapshotLogCount > 0) {
			if (lastApplied - lastSnapshotIndex > snapshotLogCount) {
				lastSnapshotIndex = lastApplied;
				var bb = ByteBuffer.Allocate();
				bb.WriteLong(lastSnapshotIndex);
				Objects.requireNonNull(rafts).put(writeOptions,
						lastSnapshotIndexKey, 0, lastSnapshotIndexKey.length,
						bb.Bytes, bb.ReadIndex, bb.size());
				TaskSpec.ofAction(this::snapshot).name("Snapshot").run();
			}
		}
	}

	public long getTestStateMachineCount() {
		return raft.getStateMachine().getDebugCount();
	}

	public void sendHeartbeatTo(Server.ConnectorEx connector) {
		raft.lock();
		try {
			var now = System.currentTimeMillis();
			connector.setHeartbeatTime(now);

			if (!raft.isLeader())
				return;

			if (connector.getPending() != null)
				return;

			if (sendSnapshotting.contains(connector.getName()))
				return;

			var socket = connector.TryGetReadySocket();
			if (socket == null)
				return; // 等待下轮心跳重试

			RaftLog last;
			try {
				last = lastRaftLogTermIndex();
			} catch (RocksDBException e) {
				logger.warn("sendHeartbeatTo lastRaftLogTermIndex", e); // 下轮心跳重试
				return;
			}
			var heartbeat = new AppendEntries();
			heartbeat.Argument.setTerm(term);
			heartbeat.Argument.setLeaderId(raft.getName());
			// 心跳携带 prevLog 与 leaderCommit，以便空闲期推进 follower 的 commitIndex/nodeReady 并探测日志分叉。
			heartbeat.Argument.setPrevLogIndex(last.getIndex());
			heartbeat.Argument.setPrevLogTerm(last.getTerm());
			heartbeat.Argument.setLeaderCommit(commitIndex);
			heartbeat.Send(socket, (p) -> {
				if (heartbeat.isTimeout())
					return 0;

				raft.lock();
				try {
					if (raft.isShutdown)
						return 0;
					if (raft.getLogSequence().trySetTerm(heartbeat.Result.getTerm()) == SetTermResult.Newer) {
						raft.convertStateTo(Raft.RaftState.Follower);
						return Procedure.Success;
					}

					if (!raft.isLeader())
						return 0;

					// 过滤跨选举迟到的旧任期心跳应答，避免误回退 nextIndex 触发多余的 InstallSnapshot。
					if (heartbeat.Argument.getTerm() != term)
						return 0;

					// 仅当无在途复制时按心跳失配结果调整 nextIndex 并驱动复制。
					if (!heartbeat.Result.getSuccess() && connector.getPending() == null) {
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
					// 借心跳应答兜底触发未应用日志的 apply，避免空闲 Leader 在并发竞态或 flush 失败后停滞。
					if (commitIndex > lastApplied)
						tryStartApplyTask(readLogForApply(commitIndex, "sendHeartbeatTo"));
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
		/** appendLog 的应答：写入的 term 与 index。 */
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
		return appendLog(log, -1);
	}

	public AppendLogResult appendLog(Log log, long expectedTerm) throws Exception {
		var future = new TaskCompletionSource<RaftLog>();
		var result = appendLog(log, (raftLog, success) -> {
			if (success)
				future.setResult(raftLog);
			else
				future.cancel(false);
		}, expectedTerm);
		if (!future.await(raft.getRaftConfig().getAppendEntriesTimeout() * 2L + 1000)) {
			raft.lock();
			try {
				var pendingLog = leaderAppendLogs.get(result.index);
				if (pendingLog != null && pendingLog.getTerm() == result.term)
					leaderAppendLogs.remove(result.index, pendingLog);
			} finally {
				raft.unlock();
			}
			// 超时后日志条目仍留在序列中且命运未决，须等待其确认已应用或已截断后再抛重试异常，避免调用方提前释放锁引发丢失更新。
			if (waitLogFateDetermined(result.index,
					raft.getRaftConfig().getAppendEntriesTimeout() * 2L + 1000, result.term) == LogFate.Applied)
				return result;
			throw new RaftRetryException("timeout or canceled");
		}
		return result;
	}

	/** 等待指定索引日志条目的命运确定（不校验 term）。 */
	LogFate waitLogFateDetermined(long index, long waitMs) {
		return waitLogFateDetermined(index, waitMs, -1);
	}

	/**
	 * 等待指定索引日志条目的命运确定（已应用或已截断删除），必须在 Raft 锁外调用，超时后放弃并返回 Undetermined。
	 *
	 * @param expectTerm 期望条目任期；>=0 时校验同索引条目任期一致以防换主覆写导致的幻影提交，负值跳过校验。
	 */
	LogFate waitLogFateDetermined(long index, long waitMs, long expectTerm) {
		var deadline = System.nanoTime() + waitMs * 1_000_000L;
		while (!raft.isShutdown) {
			raft.lock();
			try {
				// lastApplied 非 volatile，在 Raft 锁内读取保证可见性。
				if (lastApplied >= index) {
					if (expectTerm < 0)
						return LogFate.Applied;
					var log = readLog(index);
					if (log != null && log.getTerm() == expectTerm)
						return LogFate.Applied;
					// 任期不匹配表示已被覆盖；null 表示已被快照压实，保守按未决处理。
					return log == null ? LogFate.Undetermined : LogFate.Removed;
				}
				if (readLog(index) == null)
					return LogFate.Removed; // 已截断或丢弃
			} catch (RocksDBException e) {
				return LogFate.Undetermined; // 读取失败（如 db 已关闭），不再等待
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

	public AppendLogResult appendLog(Log log, @Nullable Action2<RaftLog, Boolean> callback) throws Exception {
		return appendLog(log, callback, -1);
	}

	public AppendLogResult appendLog(Log log, @Nullable Action2<RaftLog, Boolean> callback, long expectedTerm) throws Exception {
		raft.lock();
		try {
			if (!raft.isWorkingLeader())
				throw new RaftRetryException("not leader"); // 快速失败
			// 在锁内校验冻结任期，避免换主重当选后写入过期业务日志或存根。
			if (expectedTerm >= 0 && term != expectedTerm)
				throw new RaftRetryException("term changed: expected=" + expectedTerm + ", actual=" + term);

			var raftLog = new RaftLog(term, lastIndex + 1, log);
			// unique 存根与日志合入同一 WriteBatch 原子提交，避免中间失败残留孤儿存根或复用 (term, index)。
			//noinspection DataFlowIssue
			try (var batch = database.borrowBatch()) {
				//noinspection DataFlowIssue
				if (raftLog.getLog().getUnique().getRequestId() > 0)
					openUniqueRequests(raftLog.getLog().getCreateTime()).save(raftLog, batch);
				saveLog(raftLog, batch);
				batch.commit(writeOptions);
			}

			// 容易出错的步骤前置。
			if (null != callback) {
				raftLog.setLeaderCallback(callback);
				if (leaderAppendLogs.putIfAbsent(raftLog.getIndex(), raftLog) != null) {
					logger.fatal("LeaderAppendLogs.TryAdd Fail. Index={}", raftLog.getIndex(), new Exception());
					raft.fatalKill();
				}
			}
			lastIndex = raftLog.getIndex();
			// 广播中途异常不回滚 lastIndex，避免复用同一 (term, index) 写不同内容破坏日志匹配不变式。
			raft.getServer().getConfig().ForEachConnector(c -> trySendAppendEntries((Server.ConnectorEx)c, null));
			var result = new AppendLogResult();
			result.term = term;
			result.index = lastIndex;
			return result;
		} finally {
			raft.unlock();
		}
	}

	boolean getSnapshotting() {
		return snapshotting;
	}

	// 调用方须持有 Raft 锁
	void setSnapshotting(boolean value) {
		snapshotting = value;
		if (!value)
			raft.signalAll(); // 唤醒停机排空等待者
	}

	public SendSnapshotting getSendSnapshotting() {
		return sendSnapshotting;
	}

	public String getSnapshotFullName() {
		return Paths.get(Objects.requireNonNull(raft.getRaftConfig().getDbHome()), snapshotFileName).toString();
	}

	// 代际快照文件：写入后不可变，只整体删除。
	public Path genSnapshotPath(long lastIncludedIndex) {
		return Paths.get(Objects.requireNonNull(raft.getRaftConfig().getDbHome()), snapshotFileName + "." + lastIncludedIndex);
	}

	// 已提交快照=firstIndex指认的gen文件；无快照时返回legacy名（不存在）。
	// 读者统一经此解析不可变文件，与后续提交/清扫不竞争。
	public String getCommittedSnapshotFile() {
		return firstIndex >= 0 ? genSnapshotPath(firstIndex).toString() : getSnapshotFullName();
	}

	/**
	 * 水位增量恢复（须持 Raft 锁调用）：若状态机记录的原子应用水位在 [firstIndex, lastIndex] 内且与日志任期一致，
	 * 则直接定位 lastApplied 与 commitIndex 并跳过全量快照装载；否则返回 false 回退全量恢复。
	 */
	public boolean tryRecoverFromWatermark() throws RocksDBException {
		var watermark = raft.getStateMachine().readAppliedWatermark();
		if (watermark == null || watermark.index() < firstIndex || watermark.index() > lastIndex)
			return false;
		var logBytes = readLogBytes(watermark.index());
		if (logBytes == null)
			return false;
		var header = RaftLog.decodeTermIndex(logBytes);
		if (header.getTerm() != watermark.term() || header.getIndex() != watermark.index())
			return false;
		lastApplied = watermark.index();
		// 应用水位证明此前缀已提交，恢复提交下界以保护它不被冲突日志截断。
		commitIndex = Math.max(commitIndex, lastApplied);
		logger.info("{} incremental recover from applied watermark: lastApplied={} firstIndex={} lastIndex={}",
				raft.getName(), lastApplied, firstIndex, lastIndex);
		return true;
	}

	/** 调用方持有 Raft 锁；后台 apply 可能落后于 commitIndex，两个前缀都不能回退。 */
	boolean isSnapshotStale(long lastIncludedIndex) {
		return lastIncludedIndex < Math.max(commitIndex, lastApplied);
	}

	long endReceiveInstallSnapshot(ReceiveSnapshotting.Entry entry, InstallSnapshot r) throws Exception {
		logsAvailable = false; // 暂停并等待后台日志裁剪结束
		var removeLogBeforeFuture = this.removeLogBeforeFuture;
		if (removeLogBeforeFuture != null)
			removeLogBeforeFuture.await();
		raft.lock();
		try {
			try {
				if (raft.isShutdown) {
					ReceiveSnapshotting.tryDelete(entry.path, "endReceiveInstallSnapshot");
					return 0;
				}
				// 锁外等待期间 term 可能变化，重新持锁后若任期不一致则放弃本次安装并清理临时文件。
				if (r.Argument.getTerm() != term) {
					logger.warn("{} InstallSnapshot stale: rpcTerm={} != currentTerm={}, leaderId={},"
									+ " LastIncludedIndex={}, term changed while waiting outside raft lock;"
									+ " discard received snapshot.",
							raft.getName(), r.Argument.getTerm(), term, r.Argument.getLeaderId(),
							r.Argument.getLastIncludedIndex());
					ReceiveSnapshotting.tryDelete(entry.path, "endReceiveInstallSnapshot");
					r.Result.setTerm(term);
					return 0; // 带回当前 term 让旧 leader 退位或重试
				}
				// 接收期间 AppendEntries 可能已推进提交或应用状态，旧快照直接忽略。
				if (isSnapshotStale(r.Argument.getLastIncludedIndex())) {
					ReceiveSnapshotting.tryDelete(entry.path, "stale snapshot");
					return 0;
				}
				// 兜底拦截与本地快照并发操作状态机目录的冲突，应答冲突码让 leader 下次心跳重试。
				if (getSnapshotting()) {
					logger.warn("{} InstallSnapshot LastIncludedIndex={} conflicts with local snapshotting;"
									+ " discard received snapshot and reply SnapshottingConflict.",
							raft.getName(), r.Argument.getLastIncludedIndex());
					ReceiveSnapshotting.tryDelete(entry.path, "endReceiveInstallSnapshot");
					return InstallSnapshot.ResultCodeSnapshottingConflict;
				}
				// 复核条目所有权与 finalizing 状态，防止归属已失效的安装继续重置本地状态。
				if (raft.receiveSnapshotting.get(r.Argument.getLastIncludedIndex()) != entry
						|| !entry.finalizing) {
					logger.warn("{} InstallSnapshot finalize ownership broken: LastIncludedIndex={},"
									+ " discard file and reply FinalizingConflict.",
							raft.getName(), r.Argument.getLastIncludedIndex());
					ReceiveSnapshotting.tryDelete(entry.path, "endReceiveInstallSnapshot");
					return InstallSnapshot.ResultCodeFinalizingConflict;
				}
				// 复核文件尺寸与 done 块记录一致，防止损坏或不完整的快照文件被提交。
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
				// 本地已存在同 index 且同 term 的日志：保留后续日志并按快照边界重置状态机。
				var last = readLog(r.Argument.getLastIncludedIndex());
				if (last != null && last.getTerm() == r.Argument.getLastIncludedTerm()) {
					logger.warn("Exist Local Log. Do It Like A Local Snapshot!");
					commitSnapshotNow(entry.path, r.Argument.getLastIncludedIndex());
					// 同边界重装时同步复位内存索引并装载快照，补全上次收尾中途失败后的状态恢复。
					lastIndex = Math.max(lastIndex, r.Argument.getLastIncludedIndex());
					commitIndex = firstIndex;
					lastApplied = firstIndex;
					setVoteFor(Objects.requireNonNull(raft.getLeaderId()));
					long t = System.nanoTime();
					try {
						raft.getStateMachine().loadSnapshot(getCommittedSnapshotFile());
					} catch (Throwable e) {
						// 状态机装载失败无原地恢复路径，直接 fatalKill，重启后从已提交快照自愈。
						logger.fatal("{} EndReceiveInstallSnapshot(ExistLog) loadSnapshot failed, fatalKill. Path={}",
								raft.getName(), entry.path, e);
						raft.fatalKill();
						throw Task.forceThrow(e);
					}
					logger.info("{} EndReceiveInstallSnapshot(ExistLog) Path={} time={}ms",
							raft.getName(), entry.path, (System.nanoTime() - t) / 1_000_000);
					// 快照是多数派提交产物，装载成功即持有已提交状态。
					trySetNodeReady();
					return 0;
				}
				// 丢弃全部旧日志并写入随最后一块传来的边界日志，确保后续 AppendEntries 可匹配 prevLog。
				logger.info("endReceiveInstallSnapshot: close logs: {}", raft.getRaftConfig().getDbHome());
				cancelPendingAppendLogFutures();
				// drop 日志前收集未应用请求的 unique 键，随后与边界日志同批清理其未应用存根，避免重试被误判 DuplicateRequest。
				var uncommittedUniqueRequests = collectUncommittedUniqueRequests();
				//noinspection DataFlowIssue
				logs.drop();
				//noinspection DataFlowIssue
				logs = database.getOrAddTable(raft.getName() + ".logs");
				var lastIncludedLog = RaftLog.decode(r.Argument.getLastIncludedLog(),
						raft.getStateMachine()::logFactory);
				//noinspection DataFlowIssue
				try (var batch = database.borrowBatch()) {
					saveLog(lastIncludedLog, batch);
					for (var stub : uncommittedUniqueRequests)
						openUniqueRequests(stub.getKey()).removeIfNotApplied(stub.getValue(), batch);
					batch.commit(writeOptions);
				}
				commitSnapshotNow(entry.path, lastIncludedLog.getIndex());

				lastIndex = lastIncludedLog.getIndex();
				commitIndex = firstIndex;
				lastApplied = firstIndex;

				// 放弃当前 Term 的投票。
				setVoteFor(Objects.requireNonNull(raft.getLeaderId()));

				// 从已提交快照重建状态机
				long t = System.nanoTime();
				try {
					raft.getStateMachine().loadSnapshot(getCommittedSnapshotFile());
				} catch (Throwable e) {
					// 日志已清空且 firstIndex 已推进，装载失败无原地恢复路径，fatalKill 由重启自愈。
					logger.fatal("{} EndReceiveInstallSnapshot loadSnapshot failed, fatalKill. Path={}",
							raft.getName(), entry.path, e);
					raft.fatalKill();
					throw Task.forceThrow(e);
				}
				logger.info("{} EndReceiveInstallSnapshot Path={} time={}ms",
						raft.getName(), entry.path, (System.nanoTime() - t) / 1_000_000);
				// 快照是多数派提交产物，装载成功即持有已提交状态。
				trySetNodeReady();
				return 0;
			} finally {
				logsAvailable = true;
			}
		} finally {
			// 在 Raft 锁内按同一性摘除 finalizing 条目，解除对后续同边界安装的占位。
			raft.receiveSnapshotting.removeIdentity(r.Argument.getLastIncludedIndex(), entry);
			raft.unlock();
		}
	}

	private ArrayList<Map.Entry<Long, UniqueRequestId>> collectUncommittedUniqueRequests() throws RocksDBException {
		var uncommitted = new ArrayList<Map.Entry<Long, UniqueRequestId>>();
		var startKey = ByteBuffer.Allocate(9);
		startKey.WriteLong(lastApplied + 1);
		//noinspection DataFlowIssue
		try (var it = logs.iterator()) {
			for (it.seek(startKey.CopyIf()); RocksDatabase.isValidOrThrow(it); it.next()) {
				var index = ByteBuffer.Wrap(it.key()).ReadLong();
				if (index <= lastApplied)
					continue;
				try {
					var raftLog = RaftLog.decode(new Binary(it.value()), raft.getStateMachine()::logFactory);
					var log = raftLog.getLog();
					if (log != null && log.getUnique().getRequestId() > 0)
						uncommitted.add(Map.entry(log.getCreateTime(), log.getUnique()));
				} catch (Throwable e) {
					logger.warn("{} collectUncommittedUniqueRequests decode log({}) failed, skip.",
							raft.getName(), index, e);
				}
			}
		}
		return uncommitted;
	}

	public void snapshot() throws Exception {
		raft.lock();
		try {
			// 正在发送或接收 InstallSnapshot 时跳过本地快照，避免与状态机重置冲突。
			if (raft.isShutdown || getSnapshotting() || !sendSnapshotting.isEmpty() || raft.isReceivingSnapshot())
				return;

			setSnapshotting(true);
		} finally {
			raft.unlock();
		}
		try {
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
		var r = (AppendEntries)p;
		raft.lock();
		try {
			if (raft.isShutdown)
				return Procedure.Success;
			// 即使请求已经过期，真实应答携带的更高任期仍必须使本节点退位。
			if (!r.isTimeout() && trySetTerm(r.Result.getTerm()) == SetTermResult.Newer) {
				raft.convertStateTo(Raft.RaftState.Follower);
				if (connector.getPending() == r)
					connector.setPending(null);
				return Procedure.Success;
			}

			// 只有当前任期、当前发送槽的应答能更新复制进度；旧 ACK 的同索引日志可能已被覆盖。
			if (connector.getPending() != r)
				return Procedure.Success;
			if (!raft.isLeader() || r.Argument.getTerm() != term) {
				connector.setPending(null);
				return Procedure.Success;
			}
			if (r.isTimeout()) {
				trySendAppendEntries(connector, r); // 超时重发
				return Procedure.Success;
			}

			if (r.Result.getSuccess()) {
				tryCommit(r, connector);
				// 推进 nextIndex 后尝试继续复制剩余日志
				trySendAppendEntries(connector, r);
				return Procedure.Success;
			}

			// 日志同步失败，调整 nextIndex 后重试。
			if (r.Result.getNextIndex() == 0)
				connector.setNextIndex(Math.max(firstIndex, connector.getNextIndex() - 1)); // 默认逐条回退并钳位到 firstIndex
			else if (r.Result.getNextIndex() <= firstIndex) {
				// leader 已快照，follower 落后到 firstIndex，后续将转入 InstallSnapshot。
				connector.setNextIndex(firstIndex);
			} else if (r.Result.getNextIndex() >= lastIndex) {
				logger.fatal("Impossible r.Result.NextIndex({}) >= LastIndex({}) there must be a bug.",
						r.Result.getNextIndex(), lastIndex, new Exception());
				raft.fatalKill();
			} else
				connector.setNextIndex(r.Result.getNextIndex()); // 快速定位
			trySendAppendEntries(connector, r);
			return Procedure.Success;
		} finally {
			// 应答已消费；处理失败时也释放该请求占用的槽位，允许心跳重试。
			if (connector.getPending() == r)
				connector.setPending(null);
			raft.unlock();
		}
	}

	void trySendAppendEntries(Server.ConnectorEx connector, @Nullable AppendEntries pending) throws Exception {
		connector.setAppendLogActiveTime(System.currentTimeMillis());
		if (connector.getPending() != pending)
			return;
		// 先释放槽位，后续各提前返回分支无需重复清理。
		connector.setPending(null);

		if (!raft.isLeader())
			return;

		// 正在安装快照时跳过日志复制
		if (sendSnapshotting.contains(connector.getName()))
			return;

		var socket = connector.TryGetReadySocket();
		if (socket == null)
			return;

		if (connector.getNextIndex() > lastIndex)
			return; // 日志已复制完毕

		if (connector.getNextIndex() <= firstIndex) {
			// 落后到 firstIndex（无 prevLog 可匹配），转入快照安装。
			connector.setNextIndex(firstIndex);
			sendSnapshotting.start(connector);
			return;
		}

		var nextLog = readLog(connector.getNextIndex());
		if (nextLog == null)
			return;
		var prevLog = readLog(nextLog.getIndex() - 1);
		if (prevLog == null)
			return;

		var request = new AppendEntries();
		request.Argument.setTerm(term);
		request.Argument.setLeaderId(raft.getName());
		request.Argument.setLeaderCommit(commitIndex);

		request.Argument.setPrevLogIndex(prevLog.getIndex());
		request.Argument.setPrevLogTerm(prevLog.getTerm());

		// 限制单次复制打包的日志条数
		int maxCount = raft.getRaftConfig().getMaxAppendEntriesCount();
		RaftLog lastCopyLog = nextLog;
		for (var copyLog = nextLog;
			 maxCount > 0 && copyLog != null && copyLog.getIndex() <= lastIndex;
			 copyLog = readLog(copyLog.getIndex() + 1), --maxCount) {
			lastCopyLog = copyLog;
			request.Argument.getEntries().add(new Binary(copyLog.encode()));
		}
		request.Argument.setLastEntryIndex(lastCopyLog.getIndex());
		// 只有发送成功的请求才占用单飞槽位。
		if (request.Send(socket, p -> processAppendEntriesResult(connector, p),
				raft.getRaftConfig().getAppendEntriesTimeout()))
			connector.setPending(request);
	}

	public RaftLog lastRaftLogTermIndex() throws RocksDBException {
		return RaftLog.decodeTermIndex(Objects.requireNonNull(readLogBytes(lastIndex)));
	}

	private void cancelLeaderAppendLogs(long startIndex, long endIndex) throws Exception {
		for (long index = startIndex; index <= endIndex; index++) {
			RaftLog raftLog;
			if (index > lastApplied && (raftLog = leaderAppendLogs.remove(index)) != null) {
				// 取消被截断的未应用日志对应的 leader 回调。
				raftLog.cancelCallback();
			}
		}
	}

	private void removeLog(long index, RocksDatabase.Batch batch) throws RocksDBException {
		var raftLog = readLog(index);
		if (raftLog != null) {
			var key = ByteBuffer.Allocate(9);
			key.WriteLong(index);
			//noinspection DataFlowIssue
			logs.delete(batch, key.Bytes, 0, key.WriteIndex);
			//noinspection DataFlowIssue
			if (raftLog.getLog().getUnique().getRequestId() > 0)
				openUniqueRequests(raftLog.getLog().getCreateTime()).remove(raftLog, batch);
		}
	}

	long followerOnAppendEntries(AppendEntries r) throws Exception {
		r.Result.setTerm(term);
		r.Result.setSuccess(false);

		if (r.Argument.getTerm() < term || r.Argument.getTerm() > TERM_MAX) {
			// 过期或超出 TERM_MAX 的 term 直接拒绝，且不重置选举计时。
			r.SendResult();
			logger.info("this={} Leader={} PrevLogIndex={} invalid term({})",
					raft.getName(), r.Argument.getLeaderId(), r.Argument.getPrevLogIndex(), r.Argument.getTerm());
			return Procedure.Success;
		}

		// 仅对有效 Leader 的消息重置选举计时。
		setLeaderActiveTime(System.currentTimeMillis());

		switch (trySetTerm(r.Argument.getTerm())) {
		case Newer:
			raft.convertStateTo(Raft.RaftState.Follower);
			r.Result.setTerm(term);
			break;

		case Same:
			switch (raft.getState()) {
			case Candidate:
				// 同 term 已存在合法 Leader，Candidate 退回 Follower。
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

		// 心跳（空 entries）统一走 prevLog 校验与 commitIndex 推进。
		var prevLog = readLog(r.Argument.getPrevLogIndex());
		if (prevLog == null || prevLog.getTerm() != r.Argument.getPrevLogTerm()) {
			// prevLog 不匹配：返回快速定位的 nextIndex。
			r.Result.setNextIndex(r.Argument.getPrevLogIndex() > lastIndex ? lastIndex + 1 : 0);

			r.SendResult();
			if (isDebugEnabled)
				logger.debug("this={} Leader={} Index={} prevLog mismatch",
						raft.getName(), r.Argument.getLeaderId(), r.Argument.getPrevLogIndex());
			return Procedure.Success;
		}

		// 观察到 LeaderCommit 推进即说明集群已达成多数派，标记本节点就绪。
		if (lastLeaderCommitIndex == 0) {
			lastLeaderCommitIndex = r.Argument.getLeaderCommit();
		} else if (r.Argument.getLeaderCommit() > lastLeaderCommitIndex) {
			trySetNodeReady();
		}

		int entryIndex = 0;
		var copyLogIndex = prevLog.getIndex() + 1;
		long truncateStart = -1;
		for (; entryIndex < r.Argument.getEntries().size(); ++entryIndex, ++copyLogIndex) {
			RaftLog copyLog;
			try {
				copyLog = RaftLog.decode(r.Argument.getEntries().get(entryIndex), raft.getStateMachine()::logFactory);
			} catch (Throwable e) {
				// 无法解码的日志属永久错误，直接 fatalKill 避免节点对复制永久无应答。
				fatalKillDecodeError("followerOnAppendEntries.copyLog", copyLogIndex, e);
				return 0;
			}
			if (copyLog.getIndex() != copyLogIndex) {
				logger.fatal("copyLog.Index({}) != copyLogIndex({}) Leader={} this={}",
						copyLog.getIndex(), copyLogIndex, r.Argument.getLeaderId(), raft.getName(), new Exception());
				raft.fatalKill();
			}
			if (copyLog.getIndex() < firstIndex)
				continue; // 快照以前的日志忽略

			if (copyLog.getIndex() <= lastIndex) {
				var conflictCheck = readLog(copyLog.getIndex());
				if (conflictCheck != null && conflictCheck.getTerm() == copyLog.getTerm())
					continue;

				// 冲突条目（同 index 不同 term）或存在空洞：截断该位置及后续所有日志。
				if (copyLog.getIndex() <= commitIndex) {
					logger.fatal("{} truncate committed entries: {} <= {}", raft.getName(),
							copyLog.getIndex(), commitIndex, new Exception());
					raft.fatalKill();
				}
				truncateStart = copyLog.getIndex();
			}
			break;
		}
		// 冲突截断（删旧日志与对应 unique 存根）与新条目追加合入同一 WriteBatch 原子提交，
		// 提交成功后更新内存 lastIndex 并取消旧 leaderAppendLogs 回调。
		if (truncateStart >= 0 || entryIndex < r.Argument.getEntries().size()) {
			long appendIndex = copyLogIndex;
			//noinspection DataFlowIssue
			try (var batch = database.borrowBatch()) {
				if (truncateStart >= 0) {
					for (long index = truncateStart; index <= lastIndex; index++)
						removeLog(index, batch);
				}
				for (int i = entryIndex; i < r.Argument.getEntries().size(); ++i, ++appendIndex)
					saveLogRaw(appendIndex, r.Argument.getEntries().get(i), batch);
				batch.commit(writeOptions);
			}
			if (truncateStart >= 0) {
				cancelLeaderAppendLogs(truncateStart, lastIndex);
				lastIndex = truncateStart - 1;
			}
			copyLogIndex = appendIndex;
		}

		copyLogIndex--;
		// 仅单调推进 lastIndex，防止旧批次覆盖。
		if (copyLogIndex > lastIndex)
			lastIndex = copyLogIndex;

		// commitIndex 仅按本次已验证匹配的尾索引推进（防本地旧分叉后缀误提交），且单调递增；
		// 若存在未应用日志（如上次 flush 失败）也借机重试 apply。
		if (r.Argument.getLeaderCommit() > commitIndex || commitIndex > lastApplied) {
			if (r.Argument.getLeaderCommit() > commitIndex) {
				var newCommitIndex = Math.min(r.Argument.getLeaderCommit(), copyLogIndex);
				if (newCommitIndex > commitIndex) {
					commitIndex = newCommitIndex;
					// commitIndex 推进即持有多数派已提交数据，标记节点就绪（覆盖空闲集群 leaderCommit 恒定的场景）。
					trySetNodeReady();
				}
			}
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
