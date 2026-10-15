package Zeze.Transaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import Zeze.Onz.OnzProcedure;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Util.PropertiesHelper;
import Zeze.Util.ZezeCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * see zeze/README.md －＞ 18) 事务提交模式
 * 一个事务内访问的记录的集合。如果事务没有没提交，需要合并集合。
 */
public final class RelativeRecordSet extends ReentrantLock {
	private static final AtomicLong idGenerator = new AtomicLong(1);
	private static final RelativeRecordSet deleted = new RelativeRecordSet();

	private final long id = idGenerator.getAndIncrement();
	// 采用链表，可以O(1)处理Merge，但是由于Merge的时候需要更新Record所属的关联集合，
	// 所以避免不了遍历，那就使用HashSet，遍历吧。
	// 可做的小优化：把Count小的关联集合Merge到大的里面。
	private @Nullable HashSet<Record> recordSet;
	private volatile @Nullable RelativeRecordSet mergeTo; // 不为null表示发生了变化，其中 == Deleted 表示被删除（已经Flush了）。
	private @Nullable Set<OnzProcedure> onzProcedures;
	private volatile @Nullable IHistory.PendingHistory history;

	// RRS锁等待观测（提交/落库双通道）：Profiler通道在生产慢事务(≥3s)时间线内按过程名
	// 归因（对齐Record.enterFairLock的RecordWaitFairLock惯例）；ZezeCounter通道聚合分布，
	// 供bench dump（collectAndReset().formattedLog()）与运行时巡检。句柄一次解析终身复用
	// （对齐Checkpoint.RunOnceObserver惯例）。
	private static final ZezeCounter.LongObserver CommitLockWaitObserver =
			ZezeCounter.instance.getRunTimeObserver("RRS.commitLockWait");
	private static final ZezeCounter.LongObserver FlushLockWaitObserver =
			ZezeCounter.instance.getRunTimeObserver("RRS.flushLockWait");
	// FlushSet整组取锁总等待告警阈值（系统属性，毫秒）：非公平锁下flusher等待是统计性
	// 安全（无理论上界，见构造器注释），本告警是饿死风险的观测兜底——只告警不介入。
	private static final long FlushLockWaitWarnNanos =
			PropertiesHelper.getLong("zeze.rrsFlushLockWaitWarnMs", 1000) * 1_000_000L;

	/** 纯读事务快路（P2）：默认开；zeze.rrs.readFastPath=false 恢复全量 rrs 路径（逃生门）。 */
	private static final boolean ReadFastPath = PropertiesHelper.getBool("zeze.rrs.readFastPath", true);

	RelativeRecordSet() {
		// 非公平构造是取舍而非疏忽（对照Record.fairLock的公平锁、Lockey.alloc同例）：等待者以
		// 并发提交事务为主，公平锁的FIFO交接在高并发提交下实测车队化（同日A/B：B +35%、
		// 脏读 +91%、flush +8~25%，口径见BENCHMARK.md第六轮）。编组语义不变（多重取锁按id
		// 全序防死锁）。代价是flusher等待从公平锁的确定性上界降为统计性安全——AQS排队FIFO
		// 保证入队后排名单调前进，饿死需在队头无限连输到达race（无机制维持、零概率但无理论
		// 上界）；观测兜底=RRS.commitLockWait/flushLockWait聚合 + FlushSet取锁总等待告警
		// （zeze.rrsFlushLockWaitWarnMs）+ RrsWaitLock慢事务时间线。
		super(false);
	}

	public @Nullable IHistory.PendingHistory getHistory() {
		return history;
	}

	void addHistory(@NotNull IHistory.PendingHistory pending) {
		var currentHistory = history;
		if (currentHistory == null)
			history = pending;
		else {
			try {
				currentHistory.mergeFrom(pending);
			} catch (Throwable ex) {
				throw new IllegalStateException("history buffer merge failed", ex);
			}
		}
	}

	@Nullable HashSet<Record> getRecordSet() {
		return recordSet;
	}

	@Nullable RelativeRecordSet getMergeTo() {
		return mergeTo;
	}

	public @Nullable Set<OnzProcedure> getOnzProcedures() {
		return onzProcedures;
	}

	public void addOnzProcedures(@Nullable OnzProcedure onzProcedure) {
		if (onzProcedure != null) {
			if (onzProcedures == null)
				onzProcedures = new HashSet<>();
			onzProcedures.add(onzProcedure);
		}
	}

