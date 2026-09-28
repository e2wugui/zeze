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

/**
 * tHistory 历史记录的回放驱动：在自身锁内单线程按序消费记录，经记录级事务应用到回放库，
 * 并随每条记录原子保存回放游标。
 */
public class ApplyHelper extends FastLock {
	private static final Logger logger = LogManager.getLogger(ApplyHelper.class);

	/**
	 * 键空洞的默认老化时间。空洞可能是"提交早但落库晚"的迟到记录（checkpoint 停滞），
	 * 也可能是永久空洞（进程崩溃时未 flush 的号段、发号服务重启作废的号段——号只保证
	 * 唯一不保证连续）。等待该时长仍未填充才按永久空洞越过。
	 */
	public static final int DEFAULT_HOLE_GRACE_MS = 10 * 60_000;

	/**
	 * 未来时间戳告警阈值（hist-04）：tHistory 记录的 timestamp 超前当前墙钟超过该值时 warn 一次。
	 * 正常生产端时钟偏移是毫秒~秒级（NTP）；显著超前意味着坏数据（时钟故障），会使游标在
	 * 时间边界上停摆直到墙钟追回——停着而非错着，但此前完全无日志。取与空洞老化
	 * （{@link #DEFAULT_HOLE_GRACE_MS}）同量级：停摆超过该时长的记录值得运维介入。
	 * 只告警，不改变推进语义。
	 */
	public static final int FUTURE_TIMESTAMP_WARN_AHEAD_MS = 10 * 60_000;

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
	// 已告警的未来时间戳记录（hist-04去重标记）：该记录把游标挡在时间边界上时warn一次，
	// 游标越过它（已应用）之前不重复告警。
	private Id128 futureAfterKey;

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
		// 持久化后端必须恢复游标——否则重启后游标归零从表头整段重放到已有状态上
		// （Edit类日志非幂等，重放污染回放副本）。内存后端loadCursor返回null，天然一致。
		exclusiveStartKey = dbApplied.loadCursor();
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
				var timestamp = value.getTimestamp();
				if (timestamp >= endTime) {
					// 时间边界停住等墙钟追上是边界语义本身（推进语义不变）；但显著超前的
					// timestamp（生产端时钟故障写坏数据）会使游标长时间静默停摆——无日志，
					// 且key存在故空洞检测不触发。每条此类记录warn一次（gsid+超前量），
					// 供运维区分"正常边界等待"与"坏数据停摆"。
					if (timestamp > now + FUTURE_TIMESTAMP_WARN_AHEAD_MS
							&& (futureAfterKey == null || futureAfterKey.compareTo(key) != 0)) {
						logger.warn("history apply stalled by future timestamp: GlobalSerialId={}, "
								+ "timestamp is {}ms ahead of now; cursor stays (by design) until "
								+ "wall clock catches up", key, timestamp - now);
						futureAfterKey = key.clone();
					}
					return false;
				}

				// 单条tHistory记录=原子应用单元：记录内全部entry的写入先
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
					// 游标在记录级事务内与记录数据同原子单元保存——持久化后端
					// 把它与entry写入路由进同一个底层事务，commit成功才一起生效；
					// 失败随记录整体回滚，游标停在上条记录，重试从断点续传。
					dbApplied.saveCursor(key, recordTxn);
					recordTxn.commit();
					committed = true;
				} catch (Throwable ex) {
					// 毒记录可观测性：此异常将穿透walkDatabase中断本批，游标停在上条
					// 记录，下轮apply会整条重放本记录（已原子回滚，重放从干净状态开始、不会叠加
					// 脏写）；若是确定性失败（如Edit目标不存在的分歧NPE）将反复卡死游标，需按此
					// GlobalSerialId人工排查tHistory记录。回滚与LRU失效统一在finally兜底。
					// 捕Throwable（hist-02）：Error型毒记录（decode依赖类缺失的NoClassDefFoundError、
					// 深递归decode的StackOverflowError、OOM）同样确定性卡死游标，poison日志
					// 必须覆盖——正确性本就由finally兜底，这里补齐的是可观测性。
					logger.error("history apply poison record: GlobalSerialId={}, deterministic failure "
							+ "suspected; cursor stays before this record and every apply retry replays it "
							+ "whole (atomically rolled back, no partial writes accumulated); manual "
							+ "inspection of this history record is required", key, ex);
					throw ex; // 精确重抛：保持原异常类型传播（Exception与Error均原样）
				} finally {
					if (!committed) {
						// Exception与Error路径统一收尾（幂等）：回滚存储侧未提交写入，并失效本记录
						// 触过的LRU键（apply先写LRU后写存储，回滚只撤销存储侧），保证重试完整重放。
						recordTxn.close();
						for (var t : touched.entrySet()) {
							for (var tableKey : t.getValue()) {
								try {
									t.getKey().invalidate(tableKey);
								} catch (Throwable e) {
									// finally中抛出的异常会替换正在传播的原始apply异常；单项失效失败仅记日志，继续其余键。
									logger.debug("history apply rollback invalidate failed. tableKey={}", tableKey, e);
								}
							}
						}
					}
				}
				lastProcessed.value = key;
				prevKey.value = key;
				// 批内逐条成功即推进游标——callback异常（Edit分歧检测fail-fast的
				// NPE等）穿透walkDatabase时，游标停在毒记录前（该记录已整体回滚，
				// 无部分落库），下次apply从断点续传，重放的是干净状态而非损坏状态。
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
			// 游标已越过（含已应用）此前告警的未来时间戳记录：解除告警去重标记。
			// 停摆期间游标停在该记录之前，标记保留，跨轮不重复告警。
			if (futureAfterKey != null && exclusiveStartKey != null
					&& exclusiveStartKey.compareTo(futureAfterKey) >= 0)
				futureAfterKey = null;
			return result;
		} finally {
			unlock();
		}
	}
}
