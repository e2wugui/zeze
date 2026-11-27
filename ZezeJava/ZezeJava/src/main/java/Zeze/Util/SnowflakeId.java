package Zeze.Util;

/**
 * 改进的非标准雪花 ID 生成器：允许时钟微退且序列号溢出时不阻塞等待，直接向未来时间戳借位。
 * 64 位布局：{@code 0(1b) | timestampOffset(41b) | serverId(10b) | sequence(12b)}。
 * 注意：进程重启期间若发生大幅时钟回拨，仍可能产生重复 ID。
 */
public final class SnowflakeId extends FastLock {
	private static final long TIMESTAMP_BASE = 1693180800000L; // 2023-08-28 00:00:00 UTC
	private static final int SERVERID_BITS = 10;
	private static final int SEQUENCE_BITS = 12;
	private static final long SERVERID_MAX = (1L << SERVERID_BITS) - 1;
	private static final long SEQUENCE_MAX = (1L << SEQUENCE_BITS) - 1;
	private static final long TIMESTAMP_MAX = (1L << (63 - SERVERID_BITS - SEQUENCE_BITS)) - 1;

	private long serverId;
	private long lastTimestamp;
	private long sequence;

	public SnowflakeId() {
	}

	public SnowflakeId(long serverId) {
		setServerId(serverId);
	}

	public static long getServerIdMax() {
		return SERVERID_MAX;
	}

	public void setServerId(long serverId) {
		lock();
		try {
			if (serverId < 0 || serverId > SERVERID_MAX)
				throw new IllegalArgumentException("serverId(" + serverId + ") is out of range[0," + SERVERID_MAX + ']');
			if (lastTimestamp != 0 && this.serverId != serverId)
				throw new IllegalArgumentException("can not setServerId(" + serverId + ") after gen()");
			this.serverId = serverId;
		} finally {
			unlock();
		}
	}

	public long gen() {
		lock();
		try {
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
		} finally {
			unlock();
		}
	}
}
