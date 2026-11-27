package Zeze.Dbh2;

import java.util.Objects;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BBucketMeta.Data;
import Zeze.Builtin.Dbh2.BWalk;
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
import Zeze.Util.FastLock;
import Zeze.Util.FuncLong;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.jspecify.annotations.Nullable;

/**
 * Dbh2 单桶 Raft 服务节点：负责一个桶的数据存储、协议处理与分桶/迁移流程。
 */
public class Dbh2 extends AbstractDbh2 implements AutoCloseable {
	private static final Logger logger = LogManager.getLogger(Dbh2.class);
	private final Dbh2Config dbh2Config = new Dbh2Config();
	private Raft raft;
	private Dbh2StateMachine stateMachine;
	private final @Nullable Dbh2Manager manager;
	private final Locks locks = new Locks();

	public Locks getLocks() {
		return locks;
	}

	public static String formatMeta(@Nullable Data meta) {
		return meta == null ? "" : meta.getDatabaseName() + '.' + meta.getTableName() +
				'[' + meta.getKeyFirst() + ',' + meta.getKeyLast() + ']';
	}

	public class Dbh2RaftServer extends Zeze.Raft.Server {
		public Dbh2RaftServer(Raft raft, String name, Config config) {
			super(raft, name, config);
			setInstanceName(raft.getName());
		}

		private final FastLock prepareQueueLock = new FastLock();
		private @Nullable ConcurrentLinkedQueue<Action0> prepareQueue;

		public void setupPrepareQueue() {
			prepareQueueLock.lock();
			try {
				if (null == prepareQueue)
					prepareQueue = new ConcurrentLinkedQueue<>();
			} finally {
				prepareQueueLock.unlock();
			}
		}

		public @Nullable ConcurrentLinkedQueue<Action0> takePrepareQueue() {
			prepareQueueLock.lock();
			try {
				var tmp = prepareQueue;
				prepareQueue = null;
				return tmp;
			} finally {
				prepareQueueLock.unlock();
			}
		}

		// 原子释放拦截队列并交由 raft 串行执行器排空：在释放 prepareQueueLock 之前提交
		// executeUserTask，保证队列内旧请求严格先于解锁后新到的 PrepareBatch 串行执行，
		// 且绝不在 raft.lock 线程上同步执行用户 RPC。
		public void releasePrepareQueue() {
			prepareQueueLock.lock();
			try {
				var tmp = prepareQueue;
				prepareQueue = null;
				if (null != tmp && !tmp.isEmpty())
					raft.executeUserTask(() -> performPrepareQueue(tmp));
			} finally {
				prepareQueueLock.unlock();
			}
		}

		@Override
		public <P extends Protocol<?>> void dispatchRaftRpcResponse(P p, ProtocolHandle<P> responseHandle,
																	ProtocolFactoryHandle<?> factoryHandle) {
			Raft.executeImportantTask(() -> {
				try {
					responseHandle.handle(p);
				} catch (Exception e) {
					logger.error("", e);
				}
			});
		}

