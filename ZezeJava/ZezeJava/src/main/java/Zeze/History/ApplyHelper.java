package Zeze.History;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Builtin.HistoryModule.ZezeHistoryTable_m_a_g_i_c;
import Zeze.Transaction.TableKey;

public class ApplyHelper {
	private final Application zeze;
	private final ZezeHistoryTable_m_a_g_i_c historyTable;
	private final IApplyDatabase dbApplied;
	private final int beforeTimeMs;
	private final ConcurrentHashMap<Integer, ApplyTable<?, ?>> applyTables = new ConcurrentHashMap<>();
	private Long exclusiveStartKey;

	public ApplyHelper(Application zeze, ZezeHistoryTable_m_a_g_i_c historyTable,
					   IApplyDatabase dbApplied, int beforeTimeMs) {
		this.zeze = zeze;
		this.historyTable = historyTable;
		this.dbApplied = dbApplied;
		this.beforeTimeMs = beforeTimeMs;
	}

	/**
	 * 应用一批历史数据。
	 * @param count 指定这次应用的历史记录数量。
	 * @return 这次应用受影响的表。
	 * @throws Exception exception。
	 */
	public Map<ApplyTable<?, ?>, Set<Object>> apply(int count) throws Exception {
		var endTime = System.currentTimeMillis() - beforeTimeMs;
		var result = new HashMap<ApplyTable<?, ?>, Set<Object>>();
		exclusiveStartKey = historyTable.walkDatabase(exclusiveStartKey, count, (key, value) -> {
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
			return true;
		});
		return result;
	}
}
