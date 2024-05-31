package Zeze.Util;

import org.jetbrains.annotations.NotNull;

public class TimeAdaptedFund {
	// config
	private final int fundMin;
	private final int doublyMs;
	private final int halveMs;

	private int fund;
	private long lastTime = System.currentTimeMillis();

	public TimeAdaptedFund(int fundMin, int doublyMs, int halveMs) {
		this.fundMin = fundMin;
		this.doublyMs = doublyMs;
		this.halveMs = halveMs;
		this.fund = fundMin;
	}

	/**
	 * 返回默认配置的实例。
	 * 可能增加配置能力。使用Properties？
	 *
	 * @return 一个新的实例
	 */
	public static @NotNull TimeAdaptedFund getDefaultFund() {
		return new TimeAdaptedFund(16, 30_000, 120_000);
	}

	public int next() {
		var now = System.currentTimeMillis();
		var diff = now - lastTime;
		lastTime = now;
		var newFund = fund;
		if (diff < doublyMs && newFund <= Integer.MAX_VALUE / 2)
			fund = newFund <<= 1; // 倍增
		else if (diff > halveMs)
			fund = newFund = Math.max(newFund >> 1, fundMin); // 减半
		return newFund;
	}

	public int get() {
		return fund;
	}
}
