package Zeze.Transaction;

import Zeze.*;

public class TableStatistics {
	// 为了使用的地方可以方便访问，定义成全局的。
	// 这里的tableId也是全局分配的，即时起多个Zeze.Application，也是没问题的。see Table.cs
	private static TableStatistics Instance = new TableStatistics();
	public static TableStatistics getInstance() {
		return Instance;
	}

	private java.util.concurrent.ConcurrentHashMap<Integer, Statistics> Tables = new java.util.concurrent.ConcurrentHashMap<Integer, Statistics> ();
	public final java.util.concurrent.ConcurrentHashMap<Integer, Statistics> getTables() {
		return Tables;
	}

	public final Statistics GetOrAdd(int tableId) {
		return getTables().putIfAbsent(tableId, (key) -> new Statistics());
	}

	public static class Statistics {
		private Zeze.Util.AtomicLong ReadLockTimes = new Util.AtomicLong();
		public final Zeze.Util.AtomicLong getReadLockTimes() {
			return ReadLockTimes;
		}
		private Zeze.Util.AtomicLong WriteLockTimes = new Util.AtomicLong();
		public final Zeze.Util.AtomicLong getWriteLockTimes() {
			return WriteLockTimes;
		}
		private Zeze.Util.AtomicLong StorageFindCount = new Util.AtomicLong();
		public final Zeze.Util.AtomicLong getStorageFindCount() {
			return StorageFindCount;
		}

		// 这两个统计用来观察cache清理的影响，
		private Zeze.Util.AtomicLong TryReadLockTimes = new Util.AtomicLong();
		public final Zeze.Util.AtomicLong getTryReadLockTimes() {
			return TryReadLockTimes;
		}
		private Zeze.Util.AtomicLong TryWriteLockTimes = new Util.AtomicLong();
		public final Zeze.Util.AtomicLong getTryWriteLockTimes() {
			return TryWriteLockTimes;
		}

		// global acquire 的次数，即时没有开启cache-sync，也会有一点点计数，因为没人抢，所以以后总是成功了。
		private Zeze.Util.AtomicLong GlobalAcquireShare = new Util.AtomicLong();
		public final Zeze.Util.AtomicLong getGlobalAcquireShare() {
			return GlobalAcquireShare;
		}
		private Zeze.Util.AtomicLong GlobalAcquireModify = new Util.AtomicLong();
		public final Zeze.Util.AtomicLong getGlobalAcquireModify() {
			return GlobalAcquireModify;
		}
		private Zeze.Util.AtomicLong GlobalAcquireInvalid = new Util.AtomicLong();
		public final Zeze.Util.AtomicLong getGlobalAcquireInvalid() {
			return GlobalAcquireInvalid;
		}

		// 虽然有锁升级存在，但数量很少，忽略掉后，就可以把读写访问加起来当作总的查找次数。
		public final long getTableFindCount() {
			return getReadLockTimes().Get() + getWriteLockTimes().Get();
		}
		public final double getTableCacheHit() {
			double total = getTableFindCount();
			return (total - getStorageFindCount().Get()) / total;
		}
		public final double getGlobalAcquireShareHit() {
			double total = getTableFindCount();
			return (total - getGlobalAcquireShare().Get()) / total;
		}
		public final double getGlobalAcquireModifyHit() {
			double total = getTableFindCount();
			return (total - getGlobalAcquireModify().Get()) / total;
		}
	}
}