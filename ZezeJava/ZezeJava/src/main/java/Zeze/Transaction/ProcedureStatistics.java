package Zeze.Transaction;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 在Procedure中统计，由于嵌套存储过程存在，总数会比实际事务数多。
 * 一般嵌套存储过程很少用，事务数量也可以参考这里的数值，不单独统计。
 * 另外Transaction在重做时会在这里保存重做次数的统计。通过name和存储过程区分开来。
 */
public class ProcedureStatistics {
	private static final Logger logger = LogManager.getLogger(ProcedureStatistics.class);

	private static final ProcedureStatistics Instance = new ProcedureStatistics();

	private final ConcurrentHashMap<String, Statistics> Procedures = new ConcurrentHashMap<>();

	public static ProcedureStatistics getInstance() {
		return Instance;
	}

	private ProcedureStatistics() {
	}

	public final ConcurrentHashMap<String, Statistics> getProcedures() {
		return Procedures;
	}

	public final Statistics GetOrAdd(String procedureName) {
		return Procedures.computeIfAbsent(procedureName, key -> new Statistics());
	}

	public static class Statistics {
		private final LongConcurrentHashMap<AtomicLong> Results = new LongConcurrentHashMap<>();

		public final LongConcurrentHashMap<AtomicLong> getResults() {
			return Results;
		}

		public final AtomicLong GetOrAdd(long result) {
			return Results.computeIfAbsent(result, key -> new AtomicLong());
		}

		public long GetTotalCount() {
			long total = 0;
			for (var v : Results)
				total += v.get();
			return total;
		}

		public void Watch(long reachPerSecond, Runnable handle) {
			var watcher = new Watcher(reachPerSecond, handle);
			Task.schedule(Watcher.CheckPeriod * 1000, Watcher.CheckPeriod * 1000, watcher::Check);
		}

		public class Watcher {
			public static final int CheckPeriod = 30; // 秒

			public long Last;
			public final long Reach;
			public final Runnable ReachHandle;

			public Watcher(long reachPerSecond, Runnable handle) {
				Last = GetTotalCount();
				Reach = reachPerSecond;
				ReachHandle = handle;
			}

			public void Check() {
				long total = GetTotalCount();
				if ((total - Last) / CheckPeriod >= Reach) {
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
