package Zeze.Transaction;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Util.IntHashMap;
import Zeze.Util.OutInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 过程持锁数监视：事务加锁数超过阈值或刷新历史峰值时告警，辅助发现热点大事务。 */
public class ProcedureLockWatcher {
	private static final Logger logger = LogManager.getLogger(ProcedureLockWatcher.class);
	private final int procedureLockWatcherMin;
	private final ConcurrentHashMap<String, AtomicInteger> procedureMaxLocks = new ConcurrentHashMap<>();

	public ProcedureLockWatcher(Application zeze) {
		procedureLockWatcherMin = zeze.getConfig().getProcedureLockWatcherMin();
	}

	public void doWatch(Procedure p, TreeMap<TableKey, RecordAccessed> recordAccessed) {
		var lockCount = recordAccessed.size();
		if (lockCount < procedureLockWatcherMin)
			return;

		p.procedureCounter().manyLocks(lockCount);

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

	private static void log(Procedure p, TreeMap<TableKey, RecordAccessed> recordAccessed) {
		// 统计表的锁数量。
		var tableIdCount = new IntHashMap<OutInt>();
		for (var tableKey : recordAccessed.keySet())
			//noinspection DataFlowIssue
			tableIdCount.computeIfAbsent(tableKey.getId(), __ -> new OutInt()).value++;
		// 格式化log
		var sb = new StringBuilder(64).append(p.getActionName()).append(": ");
		for (var it = tableIdCount.iterator(); it.moveToNext(); )
			sb.append(TableKey.tables.get(it.key())).append('=').append(it.value().value).append(',');
		logger.warn("{}", sb.toString());
	}
}
