package Zeze.History;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.Builtin.HistoryModule.BTableKey;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Database;
import Zeze.Util.DaemonTimer;
import Zeze.Util.Id128;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 一个事务关联集合（rrs）的tHistory变更缓冲，两段流水线：
 * logChanges（原始对象）--encode0--&gt; encoded（编码字节）--writeOnly--&gt; tHistory库事务。
 * 不变量：容器只在 rrs 锁内变更；条目离开 logChanges 前必已进 encoded；encoded 只能
 * 由 commitDone 清空——它在数据库事务全部提交成功后由 Checkpoint.flush 调用，失败回滚
 * 后容器保留，重试按系列号幂等重写。
 */
public class History {
	private static final @NotNull Logger logger = LogManager.getLogger(History.class);

	// 为了节约内存，在确实需要的时候才分配。
	// 容器访问已全部在 rrs 锁内，并发Map非必需，改动无收益。
	private volatile @Nullable ConcurrentHashMap<Id128, BLogChanges.Data> logChanges;

	private final ConcurrentHashMap<Id128, Binary> encoded = new ConcurrentHashMap<>();

	// ----- 已发 gid 的落库对账账本（FND29 history-02）-----
	// gid 在 buildLogChanges 消费即登记；tHistory 行随数据库事务提交成功由 commitDone
	// 核销。登记后超龄未核销 = 数据已应用而历史行未落库的确定性缺口（Immediately 补刷
	// 丢历史、Table 反复 flush 失败、编码失败后 gid 已消费等形态）——此前键空间连空洞
	// 都可能不产生（gid 未入流水线），消费端/Verify 均无从感知。账本把静默分歧变成
	// 显式 error 告警（周期对账，模式 A reconciler）。覆盖边界：进程重启即失——重启窗口
	// 的缺口仍靠消费端空洞老化+离线 Verify 兜底；跨库崩溃窗口（业务库已提交历史库未提交）
	// 由 Checkpoint 域的跨域线索另行跟踪。
	private static final @NotNull ConcurrentHashMap<Id128, Long> pendingCommitGids = new ConcurrentHashMap<>();
	private static final @NotNull AtomicLong pendingAlertedCount = new AtomicLong();
	// 超龄阈值须覆盖 Checkpoint 周期重试的常态抖动，量级对齐消费端 holeGraceMs。
	static final long PENDING_ALERT_MILLIS = 10 * 60 * 1000;
	private static final @NotNull DaemonTimer sweepDaemon =
			new DaemonTimer("HistoryPendingGidSweep", 60_000, History::sweepPendingCommitGidsDaemon);
	private static final @NotNull AtomicBoolean sweepStarted = new AtomicBoolean();

	/** gid 已消费、绑定历史数据进入（或即将进入）落库流水线——入账并惰性启动对账。 */
	static void registerPendingCommitGid(@NotNull Id128 gid, long atMillis) {
		pendingCommitGids.put(gid, atMillis);
		if (sweepStarted.compareAndSet(false, true))
			sweepDaemon.start();
	}

	/**
	 * 周期对账（包内可见供测试注入时钟）：超龄未核销的 gid 显式 error 告警（每 gid 一次）
	 * 并出账（防重复告警与无界增长，累计计数保留总量）。告警是运维触发对账/离线 Verify
	 * 的信号，不自动修复——修复动作依赖缺口成因人工判定。
	 *
	 * @return 本轮告警的 gid 数
	 */
	static int sweepPendingCommitGids(long nowMillis) {
		var alerted = new int[1];
		pendingCommitGids.entrySet().removeIf(e -> {
			if (nowMillis - e.getValue() < PENDING_ALERT_MILLIS)
				return false;
			logger.error("history gap detected: gid {} issued but tHistory row not confirmed committed"
					+ " for over {}ms -- replay diverged from business db, run offline Verify to assess"
					+ " (FND29 history-02)", e.getKey(), PENDING_ALERT_MILLIS);
			pendingAlertedCount.incrementAndGet();
			alerted[0]++;
			return true;
		});
		return alerted[0];
	}

	private static void sweepPendingCommitGidsDaemon() {
		sweepPendingCommitGids(System.currentTimeMillis());
	}

	public History(@NotNull BLogChanges.Data firstData) {
		addLogChanges(firstData);
	}

	public void addLogChanges(@NotNull BLogChanges.Data _logChanges) {
		var logChanges = this.logChanges;
		if (logChanges == null)
			this.logChanges = logChanges = new ConcurrentHashMap<>();
		logChanges.put(_logChanges.getGlobalSerialId(), _logChanges);
	}

