package Zeze.Transaction;

import Zeze.Util.PerfCounter;
import Zeze.Util.Task;
import Zeze.Util.TimerFuture;
import org.jetbrains.annotations.NotNull;

public final class ProcedureStatistics {
	private ProcedureStatistics() {
	}

	public static TimerFuture<?> watch(@NotNull String procedureName, long reachPerSecond, @NotNull Runnable handle) {
		var watcher = new Watcher(procedureName, reachPerSecond, handle);
		return Task.scheduleUnsafe(Watcher.CheckPeriod, Watcher.CheckPeriod, watcher::check);
	}

	static final class Watcher {
		static final int CheckPeriod = 30_000; // 毫秒

		private final @NotNull String procedureName;
		private final long reach;
		private final Runnable reachHandle;
		private long last;

		Watcher(@NotNull String procedureName, long reachPerSecond, Runnable handle) {
			this.procedureName = procedureName;
			last = getTotalCount(procedureName);
			reach = reachPerSecond;
			reachHandle = handle;
		}

		static long getTotalCount(@NotNull String procedureName) {
			long total = 0;
			var pInfo = PerfCounter.instance.getProcedureInfo(procedureName);
			if (pInfo != null) {
				for (var v : pInfo.getResultMapLast())
					total += v.sum();
			}
			return total;
		}

		void check() {
			long total = getTotalCount(procedureName);
			if ((total - last) / CheckPeriod >= reach) {
				try {
					reachHandle.run();
				} catch (Throwable e) { // logger.error
					PerfCounter.logger.error("ProcedureStatistics.Watcher.check exception:", e);
				}
			}
			last = total;
		}
	}
}
