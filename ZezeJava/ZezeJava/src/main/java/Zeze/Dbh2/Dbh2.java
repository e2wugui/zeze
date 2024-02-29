package Zeze.Dbh2;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BWalkKeyValue;
import Zeze.Builtin.Dbh2.CommitBatch;
import Zeze.Builtin.Dbh2.Get;
import Zeze.Builtin.Dbh2.KeepAlive;
import Zeze.Builtin.Dbh2.PrepareBatch;
import Zeze.Builtin.Dbh2.SetBucketMeta;
import Zeze.Builtin.Dbh2.SplitPut;
import Zeze.Builtin.Dbh2.UndoBatch;
import Zeze.Builtin.Dbh2.Walk;
import Zeze.Builtin.Dbh2.WalkKey;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Raft.Agent;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RaftLog;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action0;
import Zeze.Util.Action2;
import Zeze.Util.FuncLong;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

public class Dbh2 extends AbstractDbh2 implements Closeable {
	private static final Logger logger = LogManager.getLogger(Dbh2.class);
	private final Dbh2Config dbh2Config = new Dbh2Config();
	private final Raft raft;
	private final Dbh2StateMachine stateMachine;
	private final Dbh2Manager manager;
	private final Locks locks = new Locks();

	public Locks getLocks() {
		return locks;
	}

	// 性能统计。
	public static String formatMeta(BBucketMeta.Data meta) {
		if (null == meta)
			return "";
		//noinspection StringBufferReplaceableByString
		var sb = new StringBuilder();
		sb.append(meta.getDatabaseName()).append(".").append(meta.getTableName());
		sb.append("[").append(meta.getKeyFirst()).append(", ").append(meta.getKeyLast()).append(")");
		return sb.toString();
	}

	public class Dbh2RaftServer extends Zeze.Raft.Server {
		public Dbh2RaftServer(Raft raft, String name, Config config) {
			super(raft, name, config);
		}

		private ConcurrentLinkedQueue<Action0> prepareQueue;

		public synchronized void setupPrepareQueue() {
			if (null == prepareQueue)
				prepareQueue = new ConcurrentLinkedQueue<>();
		}

		public synchronized ConcurrentLinkedQueue<Action0> takePrepareQueue() {
			var tmp = prepareQueue;
			prepareQueue = null;
			return tmp;
		}

		@Override
		public <P extends Protocol<?>> void dispatchRaftRpcResponse(P p, ProtocolHandle<P> responseHandle,
																	ProtocolFactoryHandle<?> factoryHandle) throws Exception {
			raft.getImportantThreadExecutor().execute(() -> {
				try {
					responseHandle.handle(p);
				} catch (Exception e) {
					logger.error("", e);
				}
			});
		}

		@Override
		public synchronized void dispatchRaftRequest(Protocol<?> p, FuncLong func, String name, Action0 cancel,
													 DispatchMode mode) throws Exception {
			if (null != prepareQueue && isPrepareRequest(p.getTypeId())) {
				prepareQueue.add(() -> Task.call(func, p));
			} else if (isQueryRequest(p.getTypeId())) {
				// 允许Get请求并发
				super.dispatchRaftRequest(p, func, name, cancel, mode);
			} else {
				raft.getUserThreadExecutor().execute(() -> Task.call(func, p));
			}
		}
	}

	private static boolean isQueryRequest(long typeId) {
		return typeId == Get.TypeId_
				|| typeId == Walk.TypeId_
				|| typeId == WalkKey.TypeId_;
	}

	private static boolean isPrepareRequest(long typeId) {
		return typeId == PrepareBatch.TypeId_;
	}

	public Raft getRaft() {
		return raft;
	}

	public Dbh2StateMachine getStateMachine() {
		return stateMachine;
	}

	public Dbh2Manager getManager() {
		return manager;
	}

	public Dbh2Config getDbh2Config() {
		return dbh2Config;
	}

