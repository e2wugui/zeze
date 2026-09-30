package Zeze.Dbh2;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BBucketMeta;
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
import org.jetbrains.annotations.NotNull;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

/**
 * Dbh2 单桶 Raft 服务节点：负责一个桶的数据存储、协议处理与分桶/迁移流程。
 */
public class Dbh2 extends AbstractDbh2 implements AutoCloseable {
	private static final Logger logger = LogManager.getLogger(Dbh2.class);
	private final Dbh2Config dbh2Config = new Dbh2Config();
	// 非final：构造半失败的catch清理需null检查后引用（blank final在catch中读取不通过确定赋值检查）。
	private Raft raft;
	private Dbh2StateMachine stateMachine;
	private final Dbh2Manager manager;
	private final Locks locks = new Locks();

	public Locks getLocks() {
		return locks;
	}

	// 性能统计。
	public static String formatMeta(BBucketMeta.Data meta) {
		return meta == null ? "" : meta.getDatabaseName() + '.' + meta.getTableName() +
				'[' + meta.getKeyFirst() + ',' + meta.getKeyLast() + ']';
	}

	public class Dbh2RaftServer extends Zeze.Raft.Server {
		public Dbh2RaftServer(Raft raft, String name, Config config) {
			super(raft, name, config);
			setInstanceName(raft.getName());
		}

		private final FastLock prepareQueueLock = new FastLock();
		private ConcurrentLinkedQueue<Action0> prepareQueue;

		public void setupPrepareQueue() {
			prepareQueueLock.lock();
			try {
				if (null == prepareQueue)
					prepareQueue = new ConcurrentLinkedQueue<>();
			} finally {
				prepareQueueLock.unlock();
			}
		}

