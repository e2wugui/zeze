package Zeze.Transaction;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProcedureLockWatcher {
	private static final Logger logger = LogManager.getLogger();
	private final Application zeze;
	private final ConcurrentHashMap<String, AtomicInteger> procedureMaxLocks = new ConcurrentHashMap<>();

	public ProcedureLockWatcher(Application zeze) {
		this.zeze = zeze;
	}

	public Application getZeze() {
		return zeze;
	}

	public void doWatch(Procedure p, TreeMap<TableKey, RecordAccessed> recordAccessed) {
		var lockCount = recordAccessed.size();
		if (lockCount < zeze.getConfig().getProcedureLockWatcherMin())
			return;

		var max = procedureMaxLocks.computeIfAbsent(p.getActionName(), __ -> new AtomicInteger());
		if (lockCount > max.get()) {
			max.set(lockCount);
			log(p, recordAccessed);
		}
	}

	private static void log(Procedure p, TreeMap<TableKey, RecordAccessed> recordAccessed) {
		// 统计表的锁数量。按名字排序。
		var tableKeyCount = new TreeMap<String, AtomicInteger>();
		for (var tableKey : recordAccessed.keySet()) {
			var tableName = TableKey.tables.get(tableKey.getId());
			var keyCount = tableKeyCount.computeIfAbsent(tableName, __ -> new AtomicInteger());
			keyCount.incrementAndGet();
		}
		// 格式化log
		var sb = new StringBuilder();
		sb.append(p.getActionName()).append(" ");
		for (var tkc : tableKeyCount.entrySet()) {
			sb.append(tkc.getKey()).append("=").append(tkc.getValue().get()).append(",");
		}
		logger.warn(sb.toString());
	}
}
