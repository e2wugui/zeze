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
import org.jspecify.annotations.Nullable;

/**
 * 周期守护定时器：调度线程仅派发、守护体在 worker 池运行，支持常量周期或逐轮动态延迟。
 * {@link #stop()} 关门、取消待触发排期并限时等待在飞轮结束（等待预算 {@code timeoutMs + 5s}）。
 */
public final class DaemonTimer {

	private static final Logger logger = LogManager.getLogger(DaemonTimer.class);

	private final String name;
	// volatile：setPeriodMs 运行期调整，下次续约生效，已排期 pending 不重排。
	private volatile long periodMs;
	// 逐轮延迟策略；非 null 时每次续约求值，此时 periodMs 与 setPeriodMs 不可用。
	private final @Nullable LongSupplier nextDelayMs;
	// 每轮看门狗预算（stop 等待预算 = timeoutMs + 5s）。
	private final long timeoutMs;
	private final Action0 body;
	// 保护 shutdown/pending/inFlight；持锁期间不做阻塞调用。
	private final ReentrantLock gate = new ReentrantLock();
	private volatile boolean shutdown = true;
	// 在飞轮完成信号：派发前置位、收尾时完成，供 stop 限时等待。
	private volatile @Nullable CompletableFuture<@Nullable Void> inFlight;
	private @Nullable ScheduledFuture<?> pending; // 已排期的下一次触发，触发时置 null
	private long generation; // gate 内递增，用于作废重启前的旧 tick
	// 在飞轮执行线程，供 stop 识别自调避免等待自身。
	private volatile @Nullable Thread bodyThread;

	public DaemonTimer(String name, long periodMs, Action0 body) {
		this(name, periodMs, 0, body);
	}

	/**
	 * @param timeoutMs 每轮看门狗超时(毫秒)，&lt;=0 时取 {@link Task#defaultTimeout}；
	 *                  stop等待预算=timeoutMs+5s（构造时快照）
	 */
	public DaemonTimer(String name, long periodMs, long timeoutMs, Action0 body) {
		if (periodMs <= 0)
			throw new IllegalArgumentException("periodMs <= 0");
		this.name = name;
		this.periodMs = periodMs;
		this.timeoutMs = timeoutMs > 0 ? timeoutMs : Task.defaultTimeout;
		this.body = body;
		this.nextDelayMs = null;
	}

	/**
	 * @param nextDelayMs 每轮续约时求值的下一次延迟(毫秒)；返回值 &lt;=0 或抛异常将终止定时器并记错。
	 *                    此构造下调用 {@link #setPeriodMs} 抛 {@link IllegalStateException}。
	 * @param timeoutMs   每轮看门狗超时(毫秒)，&lt;=0 时取 {@link Task#defaultTimeout}
	 */
	public DaemonTimer(String name, LongSupplier nextDelayMs, long timeoutMs, Action0 body) {
		this.name = name;
		this.periodMs = -1; // 供应商模式不使用
		this.nextDelayMs = Objects.requireNonNull(nextDelayMs);
		this.timeoutMs = timeoutMs > 0 ? timeoutMs : Task.defaultTimeout;
		this.body = body;
	}

	/** 启动（首轮在周期延迟后触发）。幂等，stop 后可再次 start。 */
	public void start() {
		gate.lock();
		try {
			if (!shutdown)
				return;
			shutdown = false;
			generation++;
			rescheduleLocked();
		} finally {
			gate.unlock();
		}
	}

	/** 调整周期(毫秒)，下次续约生效（已排期的 pending 不重排）；仅常量周期构造可用。 */
	public void setPeriodMs(long periodMs) {
		if (nextDelayMs != null)
			throw new IllegalStateException("period is owned by the nextDelayMs supplier");
		if (periodMs <= 0)
			throw new IllegalArgumentException("periodMs <= 0");
		this.periodMs = periodMs;
	}

	/**
	 * 停止（幂等）：关门、取消已排期触发并限时等待在飞轮结束；body 线程内自调跳过等待。
	 */
	public void stop() {
		ScheduledFuture<?> p;
		CompletableFuture<@Nullable Void> f;
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
			p.cancel(false); // 普通 ScheduledFuture，cancel 不经 TimerFuture 锁，无死锁风险
		if (f == null)
			return;
		if (t == Thread.currentThread()) { // body 内自调 stop，跳过等待避免自等超时
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

	// 调度线程上的 tick 入口：仅判关门、置在飞并派发，不内联执行守护体。
	private void fire(long expectedGeneration) {
		final CompletableFuture<@Nullable Void> f;
		gate.lock();
		try {
			if (shutdown || generation != expectedGeneration)
				return;
			pending = null;
			f = new CompletableFuture<>();
			inFlight = f;
			bodyThread = null;
		} finally {
			gate.unlock();
		}
		try {
			// 收尾放在 runBody 外层 finally，确保看门狗初始化失败时仍完成 inFlight 并续约。
			Task.poolOrThrow(false).execute(() -> runBody(f));
		} catch (Throwable e) {
			finishRound(f);
			logger.error("DaemonTimer {} dispatch failed, rescheduled", name, e);
		}
	}

	private void runBody(CompletableFuture<@Nullable Void> f) {
		try {
			gate.lock();
			try {
				if (shutdown || inFlight != f)
					return;
				bodyThread = Thread.currentThread();
			} finally {
				gate.unlock();
			}
			try (var ignored = TimeoutDiagnosable.create(timeoutMs)) {
				gate.lock();
				try {
					if (shutdown || inFlight != f)
						return; // 创建看门狗期间发生 stop/restart，旧轮不进入业务体
				} finally {
					gate.unlock();
				}
				Task.callCore(new TaskBody.OfAction(body), name);
			}
		} catch (Throwable e) {
			logger.error("DaemonTimer {} body exception", name, e);
		} finally {
			finishRound(f);
		}
	}

	// gate 内按 inFlight==f 归属清状态并续约，锁外完成信号（避免逃逸旧轮清除新轮状态）。
	private void finishRound(CompletableFuture<@Nullable Void> f) {
		gate.lock();
		try {
			if (inFlight == f) {
				inFlight = null;
				bodyThread = null;
				rescheduleLocked();
			}
		} finally {
			gate.unlock();
		}
		f.complete(null);
	}

	// 需在 gate 内调用；pending 非空时不重复排期，保证链上至多一个待触发项。
	private void rescheduleLocked() {
		if (shutdown || pending != null)
			return;
		long delay;
		try {
			delay = nextDelayMs != null ? nextDelayMs.getAsLong() : periodMs;
		} catch (Throwable e) {
			shutdown = true;
			logger.error("DaemonTimer {} nextDelayMs supplier exception, daemon terminated", name, e);
			return;
		}
		if (delay <= 0) {
			shutdown = true;
			logger.error("DaemonTimer {} nextDelayMs={} <= 0, daemon terminated", name, delay);
			return;
		}
		try {
			long expectedGeneration = generation;
			// tick 仅管理排期和派发，不套用看门狗。
			pending = Task.getScheduledThreadPool().schedule(() -> fire(expectedGeneration), delay, TimeUnit.MILLISECONDS);
		} catch (Throwable e) {
			shutdown = true;
			logger.error("DaemonTimer {} reschedule failed, daemon terminated", name, e);
		}
	}

	private void awaitIdle(CompletableFuture<@Nullable Void> f) {
		var budget = Math.max(1, timeoutMs + 5_000);
		try {
			f.get(budget, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			logger.warn("DaemonTimer {} body still running, skip waiting", name);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			logger.error("DaemonTimer {} await error", name, e);
		}
	}
}
