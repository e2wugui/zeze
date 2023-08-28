package Zeze.Util;

// 改进的非标准雪花ID分配算法. 跟标准算法的区别在不限制获取倒退的时间戳,也不在序列号超范围时等待,可能出现生成ID中的时间戳比当前值稍大
// 0(1b) | timestampOffset(42b) | serverId(10b) | sequence(12b)
public final class SnowflakeId {
	private static final long TIMESTAMP_BASE = 1693180800000L; // new Date("28 Aug 2023 00:00:00 UTC").getTime(); // 本类首次提交的日期
	private static final int SERVERID_BITS = 10;
	private static final int SEQUENCE_BITS = 12;
	private static final long SERVERID_MAX = (1L << SERVERID_BITS) - 1;
	private static final long SEQUENCE_MAX = (1L << SEQUENCE_BITS) - 1;
	private static final long TIMESTAMP_MAX = (1L << (63 - SERVERID_BITS - SEQUENCE_BITS)) - 1;

	private static long serverId;
	private static long lastTimestamp;
	private static long sequence;

	public static synchronized void setServerId(long serverId) {
		if (serverId < 0 || serverId >= SERVERID_MAX)
			throw new IllegalArgumentException("serverId(" + serverId + ") is out of range[0," + SERVERID_MAX + ']');
		if (lastTimestamp != 0 && SnowflakeId.serverId != serverId)
			throw new IllegalArgumentException("can not setServerId(" + serverId + ") after gen()");
		SnowflakeId.serverId = serverId;
	}

	public static synchronized long gen() {
		long timestamp = System.currentTimeMillis() - TIMESTAMP_BASE;
		if (timestamp <= lastTimestamp) {
			timestamp = lastTimestamp;
			if (++sequence > SEQUENCE_MAX) {
				lastTimestamp = ++timestamp;
				sequence = 0;
			}
		} else {
			lastTimestamp = timestamp;
			sequence = 0;
		}
		if (timestamp > TIMESTAMP_MAX)
			throw new IllegalStateException("timestamp overflow: " + timestamp);
		return (timestamp << (SERVERID_BITS + SEQUENCE_BITS)) + (serverId << SEQUENCE_BITS) + sequence;
	}

	private SnowflakeId() {
	}
}
