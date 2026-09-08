package Zeze.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Builtin.Takeover.tTakeoverLease;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
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
 * <li>tryTransfer：【校验租约过期 → 每scope独立事务搬运（事务内重验租约）→ 全部完成后立墓碑】，
 *     Suspect提示与扫描兜底都汇入这里；原子单位=单个scope（每条CsQueue/Timer链），搬运中途
 *     死者复活则后续scope事务中止；跨scope的部分搬运状态由幂等重入收敛（各scope事务内给死者
 *     root自立stamp=0墓碑，重入transferAll幂等返回0）；</li>
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
	// 已成功stamp（事务提交后置位）的scope：未scoped的写路径checkFence抛NotStartException拒绝
	// 而非致命退出——未登记≠被接管，stamp的瞬态失败不该被放大为进程死亡（FND-C1-11）。
	// TakeoverScope实现类不覆写equals（按实例标识），可直接做identity集合用。
	private final @NotNull Set<TakeoverScope> scopedScopes = ConcurrentHashMap.newKeySet();
	private final @NotNull LongHashMap<Future<?>> retryFutures = new LongHashMap<>(); // key:deadServerId 单发精确重试
	private volatile @Nullable Future<?> renewFuture;
	private volatile @Nullable Future<?> scanFuture;
	private long renewFailCount; // 仅renew周期调度线程串行访问
	// veto（死者数据版本高于本进程）会令扫描对同一死者周期性无限重试，on模式此前零输出——
	// 按(死者serverId,epoch)去重告警一次给运维留线索。条目量以死者数×其历次epoch为界，与租约行同量级。
	private final @NotNull Set<String> vetoWarned = ConcurrentHashMap.newKeySet();

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

	/** 测试/诊断用：scope是否已完成本生命周期的stamp登记（release清零，重启后由start重建）。 */
	public boolean isScoped(@NotNull TakeoverScope scope) {
		return scopedScopes.contains(scope);
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
		var action = (FuncLong)() -> {
			var serverId = zeze.getConfig().getServerId();
			var lease = _tTakeoverLease.get(serverId);
			// 行缺失或epoch==0（外部清表/半清表）不是被接管：与renewOnce同款自愈条件重写
			// 自己的租约——若只认null，epoch==0的残留行会走lost致命退出，与renew的处置相反。
			// 并发自愈由行冲突串行化；若期间已被新owner claim（含墓碑行），getOrAdd
			// 看到别人的epoch→致命退出，不会覆盖新owner。
			if (lease == null || lease.getEpoch() == 0) {
				lease = _tTakeoverLease.getOrAdd(serverId);
				if (lease.getEpoch() != 0 && lease.getEpoch() != myEpoch) {
					lost[0] = true;
					return Procedure.LogicError;
				}
				lease.setEpoch(myEpoch);
				lease.setExpireAt(System.currentTimeMillis() + ttl);
				// 置位必须等提交：体内直接赋值在该轮回滚后残留，终局warn会误报已自愈。
				Transaction.whileCommit(() -> healed[0] = true);
			} else if (lease.getEpoch() != myEpoch) {
				lost[0] = true;
				return Procedure.LogicError;
			}
			scope.stamp(myEpoch);
			// scoped同样必须等提交置位：回滚后root仍是旧值，提前置位会令NotStart检查失效。
			Transaction.whileCommit(() -> scopedScopes.add(scope));
			return 0L;
		};
		var r = callDirect(action, "Takeover.stamp@" + scope.name());
		if (lost[0]) {
			// 租约已属别人=确定被接管：立即致命，不等renew周期（那期间写路径无防护窗口）。
			fenceFailed("stampScope: lease lost, scope=" + scope.name() + " myEpoch=" + myEpoch);
			return;
		}
		if (healed[0])
			logger.warn("Takeover.stamp: lease row missing, rewritten (self-heal), serverId={} scope={}",
					zeze.getConfig().getServerId(), scope.name());
		// FND-C1-11改：瞬态失败（乐观冲突等，rc!=0）不再就地重试——未scoped的scope写路径
		// checkFence抛NotStartException拒绝（不认领、不致命），stamp由renew周期补做，
		// 瞬态失败不再可能被放大为进程死亡。
		if (r != 0)
			logger.error("Takeover.stamp scope={} rc={}，等待renew周期补stamp（期间该scope写路径NotStart拒绝）",
					scope.name(), r);
	}

	/**
	 * 写路径fence（scoped版）：owner在事务内写自己root/链数据前调用（root行本就在事务工作集内，零额外IO）。
	 * 未成功stamp的scope抛{@link NotStartException}拒绝（未登记≠被接管，stamp瞬态失败不致命，
	 * 由renew周期补做）；已scoped且 rootEpoch != myEpoch → 致命退出+告警。击杀场景=同serverId
	 * 双进程（后启动者claim epoch+1并stampScope覆盖前者的root）或stamp被外部篡改。
	 * 【需求语义】被接管后醒来不在击杀之列：transferAll留下的墓碑stamp=0由调用方认领
	 * （stamp=myEpoch后继续写），被接管者在空链上复活、写新数据并提供新数据的服务；其renew
	 * 会把墓碑租约续上复活（epoch保留），下次真死租约可再次过期被接管，生命周期闭环。
	 */
	public void checkFence(@NotNull TakeoverScope scope, long rootEpoch) {
		if (!ModeOn.equals(mode) || !started)
			return;
		requireScoped(scope);
		if (rootEpoch != myEpoch)
			fenceFailed("checkFence: fenced! rootEpoch=" + rootEpoch + " myEpoch=" + myEpoch
					+ "（数据已被接管，本进程必须立即退出）");
	}

	/**
	 * 未完成stamp登记的scope：写路径拒绝（抛{@link NotStartException}），不认领数据行、不致命。
	 * mode!=on或未start恒通过（与checkFence同款前置）。调用方应在访问/认领root行之前调用。
	 */
	public void requireScoped(@NotNull TakeoverScope scope) {
		if (!ModeOn.equals(mode) || !started)
			return;
		if (!scopedScopes.contains(scope))
			throw new NotStartException("Takeover scope未完成stamp登记: " + scope.name()
					+ "（stamp未成功，等待renew周期补做；期间该scope写路径拒绝）");
	}

	/** scope尚未成功stamp（瞬态失败等待renew周期补做）：写路径拒绝继续，而非致命退出。 */
	public static final class NotStartException extends IllegalStateException {
		public NotStartException(@NotNull String message) {
			super(message);
		}
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
			// 补stamp：start/addScope时stamp瞬态失败（rc!=0）的scope由本周期重试，
			// 成功前其写路径NotStart拒绝（fail-safe：拒绝服务好过误杀进程）。
			if (ModeOn.equals(mode))
				for (var scope : scopes)
					if (!scopedScopes.contains(scope))
						stampScope(scope);
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

	/** 单scope搬运事务的结果。 */
	private enum ScopeTransfer {
		Done,       // 本scope事务成功（含幂等搬运0）
		Veto,       // scope拒绝搬运（如Timer版本高于本进程）：不立租约墓碑，留给高版本
		TxFailed,   // 事务失败（冲突等）：本scope留给下轮扫描/重试，其他scope继续
		NotExpired, // 事务内重验发现租约已续期（死者复活/并发处理）：中止剩余搬运
		Finished    // 租约已无或已立墓碑（并发接管完成/正常关闭）：全部结束
	}

	private void tryTransferNow(int deadServerId) {
		if (!started)
			return;
		// 【校验】小事务探租约：幂等出口（无租约/墓碑）、未过期精确重试、dryrun都在这里终结。
		var retryAt = new OutLong();
		var deadEpoch = new OutLong();
		var expired = new boolean[1];
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
			return; // 幂等出口（无租约/墓碑）或dryrun

		// 【搬运】每个scope独立事务：锁足迹小、单scope冲突只影响自己。事务内重验租约——
		// 搬运中途死者复活续约，后续scope事务看到未过期即中止剩余（原子单位=单个scope，
		// 跨scope的部分搬运由幂等重入收敛：已搬scope的死者root stamp=0墓碑在各scope事务内
		// 自立，重入transferAll幂等返回0）。任一scope失败→不立墓碑保持过期态，扫描兜底。
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
					scope.afterTransfer(deadServerId); // 本scope事务提交成功后回调（事务外），如Timer重调度。
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
				// 复活：到新到期时刻重试（已搬的不丢——各scope墓碑已立，重入续搬剩余）。
				scheduleRetryAt(deadServerId, retryAt.value);
				return;
			case Finished:
			default:
				return; // 并发接管已完成（含正常关闭）：退出
			}
		}
		// 【立碑】全部scope完成且无veto无失败：最后一个小事务重验后立墓碑
		// （veto保持过期态留给高版本；failed留给扫描重试幂等收敛）。
		if (veto || failed)
			return;
		var rr = callDirect(() -> {
			var lease = _tTakeoverLease.get(deadServerId);
			if (lease == null || lease.getExpireAt() == 0)
				return 0L; // 已被并发立碑/清除：幂等
			if (lease.getExpireAt() >= System.currentTimeMillis()) {
				retryAt.value = lease.getExpireAt();
				return 0L; // 立碑前死者复活：不立碑
			}
			lease.setExpireAt(0); // 立墓碑
			return 0L;
		}, "Takeover.tryTransfer.tombstone@" + deadServerId);
		if (rr != 0)
			return; // 立碑事务失败：保持过期态，扫描兜底
		if (retryAt.value > 0)
			scheduleRetryAt(deadServerId, retryAt.value);
		else
			logger.info("Takeover: transfer complete, serverId={} total={} scopes={}",
					deadServerId, transferredTotal, scopeNames());
	}

	/** 单scope搬运事务：事务内重验租约（无/墓碑→Finished；未过期→NotExpired）后才transferAll。 */
	private @NotNull ScopeTransfer transferScope(int deadServerId, @NotNull TakeoverScope scope,
	                                             @NotNull long[] moved, @NotNull OutLong retryAt) {
		var result = new ScopeTransfer[1];
		var r = callDirect(() -> {
			var lease = _tTakeoverLease.get(deadServerId);
			if (lease == null || lease.getExpireAt() == 0) {
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
			return ScopeTransfer.TxFailed; // 本scope事务失败（冲突等）：留给下轮，其他scope继续
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
		// 同进程stop→start（重启）时残留的scoped登记会让requireScoped在【新claim生效、
		// stampScope重盖戳之前】的窗口放行写路径：读到旧epoch的root即被fence误杀健康重启
		// （全新进程此窗口走NotStart拒绝，是安全方向）。清掉：重启后由start()重新stamp。
		scopedScopes.clear();
		// fenceFatal是"本生命周期被接管"的状态，生命周期结束即复位——否则stale值会让
		// 重启后下一次release错误跳过自己的正常停机墓碑。
		var wasFenced = fenceFatal;
		fenceFatal = false;
		if (ModeOff.equals(mode))
			return;
		if (wasFenced)
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
