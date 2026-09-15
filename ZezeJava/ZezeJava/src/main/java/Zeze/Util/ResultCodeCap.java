package Zeze.Util;

/**
 * 每procedure的resultCode基数封顶：超出上限的新码不再单独记录
 * （PerfCounter丢弃到共享废弃累加器，Prometheus归入"other"标签），
 * 防业务自定义错误码无限膨胀series/统计map。上限由
 * -DZezeCounter.maxResultCodes配置，默认20。
 */
public final class ResultCodeCap {
	public static final String OTHER = "other";
	public static final int DEFAULT_MAX = Integer.parseInt(System.getProperty("ZezeCounter.maxResultCodes", "20"));

	private final int max;
	private final LongConcurrentHashMap<Boolean> seen = new LongConcurrentHashMap<>();

	public ResultCodeCap() {
		this(DEFAULT_MAX);
	}

	public ResultCodeCap(int max) {
		this.max = max;
	}

	/** @return true=接受该码；false=超出封顶，由调用方决定丢弃或归other */
	public boolean accept(long code) {
		if (seen.containsKey(code))
			return true;
		if (seen.size() >= max)
			return false;
		seen.putIfAbsent(code, Boolean.TRUE);
		return true;
	}
}
