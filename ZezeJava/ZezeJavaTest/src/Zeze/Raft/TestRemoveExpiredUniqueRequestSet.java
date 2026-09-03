package Zeze.Raft;

import java.util.Date;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * LogSequence.removeExpiredUniqueRequestSet 的表名过滤与日期解析（FND-R1-1）。
 * 旧实现按前缀"unique."匹配待清理表，但实际表名是"&lt;raftName&gt;.unique.&lt;yyyy&gt;.&lt;M&gt;.&lt;d&gt;"，
 * 永不匹配：unique请求存根列族与uniqueRequestSets内存map随运行天数无界增长。
 * 起真实Raft做端到端清理需要3节点网络选举+20秒低精度定时器触发，属integration量级；
 * 这里直接单测提取出的表名生成/解析纯函数（makeUniqueRequestTableName /
 * parseUniqueRequestDate / toUniqueRequestKey），覆盖"只清理本raft的过期表"的全部判定逻辑。
 */
@Fast
public class TestRemoveExpiredUniqueRequestSet {

	private static final String raftName = "127.0.0.1_5556";

	// 建表（openUniqueRequests按请求createTime当天的key生成表名）与清理（按表名解析日期）
	// 必须往返一致：解析出的日期重新算key与建表时相同，removeExpiredUniqueRequestSet才能
	// 用该key从uniqueRequestSets移除已打开的Table（否则内存map残留已drop的句柄）。
	@SuppressWarnings("deprecation")
	@Test
	public void testTableNameRoundTrip() {
		// 一天中间的任意时刻。
		var createTime = new Date(2026 - 1900, 9 - 1, 3, 15, 30, 45);
		var key = LogSequence.toUniqueRequestKey(createTime);
		var tableName = LogSequence.makeUniqueRequestTableName(raftName, key);
		// 实际表名带raft名前缀、月日不补零。
		Assertions.assertEquals("127.0.0.1_5556.unique.2026.9.3", tableName);

		var date = LogSequence.parseUniqueRequestDate(raftName, tableName);
		Assertions.assertNotNull(date);
		Assertions.assertEquals(key, LogSequence.toUniqueRequestKey(date));
		Assertions.assertEquals(new Date(2026 - 1900, 9 - 1, 3), date); // 解析为当天0点，过期判断按天粒度

		// 跨多天：每天一张表，key与解析逐日一致。
		for (int d = 1; d <= 10; d++) {
			var time = new Date(2026 - 1900, 9 - 1, d, 23, 59);
			var k = LogSequence.toUniqueRequestKey(time);
			var parsed = LogSequence.parseUniqueRequestDate(raftName,
					LogSequence.makeUniqueRequestTableName(raftName, k));
			Assertions.assertNotNull(parsed);
			Assertions.assertEquals(k, LogSequence.toUniqueRequestKey(parsed));
		}
	}

	// 不是本raft的unique表（或表名非法）必须返回null而不是抛异常：
	// 多raft共享同一database时只清理本raft的表，其他raft的表由其自己的任务清理。
	@Test
	public void testRejectsForeignAndInvalidTableNames() {
		// 旧代码匹配的前缀（真实表名从不存在的形态）。
		Assertions.assertNull(LogSequence.parseUniqueRequestDate(raftName, "unique.2026.9.3"));
		// 本raft的非unique表。
		Assertions.assertNull(LogSequence.parseUniqueRequestDate(raftName, raftName + ".logs"));
		Assertions.assertNull(LogSequence.parseUniqueRequestDate(raftName, raftName + ".rafts"));
		// 共享database下其他raft的unique表（即使已过期）也不能动。
		Assertions.assertNull(LogSequence.parseUniqueRequestDate(raftName, "otherRaft.unique.2026.9.3"));
		Assertions.assertNull(LogSequence.parseUniqueRequestDate(raftName, "127.0.0.1_5557.unique.2026.9.3"));
		// 其他raft的名字本身包含".unique."时同样不能误认。
		Assertions.assertNull(LogSequence.parseUniqueRequestDate("a", "a.unique.2026.9.3.unique.2026.9.3"));
		// 非法/不完整日期。
		Assertions.assertNull(LogSequence.parseUniqueRequestDate(raftName, raftName + ".unique.2026.13.1"));
		Assertions.assertNull(LogSequence.parseUniqueRequestDate(raftName, raftName + ".unique.2026.2.30"));
		Assertions.assertNull(LogSequence.parseUniqueRequestDate(raftName, raftName + ".unique.2026.9"));
		Assertions.assertNull(LogSequence.parseUniqueRequestDate(raftName, raftName + ".unique.2026.9.3.4"));
		Assertions.assertNull(LogSequence.parseUniqueRequestDate(raftName, raftName + ".unique.abc"));
	}
}
