package Zeze.Component;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Builtin.Takeover.tTakeoverLease;
import Zeze.Transaction.Procedure;
import Zeze.Util.FuncLong;
import Zeze.Util.LongHashMap;
import Zeze.Util.OutLong;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 接管租约组件：把“死者的数据搬给活人”的裁决权从SM内存降到与数据同库的租约表
 * {@code tTakeoverLease(serverId -> {epoch, expireAt})}，裁决（立碑）与搬运同一zeze事务，
 * 消除旧OfflineNotify机制裁决与搬运之间的残余窗口。
 *
 * <ul>
 * <li>claim：抢占式，{@code epoch = old + 1}，重启不等旧租约过期；安全性由epoch fencing保证；</li>
 * <li>renew：TTL/3周期续约；租约epoch属于别人才=fence失败=致命退出；行丢失（外部清表）自愈重写；</li>
 * <li>release：正常停机写墓碑（expireAt=0），接管者可立即接管；被接管(fenceFatal)时不写；</li>
 * <li>tryTransfer：单事务内【校验租约过期 → 遍历scope搬运 → 立墓碑】，Suspect提示与扫描兜底都汇入这里；</li>
 * <li>scan：周期扫描兜底活性（替代AnnounceServers），启动时立即扫描一次。</li>
 * </ul>
 *
 * <p>mode（Config.TakeoverMode）：
 * <ul>
 * <li>off：完全不启动，checkFence恒通过；</li>
 * <li>dryrun：只做租约簿记（claim/renew/release/scan）+ dry-run日志；不stamp、不fence、不搬运；</li>
 * <li>on：全量接管（stamp/fence/搬运）。默认dryrun保守起步。</li>
 * </ul>
 */
public class Takeover extends AbstractTakeover {
	private static final @NotNull Logger logger = LogManager.getLogger(Takeover.class);

	public static final @NotNull String ModeOff = "off";
	public static final @NotNull String ModeDryrun = "dryrun";
	public static final @NotNull String ModeOn = "on";

	/** tryTransfer串行队列key（public：测试用它投递哨兵任务观察队列排空）。 */
	public static final @NotNull String TryTransferOneByOneKey = "Takeover.TryTransfer";

	public final @NotNull Application zeze;
	private final @NotNull String mode;
	private final long ttl;
	private final long scanPeriod;

	private volatile long myEpoch; // 0=未claim
	private volatile boolean started;
	private volatile long suppressScanUntil; // 风暴防护v1：renew恢复成功后冻结扫描一个TTL
	private volatile boolean fenceFatal; // 已被接管：release不得写墓碑
	private volatile @Nullable Runnable fatalAction; // fence失败动作，默认致命退出；测试注入替换

	private final @NotNull CopyOnWriteArrayList<TakeoverScope> scopes = new CopyOnWriteArrayList<>();
	private final @NotNull LongHashMap<Future<?>> retryFutures = new LongHashMap<>(); // key:deadServerId 单发精确重试
	private volatile @Nullable Future<?> renewFuture;
	private volatile @Nullable Future<?> scanFuture;
	private long renewFailCount; // 仅renew周期调度线程串行访问

	public Takeover(@NotNull Application zeze) {
		this.zeze = zeze;
		var conf = zeze.getConfig();
		var m = conf.getTakeoverMode();
		if (!ModeOff.equals(m) && !ModeDryrun.equals(m) && !ModeOn.equals(m))
			throw new IllegalStateException("unknown TakeoverMode: " + m);
		mode = m;
		ttl = conf.getTakeoverTtl();
		scanPeriod = conf.getTakeoverScanPeriod();
		fatalAction = () -> System.exit(-1);
		RegisterZezeTables(zeze);
	}

	public @NotNull String getMode() {
		return mode;
	}

	public long getMyEpoch() {
		return myEpoch;
	}

	/** 测试/诊断用：直接访问租约表。 */
	public @NotNull tTakeoverLease getTable() {
		return _tTakeoverLease;
	}

	/** 测试注入fence失败动作（如计数器）；finally里复原。null恢复默认致命退出。 */
	public void setFatalAction(@Nullable Runnable action) {
		fatalAction = action != null ? action : () -> System.exit(-1);
	}

