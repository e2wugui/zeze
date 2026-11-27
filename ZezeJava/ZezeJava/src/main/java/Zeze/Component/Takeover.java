package Zeze.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Builtin.Takeover.tTakeoverLease;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.DaemonTimer;
import Zeze.Util.FuncLong;
import Zeze.Util.LongHashMap;
import Zeze.Util.OutLong;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 接管租约组件：基于与业务同库的租约表 {@code tTakeoverLease(serverId -> {epoch, expireAt})}，
 * 通过抢占式 claim、周期 renew、停机宽限 release、写路径 epoch fence 与按 scope 独立事务的幂等搬运，
 * 完成宕机/缩容节点数据的安全接管。支持 off / dryrun / on 三种模式（Config.TakeoverMode）。
 */
public class Takeover extends AbstractTakeover {
	private static final Logger logger = LogManager.getLogger(Takeover.class);

	public static final String ModeOff = "off";
	public static final String ModeDryrun = "dryrun";
	public static final String ModeOn = "on";

	/** tryTransfer 串行队列 key。 */
	public static final String TryTransferOneByOneKey = "Takeover.TryTransfer";

	public final Application zeze;
	private final String mode;
	private final long ttl;

	private volatile long myEpoch; // 0 表示未 claim
	private volatile boolean started;
	private volatile long suppressScanUntil; // renew 恢复成功后冻结扫描一个 TTL
	private volatile boolean fenceFatal; // 已被接管，release 不得再修改租约

	private final CopyOnWriteArrayList<TakeoverScope> scopes = new CopyOnWriteArrayList<>();
	// 已完成本生命周期 stamp 提交的 scope；未登记时写路径抛 NotStartException 而非致命退出
	private final Set<TakeoverScope> scopedScopes = ConcurrentHashMap.newKeySet();
	private final LongHashMap<Future<?>> retryFutures = new LongHashMap<>(); // key: deadServerId 单发精确重试
	private final DaemonTimer renewDaemon;
	private final DaemonTimer scanDaemon;
	private long renewFailCount; // 仅 renew 守护线程串行访问
	// 按 (deadServerId, epoch) 去重记录 veto 告警，避免扫描持续刷屏
	private final Set<String> vetoWarned = ConcurrentHashMap.newKeySet();

	public Takeover(Application zeze) {
		this.zeze = zeze;
		var conf = zeze.getConfig();
		var m = conf.getTakeoverMode();
		if (!ModeOff.equals(m) && !ModeDryrun.equals(m) && !ModeOn.equals(m))
			throw new IllegalStateException("unknown TakeoverMode: " + m);
		mode = m;
		ttl = conf.getTakeoverTtl();
		long scanPeriod = conf.getTakeoverScanPeriod();
		RegisterZezeTables(zeze);
		renewDaemon = new DaemonTimer("Takeover.renew", Math.max(ttl / 3, 1), this::renewOnce);
		scanDaemon = new DaemonTimer("Takeover.scan", scanPeriod, this::scanOnce);
	}

	public String getMode() {
		return mode;
	}

	public long getMyEpoch() {
		return myEpoch;
	}

	/** 返回租约表句柄（诊断用）。 */
	public tTakeoverLease getTable() {
		return _tTakeoverLease;
	}

	/** 返回指定 scope 是否已完成本生命周期的 stamp 登记。 */
	public boolean isScoped(TakeoverScope scope) {
		return scopedScopes.contains(scope);
	}

	/**
	 * 在 Application.start() 打开数据库后调用：抢占租约、为已注册 scope 盖戳并启动 renew/scan 守护任务。
	 */
	public void start() {
		if (ModeOff.equals(mode))
			return;
		if (started)
			return;
		myEpoch = claim();
		started = true;
		if (ModeOn.equals(mode))
			for (var scope : scopes)
				stampScope(scope);
		renewDaemon.start();
		scanDaemon.start();
		scanOnce();
	}

	/**
	 * 直接构造 Procedure 运行事务（Takeover 事务横跨 Application 启动与停机边界，需绕过 newProcedure 的 isStart 检查）。
	 */
	private long callDirect(FuncLong action, String actionName) {
		return new Procedure(zeze, action, actionName, null).call();
	}

	/**
	 * 抢占式 claim：递增租约 epoch 并刷新到期时间，旧 epoch 持有者由 epoch fence 拦截。
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
	 * 注册接管作用域；若已启动且 mode==on，则在独立事务内断言租约后执行 stamp 并触发扫描。
	 */
	public void addScope(TakeoverScope scope) {
		scopes.addIfAbsent(scope);
		if (started && ModeOn.equals(mode)) {
			stampScope(scope);
			scanOnce(); // 新注册 scope 也须检查已存在的租约墓碑
		}
	}

