package Zeze.Raft;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Transaction.Procedure;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteOptions;

public class LogSequence {
	static final Logger logger = LogManager.getLogger(LogSequence.class);
	public static final String SnapshotFileName = "snapshot.dat";

	private final Raft Raft;
	private long Term;
	private long LastIndex;
	private long FirstIndex; // 用来处理NextIndex回溯时限制搜索。snapshot需要修订这个值。
	private long CommitIndex;
	private long LastApplied;

	// 这个不是日志需要的，因为持久化，所以就定义在这里吧。
	private String VoteFor;
	private boolean NodeReady;
	private long LastLeaderCommitIndex;

	public volatile TaskCompletionSource<Boolean> RemoveLogBeforeFuture;
	public volatile boolean LogsAvailable;

	private long LeaderActiveTime = System.currentTimeMillis(); // Leader, Follower

	private WriteOptions WriteOptions = DatabaseRocksDb.getSyncWriteOptions();
	private RocksDB Logs;
	private RocksDB Rafts;
	private final ConcurrentHashMap<String, UniqueRequestSet> UniqueRequestSets = new ConcurrentHashMap<>();

	private final byte[] RaftsTermKey;
	private final byte[] RaftsVoteForKey;
	private final byte[] RaftsFirstIndexKey;
	private final byte[] RaftsNodeReadyKey; // 只会被写一次，所以这个优化可以不做，统一形式吧。

	public volatile TaskCompletionSource<Boolean> ApplyFuture; // follower background apply task
	private final LongConcurrentHashMap<RaftLog> LeaderAppendLogs = new LongConcurrentHashMap<>();

	// 是否有安装进程正在进行中，用来阻止新的创建请求。
	private final ConcurrentHashMap<String, Server.ConnectorEx> InstallSnapshotting = new ConcurrentHashMap<>();
	private long PrevSnapshotIndex;
	private boolean Snapshotting = false; // 是否正在创建Snapshot过程中，用来阻止新的创建请求。

	static {
		RocksDB.loadLibrary();
	}

	public Raft getRaft() {
		return Raft;
	}

	public long getTerm() {
		return Term;
	}

	public long getLastIndex() {
		return LastIndex;
	}

	public long getFirstIndex() {
		return FirstIndex;
	}

	public long getCommitIndex() {
		return CommitIndex;
	}

	public long getLastApplied() {
		return LastApplied;
	}

	String getVoteFor() {
		return VoteFor;
	}

	boolean getNodeReady() {
		return NodeReady;
	}

	// 初始化的时候会加入一条日志(Index=0，不需要真正apply)，
	// 以后Snapshot时，会保留LastApplied的。
	// 所以下面方法不会返回空。
	// 除非什么例外发生。那就抛空指针异常吧。
	public RaftLog LastAppliedLogTermIndex() throws RocksDBException {
		return RaftLog.DecodeTermIndex(ReadLogBytes(LastApplied));
	}

	private void SaveFirstIndex(long newFirstIndex) throws RocksDBException {
		var firstIndexValue = ByteBuffer.Allocate(9);
		firstIndexValue.WriteLong(newFirstIndex);
		Rafts.put(WriteOptions, RaftsFirstIndexKey, 0, RaftsFirstIndexKey.length,
				firstIndexValue.Bytes, 0, firstIndexValue.WriteIndex);
		FirstIndex = newFirstIndex;
	}

	public void CommitSnapshot(String path, long newFirstIndex) throws IOException, RocksDBException {
		Raft.lock();
		try {
			Files.move(Paths.get(path), Paths.get(getSnapshotFullName()), StandardCopyOption.REPLACE_EXISTING);
			SaveFirstIndex(newFirstIndex);
			StartRemoveLogOnlyBefore(newFirstIndex);
		} finally {
			Raft.unlock();
		}
	}

	private RocksIterator NewLogsIterator() {
		Raft.lock();
		try {
			return Logs.newIterator();
		} finally {
			Raft.unlock();
		}
	}

