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
import org.jetbrains.annotations.Nullable;

public class ApplyHelper extends FastLock {
	private static final Logger logger = LogManager.getLogger(ApplyHelper.class);

	/**
	 * 键空洞的默认老化时间。空洞可能是"提交早但落库晚"的迟到记录（checkpoint 停滞），
	 * 也可能是永久空洞（进程崩溃时未 flush 的号段、发号服务重启作废的号段——号只保证
	 * 唯一不保证连续）。等待该时长仍未填充才按永久空洞越过。
	 */
	public static final int DEFAULT_HOLE_GRACE_MS = 10 * 60_000;

	private final Application zeze;
	private final tHistory historyTable;
	private final IApplyDatabase dbApplied;
	private final int beforeTimeMs;
	private final int holeGraceMs;
	private final ConcurrentHashMap<Integer, ApplyTable<?, ?>> applyTables = new ConcurrentHashMap<>();
	private Id128 exclusiveStartKey;
	// 当前阻塞游标的空洞（空洞前一个已确认的key）及首次发现时间。
	private Id128 holeAfterKey;
	private long holeSince;

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
	}

	public ConcurrentHashMap<Integer, ApplyTable<?, ?>> getApplyTables() {
		return applyTables;
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
			var endTime = now - beforeTimeMs;
			var result = new HashMap<ApplyTable<?, ?>, Set<Object>>();
			var lastProcessed = new OutObject<Id128>();
			var prevKey = new OutObject<>(exclusiveStartKey); // 本轮walk中当前key的前一个key
			var stopByHole = new OutObject<Boolean>();
			historyTable.walkDatabase(exclusiveStartKey, count, (key, value) -> {
				// 空洞检查：prev与key之间存在未落库的GlobalSerialId（key > prev+1）。
				// 多进程共享发号名时，rrs flush 顺序与 GlobalSerialId 顺序无关，空洞里可能是
				// "提交早但落库晚"的迟到记录——游标一旦越过，它落库后永远在游标之后，静默丢失。
				// 号段分配保证稳态下key连续（每个号必被使用），空洞只在异常时产生，故：
				// 遇空洞即停，等它填充或老化（holeGraceMs）后才越过。
				// 游标为null（从头消费）时表前缀无法判断，跳过检查。
				var prev = prevKey.value;
				if (prev != null && key.compareTo(prev.add(1)) > 0) {
					var aged = holeAfterKey != null && prev.compareTo(holeAfterKey) == 0
							&& now - holeSince > holeGraceMs;
					if (!aged) {
						if (holeAfterKey == null || prev.compareTo(holeAfterKey) != 0) {
							holeAfterKey = prev.clone();
							holeSince = now;
						}
						stopByHole.value = true;
						return false;
					}
					logger.warn("history apply cross key hole after {} ({}ms), skip missing GlobalSerialId(s)",
							prev, now - holeSince);
				}
				if (value.getTimestamp() >= endTime)
					return false;

				// 单条tHistory记录=原子应用单元（c67a10b9残留P2）：记录内全部entry的写入先
				// 计入记录级事务（暂存/挂起，不即时落库），全部entry成功后commit一次性生效；
				// 任一entry异常则rollback丢弃本记录已产生的全部写入后原样重抛。否则前缀entry
				// 已落库而游标停在上条记录，重试整条记录时前缀被二次应用（PList2的OP_ADD按
				// 索引插入，重放产生重复元素），确定性失败下每次重试无上界叠加。
				// 本条记录已触及的表与键（先登记后应用：失败的entry也可能已把未提交脏值写进LRU）。
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
							return originTable.createApplyTable(dbApplied);
						});
						touched.computeIfAbsent(applyTable, __ -> new HashSet<>()).add(r.getKey());
						var affectKeys = result.computeIfAbsent(applyTable, __ -> new HashSet<>());
						affectKeys.add(applyTable.apply(r.getKey(), r.getValue()));
					}
					recordTxn.commit();
					committed = true;
				} catch (Exception ex) {
					// 毒记录可观测性（残留P3）：此异常将穿透walkDatabase中断本批，游标停在上条
					// 记录，下轮apply会整条重放本记录（已原子回滚，重放从干净状态开始、不会叠加
					// 脏写）；若是确定性失败（如Edit目标不存在的分歧NPE）将反复卡死游标，需按此
					// GlobalSerialId人工排查tHistory记录。回滚与LRU失效统一在finally兜底。
					logger.error("history apply poison record: GlobalSerialId={}, deterministic failure "
							+ "suspected; cursor stays before this record and every apply retry replays it "
							+ "whole (atomically rolled back, no partial writes accumulated); manual "
							+ "inspection of this history record is required", key, ex);
					throw ex; // 精确重抛：保持原异常类型传播
				} finally {
					if (!committed) {
						// Exception与Error路径统一收尾（幂等）：回滚存储侧未提交写入，并失效本记录
						// 触过的LRU键（apply先写LRU后写存储，回滚只撤销存储侧），保证重试完整重放。
						recordTxn.close();
						for (var t : touched.entrySet())
							for (var tableKey : t.getValue())
								t.getKey().invalidate(tableKey);
					}
				}
				lastProcessed.value = key;
				prevKey.value = key;
				// FND6-37：批内逐条成功即推进游标——callback异常（Edit分歧检测fail-fast的
				// NPE等）穿透walkDatabase时，游标停在批前（现为停在毒记录前：该记录已整体
				// 回滚，无部分落库），下次apply从断点续传，重放的是干净状态而非损坏状态。
				exclusiveStartKey = key;
				return true;
			});
			// 空洞跟踪：本轮被（新）空洞挡住则保留等待老化或填充；有推进则清掉——
			// 被跟踪的空洞要么已填充，要么已在游标之后。零推进且未被空洞挡住（时间边界/表尾）时保留，
			// 避免已老化的空洞因时间边界反复重置老化时钟。
			if (!Boolean.TRUE.equals(stopByHole.value) && lastProcessed.value != null) {
				holeAfterKey = null;
				holeSince = 0;
			}
			return result;
		} finally {
			unlock();
		}
	}
}
