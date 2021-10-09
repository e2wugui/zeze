package Zeze.Transaction;

import Zeze.*;
import java.util.*;

public final class ChangeTableCollector {
	private final HashMap<Object, ChangeRecordCollector> records = new HashMap<Object, ChangeRecordCollector>(); // key is Record.Key
	private final Table table;
	private final boolean tableHasListener;

	public ChangeTableCollector(TableKey tableKey) {
		table = Table.GetTable(tableKey.getTableId());
		tableHasListener = table.ChangeListenerMap.HasListener();
	}

	public void BuildCollect(TableKey tableKey, Zeze.Transaction.RecordAccessed recordAccessed) {
		if (tableHasListener) {
			ChangeRecordCollector recordCollector = new ChangeRecordCollector(tableKey, table, recordAccessed);
			records.put(tableKey.getKey(), recordCollector);
		}
	}

	public void CollectChanged(TableKey tableKey, ChangeCollector.Collect collect) {
		if (false == this.tableHasListener) {
			return; // 优化，表格没有监听者时，不收集改变。
		}

		TValue crc;
		if (records.containsKey(tableKey.getKey()) && (crc = records.get(tableKey.getKey())) == crc) {
			crc.CollectChanged(collect);
		}
		// else skip error . 只有测试代码可能会走到这个分支。
	}

	public void Notify() {
		if (false == this.tableHasListener) {
			return;
		}

		for (var crc : records.values()) {
			crc.Notify();
		}
	}
}