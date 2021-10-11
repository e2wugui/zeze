package Zeze.Transaction;

import java.util.*;

public final class ChangeCollector {
	private final HashMap<Integer, ChangeTableCollector> tables = new HashMap<Integer, ChangeTableCollector>(); // key is Table.Id

	@FunctionalInterface
	public interface Collect {
		ChangePathAndNote doIt();
	}

	public void BuildCollect(TableKey tableKey, Zeze.Transaction.RecordAccessed recordAccessed) {
		var tableCollector = tables.get(tableKey.getTableId());
		if (null == tableCollector) {
			tableCollector = new ChangeTableCollector(tableKey);
			tables.put(tableKey.getTableId(), tableCollector);
		}
		tableCollector.BuildCollect(tableKey, recordAccessed);
	}

	public void CollectChanged(TableKey tableKey, Collect collect) {
		var ctc = tables.get(tableKey.getTableId());
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