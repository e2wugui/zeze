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
 * 账本为 Application 实例维度（FND30 history-02）：每个 Application 各用一份
 * PendingGidLedger——不同发号名的号段计数器都从 (0,0) 起步、gid 数值大量重叠，
 * 进程级共享时登记互相覆盖、commitDone 跨 app 误核销。
 */
@Fast
public class TestHistoryPendingGidLedger {
	private static final long NOW = System.currentTimeMillis();

	/** 超龄未核销：告警一次并出账（防重复告警与无界增长）。 */
	@Test
	public void testOverdueGidAlertsOnce() {
		var ledger = new PendingGidLedger("app");
		var gid = new Id128(NOW, 1);
		ledger.register(gid, NOW - PendingGidLedger.PENDING_ALERT_MILLIS - 1);
		Assertions.assertEquals(1, ledger.sweep(NOW), "超龄未核销必须告警");
		Assertions.assertEquals(0, ledger.sweep(NOW), "出账后不得重复告警");
	}

	/** 未超龄（覆盖 Checkpoint 周期重试的常态抖动）不告警。 */
	@Test
	public void testFreshGidNotAlerted() {
		var ledger = new PendingGidLedger("app");
		var gid = new Id128(NOW, 2);
		ledger.register(gid, NOW - 1000);
		Assertions.assertEquals(0, ledger.sweep(NOW), "常态抖动窗口内不得告警");
	}

	/** tHistory 行提交成功（commitDone 清容器）即核销——正常路径不残留。 */
	@Test
	public void testCommitDoneRetiresGid() {
		var ledger = new PendingGidLedger("app");
		var gid = new Id128(NOW, 3);
		var data = new BLogChanges.Data();
		data.setGlobalSerialId(gid);
		var history = new History(data);
		ledger.register(gid, NOW - PendingGidLedger.PENDING_ALERT_MILLIS - 1);
		history.commitDone(ledger);
		Assertions.assertEquals(0, ledger.sweep(NOW), "commitDone 核销后不得告警");
	}

	/** FND30 history-02：同 JVM 多 Application 各用不同 history 发号名时，两名计数器都从
	 * (0,0) 起步、gid 数值大量重叠——一侧 commitDone 的按 gid remove 不得误删另一侧的登记
	 * （误删则该侧 flush 停滞的真实缺口永远扫不出来，告警面被静默击穿）。 */
	@Test
	public void testCrossAppSameNumericGidIsolated() {
		var app1 = new PendingGidLedger("app1");
		var app2 = new PendingGidLedger("app2");
		var gid = new Id128(NOW, 4);
		// 两个 app 各自登记同一数值 gid（不同发号名，各自独立取号，数值重叠是常态）。
		app1.register(gid, NOW - PendingGidLedger.PENDING_ALERT_MILLIS - 1);
		app2.register(gid, NOW - PendingGidLedger.PENDING_ALERT_MILLIS - 1);
		// app1 侧 tHistory 提交成功，核销自己的登记。
		var data = new BLogChanges.Data();
		data.setGlobalSerialId(gid);
		new History(data).commitDone(app1);
		// app1 已核销干净；app2 的登记必须仍在——超龄对账必须还能扫出它的缺口。
		Assertions.assertEquals(0, app1.sweep(NOW), "app1 核销自己的登记后不得告警");
		Assertions.assertEquals(1, app2.sweep(NOW), "app2 的登记不得被 app1 的 commitDone 误删");
	}
}
