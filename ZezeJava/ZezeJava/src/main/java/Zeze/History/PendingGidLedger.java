package Zeze.History;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Util.DaemonTimer;
import Zeze.Util.Id128;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * 已发 gid 的落库对账账本，每个 Application 一个实例：不同 history 发号名的 gid 数值空间
 * 重叠，进程级共享账本会跨 app 互相覆盖/误核销。gid 在 Transaction.finalCommit 的
 * beforeApply 取号即登记（登记先于数据应用——取号失败=干净失败不入账，成功后任何失败
 * 都留下"已发号未核销"痕迹），tHistory 行随数据库事务提交成功由 History.commitDone 核销；
 * 登记后超龄未核销 = 数据已应用而历史行未落库的确定性缺口，周期对账将其显式化为 error
 * 告警（每 gid 一次），不自动修复。
 * 覆盖边界：进程重启即失——重启窗口的缺口靠消费端空洞老化+离线 Verify 兜底。
 * 账龄测量用单调钟 System.nanoTime（对齐消费端空洞老化的同款判据），墙钟跳变不进入判据
 * ——前跳不得假告警并提前出账，回拨不得推迟真缺口告警。进程内不持久化，nanoTime 原点
 * 跨进程差异不可达。
 */
public final class PendingGidLedger {
	private static final @NotNull Logger logger = LogManager.getLogger(PendingGidLedger.class);

	private final @NotNull String owner; // 诊断归属（Application 名）：告警与守护线程名
	private final @NotNull ConcurrentHashMap<Id128, Long> pendingCommitGids = new ConcurrentHashMap<>();
	private final @NotNull AtomicLong pendingAlertedCount = new AtomicLong();
	// 超龄阈值须覆盖 Checkpoint 周期重试的常态抖动，量级对齐消费端 holeGraceMs。
	static final long PENDING_ALERT_MILLIS = 10 * 60 * 1000;
	private final @NotNull DaemonTimer sweepDaemon;
	private final @NotNull AtomicBoolean sweepStarted = new AtomicBoolean();

	public PendingGidLedger(@NotNull String owner) {
		this.owner = owner;
		sweepDaemon = new DaemonTimer("HistoryPendingGidSweep@" + owner, 60_000, this::sweepDaemonBody);
	}

	/** gid 已消费、绑定历史数据进入（或即将进入）落库流水线——入账并惰性启动对账。
	 * 调用点：Transaction.finalCommit 的 beforeApply 取号成功即入账（先于数据应用，
	 * 失败路径不产生无 gid 的登记）。atNanos 为单调钟读数（System.nanoTime），
	 * 与 sweep 的判龄钟同基。 */
	public void register(@NotNull Id128 gid, long atNanos) {
		pendingCommitGids.put(gid, atNanos);
		if (sweepStarted.compareAndSet(false, true))
			sweepDaemon.start();
	}

	/** tHistory 行提交成功（History.commitDone）核销该 gid 的登记。 */
	void retire(@NotNull Id128 gid) {
		pendingCommitGids.remove(gid);
	}

	/**
	 * 停止对账守护（Application.stop 终检点之后收编）：幂等。复位 sweepStarted，
	 * 使再次 start 后的首个登记重新拉起守护。
	 */
	public void stop() {
		sweepDaemon.stop();
		sweepStarted.set(false);
	}

	/**
	 * 周期对账（包内可见供测试注入时钟）：超龄未核销的 gid 显式 error 告警（每 gid 一次）
	 * 并出账（防重复告警与无界增长，累计计数保留总量）。告警是运维触发对账/离线 Verify
	 * 的信号，不自动修复——修复动作依赖缺口成因人工判定。判龄为单调钟差（nowNanos 与
	 * register 的 atNanos 同基，阈值 ms→ns 换算收在比较点）。
	 *
	 * @return 本轮告警的 gid 数
	 */
	int sweep(long nowNanos) {
		var alerted = new int[1];
		pendingCommitGids.entrySet().removeIf(e -> {
			if (nowNanos - e.getValue() < PENDING_ALERT_MILLIS * 1_000_000L)
				return false;
			logger.error("history gap detected[{}]: gid {} issued but tHistory row not confirmed committed"
					+ " for over {}ms -- replay diverged from business db, run offline Verify to assess",
					owner, e.getKey(), PENDING_ALERT_MILLIS);
			pendingAlertedCount.incrementAndGet();
			alerted[0]++;
			return true;
		});
		return alerted[0];
	}

	private void sweepDaemonBody() {
		sweep(System.nanoTime());
	}
}
