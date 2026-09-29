package Zeze.Dbh2;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BSplitPut;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.Raft;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Random;
import Zeze.Util.RocksDatabase;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

/**
 * Dbh2 桶的 Raft 状态机：apply 各类日志到桶存储，管理桶内事务与负载统计。
 */
public class Dbh2StateMachine extends Zeze.Raft.StateMachine {
	private static final Logger logger = LogManager.getLogger(Dbh2StateMachine.class);
	private Bucket bucket;
	private TidAllocator tidAllocator;
	private final ConcurrentHashMap<Long, Dbh2Transaction> transactions = new ConcurrentHashMap<>();

	// 分桶事务同步持久队列（dbh2-01）：复用bucket的meta列族，key首字节区分用途（既有meta键
	// {1}..{4}与空键不冲突）：5+BE seq=队列记录（value=BSplitPut(fromTransaction=true)编码，
	// delete编码为Binary.Empty墓碑put）；{6}=入队序号计数器；{7}=已投递水位。与bucket同库：
	// 随checkpoint快照、随raft日志apply确定重放——任何副本（含换主后的新leader）apply后即持有
	// 同一队列，事务同步义务不随leader死亡灭失。
	// 写线程约束：记录/计数器只在raft apply线程写（enqueue/clear，与既有apply串行）；水位只在
	// 投递回调线程写（单rpc在途，Dbh2.driveSplitSync的CAS串行）。volatile供对端线程读到最新值。
	// splitSyncGeneration=队列世代号（clearSplitSyncQueue换新时递增）：seq在换代后从1重新分配，
	// 旧世代迟到的投递ACK若按seq推进水位，会把新世代未投递的记录误标为已送达（endSplit0门槛
	// 假通、源桶deleteToEnd灭失数据）——advanceSplitSyncWatermark按世代戳拒绝陈旧ACK。
	private static final byte SplitSyncRecordPrefix = 5;
	private static final byte[] SplitSyncSeqKey = {6};
	private static final byte[] SplitSyncWatermarkKey = {7};
	private RocksDatabase.Table splitSyncTable;
	private volatile long splitSyncSeq;
	private volatile long splitSyncWatermark;
	private volatile long splitSyncGeneration;
	// advance与clear互斥：世代守卫是check-then-act（读世代→写水位→持久化{7}），不互斥时apply线程
	// 的clear可插进守卫与写之间——陈旧ACK把旧世代seq写回水位并复活磁盘{7}（重启经openBucket复活），
	// hasPendingSplitSync假通使endSplit0门槛失守、deleteToEnd灭失未投递记录。叶子锁：apply线程在
	// raft.mutex下取本锁，投递回调线程只取本锁（锁内仅rocksdb写，不回取raft锁），单向无ABBA。
	private final ReentrantLock splitSyncLock = new ReentrantLock();

	private static byte[] splitSyncRecordKey(long seq) {
		var bb = ByteBuffer.Allocate(9); // 1前缀+8BE long：BE序=数值序（seq单调非负），键序即投递序
		bb.WriteByte(SplitSyncRecordPrefix);
		bb.WriteLong8BE(seq);
		return bb.Bytes;
	}

	// eCommitting悬挂告警阈值=10×bucketMaxTime（默认1000s）。推导：正常redo收敛时间=redoDaemon周期
	//（60s）+目标桶raft可用时间，bucketMaxTime(100s)>prepareMaxTime(80s)的既有排序已覆盖协调周期；
	// 10×（默认1000s，约16个redo周期）远超任何正常收敛时间仍停在此状态，才认定"协调者已决定提交
	// 但长期不redo"的灾难形态（CommitRocks损坏/redo永久失败）。
	private static final int CommittingHangWarnFactor = 10;
	// 告警去重（对齐OnzServer.hangWarnedTids形态）：每tid只error一次；事务完结（commit/undo）即回收，
	// 集合有界于悬挂事务数。
	private final ConcurrentHashMap.KeySetView<Long, Boolean> committingHangWarnedTids = ConcurrentHashMap.newKeySet();

	// ----- 自主 undo 未确认墓碑（FND29 dbh2-03 断根的兜底层）-----
	// 桶侧 onTimer 自主 undo 落日志时协调者决策未知：事务不立即毁尸（锁释放、trans blob
	// 保留、入本表）。围栏冲突的三种收敛：①迟到 LogCommitBatch（协调者已持久化
	// eCommitting，commitPoint 存在）在墓碑窗内到达——复活并提交，数据不丢，客户端
	// 成功变真；②协调者驱动的 UndoBatch 到达=确认 undo 终局，物理删除；③墓碑窗超时
	// 仍无协调者消亡（进程丢失/极端病理）——物理删除并响亮 error（可见化，对齐
	// eCommitting 悬挂告警姿态）。主防线=onTimer 年龄判据用单调钟（Dbh2Transaction.
	// elapsedMillis，墙钟步进免疫），墓碑是防御纵深：任何残余破栅形态（重启窗口、
	// 极端速率分歧）由复活/告警兜住，"已确认提交而数据灭失"的形态不再存在。
	private static final class UndonePending {
		final Dbh2Transaction txn;
		final long tombstoneNanos;

