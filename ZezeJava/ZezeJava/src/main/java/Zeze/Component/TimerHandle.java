package Zeze.Component;

import Zeze.Builtin.Timer.BTimer;
import org.jetbrains.annotations.NotNull;

public interface TimerHandle {
	/**
	 * 事务内运行, 如果抛出异常, 会自动取消该timer
	 */
	void onTimer(@NotNull TimerContext context) throws Exception;

	/**
	 * 事务内运行, 时机在取消后, 即允许复活该timerId的定时器
	 */
	default void onTimerCancel(@NotNull BTimer timer) throws Exception {
	}
}
