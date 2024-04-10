package Zeze.Util;

// 改进的非标准雪花ID分配算法. 跟标准算法区别在不限制获取倒退的时间戳,也不在序列号超范围时等待,可能出现生成ID中的时间戳比当前值稍大
// 64位ID格式: 0(1b) | timestampOffset(42b) | serverId(10b) | sequence(12b)
// 缺点: 进程停止后的时间回拨可能导致分配重复的ID. 通常生产用途的服务器应该保证一定的时间准确性,不太可能出现回拨. 解决办法只能靠本地或数据库做点持久化
public final class SnowflakeId extends FastLock {
	private static final long TIMESTAMP_BASE = 1693180800000L; // new Date("28 Aug 2023 00:00:00 UTC").getTime(); // 本类首次提交的日期
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

	public void setServerId(long serverId) {
		lock();
		try {
			if (serverId < 0 || serverId >= SERVERID_MAX)
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
