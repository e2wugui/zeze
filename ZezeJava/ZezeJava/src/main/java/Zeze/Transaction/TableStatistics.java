package Zeze.Transaction;

import java.util.concurrent.atomic.LongAdder;
import Zeze.Util.LongConcurrentHashMap;

public final class TableStatistics {
	// 为了使用的地方可以方便访问，定义成全局的。
	// 这里的tableId也是全局分配的，即时起多个Zeze.Application，也是没问题的。see Table.cs
	private static final TableStatistics Instance = new TableStatistics();

	public static TableStatistics getInstance() {
		return Instance;
	}

	private final LongConcurrentHashMap<Statistics> Tables = new LongConcurrentHashMap<>();

	private TableStatistics() {
	}

	public LongConcurrentHashMap<Statistics> getTables() {
		return Tables;
	}

	public Statistics GetOrAdd(int id) {
		return getTables().computeIfAbsent(id, __ -> new Statistics());
	}

	public static final class Statistics {
		private final LongAdder ReadLockTimes = new LongAdder();
		private final LongAdder WriteLockTimes = new LongAdder();
		private final LongAdder StorageFindCount = new LongAdder();
		// 这两个统计用来观察cache清理的影响，
		private final LongAdder TryReadLockTimes = new LongAdder();
		private final LongAdder TryWriteLockTimes = new LongAdder();
		// global acquire 的次数，即时没有开启cache-sync，也会有一点点计数，因为没人抢，所以以后总是成功了。
		private final LongAdder GlobalAcquireShare = new LongAdder();
		private final LongAdder GlobalAcquireModify = new LongAdder();
		private final LongAdder GlobalAcquireInvalid = new LongAdder();

		public LongAdder getReadLockTimes() {
			return ReadLockTimes;
		}

		public LongAdder getWriteLockTimes() {
			return WriteLockTimes;
		}

		public LongAdder getStorageFindCount() {
			return StorageFindCount;
		}

		public LongAdder getTryReadLockTimes() {
			return TryReadLockTimes;
		}

		public LongAdder getTryWriteLockTimes() {
			return TryWriteLockTimes;
		}

		public LongAdder getGlobalAcquireShare() {
			return GlobalAcquireShare;
		}

		public LongAdder getGlobalAcquireModify() {
			return GlobalAcquireModify;
		}

		public LongAdder getGlobalAcquireInvalid() {
			return GlobalAcquireInvalid;
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
	}
}
