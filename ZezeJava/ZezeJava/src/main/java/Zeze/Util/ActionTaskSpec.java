package Zeze.Util;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import Zeze.Transaction.DispatchMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 普通 Action0 任务的提交参数，通过 {@link TaskSpec#ofAction} 构造。
 *
 * <pre>
 * TaskSpec.ofAction(this::reconnect).name("Reconnect").schedule(1000);
 * TaskSpec.ofAction(this::dailyReset).period(24 * 3600_000L).scheduleAt(0, 0);
 * </pre>
 */
public final class ActionTaskSpec extends AbstractTaskSpec implements TaskSpec {
	private final @NotNull Action0 action;
	private long period = -1; // 仅 scheduleAt 使用，>0 时周期触发

	ActionTaskSpec(@NotNull Action0 action) {
		this.action = action;
	}

	/**
	 * @param name 任务名，用于日志与统计，默认使用 action 的类名
	 */
	public @NotNull ActionTaskSpec name(@Nullable String name) {
		this.name = name;
		return this;
	}

	/**
	 * @param mode 调度模式，null 等同 Normal；schedule 族终结方法忽略此参数（与旧 API 一致）
	 */
	public @NotNull ActionTaskSpec mode(@Nullable DispatchMode mode) {
		this.mode = mode;
		return this;
	}

	/**
	 * @param timeout 任务超时(毫秒)，不设置或小于0时，终结方法执行时取 Task.defaultTimeout
	 */
	public @NotNull ActionTaskSpec timeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * @param period 触发周期(毫秒)，仅 {@link #scheduleAt} 使用，大于0时周期触发，默认-1只触发一次
	 */
	public @NotNull ActionTaskSpec period(long period) {
		this.period = period;
		return this;
	}

	/**
	 * 等价 {@link Task#call(Action0, String)}：当前线程同步执行，异常吞掉只记录日志。
	 */
	public void call() {
		Task.callActionCore(action, name);
	}

	/**
	 * 事务感知：mode != Direct 且当前在运行中的事务内时延迟到事务提交后执行，否则立即异步执行。
	 * 等价 {@link Task#run(Action0, String, DispatchMode, long)}。
	 */
	public void run() {
		Task.runTxnAware(mode, () -> Task.executeUnsafeActionCore(action, name, mode, timeoutOrDefault()));
	}

	/**
	 * 注意: Unsafe 在事务中也会立即异步执行，即使之后该事务 redo 或 rollback 也无法撤销。等价
	 * {@link Task#runUnsafe(Action0, String, DispatchMode, long)}。
	 */
	public @NotNull Future<?> runUnsafe() {
		return Task.runUnsafeActionCore(action, name, mode, timeoutOrDefault());
	}

	/**
	 * 注意: Unsafe 在事务中也会立即异步执行，即使之后该事务 redo 或 rollback 也无法撤销。等价
	 * {@link Task#executeUnsafe(Action0, String, DispatchMode, long)}。
	 */
	public void executeUnsafe() {
		Task.executeUnsafeActionCore(action, name, mode, timeoutOrDefault());
	}

	/**
	 * 事务感知的延迟调度(毫秒)。等价 {@link Task#schedule(long, Action0, long)}。忽略 mode。
	 */
	public void schedule(long delay) {
		Task.runTxnAware(null, () -> Task.scheduleActionCore(delay, action, timeoutOrDefault()));
	}

	/**
	 * 延迟调度(毫秒)，不走事务检查。等价 {@link Task#scheduleUnsafe(long, Action0, long)}。忽略 mode。
	 */
	public @NotNull ScheduledFuture<?> scheduleUnsafe(long delay) {
		return Task.scheduleActionCore(delay, action, timeoutOrDefault());
	}

	/**
	 * 事务感知的固定延迟周期调度(毫秒)。等价 {@link Task#schedule(long, long, Action0, long)}。忽略 mode。
	 */
	public void scheduleWithPeriod(long delay, long period) {
		Task.runTxnAware(null, () -> Task.schedulePeriodCore(delay, period, action, timeoutOrDefault()));
	}

	/**
	 * 固定延迟周期调度(毫秒)，不走事务检查。等价
	 * {@link Task#scheduleUnsafe(long, long, Action0, long)}。忽略 mode。
	 */
	public @NotNull TimerFuture<?> scheduleWithPeriodUnsafe(long delay, long period) {
		return Task.schedulePeriodCore(delay, period, action, timeoutOrDefault());
	}

	/**
	 * 事务感知的每天 hour:minute 调度，需周期时先用 {@link #period} 设置（&gt;0）。等价
	 * {@link Task#scheduleAt(int, int, long, Action0, long)}。忽略 mode。
	 */
	public void scheduleAt(int hour, int minute) {
		Task.runTxnAware(null, () -> Task.scheduleAtCore(hour, minute, period, action, timeoutOrDefault()));
	}

	/**
	 * 每天 hour:minute 调度，不走事务检查，需周期时先用 {@link #period} 设置（&gt;0）。等价
	 * {@link Task#scheduleAtUnsafe(int, int, long, Action0, long)}。忽略 mode。
	 */
	public @NotNull ScheduledFuture<?> scheduleAtUnsafe(int hour, int minute) {
		return Task.scheduleAtCore(hour, minute, period, action, timeoutOrDefault());
	}
}