		UndonePending(Dbh2Transaction txn) {
			this.txn = txn;
			this.tombstoneNanos = System.nanoTime();
		}
	}

	private final ConcurrentHashMap<Long, UndonePending> undonePending = new ConcurrentHashMap<>();

	/** 墓碑窗（毫秒）：覆盖协调者在其自身 prepare 窗口内的最迟 commit 决策 + CommitBatch
	 * 在途（rpcTimeout）+ 余量。低于此窗的迟到 commit 将落入超窗删除分支（协调者 rpc
	 * 必已失败、客户端已见失败，非静默）。 */
	long undoResurrectGraceMillis() {
		var conf = dbh2.getDbh2Config();
		return conf.getRpcTimeout() + conf.getPrepareMaxTime() + 30_000L;
	}

	/** 墓碑超窗清扫（包内可见供确定性测试）：超窗未决的墓碑物理删除 blob 并响亮告警。 */
	void expireDueTombstones(long graceMillis) {
		var graceNanos = graceMillis * 1_000_000L;
		for (var e : undonePending.entrySet()) {
			if (System.nanoTime() - e.getValue().tombstoneNanos < graceNanos)
				continue;
			var pending = undonePending.remove(e.getKey());
			if (null == pending)
				continue;
			try {
				pending.txn.undoBatch(bucket);
				logger.error("undo tombstone expired without coordinator resolution: tid={} (physical undo"
						+ " applied; coordinator neither confirmed undo nor delivered commit within {}ms --"
						+ " divergence possible, manual check)", e.getKey(), graceMillis);
			} catch (RocksDBException ex) {
				logger.error("expire undo tombstone fail, retain for next round: tid={}", e.getKey(), ex);
				undonePending.putIfAbsent(e.getKey(), pending);
			}
		}
	}

	private Future<?> timer;
	private CommitAgent commitAgent;
	private final Dbh2 dbh2;
	// 访问由noTransactionLock保护：setupOneShotIfNoTransaction在user-task线程，triggerNoTransactionIf在raft apply线程。
	// 两个线程默认都是虚拟线程,ReentrantLock保证handle将来混入阻塞调用时也不pin载体(JDK21-23)。
	private final ReentrantLock noTransactionLock = new ReentrantLock();
	private Runnable noTransactionHandle;

	final AtomicLong counterGet = new AtomicLong();
	private final AtomicLong counterPut = new AtomicLong();
	final AtomicLong sizeGet = new AtomicLong();
	private final AtomicLong sizePut = new AtomicLong();
	private final AtomicLong counterDelete = new AtomicLong();
	private final AtomicLong counterPrepareBatch = new AtomicLong();
	private final AtomicLong counterCommitBatch = new AtomicLong();
	private final AtomicLong counterUndoBatch = new AtomicLong();

	private long lastGet;
	private long lastPut;
	private long lastSizeGet;
	private long lastSizePut;
	private long lastDelete;
	private long lastPrepareBatch;
	private long lastCommitBatch;
	private long lastUndoBatch;
	private long lastReportTime = System.currentTimeMillis();
	private boolean loadSwitch = false;
	// load被loadMonitor定时器线程与setLoadSwitch（raft回调线程）并发调用，last*统计字段需要同步保护。
	private final ReentrantLock loadLock = new ReentrantLock();

	public void setLoadSwitch(boolean value) {
		loadLock.lock();
		try {
			load(); // 修改loadSwitch强制报告一次，达到清理旧的load的目的。loadLock可重入，持锁调用load()安全
			loadSwitch = value;
		} finally {
			loadLock.unlock();
		}
	}