	public void encode0() {
		// 锁内。只编码不清空：条目离开 logChanges 前必已进 encoded，但 encoded 的清理
		// 由 commitDone 绑定数据库事务提交结果——flush 失败回滚后容器保留，重试幂等重写。
		var changes = logChanges;
		if (changes != null) {
			changes.forEach((key, v) -> {
				// logChanges只要系列号一样，表示内容一样，所以，只要key存在，不需要再encode一次。
				encoded.computeIfAbsent(v.getGlobalSerialId(), __ -> {
					var bb = ByteBuffer.Allocate();
					v.encode(bb);
					return new Binary(bb);
				});
			});
		}
	}

	/** 把 encoded 写入tHistory事务。只写不清空，清理由 commitDone 绑定提交结果。 */
	public void writeOnly(@NotNull Database.Table table, @NotNull Database.Transaction txn) {
		// 但仅仅Checkpoint访问，不需要加锁。现实也在锁内。
		for (var e : encoded.entrySet()) {
			var key = ByteBuffer.Allocate();
			e.getKey().encode(key);
			var value = ByteBuffer.Wrap(e.getValue());
			table.replace(txn, key, value);
		}
	}

	/** 数据库事务全部提交成功后调用：tHistory 行已持久化，容器可以安全清空。
	 * 清空前按 gid 核销对账账本（FND29 history-02）。 */
	public void commitDone() {
		for (var k : encoded.keySet())
			pendingCommitGids.remove(k);
		var changes = logChanges;
		if (changes != null) {
			for (var k : changes.keySet())
				pendingCommitGids.remove(k);
			changes.clear();
		}
		encoded.clear();
	}

	public static void putLogChangesAll(@NotNull ConcurrentHashMap<Id128, BLogChanges.Data> to,
										@NotNull ConcurrentHashMap<Id128, BLogChanges.Data> other) {
		other.forEach((key, v) -> to.putIfAbsent(v.getGlobalSerialId(), v));
	}

	public static void putEncodedAll(@NotNull ConcurrentHashMap<Id128, Binary> to,
									 @NotNull ConcurrentHashMap<Id128, Binary> other) {
		other.forEach(to::putIfAbsent);
	}

	// merge 辅助方法，完整的判断to,from及里面的logChanges的null状况。
	public static @Nullable History merge(@Nullable History to, @Nullable History from) {
		// rrs 锁内
		if (to == null)
			return from; // still maybe null. 直接全部接管。

		// 合并encoded
		if (from != null)
			putEncodedAll(to.encoded, from.encoded);

		// 合并logChanges
		var toLogChanges = to.logChanges;
		if (toLogChanges == null) {
			if (from != null)
				to.logChanges = from.logChanges; // still maybe null
			return to;
		}

		if (from != null) {
			var fromLogChanges = from.logChanges;
			if (fromLogChanges != null)
				putLogChangesAll(toLogChanges, fromLogChanges);
		}
		return to;
	}

	/** globalSerialId 必须在日志应用（finalCommit 的 commit.run）之前解析：取号失败时数据
	 * 未应用、事务干净失败，历史与数据同生共死（调用方 Transaction.finalCommit 的
	 * HistoryChangesCollector.beforeApply）。gid 在此消费即入对账账本（见类注释账本段），
	 * 随 tHistory 行提交成功由 commitDone 核销。 */
	public static @NotNull BLogChanges.Data buildLogChanges(@NotNull Id128 globalSerialId,
															@NotNull Changes changes,
															@Nullable String protocolClassName,
															@Nullable Binary protocolArgument) {
		registerPendingCommitGid(globalSerialId, System.currentTimeMillis());
		var logChanges = new BLogChanges.Data();
		if (protocolClassName != null)
			logChanges.setProtocolClassName(protocolClassName);
		if (protocolArgument != null)
			logChanges.setProtocolArgument(protocolArgument);
		for (var e : changes.getRecords().entrySet()) {
			var value = e.getValue();
			var table = value.getTable();
			if (table != null && !table.isMemory()) { // 内存表的日志变更不需要持久化，直接忽略。
				var key = e.getKey();
				var tableKey = new BTableKey(key.getId(), new Binary(table.encodeKey(key.getKey())));
				var bbValue = ByteBuffer.Allocate();
				value.encode(bbValue);
				logChanges.getChanges().put(tableKey, new Binary(bbValue));
			}
		}
		logChanges.setTimestamp(System.currentTimeMillis());
		logChanges.setGlobalSerialId(globalSerialId);
		return logChanges;
	}
}
