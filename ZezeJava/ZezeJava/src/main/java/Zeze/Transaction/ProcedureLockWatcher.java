package Zeze.Transaction;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Util.IntHashMap;
import Zeze.Util.OutInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ProcedureLockWatcher {
	private static final @NotNull Logger logger = LogManager.getLogger(ProcedureLockWatcher.class);
	private final int procedureLockWatcherMin;
	private final ConcurrentHashMap<String, AtomicInteger> procedureMaxLocks = new ConcurrentHashMap<>();

	public ProcedureLockWatcher(@NotNull Application zeze) {
		procedureLockWatcherMin = zeze.getConfig().getProcedureLockWatcherMin();
	}

	public void doWatch(@NotNull Procedure p, @NotNull TreeMap<TableKey, RecordAccessed> recordAccessed) {
		var lockCount = recordAccessed.size();
		if (lockCount < procedureLockWatcherMin)
			return;

		var max = procedureMaxLocks.computeIfAbsent(p.getActionName(), __ -> new AtomicInteger());
		for (; ; ) {
			var oldMax = max.get();
			if (lockCount <= oldMax)
				break;
			if (max.compareAndSet(oldMax, lockCount)) {
				log(p, recordAccessed);
				break;
			}
		}
	}

	private static void log(@NotNull Procedure p, @NotNull TreeMap<TableKey, RecordAccessed> recordAccessed) {
		// 统计表的锁数量。
		var tableIdCount = new IntHashMap<OutInt>();
		for (var tableKey : recordAccessed.keySet())
			tableIdCount.computeIfAbsent(tableKey.getId(), __ -> new OutInt()).value++;
		// 格式化log
		var sb = new StringBuilder(64).append(p.getActionName()).append(": ");
		for (var it = tableIdCount.iterator(); it.moveToNext(); )
			sb.append(TableKey.tables.get(it.key())).append('=').append(it.value().value).append(',');
		logger.warn("{}", sb.toString());
	}
}
