package Zeze.Component;

import java.text.ParseException;
import java.util.Date;
import Zeze.Builtin.Timer.BCronTimer;
import org.apache.logging.log4j.core.util.CronExpression;
import org.jetbrains.annotations.NotNull;

/**
 * BCronTimer 的参数构造器，同时持有 BCronTimer 的运行时推进逻辑（cron 解析与下次触发时间计算）。
 * 生成的 bean 类无法持有手写逻辑，本类是其唯一的手写伴生类。
 * 本类只是调度参数的描述，build() 时才捕获当前时间并生成真正可调度的 bean。
 * 注意：不要缓存复用 build() 的产物，每次调度都应重新 build（schedule 入口内部自动完成）。
 *
 * <pre>
 * var timerId = timer.schedule(BCronTimerBuilder.ofCron("0 0 4 * * ?")
 *         .times(-1), MyHandle.class, customData);
 * </pre>
 */
public final class BCronTimerBuilder {
	private final @NotNull String cronExpression;
	private long times = -1;
	private long endTime = -1;
	private int missfirePolicy = Timer.eMissfirePolicyNothing;
	private @NotNull String oneByOneKey = "";

	private BCronTimerBuilder(@NotNull String cronExpression) {
		if (cronExpression.isEmpty())
			throw new IllegalArgumentException("cronExpression is empty");
		this.cronExpression = cronExpression;
	}

	/**
	 * 直接使用cron表达式构造。
	 *
	 * @param cronExpression cron表达式, 不能为空
	 */
	public static @NotNull BCronTimerBuilder ofCron(@NotNull String cronExpression) {
		return new BCronTimerBuilder(cronExpression);
	}

	/**
	 * 每月第monthDay天的hour:minute:second触发。
	 */
	public static @NotNull BCronTimerBuilder ofMonth(int monthDay, int hour, int minute, int second) {
		return new BCronTimerBuilder(second + " " + minute + " " + hour + " " + monthDay + " * ?");
	}

	/**
	 * 每周第weekDay天的hour:minute:second触发。
	 */
	public static @NotNull BCronTimerBuilder ofWeek(int weekDay, int hour, int minute, int second) {
		return new BCronTimerBuilder(second + " " + minute + " " + hour + " * * " + weekDay);
	}

	/**
	 * 每天的hour:minute:second触发。
	 */
	public static @NotNull BCronTimerBuilder ofDay(int hour, int minute, int second) {
		return new BCronTimerBuilder(second + " " + minute + " " + hour + " * * ?");
	}

	public @NotNull String getCronExpression() {
		return cronExpression;
	}

	public long getTimes() {
		return times;
	}

	public long getEndTime() {
		return endTime;
	}

	public int getMissfirePolicy() {
		return missfirePolicy;
	}

	public @NotNull String getOneByOneKey() {
		return oneByOneKey;
	}

	/**
	 * @param times 限制触发次数, -1表示不限次数, 不允许0
	 */
	public @NotNull BCronTimerBuilder times(long times) {
		if (times == 0)
			throw new IllegalArgumentException("times must not be 0");
		this.times = times;
		return this;
	}

	/**
	 * @param endTime 限制触发的最后时间(unix毫秒时间戳), 只有大于0会限制
	 */
	public @NotNull BCronTimerBuilder endTime(long endTime) {
		this.endTime = endTime;
		return this;
	}

	/**
	 * @param missfirePolicy 错过指定触发时间的处理方式, 见Timer模块定义的eMissfirePolicy开头枚举
	 */
	public @NotNull BCronTimerBuilder missfirePolicy(int missfirePolicy) {
		this.missfirePolicy = missfirePolicy;
		return this;
	}

	public @NotNull BCronTimerBuilder oneByOneKey(@NotNull String oneByOneKey) {
		this.oneByOneKey = oneByOneKey;
		return this;
	}

	/**
	 * 生成可调度的bean。调用时捕获当前时间, 产物不要缓存复用。
	 *
	 * @throws ParseException cron表达式解析异常
	 */
	public @NotNull BCronTimer build() throws ParseException {
		var cronTimer = new BCronTimer();
		cronTimer.setCronExpression(cronExpression);
		cronTimer.setNextExpectedTime(cronNextTime(cronExpression, System.currentTimeMillis()));
		cronTimer.setRemainTimes(times);
		cronTimer.setEndTime(endTime);
		cronTimer.setOneByOneKey(oneByOneKey);
		cronTimer.setMissfirePolicy(missfirePolicy);
		return cronTimer;
	}

	public boolean matches(@NotNull BCronTimer cronTimer) {
		return cronTimer.getCronExpression().equals(cronExpression)
				&& cronTimer.getRemainTimes() == times
				&& cronTimer.getEndTime() == endTime
				&& cronTimer.getMissfirePolicy() == missfirePolicy
				&& cronTimer.getOneByOneKey().equals(oneByOneKey);
	}

	public static long cronNextTime(@NotNull String cron, long time) throws ParseException {
		var cronExpression = new CronExpression(cron);
		return cronExpression.getNextValidTimeAfter(new Date(time)).getTime();
	}

	public static boolean nextCronTimer(@NotNull BCronTimer cronTimer, boolean missfire) throws ParseException {
		// check remain times
		var remainTimes = cronTimer.getRemainTimes();
		if (remainTimes >= 0) {
			if (remainTimes > 0)
				cronTimer.setRemainTimes(--remainTimes);
			if (remainTimes == 0)
				return false;
		}

		var nextExpectedTime = cronTimer.getNextExpectedTime();
		cronTimer.setExpectedTime(nextExpectedTime);
		cronTimer.setHappenTimes(cronTimer.getHappenTimes() + 1);
		var now = System.currentTimeMillis();
		cronTimer.setHappenTime(now);

		long baseTime;
		if (missfire && cronTimer.getMissfirePolicy() == AbstractTimer.eMissfirePolicyRunOnce) {
			// 这种策略重置时间，定时器将在新的开始时间之后按原来的间隔执行。
			// cronTimer.setStartTime(now);
			baseTime = now;
		} else
			baseTime = nextExpectedTime;
		nextExpectedTime = cronNextTime(cronTimer.getCronExpression(), baseTime);
		cronTimer.setNextExpectedTime(nextExpectedTime);

		// check endTime
		var endTime = cronTimer.getEndTime();
		return endTime <= 0 || nextExpectedTime <= endTime;
	}
}