	private void merge(@NotNull Record r) {
		if (recordSet == null)
			recordSet = new HashSet<>();
		recordSet.add(r);
		if (r.getRelativeRecordSet() != this) { // 自己：不需要更新MergeTo和引用。
			r.getRelativeRecordSet().mergeTo = this;
			r.setRelativeRecordSet(this);
		}
	}

	private void merge(@NotNull RelativeRecordSet rrs) {
		if (rrs == this) // 这个方法仅用于合并其他rrs
			throw new IllegalStateException("Merge Self! " + rrs);

		var pending = rrs.history;
		if (pending != null)
			addHistory(pending);

		if (rrs.recordSet == null)
			return; // 孤立记录，后面单独合并。

		if (recordSet == null)
			recordSet = new HashSet<>();

		for (var r : rrs.recordSet) {
			recordSet.add(r);
			r.setRelativeRecordSet(this);
		}

		if (rrs.onzProcedures != null) {
			if (onzProcedures == null)
				onzProcedures = new HashSet<>();
			onzProcedures.addAll(rrs.onzProcedures);
		}

		rrs.mergeTo = this;
	}

	private void delete() {
		if (recordSet != null) { // 孤立记录不需要更新。
			// Flush完成以后，清除关联集合，
			recordSet.forEach(r -> r.setRelativeRecordSet(new RelativeRecordSet()));
			mergeTo = deleted;
		}
	}

	boolean tryLockWhenIdle() {
		return !hasQueuedThreads() && tryLock();
	}

	// 提交路径取锁：快路tryLock（非公平锁下与lock()首CAS等价，无争零额外开销）；
	// 慢路进Profiler（慢事务时间线按过程名归因）+ ZezeCounter聚合等待分布。
	private static void lockCommit(@NotNull RelativeRecordSet rrs) {
		if (rrs.tryLock())
			return;
		var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
		try (var ignored = Profiler.begin("RrsWaitLock", rrs.id)) {
			rrs.lock();
		} finally {
			if (timeBegin != 0)
				CommitLockWaitObserver.observe(System.nanoTime() - timeBegin);
		}
	}

	// 落库路径取锁：返回本次等待纳秒（供FlushSet整组累计告警）。flusher线程无当前
	// 事务，Profiler.begin安全no-op；聚合走ZezeCounter。统计禁用时返回0——FlushSet的
	// 饿死告警与聚合同源于等待时长，随统计开关一起静默（禁用态典型为压测基线，不收告警噪音）。
	private static long lockFlush(@NotNull RelativeRecordSet rrs) {
		if (rrs.tryLock())
			return 0;
		var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
		try (var ignored = Profiler.begin("RrsWaitLock", rrs.id)) {
			rrs.lock();
		}
		var waited = timeBegin != 0 ? System.nanoTime() - timeBegin : 0;
		FlushLockWaitObserver.observe(waited);
		return waited;
	}

	private static @Nullable IHistory.PreparedCommit prepareHistory(@NotNull Procedure procedure,
			@Nullable Changes changes, @Nullable IHistory history) throws IHistory.CommitRejectedException {
		if (history == null || changes == null)
			return null;
		return Objects.requireNonNull(history.prepareCommit(procedure), "History.prepareCommit returned null");
	}

	private static @Nullable IHistory.PendingHistory collectAndCaptureHistory(@NotNull Transaction trans,
			@Nullable Changes changes, @Nullable IHistory.PreparedCommit prepared) {
		try {
			if (changes != null)
				trans.collectChanges(changes);
			if (prepared == null)
				return null;
			return Objects.requireNonNull(prepared.capture(changes), "History.capture returned null");
		} catch (Throwable ex) {
			// 数据已应用。包成普通异常，不能落入只服务于终局回调断言的 AssertionError 旁路。
			if (prepared != null)
				throw new IllegalStateException("history post-apply capture failed", ex);
			throw ex;
		}
	}