	/**
	 * Application.start() 在 atomicOpenDatabase() 之后调用一次。
	 * 此后 Timer/CsQueue 全走 addScope 晚注册（claim时不可能集齐scope）。
	 */
	public void start() {
		if (ModeOff.equals(mode))
			return;
		if (started)
			return;
		myEpoch = claim();
		started = true;
		// claim之前已注册的scope补stamp（常规为空：claim发生在Application.start最早期）。
		if (ModeOn.equals(mode))
			for (var scope : scopes)
				stampScope(scope);
		var renewPeriod = Math.max(ttl / 3, 1);
		renewFuture = TaskSpec.ofAction(this::renewOnce).name("Takeover.renew")
				.schedulePeriodNow(renewPeriod, renewPeriod);
		scanFuture = TaskSpec.ofAction(this::scanOnce).name("Takeover.scan")
				.schedulePeriodNow(scanPeriod, scanPeriod);
		// 启动立即扫描一次：接管之前死掉的server（AnnounceServers的功能替代）。
		scanOnce();
	}

	/**
	 * 直接构造Procedure运行事务：Takeover的事务横跨Application生命周期边界
	 * （claim在start()内startState置eStarted之前、release在stop()内置回之后），
	 * zeze.newProcedure的isStart()检查会拒绝，这里自己保证只在数据库打开后调用。
	 */
	private long callDirect(@NotNull FuncLong action, @NotNull String actionName) {
		return new Procedure(zeze, action, actionName, null).call();
	}

	/**
	 * 抢占式claim：无论旧租约是否到期都 {@code epoch=old+1}（重启不等TTL）。
	 * 安全性由fencing保证：旧epoch持有者的事务会被checkFence/stamp对账拒绝。
	 */
	public long claim() {
		var serverId = zeze.getConfig().getServerId();
		var out = new OutLong();
		var r = callDirect(() -> {
			var lease = _tTakeoverLease.getOrAdd(serverId);
			var epoch = lease.getEpoch() + 1;
			lease.setEpoch(epoch);
			lease.setExpireAt(System.currentTimeMillis() + ttl);
			out.value = epoch;
			return 0L;
		}, "Takeover.claim");
		if (r != 0)
			throw new IllegalStateException("Takeover.claim rc=" + r);
		return out.value;
	}

	/**
	 * 注册接管作用域。未start仅入列表；已start且mode==on时在独立小事务内
	 * 【断言租约epoch仍属于自己的 → scope.stamp(myEpoch)】。
	 * 只写自己的root行，与并发接管者由zeze行冲突串行化。
	 */
	public void addScope(@NotNull TakeoverScope scope) {
		scopes.addIfAbsent(scope);
		if (started && ModeOn.equals(mode))
			stampScope(scope);
	}

	private void stampScope(@NotNull TakeoverScope scope) {
		var lost = new boolean[1];
		var healed = new boolean[1];
		var r = callDirect(() -> {
			var serverId = zeze.getConfig().getServerId();
			var lease = _tTakeoverLease.get(serverId);
			if (lease == null) {
				// 行缺失（外部清表等）不是被接管：与renewOnce同款自愈重写自己的租约。
				// 并发自愈由行冲突串行化；若期间已被新owner claim（含墓碑行），getOrAdd
				// 看到别人的epoch→致命退出，不会覆盖新owner。
				lease = _tTakeoverLease.getOrAdd(serverId);
				if (lease.getEpoch() != 0 && lease.getEpoch() != myEpoch) {
					lost[0] = true;
					return Procedure.LogicError;
				}
				lease.setEpoch(myEpoch);
				lease.setExpireAt(System.currentTimeMillis() + ttl);
				healed[0] = true;
			} else if (lease.getEpoch() != myEpoch) {
				lost[0] = true;
				return Procedure.LogicError;
			}
			scope.stamp(myEpoch);
			return 0L;
		}, "Takeover.stamp@" + scope.name());
		if (lost[0]) {
			fenceFailed("addScope: lease lost, scope=" + scope.name() + " myEpoch=" + myEpoch);
			return;
		}
		if (healed[0])
			logger.warn("Takeover.stamp: lease row missing, rewritten (self-heal), serverId={} scope={}",
					zeze.getConfig().getServerId(), scope.name());
		if (r != 0)
			logger.error("Takeover.stamp scope={} rc={}", scope.name(), r);
	}

