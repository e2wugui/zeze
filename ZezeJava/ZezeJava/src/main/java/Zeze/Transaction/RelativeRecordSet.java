package Zeze.Transaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.History.History;
import Zeze.Onz.OnzProcedure;
import Zeze.Services.GlobalCacheManagerConst;
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
	private volatile @Nullable History history;

	RelativeRecordSet() {
		super(true);
	}

	public @Nullable History getHistory() {
		return history;
	}

	void addLogChanges(@NotNull BLogChanges.Data logChanges) {
		var h = history;
		if (h == null)
			history = new History(logChanges);
		else
			h.addLogChanges(logChanges); // 这是锁内的，可以不考虑这个警告。
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

		//noinspection NonAtomicOperationOnVolatileField
		history = History.merge(history, rrs.history);

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

	/**
	 * 历史变更收集的两阶段契约：取号（含段耗尽的续段分配）在日志应用（commit.run）之前，
	 * 走查与编码在应用之后。取号是对 Id128 发号服务的阻塞等待，若发生在应用之后，失败时
	 * 数据已生效而 tHistory 永久缺失（gid 未消费，键空间连空洞都没有，回放端无从感知），
	 * Immediately 模式补刷后更是吞异常报假成功——前移到应用前，失败=事务未应用即干净失败
	 * （{@code Transaction.RejectHistoryAllocFailed}，RejectWhileStopping 同款路径），历史
	 * 与数据同生共死。取号成功立即入对账账本（PendingGidLedger.register）：此后任何失败
	 * （应用/收集/落库）都留下"已发号未核销"痕迹。走查/编码必须后置：Record.collect 对
	 * Put/Remove 的分类读 ar.committedPutLog，它由应用期（PutLog.commit）填充——前置走查
	 * 会把 put 误分类为 edit（监听者拿到 null LogBean、History 编码错误）。beforeApply 的
	 * 门控（isHistory && anyDirty）与走查后的 History 分支门（isHistory && records非空）
	 * 精确等价：isHistory 下 collectRecord 无早退、每个 dirty 记录必登记；!isHistory 时
	 * records 仍会因监听者登记而非空，但 History 分支被 isHistory 挡住，不参与等价。
	 */
	interface HistoryChangesCollector {
		/** commit.run() 之前调用：isHistory 且有 dirty 记录时阻塞取号（含续段分配）并
		 * 入对账账本，失败干净拒绝；不做任何走查。 */
		void beforeApply() throws Exception;

		/** commit.run() 之后调用：走查日志建 Changes（供监听者与 History），用已解析的
		 * gid 编码日志变更；未解析 gid（历史关闭或无记录）时返回 null。 */
		@Nullable BLogChanges.Data afterApply();
	}

	static void tryUpdateAndCheckpoint(@NotNull Transaction trans, @NotNull Procedure procedure,
									   @NotNull Runnable commit, @Nullable OnzProcedure onzProcedure,
									   @NotNull HistoryChangesCollector collectChanges) throws Exception {
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
				collectChanges.beforeApply(); // 取号在应用前（HistoryChangesCollector）：失败时数据未应用
				commit.run();
				BLogChanges.Data logChanges = null;
				try {
					logChanges = collectChanges.afterApply();
					checkpoint.flush(trans, onzProcedure, logChanges != null ? new History(logChanges) : null);
				} catch (Throwable ex) {
					// 修改已应用（commit.run）而收集/落库失败——runOnce对Immediately是no-op，
					// perform的halt兜底刷不到这批"已应用未落库"的数据。趁记录锁未释放（holdLocks在
					// finalCommit返回后才清）用同一入口做一次受控补刷：成功则数据已落库，fatal记原异常
					// 后吞掉继续；再失败则重抛原异常走halt（DB硬故障，明确接受丢失）。
					// 提交使用权使同步补刷与停机终检点互斥，不会在修改应用后失去补刷通道。
					try {
						checkpoint.flush(trans, onzProcedure, logChanges != null ? new History(logChanges) : null);
					} catch (Throwable ex2) { // logger.fatal
						Checkpoint.logger.fatal("Immediately commit flush fail, salvage flush fail again, accept loss", ex2);
						throw ex;
					}
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
		var all = new TreeMap<Long, RelativeRecordSet>();
		var transAccessRecords = new HashSet<Record>();
		boolean allRead = true;
		for (var ar : trans.getAccessedRecords().values()) {
			var record = ar.atomicTupleRecord.record();
			if (ar.dirty || record.getDirty())
				allRead = false;

			if (record.getTable().getTableConf().getCheckpointWhenCommit()) {
				// 修改了需要马上提交的记录。
				if (ar.dirty) {
					needFlushNow = true;
				}
			}
			// 读写都需要收集。
			transAccessRecords.add(record);
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
					collectChanges.beforeApply(); // 取号在应用前（HistoryChangesCollector）：失败时数据未应用
					commit.run(); // 必须在锁获得并且合并完集合以后才提交修改。
					mergedSet.addOnzProcedures(onzProcedure);
					try {
						var logChanges = collectChanges.afterApply();
						if (logChanges != null)
							mergedSet.addLogChanges(logChanges); // History存在并且开启，则加入rrs。

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
		rrs.lock();
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
				for (var rrs : sortedRrs.values()) {
					rrs.lock();
					locks.add(rrs);
					//noinspection DataFlowIssue
					nr += rrs.recordSet.size();
				}
				var rs = new ArrayList<Record>(nr);
				var onzProcedures = new HashSet<OnzProcedure>();
				History history = null;
				for (var rrs : sortedRrs.values()) {
					if (rrs.mergeTo != null)
						continue; // merged or deleted
					rs.addAll(rrs.recordSet);
					// combine（非 merge）只读组装快照，不写成员 rrs 的自有 History 容器：
					// 失败轮回滚后成员容器原样保留，重试轮无论怎么重分组，每个快照的写入/核销
					// 集合恒等于本轮成员集合——不会带出幽灵 tHistory 行、不会跨组核销别人的 gid。
					history = History.combine(history, rrs.getHistory());
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
		// 它与并发轮的 writeOnly/commitDone、并发事务 merge 存在桶级交错：搬运者
		// 拿锁后发现 rrs 已 deleted/merged 而跳过 flush，滞留条目永不落库。
		rrs.lock();
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
			checkpoint.relativeRecordSetMap.keySet().parallelStream().forEach(rrs -> {
				try {
					flush(checkpoint, rrs);
				} catch (Throwable ex) { // logger.error
					// lambda内必须捕获：逃逸会取消parallelStream尚未启动的任务并传播出去。
					Checkpoint.logger.error("flushWhenCheckpoint(MultiThread) flush fail, keep for next checkpoint", ex);
				}
			});
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
			checkpoint.relativeRecordSetMap.keySet().parallelStream().forEach(rrs -> {
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
			});
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
		rrs.lock();
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
