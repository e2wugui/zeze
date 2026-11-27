package Zeze.Util;

import java.util.Objects;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 周期任务句柄（由 {@link Task#schedulePeriodCore} 创建）：补足 JDK ScheduledFuture.cancel 不等待在飞轮退出的缺口。
 * {@link #cancel} 先发布取消标志并取消底层 Future，再取本锁等待已准入的当前轮退出（同线程自 cancel 重入返回）；
 * 等锁超过看门狗预算 + {@link #CANCEL_HANG_MARGIN_MS} 时输出双栈告警。
 */
public class TimerFuture<V extends @Nullable Object> extends ReentrantLock implements ScheduledFuture<V> {
	// 挂死告警余量（叠加任务超时作为检测阈值）
	public static volatile long CANCEL_HANG_MARGIN_MS = 35_000;

	private static final Logger logger = LogManager.getLogger(TimerFuture.class);

	private final @Nullable String name; // 日志名（body.logName(name)），仅用于告警定位
	private final long timeoutMs; // 任务体看门狗预算，与余量共同构成挂死告警阈值

	// volatile：setFuture（调度方）与cancel/isCancelled（取消方、任务线程）跨线程读写
	private volatile @Nullable ScheduledFuture<V> future;
	// volatile: isCancelled() 在任务线程中无锁轮询，需与 cancel() 的写入保持可见性
	private volatile boolean canceled;
	private volatile boolean interruptRequested;

	TimerFuture(@Nullable String name, long timeoutMs) {
		this.name = name;
		this.timeoutMs = timeoutMs;
	}

	@SuppressWarnings("unchecked")
	public void setFuture(ScheduledFuture<?> future) {
		this.future = (ScheduledFuture<V>)future;
		if (canceled)
			future.cancel(interruptRequested); // 取消可能先于调度句柄发布。
	}

	/**
	 * 取消并等待已准入的当前轮退出（同线程自 cancel 重入返回）。
	 * 【锁序约束】调用方不得持有任务体可能获取的锁，否则会与在飞轮形成 ABBA 死锁；
	 * 等锁超时（{@code timeoutMs + CANCEL_HANG_MARGIN_MS}）时告警双栈后继续等待。
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (mayInterruptIfRunning)
			interruptRequested = true;
		canceled = true;
		var current = future;
		// 必须先中断，再等任务体锁；否则可中断等待中的 body 永远收不到中断。
		boolean result = current == null || current.cancel(mayInterruptIfRunning);
		boolean acquired = false;
		boolean interrupted = false;
		try {
			acquired = tryLock(timeoutMs + CANCEL_HANG_MARGIN_MS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // 保留中断标志，继续等待排空
			interrupted = true;
		}
		if (!acquired) {
			if (!interrupted)
				warnHang();
			lock(); // 维持排空契约，继续阻塞直到获取锁
		}
		try {
			return result;
		} finally {
			unlock();
		}
	}

	// 诊断告警：打印持锁线程与当前取消线程双栈，异常不外抛
	private void warnHang() {
		try {
			var owner = getOwner();
			var sb = new StringBuilder();
			if (owner != null) {
				sb.append("owner thread '").append(owner.getName()).append("' stack:\n");
				TimeoutDiagnosable.formatStackTrace(owner.getStackTrace(), sb);
			} else
				sb.append("owner already released\n");
			sb.append("cancel thread '").append(Thread.currentThread().getName()).append("' stack:\n");
			TimeoutDiagnosable.formatStackTrace(Thread.currentThread().getStackTrace(), sb);
			logger.warn("TimerFuture({}) cancel blocked > {}ms acquiring lock, possible ABBA "
					+ "(caller holds a lock the body needs) or uninterruptible body;\n{}", name,
					timeoutMs + CANCEL_HANG_MARGIN_MS, sb);
		} catch (Throwable e) { // 诊断不得弄坏cancel本体；logger自身故障时的最后出口
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
		}
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return Objects.requireNonNull(future, "timer is not scheduled").getDelay(unit);
	}

	@Override
	public int compareTo(Delayed o) {
		if (o == this)
			return 0;
		if (o instanceof TimerFuture<?> timer)
			o = Objects.requireNonNull(timer.future, "timer is not scheduled");
		return Objects.requireNonNull(future, "timer is not scheduled").compareTo(o);
	}

	@Override
	public boolean isCancelled() {
		var current = future;
		return canceled || current != null && current.isCancelled();
	}

	@Override
	public boolean isDone() {
		return Objects.requireNonNull(future, "timer is not scheduled").isDone();
	}

	@Override
	public V get() throws InterruptedException, ExecutionException {
		return Objects.requireNonNull(future, "timer is not scheduled").get();
	}

	@Override
	public V get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return Objects.requireNonNull(future, "timer is not scheduled").get(timeout, unit);
	}
}