	/**
	 * 写路径fence：owner在事务内写自己root/链数据前调用（root行本就在事务工作集内，零额外IO）。
	 * mode==on 且 rootEpoch != myEpoch（被接管/serial被覆盖）→ 致命退出+告警。
	 */
	public void checkFence(long rootEpoch) {
		if (!ModeOn.equals(mode) || !started)
			return;
		if (rootEpoch != myEpoch)
			fenceFailed("checkFence: fenced! rootEpoch=" + rootEpoch + " myEpoch=" + myEpoch
					+ "（数据已被接管，本进程必须立即退出）");
	}

	private void fenceFailed(@NotNull String reason) {
		fenceFatal = true; // release不得写墓碑：不能打掉新owner的租约
		// 带触发点栈：fence失败必须致命退出，现场只此一条日志，没有栈无法定位是哪条写路径触发。
		logger.fatal("Takeover: " + reason, new Exception("Takeover fence trigger stack"));
		var action = fatalAction;
		if (action != null)
			action.run();
	}

	private void renewOnce() {
		if (!started)
			return;
		var lost = new boolean[1];
		var healed = new boolean[1];
		try {
			var r = callDirect(() -> {
				var lease = _tTakeoverLease.get(zeze.getConfig().getServerId());
				if (lease == null || lease.getEpoch() == 0) {
					// 租约行丢失（如表被外部清空，Simulate批间清表）：自愈重写自己的租约。
					// epoch保持myEpoch（stamp/fence对账不受影响）；并发自愈由行冲突串行化，输家看到别人的epoch→致命。
					lease = _tTakeoverLease.getOrAdd(zeze.getConfig().getServerId());
					lease.setEpoch(myEpoch);
					lease.setExpireAt(System.currentTimeMillis() + ttl);
					healed[0] = true;
					return 0L;
				}
				if (lease.getEpoch() != myEpoch) {
					lost[0] = true;
					return Procedure.LogicError;
				}
				lease.setExpireAt(System.currentTimeMillis() + ttl);
				return 0L;
			}, "Takeover.renew");
			if (lost[0]) {
				fenceFailed("renew: lease lost, myEpoch=" + myEpoch);
				return;
			}
			if (r == 0 && healed[0])
				logger.warn("Takeover.renew: lease row missing, rewritten (self-heal), serverId={} myEpoch={}",
						zeze.getConfig().getServerId(), myEpoch);
			if (r != 0) {
				renewFailCount++;
				return; // 事务失败（冲突等），下个周期重试；期间expireAt不变，扫描不会误判本进程。
			}
			if (renewFailCount > 0) {
				// 停机窗口内本进程可能被误接管过；恢复后冻结扫描一个TTL，等系统稳定。
				suppressScanUntil = System.currentTimeMillis() + ttl;
				renewFailCount = 0;
			}
		} catch (Throwable e) { // stop竞态（数据库已关）时这里忽略。
			logger.debug("Takeover.renew", e);
		}
	}

	private void scanOnce() {
		if (!started)
			return;
		var now = System.currentTimeMillis();
		if (now < suppressScanUntil)
			return;
		try {
			_tTakeoverLease.walk((serverId, lease) -> {
				var expireAt = lease.getExpireAt();
				if (expireAt != 0 && expireAt < now) // 墓碑(0)跳过；未过期跳过
					tryTransfer(serverId);
				return true;
			});
		} catch (Throwable e) {
			logger.error("Takeover.scan", e);
		}
	}

	/** 异步唯一入口：所有tryTransfer在同一个OneByOne队列串行执行（防惊群）。 */
	public void tryTransfer(int deadServerId) {
		if (!started || ModeOff.equals(mode))
			return;
		if (deadServerId == zeze.getConfig().getServerId())
			return;
		TaskSpec.ofAction(() -> tryTransferNow(deadServerId))
				.name("Takeover.TryTransfer@" + deadServerId)
				.executeOneByOne(TryTransferOneByOneKey, Task.getOneByOne());
	}

