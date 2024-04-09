package Zeze.Raft;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action2;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
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

	private final Raft raft;
	private long term;
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
	private RocksDB logs;
	private RocksDatabase database;
	private RocksDatabase.Table rafts;
	private final LongConcurrentHashMap<UniqueRequestSet> uniqueRequestSets = new LongConcurrentHashMap<>();
	private final SimpleDateFormat uniqueDateFormat = new SimpleDateFormat("yyyy.M.d");

	private final byte[] raftsTermKey;
	private final byte[] raftsVoteForKey;
	private final byte[] raftsFirstIndexKey;
	private final byte[] raftsNodeReadyKey; // 只会被写一次，所以这个优化可以不做，统一形式吧。
	private final byte[] lastSnapshotIndexKey;

	public volatile TaskCompletionSource<Boolean> applyFuture; // follower background apply task
	private final LongConcurrentHashMap<RaftLog> leaderAppendLogs = new LongConcurrentHashMap<>();

	// 是否有安装进程正在进行中，用来阻止新的创建请求。
	private final ConcurrentHashMap<String, Server.ConnectorEx> installSnapshotting = new ConcurrentHashMap<>();
	private long lastSnapshotIndex;
	private boolean snapshotting = false; // 是否正在创建Snapshot过程中，用来阻止新的创建请求。

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
		if (raft.getRaftConfig().isSnapshotCommitDelayed()) {
			// 查找目录下已经存在的延时提交的snapshot。
			// 实际上最多只会存在一个延时提交的snapshot，这里的代码写法能处理多个。
			var files = new File(path).getParentFile().listFiles();
			var delayed = new TreeMap<Long, File>();
			if (null != files) {
				for (var file : files) {
					if (!file.isFile())
						continue;

					var fileName = file.getName();
					if (fileName.endsWith(".commit.delayed")) {
						var splits = fileName.split("\\.");
						try {
							var index = Long.parseLong(splits[splits.length - 3]);
							delayed.put(index, file);
						} catch (Exception ex) {
							// skip
						}
					}
				}
			}
			if (!delayed.isEmpty()) {
				// 删除最后一个entry，并且提交。
				var biggestIndex = delayed.lastKey();
				var biggestFile = delayed.remove(biggestIndex);
				// 里面会把这个rename成真正的snapshot。
				_commitSnapshot(biggestFile.toString(), biggestIndex);
				// 删除多余的延时提交文件。一般不会发生。
				for (var file : delayed.values())
					Files.deleteIfExists(file.toPath());
			}
			// 当前snapshot重命名，带上index信息。等到下一个snapshot发生的时候推进。
			Files.move(Paths.get(path), Paths.get(path + "." + newFirstIndex + ".commit.delayed"));
			return;
		}
		_commitSnapshot(path, newFirstIndex);
	}

	private void _commitSnapshot(String path, long newFirstIndex) throws IOException, RocksDBException {
		raft.lock();
		try {
			Files.move(Paths.get(path), Paths.get(getSnapshotFullName()), StandardCopyOption.REPLACE_EXISTING);
			saveFirstIndex(newFirstIndex);
			startRemoveLogOnlyBefore(newFirstIndex);
		} finally {
			raft.unlock();
		}
	}

	private RocksIterator newLogsIterator() {
		raft.lock();
		try {
			return logs.newIterator(RocksDatabase.getDefaultReadOptions());
		} finally {
			raft.unlock();
		}
	}

	private void startRemoveLogOnlyBefore(long index) {
		raft.lock();
		try {
			if (removeLogBeforeFuture != null || !logsAvailable || raft.isShutdown)
				return;
			removeLogBeforeFuture = new TaskCompletionSource<>();
		} finally {
			raft.unlock();
		}

		// 直接对 RocksDb 多线程访问，这里就不做多线程保护了。
		Task.run(() -> {
			try {
				try (var it = newLogsIterator()) {
					it.seekToFirst();
					while (logsAvailable && !raft.isShutdown && it.isValid()) {
						// 这里只需要log的Index，直接从key里面获取了。
						if (ByteBuffer.Wrap(it.key()).ReadLong() >= index) {
							removeLogBeforeFuture.setResult(true);
							return;
						}

						var key = it.key();
						logs.delete(writeOptions, key);

						// 删除快照前的日志时，不删除唯一请求存根，否则快照建立时刻前面一点时间的请求无法保证唯一。
						// 唯一请求存根自己管理删除，
						// 【注意】
						// 服务器完全奔溃（数据全部丢失）后，重新配置一台新的服务器，仍然又很小的机会存在无法判断唯一。
						// 此时比较好的做法时，从工作节点的数据库(unique/)复制出一份，作为开始数据。
						// 参考 RemoveLogAndCancelStart

						//if (raftLog.Log.Unique.RequestId > 0)
						//    OpenUniqueRequests(raftLog.Log.CreateTime).Remove(raftLog);
						it.next();
					}
				}
			} finally {
				removeLogBeforeFuture.setResult(false);
				removeLogBeforeFuture = null;
			}
		}, "RemoveLogBefore" + index, DispatchMode.Normal);
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

	void setLeaderActiveTime(long value) {
		leaderActiveTime = value;
	}

	final class UniqueRequestSet {
		private final RocksDatabase.Table table;

		public UniqueRequestSet(String tableName) {
			try {
				table = database.getOrAddTable(tableName);
			} catch (RocksDBException e) {
				Task.forceThrow(e);
				throw new AssertionError(); // never run here
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
			}
		}
		throw new IllegalStateException("delete '" + path + "' failed");
	}

	public static void deletedDirectoryAndCheck(File path) {
		deletedDirectoryAndCheck(path, 10);
	}

	void removeExpiredUniqueRequestSet() throws ParseException, RocksDBException {
		RaftConfig raftConf = raft.getRaftConfig();
		long expired = System.currentTimeMillis() - (raftConf.getUniqueRequestExpiredDays() + 1) * 86400_000L;

		for (var tableName : database.getTableMap().keySet()) {
			if (!tableName.startsWith("unique."))
				continue;
			var db = uniqueDateFormat.parse(tableName.substring("unique.".length()));
			if (db.getTime() < expired) {
				var opened = uniqueRequestSets.remove(toUniqueRequestKey(db));
				if (null != opened)
					opened.table.drop(); // 包含opened.close。
				else
					database.getOrAddTable(tableName).drop();
			}
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
				logger.info("closeDb: {}, logs", raft.getRaftConfig().getDbHome());
				logs.close();
				logs = null;
			}

			rafts = null;

			if (database != null) {
				logger.info("closeDb: {}, rafts", raft.getRaftConfig().getDbHome());
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
		this.raft = raft;

		database = new RocksDatabase(Paths.get(raft.getRaftConfig().getDbHome(), "rafts").toString());
		rafts = database.getOrAddTable("rafts");
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

		logs = RocksDatabase.open(Path.of(raft.getRaftConfig().getDbHome(), "logs").toString());
		{
			// Read Last Log Index
			try (var itLast = logs.newIterator(RocksDatabase.getDefaultReadOptions())) {
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

				// 【注意】snapshot 以后 FirstIndex 会推进，不再是从-1开始。
				if (firstIndex == -1) { // no committed snapshot
					try (var itFirst = logs.newIterator(RocksDatabase.getDefaultReadOptions())) {
						itFirst.seekToFirst();
						if (itFirst.isValid()) {
							firstIndex = RaftLog.decode(new Binary(itFirst.value()),
									raft.getStateMachine()::logFactory).getIndex();
						}
					}
				}
				lastApplied = firstIndex;
				commitIndex = firstIndex;
			}
		}
		logsAvailable = true;

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
	 * 1. return null 表示RaftExpired，这个错误不可忽略。
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
		if ((now - create) / 86400_000 >= raft.getRaftConfig().getUniqueRequestExpiredDays())
			return null;

		UniqueRequestState state = openUniqueRequests(raftRpc.getCreateTime()).getRequestState(raftRpc);
		return state != null ? state : UniqueRequestState.NOT_FOUND;
	}

	private UniqueRequestSet openUniqueRequests(long time) {
		return uniqueRequestSets.computeIfAbsent(toUniqueRequestKey(new Date(time)),
				k -> new UniqueRequestSet("unique." + (k >> 16) + '.' + ((k >> 8) & 0xff) + '.' + (k & 0xff)));
	}

	@SuppressWarnings("deprecation")
	private static long toUniqueRequestKey(Date date) {
		return ((date.getYear() + 1900L) << 16) + ((date.getMonth() + 1) << 8) + date.getDate();
	}

	public WriteOptions getWriteOptions() {
		return writeOptions;
	}

	public void setWriteOptions(WriteOptions value) {
		writeOptions = value;
	}

	private void saveLog(RaftLog log) throws RocksDBException {
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
		RocksDB logs = this.logs;
		return logs != null ? logs.get(RocksDatabase.getDefaultReadOptions(), key.Bytes, 0, key.WriteIndex) : null;
	}

	private RaftLog readLog(long index) throws RocksDBException {
		var value = readLogBytes(index);
		return value != null ? RaftLog.decode(new Binary(value), raft.getStateMachine()::logFactory) : null;
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
	public SetTermResult trySetTerm(long term) throws RocksDBException {
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

		// 旧的 AppendEntries 的结果，不用继续处理了。
		// 【注意】这个不是必要的，是一个小优化。
		if (rpc.Argument.getLastEntryIndex() <= commitIndex)
			return;

		// find MaxMajorityLogIndex
		// Rules for Servers
		// If there exists an N such that N > commitIndex, a majority
		// of matchIndex[i] ≥ N, and log[N].term == currentTerm:
		// set commitIndex = N(§5.3, §5.4).
		var followers = new ArrayList<Server.ConnectorEx>();
		raft.getServer().getConfig().forEachConnector(c ->
				followers.add(c instanceof Server.ConnectorEx ? (Server.ConnectorEx)c : null));
		followers.sort((a, b) -> Long.compare(b.getMatchIndex(), a.getMatchIndex()));
		var maxMajorityLogIndex = followers.get(raft.getRaftConfig().getHalfCount() - 1).getMatchIndex();
		if (maxMajorityLogIndex > commitIndex) {
			var maxMajorityLog = readLog(maxMajorityLogIndex);
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
					applyFuture.setResult(Task.call(this::backgroundApply, "BackgroundApply") == 0); // 如果有人等待。
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
				var lastApplicableLog = readLog(commitIndex);
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

	private void tryApply(RaftLog lastApplicableLog, long count) throws Exception {
		if (lastApplicableLog == null) {
			logger.error("lastApplicableLog is null.");
			return;
		}
		for (long index = lastApplied + 1; index <= lastApplicableLog.getIndex() && count > 0; --count) {
			RaftLog raftLog = leaderAppendLogs.remove(index);
			if (raftLog == null && (raftLog = readLog(index)) == null) {
				logger.warn("What Happened! index={} lastApplicableLog={} LastApplied={}",
						index, lastApplicableLog.getIndex(), lastApplied);
				// trySnapshot(); // 错误的时候不做这个尝试了。
				return; // end?
			}

			index = raftLog.getIndex() + 1;
			raftLog.getLog().apply(raftLog, raft.getStateMachine());
			if (raftLog.getLog().getUnique().getRequestId() > 0)
				openUniqueRequests(raftLog.getLog().getCreateTime()).apply(raftLog);
			lastApplied = raftLog.getIndex(); // 循环可能退出，在这里修改。
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
				Task.run(this::snapshot, "Snapshot", DispatchMode.Normal);
			}
		}
		// else disable
	}

	public long getTestStateMachineCount() {
		StateMachine stateMachine = raft.getStateMachine();
		return stateMachine instanceof Test.TestStateMachine ? ((Test.TestStateMachine)stateMachine).getCount() : -1;
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

			if (getInstallSnapshotting().containsKey(connector.getName()))
				return;

			var socket = connector.TryGetReadySocket();
			if (socket == null)
				return; // Heartbeat Will Retry

			var heartbeat = new AppendEntries();
			heartbeat.Argument.setTerm(term);
			heartbeat.Argument.setLeaderId(raft.getName());
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
			throw new RaftRetryException("timeout or canceled");
		}
		return result;
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
			// 广播给followers并异步等待多数确认
			try {
				raft.getServer().getConfig().ForEachConnector(c -> trySendAppendEntries((Server.ConnectorEx)c, null));
			} catch (Throwable e) { // rollback. 必须捕捉所有异常。rethrow
				lastIndex--;
				// 只有下面这个需要回滚，日志(SaveLog, OpenUniqueRequests(...).Save)以后根据LastIndex覆盖。
				if (null != callback)
					leaderAppendLogs.remove(raftLog.getIndex());
				throw e;
			}
			var result = new AppendLogResult();
			result.term = term;
			result.index = lastIndex;
			return result;
		} finally {
			raft.unlock();
		}
	}

	private boolean getSnapshotting() {
		return snapshotting;
	}

	private void setSnapshotting(boolean value) {
		snapshotting = value;
	}

	public ConcurrentHashMap<String, Server.ConnectorEx> getInstallSnapshotting() {
		return installSnapshotting;
	}

	public String getSnapshotFullName() {
		return Paths.get(raft.getRaftConfig().getDbHome(), snapshotFileName).toString();
	}

	void endReceiveInstallSnapshot(String path, InstallSnapshot r) throws Exception {
		logsAvailable = false; // cancel RemoveLogBefore
		var removeLogBeforeFuture = this.removeLogBeforeFuture;
		if (removeLogBeforeFuture != null)
			removeLogBeforeFuture.await();
		raft.lock();
		try {
			try {
				// 6. If existing log entry has same index and term as snapshot's
				// last included entry, retain log entries following it and reply
				var last = readLog(r.Argument.getLastIncludedIndex());
				if (null != last && last.getTerm() == r.Argument.getLastIncludedTerm()) {
					// 【注意】没有错误处理：比如LastIncludedIndex是否超过CommitIndex之类的。
					// 按照现在启动InstallSnapshot的逻辑，不会发生这种情况。
					logger.warn("Exist Local Log. Do It Like A Local Snapshot!");
					commitSnapshot(path, r.Argument.getLastIncludedIndex());
					return;
				}
				// 7. Discard the entire log
				// 整个删除，那么下一次AppendEntries又会找不到prev。不就xxx了吗?
				// 我的想法是，InstallSnapshot 最后一个 trunk 带上 LastIncludedLog，
				// 接收者清除log，并把这条日志插入（这个和系统初始化时插入的Index=0的日志道理差不多）。
				// 【除了快照最后包含的日志，其他都删除。】
				logger.info("closeDb: {}, logs", raft.getRaftConfig().getDbHome());
				logs.close();
				logs = null;
				cancelPendingAppendLogFutures();
				var logsDir = Paths.get(raft.getRaftConfig().getDbHome(), "logs").toString();
				deletedDirectoryAndCheck(new File(logsDir), 10000);
				logs = RocksDatabase.open(logsDir);
				var lastIncludedLog = RaftLog.decode(r.Argument.getLastIncludedLog(),
						raft.getStateMachine()::logFactory);
				saveLog(lastIncludedLog);
				commitSnapshot(path, lastIncludedLog.getIndex());

				lastIndex = lastIncludedLog.getIndex();
				commitIndex = firstIndex;
				lastApplied = firstIndex;

				// 【关键】记录这个，放弃当前Term的投票。
				setVoteFor(raft.getLeaderId());

				// 8. Reset state machine using snapshot contents (and load
				// snapshot's cluster configuration)
				long t = System.nanoTime();
				raft.getStateMachine().loadSnapshot(getSnapshotFullName());
				logger.info("{} EndReceiveInstallSnapshot Path={} time={}ms",
						raft.getName(), path, (System.nanoTime() - t) / 1_000_000);
			} finally {
				logsAvailable = true;
			}
		} finally {
			raft.unlock();
		}
	}

	public void snapshot() throws Exception {
		raft.lock();
		try {
			if (getSnapshotting() || !getInstallSnapshotting().isEmpty())
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

	public void cancelAllInstallSnapshot() throws Exception {
		for (var installing : getInstallSnapshotting().values())
			endInstallSnapshot(installing);
	}

	public void endInstallSnapshot(Server.ConnectorEx c) throws Exception {
		var cex = getInstallSnapshotting().remove(c.getName());
		if (cex != null) {
			var state = cex.getInstallSnapshotState();
			logger.info("{} InstallSnapshot LastIncludedIndex={} Done={} c={}", raft.getName(),
					state.getPending().Argument.getLastIncludedIndex(),
					state.getPending().Argument.getDone(), c.getName());
			state.getFile().close();
			if (state.getPending().Argument.getDone() && state.getPending().getResultCode() == 0) {
				cex.setNextIndex(state.getPending().Argument.getLastIncludedIndex() + 1);

				if (state.getPending().Argument.getLastIncludedIndex() > cex.getMatchIndex()) // see EndReceiveInstallSnapshot 6.
					cex.setMatchIndex(state.getPending().Argument.getLastIncludedIndex());
				// start log copy
				trySendAppendEntries(c, null);
			}
		}
		c.setInstallSnapshotState(null);
	}

	private void startInstallSnapshot(Server.ConnectorEx c) throws Exception {
		if (getInstallSnapshotting().containsKey(c.getName()))
			return;
		var path = getSnapshotFullName();
		// 如果 Snapshotting，此时不启动安装。
		// 以后重试 AppendEntries 时会重新尝试 Install.
		if ((new File(path)).isFile() && !getSnapshotting()) {
			if (getInstallSnapshotting().putIfAbsent(c.getName(), c) != null)
				throw new IllegalStateException("Impossible");

			c.setInstallSnapshotState(new InstallSnapshotState());
			var st = c.getInstallSnapshotState();
			st.setFile(new RandomAccessFile(path, "r"));
			st.setFirstLog(readLog(firstIndex));
			st.getPending().Argument.setTerm(term);
			st.getPending().Argument.setLeaderId(raft.getName());
			st.getPending().Argument.setLastIncludedIndex(st.getFirstLog().getIndex());
			st.getPending().Argument.setLastIncludedTerm(st.getFirstLog().getTerm());

			logger.info("{} InstallSnapshot Start... Path={} c={}", raft.getName(), path, c.getName());
			st.trySend(this, c);
		} else {
			// 这一般的情况是snapshot文件被删除了。
			// 【注意】这种情况也许报错更好？
			// 内部会判断，不会启动多个snapshot。
			snapshot();
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
		if (getInstallSnapshotting().containsKey(connector.getName()))
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
			startInstallSnapshot(connector);
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
		setLeaderActiveTime(System.currentTimeMillis());
		r.Result.setTerm(term); // maybe rewrite later
		r.Result.setSuccess(false); // set default false

		if (r.Argument.getTerm() < term) {
			// 1. Reply false if term < currentTerm (§5.1)
			r.SendResult();
			logger.info("this={} Leader={} PrevLogIndex={} term < currentTerm",
					raft.getName(), r.Argument.getLeaderId(), r.Argument.getPrevLogIndex());
			return Procedure.Success;
		}

		switch (trySetTerm(r.Argument.getTerm())) {
		case Newer:
			raft.convertStateTo(Raft.RaftState.Follower);
			r.Result.setTerm(term); // new term
			break;

		case Same:
			switch (raft.getState()) {
			case Candidate:
				// see raft.pdf 文档. 仅在 Candidate 才转。【找不到在文档哪里了，需要确认这点】
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

		// is Heartbeat(KeepAlive)
		if (r.Argument.getEntries().isEmpty()) {
			r.Result.setSuccess(true);
			r.SendResult();
			if (null != raft.onFollowerReceiveKeepAlive)
				raft.onFollowerReceiveKeepAlive.run();
			return Procedure.Success;
		}

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

		// NodeReady 严格点，仅在正常复制时才检测。
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
			var copyLog = RaftLog.decode(r.Argument.getEntries().get(entryIndex), raft.getStateMachine()::logFactory);
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
		if (r.Argument.getLeaderCommit() > commitIndex) {
			commitIndex = Math.min(r.Argument.getLeaderCommit(), lastRaftLogTermIndex().getIndex());
			tryStartApplyTask(readLog(commitIndex));
		}
		r.Result.setSuccess(true);
		if (isDebugEnabled)
			logger.debug("{}: {}", raft.getName(), r);
		r.SendResultCode(0);

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
