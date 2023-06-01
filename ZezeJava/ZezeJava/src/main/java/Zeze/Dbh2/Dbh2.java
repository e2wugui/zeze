package Zeze.Dbh2;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.CommitBatch;
import Zeze.Builtin.Dbh2.Get;
import Zeze.Builtin.Dbh2.KeepAlive;
import Zeze.Builtin.Dbh2.PrepareBatch;
import Zeze.Builtin.Dbh2.SetBucketMeta;
import Zeze.Builtin.Dbh2.SplitPut;
import Zeze.Builtin.Dbh2.UndoBatch;
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
	private final Dbh2Config config = new Dbh2Config();
	private final Raft raft;
	private final Dbh2StateMachine stateMachine;
	private final Dbh2Manager manager;
	private final Locks locks = new Locks();

	public Locks getLocks() {
		return locks;
	}

	// 性能统计。
	public static String formatMeta(BBucketMeta.Data meta) {
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
			} else if (p.getTypeId() == Get.TypeId_) {
				super.dispatchRaftRequest(p, func, name, cancel, mode);
			} else {
				raft.getUserThreadExecutor().execute(() -> Task.call(func, p));
			}
		}
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

	public Dbh2Config getConfig() {
		return config;
	}

	public Dbh2(Dbh2Manager manager, String raftName, RaftConfig raftConf, Config config, boolean writeOptionSync) {
		this.manager = manager;

		if (config == null)
			config = new Config().addCustomize(this.config).loadAndParse();

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
		raft.appendLog(log, r.Result, (raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException)); // result is empty
		return 0;
	}

	@Override
	protected long ProcessGetRequest(Zeze.Builtin.Dbh2.Get r) throws RocksDBException, InterruptedException {
		stateMachine.counterGet.incrementAndGet();
		var lock = getLocks().get(r.Argument.getKey());
		lock.lock(this);
		try {
			// 直接读取数据库。是否可以读取由raft控制。raft启动时有准备阶段。
			var bucket = stateMachine.getBucket();
			if (!bucket.inBucket(r.Argument.getDatabase(), r.Argument.getTable(), r.Argument.getKey()))
				return errorCode(eBucketMissmatch);
			var value = bucket.get(r.Argument.getKey());
			if (null == value)
				r.Result.setNull(true);
			else {
				r.Result.setValue(value);
				stateMachine.sizeGet.addAndGet(value.size());
			}
			r.SendResult();
		} finally {
			lock.unlock();
		}
		return 0;
	}

	@Override
	protected long ProcessKeepAliveRequest(KeepAlive r) throws Exception {
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessPrepareBatchRequest(PrepareBatch r) throws Exception {
		//var tid = stateMachine.getTidAllocator().next(stateMachine);
		// lock
		var txn = new Dbh2Transaction(this, r.Argument.getBatch());
		try {
			// save txn
			if (null != stateMachine.getTransactions().putIfAbsent(r.Argument.getBatch().getTid(), txn))
				return errorCode(eDuplicateTid);
			// check inBucket
			if (!stateMachine.getBucket().inBucket(r.Argument.getDatabase(), r.Argument.getTable()))
				return errorCode(eBucketMissmatch);

			var refused = r.Result; // 对于puts可以考虑只传key，网络占用少一些。
			var splitHistory = stateMachine.getBucket().getSplitMetaHistory();
			for (var e : r.Argument.getBatch().getPuts().entrySet()) {
				var key = e.getKey();
				var value = e.getValue();
				if (!stateMachine.getBucket().inBucket(key)) {
					var locate = splitHistory.locate(key);

					if (null == locate || locate.getKeyFirst().equals(stateMachine.getBucket().getMeta().getKeyFirst()))
						return errorCode(eBucketNotFound); // 找不到或者又找到了自己。

					var batches = refused.getRefused().computeIfAbsent(locate.getRaftConfig(), (__) -> new BBatch.Data());
					batches.getPuts().put(key, value);
				}
			}
			for (var del : r.Argument.getBatch().getDeletes()) {
				if (!stateMachine.getBucket().inBucket(del)) {
					var locate = splitHistory.locate(del);

					if (null == locate || locate.getKeyFirst().equals(stateMachine.getBucket().getMeta().getKeyFirst()))
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
			getRaft().appendLog(new LogPrepareBatch(r), (raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));
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
		getRaft().appendLog(new LogCommitBatch(r), (raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));
		return 0;
	}

	@Override
	protected long ProcessUndoBatchRequest(UndoBatch r) throws Exception {
		getRaft().appendLog(new LogUndoBatch(r), (raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));
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
				raft.getUserThreadExecutor().execute(() -> Task.runUnsafe(
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

	public void tryStartSplit() throws Exception {
		if (!raft.isLeader())
			return;

		var bucket = stateMachine.getBucket();
		var splitting = bucket.getSplittingMeta();
		if (null != splitting) {
			logger.info("start but in splitting. {}->{}", formatMeta(bucket.getMeta()), formatMeta(splitting));
			return; // splitting
		}

		startSplit();
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
		if (null != bucket.getSplittingMeta())
			startSplit();
	}

	private volatile long splitSerialNo;
	private volatile Dbh2Agent dbh2Splitting;

	private RocksIterator locateMiddle() throws RocksDBException {
		var bucket = stateMachine.getBucket();
		var it = bucket.getTData().iterator();
		var keyNumbers = bucket.getTData().getKeyNumbers();
		var count = keyNumbers / 2;
		for (it.seekToFirst(); it.isValid() && count > 0; it.next(), --count) {
			// searching middle
		}
		if (!it.isValid()) {
			it.close();
			throw new RocksDBException("middle key not found.");
		}
		logger.info("splitting start locateMiddle keyNumbers={} middle={} {}",
				keyNumbers, new Binary(it.key()),
				formatMeta(stateMachine.getBucket().getMeta()));
		return it;
	}

	private RocksIterator locateMiddle(Binary middleKey) throws RocksDBException {
		var it = stateMachine.getBucket().getTData().iterator();
		it.seek(Database.copyIf(middleKey.bytesUnsafe(), middleKey.getOffset(), middleKey.size()));
		if (!it.isValid()) {
			it.close();
			throw new RocksDBException("middle key not found.");
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
	private synchronized void startSplit() throws Exception {
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
				if (manager.getMasterAgent().checkFreeManager() < config.getRaftClusterCount()) {
					logger.warn("not enough free manager.");
					return;
				}
				var metaCopy = bucket.getMeta().copy();
				bucket.getTData().compact(); // 上一次分桶结束的deleteRange可能还没compact，此时keyNumbers不准确，这里总是执行一次。
				it = locateMiddle();
				metaCopy.setKeyFirst(new Binary(it.key()));
				metaCopy.setRaftConfig("");
				splitting = manager.getMasterAgent().createSplitBucket(metaCopy);

				// 设置分桶进行中的标记到raft集群中。
				getRaft().appendLog(new LogSetSplittingMeta(splitting));
				// 创建到分桶目标的客户端。
				logger.info("splitting start... {}->{}", formatMeta(bucket.getMeta()), formatMeta(splitting));
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
				it = locateMiddle(splitting.getKeyFirst());
				logger.info("splitting restart... {}->{}", formatMeta(bucket.getMeta()), formatMeta(splitting));
			}

			// 开始同步数据，这个阶段对于rocks时同步访问的，对于网络是异步的。
			var puts = buildSplitPut(it);
			var fit = it;
			dbh2Splitting.getRaftAgent().send(puts, (p) -> splitPutNext((SplitPut)p, fit, serialNo));
			it = null;
		} finally {
			if (null != it)
				it.close();
		}
	}

	private SplitPut buildSplitPut(RocksIterator it) {
		var r = new SplitPut();
		r.Argument.setFromTransaction(false);
		var count = config.getSplitPutCount();
		for (; it.isValid() && count > 0; it.next(), --count) {
			r.Argument.getPuts().put(new Binary(it.key()), new Binary(it.value()));
		}
		return r;
	}

	public long splitPutNext(SplitPut r, RocksIterator it, long serialNo) {
		try {
			var hasError = r.getResultCode() != 0;
			if (!raft.isLeader() || dbh2Splitting == null || serialNo != splitSerialNo) {
				it.close();
				if (hasError)
					startSplit();
				return 0;
			}

			if (hasError) {
				startSplit();
				return 0;
			}

			var puts = buildSplitPut(it);
			if (puts.Argument.getPuts().isEmpty()) {
				it.close();

				blockPrepareUntilNoTransaction();
				return 0; // split done.
			}

			dbh2Splitting.getRaftAgent().send(puts, (p) -> splitPutNext((SplitPut)p, it, serialNo));
		} catch (Exception ex) {
			logger.error("", ex);
		}
		return 0;
	}

	private void blockPrepareUntilNoTransaction() {
		var meta = stateMachine.getBucket().getMeta();
		var splittingMeta = stateMachine.getBucket().getSplittingMeta();
		logger.info("splitting end ... {}->{}", formatMeta(meta), formatMeta(splittingMeta));

		// 截住新的事务请求。
		var server = (Dbh2RaftServer)getRaft().getServer();
		server.setupPrepareQueue();

		// 设置一个超时，每秒放行一次。
		Task.scheduleUnsafe(1000,
				() -> getRaft().getUserThreadExecutor().execute(this::consumePrepareAndBlockAgain));

		// 在队列中增加endSplit启动任务，先要处理完队列中的请求。
		// 此时PrepareBatch已经被拦截，但是还有CommitBatch,UndoBatch等其他请求在处理。
		// setupHandleIfNoTransaction 将在没有进行中的事务时触发。
		getRaft().getUserThreadExecutor().execute(() ->
				stateMachine.setupHandleIfNoTransaction(this::endSplit0));
	}

	private void consumePrepareAndBlockAgain() {
		if (stateMachine.hasNoTransactionHandle()) {
			// 还在等待事务清空，此时...
			// 处理一下累积的Prepare请求，暂时放行一下。
			var server = (Dbh2RaftServer)getRaft().getServer();
			performPrepareQueue(server.takePrepareQueue());
			blockPrepareUntilNoTransaction();
		}
	}

	private void endSplit0() {
		// 第一步，设置新桶的meta
		dbh2Splitting.setBucketMetaAsync(stateMachine.getBucket().getSplittingMeta(), this::endSplit1);
	}

	private long endSplit1(Protocol<?> p) {
		var r = (SetBucketMeta)p;
		if (r.getResultCode() != 0) {
			try {
				startSplit();
			} catch (Exception e) {
				logger.error("", e);
			}
			return 0;
		}

		// 【原子化】设置源桶状态（修改源桶Meta；保存新桶Meta到历史中；删除分桶Meta）。
		var bucket = stateMachine.getBucket();
		var from = bucket.getMeta().copy();
		var to = bucket.getSplittingMeta();
		from.setKeyLast(to.getKeyFirst());

		getRaft().appendLog(new LogEndSplit(from, to), this::endSplit2);
		return 0;
	}

	private void endSplit2(RaftLog raftLog, Boolean result) {
		if (!result) {
			try {
				startSplit();
			} catch (Exception e) {
				logger.error("", e);
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

		// 可以安全的发布新旧桶的信息到Master了。
		var endSplit = (LogEndSplit)raftLog.getLog();
		manager.getMasterAgent().endSplitWithRetryAsync(endSplit.getFrom(), endSplit.getTo());
		var meta = stateMachine.getBucket().getMeta();
		logger.info("splitting end done. {}", formatMeta(meta));
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
					startSplit(); // restart split.
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
