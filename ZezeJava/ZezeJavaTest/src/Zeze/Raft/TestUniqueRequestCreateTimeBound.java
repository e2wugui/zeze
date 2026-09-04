package Zeze.Raft;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LogSequence.isUniqueRequestCreateTimeValid 的上下界判定（FND2-R1-3）。
 * 唯一请求去重完全信任客户端 createTime：客户端应用服务器时钟故障（NTP 失准
 * 跳到未来）或恶意直连 raft 端口，携带未来 createTime 的 RaftRpc 会按天创建
 * 存根列族并复制到集群所有节点，而 removeExpiredUniqueRequestSet 的过期判定
 * （当天 0 点 + (N+1) 天）对未来日期永不满足——列族无界增长，拖慢 RocksDB
 * 打开与 compaction。
 * 修复：超过 1 天的未来时间按 RaftExpired 拒绝（判定为纯函数，同一 createTime
 * 的重发每次同样拒绝，确定性不变；拒绝发生在 handle 之前，不产生携带未来
 * createTime 的日志条目）。1 天容忍保留集群内合理的少量时钟偏差，期间可建到
 * 的表随时间自然过期清理；下界（过老）判定保持原有语义不变。
 */
@Fast
public class TestUniqueRequestCreateTimeBound {

	private static final int expiredDays = 3;
	private static final long day = 86400_000L;
	private static final long now = 1_800_000_000_000L; // 固定时钟，只测区间数学

	@Test
	public void testValidRange() {
		// 正常：现在、稍早、时钟回拨方向（create 在过去）。
		assertTrue(LogSequence.isUniqueRequestCreateTimeValid(now, now, expiredDays));
		assertTrue(LogSequence.isUniqueRequestCreateTimeValid(now - 3600_000, now, expiredDays));
		// 合法窗口内最老的时刻（下界沿用原有判定：恰满 expiredDays 天即过期）。
		assertTrue(LogSequence.isUniqueRequestCreateTimeValid(now - (expiredDays - 1) * day, now, expiredDays));
		// 上界含 1 天未来（时钟偏差容忍），含边界。
		assertTrue(LogSequence.isUniqueRequestCreateTimeValid(now + day, now, expiredDays));
	}

	@Test
	public void testRejectsTooOld() {
		// 原有下界语义保持：create 比 now 老满 expiredDays 天即拒绝。
		assertFalse(LogSequence.isUniqueRequestCreateTimeValid(now - expiredDays * day, now, expiredDays));
		assertFalse(LogSequence.isUniqueRequestCreateTimeValid(now - 365 * day, now, expiredDays));
	}

	@Test
	public void testRejectsFarFuture() {
		// 超过 1 天的未来时间拒绝：时钟跳未来数天/数月/数年都关闭建表路径。
		assertFalse(LogSequence.isUniqueRequestCreateTimeValid(now + day + 1, now, expiredDays));
		assertFalse(LogSequence.isUniqueRequestCreateTimeValid(now + 2 * day, now, expiredDays));
		assertFalse(LogSequence.isUniqueRequestCreateTimeValid(now + 30 * day, now, expiredDays));
		assertFalse(LogSequence.isUniqueRequestCreateTimeValid(now + 3650 * day, now, expiredDays));
	}
}