	public double load() {
		loadLock.lock();
		try {
			var now = System.currentTimeMillis();
			// 除零守卫：loadMonitor周期与setLoadSwitch的强制报告同毫秒先后进入时elapse==0，
			// 速率=Infinity会误触发分桶决策并污染master负载排序。最小分母1ms（时钟回拨的负值同样钳制）。
			var elapse = Math.max((now - lastReportTime) / 1000.0f, 0.001f);
			lastReportTime = now;

			var nowGet = counterGet.get();
			var nowPut = counterPut.get();
			var nowSizeGet = sizeGet.get();
			var nowSizePut = sizePut.get();
			var nowDelete = counterDelete.get();
			var nowPrepareBatch = counterPrepareBatch.get();
			var nowCommitBatch = counterCommitBatch.get();
			var nowUndoBatch = counterUndoBatch.get();

			var diffGet = nowGet - lastGet;
			var diffPut = nowPut - lastPut;
			var diffSizeGet = nowSizeGet - lastSizeGet;
			var diffSizePut = nowSizePut - lastSizePut;
			var diffDelete = nowDelete - lastDelete;
			var diffPrepareBatch = nowPrepareBatch - lastPrepareBatch;
			var diffCommitBatch = nowCommitBatch - lastCommitBatch;
			var diffUndoBatch = nowUndoBatch - lastUndoBatch;

			if (diffGet > 0 || diffPut > 0 || diffDelete > 0 || diffSizeGet > 0 || diffSizePut > 0
					|| diffPrepareBatch > 0 || diffCommitBatch > 0 || diffUndoBatch > 0) {
				lastGet = nowGet;
				lastPut = nowPut;
				lastSizeGet = nowSizeGet;
				lastSizePut = nowSizePut;
				lastDelete = nowDelete;
				lastPrepareBatch = nowPrepareBatch;
				lastCommitBatch = nowCommitBatch;
				lastUndoBatch = nowUndoBatch;

				var avgGet = diffGet / elapse;
				var avgPut = diffPut / elapse;
				var avgDelete = diffDelete / elapse;

				//noinspection StringBufferReplaceableByString
				var sb = new StringBuilder();
				sb.append("load: ");
				sb.append(Dbh2.formatMeta(getBucket().getBucketMeta()));
				sb.append(" get=").append(avgGet);
				sb.append(" put=").append(avgPut);
				sb.append(" getSize=").append(diffSizeGet / elapse);
				sb.append(" putSize=").append(diffSizePut / elapse);
				sb.append(" delete=").append(avgDelete);
				sb.append(" prepare=").append(diffPrepareBatch / elapse);
				sb.append(" commit=").append(diffCommitBatch / elapse);
				sb.append(" undo=").append(diffUndoBatch / elapse);

				logger.info("{}", sb.toString());

				// 负载，put，delete全算，get算1%。
				return loadSwitch ? (avgPut + avgDelete) + avgGet * 0.01 : 0.0;
				// loadSwitch 没有生效前总是报告负载为0，但是上面的日志还是记录了。
			}
			return 0.0;
		} finally {
			loadLock.unlock();
		}
	}

	public Dbh2StateMachine(Dbh2 dbh2) {
		this.dbh2 = dbh2;

		super.addFactory(LogPrepareBatch.TypeId_, LogPrepareBatch::new);
		super.addFactory(LogCommitBatch.TypeId_, LogCommitBatch::new);
		super.addFactory(LogUndoBatch.TypeId_, LogUndoBatch::new);
		super.addFactory(LogSetBucketMeta.TypeId_, LogSetBucketMeta::new);
		super.addFactory(LogAllocateTid.TypeId_, LogAllocateTid::new);

		super.addFactory(LogEndSplit.TypeId_, LogEndSplit::new);
		super.addFactory(LogSetSplittingMeta.TypeId_, LogSetSplittingMeta::new);
		super.addFactory(LogSplitPut.TypeId_, LogSplitPut::new);

		super.addFactory(LogEndMove.TypeId_, LogEndMove::new);
		super.addFactory(LogClearPendingSettle.TypeId_, LogClearPendingSettle::new);
	}

	public void setupOneShotIfNoTransaction(Runnable handle) {
		noTransactionLock.lock();
		try {
			if (transactions.isEmpty())
				handle.run(); // 异步网络调用，持锁执行无阻塞风险
			else
				noTransactionHandle = handle;
		} finally {
			noTransactionLock.unlock();
		}
	}

	public boolean hasNoTransactionHandle() {
		noTransactionLock.lock();
		try {
			return noTransactionHandle != null;
		} finally {
			noTransactionLock.unlock();
		}
	}

	// 门槛触发（commitBatch/undoBatch的apply尾部，raft apply线程）：锁内摘handle后转投
	// raft串行执行器重新过闸（setupOneShotIfNoTransaction），不得在apply线程内联运行——
	// 全序化不变量（dbh2-01）：所有门槛判定与所有PrepareBatch注册必须落在同一条FIFO序
	// 上：装载（拦截队列）先于提交⇒拦截；提交先于装载⇒FIFO先注册⇒过闸看到非空事务而
	// 推迟。内联运行时门槛收口无序于拦截排空的注册，晚注册事务落入迁出键域的写入被收尾
	// deleteToEnd静默丢弃。
	private void triggerNoTransactionIf() {
		Runnable handle;
		noTransactionLock.lock();
		try {
			if (!transactions.isEmpty() || null == noTransactionHandle)
				return;
			handle = noTransactionHandle;
			noTransactionHandle = null;
		} finally {
			noTransactionLock.unlock();
		}
		dbh2.getRaft().executeUserTask(() -> setupOneShotIfNoTransaction(handle));
	}

	public Bucket getBucket() {
		return bucket;
	}

