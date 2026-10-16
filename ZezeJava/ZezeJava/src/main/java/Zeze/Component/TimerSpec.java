package Zeze.Component;


/**
 * 定时器调度参数的统一描述。
 * 通过静态工厂构造具体参数：ofDelay=simple定时器, ofCron/ofMonth/ofWeek/ofDay=cron定时器。
 */
public sealed interface TimerSpec permits SimpleTimerSpec, CronTimerSpec {
	/**
	 * @param delay 首次触发延迟(毫秒), 不能小于0, 0表示立即触发
	 */
	static SimpleTimerSpec ofDelay(long delay) {
		return new SimpleTimerSpec(delay);
	}

	/**
	 * 直接使用cron表达式构造。
	 *
	 * @param cronExpression cron表达式, 不能为空
	 */
	static CronTimerSpec ofCron(String cronExpression) {
		return new CronTimerSpec(cronExpression);
	}

	/**
	 * 每月第monthDay天的hour:minute:second触发。
	 */
	static CronTimerSpec ofMonth(int monthDay, int hour, int minute, int second) {
		return new CronTimerSpec(second + " " + minute + " " + hour + " " + monthDay + " * ?");
	}

	/**
	 * 每周第weekDay天的hour:minute:second触发。
	 */
	static CronTimerSpec ofWeek(int weekDay, int hour, int minute, int second) {
		return new CronTimerSpec(second + " " + minute + " " + hour + " * * " + weekDay);
	}

	/**
	 * 每天的hour:minute:second触发。
	 */
	static CronTimerSpec ofDay(int hour, int minute, int second) {
		return new CronTimerSpec(second + " " + minute + " " + hour + " * * ?");
	}
}
