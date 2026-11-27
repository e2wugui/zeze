package Zeze.Transaction;

import Zeze.Util.PerfCounter;
import Zeze.Util.TaskSpec;
import Zeze.Util.TimerFuture;
import Zeze.Util.ZezeCounter;

/**
 * 过程执行速率监视：按名称周期统计 QPS，达到阈值时触发回调（如动态降级）。
 * 依赖单调累计计数源，当前仅 {@link PerfCounter} 支持，其他计数器实现下计数恒为 0。
 */
public final class ProcedureStatistics {
	private ProcedureStatistics() {
	}

	public static TimerFuture<?> watch(String procedureName, long reachPerSecond,
												Runnable handle) {
		var watcher = new Watcher(procedureName, reachPerSecond, handle);
		return TaskSpec.ofAction(watcher::check).schedulePeriodNow(Watcher.CheckPeriod, Watcher.CheckPeriod);
	}

	static final class Watcher {
		static final int CheckPeriod = 30_000; // 毫秒

		private final String procedureName;
		private final long reach;
		private final Runnable reachHandle;
		private long last;

		Watcher(String procedureName, long reachPerSecond, Runnable handle) {
			this.procedureName = procedureName;
			last = getTotalCount(procedureName);
			reach = reachPerSecond;
			reachHandle = handle;
		}

		static long getTotalCount(String procedureName) {
			// 速率差分必须使用单调累计计数源，不能用每轮重置的周期快照。
			if (ZezeCounter.instance instanceof PerfCounter perfCounter)
				return perfCounter.getProcedureTotalCount(procedureName);
			return 0;
		}

		void check() {
			long total = getTotalCount(procedureName);
			if ((total - last) * 1000 / CheckPeriod >= reach) {
				try {
					reachHandle.run();
				} catch (Throwable e) { // logger.error
					ZezeCounter.logger.error("ProcedureStatistics.Watcher.check exception:", e);
				}
			}
			last = total;
		}
	}
}
