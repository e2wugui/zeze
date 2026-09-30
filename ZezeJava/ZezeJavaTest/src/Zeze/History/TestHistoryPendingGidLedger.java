package Zeze.History;

import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.Util.Id128;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 已发 gid 的落库对账账本（缺口显式化）：gid 在 buildLogChanges 消费即登记，
 * tHistory 行随数据库事务提交由 commitDone 核销；超龄未核销=数据已应用而历史行未落库的
 * 确定性缺口，周期对账每 gid 告警一次并出账。修复前该形态完全静默（键空间连空洞都可能
 * 不产生，消费端/Verify 均无从感知），回放副本永久分歧。
 * 账龄测量用单调钟（System.nanoTime 基，与消费端空洞老化 ApplyHelper 的同款判据）：
 * 墙钟跳变（NTP 步进/手动调钟/VM 暂停恢复）不得误判账龄——前向跳变把正常落库流水中的
 * gid 假告警"replay diverged"并提前出账（消耗每 gid 一次的告警额度），回拨方向推迟
 * 真缺口的告警。
 * 账本为 Application 实例维度：每个 Application 各用一份 PendingGidLedger——不同发号名的
 * 号段计数器都从 (0,0) 起步、gid 数值大量重叠，进程级共享时登记互相覆盖、
 * commitDone 跨 app 误核销。
 */
@Fast
public class TestHistoryPendingGidLedger {
	private static final long NANO_BASE = System.nanoTime();

	private static long msToNanos(long millis) {
		return millis * 1_000_000L;
	}

	/** 超龄未核销：告警一次并出账（防重复告警与无界增长）。 */
	@Test
	public void testOverdueGidAlertsOnce() {
		var ledger = new PendingGidLedger("app");
		var gid = new Id128(NANO_BASE, 1);
		ledger.register(gid, NANO_BASE - msToNanos(PendingGidLedger.PENDING_ALERT_MILLIS + 60_000));
		Assertions.assertEquals(1, ledger.sweep(NANO_BASE), "超龄未核销必须告警");
		Assertions.assertEquals(0, ledger.sweep(NANO_BASE), "出账后不得重复告警");
	}

	/** 未超龄（覆盖 Checkpoint 周期重试的常态抖动）不告警。 */
	@Test
	public void testFreshGidNotAlerted() {
		var ledger = new PendingGidLedger("app");
		var gid = new Id128(NANO_BASE, 2);
		ledger.register(gid, NANO_BASE - msToNanos(1_000));
		Assertions.assertEquals(0, ledger.sweep(NANO_BASE), "常态抖动窗口内不得告警");
	}

	/** 墙钟前跳不误判：判龄只认单调差——入账后对账钟仅前进 1ms（墙钟此刻前跳 12 分钟
	 * 也与判龄无关，墙钟不进入判据）。修复前 sweep 按墙钟毫秒语义解读裸数值，同样的
	 * 注入被读成约 16.7 分钟账龄：刚入账、tHistory 行仍在正常 flush 流水线中的 gid 被
	 * 假告警 "replay diverged ... run offline Verify" 并提前出账，每 gid 一次的告警
	 * 额度被假阳性消耗。 */
	@Test
	public void testWallClockForwardJumpProducesNoFalseAlert() {
		var ledger = new PendingGidLedger("app");
		var gid = new Id128(NANO_BASE, 3);
		ledger.register(gid, NANO_BASE);
		Assertions.assertEquals(0, ledger.sweep(NANO_BASE + msToNanos(1)),
				"单调账龄 1ms 不得告警（修复前注入值按墙钟毫秒误读，亚秒差即假超龄）");
	}

