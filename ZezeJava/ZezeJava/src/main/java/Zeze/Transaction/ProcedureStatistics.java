package Zeze.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.LongAdder;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.LongMap;
import Zeze.Util.Random;
import Zeze.Util.Task;
import com.amazonaws.services.dynamodbv2.xspec.S;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

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

	public synchronized void start(long period) {
		if (null != timer)
			return;
		timer = Task.scheduleUnsafe(Random.getInstance().nextLong(period), period, this::report);
	}

	private void report() {
		var sorted = new Statistics[getProcedures().size()];
		getProcedures().values().toArray(sorted);
		Arrays.sort(sorted);

		var sb = new StringBuilder();
		var min = Math.min(sorted.length, 20);
		for (int i = 0; i < min; ++i) {
			var stat = sorted[i];
			var success = stat.getResults().get(0L);
			if (null == success || success.sum() == 0)
				break;
			if (sb.length() == 0)
				sb.append("ProcedureStatistics:\n");
			sb.append('\t').append(stat.getProcedureName());
			stat.buildString(", ", sb, "");
			sb.append('\n');
		}
		if (sb.length() > 0)
			logger.info(sb.toString());
	}

	public List<String> selectProcedureNameList(){
		List<String> nameList = new ArrayList<>();
		Enumeration<String> names = getProcedures().keys();
		while (names.hasMoreElements()){
			String name = names.nextElement();
			nameList.add(name);
		}
		return nameList;
	}

	public List<ResultLog> selectProcedureResults(String procedureName){
		Statistics statistics = procedures.get(procedureName);
		if (statistics == null){
			return Collections.emptyList();
		}
		List<ResultLog> resultLogs = new ArrayList<>();
		LongConcurrentHashMap<LongAdder> results = statistics.getResults();
		LongMap.LongIterator keys = results.keyIterator();
		while (keys.hasNext()){
			long key = keys.next();
			LongAdder longAdder = results.get(key);
			long sum = 0;
			if (longAdder != null){
				sum = longAdder.sum();
			}
			resultLogs.add(new ResultLog(key, sum));
		}
		return resultLogs;
	}



	public synchronized void stop() {
		if (null != timer) {
			timer.cancel(true);
			timer = null;
		}
		procedures.clear();
	}

	public ConcurrentHashMap<String, Statistics> getProcedures() {
		return procedures;
	}

	public Statistics getOrAdd(String procedureName) {
		return procedures.computeIfAbsent(procedureName, Statistics::new);
	}

	public static final class Statistics implements Comparable<Statistics> {
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

		@Override
		public int compareTo(@NotNull ProcedureStatistics.Statistics o) {
			var success = o.getResults().get(0L);
			var other = success != null ? success.sum() : 0L;

			success = getResults().get(0L);
			var self = success != null ? success.sum() : 0L;

			// 大的在前面。
			return Long.compare(other, self);
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

	public static final class ResultLog{
		private long result;
		private long sum;

		public ResultLog(long result, long sum) {
			this.result = result;
			this.sum = sum;
		}

		public ResultLog() {
		}

		public long getSum() {
			return sum;
		}

		public void setSum(long sum) {
			this.sum = sum;
		}

		public long getResult() {
			return result;
		}

		public void setResult(long result) {
			this.result = result;
		}
	}

}
