package Zeze.History;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Application;
import Zeze.Builtin.HistoryModule.BTableKey;
import Zeze.Builtin.HistoryModule.tHistory;
import Zeze.Transaction.TableKey;
import Zeze.Util.FastLock;
import Zeze.Util.Id128;
import Zeze.Util.OutObject;
import org.jspecify.annotations.Nullable;

/**
 * tHistory 历史记录的回放驱动：在自身锁内单线程按序消费记录，经记录级事务应用到回放库，
 * 并随每条记录原子保存回放游标。
 */
public class ApplyHelper extends FastLock {
	private static final Logger logger = LogManager.getLogger(ApplyHelper.class);

	/** 键空洞的默认老化时间：超过该时长仍未填充时按永久空洞越过。 */
	public static final int DEFAULT_HOLE_GRACE_MS = 10 * 60_000;

	/** 未来时间戳告警阈值：记录时间戳超前墙钟超过该值时每条告警一次，不改变推进语义。 */
	public static final int FUTURE_TIMESTAMP_WARN_AHEAD_MS = 10 * 60_000;

	private final Application zeze;
	private final tHistory historyTable;
	private final IApplyDatabase dbApplied;
	private final int beforeTimeMs;
	private final int holeGraceMs;
	// 仅在 ApplyHelper 锁内单线程读写，不对外暴露（内部 ApplyTable 的 LRU 无并发保护）。
	private final ConcurrentHashMap<Integer, ApplyTable<?, ?>> applyTables = new ConcurrentHashMap<>();
	private @Nullable Id128 exclusiveStartKey;
	// 当前阻塞游标的空洞位置及单调时钟起点（避免 NTP 墙钟回拨导致空洞永不老化）。
	private @Nullable Id128 holeAfterKey;
	private long holeSinceNanos;
	// 已告警的未来时间戳记录去重标记，游标越过前不重复告警。
	private @Nullable Id128 futureAfterKey;

	public ApplyHelper(Application zeze, tHistory historyTable,
					   IApplyDatabase dbApplied, int beforeTimeMs) {
		this(zeze, historyTable, dbApplied, beforeTimeMs, DEFAULT_HOLE_GRACE_MS);
	}

	public ApplyHelper(Application zeze, tHistory historyTable,
					   IApplyDatabase dbApplied, int beforeTimeMs, int holeGraceMs) {
		this.zeze = zeze;
		this.historyTable = historyTable;
		this.dbApplied = dbApplied;
		this.beforeTimeMs = beforeTimeMs;
		this.holeGraceMs = holeGraceMs;
		// 恢复持久化游标，避免重启后重放非幂等 Edit 日志。
		exclusiveStartKey = dbApplied.loadCursor();
	}

	public @Nullable Id128 getExclusiveStartKey() {
		lock();
		try {
			return exclusiveStartKey != null ? exclusiveStartKey.clone() : null;
		} finally {
			unlock();
		}
	}

