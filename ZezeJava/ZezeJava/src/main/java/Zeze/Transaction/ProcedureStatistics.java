package Zeze.Transaction;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
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

	private Future<?> timer;

	public synchronized void start() {
		if (null != timer)
			return;
		timer = Task.scheduleUnsafe(60000, 60000, this::report);
	}

	private void report() {
		var sorted = new TreeMap<Long, Statistics>();
		for (var e : getProcedures().values()) {
			sorted.put(e.getResults().get(0L).sum(), e);
		}
		var sb = new StringBuilder();
		sb.append("ProcedureStatistics:\n");
		var it = sorted.descendingMap().entrySet().iterator();
		for (int i = 0; i < 20 && it.hasNext(); ++i) {
			var stat = it.next().getValue();
			sb.append("\t").append(stat.getProcedureName());
			stat.buildString(",", sb, "");
		}
		logger.info(sb.toString());
	}

	public synchronized void stop() {
		if (null != timer) {
			timer.cancel(true);
			timer = null;
		}
	}

	public ConcurrentHashMap<String, Statistics> getProcedures() {
		return procedures;
	}

	public Statistics getOrAdd(String procedureName) {
		return procedures.computeIfAbsent(procedureName, Statistics::new);
	}

	public static final class Statistics {
		private final LongConcurrentHashMap<LongAdder> Results = new LongConcurrentHashMap<>();
		private final String procedureName;

		public Statistics(String name) {
			procedureName = name;
		}

		public LongConcurrentHashMap<LongAdder> getResults() {
			return Results;
		}

		public LongAdder getOrAdd(long result) {
			return Results.computeIfAbsent(result, __ -> new LongAdder());
		}

		public String getProcedureName() {
			return procedureName;
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
