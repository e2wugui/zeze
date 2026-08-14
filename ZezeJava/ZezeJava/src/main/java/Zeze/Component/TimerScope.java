package Zeze.Component;

import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 某个身份(角色/账号)下某一类定时器的统一入口。
 * 通过 {@link Timer#roles(long)}/{@link Timer#accounts(String, String)} 获取门面后，
 * 再用 online()/onlineHot()/offline() 选择本接口的具体实现。
 */
public interface TimerScope {
	/**
	 * 调度一个定时器。
	 * 需要在事务内调用。
	 *
	 * @param spec        定时器调度参数
	 * @param handleClass 回调class
	 * @return 自动生成的timerId
	 */
	default @NotNull String schedule(@NotNull TimerSpec spec, @NotNull Class<? extends TimerHandle> handleClass) {
		return schedule(spec, handleClass, null);
	}

	/**
	 * 调度一个定时器。
	 * 需要在事务内调用。
	 *
	 * @param spec        定时器调度参数
	 * @param handleClass 回调class
	 * @param customData  自定义数据
	 * @return 自动生成的timerId
	 */
	@NotNull String schedule(@NotNull TimerSpec spec, @NotNull Class<? extends TimerHandle> handleClass,
							 @Nullable Bean customData);

	/**
	 * 调度一个命名定时器，如果同名timer无法调度则返回false。
	 * 各实现语义不同：online定时器是同名已占用返回false，全局Timer是同名在本server会先取消再重建。
	 * 需要在事务内调用。
	 *
	 * @param timerId     名字
	 * @param spec        定时器调度参数
	 * @param handleClass 回调class
	 * @return 调度是否成功
	 */
	default boolean scheduleNamed(@NotNull String timerId, @NotNull TimerSpec spec,
								  @NotNull Class<? extends TimerHandle> handleClass) {
		return scheduleNamed(timerId, spec, handleClass, null);
	}

	/**
	 * 调度一个命名定时器，如果同名timer无法调度则返回false。
	 * 各实现语义不同：online定时器是同名已占用返回false，全局Timer是同名在本server会先取消再重建。
	 * 需要在事务内调用。
	 *
	 * @param timerId     名字
	 * @param spec        定时器调度参数
	 * @param handleClass 回调class
	 * @param customData  自定义数据
	 * @return 调度是否成功
	 */
	boolean scheduleNamed(@NotNull String timerId, @NotNull TimerSpec spec,
						  @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData);

	/**
	 * 取消定时器，取消不存在的timer认为成功，返回是否真的取消了已存在的timer。
	 *
	 * @return 是否真的取消了已存在的timer
	 */
	boolean cancel(@Nullable String timerId);
}
