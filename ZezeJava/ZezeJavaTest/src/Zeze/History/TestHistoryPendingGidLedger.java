package Zeze.History;

import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.Util.Id128;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 已发 gid 的落库对账账本（history-02 缺口显式化）：gid 在 buildLogChanges 消费即登记，
 * tHistory 行随数据库事务提交由 commitDone 核销；超龄未核销=数据已应用而历史行未落库的
 * 确定性缺口，周期对账每 gid 告警一次并出账。修复前该形态完全静默（键空间连空洞都可能
 * 不产生，消费端/Verify 均无从感知），回放副本永久分歧。
 */
@Fast
public class TestHistoryPendingGidLedger {
	private static final long NOW = System.currentTimeMillis();

	/** 超龄未核销：告警一次并出账（防重复告警与无界增长）。 */
	@Test
	public void testOverdueGidAlertsOnce() {
		var gid = new Id128(NOW, 1);
		History.registerPendingCommitGid(gid, NOW - History.PENDING_ALERT_MILLIS - 1);
		Assertions.assertEquals(1, History.sweepPendingCommitGids(NOW), "超龄未核销必须告警");
		Assertions.assertEquals(0, History.sweepPendingCommitGids(NOW), "出账后不得重复告警");
	}

	/** 未超龄（覆盖 Checkpoint 周期重试的常态抖动）不告警。 */
	@Test
	public void testFreshGidNotAlerted() {
		var gid = new Id128(NOW, 2);
		History.registerPendingCommitGid(gid, NOW - 1000);
		Assertions.assertEquals(0, History.sweepPendingCommitGids(NOW), "常态抖动窗口内不得告警");
	}

	/** tHistory 行提交成功（commitDone 清容器）即核销——正常路径不残留。 */
	@Test
	public void testCommitDoneRetiresGid() {
		var gid = new Id128(NOW, 3);
		var data = new BLogChanges.Data();
		data.setGlobalSerialId(gid);
		var history = new History(data);
		History.registerPendingCommitGid(gid, NOW - History.PENDING_ALERT_MILLIS - 1);
		history.commitDone();
		Assertions.assertEquals(0, History.sweepPendingCommitGids(NOW), "commitDone 核销后不得告警");
	}
}
