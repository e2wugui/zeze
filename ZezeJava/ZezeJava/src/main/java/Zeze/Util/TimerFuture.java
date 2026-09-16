package Zeze.Util;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;

public class TimerFuture<V> extends ReentrantLock implements ScheduledFuture<V> {
	private ScheduledFuture<V> future;
	// volatile: isCancelled() 在任务线程中无锁轮询，需与 cancel() 的写入保持可见性
	private volatile boolean canceled;

	@SuppressWarnings("unchecked")
	public void setFuture(@NotNull ScheduledFuture<?> future) {
		this.future = (ScheduledFuture<V>)future;
	}

	/**
	 * 取消周期任务并阻断后续触发。
	 * <p>
	 * 隐式不变量（复审R2成文，曾仅存在于 Task.schedulePeriodCore 的实现细节）：
	 * {@link Task#schedulePeriodCore} 在任务体执行期间持有<b>本对象</b>的锁（body.call 在
	 * future.lock 内跑），本方法需先取得该锁——因此 cancel 天然 join 当前正在执行的一轮任务体
	 * （不中断它，mayInterruptIfRunning 只作用于底层 future），期间阻塞。
	 * 【调用约束】调用方不得持有任务体执行期间可能获取的任何锁：否则与在飞任务体构成 ABBA
	 * 死锁（cancel 等本对象的锁、任务体等调用方持有的锁，双方永久挂起）。需要与停机门禁互斥时，
	 * 先在锁内置关门标志并捕获句柄，释放锁后再 cancel（参见 LoginQueue.stop、
	 * GlobalCacheManagerServer.stop、GlobalCacheManagerWithRaft.close 的形态）。
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		lock();
		try {
			canceled = true;
			return future.cancel(mayInterruptIfRunning);
		} finally {
			unlock();
		}
	}

	@Override
	public long getDelay(@NotNull TimeUnit unit) {
		return future.getDelay(unit);
	}

	@Override
	public int compareTo(@NotNull Delayed o) {
		return future.compareTo(o);
	}

	@Override
	public boolean isCancelled() {
		return canceled; // future.isCancelled(); // 调用此方法可能future字段还没赋值
	}

	@Override
	public boolean isDone() {
		return future.isDone();
	}

	@Override
	public V get() throws InterruptedException, ExecutionException {
		return future.get();
	}

	@Override
	public V get(long timeout, @NotNull TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return future.get(timeout, unit);
	}
}