	private void StartRemoveLogOnlyBefore(long index) {
		Raft.lock();
		try {
			if (RemoveLogBeforeFuture != null || !LogsAvailable || Raft.IsShutdown)
				return;
			RemoveLogBeforeFuture = new TaskCompletionSource<>();
		} finally {
			Raft.unlock();
		}

		// 直接对 RocksDb 多线程访问，这里就不做多线程保护了。
		Task.run(() -> {
			try {
				try (var it = NewLogsIterator()) {
					it.seekToFirst();
					while (LogsAvailable && !Raft.IsShutdown && it.isValid()) {
						// 这里只需要log的Index，直接从key里面获取了。
						if (ByteBuffer.Wrap(it.key()).ReadLong() >= index) {
							RemoveLogBeforeFuture.SetResult(true);
							return;
						}

						var key = it.key();
						Logs.delete(WriteOptions, key);

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
				RemoveLogBeforeFuture.SetResult(false);
				RemoveLogBeforeFuture = null;
			}
		}, "RemoveLogBefore" + index);
	}

	/*
	private void RemoveLogReverse(long startIndex, long firstIndex)
	{
	    for (var index = startIndex; index >= firstIndex; index--)
	        RemoveLog(index);
	}
	*/

	public long getLeaderActiveTime() {
		return LeaderActiveTime;
	}

	void setLeaderActiveTime(long value) {
		LeaderActiveTime = value;
	}

	public static RocksDB OpenDb(Options options, String path) throws RocksDBException {
		RocksDBException lastE = null;
		for (int i = 0; i < 10; ++i) {
			try {
				return RocksDB.open(options, path);
			} catch (RocksDBException e) {
				logger.info("RocksDB.open {}", path, e);
				lastE = e;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ignored) {
				}
			}
		}
		throw lastE;
	}

	static final class UniqueRequestSet {
		private RocksDB Db;
		private String DbName;
		private LogSequence LogSequence;

		private RocksDB getDb() {
			return Db;
		}

		public String getDbName() {
			return DbName;
		}

		public void setDbName(String value) {
			DbName = value;
		}

		public LogSequence getLogSequence() {
			return LogSequence;
		}

		public void setLogSequence(LogSequence value) {
			LogSequence = value;
		}

		public UniqueRequestSet(LogSequence lq, String dbName) {
			setLogSequence(lq);
			setDbName(dbName);
		}

		private void Put(RaftLog log, boolean isApply) throws IOException, RocksDBException {
			var db = OpenDb();
			var key = ByteBuffer.Allocate(32);
			log.getLog().getUnique().Encode(key);

			// 先读取并检查状态，减少写操作。
			var existBytes = db.get(key.Bytes, 0, key.WriteIndex);
			if (!isApply && existBytes != null)
				throw new RaftRetryException("Duplicate Request Found = " + log.getLog().getUnique());

			if (existBytes != null) {
				var existState = new UniqueRequestState();
				existState.Decode(ByteBuffer.Wrap(existBytes));
				if (existState.isApplied())
					return;
			}

			var value = ByteBuffer.Allocate(32);
			new UniqueRequestState(log, isApply).Encode(value);
			db.put(getLogSequence().WriteOptions, key.Bytes, 0, key.WriteIndex, value.Bytes, 0, value.WriteIndex);
		}

		public void Save(RaftLog log) throws IOException, RocksDBException {
			Put(log, false);
		}

		public void Apply(RaftLog log) throws IOException, RocksDBException {
			Put(log, true);
		}

		public void Remove(RaftLog log) throws IOException, RocksDBException {
			var key = ByteBuffer.Allocate(32);
			log.getLog().getUnique().Encode(key);
			OpenDb().delete(getLogSequence().WriteOptions, key.Bytes, 0, key.WriteIndex);
		}

		public UniqueRequestState GetRequestState(IRaftRpc raftRpc) throws IOException, RocksDBException {
			var key = ByteBuffer.Allocate(32);
			raftRpc.getUnique().Encode(key);
			var val = OpenDb().get(key.Bytes, 0, key.WriteIndex);
			if (val == null)
				return null;
			var bb = ByteBuffer.Wrap(val);
			var state = new UniqueRequestState();
			state.Decode(bb);
			return state;
		}

		private final Lock mutex = new ReentrantLock();

		private RocksDB OpenDb() throws IOException, RocksDBException {
			mutex.lock();
			try {
				if (null == getDb()) {
					var dir = Paths.get(getLogSequence().Raft.getRaftConfig().getDbHome(), "unique").toString();
					try {
						Files.createDirectories(Paths.get(dir));
					} catch (FileAlreadyExistsException ignored) {
					}
					Db = Zeze.Raft.LogSequence.OpenDb(DatabaseRocksDb.getCommonOptions(),
									Paths.get(dir, getDbName()).toString());
				}
				return getDb();
			} finally {
				mutex.unlock();
			}
		}

		public void Dispose() {
			mutex.lock();
			try {
				if (Db != null) {
					Db.close();
					Db = null;
				}
			} finally {
				mutex.unlock();
			}
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

	public void RemoveExpiredUniqueRequestSet() throws ParseException {
		RaftConfig raftConf = Raft.getRaftConfig();
		long expired = System.currentTimeMillis() - (raftConf.getUniqueRequestExpiredDays() + 1) * 86400_000L;
		SimpleDateFormat format = new SimpleDateFormat("yyyy.M.d");
		var uniqueHome = Paths.get(raftConf.getDbHome(), "unique").toString();

		// try close and delete
		for (var reqSets : UniqueRequestSets.entrySet()) {
			Date db = format.parse(reqSets.getKey());
			if (db.getTime() < expired) {
				reqSets.getValue().Dispose();
				UniqueRequestSets.remove(reqSets.getKey());
				deleteDirectory(new File(Paths.get(uniqueHome, reqSets.getKey()).toString()));
			}
		}
		// try delete in dirs
		File file = new File(uniqueHome);
		if (file.isDirectory()) {
			File[] subFiles = file.listFiles();
			if (subFiles != null) {
				for (var subFile : subFiles) {
					if (subFile.isDirectory()) {
						var dirName = subFile.getName();
						var db = format.parse(dirName);
						if (db.getTime() < expired)
							deleteDirectory(new File(Paths.get(uniqueHome, dirName).toString()));
					}
				}
			}
		}
	}

	void CancelPendingAppendLogFutures() {
		for (var job : LeaderAppendLogs)
			job.getLeaderFuture().cancel(false);
		LeaderAppendLogs.clear();
	}

	void Close() {
		// must after set Raft.IsShutdown = false;
		CancelPendingAppendLogFutures();

		Raft.lock();
		try {
			if (Logs != null) {
				Logs.close();
				Logs = null;
			}
			if (Rafts != null) {
				Rafts.close();
				Rafts = null;
			}
			for (var db : UniqueRequestSets.values())
				db.Dispose();
			UniqueRequestSets.clear();
		} finally {
			Raft.unlock();
		}
	}

	public LogSequence(Raft raft) throws RocksDBException {
		Raft = raft;

		Rafts = OpenDb(DatabaseRocksDb.getCommonOptions(), Paths.get(Raft.getRaftConfig().getDbHome(), "rafts").toString());
		{
			// Read Term
			var termKey = ByteBuffer.Allocate(1);
			termKey.WriteInt(0);
			RaftsTermKey = termKey.Copy();
			var termValue = Rafts.get(RaftsTermKey);
			Term = termValue != null ? ByteBuffer.Wrap(termValue).ReadLong() : 0;
			// Read VoteFor
			var voteForKey = ByteBuffer.Allocate(1);
			voteForKey.WriteInt(1);
			RaftsVoteForKey = voteForKey.Copy();
			var voteForValue = Rafts.get(RaftsVoteForKey);
			VoteFor = voteForValue != null ? ByteBuffer.Wrap(voteForValue).ReadString() : "";
			// Read FirstIndex 由于snapshot并发，Logs中的第一条记录可能不是FirstIndex了。
			var firstIndexKey = ByteBuffer.Allocate(1);
			firstIndexKey.WriteInt(2);
			RaftsFirstIndexKey = firstIndexKey.Copy();
			var firstIndexValue = Rafts.get(RaftsFirstIndexKey);
			FirstIndex = firstIndexValue != null ? ByteBuffer.Wrap(firstIndexValue).ReadLong() : -1; // never snapshot. will re-initialize later.
			// NodeReady
			// 节点第一次启动，包括机器毁坏后换了新机器再次启动时为 false。
			// 当满足以下条件之一：
			// 1. 成为Leader并且Ready
			// 2. 成为Follower并在处理AppendEntries时观察到LeaderCommit发生了变更
			// 满足条件以后设置 NodeReady 为 true。
			// 这个条件影响投票逻辑：NodeReady 为 true 以前，只允许给 Candidate.LastIndex == 0 的节点投票。
			var nodeReadyKey = ByteBuffer.Allocate(1);
			nodeReadyKey.WriteInt(3);
			RaftsNodeReadyKey = nodeReadyKey.Copy();
			var nodeReadyValue = Rafts.get(RaftsNodeReadyKey);
			if (nodeReadyValue != null)
				NodeReady = ByteBuffer.Wrap(nodeReadyValue).ReadBool();
		}

		Logs = OpenDb(DatabaseRocksDb.getCommonOptions(), Paths.get(Raft.getRaftConfig().getDbHome(), "logs").toString());
		{
			// Read Last Log Index
			try (var itLast = Logs.newIterator()) {
				itLast.seekToLast();
				if (itLast.isValid())
					LastIndex = RaftLog.DecodeTermIndex(itLast.value()).getIndex();
				else {
					// empty. add one for prev.
					SaveLog(new RaftLog(Term, 0, new HeartbeatLog()));
					LastIndex = 0;
				}
				logger.info("{}-{} {} LastIndex={} Count={}", Raft.getName(), Raft.isLeader(),
						Raft.getRaftConfig().getDbHome(), LastIndex, GetTestStateMachineCount());

				// 【注意】snapshot 以后 FirstIndex 会推进，不再是从0开始。
				if (FirstIndex == -1) { // never snapshot
					try (var itFirst = Logs.newIterator()) {
						itFirst.seekToFirst();
						FirstIndex = RaftLog.Decode(new Binary(itFirst.value()),
								Raft.getStateMachine()::LogFactory).getIndex();
					}
				}
				LastApplied = FirstIndex;
				CommitIndex = FirstIndex;
			}
		}
		LogsAvailable = true;

		// 可能有没有被清除的日志存在。启动任务。
		StartRemoveLogOnlyBefore(FirstIndex);
	}

	private void TrySetNodeReady() throws RocksDBException {
		if (NodeReady)
			return;

		NodeReady = true;

		var value = ByteBuffer.Allocate(1);
		value.WriteBool(true);
		Rafts.put(WriteOptions, RaftsNodeReadyKey, 0, RaftsNodeReadyKey.length, value.Bytes, 0, value.WriteIndex);
	}

	public UniqueRequestState TryGetRequestState(Protocol<?> p) throws IOException, RocksDBException {
		var raftRpc = (IRaftRpc)p;

		var create = raftRpc.getCreateTime();
		var now = System.currentTimeMillis();
		if ((now - create) / 86400_000 >= Raft.getRaftConfig().getUniqueRequestExpiredDays())
			return null;

		UniqueRequestState state = OpenUniqueRequests(raftRpc.getCreateTime()).GetRequestState(raftRpc);
		return state != null ? state : UniqueRequestState.NOT_FOUND;
	}

	private UniqueRequestSet OpenUniqueRequests(long time) {
		var dateTime = new Date(time);
		@SuppressWarnings("deprecation")
		var dbName = String.format("%d.%d.%d", dateTime.getYear() + 1900, dateTime.getMonth() + 1, dateTime.getDate());
		return UniqueRequestSets.computeIfAbsent(dbName, db -> new UniqueRequestSet(this, db));
	}

	public WriteOptions getWriteOptions() {
		return WriteOptions;
	}

	public void setWriteOptions(WriteOptions value) {
		WriteOptions = value;
	}

	private void SaveLog(RaftLog log) throws RocksDBException {
		var key = ByteBuffer.Allocate(9);
		key.WriteLong(log.getIndex());
		var value = log.Encode();
		Logs.put(WriteOptions, key.Bytes, 0, key.WriteIndex, value.Bytes, 0, value.WriteIndex);

		logger.debug("{}-{} RequestId={} Index={} Count={}", Raft.getName(), Raft.isLeader(),
				log.getLog().getUnique().getRequestId(), log.getIndex(), GetTestStateMachineCount());
	}

	private void SaveLogRaw(long index, Binary rawValue) throws RocksDBException {
		var key = ByteBuffer.Allocate(9);
		key.WriteLong(index);
		Logs.put(WriteOptions, key.Bytes, 0, key.WriteIndex,
				rawValue.InternalGetBytesUnsafe(), rawValue.getOffset(), rawValue.size());

		logger.debug("{}-{} RequestId=? Index={} Count={}",
				Raft.getName(), Raft.isLeader(), index, GetTestStateMachineCount());
	}

	private byte[] ReadLogBytes(long index) throws RocksDBException {
		var key = ByteBuffer.Allocate(9);
		key.WriteLong(index);
		RocksDB logs = Logs;
		if (logs == null)
			return null;
		return logs.get(key.Bytes, 0, key.WriteIndex);
	}

	private RaftLog ReadLog(long index) throws RocksDBException {
		var value = ReadLogBytes(index);
		return value != null ? RaftLog.Decode(new Binary(value), Raft.getStateMachine()::LogFactory) : null;
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
	public SetTermResult TrySetTerm(long term) throws RocksDBException {
		if (term > Term) {
			Term = term;
			var termValue = ByteBuffer.Allocate(9);
			termValue.WriteLong(term);
			Rafts.put(WriteOptions, RaftsTermKey, 0, RaftsTermKey.length, termValue.Bytes, 0, termValue.WriteIndex);
			Raft.setLeaderId("");
			SetVoteFor("");
			LastLeaderCommitIndex = 0;
			return SetTermResult.Newer;
		}
		return term == Term ? SetTermResult.Same : SetTermResult.Older;
	}

	public boolean CanVoteFor(String voteFor) {
		String thisVoteFor = VoteFor;
		return thisVoteFor == null || thisVoteFor.isEmpty() || thisVoteFor.equals(voteFor);
	}

	public void SetVoteFor(String voteFor) throws RocksDBException {
		if (!VoteFor.equals(voteFor)) {
			VoteFor = voteFor;
			var voteForValue = ByteBuffer.Allocate(5 + voteFor.length());
			voteForValue.WriteString(voteFor);
			Rafts.put(WriteOptions, RaftsVoteForKey, 0, RaftsVoteForKey.length,
					voteForValue.Bytes, 0, voteForValue.WriteIndex);
		}
	}

	private void TryCommit(AppendEntries rpc, Server.ConnectorEx connector) throws Throwable {
		connector.setNextIndex(rpc.Argument.getLastEntryIndex() + 1);
		connector.setMatchIndex(rpc.Argument.getLastEntryIndex());

		// 旧的 AppendEntries 的结果，不用继续处理了。
		// 【注意】这个不是必要的，是一个小优化。
		if (rpc.Argument.getLastEntryIndex() <= CommitIndex)
			return;

		// find MaxMajorityLogIndex
		// Rules for Servers
		// If there exists an N such that N > commitIndex, a majority
		// of matchIndex[i] ≥ N, and log[N].term == currentTerm:
		// set commitIndex = N(§5.3, §5.4).
		var followers = new ArrayList<Server.ConnectorEx>();
		Raft.getServer().getConfig().ForEachConnector(c ->
				followers.add(c instanceof Server.ConnectorEx ? (Server.ConnectorEx)c : null));
		followers.sort((a, b) -> Long.compare(b.getMatchIndex(), a.getMatchIndex()));
		var maxMajorityLogIndex = followers.get(Raft.getRaftConfig().getHalfCount() - 1).getMatchIndex();
		if (maxMajorityLogIndex > CommitIndex) {
			var maxMajorityLog = ReadLog(maxMajorityLogIndex);
			if (maxMajorityLog == null || maxMajorityLog.getTerm() != Term) {
				// 如果是上一个 Term 未提交的日志在这一次形成的多数派，
				// 不自动提交。
				// 总是等待当前 Term 推进时，顺便提交它。
				return;
			}
			// 推进！
			CommitIndex = maxMajorityLogIndex;
			TrySetNodeReady();
			TryStartApplyTask(maxMajorityLog);
		}
	}

	// under lock (Raft)
	private void TryStartApplyTask(RaftLog lastApplicableLog) throws Throwable {
		if (ApplyFuture == null && !Raft.IsShutdown) {
			// 仅在没有 apply 进行中才尝试进行处理。
			if (CommitIndex - LastApplied < Raft.getRaftConfig().getBackgroundApplyCount()) {
				// apply immediately in current thread
				TryApply(lastApplicableLog, Long.MAX_VALUE);
				return;
			}

			ApplyFuture = new TaskCompletionSource<>();
			Raft.getImportantThreadPool().execute(() -> {
				try {
					ApplyFuture.SetResult(Task.Call(this::BackgroundApply, "BackgroundApply") == 0); // 如果有人等待。
				} finally {
					ApplyFuture = null; // 允许再次启动，不需要等待了。
				}
			});
		}
	}

	private long BackgroundApply() throws Throwable {
		while (!Raft.IsShutdown) {
			Raft.lock();
			try {
				// ReadLog Again，CommitIndex Maybe Grow.
				var lastApplicableLog = ReadLog(CommitIndex);
				TryApply(lastApplicableLog, Raft.getRaftConfig().getBackgroundApplyCount());
				if (lastApplicableLog != null && LastApplied == lastApplicableLog.getIndex())
					return 0; // 本次Apply结束。
			} finally {
				Raft.unlock();
			}
			Thread.yield();
		}
		return Procedure.CancelException;
	}

	private void TryApply(RaftLog lastApplicableLog, long count) throws Throwable {
		if (lastApplicableLog == null) {
			logger.error("lastApplicableLog is null.");
			return;
		}
		for (long index = LastApplied + 1; index <= lastApplicableLog.getIndex() && count > 0; --count) {
			RaftLog raftLog = LeaderAppendLogs.remove(index);
			if (raftLog == null && (raftLog = ReadLog(index)) == null) {
				logger.warn("What Happened! index={} lastApplicableLog={} LastApplied={}",
						index, lastApplicableLog.getIndex(), LastApplied);
				// trySnapshot(); // 错误的时候不做这个尝试了。
				return; // end?
			}

			index = raftLog.getIndex() + 1;
			raftLog.getLog().Apply(raftLog, Raft.getStateMachine());
			if (raftLog.getLog().getUnique().getRequestId() > 0)
				OpenUniqueRequests(raftLog.getLog().getCreateTime()).Apply(raftLog);
			LastApplied = raftLog.getIndex(); // 循环可能退出，在这里修改。
			//*
			if (LastIndex - LastApplied < 10) {
				logger.debug("{}-{} {} RequestId={} LastIndex={} LastApplied={} Count={}",
						Raft.getName(), Raft.isLeader(), Raft.getRaftConfig().getDbHome(),
						raftLog.getLog().getUnique().getRequestId(), LastIndex, LastApplied,
						GetTestStateMachineCount());
			}
			// */
			var future = raftLog.getLeaderFuture();
			if (future != null)
				future.SetResult(0);
		}
		// logger.debug($"{Raft.Name}-{Raft.IsLeader} CommitIndex={CommitIndex} RequestId={lastApplicableLog.Log.Unique.RequestId} LastIndex={LastIndex} LastApplied={LastApplied} Count={GetTestStateMachineCount()}");
		trySnapshot();
	}

	private void trySnapshot() {
		var snapshotLogCount = Raft.getRaftConfig().getSnapshotLogCount();
		if (snapshotLogCount > 0) {
			if (LastApplied - PrevSnapshotIndex > snapshotLogCount) {
				PrevSnapshotIndex = LastApplied;
				Task.run(this::Snapshot, "Snapshot");
			}
		}
		// else disable
	}

	public long GetTestStateMachineCount() {
		StateMachine stateMachine = Raft.getStateMachine();
		return stateMachine instanceof Test.TestStateMachine ? ((Test.TestStateMachine)stateMachine).getCount() : -1;
	}

	public void SendHeartbeatTo(Server.ConnectorEx connector) {
		Raft.lock();
		try {
			connector.setAppendLogActiveTime(System.currentTimeMillis());

			if (!Raft.isLeader())
				return; // skip if is not a leader

			if (connector.getPending() != null)
				return;

			if (getInstallSnapshotting().containsKey(connector.getName()))
				return;

			var socket = connector.TryGetReadySocket();
			if (socket == null)
				return; // Heartbeat Will Retry

			var heartbeat = new AppendEntries();
			heartbeat.Argument.setTerm(Term);
			heartbeat.Argument.setLeaderId(Raft.getName());
			heartbeat.Send(socket, (p) -> {
				if (heartbeat.isTimeout())
					return 0; // skip

				Raft.lock();
				try {
					if (Raft.getLogSequence().TrySetTerm(heartbeat.Result.getTerm()) == SetTermResult.Newer) {
						// new term found.
						Raft.ConvertStateTo(Zeze.Raft.Raft.RaftState.Follower);
						return Procedure.Success;
					}
				} finally {
					Raft.unlock();
				}
				return 0;
			}, Raft.getRaftConfig().getAppendEntriesTimeout());
		} finally {
			Raft.unlock();
		}
	}

	public void AppendLog(Log log, boolean WaitApply) throws Throwable {
		AppendLog(log, WaitApply, null);
	}

	public static final class AppendLogResult {
		public long term;
		public long index;
	}

	public void AppendLog(Log log, boolean WaitApply, AppendLogResult result) throws Throwable {
		TaskCompletionSource<Integer> future = null;
		Raft.lock();
		try {
			if (!Raft.isLeader())
				throw new RaftRetryException("not leader"); // 快速失败

			var raftLog = new RaftLog(Term, LastIndex + 1, log);
			if (raftLog.getLog().getUnique().getRequestId() > 0)
				OpenUniqueRequests(raftLog.getLog().getCreateTime()).Save(raftLog);
			SaveLog(raftLog);

			// 容易出错的放到前面。
			if (WaitApply) {
				raftLog.setLeaderFuture(future = new TaskCompletionSource<>());
				if (LeaderAppendLogs.putIfAbsent(raftLog.getIndex(), raftLog) != null) {
					logger.fatal("LeaderAppendLogs.TryAdd Fail. Index={}", raftLog.getIndex(), new Exception());
					Raft.FatalKill();
				}
			}
			// 最后修改LastIndex。
			LastIndex = raftLog.getIndex();
			// 广播给followers并异步等待多数确认
			try {
				Raft.getServer().getConfig().ForEachConnector(c -> TrySendAppendEntries((Server.ConnectorEx)c, null));
			} catch (Throwable e) {
				LastIndex--;
				// 只有下面这个需要回滚，日志(SaveLog, OpenUniqueRequests(...).Save)以后根据LastIndex覆盖。
				if (WaitApply)
					LeaderAppendLogs.remove(raftLog.getIndex());
				throw e;
			}
			if (result != null) {
				result.term = Term;
				result.index = LastIndex;
			}
		} finally {
			Raft.unlock();
		}

		if (WaitApply && !future.await(Raft.getRaftConfig().getAppendEntriesTimeout() * 2L + 1000)) {
			LeaderAppendLogs.remove(LastIndex);
			throw new RaftRetryException("timeout or canceled");
		}
	}

	private boolean getSnapshotting() {
		return Snapshotting;
	}

	private void setSnapshotting(boolean value) {
		Snapshotting = value;
	}

	public ConcurrentHashMap<String, Server.ConnectorEx> getInstallSnapshotting() {
		return InstallSnapshotting;
	}

	public String getSnapshotFullName() {
		return Paths.get(Raft.getRaftConfig().getDbHome(), SnapshotFileName).toString();
	}

	public void EndReceiveInstallSnapshot(String path, InstallSnapshot r) throws Throwable {
		LogsAvailable = false; // cancel RemoveLogBefore
		var removeLogBeforeFuture = RemoveLogBeforeFuture;
		if (removeLogBeforeFuture != null)
			removeLogBeforeFuture.await();
		Raft.lock();
		try {
			try {
				// 6. If existing log entry has same index and term as snapshot's
				// last included entry, retain log entries following it and reply
				var last = ReadLog(r.Argument.getLastIncludedIndex());
				if (null != last && last.getTerm() == r.Argument.getLastIncludedTerm()) {
					// 【注意】没有错误处理：比如LastIncludedIndex是否超过CommitIndex之类的。
					// 按照现在启动InstallSnapshot的逻辑，不会发生这种情况。
					logger.warn("Exist Local Log. Do It Like A Local Snapshot!");
					CommitSnapshot(path, r.Argument.getLastIncludedIndex());
					return;
				}
				// 7. Discard the entire log
				// 整个删除，那么下一次AppendEntries又会找不到prev。不就xxx了吗?
				// 我的想法是，InstallSnapshot 最后一个 trunk 带上 LastIncludedLog，
				// 接收者清除log，并把这条日志插入（这个和系统初始化时插入的Index=0的日志道理差不多）。
				// 【除了快照最后包含的日志，其他都删除。】
				Logs.close();
				Logs = null;
				CancelPendingAppendLogFutures();
				var logsDir = Paths.get(Raft.getRaftConfig().getDbHome(), "logs").toString();
				deleteDirectory(new File(logsDir));
				Logs = OpenDb(DatabaseRocksDb.getCommonOptions(), logsDir);
				var lastIncludedLog = RaftLog.Decode(r.Argument.getLastIncludedLog(),
						Raft.getStateMachine()::LogFactory);
				SaveLog(lastIncludedLog);
				CommitSnapshot(path, lastIncludedLog.getIndex());

				LastIndex = lastIncludedLog.getIndex();
				CommitIndex = FirstIndex;
				LastApplied = FirstIndex;

				// 【关键】记录这个，放弃当前Term的投票。
				SetVoteFor(Raft.getLeaderId());

				// 8. Reset state machine using snapshot contents (and load
				// snapshot's cluster configuration)
				Raft.getStateMachine().LoadSnapshot(getSnapshotFullName());
				logger.info("{} EndReceiveInstallSnapshot Path={}", Raft.getName(), path);
			} finally {
				LogsAvailable = true;
			}
		} finally {
			Raft.unlock();
		}
	}

	public void Snapshot() throws Throwable {
		Raft.lock();
		try {
			if (getSnapshotting() || !getInstallSnapshotting().isEmpty())
				return;

			setSnapshotting(true);
		} finally {
			Raft.unlock();
		}
		try {
			// 忽略Snapshot返回结果。肯定是重复调用导致的。
			// out 结果这里没有使用，定义在参数里面用来表示这个很重要。
			var path = getSnapshotFullName() + ".tmp";
			var result = Raft.getStateMachine().Snapshot(path);
			logger.info("{} Snapshot Path={} LastIndex={} LastTerm={}",
					Raft.getName(), path, result.LastIncludedIndex, result.LastIncludedTerm);
		} finally {
			Raft.lock();
			try {
				setSnapshotting(false);
			} finally {
				Raft.unlock();
			}
		}
	}

	public void CancelAllInstallSnapshot() throws Throwable {
		for (var installing : getInstallSnapshotting().values())
			EndInstallSnapshot(installing);
	}

	public void EndInstallSnapshot(Server.ConnectorEx c) throws Throwable {
		var cex = getInstallSnapshotting().remove(c.getName());
		if (cex != null) {
			var state = cex.getInstallSnapshotState();
			logger.info("{} InstallSnapshot LastIncludedIndex={} Done={} c={}", Raft.getName(),
					state.getPending().Argument.getLastIncludedIndex(),
					state.getPending().Argument.getDone(), c.getName());
			state.getFile().close();
			if (state.getPending().Argument.getDone() && state.getPending().getResultCode() == 0) {
				cex.setNextIndex(state.getPending().Argument.getLastIncludedIndex() + 1);

				if (state.getPending().Argument.getLastIncludedIndex() > cex.getMatchIndex()) // see EndReceiveInstallSnapshot 6.
					cex.setMatchIndex(state.getPending().Argument.getLastIncludedIndex());
				// start log copy
				TrySendAppendEntries(c, null);
			}
		}
		c.setInstallSnapshotState(null);
	}

	private void StartInstallSnapshot(Server.ConnectorEx c) throws Throwable {
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
			st.setFirstLog(ReadLog(FirstIndex));
			st.getPending().Argument.setTerm(Term);
			st.getPending().Argument.setLeaderId(Raft.getName());
			st.getPending().Argument.setLastIncludedIndex(st.getFirstLog().getIndex());
			st.getPending().Argument.setLastIncludedTerm(st.getFirstLog().getTerm());

			logger.info("{} InstallSnapshot Start... Path={} c={}", Raft.getName(), path, c.getName());
			st.TrySend(this, c);
		} else {
			// 这一般的情况是snapshot文件被删除了。
			// 【注意】这种情况也许报错更好？
			// 内部会判断，不会启动多个snapshot。
			Snapshot();
		}
	}

	@SuppressWarnings("SameReturnValue")
	private long ProcessAppendEntriesResult(Server.ConnectorEx connector, Protocol<?> p) throws Throwable {
		// 这个rpc处理流程总是返回 Success，需要统计观察不同的分支的发生情况，再来定义不同的返回值。
		var r = (AppendEntries)p;
		Raft.lock();
		try {
			if (r.isTimeout() && Raft.isLeader()) {
				TrySendAppendEntries(connector, r); // timeout and resend
				return Procedure.Success;
			}

			if (Raft.getLogSequence().TrySetTerm(r.Result.getTerm()) == SetTermResult.Newer) {
				// new term found.
				Raft.ConvertStateTo(Zeze.Raft.Raft.RaftState.Follower);
				// 发现新的 Term，已经不是Leader，不能继续处理了。
				// 直接返回。
				connector.setPending(null);
				return Procedure.Success;
			}

			if (!Raft.isLeader()) {
				connector.setPending(null);
				return Procedure.Success;
			}

			if (r.Result.getSuccess()) {
				TryCommit(r, connector);
				// TryCommit 推进了NextIndex，
				// 可能日志没有复制完或者有新的AppendLog。
				// 尝试继续复制日志。
				// see TrySendAppendEntries 内的
				// "限制一次发送的日志数量”
				TrySendAppendEntries(connector, r);
				return Procedure.Success;
			}

			// 日志同步失败，调整NextIndex，再次尝试。
			if (r.Result.getNextIndex() == 0)
				connector.setNextIndex(connector.getNextIndex() - 1); // 默认的回退模式。
			else if (r.Result.getNextIndex() <= FirstIndex) {
				// leader snapshot，follower 完全没法匹配了，后续的 TrySendAppendEntries 将启动 InstallSnapshot。
				connector.setNextIndex(FirstIndex);
			} else if (r.Result.getNextIndex() >= LastIndex) {
				logger.fatal("Impossible r.Result.NextIndex({}) >= LastIndex({}) there must be a bug.",
						r.Result.getNextIndex(), LastIndex, new Exception());
				Raft.FatalKill();
			} else
				connector.setNextIndex(r.Result.getNextIndex()); // fast locate
			TrySendAppendEntries(connector, r); //resend. use new NextIndex。
			return Procedure.Success;
		} finally {
			Raft.unlock();
		}
	}

	public void TrySendAppendEntries(Server.ConnectorEx connector, AppendEntries pending) throws Throwable {
		// Pending 处理必须完成。
		connector.setAppendLogActiveTime(System.currentTimeMillis());
		if (connector.getPending() != pending)
			return;
		// 先清除，下面中断(return)不用每次自己清除。
		connector.setPending(null);

		if (!Raft.isLeader())
			return; // skip if is not a leader

		// 【注意】
		// 正在安装Snapshot，此时不复制日志，肯定失败。
		// 不做这个判断也是可以工作的，算是优化。
		if (getInstallSnapshotting().containsKey(connector.getName()))
			return;

		var socket = connector.TryGetReadySocket();
		if (socket == null)
			return;

		if (connector.getNextIndex() > LastIndex)
			return; // copy end.

		if (connector.getNextIndex() == FirstIndex) {
			// 已经到了日志开头，此时不会有prev-log，无法复制日志了。
			// 这一般发生在Leader进行了Snapshot，但是Follower的日志还更老。
			// 新起的Follower也一样。
			StartInstallSnapshot(connector);
			return;
		}

		var nextLog = ReadLog(connector.getNextIndex());
		if (nextLog == null) // Logs可能已经变成null了, 小概率事件
			return;
		var prevLog = ReadLog(nextLog.getIndex() - 1);
		if (prevLog == null) // Logs可能已经变成null了, 小概率事件
			return;

		connector.setPending(new AppendEntries());
		connector.getPending().Argument.setTerm(Term);
		connector.getPending().Argument.setLeaderId(Raft.getName());
		connector.getPending().Argument.setLeaderCommit(CommitIndex);

		connector.getPending().Argument.setPrevLogIndex(prevLog.getIndex());
		connector.getPending().Argument.setPrevLogTerm(prevLog.getTerm());

		// 限制一次发送的日志数量，【注意】这个不是raft要求的。
		int maxCount = Raft.getRaftConfig().getMaxAppendEntriesCount();
		RaftLog lastCopyLog = nextLog;
		for (var copyLog = nextLog;
			 maxCount > 0 && copyLog != null && copyLog.getIndex() <= LastIndex;
			 copyLog = ReadLog(copyLog.getIndex() + 1), --maxCount) {
			lastCopyLog = copyLog;
			connector.getPending().Argument.getEntries().add(new Binary(copyLog.Encode()));
		}
		connector.getPending().Argument.setLastEntryIndex(lastCopyLog.getIndex());
		if (!connector.getPending().Send(socket, (p) ->
				ProcessAppendEntriesResult(connector, p), Raft.getRaftConfig().getAppendEntriesTimeout())) {
			connector.setPending(null);
			// Heartbeat Will Retry
		}
	}

	public RaftLog LastRaftLogTermIndex() throws RocksDBException {
		return RaftLog.DecodeTermIndex(ReadLogBytes(LastIndex));
	}

	private void RemoveLogAndCancelStart(long startIndex, long endIndex) throws IOException, RocksDBException {
		for (long index = startIndex; index <= endIndex; index++) {
			RaftLog raftLog;
			if (index > LastApplied && (raftLog = LeaderAppendLogs.remove(index)) != null) {
				// 还没有applied的日志被删除，
				// 当发生在重新选举，但是旧的leader上还有一些没有提交的请求时，
				// 需要取消。
				// 其中判断：index > LastApplied 不是必要的。
				// Apply的时候已经TryRemove了，仅会成功一次。
				var future = raftLog.getLeaderFuture();
				if (future != null)
					future.cancel(false);
			}
			RemoveLog(index);
		}
	}

	private void RemoveLog(long index) throws IOException, RocksDBException {
		var raftLog = ReadLog(index);
		if (raftLog != null) {
			var key = ByteBuffer.Allocate(9);
			key.WriteLong(index);
			Logs.delete(WriteOptions, key.Bytes, 0, key.WriteIndex);
			if (raftLog.getLog().getUnique().getRequestId() > 0)
				OpenUniqueRequests(raftLog.getLog().getCreateTime()).Remove(raftLog);
		}
	}

	public long FollowerOnAppendEntries(AppendEntries r) throws Throwable {
		setLeaderActiveTime(System.currentTimeMillis());
		r.Result.setTerm(Term); // maybe rewrite later
		r.Result.setSuccess(false); // set default false

		if (r.Argument.getTerm() < Term) {
			// 1. Reply false if term < currentTerm (§5.1)
			r.SendResult();
			logger.info("this={} Leader={} PrevLogIndex={} term < currentTerm",
					Raft.getName(), r.Argument.getLeaderId(), r.Argument.getPrevLogIndex());
			return Procedure.Success;
		}

		switch (TrySetTerm(r.Argument.getTerm())) {
		case Newer:
			Raft.ConvertStateTo(Zeze.Raft.Raft.RaftState.Follower);
			r.Result.setTerm(Term); // new term
			break;

		case Same:
			switch (Raft.getState()) {
			case Candidate:
				// see raft.pdf 文档. 仅在 Candidate 才转。【找不到在文档哪里了，需要确认这点】
				Raft.ConvertStateTo(Zeze.Raft.Raft.RaftState.Follower);
				break;
			case Leader:
				logger.fatal("Receive AppendEntries from another leader={} with same term={}, there must be a bug. this={}",
						r.Argument.getLeaderId(), Term, Raft.getLeaderId(), new Exception());
				Raft.FatalKill();
				return 0;
			}
			break;
		}

		Raft.setLeaderId(r.Argument.getLeaderId());

		// is Heartbeat(KeepAlive)
		if (r.Argument.getEntries().isEmpty()) {
			r.Result.setSuccess(true);
			r.SendResult();
			return Procedure.Success;
		}

		// check and copy log ...
		var prevLog = ReadLog(r.Argument.getPrevLogIndex());
		if (prevLog == null || prevLog.getTerm() != r.Argument.getPrevLogTerm()) {
			// 2. Reply false if log doesn't contain an entry
			// at prevLogIndex whose term matches prevLogTerm(§5.3)

			// fast locate when mismatch
			r.Result.setNextIndex(r.Argument.getPrevLogIndex() > LastIndex ? LastIndex + 1 : 0);

			r.SendResult();
			logger.debug("this={} Leader={} Index={} prevLog mismatch",
					Raft.getName(), r.Argument.getLeaderId(), r.Argument.getPrevLogIndex());
			return Procedure.Success;
		}

		// NodeReady 严格点，仅在正常复制时才检测。
		if (LastLeaderCommitIndex == 0) {
			// Term 增加时会重置为0，see TrySetTerm。严格点？
			LastLeaderCommitIndex = r.Argument.getLeaderCommit();
		} else if (r.Argument.getLeaderCommit() > LastLeaderCommitIndex) {
			// 这里只要LeaderCommit推进就行，不需要自己的CommitIndex变更。
			// LeaderCommit推进，意味着，已经达成了多数，自己此时可能处于少数派。
			// 本结点CommitIndex是否还处于更早的时期，是没有关系的。
			TrySetNodeReady();
		}

		int entryIndex = 0;
		var copyLogIndex = prevLog.getIndex() + 1;
		for (; entryIndex < r.Argument.getEntries().size(); ++entryIndex, ++copyLogIndex) {
			var copyLog = RaftLog.Decode(r.Argument.getEntries().get(entryIndex), Raft.getStateMachine()::LogFactory);
			if (copyLog.getIndex() != copyLogIndex) {
				logger.fatal("copyLog.Index({}) != copyLogIndex({}) Leader={} this={}",
						copyLog.getIndex(), copyLogIndex, r.Argument.getLeaderId(), Raft.getName(), new Exception());
				Raft.FatalKill();
			}
			if (copyLog.getIndex() < FirstIndex)
				continue; // 快照以前的日志忽略。

			// 本地已经存在日志。
			if (copyLog.getIndex() <= LastIndex) {
				var conflictCheck = ReadLog(copyLog.getIndex());
				if (conflictCheck == null || conflictCheck.getTerm() == copyLog.getTerm())
					continue;

				// 3. If an existing entry conflicts
				// with a new one (same index but different terms),
				// delete the existing entry and all that follow it(§5.3)
				// raft.pdf 5.3
				if (conflictCheck.getIndex() <= CommitIndex) {
					logger.fatal("{} truncate committed entries: {} <= {}", Raft.getName(),
							conflictCheck.getIndex(), CommitIndex, new Exception());
					Raft.FatalKill();
				}
				RemoveLogAndCancelStart(conflictCheck.getIndex(), LastIndex);
				LastIndex = conflictCheck.getIndex() - 1;
			}
			break;
		}
		// Append this and all following entries.
		// 4. Append any new entries not already in the log
		for (; entryIndex < r.Argument.getEntries().size(); ++entryIndex, ++copyLogIndex)
			SaveLogRaw(copyLogIndex, r.Argument.getEntries().get(entryIndex));

		copyLogIndex--;
		// 必须判断，防止本次AppendEntries都是旧的。
		if (copyLogIndex > LastIndex)
			LastIndex = copyLogIndex;

		// CheckDump(prevLog.Index, copyLogIndex, r.Argument.Entries);

		// 5. If leaderCommit > commitIndex,
		// set commitIndex = min(leaderCommit, index of last new entry)
		if (r.Argument.getLeaderCommit() > CommitIndex) {
			CommitIndex = Math.min(r.Argument.getLeaderCommit(), LastRaftLogTermIndex().getIndex());
			TryStartApplyTask(ReadLog(CommitIndex));
		}
		r.Result.setSuccess(true);
		logger.debug("{}: {}", Raft.getName(), r);
		r.SendResultCode(0);

		return Procedure.Success;
	}

	@SuppressWarnings("unused")
	private void CheckDump(long prevLogIndex, long lastIndex, ArrayList<Binary> entries) throws RocksDBException {
		var logs = new StringBuilder();
		for (var index = prevLogIndex + 1; index <= lastIndex; index++)
			logs.append(ReadLog(index)).append('\n');
		var copies = new StringBuilder();
		for (var entry : entries)
			copies.append(RaftLog.Decode(entry, Raft.getStateMachine()::LogFactory)).append('\n');

		if (logs.toString().equals(copies.toString()))
			return;

		logger.info("================= logs ======================");
		logger.info("{}", logs);
		logger.info("================= copies ======================");
		logger.info("{}", copies);
		Raft.FatalKill();
	}
}