		@Override
		public void dispatchRaftRequest(Protocol<?> p, FuncLong func, String name, Action0 cancel,
										DispatchMode mode) throws Exception {
			if (isQueryRequest(p.getTypeId())) {
				// 允许Get请求并发：query不注册事务、不参与门槛序，锁外常规派发。
				super.dispatchRaftRequest(p, func, name, cancel, mode);
				return;
			}

			// 拦截检查与提交串行执行器必须在 prepareQueueLock 内原子完成，确保不晚于 setupPrepareQueue 漏入迁出写入
			prepareQueueLock.lock();
			try {
				if (null != prepareQueue && isPrepareRequest(p.getTypeId())) {
					prepareQueue.add(() -> TaskSpec.ofFunc(func, p).call());
					return;
				}
				raft.executeUserTask(() -> TaskSpec.ofFunc(func, p).call());
			} finally {
				prepareQueueLock.unlock();
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

	// 死桶不拥有键域，始终允许重定向，避免与单字节哨兵键误判自指
	private static boolean isLocateSelf(BBucketMeta.Data bucketMeta, BBucketMeta.Data locate) {
		if (Bucket.DeadBucketMetaBound.equals(bucketMeta.getKeyFirst())
				&& Bucket.DeadBucketMetaBound.equals(bucketMeta.getKeyLast()))
			return false;
		return locate.getKeyFirst().equals(bucketMeta.getKeyFirst());
	}

	// 新建桶在 SetBucketMeta 生效前无元数据，查询与事务入口返回 eBucketNotReady 供调用方重试
	private boolean isBucketNotReady() {
		//noinspection DataFlowIssue
		return stateMachine.getBucket().getBucketMeta() == null;
	}

	public Raft getRaft() {
		return raft;
	}

	public Dbh2StateMachine getStateMachine() {
		return stateMachine;
	}

	public @Nullable Dbh2Manager getManager() {
		return manager;
	}

	public Dbh2Config getDbh2Config() {
		return dbh2Config;
	}

	public Dbh2(@Nullable Dbh2Manager manager, String raftName, @Nullable RocksDatabase database,
				RaftConfig raftConf, @Nullable Config config,
				TaskOneByOneByKey taskOneByOne) {
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
			// 桶存储写选项由RaftConfig.SyncWrite决定（默认普通写），与LogSequence同源同值。
			var writeOptions = raftConf.isSyncWrite()
					? RocksDatabase.getSyncWriteOptions() : RocksDatabase.getDefaultWriteOptions();
			stateMachine.setWriteOptions(writeOptions);
			raft = new Raft(stateMachine, raftName, database, raftConf, config,
					"Zeze.Dbh2.Server", Dbh2RaftServer::new, taskOneByOne);
			raft.getServer().getSocketOptions().setInputBufferMaxProtocolSize(100 * 1024 * 1024);
			raft.getServer().getSocketOptions().setOutputBufferMaxSize(100 * 1024 * 1024);
			raft.getRaftConfig().setSnapshotCommitDelayed(true);
			logger.info("newRaft: {}", raft.getName());
			stateMachine.openBucket();

			RegisterProtocols(raft.getServer());
			raft.setOnLeaderReady(this::recoverSplitting);
			raft.setOnFollowerReceiveKeepAlive(this::onFollowerReceiveKeepAlive);
			raft.getServer().start();
		} catch (Exception ex) {
			// 半失败逆序清理已初始化的 raft 与状态机句柄，避免目录被占用
			if (null != raft) {
				try {
					raft.shutdown();
				} catch (Exception e) {
					ex.addSuppressed(e);
					logger.error("constructor cleanup: raft.shutdown fail. {}", raftName, e);
				}
			}
			if (null != stateMachine) {
				try {
					stateMachine.close();
				} catch (Exception e) {
					ex.addSuppressed(e);
					logger.error("constructor cleanup: stateMachine.close fail. {}", raftName, e);
				}
			}
			throw new RuntimeException(ex);
		}
	}

	private volatile boolean closed;

	@Override
	public void close() {
		if (closed)
			return;
		closed = true;
		logger.info("closeRaft: {}", raft.getName());
		// 逐项独立清理并在结束后重抛首个异常，确保 raft 失败时仍释放 stateMachine 与分桶代理句柄
		Exception first = null;
		try {
			raft.shutdown();
		} catch (Exception e) {
			first = e;
			logger.error("closeRaft: raft.shutdown fail. {}", raft.getName(), e);
		}
		try {
			stateMachine.close();
		} catch (Exception e) {
			logger.error("closeRaft: stateMachine.close fail. {}", raft.getName(), e);
			if (null == first)
				first = e;
		}
		// 必须在 raft.shutdown 之后关闭分桶代理，避免超时回调以 leader 身份重入 startSplit
		var splittingAgent = dbh2Splitting;
		dbh2Splitting = null;
		if (null != splittingAgent) {
			try {
				splittingAgent.close();
			} catch (Exception e) {
				logger.error("closeRaft: dbh2Splitting.close fail. {}", raft.getName(), e);
				if (null == first)
					first = e;
			}
		}
		if (null != first)
			throw new RuntimeException(first);
	}

	@Override
	protected long ProcessSetBucketMetaRequest(SetBucketMeta r) {
		var log = new LogSetBucketMeta(r);
		raft.appendLog(log, r.Result, (raftLog, result)
				-> r.SendResultCode(result ? 0 : Procedure.CancelException));
		return 0;
	}

	@Override
	protected long ProcessGetRequest(Zeze.Builtin.Dbh2.Get r) throws RocksDBException {
		if (isBucketNotReady())
			return errorCode(eBucketNotReady);
		stateMachine.counterGet.incrementAndGet();
		// 直接读取数据库。是否可以读取由raft控制。raft启动时有准备阶段。
		var bucket = Objects.requireNonNull(stateMachine.getBucket());
		// 读前快照 meta；miss 时复核引用，若期间发生收尾换代则返回 eBucketMismatch 重路由（endSplit/endMove 先切 meta 后 deleteToEnd）
		var metaOnCheck = Objects.requireNonNull(bucket.getBucketMeta());
		if (!bucket.inBucket(metaOnCheck, r.Argument.getDatabase(), r.Argument.getTable(), r.Argument.getKey()))
			return errorCode(eBucketMismatch);
		var value = bucket.get(r.Argument.getKey());
		if (null == value || value.size() == 0) { // 空值为分桶墓碑标记，视为不存在
			var metaOnMiss = bucket.getBucketMeta();
			if (metaOnMiss != metaOnCheck
					|| !bucket.inBucket(metaOnMiss, r.Argument.getDatabase(), r.Argument.getTable(), r.Argument.getKey()))
				return errorCode(eBucketMismatch);
			r.Result.setNull(true);
		} else {
			r.Result.setValue(value);
			stateMachine.sizeGet.addAndGet(value.size());
		}
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessKeepAliveRequest(KeepAlive r) {
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessPrepareBatchRequest(PrepareBatch r) throws Exception {
		if (isBucketNotReady())
			return errorCode(eBucketNotReady);
		var tid = r.Argument.getBatch().getTid();
		if (stateMachine.getTransactions().containsKey(tid))
			return errorCode(eDuplicateTid);
		var bucket = Objects.requireNonNull(stateMachine.getBucket());
		if (!bucket.inBucket(r.Argument.getDatabase(), r.Argument.getTable()))
			return errorCode(eBucketMismatch);

		var refused = r.Result;
		var splitHistory = bucket.getSplitMetaHistory();
		for (var e : r.Argument.getBatch().getPuts().entrySet()) {
			var key = e.getKey();
			var value = e.getValue();
			if (!bucket.inBucket(key)) {
				var locate = splitHistory.locate(key);

				if (null == locate || isLocateSelf(Objects.requireNonNull(bucket.getBucketMeta()), locate))
					return errorCode(eBucketNotFound); // 找不到或者又找到了自己。

				var batches = refused.getRefused().computeIfAbsent(locate.getRaftConfig(), (__) -> new BBatch.Data());
				batches.getPuts().put(key, value);
			}
		}
		for (var del : r.Argument.getBatch().getDeletes()) {
			if (!bucket.inBucket(del)) {
				var locate = splitHistory.locate(del);

				if (null == locate || isLocateSelf(Objects.requireNonNull(bucket.getBucketMeta()), locate))
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

		var txn = new Dbh2Transaction(this, r.Argument.getBatch());
		try {
			if (null != stateMachine.getTransactions().putIfAbsent(tid, txn))
				return errorCode(eDuplicateTid);

			final var pendingTxn = txn;
			getRaft().appendLog(new LogPrepareBatch(r), r.Result, (raftLog, result) -> {
				if (!result && stateMachine.getTransactions().remove(tid, pendingTxn))
					pendingTxn.close();
				r.SendResultCode(result ? 0 : Procedure.CancelException);
			});

			// 操作成功，释放所有权。see finally.
			txn = null;
		} finally {
			if (null != txn) {
				// 仅当映射值仍为本事务时才删除，防止重复投递时误删已存活的同名 tid 事务
				stateMachine.getTransactions().remove(tid, txn);
				txn.close();
			}
		}
		return 0;
	}

	@Override
	protected long ProcessCommitBatchRequest(CommitBatch r) {
		getRaft().appendLog(new LogCommitBatch(r), r.Result,
				(raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));
		return 0;
	}

	@Override
	protected long ProcessUndoBatchRequest(UndoBatch r) {
		getRaft().appendLog(new LogUndoBatch(r), r.Result,
				(raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));
		return 0;
	}

	private boolean walkDesc(Bucket bucket, BBucketMeta.Data meta, Binary exclusiveStartKey, int proposeLimit,
							 Binary prefix, Action2<Binary, RocksIterator> fill) throws Exception {
		try (var it = bucket.getData().iterator()) {
			var upper = meta.getKeyLast();
			if (exclusiveStartKey.size() > 0 && (upper.size() == 0 || exclusiveStartKey.compareTo(upper) < 0))
				upper = exclusiveStartKey;
			var prefixUpper = prefix.size() > 0 ? prefixUpperBound(prefix) : null;
			if (prefixUpper != null && (upper.size() == 0 || prefixUpper.compareTo(upper) < 0))
				upper = prefixUpper;
			if (upper.size() == 0)
				it.seekToLast();
			else {
				it.seekForPrev(upper.copyIf());
				// 游标、桶尾与前缀上界均为排他边界，seekForPrev 会包含等值键。
				if (RocksDatabase.isValidOrThrow(it) && upper.contentEquals(it.key()))
					it.prev();
			}

			var count = proposeLimit;
			var bucketEnd = false;
			var lower = meta.getKeyFirst();
			for (; RocksDatabase.isValidOrThrow(it) && count > 0; it.prev()) {
				var key = new Binary(it.key());
				if (key.compareTo(lower) < 0 || (prefix.size() > 0 && !key.startsWith(prefix))) {
					bucketEnd = true;
					break;
				}
				if (it.value().length == 0) // 空值为分桶墓碑标记，跳过且不消耗页配额
					continue;
				fill.run(key, it);
				count--;
			}

			return bucketEnd || !RocksDatabase.isValidOrThrow(it);
		}
	}

	// 计算大于所有 prefix 开头键的最小字节串上界；全 0xFF 无有限上界返回 null
	private static @Nullable Binary prefixUpperBound(Binary prefix) {
		var bytes = prefix.toBytes(); // 副本，避免原地修改入参
		for (var i = bytes.length - 1; i >= 0; --i) {
			if (bytes[i] != -1) {
				++bytes[i];
				return new Binary(bytes, 0, i + 1);
			}
		}
		return null;
	}

	private boolean walk(Bucket bucket, BBucketMeta.Data meta, Binary exclusiveStartKey, int proposeLimit,
						 boolean desc, Binary prefix, Action2<Binary, RocksIterator> fill) throws Exception {
		if (desc) {
			return walkDesc(bucket, meta, exclusiveStartKey, proposeLimit, prefix, fill);
		}

		try (var it = bucket.getData().iterator()) {
			var lower = meta.getKeyFirst();
			if (exclusiveStartKey.compareTo(lower) > 0)
				lower = exclusiveStartKey;
			if (prefix.compareTo(lower) > 0)
				lower = prefix;
			if (lower.size() == 0)
				it.seekToFirst();
			else
				it.seek(lower.copyIf());

			if (RocksDatabase.isValidOrThrow(it) && exclusiveStartKey.size() > 0 && exclusiveStartKey.contentEquals(it.key()))
				it.next(); // 跳过排他起始键

			var count = proposeLimit;
			var bucketEnd = false;
			var keyLast = meta.getKeyLast();
			for (; RocksDatabase.isValidOrThrow(it) && count > 0; it.next()) {
				var key = new Binary(it.key());
				if (keyLast.size() > 0 && key.compareTo(keyLast) >= 0) {
					// 分桶中刚完成时，数据可能超过Last，此时应该检查出来并结束walk。
					bucketEnd = true;
					break;
				}
				// 如果使用了prefix，那么发现了新的prefix时，也表示搜索结束。
				if (prefix.size() > 0 && !key.startsWith(prefix)) {
					bucketEnd = true;
					break;
				}
				if (it.value().length == 0) // 空值为分桶墓碑标记，跳过且不消耗页配额
					continue;
				fill.run(key, it);
				count--;
			}

			return bucketEnd || !RocksDatabase.isValidOrThrow(it);
		}
	}

	// 检测死桶、越界游标或客户端陈旧边界，拒绝后由客户端刷新路由重定位
	private boolean isWalkBucketRefuse(BWalk.Data argument, BBucketMeta.Data meta) {
		if (Bucket.DeadBucketMetaBound.equals(meta.getKeyFirst())
				&& Bucket.DeadBucketMetaBound.equals(meta.getKeyLast()))
			return true;
		var exclusiveStartKey = argument.getExclusiveStartKey();
		//noinspection DataFlowIssue
		if (exclusiveStartKey.size() > 0 && !stateMachine.getBucket().inBucket(meta, exclusiveStartKey))
			return true;
		return argument.isVerifyBucketMeta()
				&& (!meta.getKeyFirst().equals(argument.getExpectedKeyFirst())
				|| !meta.getKeyLast().equals(argument.getExpectedKeyLast()));
	}

	@Override
	protected long ProcessWalkRequest(Walk r) throws Exception {
		if (isBucketNotReady())
			return errorCode(eBucketNotReady);
		var bucket = Objects.requireNonNull(stateMachine.getBucket());
		var metaOnCheck = Objects.requireNonNull(bucket.getBucketMeta());
		if (isWalkBucketRefuse(r.Argument, metaOnCheck)) {
			r.Result.setBucketRefuse(true);
			r.SendResult();
			return 0;
		}
		var bucketEnd = walk(
				bucket, metaOnCheck,
				r.Argument.getExclusiveStartKey(),
				r.Argument.getProposeLimit(),
				r.Argument.isDesc(),
				r.Argument.getPrefix(),
				(key, it) -> r.Result.getKeyValues().add(new BWalkKeyValue.Data(key, new Binary(it.value()))));
		// 迭代期间若发生收尾换代，丢弃结果并返回 bucketRefuse 触发客户端重定位
		if (bucket.getBucketMeta() != metaOnCheck) {
			r.Result.getKeyValues().clear();
			r.Result.setBucketRefuse(true);
			r.SendResult();
			return 0;
		}
		r.Result.setBucketEnd(bucketEnd);
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessWalkKeyRequest(WalkKey r) throws Exception {
		if (isBucketNotReady())
			return errorCode(eBucketNotReady);
		var bucket = Objects.requireNonNull(stateMachine.getBucket());
		var metaOnCheck = Objects.requireNonNull(bucket.getBucketMeta());
		if (isWalkBucketRefuse(r.Argument, metaOnCheck)) {
			r.Result.setBucketRefuse(true);
			r.SendResult();
			return 0;
		}
		var bucketEnd = walk(
				bucket, metaOnCheck,
				r.Argument.getExclusiveStartKey(),
				r.Argument.getProposeLimit(),
				r.Argument.isDesc(),
				r.Argument.getPrefix(),
				(key, it) -> r.Result.getKeys().add(key));
		// 同ProcessWalkRequest的正向交错复核。
		if (bucket.getBucketMeta() != metaOnCheck) {
			r.Result.getKeys().clear();
			r.Result.setBucketRefuse(true);
			r.SendResult();
			return 0;
		}
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
			raft.executeUserTask(() -> {
				try {
					responseHandle.handle(rpc);
				} catch (Throwable e) { // run handle. 必须捕捉所有异常。logger.error
					logger.error("Agent.NetClient.dispatchRpcResponse", e);
				}
			});
		}

		@Override
		public void dispatchProtocol(Protocol<?> p, ProtocolFactoryHandle<?> factoryHandle) throws Exception {
			if (p.getTypeId() == Zeze.Raft.LeaderIs.TypeId_) {
				//noinspection DataFlowIssue
				Task.getCriticalThreadPool().execute(() -> TaskSpec.ofFunc(() -> p.handle(this, factoryHandle)).name("InternalRequest").call());
			} else {
				raft.executeUserTask(() -> TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p).runNow());
			}
		}

		@Override
		public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, @Nullable AsyncSocket so) throws Exception {
			// 不支持事务，传统dispatch即可。
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			p.dispatch(this, factoryHandle);
		}
	}

	public void tryStartSplit(boolean isMove) throws Exception {
		if (!raft.isLeader())
			return;

		var bucket = Objects.requireNonNull(stateMachine.getBucket());
		var splitting = bucket.getSplittingMeta();
		if (null != splitting) {
			logger.info("start but in splitting or ending. {}->{}", formatMeta(bucket.getBucketMeta()), formatMeta(splitting));
			return; // splitting
		}

		// 上一笔迁移的 pending-settle 标志未清前不启动新迁移，避免单槽覆盖丢失结算恢复源
		var pendingSettle = bucket.getPendingSettle();
		if (null != pendingSettle) {
			logger.info("start split but pending settle not cleared. to={}", formatMeta(pendingSettle.getTo()));
			return;
		}

		startSplit(isMove);
	}

	private void onFollowerReceiveKeepAlive() {
		// 集群中的leader已经开始工作。自己是follower.
		stateMachine.setLoadSwitch(true);
	}

	private void recoverSplitting() {
		if (!raft.isLeader())
			return;

		// 释放上一任期可能残留的拦截队列，防止新主复选后写请求挂死
		releasePrepareGate();

		// 换主围栏锚点，供 driveSplitSync 延迟 replace 投递以避开旧 leader 在途 RPC
		splitLeaderReadyTime = System.currentTimeMillis();
		stateMachine.setLoadSwitch(true);
		var bucket = Objects.requireNonNull(stateMachine.getBucket());
		var splitting = bucket.getSplittingMeta();
		if (null != splitting) {
			// 仅按元数据边界判别 move/split：move 与源桶同边界，split 的 keyFirst 为严格更大的中位键
			var bucketMeta = Objects.requireNonNull(bucket.getBucketMeta());
			boolean isMove = splitting.getKeyFirst().compareTo(bucketMeta.getKeyFirst()) == 0
					&& splitting.getKeyLast().compareTo(bucketMeta.getKeyLast()) == 0;
			// onLeaderReady 在持 raft.mutex 线程执行，须异步转投用户任务池避免 ABBA 死锁
			startSplitAsync(isMove);
			return;
		}

		// splittingMeta 已清空但 pendingSettle 仍在：迁移已 commit 但结算未终局，幂等补发结算通知
		var pending = bucket.getPendingSettle();
		if (null != pending) {
			logger.info("recoverSplitting pending-settle reissue. isMove={} from={} to={}",
					null == pending.getFrom(), formatMeta(pending.getFrom()), formatMeta(pending.getTo()));
			var from = pending.getFrom();
			if (from != null)
				//noinspection DataFlowIssue
				manager.getMasterAgent().endSplitWithRetryAsync(from, pending.getTo(),
						() -> appendClearPendingSettle(pending.getTo()));
			else
				//noinspection DataFlowIssue
				manager.getMasterAgent().endMoveWithRetryAsync(pending.getTo(),
						() -> appendClearPendingSettle(pending.getTo()));
		}
	}

	// 结算终局后追加清除日志；非 leader 时失败无害，待下轮 leader-ready 补发收敛
	private void appendClearPendingSettle(BBucketMeta.Data to) {
		try {
			getRaft().appendLog(new LogClearPendingSettle(to));
		} catch (Exception e) {
			logger.warn("appendClearPendingSettle fail, leave flag for next leader-ready reissue. to={}",
					formatMeta(to), e);
		}
	}

	private volatile long splitSerialNo;
	private volatile @Nullable Dbh2Agent dbh2Splitting;
	// 事务同步投递状态：单 RPC 在途（FIFO），splitLeaderReadyTime 为换主围栏锚点
	private final AtomicBoolean splitSyncInFlight = new AtomicBoolean();
	private volatile long splitLeaderReadyTime;

	private @Nullable RocksIterator locateFirst() {
		var bucket = Objects.requireNonNull(stateMachine.getBucket());
		var it = bucket.getData().iterator();
		try {
			it.seekToFirst();
			if (RocksDatabase.isValidOrThrow(it))
				return it;
		} catch (Throwable e) {
			it.close();
			throw e;
		}
		it.close();
		return null;
	}

	// 比较目标 raftConfig 的 sortedNames 是否与本桶一致（自指守卫）
	private boolean isSelfReference(BBucketMeta.Data splitting) {
		try {
			return raft.getRaftConfig().getSortedNames()
					.equals(RaftConfig.loadFromString(splitting.getRaftConfig()).getSortedNames());
		} catch (Exception e) {
			logger.warn("startSplit parse splitting raftConfig fail, treat as not self. entry={}",
					splitting.getRaftConfig(), e);
			return false;
		}
	}

	private @Nullable RocksIterator locateMiddle() throws RocksDBException {
		var bucket = Objects.requireNonNull(stateMachine.getBucket());
		var it = bucket.getData().iterator();
		try {
			var keyNumbers = bucket.getData().getKeyNumbers();
			var count = keyNumbers / 2;
			if (count <= 0) {
				it.close();
				return null;
			}
			//noinspection StatementWithEmptyBody
			for (it.seekToFirst(); RocksDatabase.isValidOrThrow(it) && count > 0; it.next(), --count) {
			}
			if (!RocksDatabase.isValidOrThrow(it)) {
				it.close();
				throw new RocksDBException("middle key not found.");
			}
			logger.info("splitting start locateMiddle keyNumbers={} middle={} {}",
					keyNumbers, new Binary(it.key()),
					formatMeta(stateMachine.getBucket().getBucketMeta()));
			return it;
		} catch (Throwable e) {
			it.close();
			throw e;
		}
	}

	private @Nullable RocksIterator locateMiddle(Binary middleKey) {
		//noinspection DataFlowIssue
		var it = stateMachine.getBucket().getData().iterator();
		try {
			it.seek(Database.copyIf(middleKey.bytesUnsafe(), middleKey.getOffset(), middleKey.size()));
			if (!RocksDatabase.isValidOrThrow(it)) {
				it.close();
				return null;
			}
			return it;
		} catch (Throwable e) {
			it.close();
			throw e;
		}
	}

	private static void performPrepareQueue(@Nullable ConcurrentLinkedQueue<Action0> tmpQueue) {
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

	// Raft 回调在持 raft.mutex 线程执行，禁止同步获取 Dbh2 锁（避免与 waitLeaderReady 构成 ABBA 死锁），统一转投用户任务池
	private void startSplitAsync(boolean isMove) {
		getRaft().executeUserTask(() -> {
			try {
				startSplit(isMove);
			} catch (Exception e) {
				logger.error("startSplitAsync isMove={}", isMove, e);
			}
		});
	}

	// 开始分桶流程有两个线程需要访问：timer & raft.UserThreadExecutor
	private void startSplit(boolean isMove) throws Exception {
		lock();
		try {
			if (!getRaft().isLeader())
				return;

			//noinspection DataFlowIssue
			var serialNo = manager.atomicSerialNo.incrementAndGet();
			splitSerialNo = serialNo;
			releasePrepareGate();
			var bucket = Objects.requireNonNull(stateMachine.getBucket());
			RocksIterator it = null;
			try {
			var splitting = bucket.getSplittingMeta(); // 对于timer，这个会调用两次。
			if (null == splitting) {
				// 第一次开始分桶，准备阶段。
				// 先去manager查一下可用的manager是否够，简单判断，不原子化。
				if (manager.getMasterAgent().checkFreeManager() < dbh2Config.getRaftClusterCount()) {
					logger.warn("splitting not enough free manager. isMove={}", isMove);
					return;
				}
				// 上一次分桶结束的deleteRange可能还没compact，此时keyNumbers不准确，这里总是执行一次。
				bucket.getData().compact();

				// 仅临时定位分界 key，用后立即关闭，避免跨 RPC 钉定陈旧快照视图
				var locateIt = isMove ? locateFirst() : locateMiddle();
				if (null == locateIt) {
					logger.info("splitting break start: it is null. isMove={}", isMove);
					return;
				}
				//noinspection DataFlowIssue
				var newMeta = stateMachine.getBucket().getBucketMeta().copy();
				newMeta.setRaftConfig("");
				newMeta.getHost2Raft().clear();
				if (!isMove)
					newMeta.setKeyFirst(new Binary(locateIt.key()));
				locateIt.close();

				splitting = manager.getMasterAgent().createSplitBucket(newMeta);

				// 防止 resume 命中指向自身的陈旧 splitting 条目导致自搬运并删空本桶数据
				if (isSelfReference(splitting)) {
					logger.error("startSplit self-reference refused, abort this round. self={} entry={}",
							raft.getRaftConfig().getSortedNames(), splitting.getRaftConfig());
					return;
				}

				// 设置分桶进行中的标记到raft集群中。
				getRaft().appendLog(new LogSetSplittingMeta(splitting));
				// 创建到分桶目标的客户端。
				logger.info("splitting start... isMove={} {}->{}",
						isMove, formatMeta(bucket.getBucketMeta()), formatMeta(splitting));
			}

			if (null == dbh2Splitting) {
				dbh2Splitting = new Dbh2Agent(splitting.getRaftConfig(), RaftAgentNetClient::new);
				//noinspection DataFlowIssue
				dbh2Splitting.getRaftAgent().setPendingLimit(Integer.MAX_VALUE);
			}

			// 须等 LogSetSplittingMeta apply 生效（开启事务同步入队）后再创建复制迭代器，确保视图与队列并集无遗漏
			var waitApplyCount = 0;
			while (null == bucket.getSplittingMeta()) {
				if (++waitApplyCount > 1500) {
					logger.warn("splitting wait splittingMeta apply before copy. isMove={}", isMove);
					return;
				}
				//noinspection BusyWait
				Thread.sleep(20);
			}
			driveSplitSync();

			it = isMove ? locateFirst() : locateMiddle(splitting.getKeyFirst());
			if (null == it) {
				// 无可复制数据时直接进入收尾流程（先等 splittingMeta apply 生效）
				var waitCount = 0;
				while (null == stateMachine.getBucket().getSplittingMeta()) {
					if (++waitCount > 1500) {
						logger.warn("splitting wait splittingMeta apply timeout. isMove={}", isMove);
						return;
					}
					//noinspection BusyWait
					Thread.sleep(20);
				}
				logger.info("splitting nothing to copy, go end. isMove={}", isMove);
				blockPrepareUntilNoTransaction(isMove, serialNo);
				return;
			}
			if (bucket.getSplittingMeta() != null)
				logger.info("splitting restart... isMove={} {}->{}",
						isMove, formatMeta(bucket.getBucketMeta()), formatMeta(splitting));

			// 开始同步数据，这个阶段对于rocks时同步访问的，对于网络是异步的。
			var puts = buildSplitPut(it);
			var fit = it;
			//noinspection DataFlowIssue
			Objects.requireNonNull(dbh2Splitting.getRaftAgent()).send(puts, (p) -> splitPutNext(isMove, (SplitPut)p, fit, serialNo));
			it = null;
			} finally {
				if (null != it)
					it.close();
			}
		} finally {
			unlock();
		}
	}

	private SplitPut buildSplitPut(RocksIterator it) {
		var r = new SplitPut();
		r.Argument.setFromTransaction(false);
		var count = dbh2Config.getSplitPutCount();
		for (; RocksDatabase.isValidOrThrow(it) && count > 0; it.next(), --count) {
			r.Argument.getPuts().put(new Binary(it.key()), new Binary(it.value()));
		}
		return r;
	}

	public long splitPutNext(boolean isMove, SplitPut r, RocksIterator it, long serialNo) {
		try {
			// 重发命中去重存根返回 RaftApplied，视为成功
			var hasError = r.getResultCode() != 0 && r.getResultCode() != Procedure.RaftApplied;
			if (!raft.isLeader() || dbh2Splitting == null || serialNo != splitSerialNo) {
				it.close();
				// 仅在本机仍为 leader 且回调属于当前轮次时重试，避免陈旧轮次回调触发新一轮分桶
				if (hasError && raft.isLeader() && serialNo == splitSerialNo)
					startSplit(isMove);
				return 0;
			}

			if (hasError) {
				it.close();
				startSplit(isMove);
				return 0;
			}

			var puts = buildSplitPut(it);
			if (puts.Argument.getPuts().isEmpty()) {
				it.close();

				blockPrepareUntilNoTransaction(isMove, serialNo);
				return 0;
			}

			//noinspection DataFlowIssue
			Objects.requireNonNull(dbh2Splitting.getRaftAgent()).send(puts, (p) -> splitPutNext(isMove, (SplitPut)p, it, serialNo));
		} catch (Exception ex) {
			it.close();
			logger.error("isMove={}", isMove, ex);
			// 本轮仍有效时重入 startSplit 续走 resume，防止同任期内分桶悬挂
			if (raft.isLeader() && serialNo == splitSerialNo) {
				try {
					startSplit(isMove);
				} catch (Exception e) {
					logger.error("splitPutNext retry startSplit fail. isMove={}", isMove, e);
				}
			}
		}
		return 0;
	}

	private void releasePrepareGate() {
		stateMachine.clearNoTransactionHandle();
		((Dbh2RaftServer)getRaft().getServer()).releasePrepareQueue();
	}

	private void blockPrepareUntilNoTransaction(boolean isMove, long serialNo) {
		if (!raft.isLeader()) {
			releasePrepareGate();
			return;
		}
		if (serialNo != splitSerialNo)
			return;
		//noinspection DataFlowIssue
		var meta = stateMachine.getBucket().getBucketMeta();
		var splittingMeta = stateMachine.getBucket().getSplittingMeta();
		logger.info("splitting end ... isMove={} {}->{}",
				isMove, formatMeta(meta), formatMeta(splittingMeta));

		// 截住新的事务请求。
		var server = (Dbh2RaftServer)getRaft().getServer();
		server.setupPrepareQueue();

		// 设置一个超时，每秒放行一次。
		TaskSpec.ofAction(() -> getRaft().executeUserTask(() -> consumePrepareAndBlockAgain(isMove, serialNo))).scheduleNow(1000);

		// 在队列中增加endSplit启动任务，先要处理完队列中的请求。
		// 此时PrepareBatch已经被拦截，但是还有CommitBatch,UndoBatch等其他请求在处理。
		// setupHandleIfNoTransaction 将在没有进行中的事务时触发。
		getRaft().executeUserTask(() -> stateMachine.setupOneShotIfNoTransaction(() -> endSplit0(isMove, serialNo)));
	}

	private void consumePrepareAndBlockAgain(boolean isMove, long serialNo) {
		if (!raft.isLeader()) {
			releasePrepareGate();
			return;
		}
		if (serialNo != splitSerialNo)
			return;
		if (stateMachine.hasNoTransactionHandle()) {
			// 还在等待事务清空，此时...
			// 处理一下累积的Prepare请求，暂时放行一下。
			var server = (Dbh2RaftServer)getRaft().getServer();
			performPrepareQueue(server.takePrepareQueue());
			blockPrepareUntilNoTransaction(isMove, serialNo);
		}
	}

	private void endSplit0(boolean isMove, long serialNo) {
		if (!raft.isLeader()) {
			releasePrepareGate();
			return; // 掉主：释放拦截队列并终止重试链，新leader经recoverSplitting→startSplit续走全流程
		}
		if (serialNo != splitSerialNo)
			return;
		//noinspection DataFlowIssue
		var splittingMeta = stateMachine.getBucket().getSplittingMeta();
		if (null == splittingMeta) {
			// LogSetSplittingMeta 尚未 apply：延迟重试，避免 transactions 为空时立即内联热自旋
			logger.warn("endSplit0 wait splittingMeta apply. isMove={}", isMove);
			TaskSpec.ofAction(() -> getRaft().executeUserTask(
					() -> stateMachine.setupOneShotIfNoTransaction(() -> endSplit0(isMove, serialNo)))).schedule(1000);
			return;
		}
		// 必须等事务同步队列全部送达后再收尾，否则后续源桶 deleteToEnd 会丢失未送达写入
		if (stateMachine.hasPendingSplitSync()) {
			driveSplitSync();
			TaskSpec.ofAction(() -> getRaft().executeUserTask(
					() -> stateMachine.setupOneShotIfNoTransaction(() -> endSplit0(isMove, serialNo)))).schedule(1000);
			return;
		}
		var splittingAgent = dbh2Splitting;
		if (null == splittingAgent) {
			releasePrepareGate();
			return;
		}
		// 第一步，设置新桶的meta
		splittingAgent.setBucketMetaAsync(splittingMeta, (p) -> endSplit1(p, isMove, serialNo));
	}

	private long endSplit1(Protocol<?> p, boolean isMove, long serialNo) {
		if (!raft.isLeader()) {
			releasePrepareGate();
			return 0;
		}
		if (serialNo != splitSerialNo)
			return 0;

		var r = (SetBucketMeta)p;
		if (r.getResultCode() != 0 && r.getResultCode() != Procedure.RaftApplied) {
			try {
				startSplit(isMove);
			} catch (Exception e) {
				logger.error("isMove={}", isMove, e);
				releasePrepareGate();
			}
			return 0;
		}

		var bucket = Objects.requireNonNull(stateMachine.getBucket());
		var splittingMeta = bucket.getSplittingMeta();
		if (null == splittingMeta) {
			releasePrepareGate();
			return 0;
		}
		try {
			if (isMove) {
				getRaft().appendLog(new LogEndMove(splittingMeta),
						(raftLog, result) -> endSplit2(raftLog, result, true, serialNo));
			} else {
				// 原子化设置源桶状态（修改源桶Meta；保存新桶Meta到历史中；删除分桶Meta）。
				//noinspection DataFlowIssue
				var from = bucket.getBucketMeta().copy();
				from.setKeyLast(splittingMeta.getKeyFirst());
				getRaft().appendLog(new LogEndSplit(from, splittingMeta),
						(raftLog, result) -> endSplit2(raftLog, result, false, serialNo));
			}
		} catch (Exception e) {
			logger.warn("endSplit1 appendLog fail. isMove={}", isMove, e);
			releasePrepareGate();
		}
		return 0;
	}

	private void endSplit2(RaftLog raftLog, Boolean result, boolean isMove, long serialNo) {
		if (!result) {
			if (!raft.isLeader() || serialNo == splitSerialNo)
				releasePrepareGate();
			try {
				// leaderCallback 在持 raft.mutex 线程执行，禁止同步进入 startSplit
				if (raft.isLeader() && serialNo == splitSerialNo)
					startSplitAsync(isMove);
			} catch (Exception e) {
				logger.error("isMove={}", isMove, e);
			}
			return;
		}

		// 此时进入拒绝模式：释放拦截队列并交由 executeUserTask 串行排空（不在 raft.lock 内执行用户 RPC）。
		releasePrepareGate();

		// 关闭到新桶连接。
		var splittingAgent = dbh2Splitting;
		dbh2Splitting = null;
		if (null != splittingAgent) {
			try {
				splittingAgent.close();
			} catch (Exception ex) {
				logger.error("", ex);
			}
		}

		//noinspection DataFlowIssue
		var meta = stateMachine.getBucket().getBucketMeta();
		if (isMove) {
			var endMove = (LogEndMove)raftLog.getLog();
			//noinspection DataFlowIssue
			manager.getMasterAgent().endMoveWithRetryAsync(Objects.requireNonNull(endMove.getTo()),
					() -> appendClearPendingSettle(Objects.requireNonNull(endMove.getTo())));
		} else {
			// 可以安全的发布新旧桶的信息到Master了。
			var endSplit = (LogEndSplit)raftLog.getLog();
			//noinspection DataFlowIssue
			manager.getMasterAgent().endSplitWithRetryAsync(Objects.requireNonNull(endSplit.getFrom()), Objects.requireNonNull(endSplit.getTo()),
					() -> appendClearPendingSettle(Objects.requireNonNull(endSplit.getTo())));
		}
		logger.info("splitting end done. isMove={} {}", isMove, formatMeta(meta));
	}

	// 驱动分桶事务同步持久队列投递：CAS 单飞 FIFO，ACK 后推进水位，失败 1s 后重投；
	// 换主后延迟超过旧 leader RPC 超时围栏再发 replace，避免旧 leader 迟到包覆盖新值。
	public void driveSplitSync() {
		if (!splitSyncInFlight.compareAndSet(false, true))
			return;
		try {
			var agent = dbh2Splitting;
			if (closed || !raft.isLeader() || null == agent) {
				splitSyncInFlight.set(false);
				return;
			}
			var delay = splitLeaderReadyTime + splitSyncFenceDelayMs(agent) - System.currentTimeMillis();
			if (delay > 0) {
				splitSyncInFlight.set(false);
				TaskSpec.ofAction(this::driveSplitSync).schedule(delay);
				return;
			}
			var batch = stateMachine.pollSplitSync(dbh2Config.getSplitPutCount());
			if (null == batch) {
				splitSyncInFlight.set(false);
				return;
			}
			var r = new SplitPut(batch.data);
			agent.getRaftAgent().send(r, (p) -> {
				// 重发命中去重存根返回 RaftApplied，视为已落盘
				if (r.getResultCode() != 0 && r.getResultCode() != Procedure.RaftApplied) {
					splitSyncInFlight.set(false);
					TaskSpec.ofAction(this::driveSplitSync).schedule(1000);
					return 0;
				}
				try {
					stateMachine.advanceSplitSyncWatermark(batch);
				} catch (Exception ex) {
					logger.error("advanceSplitSyncWatermark", ex);
					splitSyncInFlight.set(false);
					TaskSpec.ofAction(this::driveSplitSync).schedule(1000);
					return 0;
				}
				splitSyncInFlight.set(false);
				driveSplitSync();
				return 0;
			});
		} catch (Exception ex) {
			logger.error("driveSplitSync", ex);
			splitSyncInFlight.set(false);
			TaskSpec.ofAction(this::driveSplitSync).schedule(1000);
		}
	}

	private static long splitSyncFenceDelayMs(Dbh2Agent agent) {
		// 覆盖旧 leader 在途 RPC 的 AgentTimeout 与重发扫描抖动裕量
		return agent.getRaftAgent().getRaftConfig().getAgentTimeout() + 2000L;
	}

	@Override
	protected long ProcessSplitPutRequest(SplitPut r) {
		raft.appendLog(new LogSplitPut(r),
				(raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));
		return 0;
	}

}