	static void tryUpdateAndCheckpoint(@NotNull Transaction trans, @NotNull Procedure procedure,
									   @NotNull Runnable commit, @Nullable OnzProcedure onzProcedure,
									   @Nullable Changes changes, @Nullable IHistory historyPlugin) throws Exception {
		var checkpoint = procedure.getZeze().getCheckpoint();
		// 入口终检点检查：所有模式（含空事务）停机后统一显式拒绝（Closed语义）。
		if (checkpoint == null)
			throw new Transaction.RejectWhileStopping("commit rejected while stopping: " + procedure.getActionName());
		//noinspection SwitchStatementWithTooFewBranches
		switch (procedure.getZeze().getConfig().getCheckpointMode()) {
		case Immediately: {
			// Immediately模式空事务也经checkpoint.flush落库，提交使用权全程持有。
			if (!checkpoint.tryBeginCommit())
				throw new Transaction.RejectWhileStopping("commit rejected while stopping: " + procedure.getActionName());
			try {
				var prepared = prepareHistory(procedure, changes, historyPlugin);
				commit.run();
				IHistory.PendingHistory pending = null;
				try {
					pending = collectAndCaptureHistory(trans, changes, prepared);
					checkpoint.flush(trans, onzProcedure, pending);
				} catch (Throwable ex) {
					// 修改已应用（commit.run）而收集/落库失败——runOnce对Immediately是no-op，
					// perform的halt兜底刷不到这批"已应用未落库"的数据。趁记录锁未释放（holdLocks在
					// finalCommit返回后才清）用同一入口做一次受控补刷。完整历史已形成时补刷成功可继续；
					// 采集失败尚无完整历史，即使业务补刷成功也须重抛并走 fatal。
					// 提交使用权使同步补刷与停机终检点互斥，不会在修改应用后失去补刷通道。
					try {
						checkpoint.flush(trans, onzProcedure, pending);
					} catch (Throwable ex2) { // logger.fatal
						Checkpoint.logger.fatal("Immediately commit flush fail, salvage flush fail again, accept loss", ex2);
						throw ex;
					}
					if (prepared != null && pending == null)
						throw ex; // 采集失败尚无完整历史，补刷成功也必须进入 fatal。
					Checkpoint.logger.fatal("Immediately commit flush fail, salvage flush success, data saved", ex);
				}
			} finally {
				checkpoint.endCommit();
			}
			// 这种模式下 RelativeRecordSet 都是空的。
			return; // done
		}

		default:
			break;
		}

		// CheckpointMode.Table
		boolean needFlushNow = onzProcedure != null; // 此参数存在即表示Onz.eFlushImmediately。
		var transAccessRecords = new HashSet<Record>();
		boolean allRead = true;
		boolean hasDirtyWrite = false;
		boolean hasUnregisteredDirty = false;
		for (var ar : trans.getAccessedRecords().values()) {
			var record = ar.atomicTupleRecord.record();
			if (ar.dirty || record.getDirty())
				allRead = false;
			if (ar.dirty)
				hasDirtyWrite = true;
			// oldTable倒库在读路径标脏且孤立未注册（写者脏必已编组——_merge_先于setDirty），
			// 其落库注册只能由老路径提交完成，快路必须让位。
			if (record.getDirty() && record.getRelativeRecordSet().getRecordSet() == null)
				hasUnregisteredDirty = true;

			if (record.getTable().getTableConf().getCheckpointWhenCommit()) {
				// 修改了需要马上提交的记录。
				if (ar.dirty) {
					needFlushNow = true;
				}
			}
			// 读写都需要收集。
			transAccessRecords.add(record);
		}

		// 纯读事务快路（P2）：无脏写、无未注册脏、无onz 的提交不触碰 rrs 锁/合并/重注册。
		// 安全性：读者无落库分组需求——读值稳定性由记录锁保证，脏集注册由写者完成，读者
		// 合并/重注册是冗余；单进程与多进程（GCM reduce 先落库后降级、整组刷原子性由
		// 写者编组边保持）均成立。唯一例外是 oldTable 读路径脏（见上），注册通道只在老路径。
		// 论证与实证：BENCHMARK.md L 节（K=80 +17.6%、H 脏读 +110%）。
		if (ReadFastPath && !hasDirtyWrite && !hasUnregisteredDirty && onzProcedure == null) {
			// Changes 仅为脏记录创建；纯读及空事务都没有变更或历史需要采集。
			commit.run(); // 保持 setNotFresh 和提交回调语义。
			return;
		}

		var all = new TreeMap<Long, RelativeRecordSet>();
		for (var record : transAccessRecords) {
			var volatileRrs = record.getRelativeRecordSet();
			all.putIfAbsent(volatileRrs.id, volatileRrs);
		}

		var locked = new ArrayList<RelativeRecordSet>();
		try {
			_lock_(locked, all, transAccessRecords);
			if (!locked.isEmpty()) {
				// 提交使用权延迟到确知非空（已持rrs锁）才获取：空事务else分支不触碰checkpoint
				// （无脏集登记、无flush），免许可证——内核空事务循环热路径零RMW，只剩入口的
				// 终检点null检查。拒绝时经外层finally释放rrs锁；tryBeginCommit的CAS不阻塞，
				// 持锁获取无停等，与停机侧（只等计数、不持rrs锁）无锁序环。
				if (!checkpoint.tryBeginCommit())
					throw new Transaction.RejectWhileStopping("commit rejected while stopping: " + procedure.getActionName());
				try {
					var mergedSet = _merge_(locked, trans, allRead);
					var prepared = prepareHistory(procedure, changes, historyPlugin);
					commit.run(); // 必须在锁获得并且合并完集合以后才提交修改。
					mergedSet.addOnzProcedures(onzProcedure);
					try {
						var pending = collectAndCaptureHistory(trans, changes, prepared);
						if (pending != null)
							mergedSet.addHistory(pending);

						if (needFlushNow) {
							if (mergedSet.recordSet != null) {
								checkpoint.flush(mergedSet);
							} else if (onzProcedure != null) {
								// 孤立mergedSet（只读/全默认值访问不合并）不会被flush(RelativeRecordSet)
								// 下传握手，Onz参与方永不发FlushReady，协调者每笔等满flushTimeout后降级。
								// 在此直接补发，语义对齐Immediately模式的flush(空记录集, Set.of(onz), null)。
								OnzProcedure.sendFlushAndWait(Set.of(onzProcedure));
							}
							mergedSet.delete();
						} else if (mergedSet.recordSet != null) {
							// mergedSet 合并结果是孤立的，不需要Flush。
							// 本次事务没有包含任何需要马上提交的记录，留给 Period 提交。
							checkpoint.relativeRecordSetMap.add(mergedSet);
						}
					} catch (Throwable ex) {
						// 修改已应用（commit.run）而收集/落库失败——直接向上抛则
						// mergedSet不进relativeRecordSetMap，perform的halt兜底checkpointRun只遍历map，
						// 已应用的脏数据（含_merge_并入的存量脏集）无任何落库通道，halt后丢失。
						// 把mergedSet注册进map交给后台checkpoint重试后再重抛：mergedSet锁全程由本线程
						// 持有（finally统一释放），map为ConcurrentHashSet，注册并发安全；flush失败时
						// flushInternal已回滚DB事务、记录保持dirty，正是"保留dirty留待重试"的既有语义。
						if (mergedSet.recordSet != null)
							checkpoint.relativeRecordSetMap.add(mergedSet);
						throw ex;
					}
				} finally {
					checkpoint.endCommit();
				}
			} else {
				// 本次事务没有访问任何数据，也要执行提交，否则 whileCommit 回调会丢失。
				commit.run();
				// 空记录集的Onz参与方同样要完成FlushReady握手（Immediately模式对空记录集
				// 无条件握手，Table模式不可遗漏），否则协调者waitFlushDone计数永不满足，
				// 每笔等满flushTimeout后降级（放行+全参与方checkpoint）。
				if (onzProcedure != null)
					OnzProcedure.sendFlushAndWait(Set.of(onzProcedure));
			}
		} finally {
			locked.forEach(ReentrantLock::unlock);
		}
	}

