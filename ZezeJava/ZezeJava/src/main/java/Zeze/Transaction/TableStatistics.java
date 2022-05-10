package Zeze.Transaction;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

public class TableStatistics {
	// 为了使用的地方可以方便访问，定义成全局的。
	// 这里的tableId也是全局分配的，即时起多个Zeze.Application，也是没问题的。see Table.cs
	private final static TableStatistics Instance = new TableStatistics();
	public static TableStatistics getInstance() {
		return Instance;
	}

	private final ConcurrentHashMap<Integer, Statistics> Tables = new ConcurrentHashMap<> ();
	public final ConcurrentHashMap<Integer, Statistics> getTables() {
		return Tables;
	}

	public final Statistics GetOrAdd(int id) {
		return getTables().computeIfAbsent(id, (key) -> new Statistics());
	}

	public static class Statistics {
		private final AtomicLong ReadLockTimes = new AtomicLong();
		public final AtomicLong getReadLockTimes() {
			return ReadLockTimes;
		}
		private final AtomicLong WriteLockTimes = new AtomicLong();
		public final AtomicLong getWriteLockTimes() {
			return WriteLockTimes;
		}
		private final AtomicLong StorageFindCount = new AtomicLong();
		public final AtomicLong getStorageFindCount() {
			return StorageFindCount;
		}

		// 这两个统计用来观察cache清理的影响，
		private final AtomicLong TryReadLockTimes = new AtomicLong();
		public final AtomicLong getTryReadLockTimes() {
			return TryReadLockTimes;
		}
		private final AtomicLong TryWriteLockTimes = new AtomicLong();
		public final AtomicLong getTryWriteLockTimes() {
			return TryWriteLockTimes;
		}

		// global acquire 的次数，即时没有开启cache-sync，也会有一点点计数，因为没人抢，所以以后总是成功了。
		private final AtomicLong GlobalAcquireShare = new AtomicLong();
		public final AtomicLong getGlobalAcquireShare() {
			return GlobalAcquireShare;
		}
		private final AtomicLong GlobalAcquireModify = new AtomicLong();
		public final AtomicLong getGlobalAcquireModify() {
			return GlobalAcquireModify;
		}
		private final AtomicLong GlobalAcquireInvalid = new AtomicLong();
		public final AtomicLong getGlobalAcquireInvalid() {
			return GlobalAcquireInvalid;
		}

		// 虽然有锁升级存在，但数量很少，忽略掉后，就可以把读写访问加起来当作总的查找次数。
		public final long getTableFindCount() {
			return getReadLockTimes().get() + getWriteLockTimes().get();
		}
		public final double getTableCacheHit() {
			double total = getTableFindCount();
			return (total - getStorageFindCount().get()) / total;
		}
		public final double getGlobalAcquireShareHit() {
			double total = getTableFindCount();
			return (total - getGlobalAcquireShare().get()) / total;
		}
		public final double getGlobalAcquireModifyHit() {
			double total = getTableFindCount();
			return (total - getGlobalAcquireModify().get()) / total;
		}
	}
}