	private void stampScope(TakeoverScope scope) {
		var lost = new boolean[1];
		var healed = new boolean[1];
		var action = (FuncLong)() -> {
			var serverId = zeze.getConfig().getServerId();
			var lease = _tTakeoverLease.get(serverId);
			// 行缺失或 epoch==0（如外部清表）时按 myEpoch 自愈重写；若期间被新 owner claim 则走 lost
			if (lease == null || lease.getEpoch() == 0) {
				lease = _tTakeoverLease.getOrAdd(serverId);
				if (lease.getEpoch() != 0 && lease.getEpoch() != myEpoch) {
					lost[0] = true;
					return Procedure.LogicError;
				}
				lease.setEpoch(myEpoch);
				lease.setExpireAt(System.currentTimeMillis() + ttl);
				Transaction.whileCommit(() -> healed[0] = true);
			} else if (lease.getEpoch() != myEpoch) {
				lost[0] = true;
				return Procedure.LogicError;
			}
			scope.stamp(myEpoch);
			Transaction.whileCommit(() -> scopedScopes.add(scope));
			return 0L;
		};
		var r = callDirect(action, "Takeover.stamp@" + scope.name());
		if (lost[0]) {
			fenceFailed("stampScope: lease lost, scope=" + scope.name() + " myEpoch=" + myEpoch);
			return;
		}
		if (healed[0])
			logger.warn("Takeover.stamp: lease row missing, rewritten (self-heal), serverId={} scope={}",
					zeze.getConfig().getServerId(), scope.name());
		// 瞬态失败交由 renew 周期补做；完成前该 scope 写路径由 requireScoped 拒绝
		if (r != 0)
			logger.error("Takeover.stamp scope={} rc={}，等待renew周期补stamp（期间该scope写路径NotStart拒绝）",
					scope.name(), r);
	}

	/**
	 * 写路径 epoch fence：在事务内写自身 root/链数据前调用；未完成 stamp 抛 {@link NotStartException}，
	 * rootEpoch 不匹配 myEpoch 则致命退出（被接管后的墓碑 stamp==0 由调用方认领后继续写）。
	 */
	public void checkFence(TakeoverScope scope, long rootEpoch) {
		if (!ModeOn.equals(mode) || !started)
			return;
		requireScoped(scope);
		if (rootEpoch != myEpoch)
			fenceFailed("checkFence: fenced! rootEpoch=" + rootEpoch + " myEpoch=" + myEpoch
					+ "（数据已被接管，本进程必须立即退出）");
	}

	/**
	 * 校验 scope 已完成 stamp 登记；未完成时抛 {@link NotStartException} 拒绝写路径（mode!=on 或未启动时直接通过）。
	 */
	public void requireScoped(TakeoverScope scope) {
		if (!ModeOn.equals(mode) || !started)
			return;
		if (!scopedScopes.contains(scope))
			throw new NotStartException("Takeover scope未完成stamp登记: " + scope.name()
					+ "（stamp未成功，等待renew周期补做；期间该scope写路径拒绝）");
	}

	/** scope 尚未完成 stamp 登记时抛出的写路径拒绝异常（非致命）。 */
	public static final class NotStartException extends IllegalStateException {
		public NotStartException(String message) {
			super(message);
		}
	}