		public ConcurrentLinkedQueue<Action0> takePrepareQueue() {
			prepareQueueLock.lock();
			try {
				var tmp = prepareQueue;
				prepareQueue = null;
				return tmp;
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

			// 全序化不变量：非query请求（含PrepareBatch）的"拦截检查"与"提交raft串行执行器"
			// 必须在prepareQueueLock临界区内原子完成——与装载（setupPrepareQueue，同锁）任一先后
			// 都安全；分离则存在"判空后、提交前被装载+one-shot抢先"的窗口，晚注册事务落入迁出
			// 键域的写入被收尾deleteToEnd静默丢弃。提交仅短暂持per-key队列锁（非阻塞），
			// prepareQueueLock→队列锁单向无环。
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

	// meta-less守卫：新raft在SetBucketMeta（桶协议第一条）之前
	// bucketMeta==null，此前Get/Walk/WalkKey/PrepareBatch入口经inBucket对meta的解引用以
	// 框架层NPE面目出现；显式判定返回专用错误码eBucketNotReady，孤儿收养
	// 路径可辨识、可重试。SetBucketMeta/SplitPut不拦——前者即初始化入口，后者是目标桶
	// 在meta就位前的数据灌入通道。
	private boolean isBucketNotReady() {
		return stateMachine.getBucket().getBucketMeta() == null;
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

	public Dbh2(Dbh2Manager manager, String raftName, RocksDatabase database,
				RaftConfig raftConf, Config config, boolean writeOptionSync,
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
			raft = new Raft(stateMachine, raftName, database, raftConf, config,
					"Zeze.Dbh2.Server", Dbh2RaftServer::new, taskOneByOne);
			raft.getServer().getSocketOptions().setInputBufferMaxProtocolSize(100 * 1024 * 1024);
			raft.getServer().getSocketOptions().setOutputBufferMaxSize(100 * 1024 * 1024);
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
			// 半失败逆序清理（次序与分段捕获对齐close()先例）：Raft构造成功即已启动timerTask、
			// 注册ShutdownHook，openBucket已打开桶独立rocksdb句柄——不清理则句柄钉死桶目录
			//（Windows），master侧建桶回滚的DestroyBucket删目录失败。清理异常addSuppressed，
			// 不掩盖原始失败原因。
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
		// 生命周期不变量：stateMachine.close()（bucket的rocksdb句柄/onTimer/commitAgent）必须无条件
		// 执行——raft.shutdown()抛异常时跳过它会把句柄泄漏到进程存续期；destroyBucket重试走幂等
		// 分支（条目已摘除）不再close，泄漏句柄钉死目录删除（Windows），销毁回滚永久卡死。
		// 分别捕获，首个异常在全部清理执行完后重抛（保持close失败不删目录的调用方语义）。
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
		// 分桶进行中停止：dbh2Splitting不清理则agent的resend任务/连接器/pending表残留至进程结束
		//（嵌入式/测试反复建销毁桶逐次累积）。先摘引用再close（对齐endSplit2的清理形态）。
		// 置于raft.shutdown之后：shutdown已convertStateTo(Follower)——stop触发的pending终局以
		// Timeout码直接调用用户handle（Agent.trigger不经sendHandle，其早退只覆盖应答到达路径），
		// 落入splitPutNext的hasError/失配分支，其startSplit重入被isLeader(false)门控拒绝；
		// 迟到的用户任务回调同样经身份检查不再重入；driveSplitSync以closed守卫收敛。
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
				-> r.SendResultCode(result ? 0 : Procedure.CancelException)); // result is empty
		return 0;
	}

	@Override
	protected long ProcessGetRequest(Zeze.Builtin.Dbh2.Get r) throws RocksDBException {
		if (isBucketNotReady())
			return errorCode(eBucketNotReady);
		stateMachine.counterGet.incrementAndGet();
		// 直接读取数据库。是否可以读取由raft控制。raft启动时有准备阶段。
		var bucket = stateMachine.getBucket();
		if (!bucket.inBucket(r.Argument.getDatabase(), r.Argument.getTable(), r.Argument.getKey()))
			return errorCode(eBucketMismatch);
		var value = bucket.get(r.Argument.getKey());
		if (null == value || value.size() == 0) // 空值是分桶墓碑标记，逻辑上不存在（存储不变量：空value==墓碑）
			r.Result.setNull(true);
		else {
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
		var txn = new Dbh2Transaction(this, r.Argument.getBatch());
		try {
			if (null != stateMachine.getTransactions().putIfAbsent(r.Argument.getBatch().getTid(), txn))
				return errorCode(eDuplicateTid);
			if (!stateMachine.getBucket().inBucket(r.Argument.getDatabase(), r.Argument.getTable()))
				return errorCode(eBucketMismatch);

			var refused = r.Result;
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

			getRaft().appendLog(new LogPrepareBatch(r), r.Result,
					(raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));

			// 操作成功，释放所有权。see finally.
			txn = null;
		} finally {
			if (null != txn) {
				// 必须两参remove（仅当映射值是本次txn才删）：重复投递撞eDuplicateTid时，
				// 表里是先到的存活事务，本txn从未入表，单参remove会误删别人的事务
				//（commitBatch随后找不到事务，已决定提交的数据被静默丢弃）。
				stateMachine.getTransactions().remove(r.Argument.getBatch().getTid(), txn);
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

	private boolean walkDesc(Binary exclusiveStartKey, int proposeLimit, Binary prefix,
							 Action2<Binary, RocksIterator> fill) throws Exception {
		try (var it = stateMachine.getBucket().getData().iterator()) {
			if (exclusiveStartKey.size() > 0) {
				// 越上界游标归一：游标高于前缀上界时seekForPrev(游标)落在非前缀键上会被
				// 判桶尾，整段前缀键静默跳过——统一seekForPrev(prefixUpper)定位（等上界
				// 键须再prev跳过）；全0xFF前缀无有限上界，不归一。
				var prefixUpper = prefix.size() > 0 ? prefixUpperBound(prefix) : null;
				if (prefixUpper != null && exclusiveStartKey.compareTo(prefixUpper) > 0) {
					it.seekForPrev(prefixUpper.copyIf());
					if (it.isValid() && prefixUpper.contentEquals(it.key()))
						it.prev();
				} else {
					it.seekForPrev(exclusiveStartKey.copyIf());
				}
			} else {
				// 分桶过程中，可能存在Last之后的数据，必须根据Last的情况定位，不能直接使用seekToLast。
				var lastKey = stateMachine.getBucket().getBucketMeta().getKeyLast();
				// 定位与过滤一致：prefix时定位到目标前缀的结尾（与keyLast取更小者），否则桶内混存的
				// 更大前缀key会让首个循环立即判定桶尾，本桶目标前缀的记录被静默跳过。
				var prefixUpper = prefix.size() > 0 ? prefixUpperBound(prefix) : null;
				if (prefixUpper != null && (lastKey.size() == 0 || prefixUpper.compareTo(lastKey) < 0))
					it.seekForPrev(prefixUpper.copyIf());
				else if (lastKey.size() > 0)
					it.seekForPrev(lastKey.copyIf());
				else
					it.seekToLast();
				// seekForPrev是闭语义（<=target）：桶内恰存在等于上界的key时迭代器落在其上（含
				// prefixUpper==keyLast的分支），先跳过，否则首循环前缀过滤失败即误判桶尾，
				// 本桶目标前缀的记录被静默跳过（与下方exclusiveStartKey同款跳过）。
				if (it.isValid() && prefixUpper != null && prefixUpper.contentEquals(it.key()))
					it.prev();
			}
			if (it.isValid() && exclusiveStartKey.size() > 0 && exclusiveStartKey.contentEquals(it.key()))
				it.prev(); // skip exclusive key if need.

			var count = proposeLimit;
			var bucketEnd = false;
			for (; it.isValid() && count > 0; it.prev()) {
				var key = new Binary(it.key());
				if (prefix.size() > 0 && !key.startsWith(prefix)) {
					bucketEnd = true;
					break;
				}
				if (it.value().length == 0) // 分桶墓碑标记，逻辑上不存在（walkKey也跳过：标记key等于已删除）
					continue; // 不消耗页配额：整页墓碑若计数退出会以0条+非桶尾返回，客户端walkPage将跳桶丢数据
				fill.run(key, it);
				count--;
			}

			return bucketEnd || !it.isValid();
		}
	}

	// prefix的上界（大于一切以prefix开头的key的最小byte串）：从后往前找首个非0xFF字节加一截断；
	// 全0xFF时无有限上界返回null（该前缀即最大可能前缀，桶内最大key就是目标前缀的结尾，走keyLast/seekToLast定位）。
	private static Binary prefixUpperBound(Binary prefix) {
		var bytes = prefix.toBytes(); // 必须副本：原地加一会改掉调用方的prefix
		for (var i = bytes.length - 1; i >= 0; --i) {
			if (bytes[i] != -1) {
				++bytes[i];
				return new Binary(bytes, 0, i + 1);
			}
		}
		return null;
	}

	private boolean walk(Binary exclusiveStartKey, int proposeLimit, boolean desc, Binary prefix,
						 Action2<Binary, RocksIterator> fill) throws Exception {
		if (desc) {
			return walkDesc(exclusiveStartKey, proposeLimit, prefix, fill);
		}

		try (var it = stateMachine.getBucket().getData().iterator()) {
			if (exclusiveStartKey.size() > 0)
				it.seek(exclusiveStartKey.copyIf());
			else if (prefix.size() > 0)
				it.seek(prefix.copyIf()); // 定位与过滤一致：空游标时从前缀起始开始（见walkDesc同款注释）
			else
				it.seekToFirst();

			if (it.isValid() && exclusiveStartKey.size() > 0 && exclusiveStartKey.contentEquals(it.key()))
				it.next(); // skip exclusive key if need.

			var count = proposeLimit;
			var bucketEnd = false;
			var keyLast = stateMachine.getBucket().getBucketMeta().getKeyLast();
			for (; it.isValid() && count > 0; it.next()) {
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
				if (it.value().length == 0) // 分桶墓碑标记，逻辑上不存在（walkKey也跳过：标记key等于已删除）
					continue; // 不消耗页配额：整页墓碑若计数退出会以0条+非桶尾返回，客户端walkPage将跳桶丢数据
				fill.run(key, it);
				count--;
			}

			return bucketEnd || !it.isValid();
		}
	}

	// walk陈旧视图检测——桶收窄（分裂/迁移完结）或置死后不得按正常桶尾应答，否则客户端
	// 按陈旧视图推进迭代器会静默跳过新桶键域。拒绝条件：置死桶（{1},{1}哨兵meta）无条件
	// 拒；非空游标出界拒；VerifyBucketMeta置位时缓存视图预期界与权威meta不等即拒（收窄后
	// 的桶对新鲜客户端是合法遍历目标，服务端无法单方面识别陈旧，预期界须客户端回带）。
	// 客户端收bucketRefuse即reload重定位（与Get的eBucketMismatch自愈同构）；master侧表
	// 长期陈旧的连续拒绝由walkPage既有256上限兜底。
	private boolean isWalkBucketRefuse(BWalk.Data argument) {
		var meta = stateMachine.getBucket().getBucketMeta();
		if (Bucket.DeadBucketMetaBound.equals(meta.getKeyFirst())
				&& Bucket.DeadBucketMetaBound.equals(meta.getKeyLast()))
			return true;
		var exclusiveStartKey = argument.getExclusiveStartKey();
		if (exclusiveStartKey.size() > 0 && !stateMachine.getBucket().inBucket(exclusiveStartKey))
			return true;
		return argument.isVerifyBucketMeta()
				&& (!meta.getKeyFirst().equals(argument.getExpectedKeyFirst())
				|| !meta.getKeyLast().equals(argument.getExpectedKeyLast()));
	}

	@Override
	protected long ProcessWalkRequest(Walk r) throws Exception {
		if (isBucketNotReady())
			return errorCode(eBucketNotReady);
		if (isWalkBucketRefuse(r.Argument)) {
			r.Result.setBucketRefuse(true);
			r.SendResult();
			return 0;
		}
		var bucketEnd = walk(
				r.Argument.getExclusiveStartKey(),
				r.Argument.getProposeLimit(),
				r.Argument.isDesc(),
				r.Argument.getPrefix(),
				(key, it) -> r.Result.getKeyValues().add(new BWalkKeyValue.Data(key, new Binary(it.value()))));
		r.Result.setBucketEnd(bucketEnd);
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessWalkKeyRequest(WalkKey r) throws Exception {
		if (isBucketNotReady())
			return errorCode(eBucketNotReady);
		if (isWalkBucketRefuse(r.Argument)) {
			r.Result.setBucketRefuse(true);
			r.SendResult();
			return 0;
		}

		var bucketEnd = walk(
				r.Argument.getExclusiveStartKey(),
				r.Argument.getProposeLimit(),
				r.Argument.isDesc(),
				r.Argument.getPrefix(),
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
		public <P extends Protocol<?>> void dispatchRpcResponse(@NotNull P rpc, @NotNull ProtocolHandle<P> responseHandle,
																@NotNull ProtocolFactoryHandle<?> factoryHandle) {
			raft.executeUserTask(() -> {
				try {
					responseHandle.handle(rpc);
				} catch (Throwable e) { // run handle. 必须捕捉所有异常。logger.error
					logger.error("Agent.NetClient.dispatchRpcResponse", e);
				}
			});
		}

		@Override
		public void dispatchProtocol(@NotNull Protocol<?> p, @NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
			if (p.getTypeId() == Zeze.Raft.LeaderIs.TypeId_) {
				Task.getCriticalThreadPool().execute(() -> TaskSpec.ofFunc(() -> p.handle(this, factoryHandle)).name("InternalRequest").call());
			} else {
				raft.executeUserTask(() -> TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p).runNow());
			}
		}

		@Override
		public void dispatchProtocol(long typeId, @NotNull ByteBuffer bb, @NotNull ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) throws Exception {
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

		// pending-settle闸（堆叠窗口闭口，setPendingSettle注释自认的二期缺口）：上一笔迁移
		// 已commit（LogEndSplit/LogEndMove apply落下标志）但settle未终局（30s内存重试链在途/
		// master不可达）时不得启动新迁移——否则新迁移完结时setPendingSettle撞上残留旧标志被
		// 跳过不落盘（单槽保留旧者），进程死亡后recoverSplitting只补发槽内旧迁移，新迁移的
		// to键域在主表无主、读写永久失败。闸只拦决策入口（loadMonitor路径），recoverSplitting
		// 的resume不走此处；settle终局（appendClearPendingSettle的apply清除标志）后下一轮
		// loadMonitor（120s）自然重试。代价：标志滞留期间本桶不分桶/迁移——可用性换正确性，
		// 恰是"等旧迁移结算"的本意。
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

		// 换主围栏锚点：本节点本次leader-ready时刻（driveSplitSync据此延迟replace语义的投递，
		// 避免旧leader在途投递rpc迟到覆盖，见driveSplitSync注释）。
		splitLeaderReadyTime = System.currentTimeMillis();
		stateMachine.setLoadSwitch(true);
		var bucket = stateMachine.getBucket();
		var splitting = bucket.getSplittingMeta();
		if (null != splitting) {
			// move/split身份=纯元数据判别不变式（startSplit准备段构造splitting时成立，
			// splitting期间保持成立，恢复时刻可直接依赖）：
			//  - move：splitting是源桶meta的副本（copy后仅清raftConfig），恒同keyFirst同keyLast；
			//  - split：splitting.keyFirst=locateMiddle取的中位key，位于第keyNumbers/2>=1个
			//    位置，恒严格大于源keyFirst（空keyFirst小于一切key，首桶move同样被等值覆盖），
			//    keyLast恒同源（copy不改）。源桶meta在splitting期间边界稳定：只有
			//    LogEndSplit/LogEndMove收尾会改写，而两者apply的同时删除splitting。
			// 故"splitting与源meta同keyFirst且同keyLast"即move，否则（keyFirst严格更大）为split。
			// 判别只读meta、不受拷贝窗口内数据增删影响，同时消灭两个误判方向：
			//  ① split误判为move（“data[0]==keyFirst”启发式的误判方向）：源桶事务delete是物理删除
			//    （commitBatch直接WriteBatch delete，无墓碑），[F,M)左半段清空后data[0]右移到
			//    分界key M——move收尾endMove从apply时刻的data[0]删到尾且源桶置死桶meta{1},{1}，
			//    [F,M)窗口写入被静默物理删除、键域永久失联且无自愈，master侧settle守卫对
			//    from==null的LogEndMove路径结构性不触发；
			//  ② move误判为split（边界key被删/空桶时data[0]>keyFirst）：master侧终态由
			//    MasterDatabase.settleSplitting守卫兜底（from.keyFirst>=to.keyFirst不put from），
			//    保留为防御层。
			// 不改raft日志schema：判别信息本来就在LogSetSplittingMeta持久化的meta边界里。
			var bucketMeta = bucket.getBucketMeta();
			boolean isMove = splitting.getKeyFirst().compareTo(bucketMeta.getKeyFirst()) == 0
					&& splitting.getKeyLast().compareTo(bucketMeta.getKeyLast()) == 0;
			// onLeaderReady在持raft.mutex的apply线程上执行，禁止同步进入startSplit（锁序见startSplitAsync）。
			startSplitAsync(isMove);
			return;
		}

		// pending-settle补发：splittingMeta==null且标志非空 = 迁移已在本桶
		// raft上commit并apply（LogEndSplit/LogEndMove），但settle通知未达master——
		// 原append节点在commit→apply→invokeCallback之间死亡，endSplit2的内存重试链随进程
		// 消失，master侧splitting条目与旧主表条目永存。标志是apply派生状态、随raft复制/快照，
		// 任何后来当选的leader都持有它——此处幂等补发（复用既有endSplit/endMoveWithRetryAsync）：
		//  - master已结算：splitting条目已消费 → eSplittingBucketNotFound（既有终局语义）；
		//  - master未结算：settle照常生效。
		// 两方向终局都触发onSettled回调追加标志清除日志，收敛闭环。
		var pending = bucket.getPendingSettle();
		if (null != pending) {
			logger.info("recoverSplitting pending-settle reissue. isMove={} from={} to={}",
					null == pending.getFrom(), formatMeta(pending.getFrom()), formatMeta(pending.getTo()));
			if (null != pending.getFrom())
				manager.getMasterAgent().endSplitWithRetryAsync(pending.getFrom(), pending.getTo(),
						() -> appendClearPendingSettle(pending.getTo()));
			else
				manager.getMasterAgent().endMoveWithRetryAsync(pending.getTo(),
						() -> appendClearPendingSettle(pending.getTo()));
		}
	}

	// settle终局清除标志：追加清除日志，apply侧身份匹配清除。非leader窗口
	// appendLog抛RaftRetryException——不清除仅意味着标志多活到下一轮leader-ready补发的
	// 终局，幂等收敛，无害。
	private void appendClearPendingSettle(BBucketMeta.Data to) {
		try {
			getRaft().appendLog(new LogClearPendingSettle(to));
		} catch (Exception e) {
			logger.warn("appendClearPendingSettle fail, leave flag for next leader-ready reissue. to={}",
					formatMeta(to), e);
		}
	}

	private volatile long splitSerialNo;
	private volatile Dbh2Agent dbh2Splitting;
	// 事务同步投递链状态（持久队列在Dbh2StateMachine.splitSync*，dbh2-01）。inFlight保证同一时刻
	// 至多一个投递rpc在途（提交序FIFO）；leaderReadyTime是换主围栏锚点（见driveSplitSync注释）。
	private final AtomicBoolean splitSyncInFlight = new AtomicBoolean();
	private volatile long splitLeaderReadyTime;

	private RocksIterator locateFirst() {
		var bucket = stateMachine.getBucket();
		var it = bucket.getData().iterator();
		it.seekToFirst();
		if (it.isValid())
			return it;
		it.close();
		return null;
	}

	// 解析返回条目raftConfig的sortedNames与本桶全等即自指（manager本地同款解析先例：
	// Dbh2Manager.createBucket）。解析失败不是自指形态，走原路径（后续对目标的连接/写入
	// 自会暴露问题）。
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

	private RocksIterator locateMiddle() throws RocksDBException {
		var bucket = stateMachine.getBucket();
		var it = bucket.getData().iterator();
		var keyNumbers = bucket.getData().getKeyNumbers();
		var count = keyNumbers / 2;
		if (count <= 0) {
			it.close();
			return null;
		}
		//noinspection StatementWithEmptyBody
		for (it.seekToFirst(); it.isValid() && count > 0; it.next(), --count) {
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

	// 锁序约束：raft回调（onLeaderReady；appendLog的leaderCallback经invokeCallback/cancelCallback）
	// 在持raft.mutex的线程上同步执行，该线程禁止获取Dbh2模块锁——startSplit持Dbh2锁内的
	// performPrepareQueue→processRequest→waitLeaderReady会再取raft.mutex，反向嵌套构成ABBA死锁。
	// 故凡从raft回调进入startSplit的入口统一转投用户任务池（按raft名串行；入队只短暂持有队列锁、
	// 池派发不阻塞，均无取raft锁的路径），回调线程只入队不等待结果。延迟窗口由startSplit既有守卫
	// 幂等收敛：isLeader失配即no-op；resume重读splittingMeta，窗口内其值不变（LogSet/EndSplit/
	// EndMove的append均源自排在本次任务之后的同队列续链），窗口内新提交事务被重拷贝迭代器的
	// 后建视图覆盖（复制迭代器在splittingMeta apply之后才创建，见startSplit内注释；meta apply
	// 后提交的事务则进同步队列，两者并集无遗漏）。
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

				// 定位middle只用临时迭代器，取到key立即关闭：迭代器钉定创建时刻的视图，
				// 跨createSplitBucket rpc（分钟级）持有的话，rpc期间提交的事务会落在
				// 复制视图之外（详见下面"同步先于复制视图"的注释）。
				var locateIt = isMove ? locateFirst() : locateMiddle();
				if (null == locateIt) {
					logger.info("splitting break start: it is null. isMove={}", isMove);
					return; // empty？不需要执行后续操作。break progress.
				}
				var newMeta = stateMachine.getBucket().getBucketMeta().copy();
				newMeta.setRaftConfig("");
				if (!isMove)
					newMeta.setKeyFirst(new Binary(locateIt.key()));
				locateIt.close(); // 不跨rpc持有钉定视图

				splitting = manager.getMasterAgent().createSplitBucket(newMeta);

				// 自指守卫：resume撞上指向自身的陈旧splitting条目——自搬运
				// 形态：move1(A→B)完成而endMove未达master，B的loadMonitor再决策move，同四元组
				// resume命中指向B自身的条目，随后B对自身putIfAbsent拷贝（无错）、endMove把源桶
				// 数据从data[0]删到尾并置死桶——数据物理灭失。判据：返回条目raftConfig的
				// sortedNames与本桶全等（新建条目恒为新端口新raft，全等只在自指时出现）。
				// 身份只在请求方本地持有（raftConfig不能经rpc进入身份判据），故拦截位在
				// 请求方。中止本轮：不发LogSetSplittingMeta，桶保持完整服务；条目由源桶
				// leader-ready补发或master侧死信消费收敛，每120s一轮的重试噪声是正确的拒绝。
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
				dbh2Splitting.getRaftAgent().setPendingLimit(Integer.MAX_VALUE);
			}

			var server = (Dbh2RaftServer)getRaft().getServer();
			performPrepareQueue(server.takePrepareQueue());

			// 同步先于复制视图（队列版不变量，dbh2-01）：事务同步的入队条件=splittingMeta!=null
			// （apply序=日志序），复制迭代器必须在LogSetSplittingMeta apply之后钉定视图——否则
			// （钉定,apply）窗口内apply的事务delete既不在视图（物理删除）也不在队列（meta仍null），
			// 永久丢失。resume路径meta已apply，立即通过。等待仅阻塞其它startSplit重入（Dbh2.lock
			// 不被raft apply路径持有），不影响正常读写。
			var waitApplyCount = 0;
			while (null == bucket.getSplittingMeta()) {
				if (++waitApplyCount > 1500) { // 30s：apply需多数派往返，正常毫秒级；超时属raft异常
					logger.warn("splitting wait splittingMeta apply before copy. isMove={}", isMove);
					return; // 放弃本轮，桶保持可写；换主后recoverSplitting自愈
				}
				//noinspection BusyWait
				Thread.sleep(20);
			}
			// 唤醒队列投递：此后提交的事务开始入队；agent刚就位（就位前入队的条目无人投递）或
			// 前一轮驱动停摆的积压在此统一唤醒。
			driveSplitSync();

			// 复制迭代器总是同步就位之后新建（首轮也走这里，不复用定位middle的旧迭代器）。
			it = isMove ? locateFirst() : locateMiddle(splitting.getKeyFirst());
			if (null == it) {
				// 无可复制数据（如定位与重定位之间分界key及以右被全部删除）：
				// 复制空完成，走splitPutNext同款收尾路径（不能return——tryStartSplit对
				// splitting!=null早退，无人重试会永久卡住分桶）。
				// 必须先等LogSetSplittingMeta apply：本分支无网络往返，append到收尾仅毫秒级，
				// 未apply就进endSplit0会读到splittingMeta==null（setBucketMetaAsync(null)必NPE，
				// 且one-shot被消耗后prepare队列永久积压）。等待期间仅阻塞其它startSplit重入
				// （Dbh2.lock不被raft apply路径持有），不影响正常读写。
				var waitCount = 0;
				while (null == stateMachine.getBucket().getSplittingMeta()) {
					if (++waitCount > 1500) { // 30s：apply需多数派往返，正常毫秒级；超时属raft异常
						logger.warn("splitting wait splittingMeta apply timeout. isMove={}", isMove);
						return; // 放弃本轮，桶保持可写；换主后recoverSplitting自愈
					}
					//noinspection BusyWait
					Thread.sleep(20);
				}
				logger.info("splitting nothing to copy, go end. isMove={}", isMove);
				blockPrepareUntilNoTransaction(isMove);
				return;
			}
			if (bucket.getSplittingMeta() != null)
				logger.info("splitting restart... isMove={} {}->{}",
						isMove, formatMeta(bucket.getBucketMeta()), formatMeta(splitting));

			// 开始同步数据，这个阶段对于rocks时同步访问的，对于网络是异步的。
			var puts = buildSplitPut(it);
			var fit = it;
			dbh2Splitting.getRaftAgent().send(puts, (p) -> splitPutNext(isMove, (SplitPut)p, fit, serialNo));
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
		for (; it.isValid() && count > 0; it.next(), --count) {
			r.Argument.getPuts().put(new Binary(it.key()), new Binary(it.value()));
		}
		return r;
	}

	public long splitPutNext(boolean isMove, SplitPut r, RocksIterator it, long serialNo) {
		try {
			// RaftApplied豁免为成功（FND28 F2，对齐Dbh2Agent.get/CommitRocks四处的既有先例）：
			// SplitPut应答丢失→Agent 1s重发→服务端unique-request重放回RaftApplied——页面已
			// 落盘（apply已发生），按错误重入startSplit会从分界键整段重拷贝（目标侧putIfAbsent
			// 幂等，纯扰动/带宽浪费）。
			var hasError = r.getResultCode() != 0 && r.getResultCode() != Procedure.RaftApplied;
			if (!raft.isLeader() || dbh2Splitting == null || serialNo != splitSerialNo) {
				it.close();
				// 身份失配不重试：重试的前提是回调仍代表当前轮（本机leader且serialNo未失配），
				// 对齐下面hasError分支"本机仍leader才重试"的本意。失配分支的经典来源：新轮启动后
				// （recoverSplitting重入），endSplit2的dbh2Splitting.close()以
				// Procedure.Timeout同步触发旧轮悬挂中的SplitPut回调——此时要么已有更新的轮次接管
				// （重试职责属于它），要么分桶已完结（是否再分桶交还loadMonitor决策）。以陈旧上下文
				// 无差别重试startSplit，会在无人决策的情况下发起一轮全新的分桶：再建桶、全量拷贝、
				// 再走完整收尾（可能携与当下负载条件不符的陈旧isMove），并在新轮进行中时提前drain
				// 正在阻塞的prepareQueue，扩大扰动。
				if (hasError && raft.isLeader() && serialNo == splitSerialNo)
					startSplit(isMove); // 失配仅因dbh2Splitting==null：本轮上下文仍有效，按原语义重试
				return 0;
			}

			if (hasError) {
				it.close(); // 目标桶错误但本机仍leader：先释放钉定的迭代器再重试，避免泄漏
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
			it.close(); // 异常路径统一释放迭代器；close幂等，前面分支已关闭过时安全
			logger.error("isMove={}", isMove, ex);
			// 异常吞掉即断页面拷贝链：同leader任期内tryStartSplit对splitting!=null早退、recoverSplitting
			// 只在换主触发，分桶悬挂。对齐hasError分支重入startSplit续走resume，重入条件同失配分支的
			// 特例线（本机仍leader且serialNo未失配，覆盖catch内两类来源：正常路径异常与嵌套startSplit
			// 自身的失败重试）。resume从分界key重建迭代器重拷贝，目标侧putIfAbsent幂等；新serialNo作废
			// 本轮陈旧回调。重入自身失败只记日志，留待换主recoverSplitting自愈。
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

	private void blockPrepareUntilNoTransaction(boolean isMove) {
		var meta = stateMachine.getBucket().getBucketMeta();
		var splittingMeta = stateMachine.getBucket().getSplittingMeta();
		logger.info("splitting end ... isMove={} {}->{}",
				isMove, formatMeta(meta), formatMeta(splittingMeta));

		// 截住新的事务请求。
		var server = (Dbh2RaftServer)getRaft().getServer();
		server.setupPrepareQueue();

		// 设置一个超时，每秒放行一次。
		TaskSpec.ofAction(() -> getRaft().executeUserTask(() -> consumePrepareAndBlockAgain(isMove))).scheduleNow(1000);

		// 在队列中增加endSplit启动任务，先要处理完队列中的请求。
		// 此时PrepareBatch已经被拦截，但是还有CommitBatch,UndoBatch等其他请求在处理。
		// setupHandleIfNoTransaction 将在没有进行中的事务时触发。
		getRaft().executeUserTask(() -> stateMachine.setupOneShotIfNoTransaction(() -> endSplit0(isMove)));
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
		if (!raft.isLeader())
			return; // 掉主：重试链就此终止，新leader经recoverSplitting→startSplit续走全流程
		var splittingMeta = stateMachine.getBucket().getSplittingMeta();
		if (null == splittingMeta) {
			// 安全网：任何路径在LogSetSplittingMeta apply前触达这里都不能带null继续——
			// setBucketMetaAsync(null)必NPE，且one-shot已被消耗、prepare队列永久积压。
			// 延迟重试而非直接重排：transactions为空时setupOneShotIfNoTransaction会立即内联执行，
			// 直接重排等于热自旋（已提交日志必会apply；未提交则换主后recoverSplitting自愈）。
			logger.warn("endSplit0 wait splittingMeta apply. isMove={}", isMove);
			TaskSpec.ofAction(() -> getRaft().executeUserTask(
					() -> stateMachine.setupOneShotIfNoTransaction(() -> endSplit0(isMove)))).schedule(1000);
			return;
		}
		// 【endSplit前置条件·dbh2-01】事务同步队列必须全部送达（水位==序号）才能进入收尾：
		// 此刻无在途事务（one-shot保证）且prepare已被拦截，队列不再增长；未清空先尝试投递，
		// 1s后经one-shot重查（防热自旋，与上面null-meta分支同机制）。不得越过本门槛——
		// setBucketMeta/EndSplit随后的源桶deleteToEnd会物理销毁[M,∞)，未送达的写与墓碑将无处可寻。
		if (stateMachine.hasPendingSplitSync()) {
			driveSplitSync();
			TaskSpec.ofAction(() -> getRaft().executeUserTask(
					() -> stateMachine.setupOneShotIfNoTransaction(() -> endSplit0(isMove)))).schedule(1000);
			return;
		}
		// 第一步，设置新桶的meta
		dbh2Splitting.setBucketMetaAsync(splittingMeta, (p) -> endSplit1(p, isMove));
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
			// 原子化设置源桶状态（修改源桶Meta；保存新桶Meta到历史中；删除分桶Meta）。
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
				// leaderCallback在持raft.mutex线程上执行（invokeCallback/cancelCallback），
				// 禁止同步进入startSplit（锁序见startSplitAsync）。
				startSplitAsync(isMove);
			} catch (Exception e) {
				logger.error("isMove={}", isMove, e);
			}
			return;
		}

		// 此时进入拒绝模式
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
			// onSettled终局回调：settle成功（rc==0）或eSplittingBucketNotFound
			// （已结算证据）时追加标志清除日志——apply侧随LogEndMove落下的pending-settle
			// 标志由此收回；进程在终局前死亡则标志留存，下轮leader-ready补发终局后同样清除。
			manager.getMasterAgent().endMoveWithRetryAsync(endMove.getTo(),
					() -> appendClearPendingSettle(endMove.getTo()));
		} else {
			// 可以安全的发布新旧桶的信息到Master了。
			var endSplit = (LogEndSplit)raftLog.getLog();
			manager.getMasterAgent().endSplitWithRetryAsync(endSplit.getFrom(), endSplit.getTo(),
					() -> appendClearPendingSettle(endSplit.getTo()));
		}
		logger.info("splitting end done. isMove={} {}", isMove, formatMeta(meta));
	}

	// 分桶事务同步投递链（dbh2-01）：持久队列（Dbh2StateMachine.splitSync*）按seq序合并取批、
	// 单rpc在途（inFlight的CAS串行，全调用源非阻塞）、ACK 0推进水位后续投、终局失败不推进水位
	// 1s后重投——条目留队列直到送达，无"发出但结果不明"的中间态。与页面拷贝链互不干扰：页面
	// putIfAbsent只增（对非null含墓碑跳过），投递replace覆盖且队列FIFO后缀必然覆盖一切旧值，
	// 两类rpc任意乱序到达目标桶都收敛到提交序终值。
	// 换主围栏：replace语义的投递rpc迟到落盘会以旧值覆盖新值（跨leader打破FIFO）。旧leader在途
	// 投递rpc的最迟重发时刻=创建+AgentTimeout，而其创建早于本节点leaderReady——故本任期内首个
	// 投递不得早于leaderReadyTime+AgentTimeout+裕量。页面拷贝不受此限（只增语义，迟到旧页面无害）。
	// 调用源：enqueue（apply线程）、startSplit/endSplit0（user task）、ACK回调、失败重试定时。
	public void driveSplitSync() {
		if (!splitSyncInFlight.compareAndSet(false, true))
			return; // 已有在途投递，其回调会续链
		try {
			var agent = dbh2Splitting; // 快照：与endSplit2置null的并发窗口内不产生NPE噪声
			if (closed || !raft.isLeader() || null == agent) {
				splitSyncInFlight.set(false);
				return; // 条目留队列：leader-ready恢复（recoverSplitting→startSplit）或endSplit0门槛再驱动
			}
			var delay = splitLeaderReadyTime + splitSyncFenceDelayMs(agent) - System.currentTimeMillis();
			if (delay > 0) {
				splitSyncInFlight.set(false); // 先放行再调度：调度任务重新CAS进入
				TaskSpec.ofAction(this::driveSplitSync).schedule(delay);
				return;
			}
			var batch = stateMachine.pollSplitSync(dbh2Config.getSplitPutCount());
			if (null == batch) {
				splitSyncInFlight.set(false);
				return; // 队列空：等待下一次enqueue或门槛驱动
			}
			var r = new SplitPut(batch.data);
			agent.getRaftAgent().send(r, (p) -> {
				// 回调在user-task线程（RaftAgentNetClient.dispatchRpcResponse→executeUserTask按raft名串行）
				// RaftApplied豁免（FND28 F2，对齐splitPutNext/Dbh2Agent.get先例）：应答丢失重发被
				// 服务端unique-request重放回RaftApplied=批次已落盘，照常推进水位续投；不豁免则
				// 不推水位、1s后重投同批（replace幂等，一轮收敛的纯扰动）。
				if (r.getResultCode() != 0 && r.getResultCode() != Procedure.RaftApplied) {
					splitSyncInFlight.set(false);
					// 终局失败（超时/非重试错误）：不推进水位，条目留队列定时重投——目标桶恢复即送达
					TaskSpec.ofAction(this::driveSplitSync).schedule(1000);
					return 0;
				}
				try {
					stateMachine.advanceSplitSyncWatermark(batch); // ACK=目标raft已commit并apply，送达成立（世代失配=旧队列迟到ACK，内部拒绝）
				} catch (Exception ex) {
					logger.error("advanceSplitSyncWatermark", ex);
					splitSyncInFlight.set(false);
					TaskSpec.ofAction(this::driveSplitSync).schedule(1000); // 水位未推进：重投（幂等）
					return 0;
				}
				splitSyncInFlight.set(false);
				driveSplitSync(); // 续投下一批（CAS重入）
				return 0;
			});
		} catch (Exception ex) {
			logger.error("driveSplitSync", ex);
			splitSyncInFlight.set(false);
			TaskSpec.ofAction(this::driveSplitSync).schedule(1000);
		}
	}

	private static long splitSyncFenceDelayMs(Dbh2Agent agent) {
		// AgentTimeout=投递rpc判死门槛（不设显式超时，走Agent默认）：旧leader的rpc在创建+
		// AgentTimeout后从pending移除、不再重发；+2000为1s重发扫描周期与调度抖动裕量。
		return agent.getRaftAgent().getRaftConfig().getAgentTimeout() + 2000L;
	}

	@Override
	protected long ProcessSplitPutRequest(SplitPut r) {
		raft.appendLog(new LogSplitPut(r),
				(raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));
		return 0;
	}

}
