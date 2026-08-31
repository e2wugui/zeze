package Zeze.History;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Builtin.HistoryModule.tHistory;
import Zeze.Transaction.TableKey;
import Zeze.Util.FastLock;
import Zeze.Util.Id128;
import Zeze.Util.OutObject;

public class ApplyHelper extends FastLock {
	private final Application zeze;
	private final tHistory historyTable;
	private final IApplyDatabase dbApplied;
	private final int beforeTimeMs;
	private final ConcurrentHashMap<Integer, ApplyTable<?, ?>> applyTables = new ConcurrentHashMap<>();
	private Id128 exclusiveStartKey;

	public ApplyHelper(Application zeze, tHistory historyTable,
					   IApplyDatabase dbApplied, int beforeTimeMs) {
		this.zeze = zeze;
		this.historyTable = historyTable;
		this.dbApplied = dbApplied;
		this.beforeTimeMs = beforeTimeMs;
	}

	public ConcurrentHashMap<Integer, ApplyTable<?, ?>> getApplyTables() {
		return applyTables;
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
			var endTime = System.currentTimeMillis() - beforeTimeMs;
			var result = new HashMap<ApplyTable<?, ?>, Set<Object>>();
			var lastProcessed = new OutObject<Id128>();
			historyTable.walkDatabase(exclusiveStartKey, count, (key, value) -> {
				if (value.getTimestamp() >= endTime)
					return false;

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
					var affectKeys = result.computeIfAbsent(applyTable, __ -> new HashSet<>());
					affectKeys.add(applyTable.apply(r.getKey(), r.getValue()));
				}
				lastProcessed.value = key;
				return true;
			});
			// 游标只推进到最后一条"已成功处理"的记录，不使用walkDatabase的返回值：
			// 各实现都在callback之前记录lastKey，返回false停止时返回的是未处理记录的key，直接当游标会把它永久跳过；
			// 且走到表尾时返回值不一致（RocksDb为null，Memory/Jdbc为最后交付的key）。本轮零处理时保留原游标，
			// 停在时间边界的记录下次apply重新交付，endTime推进后即被处理。
			if (lastProcessed.value != null)
				exclusiveStartKey = lastProcessed.value;
			return result;
		} finally {
			unlock();
		}
	}
}
