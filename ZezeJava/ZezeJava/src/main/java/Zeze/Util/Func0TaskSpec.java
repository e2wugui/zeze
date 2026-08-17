package Zeze.Util;

import java.util.concurrent.Future;
import org.jetbrains.annotations.NotNull;

/**
 * 带返回值延迟任务的提交参数，通过 {@link TaskSpec#ofFunc0} 构造。
 * 返回值与异常都经 Future 传播（区别于 ofAction 的异常吞掉只打日志）。
 *
 * <pre>
 * var future = TaskSpec.&lt;Long&gt;ofFunc0(() -> TriggerTimerLocal(serverId, timerId, nodeId, name))
 *         .scheduleUnsafe(delay);
 * </pre>
 */
public final class Func0TaskSpec<R> extends AbstractTaskSpec implements TaskSpec {
	private final @NotNull Func0<R> func;

	Func0TaskSpec(@NotNull Func0<R> func) {
		this.func = func;
	}

	/**
	 * @param timeout 任务超时(毫秒)，不设置或小于0时，终结方法执行时取 Task.defaultTimeout
	 */
	public @NotNull Func0TaskSpec<R> timeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * 延迟调度(毫秒)，返回值与异常经 Future 传播。等价
	 * {@link Task#scheduleUnsafe(long, Func0, long)}。
	 */
	public @NotNull Future<R> scheduleUnsafe(long delay) {
		return Task.scheduleFunc0Core(delay, func, timeoutOrDefault());
	}
}
