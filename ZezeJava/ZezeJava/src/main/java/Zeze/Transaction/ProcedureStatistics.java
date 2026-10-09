package Zeze.Transaction;

import Zeze.Util.PerfCounter;
import Zeze.Util.TaskSpec;
import Zeze.Util.TimerFuture;
import Zeze.Util.ZezeCounter;
import org.jetbrains.annotations.NotNull;

/**
 * 过程执行速率监视：按名称周期统计 QPS，达到阈值时触发回调（如动态降级）。
 *
 * <p>速率差分依赖单调递增的累计计数源，当前仅 {@link PerfCounter} 实现提供
 * （{@code -DZezeCounter} 指定其他实现或禁用统计时计数恒为0，watch 永不触发，
 * 属知情不可用）。
 */
public final class ProcedureStatistics {
	private ProcedureStatistics() {
	}

	public static @NotNull TimerFuture<?> watch(@NotNull String procedureName, long reachPerSecond,
												@NotNull Runnable handle) {
		var watcher = new Watcher(procedureName, reachPerSecond, handle);
		return TaskSpec.ofAction(watcher::check).schedulePeriodNow(Watcher.CheckPeriod, Watcher.CheckPeriod);
	}

	static final class Watcher {
		static final int CheckPeriod = 30_000; // 毫秒

		private final @NotNull String procedureName;
		private final long reach;
		private final @NotNull Runnable reachHandle;
		private long last;

		Watcher(@NotNull String procedureName, long reachPerSecond, @NotNull Runnable handle) {
			this.procedureName = procedureName;
			last = getTotalCount(procedureName);
			reach = reachPerSecond;
			reachHandle = handle;
		}

		static long getTotalCount(@NotNull String procedureName) {
			// 不能用周期快照（getLast().procedureResults()）做差分：快照每轮换新且
			// 只含上一周期计数，(total-last)在稳态恒0永不触发，收集边界恰好落入
			// 检查间隔时又把整周期计数当30s速率（约3.3倍高估）。
			if (ZezeCounter.instance instanceof PerfCounter perfCounter)
				return perfCounter.getProcedureTotalCount(procedureName);
			return 0; // 非PerfCounter实现无单调累计源，watch不可用（见类注释）
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
