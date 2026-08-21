package Zeze.Component;

import Zeze.Builtin.Timer.BSimpleTimer;
import org.jetbrains.annotations.NotNull;

/**
 * BSimpleTimer 的调度参数描述，同时持有 BSimpleTimer 的运行时推进逻辑（下次触发时间计算）。
 * 生成的 bean 类无法持有手写逻辑，本类是其唯一的手写伴生类。
 * 本类只是调度参数的描述，build() 时才捕获当前时间并生成真正可调度的 bean。
 * 注意：不要缓存复用 build() 的产物，每次调度都应重新 build（schedule 入口内部自动完成）。
 *
 * <pre>
 * var timerId = timer.schedule(TimerSpec.ofDelay(1000)
 *         .period(5000).times(-1), MyHandle.class, customData);
 * </pre>
 */
public final class SimpleTimerSpec implements TimerSpec {
	private final long delay;
	private long period = -1;
	private long times = -1;
	private long endTime = -1;
	private int missfirePolicy = Timer.eMissfirePolicyNothing;
	private @NotNull String oneByOneKey = "";

	SimpleTimerSpec(long delay) {
		if (delay < 0)
			throw new IllegalArgumentException("delay(" + delay + ") < 0");
		this.delay = delay;
	}

	public long getDelay() {
		return delay;
	}

	public long getPeriod() {
		return period;
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
	 * @param period 触发周期(毫秒), 只有大于0才会周期触发, 默认-1表示只触发一次
	 */
	public @NotNull SimpleTimerSpec period(long period) {
		this.period = period;
		return this;
	}

	/**
	 * @param times 限制触发次数, -1表示不限次数, 不允许0
	 */
	public @NotNull SimpleTimerSpec times(long times) {
		if (times == 0)
			throw new IllegalArgumentException("times must not be 0");
		this.times = times;
		return this;
	}

	/**
	 * @param endTime 限制触发的最后时间(unix毫秒时间戳), 只有大于0会限制
	 */
	public @NotNull SimpleTimerSpec endTime(long endTime) {
		this.endTime = endTime;
		return this;
	}

	/**
	 * @param missfirePolicy 错过指定触发时间的处理方式, 见Timer模块定义的eMissfirePolicy开头枚举
	 */
	public @NotNull SimpleTimerSpec missfirePolicy(int missfirePolicy) {
		this.missfirePolicy = missfirePolicy;
		return this;
	}

	public @NotNull SimpleTimerSpec oneByOneKey(@NotNull String oneByOneKey) {
		this.oneByOneKey = oneByOneKey;
		return this;
	}

	/**
	 * 生成可调度的bean。调用时捕获当前时间, 产物不要缓存复用。
	 */
	public @NotNull BSimpleTimer build() {
		var now = System.currentTimeMillis();
		if (delay > Long.MAX_VALUE - now)
			throw new IllegalArgumentException("delay overflow.");

		var simpleTimer = new BSimpleTimer();
		simpleTimer.setPeriod(period);
		simpleTimer.setRemainTimes(times);
		simpleTimer.setStartTime(now);
		simpleTimer.setEndTime(endTime);
		simpleTimer.setNextExpectedTime(now + delay);
		simpleTimer.setOneByOneKey(oneByOneKey);
		simpleTimer.setMissfirePolicy(missfirePolicy);
		return simpleTimer;
	}

	public static void beforeCallSimpleTimer(@NotNull BSimpleTimer simpleTimer, boolean missfire) {
		var nextExpectedTime = simpleTimer.getNextExpectedTime();
		simpleTimer.setExpectedTime(nextExpectedTime);
		simpleTimer.setHappenTimes(simpleTimer.getHappenTimes() + 1);
		long now = System.currentTimeMillis();
		simpleTimer.setHappenTime(now);

		var remainTimes = simpleTimer.getRemainTimes();
		if (remainTimes >= 0) {
			if (remainTimes > 0)
				simpleTimer.setRemainTimes(--remainTimes);
			if (remainTimes == 0)
				nextExpectedTime = 0;
		}
		if (nextExpectedTime != 0) {
			var period = simpleTimer.getPeriod();
			if (period <= 0)
				nextExpectedTime = 0;
			else {
				var endTime = simpleTimer.getEndTime();
				if (endTime > 0 && endTime < nextExpectedTime)
					nextExpectedTime = 0;
				else {
					if (missfire && simpleTimer.getMissfirePolicy() == AbstractTimer.eMissfirePolicyRunOnce) {
						// 这种策略重置时间，定时器将在新的开始时间之后按原来的间隔执行。
						// simpleTimer.setStartTime(now);
						nextExpectedTime = now + period;
					} else { // eMissfirePolicyRunOnceOldNext
						nextExpectedTime += period;
						if (missfire && nextExpectedTime <= now) {
							// OldNext只补触发一次：保持定点对齐，直接跳到未来最近的定点，避免追赶式连发。
							var step = (now - nextExpectedTime) / period + 1;
							nextExpectedTime += step * period;
						}
					}
				}
			}
		}
		simpleTimer.setNextExpectedTime(nextExpectedTime);
	}
}
