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
 * 已发 gid 的落库对账账本（FND29 history-02 缺口显式化；FND30 history-02 下沉实例维度）。
 * 每个 Application 一个实例（构造期创建、final、停机不置 null）：不同 history 发号名的
 * 号段计数器各自从 (0,0) 起步、gid 数值大量重叠，进程级共享账本（static + 裸 Id128 键）
 * 下后注册者 put 覆盖前者登记、前者 commitDone 的 remove 误删后者登记——缺口告警面被
 * 跨 app 误核销静默击穿。实例隔离后，同名多 app（共享同一发号计数器，gid 全局唯一）
 * 天然不回归。
 *
 * gid 在 History.buildLogChanges 消费即登记；tHistory 行随数据库事务提交成功由
 * History.commitDone 核销。登记后超龄未核销 = 数据已应用而历史行未落库的确定性缺口
 * （Immediately 补刷丢历史、Table 反复 flush 失败、编码失败后 gid 已消费等形态）——
 * 此前键空间连空洞都可能不产生（gid 未入流水线），消费端/Verify 均无从感知。账本把
 * 静默分歧变成显式 error 告警（周期对账，模式 A reconciler）。覆盖边界：进程重启即失
 * ——重启窗口的缺口仍靠消费端空洞老化+离线 Verify 兜底；跨库崩溃窗口（业务库已提交
 * 历史库未提交）由 Checkpoint 域的跨域线索另行跟踪。
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

	/** gid 已消费、绑定历史数据进入（或即将进入）落库流水线——入账并惰性启动对账。 */
	void register(@NotNull Id128 gid, long atMillis) {
		pendingCommitGids.put(gid, atMillis);
		if (sweepStarted.compareAndSet(false, true))
			sweepDaemon.start();
	}

	/** tHistory 行提交成功（History.commitDone）核销该 gid 的登记。 */
	void retire(@NotNull Id128 gid) {
		pendingCommitGids.remove(gid);
	}

	/**
	 * 周期对账（包内可见供测试注入时钟）：超龄未核销的 gid 显式 error 告警（每 gid 一次）
	 * 并出账（防重复告警与无界增长，累计计数保留总量）。告警是运维触发对账/离线 Verify
	 * 的信号，不自动修复——修复动作依赖缺口成因人工判定。
	 *
	 * @return 本轮告警的 gid 数
	 */
	int sweep(long nowMillis) {
		var alerted = new int[1];
		pendingCommitGids.entrySet().removeIf(e -> {
			if (nowMillis - e.getValue() < PENDING_ALERT_MILLIS)
				return false;
			logger.error("history gap detected[{}]: gid {} issued but tHistory row not confirmed committed"
					+ " for over {}ms -- replay diverged from business db, run offline Verify to assess"
					+ " (FND29 history-02)", owner, e.getKey(), PENDING_ALERT_MILLIS);
			pendingAlertedCount.incrementAndGet();
			alerted[0]++;
			return true;
		});
		return alerted[0];
	}

	private void sweepDaemonBody() {
		sweep(System.currentTimeMillis());
	}
}