	private static void verify(@NotNull TreeMap<String, ArrayList<Object>> group,
							   @NotNull TreeMap<String, ArrayList<Object>> result) {
		for (var g : group.entrySet()) {
			for (var value : g.getValue()) {
				var keys = result.get(g.getKey());
				if (keys != null) {
					keys.remove(value);
					if (keys.isEmpty())
						result.remove(g.getKey());
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private static void verify(@NotNull ArrayList<TreeMap<String, ArrayList<Object>>> groupLocked,
							   @NotNull TreeMap<String, ArrayList<Object>> groupTrans,
							   @NotNull RelativeRecordSet result) {
		var groupResult = new TreeMap<String, ArrayList<Object>>();
		if (result.recordSet != null) {
			for (var r : result.recordSet)
				groupResult.computeIfAbsent(r.getTable().getName(), __ -> new ArrayList<>()).add(r.getObjectKey());
		}
		for (var locked : groupLocked)
			verify(locked, groupResult);
		verify(groupTrans, groupResult);
		if (!groupResult.isEmpty()) {
			groupResult.clear(); // reuse this var
			if (result.recordSet != null) {
				for (var r : result.recordSet)
					groupResult.computeIfAbsent(r.getTable().getName(), __ -> new ArrayList<>()).add(r.getObjectKey());
			}
			Checkpoint.logger.info("locked.size={} trans.size={}\nlocked:{}\ntrans:{}\nresult:{}",
				groupLocked.size(), groupTrans.size(), groupLocked, groupTrans, groupResult);
		}
	}

	@SuppressWarnings("unused")
	private static void build(@NotNull Transaction trans, @NotNull TreeMap<String, ArrayList<Object>> groupTrans) {
		for (var ar : trans.getAccessedRecords().values()) {
			groupTrans.computeIfAbsent(ar.atomicTupleRecord.record().getTable().getName(), __ -> new ArrayList<>())
				.add(ar.atomicTupleRecord.record().getObjectKey());
		}
	}

	@SuppressWarnings("unused")
	private static void build(@NotNull ArrayList<RelativeRecordSet> locked,
							  @NotNull ArrayList<TreeMap<String, ArrayList<Object>>> groupLocked) {
		for (var rrs : locked) {
			var group = new TreeMap<String, ArrayList<Object>>();
			if (rrs.recordSet != null) {
				for (var r : rrs.recordSet) {
					group.computeIfAbsent(r.getTable().getName(), __ -> new ArrayList<>()).add(r.getObjectKey());
				}
			}
			groupLocked.add(group);
		}
	}

	private static @NotNull RelativeRecordSet _merge_(@NotNull ArrayList<RelativeRecordSet> locked,
													  @NotNull Transaction trans, boolean allRead) {
		// find largest
		var largest = locked.getFirst();
		for (int index = 1; index < locked.size(); ++index) {
			var r = locked.get(index);
			var cur = largest.recordSet == null ? 0 : largest.recordSet.size();
			if (r.recordSet != null && r.recordSet.size() > cur) {
				largest = r;
			}
		}

		// merge all other set to largest
		for (var r : locked) {
			if (r != largest) // skip self
				largest.merge(r);
		}

		// 所有的记录都是读，并且所有的记录都是孤立的，此时不需要关联起来。
		if (largest.recordSet != null || !allRead) {
			// merge 孤立记录。
			for (var ar : trans.getAccessedRecords().values()) {
				var record = ar.atomicTupleRecord.record();
				var rrs = record.getRelativeRecordSet();
				if (rrs.recordSet == null || rrs == largest /* is self. ugly */)
					largest.merge(record); // 合并孤立记录。这里包含largest是孤立记录的情况。
			}
		}
		return largest;
	}

	private static void _lock_(@NotNull ArrayList<RelativeRecordSet> locked,
							   @NotNull TreeMap<Long, RelativeRecordSet> all,
							   @NotNull HashSet<Record> transAccessRecords) {
		while (true) {
			var GotoLabelLockRelativeRecordSets = false;
			int index = 0;
			int n = locked.size();
			final var itRrs = all.values().iterator();
			var rrs = itRrs.hasNext() ? itRrs.next() : null;
			while (rrs != null) {
				if (index >= n) {
					if (_lock_and_check_(locked, all, rrs, transAccessRecords)) {
						rrs = itRrs.hasNext() ? itRrs.next() : null;
						continue;
					}
					GotoLabelLockRelativeRecordSets = true;
					break;
				}
				var curSet = locked.get(index);
				int c = Long.compare(curSet.id, rrs.id);
				if (c == 0) {
					index++;
					rrs = itRrs.hasNext() ? itRrs.next() : null;
					continue;
				}
				if (c < 0) {
					// 释放掉不需要的锁（已经被Delete了，Has Flush）。
					int unlockEndIndex = index;
					while (unlockEndIndex < n && locked.get(unlockEndIndex).id < rrs.id)
						locked.get(unlockEndIndex++).unlock();
					locked.subList(index, unlockEndIndex).clear();
					n = locked.size();
					// 重新从当前 rrs 继续锁。
					continue;
				}
				// RelativeRecordSets发生了变化，并且出现排在当前已经锁住对象前面的集合。
				// 从当前位置释放锁，再次尝试。
				for (int i = index; i < n; i++)
					locked.get(i).unlock();
				locked.subList(index, n).clear();
				n = locked.size();
				// 重新从当前 rrs 继续锁。
			}
			if (!GotoLabelLockRelativeRecordSets)
				break; // success
		}
	}

	private static boolean _lock_and_check_(@NotNull ArrayList<RelativeRecordSet> locked,
											@NotNull TreeMap<Long, RelativeRecordSet> all,
											@NotNull RelativeRecordSet rrs,
											@NotNull HashSet<Record> transAccessRecords) {
		lockCommit(rrs);
		var mergeTo = rrs.mergeTo;
		if (mergeTo != null) {
			rrs.unlock();
			all.remove(rrs.id); // remove merged or deleted rrs
			if (mergeTo == deleted) {
				// flush 后进入这个状态。此时表示旧的关联集合的checkpoint点已经完成。
				// 但仍然需要重新获得当前事务中访问的记录的rrs。
				// 进入 deleted 以后，rrs.recordSet 不再发生变化。只读，锁外使用。
				for (var r : transAccessRecords) {
					//noinspection DataFlowIssue
					if (rrs.recordSet.contains(r)) {
						var volatileTmp = r.getRelativeRecordSet();
						all.putIfAbsent(volatileTmp.id, volatileTmp);
					}
				}
				return false;
			}
			all.putIfAbsent(mergeTo.id, mergeTo);
			return false;
		}
		locked.add(rrs);
		return true;
	}

	static class FlushSet {
		private final @NotNull Checkpoint checkpoint;
		private final TreeMap<Long, RelativeRecordSet> sortedRrs = new TreeMap<>();
		private int sumHint;

		public FlushSet(@NotNull Checkpoint cp) {
			checkpoint = cp;
		}

		// package-private：测试需手工组装分组复现失败轮重分组形态。
		boolean add(@NotNull RelativeRecordSet rrs) {
			if (sortedRrs.putIfAbsent(rrs.id, rrs) != null)
				throw new IllegalStateException("duplicate rrs");
			if (null != rrs.recordSet) {
				// 这里没有加锁，得到的recordSet.size可能会变，但这里仅作为一个控制一次提交的量，不加锁是可以的。
				sumHint += rrs.recordSet.size();
			}
			var flushLimit = checkpoint.getZeze().getConfig().getCheckpointModeTableFlushSetCount();
			return sortedRrs.size() >= flushLimit || sumHint >= 10000;
		}

		private int size() {
			return sortedRrs.size();
		}

		void flush() {
			var timeBegin = System.nanoTime();
			var n = sortedRrs.size();
			var locks = new ArrayList<RelativeRecordSet>(n);
			try {
				var nr = 0;
				long lockWaited = 0;
				for (var rrs : sortedRrs.values()) {
					lockWaited += lockFlush(rrs);
					locks.add(rrs);
					//noinspection DataFlowIssue
					nr += rrs.recordSet.size();
				}
				if (lockWaited > FlushLockWaitWarnNanos)
					Checkpoint.logger.warn(
							"FlushSet acquire {} rrs locks waited {}ms: flusher排队超阈值(zeze.rrsFlushLockWaitWarnMs)，"
									+ "查RRS.flushLockWait聚合分布与RrsWaitLock慢事务时间线",
							locks.size(), lockWaited / 1_000_000);
				var rs = new ArrayList<Record>(nr);
				var onzProcedures = new HashSet<OnzProcedure>();
				IHistory.PendingHistory history = null;
				for (var rrs : sortedRrs.values()) {
					if (rrs.mergeTo != null)
						continue; // merged or deleted
					rs.addAll(rrs.recordSet);
					// 首个成员创建快照，后续只向快照合并，不写成员 rrs 的自有历史容器：
					// 失败轮回滚后成员容器原样保留，重试轮无论怎么重分组，每个快照的写入/核销
					// 集合恒等于本轮成员集合——不会带出幽灵 tHistory 行、不会跨组核销别人的 gid。
					var member = rrs.getHistory();
					if (member != null) {
						if (history == null)
							history = member.snapshot();
						else
							history.mergeFrom(member);
					}
					// 恢复onz聚集（判空后addAll，直接addAll(null)会NPE）：
					// 正常流程带onz的rrs恒为flush-now（needFlushNow=onzProcedure!=null）不进
					// relativeRecordSetMap，此处恒为空集；唯一的真实到达路径是失败
					// 重注册——needFlushNow的flush失败后mergedSet（可携带onzProcedures且握手
					// 可能未完成）被重新注册进map，此后Merge模式经FlushSet重试落库。不聚集则
					// 重试永不补发FlushReady，协调者每笔等满flushTimeout后降级，两段式提交被
					// 静默绕过（与补孤立/空记录集握手的意图矛盾）。锁域安全：本循环持有
					// 全部成员rrs锁，addOnzProcedures的写与merge()的转移互斥；被
					// mergeTo跳过的成员其onz已转移至存活者，不丢不重（HashSet去重）。
					var onz = rrs.getOnzProcedures();
					if (onz != null)
						onzProcedures.addAll(onz);
				}

				checkpoint.flush(rs, onzProcedures, history);

				for (var rrs : sortedRrs.values()) {
					if (rrs.mergeTo == null)
						rrs.delete(); // normal rrs: not merged and not deleted.
					checkpoint.relativeRecordSetMap.remove(rrs);
				}
				sortedRrs.clear();
				sumHint = 0;

				// verify
				var verifyAction = DatabaseRocksDb.verifyAction;
				if (verifyAction != null)
					verifyAction.run();
			} finally {
				locks.forEach(RelativeRecordSet::unlock);
				Checkpoint.logger.trace("flush: {} rrs, {} ns", n, System.nanoTime() - timeBegin);
			}
		}
	}

	static void flush(@NotNull Checkpoint checkpoint, @NotNull RelativeRecordSet rrs) {

		// 编码统一由 Checkpoint.flush 锁内 encode0 承担。不得锁外 encodeN 预编码：
		// 它与并发轮的 writeOnly/onPersisted、并发事务 merge 存在桶级交错：搬运者
		// 拿锁后发现 rrs 已 deleted/merged 而跳过 flush，滞留条目永不落库。
		lockFlush(rrs);
		try {
			if (rrs.mergeTo == null) {
				checkpoint.flush(rrs);
				rrs.delete();
			}
			checkpoint.relativeRecordSetMap.remove(rrs);
			if (DatabaseRocksDb.verifyAction != null)
				DatabaseRocksDb.verifyAction.run();
		} finally {
			rrs.unlock();
		}
	}

	static void flushWhenCheckpoint(@NotNull Checkpoint checkpoint) {
		// 根据选项执行不同的flush模式。
		// 单元隔离：任一rrs（Merge模式为FlushSet）flush失败（编码异常、关系库约束冲突、db连接抖动等）
		// 只记error，失败单元保留在relativeRecordSetMap中（flush成功才会remove），留待下轮checkpoint重试；
		// 不能让单个坏记录中断整轮，饿死迭代顺序在其后的脏集合（积压不可被cleanNow清理，最终OOM）。
		switch (checkpoint.zeze.getConfig().getCheckpointFlushMode()) {
		case SingleThread:
			for (var rrs : checkpoint.relativeRecordSetMap) {
				try {
					flush(checkpoint, rrs);
				} catch (Throwable ex) { // logger.error
					Checkpoint.logger.error("flushWhenCheckpoint(SingleThread) flush fail, keep for next checkpoint", ex);
				}
			}
			break;

		case MultiThread:
			// 专用dbIo池：flush是阻塞DB IO，commonPool面向CPU任务，阻塞互抢可整体停摆
			// 并拖垮进程内其他parallelStream使用者。
			Zeze.Util.Task.runInDbIoPool(() -> checkpoint.relativeRecordSetMap.keySet().parallelStream().forEach(rrs -> {
				try {
					flush(checkpoint, rrs);
				} catch (Throwable ex) { // logger.error
					// lambda内必须捕获：逃逸会取消parallelStream尚未启动的任务并传播出去。
					Checkpoint.logger.error("flushWhenCheckpoint(MultiThread) flush fail, keep for next checkpoint", ex);
				}
			}));
			break;

		case SingleThreadMerge: {
			var flushSet = new FlushSet(checkpoint);
			for (var rrs : checkpoint.relativeRecordSetMap) {
				if (flushSet.add(rrs)) {
					try {
						flushSet.flush();
					} catch (Throwable ex) { // logger.error
						Checkpoint.logger.error("flushWhenCheckpoint(SingleThreadMerge) FlushSet flush fail, keep for next checkpoint", ex);
						// 失败成员仍留在relativeRecordSetMap中（flush成功才会remove），下轮重试。
						// 丢弃毒化的积累换新FlushSet：sortedRrs未清空时集合保持"满"，后续每个rrs都会
						// 立即触发整组重试并继续失败，等于本轮余下集合全部饿死。
						flushSet = new FlushSet(checkpoint);
					}
				}
			}
			flushTail(flushSet);
		}
		break;

		case MultiThreadMerge: {
			var flushSetMap = new ConcurrentHashMap<Thread, FlushSet>();
			// 同MultiThread：专用dbIo池承载阻塞flush（FlushSet按worker线程归属，池worker同样成立）。
			Zeze.Util.Task.runInDbIoPool(() -> checkpoint.relativeRecordSetMap.keySet().parallelStream().forEach(rrs -> {
				var fs = parallelFlushSet(checkpoint, flushSetMap);
				if (fs.add(rrs)) {
					try {
						fs.flush();
					} catch (Throwable ex) { // logger.error
						Checkpoint.logger.error("flushWhenCheckpoint(MultiThreadMerge) FlushSet flush fail, keep for next checkpoint", ex);
						// 同SingleThreadMerge：丢弃毒化积累，本线程后续rrs换新FlushSet（computeIfAbsent不会替换，用put）。
						flushSetMap.put(Thread.currentThread(), new FlushSet(checkpoint));
					}
				}
			}));
			for (var fs : flushSetMap.values())
				flushTail(fs);
		}
		break;
		}
	}

	private static void flushTail(@NotNull FlushSet flushSet) {
		if (flushSet.size() <= 0)
			return;
		try {
			flushSet.flush();
		} catch (Throwable ex) { // logger.error
			Checkpoint.logger.error("flushWhenCheckpoint flushTail fail, keep for next checkpoint", ex);
		}
	}

	private static @NotNull FlushSet parallelFlushSet(@NotNull Checkpoint checkpoint,
													  @NotNull ConcurrentHashMap<Thread, FlushSet> map) {
		return map.computeIfAbsent(Thread.currentThread(), __ -> new FlushSet(checkpoint));
	}

	static void flushWhenReduce(@NotNull Record r, @NotNull Checkpoint checkpoint) {
		var rrs = r.getRelativeRecordSet();
		while (rrs != null) {
			r.enterFairLock(); // 用来保护State的查看。
			try {
				if (r.getState() == GlobalCacheManagerConst.StateRemoved)
					return;
			} finally {
				r.exitFairLock();
			}
			rrs = flushWhenReduce(rrs, checkpoint);
		}
	}

	private static @Nullable RelativeRecordSet flushWhenReduce(@NotNull RelativeRecordSet rrs,
															   @NotNull Checkpoint checkpoint) {
		lockFlush(rrs);
		try {
			var mergeTo = rrs.mergeTo;
			if (mergeTo == null) {
				if (rrs.recordSet != null) { // 孤立记录不用保存，肯定没有修改。
					checkpoint.flush(rrs);
					rrs.delete();
				}
				return null;
			}

			// 这个方法是在 Reduce 获得记录锁，并降级（设置状态）以后才调用。
			// 已经不会有后续的修改（但可能有读取并且被合并然后又被Flush），
			// 或者被 Checkpoint Flush。
			if (mergeTo == RelativeRecordSet.deleted)
				return null; // has flush

			return mergeTo; // 返回这个能更快得到新集合的引用。
		} finally {
			rrs.unlock();
		}
	}

	@Override
	public @NotNull String toString() {
		lock();
		try {
			var mergeTo = this.mergeTo;
			if (mergeTo != null)
				return "[MergeTo-" + mergeTo.id + "]";
			if (recordSet == null)
				return id + "-[Isolated]";
			return id + "-" + recordSet;
		} finally {
			unlock();
		}
	}

	public static @NotNull String relativeRecordSetMapToString(@NotNull Checkpoint checkpoint) {
		return checkpoint.relativeRecordSetMap.toString();
	}
}
