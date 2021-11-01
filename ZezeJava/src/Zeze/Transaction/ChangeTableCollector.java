package Zeze.Transaction;

import java.util.*;

public final class ChangeTableCollector {
	private final HashMap<Object, ChangeRecordCollector> records = new HashMap<>(); // key is Record.Key
	private final Table table;
	private final boolean tableHasListener;

	public ChangeTableCollector(Table t, TableKey tableKey) {
		table = t;
		tableHasListener = table.getChangeListenerMap().HasListener();
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

		var crc = records.get(tableKey.getKey());
		if (null != crc) {
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