	/** 墙钟回拨不延迟真缺口告警：单调账龄超阈值即告警，延迟恒为零。修复前判龄是墙钟差，
	 * 回拨使 now-atMillis 骤减甚至为负——真缺口的告警被推迟回拨量（VM 暂停恢复等形态
	 * 可达分钟级以上），直到墙钟追回，账本检测面在该窗口内失效。 */
	@Test
	public void testWallClockBackwardJumpDoesNotDelayTrueGapAlert() {
		var ledger = new PendingGidLedger("app");
		var gid = new Id128(NANO_BASE, 4);
		ledger.register(gid, NANO_BASE - msToNanos(PendingGidLedger.PENDING_ALERT_MILLIS + 60_000));
		Assertions.assertEquals(1, ledger.sweep(NANO_BASE),
				"单调账龄超阈值的真缺口必须告警（判龄对墙钟回拨零敏感）");
	}

	/** tHistory 行提交成功（commitDone 清容器）即核销——正常路径不残留。 */
	@Test
	public void testCommitDoneRetiresGid() {
		var ledger = new PendingGidLedger("app");
		var gid = new Id128(NANO_BASE, 5);
		var data = new BLogChanges.Data();
		data.setGlobalSerialId(gid);
		var history = new History(data);
		ledger.register(gid, NANO_BASE - msToNanos(PendingGidLedger.PENDING_ALERT_MILLIS + 60_000));
		history.commitDone(ledger);
		Assertions.assertEquals(0, ledger.sweep(NANO_BASE), "commitDone 核销后不得告警");
	}

	/** 同 JVM 多 Application 各用不同 history 发号名时，两名计数器都从 (0,0) 起步、
	 * gid 数值大量重叠——一侧 commitDone 的按 gid remove 不得误删另一侧的登记
	 * （误删则该侧 flush 停滞的真实缺口永远扫不出来，告警面被静默击穿）。 */
	@Test
	public void testCrossAppSameNumericGidIsolated() {
		var app1 = new PendingGidLedger("app1");
		var app2 = new PendingGidLedger("app2");
		var gid = new Id128(NANO_BASE, 6);
		// 两个 app 各自登记同一数值 gid（不同发号名，各自独立取号，数值重叠是常态）。
		app1.register(gid, NANO_BASE - msToNanos(PendingGidLedger.PENDING_ALERT_MILLIS + 60_000));
		app2.register(gid, NANO_BASE - msToNanos(PendingGidLedger.PENDING_ALERT_MILLIS + 60_000));
		// app1 侧 tHistory 提交成功，核销自己的登记。
		var data = new BLogChanges.Data();
		data.setGlobalSerialId(gid);
		new History(data).commitDone(app1);
		// app1 已核销干净；app2 的登记必须仍在——超龄对账必须还能扫出它的缺口。
		Assertions.assertEquals(0, app1.sweep(NANO_BASE), "app1 核销自己的登记后不得告警");
		Assertions.assertEquals(1, app2.sweep(NANO_BASE), "app2 的登记不得被 app1 的 commitDone 误删");
	}

	/** stop（Application.stop 收编入口）后首个登记重新拉起守护：对齐 DaemonTimer
	 * "stop 后可再次 start"的重启语义——Application 再次 start 后账本不因上一世
	 * 的 stop 而失去对账守护。守护态经反射读（与 app 级测试同法，不为主流程增设
	 * 只读访问面）。 */
	@Test
	public void testStopThenRegisterRestartsSweepDaemon() throws Exception {
		var ledger = new PendingGidLedger("app");
		var field = PendingGidLedger.class.getDeclaredField("sweepDaemon");
		field.setAccessible(true);
		var daemon = (Zeze.Util.DaemonTimer) field.get(ledger);
		Assertions.assertTrue(daemon.isShutdown(), "前置：构造后未启动（关门态）");
		ledger.register(new Id128(NANO_BASE, 7), NANO_BASE);
		Assertions.assertFalse(daemon.isShutdown(), "首个登记惰性启动守护");
		ledger.stop();
		Assertions.assertTrue(daemon.isShutdown(), "stop 关停守护");
		ledger.stop();
		Assertions.assertTrue(daemon.isShutdown(), "stop 幂等");
		ledger.register(new Id128(NANO_BASE, 8), NANO_BASE);
		Assertions.assertFalse(daemon.isShutdown(), "stop 后首个登记重新拉起守护（重启语义）");
	}
}
