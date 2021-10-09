package Zeze.Transaction;

import Zeze.*;
import java.util.*;

public final class ChangeCollector {
	private final HashMap<Integer, ChangeTableCollector> tables = new HashMap<Integer, ChangeTableCollector>(); // key is Table.Id

	@FunctionalInterface
	public interface Collect {
		void invoke(tangible.OutObject<java.util.ArrayList<Util.KV<Bean, Integer>>> path, tangible.OutObject<ChangeNote> note);
	}

	public void BuildCollect(TableKey tableKey, Zeze.Transaction.RecordAccessed recordAccessed) {
		TValue tableCollector;
		if (false == (tables.containsKey(tableKey.getTableId()) && (tableCollector = tables.get(tableKey.getTableId())) == tableCollector)) {
			tableCollector = new ChangeTableCollector(tableKey);
			tables.put(tableKey.getTableId(), tableCollector);
		}
		tableCollector.BuildCollect(tableKey, recordAccessed);
	}

	public void CollectChanged(TableKey tableKey, Collect collect) {
		TValue ctc;
		if (tables.containsKey(tableKey.getTableId()) && (ctc = tables.get(tableKey.getTableId())) == ctc) {
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