	/**
	 * 应用一批历史数据。
	 * @param count 指定这次应用的历史记录数量。
	 * @return 这次应用受影响的表。
	 * @throws Exception exception。
	 */
	public Map<ApplyTable<?, ?>, Set<Object>> apply(int count) throws Exception {
		lock();
		try {
			var now = System.currentTimeMillis();
			var nowNanos = System.nanoTime(); // 空洞老化专用（单调）：now墙钟仍用于endTime与未来时间戳告警
			var endTime = now - beforeTimeMs;
			var result = new HashMap<ApplyTable<?, ?>, Set<Object>>();
			var lastProcessed = new OutObject<Id128>();
			var prevKey = new OutObject<>(exclusiveStartKey); // 本轮walk中当前key的前一个key
			var stopByHole = new OutObject<Boolean>();
			historyTable.walkDatabase(exclusiveStartKey, count, (key, value) -> {
				// 空洞检查：多进程 flush 顺序可能乱序，遇空洞先等待填充或老化（holeGraceMs）后再越过。
				var prev = prevKey.value;
				if (prev != null && key.compareTo(prev.add(1)) > 0) {
					var aged = holeAfterKey != null && prev.compareTo(holeAfterKey) == 0
							&& nowNanos - holeSinceNanos > holeGraceMs * 1_000_000L;
					if (!aged) {
						if (holeAfterKey == null || prev.compareTo(holeAfterKey) != 0) {
							holeAfterKey = prev.clone();
							holeSinceNanos = nowNanos;
						}
						stopByHole.value = true;
						return false;
					}
					logger.warn("history apply cross key hole after {} ({}ms), skip missing GlobalSerialId(s)",
							prev, (nowNanos - holeSinceNanos) / 1_000_000);
				}
				var timestamp = value.getTimestamp();
				if (timestamp >= endTime) {
					// 记录时间戳显著超前墙钟时每条告警一次，便于区分正常边界等待与坏数据停摆。
					if (timestamp > now + FUTURE_TIMESTAMP_WARN_AHEAD_MS
							&& (futureAfterKey == null || futureAfterKey.compareTo(key) != 0)) {
						logger.warn("history apply stalled by future timestamp: GlobalSerialId={}, "
								+ "timestamp is {}ms ahead of now; cursor stays (by design) until "
								+ "wall clock catches up", key, timestamp - now);
						futureAfterKey = key.clone();
					}
					return false;
				}

				// 单条记录作为原子单元在记录级事务内应用；先登记触及的表与键，失败时统一回滚并失效 LRU。
				var touched = new HashMap<ApplyTable<?, ?>, HashSet<BTableKey>>();
				var recordTxn = dbApplied.beginRecordTxn();
				var committed = false;
				try {
					for (var r : value.getChanges().entrySet()) {
						var applyTable = applyTables.computeIfAbsent(r.getKey().getTableId(), __ -> {
							var tableName = TableKey.tables.get(r.getKey().getTableId());
							if (null == tableName)
								throw new RuntimeException("table id not found. id=" + r.getKey().getTableId());
							var originTable = zeze.getTable(tableName);
							if (null == originTable)
								throw new RuntimeException("table not found. name=" + tableName);
							return ApplyTable.create(originTable, dbApplied);
						});
						touched.computeIfAbsent(applyTable, __ -> new HashSet<>()).add(r.getKey());
						var affectKeys = result.computeIfAbsent(applyTable, __ -> new HashSet<>());
						affectKeys.add(applyTable.apply(r.getKey(), r.getValue()));
					}
					// 游标与记录数据在同一记录级事务中原子提交。
					dbApplied.saveCursor(key, recordTxn);
					recordTxn.commit();
					committed = true;
				} catch (Throwable ex) {
					// 捕获 Throwable 以记录毒记录（含 Error）的 GlobalSerialId，随后原样重抛并由 finally 回滚。
					logger.error("history apply poison record: GlobalSerialId={}, deterministic failure "
							+ "suspected; cursor stays before this record and every apply retry replays it "
							+ "whole (atomically rolled back, no partial writes accumulated); manual "
							+ "inspection of this history record is required", key, ex);
					throw ex;
				} finally {
					if (!committed) {
						// 回滚存储侧并失效本记录触及的 LRU 键，避免重试命中脏缓存。
						recordTxn.close();
						for (var t : touched.entrySet()) {
							for (var tableKey : t.getValue()) {
								try {
									t.getKey().invalidate(tableKey);
								} catch (Throwable e) {
									// 不抛出以免覆盖原始 apply 异常。
									logger.debug("history apply rollback invalidate failed. tableKey={}", tableKey, e);
								}
							}
						}
					}
				}
				lastProcessed.value = key;
				prevKey.value = key;
				// 逐条提交后即推进游标，确保后续记录失败时从断点续传。
				exclusiveStartKey = key;
				return true;
			});
			// 仅本轮有推进且未被空洞阻断时清除空洞跟踪，避免在时间边界反复重置老化时钟。
			if (!Boolean.TRUE.equals(stopByHole.value) && lastProcessed.value != null) {
				holeAfterKey = null;
				holeSinceNanos = 0;
			}
			// 游标越过已告警的未来时间戳记录后清除去重标记。
			if (futureAfterKey != null && exclusiveStartKey != null
					&& exclusiveStartKey.compareTo(futureAfterKey) >= 0)
				futureAfterKey = null;
			return result;
		} finally {
			unlock();
		}
	}
}
