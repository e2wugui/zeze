package Zeze.Component;

import Zeze.Builtin.Timer.BTimer;
import org.jetbrains.annotations.NotNull;

/**
 * 定时器回调处理方法. 仅持久化类名,不会持久化成员,不能有状态
 */
public interface TimerHandle {
	/**
	 * 事务内运行, 如果抛出异常, 会自动取消该timer
	 */
	void onTimer(@NotNull TimerContext context) throws Exception;

	/**
	 * 事务内运行, 时机在取消后, 即允许复活该timerId的定时器
	 *
	 * @param timer 刚从数据库中移除的记录, 不能再次复用到数据库中
	 */
	@SuppressWarnings("RedundantThrows")
	default void onTimerCancel(@NotNull BTimer timer) throws Exception {
	}
}
