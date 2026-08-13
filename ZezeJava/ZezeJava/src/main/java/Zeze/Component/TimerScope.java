package Zeze.Component;

import java.text.ParseException;
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
	 * 调度一个simple定时器。
	 * 需要在事务内调用。
	 *
	 * @param builder     simple timer参数构造器
	 * @param handleClass 回调class
	 * @param customData  自定义数据
	 * @return 自动生成的timerId
	 */
	@NotNull String schedule(@NotNull BSimpleTimerBuilder builder,
							 @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData);

	/**
	 * 调度一个cron定时器。
	 * 需要在事务内调用。
	 *
	 * @param builder     cron timer参数构造器
	 * @param handleClass 回调class
	 * @param customData  自定义数据
	 * @return 自动生成的timerId
	 * @throws ParseException cron解析异常
	 */
	@NotNull String schedule(@NotNull BCronTimerBuilder builder,
							 @NotNull Class<? extends TimerHandle> handleClass,
							 @Nullable Bean customData) throws ParseException;

	/**
	 * 调度一个命名定时器，如果同名timer已存在则直接返回false。
	 * 需要在事务内调用。
	 *
	 * @param timerId     名字
	 * @param builder     simple timer参数构造器
	 * @param handleClass 回调class
	 * @param customData  自定义数据
	 * @return 调度是否成功
	 */
	boolean scheduleNamed(@NotNull String timerId, @NotNull BSimpleTimerBuilder builder,
						  @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData);

	/**
	 * 调度一个命名定时器，如果同名timer已存在则直接返回false。
	 * 需要在事务内调用。
	 *
	 * @param timerId     名字
	 * @param builder     cron timer参数构造器
	 * @param handleClass 回调class
	 * @param customData  自定义数据
	 * @return 调度是否成功
	 * @throws ParseException cron解析异常
	 */
	boolean scheduleNamed(@NotNull String timerId, @NotNull BCronTimerBuilder builder,
						  @NotNull Class<? extends TimerHandle> handleClass,
						  @Nullable Bean customData) throws ParseException;

	/**
	 * 取消定时器，取消不存在的timer认为成功。
	 *
	 * @return 是否真的取消了已存在的timer
	 */
	boolean cancel(@Nullable String timerId);
}