	private void tryTransferNow(int deadServerId) {
		if (!started)
			return;
		var retryAt = new OutLong(); // >0: 租约未过期（Suspect迟到/重复），到过期时刻精确重试
		var transferred = new long[1];
		var r = callDirect(() -> {
			var lease = _tTakeoverLease.get(deadServerId);
			if (lease == null || lease.getExpireAt() == 0)
				return 0L; // 幂等出口：无租约或已立墓碑（已被接管/正常关闭）
			var now = System.currentTimeMillis();
			if (lease.getExpireAt() >= now) {
				retryAt.value = lease.getExpireAt();
				return 0L;
			}
			if (ModeDryrun.equals(mode)) {
				logger.info("Takeover.dryrun: would transfer serverId={} epoch={} scopes={}",
						deadServerId, lease.getEpoch(), scopeNames());
				return 0L; // 不搬运不立碑，纯读路径灰度
			}
			var veto = false;
			for (var scope : scopes) {
				var n = scope.transferAll(deadServerId, lease.getEpoch());
				if (n < 0)
					veto = true;
				else
					transferred[0] += n;
			}
			if (!veto)
				lease.setExpireAt(0); // 立墓碑；veto时保持过期态留给高版本
			return 0L;
		}, "Takeover.tryTransfer@" + deadServerId);
		if (r != 0)
			return; // 事务冲突等失败，扫描/重试兜底
		if (retryAt.value > 0)
			scheduleRetryAt(deadServerId, retryAt.value);
		if (transferred[0] > 0) {
			logger.info("Takeover: transferred serverId={} count={} scopes={}",
					deadServerId, transferred[0], scopeNames());
			for (var scope : scopes) // 事务成功后的回调（Timer重调度等），事务外执行。
				scope.afterTransfer(deadServerId);
		}
	}

	private void scheduleRetryAt(int deadServerId, long expireAt) {
		var delay = Math.max(expireAt - System.currentTimeMillis() + 50, 0);
		synchronized (retryFutures) {
			if (retryFutures.containsKey(deadServerId))
				return; // 去重：已有该server的精确重试在途
			var future = TaskSpec.ofAction(() -> {
				synchronized (retryFutures) {
					retryFutures.remove(deadServerId);
				}
				tryTransfer(deadServerId);
			}).name("Takeover.retry@" + deadServerId).scheduleNow(delay);
			retryFutures.put(deadServerId, future);
		}
	}

	private @NotNull String scopeNames() {
		var sb = new StringBuilder("[");
		for (var scope : scopes) {
			if (sb.length() > 1)
				sb.append(',');
			sb.append(scope.name());
		}
		return sb.append(']').toString();
	}

	/**
	 * Application.stop() 早期调用（数据库尚未关闭）。正常关闭写墓碑，
	 * 接管者tryTransfer看到墓碑即可立即干净退出（数据不用搬，都是自己的）。
	 * fenceFatal==true（已被接管）时不写，避免打掉新owner的租约。
	 */
	public void release() {
		var renew = renewFuture;
		renewFuture = null;
		if (renew != null)
			renew.cancel(false);
		var scan = scanFuture;
		scanFuture = null;
		if (scan != null)
			scan.cancel(false);
		synchronized (retryFutures) {
			retryFutures.foreachValue(f -> f.cancel(false));
			retryFutures.clear();
		}
		if (!started)
			return;
		started = false;
		if (ModeOff.equals(mode))
			return;
		if (fenceFatal)
			return; // 已被接管：写墓碑会打掉新owner的租约
		var serverId = zeze.getConfig().getServerId();
		var r = callDirect(() -> {
			var lease = _tTakeoverLease.get(serverId);
			if (lease != null && lease.getEpoch() == myEpoch)
				lease.setExpireAt(0); // 墓碑：正常关闭
			return 0L;
		}, "Takeover.release");
		if (r != 0)
			logger.error("Takeover.release rc={}", r);
	}
}
