package Zeze.Transaction;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 在Procedure中统计，由于嵌套存储过程存在，总数会比实际事务数多。
 * 一般嵌套存储过程很少用，事务数量也可以参考这里的数值，不单独统计。
 * 另外Transaction在重做时会在这里保存重做次数的统计。通过name和存储过程区分开来。
 */
public final class ProcedureStatistics {
	private static final Logger logger = LogManager.getLogger(ProcedureStatistics.class);
	private static final ProcedureStatistics instance = new ProcedureStatistics();

	public static ProcedureStatistics getInstance() {
		return instance;
	}

	private final ConcurrentHashMap<String, Statistics> procedures = new ConcurrentHashMap<>();

	private ProcedureStatistics() {
	}

	public ConcurrentHashMap<String, Statistics> getProcedures() {
		return procedures;
	}

	public Statistics getOrAdd(String procedureName) {
		return procedures.computeIfAbsent(procedureName, __ -> new Statistics());
	}

	public static final class Statistics {
		private final LongConcurrentHashMap<LongAdder> Results = new LongConcurrentHashMap<>();

		public LongConcurrentHashMap<LongAdder> getResults() {
			return Results;
		}

		public LongAdder getOrAdd(long result) {
			return Results.computeIfAbsent(result, __ -> new LongAdder());
		}

		public void buildString(String prefix, StringBuilder sb, String end) {
			for (var it = Results.entryIterator(); it.moveToNext(); )
				sb.append(prefix).append(it.key()).append("=").append(it.value().sum()).append(end);
		}

		public long getTotalCount() {
			long total = 0;
			for (var v : Results)
				total += v.sum();
			return total;
		}

		public void watch(long reachPerSecond, Runnable handle) {
			var watcher = new Watcher(reachPerSecond, handle);
			Task.schedule(Watcher.CheckPeriod * 1000, Watcher.CheckPeriod * 1000, watcher::check);
		}

		public final class Watcher {
			public static final int CheckPeriod = 30; // 秒

			public long last;
			public final long reach;
			public final Runnable reachHandle;

			public Watcher(long reachPerSecond, Runnable handle) {
				last = getTotalCount();
				reach = reachPerSecond;
				reachHandle = handle;
			}

			public void check() {
				long total = getTotalCount();
				if ((total - last) / CheckPeriod >= reach) {
					try {
						reachHandle.run();
					} catch (Throwable e) { // logger.error
						logger.error("ProcedureStatistics.Watcher", e);
					}
				}
				last = total;
			}
		}
	}
}
