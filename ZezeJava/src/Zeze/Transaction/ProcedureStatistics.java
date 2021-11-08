package Zeze.Transaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

/** 
 在Procedure中统计，由于嵌套存储过程存在，总数会比实际事务数多。
 一般嵌套存储过程很少用，事务数量也可以参考这里的数值，不单独统计。
 另外Transaction在重做时会在这里保存重做次数的统计。通过name和存储过程区分开来。
*/
public class ProcedureStatistics {
	private static ProcedureStatistics Instance = new ProcedureStatistics();
	public static ProcedureStatistics getInstance() {
		return Instance;
	}
	static final Logger logger = LogManager.getLogger(ProcedureStatistics.class);

	private ConcurrentHashMap<String, Statistics> Procedures = new ConcurrentHashMap<> ();
	public final ConcurrentHashMap<String, Statistics> getProcedures() {
		return Procedures;
	}

	public final Statistics GetOrAdd(String procedureName) {
		return getProcedures().computeIfAbsent(procedureName, (key) -> new Statistics());
	}

	public static class Statistics {
		private ConcurrentHashMap<Long, AtomicLong> Results = new ConcurrentHashMap<> ();
		public final ConcurrentHashMap<Long, AtomicLong> getResults() {
			return Results;
		}

		public final AtomicLong GetOrAdd(long result) {
			return getResults().computeIfAbsent(result, (key) -> new AtomicLong());
		}

		public long GetTotalCount() {
			long total = 0;
			for (var e : Results.entrySet())
			{
				total += e.getValue().get();
			}
			return total;
		}

		public void Watch(long reachPerSecond, Runnable handle) {
			var watcher = new Watcher(this, reachPerSecond, handle);
			Zeze.Util.Task.schedule((thisTask) -> watcher.Check(this),
					Watcher.CheckPeriod * 1000, Watcher.CheckPeriod * 1000);
		}

		public class Watcher {
			public final static int CheckPeriod = 30;

			public long Last;
			public long Reach;
			public Runnable ReachHandle;

			public Watcher(Statistics stats, long reachPerSecond, Runnable handle) {
				Last = stats.GetTotalCount();
				Reach = reachPerSecond;
				ReachHandle = handle;
			}

			public void Check(Statistics stats) {
				var total = stats.GetTotalCount();
				if ((total - Last) / CheckPeriod > Reach)
				{
					try {
						ReachHandle.run();
					} catch (Throwable e) {
						logger.error("ProcedureStatistics.Watcher", e);
					}
				}
				Last = total;
			}
		}
	}
}