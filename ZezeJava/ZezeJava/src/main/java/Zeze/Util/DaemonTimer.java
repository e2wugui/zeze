package Zeze.Util;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 周期守护定时器：
 * 调度线程只派发、守护体进worker池、
 * stop三段式（关门→cancel→限时等待在飞一轮）；stop后已派发未启动的迟到轮在runBody入口作废。
 * 每轮看门狗超时timeoutMs可配（&lt;=0取{@link Task#defaultTimeout}），
 * stop等待预算=timeoutMs+5s（对齐每轮executeCore看门狗）。
 * 延迟策略二选一：常量periodMs，或LongSupplier逐轮续约时求值（每日锚点重对齐/自适应退避/随机抖动）
 */
public final class DaemonTimer {

	private static final @NotNull Logger logger = LogManager.getLogger(DaemonTimer.class);

	private final @NotNull String name;
	// volatile：setPeriodMs运行期调整，下一次续约（rescheduleLocked）读取生效，已排期pending不重排
	private volatile long periodMs;
	// 逐轮延迟策略：null=常量周期（用periodMs）；非null=每次续约时求值（锚点重对齐/退避/抖动），
	// 此模式periodMs不参与调度、setPeriodMs不可用
	private final @Nullable LongSupplier nextDelayMs;
	// 每轮executeCore看门狗预算；stop等待预算=timeoutMs+5s。构造时快照，实例期内确定
	private final long timeoutMs;
	private final @NotNull Action0 body;
	// gate守护shutdown/pending/inFlight；gate内不做任何等待与阻塞调用。
	private final ReentrantLock gate = new ReentrantLock();
	// volatile：runBody入口锁外复查作废迟到轮（与inFlight的处置一致），转换仍全在gate内
	private volatile boolean shutdown = true; // 未启动即关门
	// 在飞一轮的完成信号：fire派发前置位，一轮收尾时完成（无论成败）。stop据此限时等待。
	private volatile CompletableFuture<Void> inFlight;
	private ScheduledFuture<?> pending; // 已排期未触发的下一次fire；fire触发时消费置null
	// 在飞body的执行线程：fire清场、runBody入口置位、finishRound归属清除；stop据此识别body内自调。
	private volatile Thread bodyThread;

	public DaemonTimer(@NotNull String name, long periodMs, @NotNull Action0 body) {
		this(name, periodMs, 0, body);
	}

	/**
	 * @param timeoutMs 每轮看门狗超时(毫秒)，&lt;=0 时取 {@link Task#defaultTimeout}；
	 *                  stop等待预算=timeoutMs+5s（构造时快照）
	 */
	public DaemonTimer(@NotNull String name, long periodMs, long timeoutMs, @NotNull Action0 body) {
		if (periodMs <= 0)
			throw new IllegalArgumentException("periodMs <= 0");
		this.name = name;
		this.periodMs = periodMs;
		this.timeoutMs = timeoutMs > 0 ? timeoutMs : Task.defaultTimeout;
		this.body = body;
		this.nextDelayMs = null;
	}

	/**
	 * @param nextDelayMs 每轮续约时求值的下一次延迟(毫秒)——每日锚点重对齐
	 *                    （如{@code () -> Task.delayUntilNextDaily(3, 14)}，其返回值钳制
	 *                    恒&gt;=1）、自适应退避、随机抖动等逐轮策略。求值&lt;=0或抛异常
	 *                    均视为协议违约：关门并记error（&lt;=0防即时触发成busy环）。
	 *                    此构造下{@link #setPeriodMs}抛
	 *                    IllegalStateException（周期归供应商所有）。
	 * @param timeoutMs 每轮看门狗超时(毫秒)，&lt;=0 时取 {@link Task#defaultTimeout}；
	 *                  stop等待预算=timeoutMs+5s（构造时快照）
	 */
	public DaemonTimer(@NotNull String name, @NotNull LongSupplier nextDelayMs, long timeoutMs, @NotNull Action0 body) {
		this.name = name;
		this.periodMs = -1; // 供应商模式不使用
		this.nextDelayMs = Objects.requireNonNull(nextDelayMs);
		this.timeoutMs = timeoutMs > 0 ? timeoutMs : Task.defaultTimeout;
		this.body = body;
	}

	/** 启动（首轮在periodMs后触发）。幂等：运行中调用空转。stop后可再次start。 */
	public void start() {
		gate.lock();
		try {
			if (!shutdown)
				return;
			shutdown = false;
			rescheduleLocked();
		} finally {
			gate.unlock();
		}
	}

	/** 调整周期(毫秒)：下一次续约生效，已排期的pending不提前不延后（对齐旧自续链在续约时取值的语义）。
	 * 仅常量周期构造可用；LongSupplier构造下周期归供应商所有，调用抛IllegalStateException。 */
	public void setPeriodMs(long periodMs) {
		if (nextDelayMs != null)
			throw new IllegalStateException("period is owned by the nextDelayMs supplier");
		if (periodMs <= 0)
			throw new IllegalArgumentException("periodMs <= 0");
		this.periodMs = periodMs;
	}

	/**
	 * 停止：关门（不再触发/续约）、cancel已排期的下一次、限时等待在飞一轮结束。幂等。
	 * body线程内自调跳过等待（等自己必超时），仅关门+cancel。
	 * 超预算或中断仅告警返回——残余的晚到一轮由调用方的资源容错兜住（墓碑化等）。
	 */
	public void stop() {
		ScheduledFuture<?> p;
		CompletableFuture<Void> f;
		Thread t;
		gate.lock();
		try {
			if (shutdown)
				return;
			shutdown = true;
			p = pending;
			pending = null;
			f = inFlight;
			inFlight = null;
			t = bodyThread;
		} finally {
			gate.unlock();
		}
		if (p != null)
			p.cancel(false); // 普通ScheduledFuture：cancel不经TimerFuture锁，无ABBA
		if (f == null)
			return;
		if (t == Thread.currentThread()) { // body内自调stop：等自己必等满预算，跳过
			logger.warn("DaemonTimer {} stop called from body thread, skip waiting", name);
			return;
		}
		awaitIdle(f);
	}

	/** 诊断/测试：当前是否有在飞的一轮（含已派发未开始）。 */
	public boolean isBusy() {
		return inFlight != null;
	}

	/** 诊断/测试：是否处于关门状态（未启动或已stop）。 */
	public boolean isShutdown() {
		gate.lock();
		try {
			return shutdown;
		} finally {
			gate.unlock();
		}
	}

	// 调度线程上的tick入口：只判关门+置在飞+派发，微秒级，绝不内联执行守护体。
	private void fire() {
		final CompletableFuture<Void> f;
		gate.lock();
		try {
			if (shutdown)
				return; // stop()已关门：本次触发作废
			pending = null; // 消费本次排期（链上恒至多一个pending）
			f = new CompletableFuture<>();
			inFlight = f;
			bodyThread = null; // 新一轮占链，body线程待runBody入口置位
		} finally {
			gate.unlock();
		}
		try {
			TaskSpec.ofAction(() -> runBody(f)).name(name).timeout(timeoutMs).runNow();
		} catch (Throwable e) { // 派发失败（worker池关闭等）：守护链不得断裂，收尾并续约下轮
			finishRound(f);
			logger.error("DaemonTimer {} dispatch failed, rescheduled", name, e);
		}
	}

	private void runBody(@NotNull CompletableFuture<Void> f) {
		if (shutdown) { // stop在派发后、启动前关门（worker池排队滞后）：迟到轮作废，不空跑守护体
			finishRound(f); // 仍完成在飞信号：正在限时等待的stop立即醒来
			return;
		}
		bodyThread = Thread.currentThread();
		try {
			body.run();
		} catch (Throwable e) { // 守护体异常不致命：记日志，链继续
			logger.error("DaemonTimer {} body exception", name, e);
		} finally {
			finishRound(f);
		}
	}

	// 一轮收尾：gate内清在飞归属+续约，锁外完成在飞信号（stop的等待线程醒来后不再触碰gate）。
	// inFlight==f的归属检查挡掉stop超预算逃逸的晚到一轮——逃逸轮不占链，不得清新一轮的在飞标志。
	private void finishRound(@NotNull CompletableFuture<Void> f) {
		gate.lock();
		try {
			if (inFlight == f) {
				inFlight = null;
				bodyThread = null;
			}
			rescheduleLocked();
		} finally {
			gate.unlock();
		}
		f.complete(null);
	}

	// gate内。pending非null说明已有一条排期（如stop超预算逃逸的旧一轮结束又逢restart），
	// 不再重复排期——保证任意时刻链上至多一个pending。
	private void rescheduleLocked() {
		if (shutdown || pending != null)
			return;
		long delay;
		try {
			delay = nextDelayMs != null ? nextDelayMs.getAsLong() : periodMs;
		} catch (Throwable e) { // 供应商抛异常：与<=0同罪关门，否则链断而shutdown=false成僵尸守护
			shutdown = true;
			logger.error("DaemonTimer {} nextDelayMs supplier exception, daemon terminated", name, e);
			return;
		}
		if (delay <= 0) { // 供应商违约：scheduleNow的<=0会即时触发成busy环，关门保持状态诚实
			shutdown = true;
			logger.error("DaemonTimer {} nextDelayMs={} <= 0, daemon terminated", name, delay);
			return;
		}
		try {
			pending = TaskSpec.ofAction(this::fire).name(name).scheduleNow(delay);
		} catch (Throwable e) { // 调度池已关（非瞬时故障）：关门让状态诚实，链断记error
			shutdown = true;
			logger.error("DaemonTimer {} reschedule failed, daemon terminated", name, e);
		}
	}

	private void awaitIdle(@NotNull CompletableFuture<Void> f) {
		var budget = Math.max(1, timeoutMs + 5_000); // 对齐每轮看门狗，超时即逃逸轮
		try {
			f.get(budget, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			logger.warn("DaemonTimer {} body still running, skip waiting", name);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) { // 不会发生：inFlight只以complete(null)完成
			logger.error("DaemonTimer {} await error", name, e);
		}
	}
}