	private void fenceFailed(String reason) {
		fenceFatal = true;
		logger.fatal("Takeover: {}", reason, new Exception("Takeover fence trigger stack"));
		System.exit(-1);
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
					// 租约行缺失或清零时按 myEpoch 自愈重写；并发自愈由行冲突串行化
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
				return;
			}
			if (renewFailCount > 0) {
				// 恢复后冻结扫描一个 TTL，避免停机窗口刚结束就误接管
				suppressScanUntil = System.currentTimeMillis() + ttl;
				renewFailCount = 0;
			}
			// 补做此前瞬态失败的 scope stamp
			if (ModeOn.equals(mode))
				for (var scope : scopes)
					if (!scopedScopes.contains(scope))
						stampScope(scope);
		} catch (Throwable e) {
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
				if (lease.getEpoch() != 0 && expireAt < now) // 墓碑仍可能留下晚注册scope的数据。
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

	/** 单scope搬运事务的结果。 */
	private enum ScopeTransfer {
		Done,       // 本scope事务成功（含幂等搬运0）
		Veto,       // scope拒绝搬运（如Timer版本高于本进程）：不立租约墓碑，留给高版本
		TxFailed,   // 事务失败（冲突等）：本scope留给下轮扫描/重试，其他scope继续
		NotExpired, // 事务内重验发现租约已续期（死者复活/并发处理）：中止剩余搬运
		Finished    // 租约已无或epoch无效：全部结束
	}

	private void tryTransferNow(int deadServerId) {
		if (!started)
			return;
		// 【校验】墓碑仅表示当时已登记的scope完成。每个scope的实际root才是持久化幂等依据。
		var retryAt = new OutLong();
		var deadEpoch = new OutLong();
		var expired = new boolean[1];
		var r = callDirect(() -> {
			var lease = _tTakeoverLease.get(deadServerId);
			if (lease == null || lease.getEpoch() == 0)
				return 0L;
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
			deadEpoch.value = lease.getEpoch(); // 供veto告警（与transferScope事务内读数可能有时差，仅日志用）
			expired[0] = true;
			return 0L;
		}, "Takeover.tryTransfer.check@" + deadServerId);
		if (r != 0)
			return; // 校验事务失败（扫描/重试兜底）
		if (retryAt.value > 0) {
			scheduleRetryAt(deadServerId, retryAt.value); // 未过期（Suspect迟到/重复/复活）：到过期时刻精确重试
			return;
		}
		if (!expired[0])
			return; // 无租约/epoch无效或dryrun

		// 每个 scope 独立事务搬运并在事务内重验租约；任一失败或 veto 则不立墓碑，留待扫描/重试幂等收敛
		var transferredTotal = 0L;
		var veto = false;
		var failed = false;
		for (var scope : scopes) {
			var moved = new long[1];
			switch (transferScope(deadServerId, scope, moved, retryAt)) {
			case Done:
				if (moved[0] > 0) {
					transferredTotal += moved[0];
					logger.info("Takeover: transferred serverId={} scope={} count={}",
							deadServerId, scope.name(), moved[0]);
					scope.afterTransfer(deadServerId);
				}
				break;
			case Veto:
				veto = true;
				if (vetoWarned.add(deadServerId + "#" + deadEpoch.value))
					logger.warn("Takeover: transfer vetoed, serverId={} epoch={}"
							+ "（死者存在版本高于本进程的scope数据，租约保持过期态留给高版本进程接管；"
							+ "在此之前扫描将周期性重试）", deadServerId, deadEpoch.value);
				break;
			case TxFailed:
				failed = true;
				break;
			case NotExpired:
				scheduleRetryAt(deadServerId, retryAt.value);
				return;
			case Finished:
			default:
				return;
			}
		}
		// 全部 scope 完成且无 veto/失败时立租约墓碑
		if (veto || failed)
			return;
		var rr = callDirect(() -> {
			var lease = _tTakeoverLease.get(deadServerId);
			if (lease == null || lease.getExpireAt() == 0)
				return 0L;
			if (lease.getExpireAt() >= System.currentTimeMillis()) {
				retryAt.value = lease.getExpireAt();
				return 0L;
			}
			lease.setExpireAt(0); // 立墓碑
			return 0L;
		}, "Takeover.tryTransfer.tombstone@" + deadServerId);
		if (rr != 0)
			return;
		if (retryAt.value > 0)
			scheduleRetryAt(deadServerId, retryAt.value);
		else {
			if (transferredTotal > 0)
				logger.info("Takeover: transfer complete, serverId={} total={} scopes={}",
						deadServerId, transferredTotal, scopeNames());
			// 立墓碑完成后通知 SM 停止接管梯队升档
			var sm = zeze.getServiceManager();
			if (sm != null)
				sm.sendTakeoverDone(deadServerId);
		}
	}

	/** 单 scope 搬运事务：重验租约后执行 transferAll。 */
	private ScopeTransfer transferScope(int deadServerId, TakeoverScope scope,
	                                             long [] moved, OutLong retryAt) {
		var result = new ScopeTransfer[1];
		var r = callDirect(() -> {
			var lease = _tTakeoverLease.get(deadServerId);
			if (lease == null || lease.getEpoch() == 0) {
				result[0] = ScopeTransfer.Finished;
				return 0L;
			}
			var now = System.currentTimeMillis();
			if (lease.getExpireAt() >= now) {
				retryAt.value = lease.getExpireAt();
				result[0] = ScopeTransfer.NotExpired;
				return 0L;
			}
			var n = scope.transferAll(deadServerId, lease.getEpoch());
			result[0] = n < 0 ? ScopeTransfer.Veto : ScopeTransfer.Done;
			if (n > 0)
				moved[0] = n;
			return 0L;
		}, "Takeover.transfer@" + deadServerId + "#" + scope.name());
		if (r != 0)
			return ScopeTransfer.TxFailed;
		return result[0] != null ? result[0] : ScopeTransfer.TxFailed;
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

	private String scopeNames() {
		var sb = new StringBuilder("[");
		for (var scope : scopes) {
			if (sb.length() > 1)
				sb.append(',');
			sb.append(scope.name());
		}
		return sb.append(']').toString();
	}

	/**
	 * 在 Application.stop() 关库前调用：停止守护任务并刷新一个 TTL 宽限期（滚动重启可抢占保数据，缩容到期后由他节点接管）；
	 * 已被接管（fenceFatal）时不改写租约。
	 */
	public void release() {
		renewDaemon.stop();
		scanDaemon.stop();
		synchronized (retryFutures) {
			retryFutures.foreachValue(f -> f.cancel(false));
			retryFutures.clear();
		}
		if (!started)
			return;
		started = false;
		// 清空本生命周期的 scoped/fenceFatal 状态，确保同进程重启后由 start() 重新 stamp
		scopedScopes.clear();
		var wasFenced = fenceFatal;
		fenceFatal = false;
		if (ModeOff.equals(mode))
			return;
		if (wasFenced)
			return;
		var serverId = zeze.getConfig().getServerId();
		var r = callDirect(() -> {
			var lease = _tTakeoverLease.get(serverId);
			if (lease != null && lease.getEpoch() == myEpoch)
				lease.setExpireAt(System.currentTimeMillis() + ttl);
			return 0L;
		}, "Takeover.release");
		if (r != 0)
			logger.error("Takeover.release rc={}", r);
	}
}