	public TidAllocator getTidAllocator() {
		return tidAllocator;
	}

	public ConcurrentHashMap<Long, Dbh2Transaction> getTransactions() {
		return transactions;
	}

	public void openBucket() {
		if (bucket != null)
			return;
		bucket = new Bucket(getRaft().getRaftConfig());
		tidAllocator = new TidAllocator();
		// 同步队列依附bucket的meta列族打开，并从落盘状态恢复序号/水位（restore路径同样经此重开）。
		try {
			splitSyncTable = bucket.getDb().getOrAddTable("meta");
		} catch (RocksDBException e) {
			throw new RuntimeException(e); // 与Bucket构造同形态：打开期rocksdb失败即建桶失败
		}
		splitSyncSeq = readSplitSyncCounter(SplitSyncSeqKey);
		splitSyncWatermark = readSplitSyncCounter(SplitSyncWatermarkKey);

		if (null == timer) {
			var period = getRaft().getRaftConfig().getAppendEntriesTimeout() + 200;
			var delay = Random.getInstance().nextLong(period);
			timer = TaskSpec.ofAction(this::onTimer).schedulePeriodNow(delay, period);
		}

		if (null == commitAgent)
			commitAgent = new CommitAgent();
	}

	private long readSplitSyncCounter(byte[] key) {
		try {
			var value = splitSyncTable.get(key);
			return null == value ? 0 : ByteBuffer.Wrap(value).ReadLong8BE();
		} catch (RocksDBException e) {
			throw new RuntimeException(e); // 与Bucket构造同形态：打开期rocksdb读失败即建桶失败
		}
	}

	private void onTimer() {
		if (!getRaft().isLeader())
			return;

		// 年龄判据用单调钟（FND29 dbh2-03 断根）：墙钟步进（NTP步进/VM恢复）可把仍在协调者
		// 合法 prepare 窗口内的事务误判超时，误 undo 已决定提交的事务=客户端确认成功而
		// 数据灭失；elapsedMillis（nanoTime）对步进免疫，两机真实速率漂移远小于配置余量。
		for (var e : transactions.entrySet()) {
			var tid = e.getKey();
			// 单条隔离：CommitAgent.query对CommitServer短暂不可达抛RuntimeException，不隔离会
			// 中止本轮剩余悬挂事务的超时检查（不可达查询卡住遍历首位，同轮后续undo判定逐轮推迟）。
			try {
				var t = e.getValue();
				if (t.elapsedMillis() < dbh2.getDbh2Config().getBucketMaxTime())
					continue;
				var state = commitAgent.query(t.getQueryIp(), t.getQueryPort(), tid, dbh2.getDbh2Config().getRpcTimeout());
				if (Commit.eCommitNotExist == state.getState()
						|| Commit.ePreparing == state.getState()) {
					logger.warn("timeout undo tid={} state={}", tid, state);
					getRaft().appendLog(new LogUndoBatch(tid));
				} else if (Commit.eCommitting == state.getState()
						&& t.elapsedMillis() >= dbh2.getDbh2Config().getBucketMaxTime() * CommittingHangWarnFactor
						&& committingHangWarnedTids.add(tid)) {
					// 2PC语义：协调者已保存commitPoint(eCommitting)，桶侧无信息安全终局（误undo=跨桶
					// 部分提交），只告警不自动终局；恢复依赖协调者CommitRocks存活，灾难场景重建协调者
					// 进程即收敛（见docs dbh2.md）。
					logger.error("eCommitting transaction hang: tid={} query={}:{} age={}ms; "
									+ "coordinator commit-point exists but redo not arriving",
							tid, t.getQueryIp(), t.getQueryPort(), t.elapsedMillis());
				}
			} catch (Exception ex) {
				logger.warn("onTimer check hanging transaction fail. tid={}", tid, ex);
			}
		}
		expireDueTombstones(undoResurrectGraceMillis());
	}

