package Zeze.Transaction;

import java.util.TreeMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.LongAdder;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Random;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class TableStatistics {
	private static final Logger logger = LogManager.getLogger(TableStatistics.class);
	// 为了使用的地方可以方便访问，定义成全局的。
	// 这里的tableId也是全局分配的，即时起多个Zeze.Application，也是没问题的。see Table.cs
	private static final TableStatistics instance = new TableStatistics();

	public static TableStatistics getInstance() {
		return instance;
	}

	private final LongConcurrentHashMap<Statistics> tables = new LongConcurrentHashMap<>();

	private TableStatistics() {
	}

	public LongConcurrentHashMap<Statistics> getTables() {
		return tables;
	}

	public Statistics getOrAdd(int id) {
		return getTables().computeIfAbsent(id, Statistics::new);
	}

	private Future<?> timer;

	public synchronized void start() {
		if (null != timer)
			return;
		timer = Task.scheduleUnsafe(Random.getInstance().nextLong(60000), 60000, this::report);
	}

	private void report() {
		var sorted = new TreeMap<Long, Statistics>();
		tables.forEach((e) -> sorted.put(e.getTableFindCount(), e));

		var sb = new StringBuilder();
		var it = sorted.descendingMap().entrySet().iterator();
		for (int i = 0; i < 20 && it.hasNext(); ++i) {
			var e = it.next();
			if (e.getKey() == 0)
				break;
			var stat = e.getValue();
			if (sb.length() == 0)
				sb.append("TableStatistics:\n");
			sb.append('\t').append(stat.getTableName());
			stat.buildString(", ", sb, "");
			sb.append('\n');
		}
		if (sb.length() > 0)
			logger.info(sb.toString());
	}

	public synchronized void stop() {
		if (null != timer) {
			timer.cancel(true);
			timer = null;
		}
		tables.clear();
	}

	public static final class Statistics {
		private final long tableId; // 实际上是int，只是TableKey.tables存储成long了。
		private final LongAdder readLockTimes = new LongAdder();
		private final LongAdder writeLockTimes = new LongAdder();
		private final LongAdder storageFindCount = new LongAdder();
		// 这两个统计用来观察cache清理的影响，
		private final LongAdder tryReadLockTimes = new LongAdder();
		private final LongAdder tryWriteLockTimes = new LongAdder();
		// global acquire 的次数，即时没有开启cache-sync，也会有一点点计数，因为没人抢，所以以后总是成功了。
		private final LongAdder globalAcquireShare = new LongAdder();
		private final LongAdder globalAcquireModify = new LongAdder();
		private final LongAdder globalAcquireInvalid = new LongAdder();

		public LongAdder getReadLockTimes() {
			return readLockTimes;
		}

		public LongAdder getWriteLockTimes() {
			return writeLockTimes;
		}

		public LongAdder getStorageFindCount() {
			return storageFindCount;
		}

		public LongAdder getTryReadLockTimes() {
			return tryReadLockTimes;
		}

		public LongAdder getTryWriteLockTimes() {
			return tryWriteLockTimes;
		}

		public LongAdder getGlobalAcquireShare() {
			return globalAcquireShare;
		}

		public LongAdder getGlobalAcquireModify() {
			return globalAcquireModify;
		}

		public LongAdder getGlobalAcquireInvalid() {
			return globalAcquireInvalid;
		}

		// 虽然有锁升级存在，但数量很少，忽略掉后，就可以把读写访问加起来当作总的查找次数。
		public long getTableFindCount() {
			return getReadLockTimes().sum() + getWriteLockTimes().sum();
		}

		public double getTableCacheHit() {
			double total = getTableFindCount();
			return (total - getStorageFindCount().sum()) / total;
		}

		public double getGlobalAcquireShareHit() {
			double total = getTableFindCount();
			return (total - getGlobalAcquireShare().sum()) / total;
		}

		public double getGlobalAcquireModifyHit() {
			double total = getTableFindCount();
			return (total - getGlobalAcquireModify().sum()) / total;
		}

		public Statistics(long tableId) {
			this.tableId = tableId;
		}

		public int getTableId() {
			return (int)tableId;
		}

		public String getTableName() {
			return TableKey.tables.get(tableId);
		}

		public void buildString(String prefix, StringBuilder sb, String end) {
			sb.append(prefix).append("CacheHit=").append(getTableCacheHit()).append(end);
			sb.append(prefix).append("AcquireShare=").append(getGlobalAcquireShareHit()).append(end);
			sb.append(prefix).append("AcquireShare=").append(getGlobalAcquireModifyHit()).append(end);

			sb.append(prefix).append("AcquireShare=").append(getGlobalAcquireShare().sum()).append(end);
			sb.append(prefix).append("AcquireModify=").append(getGlobalAcquireModify().sum()).append(end);
			sb.append(prefix).append("AcquireInvalid=").append(getGlobalAcquireInvalid().sum()).append(end);

			sb.append(prefix).append("StorageFind=").append(getStorageFindCount().sum()).append(end);
			sb.append(prefix).append("TableFind=").append(getTableFindCount()).append(end);

			sb.append(prefix).append("ReadLocks=").append(getReadLockTimes().sum()).append(end);
			sb.append(prefix).append("WriteLocks=").append(getReadLockTimes().sum()).append(end);
			sb.append(prefix).append("TryReadLocks=").append(getTryReadLockTimes().sum()).append(end);
			sb.append(prefix).append("TryWriteLocks=").append(getTryWriteLockTimes().sum()).append(end);
		}
	}
}
