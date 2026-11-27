package Zeze.History;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Util.DaemonTimer;
import Zeze.Util.Id128;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 已发 gid 的落库对账账本（按 Application 隔离）：取号即登记，tHistory 行持久化提交后由 {@link History#onPersisted()} 核销。
 * 后台按单调时钟周期巡检，超龄未核销的 gid 记 error 告警一次并出账，供运维排查历史缺口。
 */
public final class PendingGidLedger {
	private static final Logger logger = LogManager.getLogger(PendingGidLedger.class);

	private final String owner; // 诊断归属（Application 名）：告警与守护线程名
	private final ConcurrentHashMap<Id128, Long> pendingCommitGids = new ConcurrentHashMap<>();
	private final AtomicLong pendingAlertedCount = new AtomicLong();
	// 超龄阈值须覆盖 Checkpoint 周期重试的常态抖动，量级对齐消费端 holeGraceMs。
	static final long PENDING_ALERT_MILLIS = 10 * 60 * 1000;
	private final DaemonTimer sweepDaemon;
	private final AtomicBoolean sweepStarted = new AtomicBoolean();

	public PendingGidLedger(String owner) {
		this.owner = owner;
		sweepDaemon = new DaemonTimer("HistoryPendingGidSweep@" + owner, 60_000, this::sweepDaemonBody);
	}

	/** 登记已分配的 gid（atNanos 为 System.nanoTime）并惰性启动对账守护。 */
	public void register(Id128 gid, long atNanos) {
		pendingCommitGids.put(gid, atNanos);
		if (sweepStarted.compareAndSet(false, true))
			sweepDaemon.start();
	}

	/** tHistory 行提交成功后核销该 gid 的登记。 */
	void retire(Id128 gid) {
		pendingCommitGids.remove(gid);
	}

	/** 幂等停止对账守护，并复位启动标记以支持后续重新拉起。 */
	public void stop() {
		sweepDaemon.stop();
		sweepStarted.set(false);
	}

	/**
	 * 巡检并告警超龄未核销的 gid（每 gid 告警一次后出账）。
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