	public Dbh2(Dbh2Manager manager, String raftName, RaftConfig raftConf, Config config, boolean writeOptionSync) {
		this.manager = manager;

		var selfNode = raftConf.getNodes().get(raftName);
		if (!selfNode.isSuggestMajority()) {
			// 根据配置，发现自己不是推荐的多数派，先去检测(等待)建议的多数派产生Leader。
			// 如果检测失败，继续启动过程，此后即时不是推荐的，也可能成为Leader。
			Agent.waitForLeader(raftConf);
		}
		if (config == null)
			config = Config.load();
		config.parseCustomize(this.dbh2Config);

		try {
			stateMachine = new Dbh2StateMachine(this);
			raft = new Raft(stateMachine, raftName, raftConf, config, "Zeze.Dbh2.Server", Dbh2RaftServer::new);
			raftConf.setSnapshotCommitDelayed(true);
			logger.info("newRaft: {}", raft.getName());
			stateMachine.openBucket();
			var writeOptions = writeOptionSync ? RocksDatabase.getSyncWriteOptions() : RocksDatabase.getDefaultWriteOptions();
			raft.getLogSequence().setWriteOptions(writeOptions);
			stateMachine.getBucket().setWriteOptions(writeOptions);

			RegisterProtocols(raft.getServer());
			raft.setOnLeaderReady(this::recoverSplitting);
			raft.setOnFollowerReceiveKeepAlive(this::onFollowerReceiveKeepAlive);
			raft.getServer().start();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void close() throws IOException {
		logger.info("closeRaft: " + raft.getName());
		try {
			raft.shutdown();
			stateMachine.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected long ProcessSetBucketMetaRequest(SetBucketMeta r) throws Exception {
		var log = new LogSetBucketMeta(r);
		raft.appendLog(log, r.Result, (raftLog, result)
				-> r.SendResultCode(result ? 0 : Procedure.CancelException)); // result is empty
		return 0;
	}

	@Override
	protected long ProcessGetRequest(Zeze.Builtin.Dbh2.Get r) throws RocksDBException, InterruptedException {
		stateMachine.counterGet.incrementAndGet();
//		var lock = getLocks().get(r.Argument.getKey());
//		lock.lock(this);
//		try {
			// 直接读取数据库。是否可以读取由raft控制。raft启动时有准备阶段。
			var bucket = stateMachine.getBucket();
			if (!bucket.inBucket(r.Argument.getDatabase(), r.Argument.getTable(), r.Argument.getKey()))
				return errorCode(eBucketMismatch);
			var value = bucket.get(r.Argument.getKey());
			if (null == value)
				r.Result.setNull(true);
			else {
				r.Result.setValue(value);
				stateMachine.sizeGet.addAndGet(value.size());
			}
			r.SendResult();
//		} finally {
//			lock.unlock();
//		}
		return 0;
	}

	@Override
	protected long ProcessKeepAliveRequest(KeepAlive r) throws Exception {
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessPrepareBatchRequest(PrepareBatch r) throws Exception {
		// lock
		var txn = new Dbh2Transaction(this, r.Argument.getBatch());
		try {
			// save txn
			if (null != stateMachine.getTransactions().putIfAbsent(r.Argument.getBatch().getTid(), txn))
				return errorCode(eDuplicateTid);
			// check inBucket
			if (!stateMachine.getBucket().inBucket(r.Argument.getDatabase(), r.Argument.getTable()))
				return errorCode(eBucketMismatch);

			var refused = r.Result; // 对于puts可以考虑只传key，网络占用少一些。
			var splitHistory = stateMachine.getBucket().getSplitMetaHistory();
			for (var e : r.Argument.getBatch().getPuts().entrySet()) {
				var key = e.getKey();
				var value = e.getValue();
				if (!stateMachine.getBucket().inBucket(key)) {
					var locate = splitHistory.locate(key);

					if (null == locate || locate.getKeyFirst().equals(stateMachine.getBucket().getBucketMeta().getKeyFirst()))
						return errorCode(eBucketNotFound); // 找不到或者又找到了自己。

					var batches = refused.getRefused().computeIfAbsent(locate.getRaftConfig(), (__) -> new BBatch.Data());
					batches.getPuts().put(key, value);
				}
			}
			for (var del : r.Argument.getBatch().getDeletes()) {
				if (!stateMachine.getBucket().inBucket(del)) {
					var locate = splitHistory.locate(del);

					if (null == locate || locate.getKeyFirst().equals(stateMachine.getBucket().getBucketMeta().getKeyFirst()))
						return errorCode(eBucketNotFound); // 找不到或者又找到了自己。

					var batches = refused.getRefused().computeIfAbsent(locate.getRaftConfig(), (__) -> new BBatch.Data());
					batches.getDeletes().add(del);
				}
			}

			// 移除拒绝的数据。
			for (var e : refused.getRefused().values()) {
				for (var p : e.getPuts().entrySet())
					r.Argument.getBatch().getPuts().remove(p.getKey());
				for (var d : e.getDeletes())
					r.Argument.getBatch().getDeletes().remove(d);
			}

			// apply to raft
			getRaft().appendLog(new LogPrepareBatch(r), r.Result,
					(raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));

			// 操作成功，释放所有权。see finally.
			txn = null;
		} finally {
			if (null != txn) {
				stateMachine.getTransactions().remove(r.Argument.getBatch().getTid()); // undo putIfAbsent
				txn.close();
			}
		}
		return 0;
	}

	@Override
	protected long ProcessCommitBatchRequest(CommitBatch r) throws Exception {
		getRaft().appendLog(new LogCommitBatch(r), r.Result,
				(raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));
		return 0;
	}

	@Override
	protected long ProcessUndoBatchRequest(UndoBatch r) throws Exception {
		getRaft().appendLog(new LogUndoBatch(r), r.Result,
				(raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));
		return 0;
	}

	private boolean walkDesc(Binary exclusiveStartKey, int proposeLimit,
							 Action2<Binary, RocksIterator> fill) throws Exception {
		try (var it = stateMachine.getBucket().getData().iterator()) {
			if (exclusiveStartKey.size() > 0) {
				it.seekForPrev(exclusiveStartKey.copyIf());
			} else {
				// 分桶过程中，可能存在Last之后的数据，必须根据Last的情况定位，不能直接使用seekToLast。
				var lastKey = stateMachine.getBucket().getBucketMeta().getKeyLast();
				if (lastKey.size() > 0)
					it.seekForPrev(lastKey.copyIf());
				else
					it.seekToLast();
			}
			if (it.isValid()) {
				var firstKey = it.key();
				//noinspection EqualsBetweenInconvertibleTypes
				if (exclusiveStartKey.size() > 0 && exclusiveStartKey.equals(firstKey))
					it.prev(); // skip exclusive key if need.
			}

			var count = proposeLimit;
			for (; it.isValid() && count > 0; it.prev(), count--) {
				var key = new Binary(it.key());
				fill.run(key, it);
			}

			return !it.isValid();
		}
	}

	private boolean walk(Binary exclusiveStartKey, int proposeLimit, boolean desc,
						 Action2<Binary, RocksIterator> fill) throws Exception {
		if (desc) {
			return walkDesc(exclusiveStartKey, proposeLimit, fill);
		}

		try (var it = stateMachine.getBucket().getData().iterator()) {
			if (exclusiveStartKey.size() > 0)
				it.seek(exclusiveStartKey.copyIf());
			else
				it.seekToFirst();

			if (it.isValid()) {
				var firstKey = it.key();
				//noinspection EqualsBetweenInconvertibleTypes
				if (exclusiveStartKey.size() > 0 && exclusiveStartKey.equals(firstKey))
					it.next(); // skip exclusive key if need.
			}

			var count = proposeLimit;
			var bucketEnd = false;
			var keyLast = stateMachine.getBucket().getBucketMeta().getKeyLast();
			for (; it.isValid() && count > 0; it.next(), count--) {
				var key = new Binary(it.key());
				if (keyLast.size() > 0 && key.compareTo(keyLast) >= 0) {
					// 分桶中刚完成时，数据可能超过Last，此时应该检查出来并结束walk。
					bucketEnd = true;
					break;
				}
				fill.run(key, it);
			}

			return bucketEnd || !it.isValid();
		}
	}

	@Override
	protected long ProcessWalkRequest(Walk r) throws Exception {
		if (!stateMachine.getBucket().inBucket(r.Argument.getExclusiveStartKey())) {
			r.Result.setBucketRefuse(true);
			r.SendResult();
			return 0;
		}
		var bucketEnd = walk(
				r.Argument.getExclusiveStartKey(),
				r.Argument.getProposeLimit(),
				r.Argument.isDesc(),
				(key, it) -> r.Result.getKeyValues().add(new BWalkKeyValue.Data(key, new Binary(it.value()))));
		r.Result.setBucketEnd(bucketEnd);
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessWalkKeyRequest(WalkKey r) throws Exception {
		if (!stateMachine.getBucket().inBucket(r.Argument.getExclusiveStartKey())) {
			r.Result.setBucketRefuse(true);
			r.SendResult();
			return 0;
		}

		var bucketEnd = walk(
				r.Argument.getExclusiveStartKey(),
				r.Argument.getProposeLimit(),
				r.Argument.isDesc(),
				(key, it) -> r.Result.getKeys().add(key));
		r.Result.setBucketEnd(bucketEnd);
		r.SendResult();
		return 0;
	}

	class RaftAgentNetClient extends Agent.NetClient {

		public RaftAgentNetClient(Agent agent, String name, Config config) {
			super(agent, name, config);
		}

		@Override
		public <P extends Protocol<?>> void dispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
																ProtocolFactoryHandle<?> factoryHandle) {
			raft.getUserThreadExecutor().execute(() -> {
				try {
					responseHandle.handle(rpc);
				} catch (Throwable e) { // run handle. 必须捕捉所有异常。logger.error
					logger.error("Agent.NetClient.dispatchRpcResponse", e);
				}
			});
		}

		@Override
		public void dispatchProtocol(@NotNull Protocol<?> p, @NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
			// 虚拟线程创建太多Critical线程反而容易卡,以后考虑跑另个虚拟线程池里
			if (p.getTypeId() == Zeze.Raft.LeaderIs.TypeId_) {
				Task.getCriticalThreadPool().execute(() -> Task.call(() -> p.handle(this, factoryHandle), "InternalRequest"));
			} else {
				raft.getUserThreadExecutor().execute(() -> Task.executeUnsafe(
						() -> p.handle(this, factoryHandle),
						p,
						null,
						null,
						DispatchMode.Normal));
			}
		}

		@Override
		public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) throws Exception {
			// 不支持事务，传统dispatch即可。
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			p.dispatch(this, factoryHandle);
		}
	}

	public void tryStartSplit(boolean isMove) throws Exception {
		if (!raft.isLeader())
			return;

		var bucket = stateMachine.getBucket();
		var splitting = bucket.getSplittingMeta();
		if (null != splitting) {
			logger.info("start but in splitting or ending. {}->{}", formatMeta(bucket.getBucketMeta()), formatMeta(splitting));
			return; // splitting
		}

		startSplit(isMove);
	}

	private void onFollowerReceiveKeepAlive() {
		// 集群中的leader已经开始工作。自己是follower.
		stateMachine.setLoadSwitch(true);
	}

	private void recoverSplitting() throws Exception {
		if (!raft.isLeader())
			return;

		stateMachine.setLoadSwitch(true);
		var bucket = stateMachine.getBucket();
		if (null != bucket.getSplittingMeta()) {
			var bucketKeyFirst = bucket.getSplittingMeta().getKeyFirst();
			boolean isMove = bucketKeyFirst.size() == 0; // keyFirst 为空肯定是move
			if (!isMove) {
				// keyFirst不为空，还不能确认就是split，需要进一步判断keyFirst确实是第一条数据。
				try (var it = bucket.getData().iterator()) {
					it.seekToFirst();
					// keyFirst 就是第一条数据，那么当前还是move。
					//noinspection EqualsBetweenInconvertibleTypes
					isMove = it.isValid() && bucketKeyFirst.equals(it.key());
				}
			}
			startSplit(isMove);
		}
	}

	private volatile long splitSerialNo;
	private volatile Dbh2Agent dbh2Splitting;

	private RocksIterator locateFirst() {
		var bucket = stateMachine.getBucket();
		var it = bucket.getData().iterator();
		it.seekToFirst();
		if (it.isValid())
			return it;
		it.close();
		return null;
	}

	private RocksIterator locateMiddle() throws RocksDBException {
		var bucket = stateMachine.getBucket();
		var it = bucket.getData().iterator();
		var keyNumbers = bucket.getData().getKeyNumbers();
		var count = keyNumbers / 2;
		if (count <= 0) // 这里可以考虑配置一个较大的值，即记录数很少的时候不分桶。
			return null;
		for (it.seekToFirst(); it.isValid() && count > 0; it.next(), --count) {
			// searching middle
		}
		if (!it.isValid()) {
			it.close();
			throw new RocksDBException("middle key not found.");
		}
		logger.info("splitting start locateMiddle keyNumbers={} middle={} {}",
				keyNumbers, new Binary(it.key()),
				formatMeta(stateMachine.getBucket().getBucketMeta()));
		return it;
	}

	private RocksIterator locateMiddle(Binary middleKey) {
		var it = stateMachine.getBucket().getData().iterator();
		it.seek(Database.copyIf(middleKey.bytesUnsafe(), middleKey.getOffset(), middleKey.size()));
		if (!it.isValid()) {
			it.close();
			return null;
		}
		return it;
	}

	private static void performPrepareQueue(ConcurrentLinkedQueue<Action0> tmpQueue) {
		if (null != tmpQueue) {
			for (var trans : tmpQueue) {
				try {
					trans.run();
				} catch (Exception e) {
					logger.error("", e);
				}
			}
		}
	}

	// 开始分桶流程有两个线程需要访问：timer & raft.UserThreadExecutor
	private synchronized void startSplit(boolean isMove) throws Exception {
		if (!getRaft().isLeader())
			return;

		// 后面需要在lambda中传递系列号作为上下文，使用成员变量是不是会跟随变化？
		var serialNo = manager.atomicSerialNo.incrementAndGet();
		splitSerialNo = serialNo;
		var bucket = stateMachine.getBucket();
		RocksIterator it = null;
		try {
			var splitting = bucket.getSplittingMeta(); // 对于timer，这个会调用两次。
			if (null == splitting) {
				// 第一次开始分桶，准备阶段。
				// 这个阶段在timer回调中执行，可以同步调用一些网络接口。
				// 先去manager查一下可用的manager是否够，简单判断，不原子化。
				if (manager.getMasterAgent().checkFreeManager() < dbh2Config.getRaftClusterCount()) {
					logger.warn("splitting not enough free manager. isMove={}", isMove);
					return;
				}
				// 上一次分桶结束的deleteRange可能还没compact，此时keyNumbers不准确，这里总是执行一次。
				bucket.getData().compact();

				it = isMove ? locateFirst() : locateMiddle();
				if (null == it) {
					logger.info("splitting break start: it is null. isMove={}", isMove);
					return; // empty？不需要执行后续操作。break progress.
				}
				var newMeta = stateMachine.getBucket().getBucketMeta().copy();
				newMeta.setRaftConfig("");
				if (!isMove)
					newMeta.setKeyFirst(new Binary(it.key()));
				splitting = manager.getMasterAgent().createSplitBucket(newMeta);

				// 设置分桶进行中的标记到raft集群中。
				getRaft().appendLog(new LogSetSplittingMeta(splitting));
				// 创建到分桶目标的客户端。
				logger.info("splitting start... isMove={} {}->{}",
						isMove, formatMeta(bucket.getBucketMeta()), formatMeta(splitting));
			}

			// 重启的时候，需要重建到分桶的连接。
			if (null == dbh2Splitting) {
				dbh2Splitting = new Dbh2Agent(splitting.getRaftConfig(), RaftAgentNetClient::new);
				dbh2Splitting.getRaftAgent().setPendingLimit(Integer.MAX_VALUE);
			}

			var server = (Dbh2RaftServer)getRaft().getServer();
			performPrepareQueue(server.takePrepareQueue());

			if (null == it) {
				// 重新开始分桶时走这个分支，根据上次找到的middle，定位it。
				it = isMove ? locateFirst() : locateMiddle(splitting.getKeyFirst());
				if (null == it) {
					logger.info("splitting break restart: it is null. isMove={}", isMove);
					return;
				}
				logger.info("splitting restart... isMove={} {}->{}",
						isMove, formatMeta(bucket.getBucketMeta()), formatMeta(splitting));
			}

			// 开始同步数据，这个阶段对于rocks时同步访问的，对于网络是异步的。
			var puts = buildSplitPut(it);
			var fit = it;
			dbh2Splitting.getRaftAgent().send(puts, (p) -> splitPutNext(isMove, (SplitPut)p, fit, serialNo));
			it = null;
		} finally {
			if (null != it)
				it.close();
		}
	}

	private SplitPut buildSplitPut(RocksIterator it) {
		var r = new SplitPut();
		r.Argument.setFromTransaction(false);
		var count = dbh2Config.getSplitPutCount();
		for (; it.isValid() && count > 0; it.next(), --count) {
			r.Argument.getPuts().put(new Binary(it.key()), new Binary(it.value()));
		}
		return r;
	}

	public long splitPutNext(boolean isMove, SplitPut r, RocksIterator it, long serialNo) {
		try {
			var hasError = r.getResultCode() != 0;
			if (!raft.isLeader() || dbh2Splitting == null || serialNo != splitSerialNo) {
				it.close();
				if (hasError)
					startSplit(isMove);
				return 0;
			}

			if (hasError) {
				startSplit(isMove);
				return 0;
			}

			var puts = buildSplitPut(it);
			if (puts.Argument.getPuts().isEmpty()) {
				it.close();

				blockPrepareUntilNoTransaction(isMove);
				return 0; // split done.
			}

			dbh2Splitting.getRaftAgent().send(puts, (p) -> splitPutNext(isMove, (SplitPut)p, it, serialNo));
		} catch (Exception ex) {
			logger.error("isMove={}", isMove, ex);
		}
		return 0;
	}

	private void blockPrepareUntilNoTransaction(boolean isMove) {
		var meta = stateMachine.getBucket().getBucketMeta();
		var splittingMeta = stateMachine.getBucket().getSplittingMeta();
		logger.info("splitting end ... isMove={} {}->{}",
				isMove, formatMeta(meta), formatMeta(splittingMeta));

		// 截住新的事务请求。
		var server = (Dbh2RaftServer)getRaft().getServer();
		server.setupPrepareQueue();

		// 设置一个超时，每秒放行一次。
		Task.scheduleUnsafe(1000,
				() -> getRaft().getUserThreadExecutor().execute(() -> consumePrepareAndBlockAgain(isMove)));

		// 在队列中增加endSplit启动任务，先要处理完队列中的请求。
		// 此时PrepareBatch已经被拦截，但是还有CommitBatch,UndoBatch等其他请求在处理。
		// setupHandleIfNoTransaction 将在没有进行中的事务时触发。
		getRaft().getUserThreadExecutor().execute(() ->
				stateMachine.setupOneShotIfNoTransaction(() -> endSplit0(isMove)));
	}

	private void consumePrepareAndBlockAgain(boolean isMove) {
		if (stateMachine.hasNoTransactionHandle()) {
			// 还在等待事务清空，此时...
			// 处理一下累积的Prepare请求，暂时放行一下。
			var server = (Dbh2RaftServer)getRaft().getServer();
			performPrepareQueue(server.takePrepareQueue());
			blockPrepareUntilNoTransaction(isMove);
		}
	}

	private void endSplit0(boolean isMove) {
		// 第一步，设置新桶的meta
		dbh2Splitting.setBucketMetaAsync(stateMachine.getBucket().getSplittingMeta(), (p) -> endSplit1(p, isMove));
	}

	private long endSplit1(Protocol<?> p, boolean isMove) {
		var r = (SetBucketMeta)p;
		if (r.getResultCode() != 0) {
			try {
				startSplit(isMove);
			} catch (Exception e) {
				logger.error("isMove={}", isMove, e);
			}
			return 0;
		}

		var bucket = stateMachine.getBucket();
		if (isMove) {
			getRaft().appendLog(new LogEndMove(bucket.getSplittingMeta()),
					(raftLog, result) -> endSplit2(raftLog, result, true));
		} else {
			// 【原子化】设置源桶状态（修改源桶Meta；保存新桶Meta到历史中；删除分桶Meta）。
			var from = bucket.getBucketMeta().copy();
			var to = bucket.getSplittingMeta();
			from.setKeyLast(to.getKeyFirst());
			getRaft().appendLog(new LogEndSplit(from, to),
					(raftLog, result) -> endSplit2(raftLog, result, false));
		}
		return 0;
	}

	private void endSplit2(RaftLog raftLog, Boolean result, boolean isMove) {
		if (!result) {
			try {
				startSplit(isMove);
			} catch (Exception e) {
				logger.error("isMove={}", isMove, e);
			}
			return;
		}

		// 【此时进入拒绝模式】
		// 【此时进入拒绝模式】
		// 【此时进入拒绝模式】
		var server = (Dbh2RaftServer)getRaft().getServer();
		performPrepareQueue(server.takePrepareQueue());

		// 关闭到新桶连接。
		try {
			dbh2Splitting.close();
		} catch (Exception ex) {
			logger.error("", ex);
		}
		dbh2Splitting = null;

		var meta = stateMachine.getBucket().getBucketMeta();
		if (isMove) {
			var endMove = (LogEndMove)raftLog.getLog();
			manager.getMasterAgent().endMoveWithRetryAsync(endMove.getTo());
		} else {
			// 可以安全的发布新旧桶的信息到Master了。
			var endSplit = (LogEndSplit)raftLog.getLog();
			manager.getMasterAgent().endSplitWithRetryAsync(endSplit.getFrom(), endSplit.getTo());
		}
		logger.info("splitting end done. isMove={} {}", isMove, formatMeta(meta));
	}

	public void onCommitBatch(Dbh2Transaction txn) {
		var splittingMeta = stateMachine.getBucket().getSplittingMeta();
		if (splittingMeta == null || dbh2Splitting == null)
			return;

		var r = new SplitPut();
		r.Argument.setFromTransaction(true);

		// 如果修改的记录落在分桶目标桶中，则同步过去。
		for (var e : txn.getBatch().getPuts().entrySet()) {
			if (splittingMeta.getKeyFirst().compareTo(e.getKey()) <= 0)
				r.Argument.getPuts().put(e.getKey(), e.getValue());
		}
		for (var delete : txn.getBatch().getDeletes()) {
			if (splittingMeta.getKeyFirst().compareTo(delete) <= 0)
				r.Argument.getPuts().put(delete, Binary.Empty);
		}

		// 事务同步流程不能重试，因为提交之后就有新的并发事务过来，而这里是异步的，重试时数据可能不是最新的了。
		dbh2Splitting.getRaftAgent().send(r, (p) -> {
			try {
				if (r.getResultCode() != 0)
					recoverSplitting(); // restart split.
				return 0;
			} catch (Exception ex) {
				logger.error("", ex);
				return Procedure.Exception;
			}
		});
	}

	@Override
	protected long ProcessSplitPutRequest(SplitPut r) throws Exception {
		raft.appendLog(new LogSplitPut(r),
				(raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));
		return 0;
	}

}
