package Zeze.Util;


/** 时间自适应配额：按消费间隔对配额倍增或减半（用于 PersistentAtomicLong 按块预分配）。 */
public class TimeAdaptedFund {
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
	 *
	 * @return 一个新的实例
	 */
	public static TimeAdaptedFund getDefaultFund() {
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