	public void setBucketMeta(BBucketMeta.Data argument) {
		try {
			bucket.setBucketMeta(argument);
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	public void setSplittingMeta(BBucketMeta.Data argument) {
		try {
			bucket.setSplittingMeta(argument);
			// 新分桶世代起点：防御清零（正常上一世代已在EndSplit/EndMove apply时清空；本apply
			// 在日志序上先于任何入队，清零不吞记录——快照恢复的副本不会重放本日志）。
			clearSplitSyncQueue();
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	// LogClearPendingSettle.apply入口：身份匹配清除标志，不匹配为陈旧世代日志，no-op。
	public void clearPendingSettle(BBucketMeta.Data to) {
		try {
			bucket.clearPendingSettle(to);
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	private static final Binary emptyBucketMetaKey = new Binary(new byte[]{1});

	public void endMove(BBucketMeta.Data to) {
		try (var it = bucket.getData().iterator()) {
			it.seekToFirst();
			bucket.getData().deleteToEnd(it);

			// 被移走的桶Meta置空（使用相同的非空key）。
			// 将会拒绝所有对这个桶的访问。
			var emptyMeta = bucket.getBucketMeta().copy();
			emptyMeta.setKeyFirst(emptyBucketMetaKey);
			emptyMeta.setKeyLast(emptyBucketMetaKey);
			bucket.setBucketMeta(emptyMeta);
			bucket.addMoveMetaHistory(to);
			// pending-settle标志：与既有meta写入同一apply内落盘（派生状态，随raft
			// 复制/快照）——迁移已在源桶commit的持久证据，leader-ready据此幂等补发settle通知。
			bucket.setPendingSettle(null, to);
			bucket.deleteSplittingMeta();
			clearSplitSyncQueue(); // 迁移完结：义务已全部送达（endSplit0门槛），队列随世代消亡
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	public void endSplit(BBucketMeta.Data from, BBucketMeta.Data to) {
		try (var it = bucket.getData().iterator()) {
			it.seek(from.getKeyLast().copyIf());
			bucket.getData().deleteToEnd(it);
			bucket.setBucketMeta(from);
			bucket.addSplitMetaHistory(from, to);
			// 同endMove：pending-settle标志随迁移commit在apply内落盘。
			bucket.setPendingSettle(from, to);
			bucket.deleteSplittingMeta();
			clearSplitSyncQueue(); // 迁移完结：义务已全部送达（endSplit0门槛），队列随世代消亡
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	/// //////////////////////////////////////////////////////////////////
	// 下面这些方法用于Log.apply，不能失败，失败将停止程序。
	public void allocateTid(long range) {
		try {
			var start = bucket.getTid();
			var end = start + range;
			bucket.setTid(end);
			tidAllocator.setRange(start, end);
		} catch (RocksDBException ex) {
			logger.error("", ex);
			getRaft().fatalKill();
		}
	}

	private Dbh2Transaction getOrAddTransaction(BBatch.Data batch) {
		return transactions.computeIfAbsent(batch.getTid(),
				_tid -> {
					try {
						return new Dbh2Transaction(dbh2, batch);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
	}

	public void prepareBatch(BBatch.Data batch) {
		try {
			counterPrepareBatch.incrementAndGet();
			var txn = getOrAddTransaction(batch);
			counterPut.addAndGet(txn.getBatch().getPuts().size());
			var totalPutValueSize = 0;
			for (var e : txn.getBatch().getPuts().entrySet()) {
				totalPutValueSize += e.getKey().size();
				totalPutValueSize += e.getValue().size();
			}
			sizePut.addAndGet(totalPutValueSize);
			counterDelete.addAndGet(txn.getBatch().getDeletes().size());
			txn.prepareBatch(bucket);
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	public void commitBatch(long tid) {
		try (var txn = transactions.remove(tid)) {
			counterCommitBatch.incrementAndGet();
			committingHangWarnedTids.remove(tid); // 悬挂告警集合随事务完结回收（有界性）
			if (null != txn) {
				// 分桶中的事务同步：入队（持久、可靠重投）替代原急切rpc（dbh2-01）。入队先于本地
				// 落盘：两写在同一apply内，崩溃经raft重放整体重演，先后无原子性要求。
				enqueueSplitSync(txn.getBatch());
				txn.commitBatch(bucket);
			} else {
				var pending = undonePending.remove(tid);
				if (null != pending) {
					// 围栏冲突复活（FND29 dbh2-03）：undo 已落日志但协调者已持久化 eCommitting
					//（commitPoint 存在，客户端将收到/已收到成功）——迟到的 LogCommitBatch 以
					// 提交为终局：数据落盘、blob 清除，成功应答变真而非静默灭失；error 留痕供对账。
					// 复活的 txn 在墓碑化时已释放锁，commitBatch(bucket) 只触存储不触锁。
					logger.error("commitBatch resurrects tombstoned transaction (undo/commit fence conflict):"
							+ " tid={} (undo applied first but coordinator commit-point exists)", tid);
					enqueueSplitSync(pending.txn.getBatch());
					pending.txn.commitBatch(bucket);
				} else
					logger.warn("commitBatch but transaction not found. tid={}", tid);
			}
			triggerNoTransactionIf();
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	/** Raft 日志 apply 入口（{@link LogUndoBatch}）。fromCoordinator：true=协调者驱动的
	 * UndoBatch（决策已终局，立即物理删除）；false=桶侧 onTimer 自主超时 undo（协调者
	 * 决策未知，未确认墓碑——迟到 commit 可复活，见 undonePending 注释）。 */
	public void undoBatch(long tid, boolean fromCoordinator) {
		try (var txn = transactions.remove(tid)) {
			counterUndoBatch.incrementAndGet();
			committingHangWarnedTids.remove(tid); // 悬挂告警集合随事务完结回收（有界性）
			if (null != txn) {
				if (fromCoordinator)
					txn.undoBatch(bucket); // 协调者终局：决策与删除同源，无围栏冲突窗口
				else
					undonePending.put(tid, new UndonePending(txn)); // 自主undo：锁随try块释放，blob待终局
			} else {
				var pending = undonePending.remove(tid);
				if (null != pending)
					// 协调者 UndoBatch 追认自主 undo：undo 终局确认，物理删除。
					pending.txn.undoBatch(bucket);
				else
					logger.warn("undoBatch but transaction not found. tid={}", tid);
			}
			triggerNoTransactionIf();
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	// 分桶事务同步入队（dbh2-01）：入队条件=splittingMeta!=null——LogSetSplittingMeta的apply序
	// 即日志序边界，全副本确定一致；边界后提交的事务要么入队（可靠重投，见Dbh2.driveSplitSync），
	// 要么在复制迭代器视图内（迭代器在meta apply之后才创建，见Dbh2.startSplit），无第三种去向。
	// delete编码为Binary.Empty墓碑put（目标侧applySplitPut的fromTransaction分支原样落盘）。
	private void enqueueSplitSync(BBatch.Data batch) throws RocksDBException {
		var splitting = bucket.getSplittingMeta();
		if (null == splitting)
			return;
		var puts = new HashMap<Binary, Binary>();
		for (var e : batch.getPuts().entrySet()) {
			if (splitting.getKeyFirst().compareTo(e.getKey()) <= 0)
				puts.put(e.getKey(), e.getValue());
		}
		for (var del : batch.getDeletes()) {
			if (splitting.getKeyFirst().compareTo(del) <= 0)
				puts.put(del, Binary.Empty); // 同批put+delete同key时delete后写覆盖——与txn.commitBatch先put后delete的落盘序一致
		}
		if (puts.isEmpty())
			return;
		// 记录先写、计数器后写：两写间崩溃时raft重放本条日志，seq按持久计数器重算，同seq重写同内容
		//（幂等）；极端交错至多多出一条同内容重复记录，重复投递幂等无害。
		var seq = splitSyncSeq + 1;
		var bb = ByteBuffer.Allocate();
		new BSplitPut.Data(true, puts).encode(bb);
		var key = splitSyncRecordKey(seq);
		splitSyncTable.put(bucket.getWriteOptions(), key, 0, key.length, bb.Bytes, bb.ReadIndex, bb.WriteIndex);
		splitSyncSeq = seq;
		var seqBb = ByteBuffer.Allocate(8);
		seqBb.WriteLong8BE(seq);
		splitSyncTable.put(bucket.getWriteOptions(), SplitSyncSeqKey, 0, SplitSyncSeqKey.length, seqBb.Bytes, 0, 8);
		// 唤醒投递链（非阻塞；直发还是围栏延迟由Dbh2.driveSplitSync自决）。
		dbh2.driveSplitSync();
	}

	public static final class SplitSyncBatch {
		public final BSplitPut.Data data;
		public final long lastSeq;
		public final long generation;

		SplitSyncBatch(BSplitPut.Data data, long lastSeq, long generation) {
			this.data = data;
			this.lastSeq = lastSeq;
			this.generation = generation;
		}
	}

	// 取一批待投递记录（水位+1起至多maxCount条，按seq序合并为一个puts——后写覆盖先写=提交序，
	// 重复键保留最后值）。只读不推进：水位推进仅在投递ACK后（advanceSplitSyncWatermark）。
	// 调用方串行约束：仅Dbh2.driveSplitSync（CAS单飞）调用。
	// 世代戳在读取任何队列状态之前快照：与并发clear（apply线程）的交错中，旧世代记录要么
	// 携旧戳（ACK被拒，见advanceSplitSyncWatermark）要么已不可见（迭代器晚于删除创建，返回空）。
	public SplitSyncBatch pollSplitSync(int maxCount) {
		var generation = splitSyncGeneration;
		var watermark = splitSyncWatermark;
		if (watermark >= splitSyncSeq)
			return null;
		var puts = new HashMap<Binary, Binary>();
		long lastSeq = watermark;
		try (var it = splitSyncTable.iterator()) {
			it.seek(splitSyncRecordKey(watermark + 1));
			for (var count = 0; it.isValid() && count < maxCount; it.next()) {
				var key = it.key();
				if (key.length != 9 || key[0] != SplitSyncRecordPrefix)
					break; // 越出记录键域（{6}/{7}或其他meta键）：seq连续下不会发生，防御截断
				var record = new BSplitPut.Data();
				record.decode(ByteBuffer.Wrap(it.value()));
				puts.putAll(record.getPuts());
				lastSeq = ByteBuffer.Wrap(key, 1, 8).ReadLong8BE();
				count++;
			}
		}
		return lastSeq == watermark ? null : new SplitSyncBatch(new BSplitPut.Data(true, puts), lastSeq, generation);
	}

	// 投递ACK后推进水位。世代失配=本批取自已清空换代的旧队列（EndSplit/EndMove/SetSplittingMeta
	// apply清空后seq重新分配）：拒绝推进——旧批的送达事实属于已消亡的世代，按其seq推进会跳过
	// 新世代尚未投递的同号记录。拒绝不抛错：投递链照常续投（下批读当前世代）。
	// 守卫与写水位、持久化同在splitSyncLock临界区内：与clear的交错中关死check-then-act窗口。
	public void advanceSplitSyncWatermark(SplitSyncBatch batch) throws RocksDBException {
		splitSyncLock.lock();
		try {
			if (batch.generation != splitSyncGeneration)
				return;
			advanceSplitSyncWatermark(batch.lastSeq);
		} finally {
			splitSyncLock.unlock();
		}
	}

	// 投递ACK后推进水位（只前进）并持久化：同进程重启/复选续投免重放；其余副本水位为旧值，换主后
	// 从旧水位FIFO重投——重复投递幂等（replace同值），有序重放收敛无害。
	public void advanceSplitSyncWatermark(long seq) throws RocksDBException {
		splitSyncLock.lock();
		try {
			if (seq <= splitSyncWatermark)
				return;
			splitSyncWatermark = seq;
			var bb = ByteBuffer.Allocate(8);
			bb.WriteLong8BE(seq);
			splitSyncTable.put(bucket.getWriteOptions(), SplitSyncWatermarkKey, 0, SplitSyncWatermarkKey.length, bb.Bytes, 0, 8);
		} finally {
			splitSyncLock.unlock();
		}
	}

	// endSplit前置门槛（Dbh2.endSplit0）：false=全部事务同步已送达目标。
	public boolean hasPendingSplitSync() {
		return splitSyncWatermark < splitSyncSeq;
	}

	// 清空队列：EndSplit/EndMove apply=迁移完结、义务已全部送达（endSplit0门槛保证）；SetSplittingMeta
	// apply=新世代起点防御清零。全副本按apply序确定性执行，崩溃重放幂等。
	private void clearSplitSyncQueue() throws RocksDBException {
		splitSyncLock.lock();
		try {
			if (splitSyncSeq == 0 && splitSyncWatermark == 0)
				return;
			try (var it = splitSyncTable.iterator()) {
				it.seek(splitSyncRecordKey(1));
				for (; it.isValid(); it.next()) { // 迭代器为快照视图，遍历中delete不影响遍历
					var key = it.key();
					if (key.length != 9 || key[0] != SplitSyncRecordPrefix)
						break;
					splitSyncTable.delete(bucket.getWriteOptions(), key, 0, key.length);
				}
			}
			splitSyncTable.delete(bucket.getWriteOptions(), SplitSyncSeqKey, 0, SplitSyncSeqKey.length);
			splitSyncTable.delete(bucket.getWriteOptions(), SplitSyncWatermarkKey, 0, SplitSyncWatermarkKey.length);
			splitSyncSeq = 0;
			splitSyncWatermark = 0;
			// 单写者=apply线程串行（见字段区线程约束），++无丢失更新，volatile只为跨线程读可见
			//noinspection NonAtomicOperationOnVolatileField
			splitSyncGeneration++; // 换代：作废一切在途/未决的旧世代投递ACK（seq即将从1重新分配）
		} finally {
			splitSyncLock.unlock();
		}
	}

	/// /////////////////////////////////////////////////////////
	// raft implement
	public String getDbHome() {
		return getRaft().getRaftConfig().getDbHome();
	}

	@Override
	public SnapshotResult snapshot(String path) throws Exception {
		long t0 = System.nanoTime();
		SnapshotResult result = new SnapshotResult();
		var cpHome = checkpoint(result);

		// cpHome必须在所有离开路径上删除（try-finally）：backup及其后任一步抛出时遗留的
		// checkpoint_<ts>目录无回收路径（目录名含时间戳不复用），持续故障（备份盘满/IO错误）下
		// 随失败次数无界累积。deleteDirectory为best-effort不抛，不会掩盖try内原始异常。
		try {
			long t1 = System.nanoTime();
			var backupDir = Paths.get(getDbHome(), "backup").toString();
			var backupFile = new File(backupDir);
			if (!backupFile.isDirectory() && !backupFile.mkdirs())
				logger.error("create backup directory failed: {}", backupDir);
			RocksDatabase.backup(RocksDatabase.DbType.eRocksDb, cpHome, backupDir);

			long t2 = System.nanoTime();
			Zeze.Raft.RocksRaft.Rocks.createZipFromDirectory(backupDir, path);

			long t3 = System.nanoTime();
			getRaft().getLogSequence().commitSnapshot(path, result.lastIncludedIndex);

			result.success = true;
			result.checkPointNanoTime = t1 - t0;
			result.backupNanoTime = t2 - t1;
			result.zipNanoTime = t3 - t2;
			result.totalNanoTime = System.nanoTime() - t0;
		} finally {
			LogSequence.deleteDirectory(new File(cpHome));
		}
		return result;
	}

	public void close() throws Exception {
		// 各清理步骤独立捕获异常，保证后续清理继续执行。
		for (var tran : transactions.values()) {
			try {
				tran.close();
			} catch (Exception e) {
				logger.error("", e);
			}
		}
		transactions.clear();

		if (bucket != null) {
			try {
				bucket.close();
			} catch (Exception e) {
				logger.error("", e);
			}
			bucket = null;
		}

		if (null != timer) {
			try {
				timer.cancel(true);
			} catch (Exception e) {
				logger.error("", e);
			}
			timer = null;
		}

		if (null != commitAgent) {
			try {
				commitAgent.stop();
			} catch (Exception e) {
				logger.error("", e);
			}
			commitAgent = null;
		}
	}

	@Override
	public void reset() {
		var path = Path.of(getDbHome(), "statemachine").toAbsolutePath().toFile();
		LogSequence.deletedDirectoryAndCheck(path, 100);
	}

	@Override
	public void loadSnapshot(String path) throws Exception {
		var backupDir = Paths.get(getDbHome(), "backup").toString();
		var backupFile = new File(backupDir);
		// 与 RocksRaft/Rocks.loadSnapshot 同款: 无条件以已提交快照为恢复源,
		// 不能按mtime跳过解压, 否则会恢复超前的延时快照而双重应用增量日志.
		LogSequence.deletedDirectoryAndCheck(backupFile, 100);
		Zeze.Raft.RocksRaft.Rocks.extractZipToDirectory(path, backupDir);

		restore(backupDir);

		// load exist transaction
		try (var it = bucket.getTrans().iterator()) {
			for (it.seekToFirst(); it.isValid(); it.next()) {
				var batch = new BBatch.Data();
				batch.decode(ByteBuffer.Wrap(it.value()));
				getOrAddTransaction(batch);
			}
		}
	}

	public String checkpoint(SnapshotResult result) throws RocksDBException {
		var checkpointDir = Paths.get(getDbHome(), "checkpoint_" + System.currentTimeMillis()).toString();

		// fast checkpoint, will stop application apply.
		Raft raft = getRaft();
		raft.lock();
		try {
			var lastAppliedLog = raft.getLogSequence().lastAppliedLogTermIndex();
			result.lastIncludedIndex = lastAppliedLog.getIndex();
			result.lastIncludedTerm = lastAppliedLog.getTerm();

			try (var cp = bucket.getDb().newCheckpoint()) {
				cp.createCheckpoint(checkpointDir);
			}
		} finally {
			raft.unlock();
		}
		return checkpointDir;
	}

	public void restore(String backupDir) throws Exception {
		getRaft().lock();
		try {
			close();
			var dbName = Paths.get(getDbHome(), "statemachine").toString();
			RocksDatabase.restore(backupDir, dbName);
			openBucket(); // reopen
		} finally {
			getRaft().unlock();
		}
	}

	public void applySplitPut(BSplitPut.Data puts) {
		try {
			var table = bucket.getData();
			if (puts.isFromTransaction()) {
				// 事务同步流程：分桶期间的delete被编码为Binary.Empty的put（入队见enqueueSplitSync，
				// 投递见Dbh2.driveSplitSync的队列重投）。
				// 空值必须作为墓碑标记原样落盘，不能解码成硬delete：复制流（fromTransaction=false，
				// 钉定T0视图、可能仍含旧值）晚于墓碑到达时，putIfAbsent靠"get非null"被标记挡住；
				// 硬delete会使get返回null而复活旧值（已删记录在新桶以旧值重现）。
				// 读路径（get/walk/walkKey）统一把空值当不存在（客户端replace本就禁止空value，
				// 存储不变量：空value==墓碑标记）。
				for (var e : puts.getPuts().entrySet()) {
					var key = e.getKey();
					var value = e.getValue();

					// replace（空值=墓碑标记）
					table.put(key.bytesUnsafe(), key.getOffset(), key.size(),
							value.bytesUnsafe(), value.getOffset(), value.size());
				}
				return;
			}

			// 数据复制流程
			for (var e : puts.getPuts().entrySet()) {
				var key = e.getKey();
				var value = e.getValue();

				// putIfAbsent（墓碑标记非null，恰好阻止迟到的复制复活已删key）
				if (table.get(key.bytesUnsafe(), key.getOffset(), key.size()) == null) {
					table.put(key.bytesUnsafe(), key.getOffset(), key.size(),
							value.bytesUnsafe(), value.getOffset(), value.size());
				}
			}
		} catch (RocksDBException ex) {
			logger.error("", ex);
			getRaft().fatalKill();
		}
	}
}
