package Zeze.History;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Application;
import Zeze.Transaction.TableKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Verify {
	private static final Logger logger = LogManager.getLogger();

	public static void run(Application zeze) {
		var applyDb = new ApplyDatabaseMemory();
		var defaultDb = zeze.getDatabase("");
		var applyTables = new ConcurrentHashMap<Integer, ApplyTable<?, ?>>();
		zeze.checkpointRun();
		var counter = new AtomicLong();
		var total = new AtomicLong();
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
			var process = counter.incrementAndGet();
			if (process >= 50000) {
				logger.info("history applying ................. " + total.addAndGet(process));
				counter.set(0);
			}
			return true;
		});
		var process = counter.incrementAndGet();
		logger.info("history apply end! +++++++++++++++++ " + total.addAndGet(process));
		for (var applyTable : applyTables.values())
			applyTable.verifyAndClear();
		logger.info("history verify success!!!!!!!!!!!!!!!!!!!!!!");
	}
}
