package Zeze.History;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Transaction.TableKey;

public class Verify {
	public static void run(Application zeze) {
		var applyDb = new ApplyDatabaseMemory();
		var defaultDb = zeze.getDatabase("");
		var applyTables = new ConcurrentHashMap<Integer, ApplyTable<?, ?>>();
		zeze.checkpointRun();
		zeze.getHistoryModule().getTable().walkDatabase((key, value) -> {
			for (var r : value.getChanges().entrySet()) {
				var applyTable = applyTables.computeIfAbsent(r.getKey().getTableId(), __ -> {
					var taleName = TableKey.tables.get(r.getKey().getTableId());
					if (null == taleName)
						throw new RuntimeException("table id not found. id=" + r.getKey().getTableId());
					var originTable = defaultDb.getTable(taleName);
					if (null == originTable)
						throw new RuntimeException("table not found. name=" + taleName);
					return originTable.createApplyTable(applyDb);
				});
				applyTable.apply(r.getKey(), r.getValue());
			}
			return true;
		});
		for (var applyTable : applyTables.values())
			applyTable.verifyAndClear();
	}
}
