package Zeze.Transaction;

import Zeze.Application;

import java.util.*;

public final class ChangeCollector {
	private final HashMap<String, ChangeTableCollector> tables = new HashMap<>(); // key is Table.Id

	@FunctionalInterface
	public interface Collect {
		ChangePathAndNote doIt();
	}

	public void BuildCollect(Application zeze, TableKey tableKey, Zeze.Transaction.RecordAccessed recordAccessed) {
		var tableCollector = tables.get(tableKey.getName());
		if (null == tableCollector) {
			tableCollector = new ChangeTableCollector(zeze.GetTable(tableKey.getName()));
			tables.put(tableKey.getName(), tableCollector);
		}
		tableCollector.BuildCollect(tableKey, recordAccessed);
	}

	public void CollectChanged(TableKey tableKey, Collect collect) {
		var ctc = tables.get(tableKey.getName());
		if (null != ctc) {
			ctc.CollectChanged(tableKey, collect);
		}
		// else skip error 只有测试代码可能会走到这个分支。
	}

	public void Notify() {
		for (var ctc : tables.values()) {
			ctc.Notify();
		}
	}
}