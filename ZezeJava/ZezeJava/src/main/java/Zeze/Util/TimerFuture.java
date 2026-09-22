package Zeze.Util;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;

/**
 * 周期任务句柄（{@link Task#schedulePeriodCore} 创建，TaskSpec.schedulePeriodNow 返回）。
 * 目的：补 JDK ScheduledFuture.cancel 的缺口——它只阻断后续触发，对已开始的当前轮既不阻止
 * 也不可等待，调用方"cancel 后即拆资源"会与在飞一轮竞态。
 * 机制：任务体在本对象锁内执行并复查取消标志，cancel 取同一把锁，因此
 * ①与"检查→执行"互斥：cancel 返回后不会再启动新的一轮；
 * ②隐式 join 在飞的一轮：返回即可安全拆除任务体访问的资源。
 * 代价：cancel 成为阻塞原语，受 {@link #cancel} 的 ABBA 调用约束。
 */
public class TimerFuture<V> extends ReentrantLock implements ScheduledFuture<V> {
	private ScheduledFuture<V> future;
	// volatile: isCancelled() 在任务线程中无锁轮询，需与 cancel() 的写入保持可见性
	private volatile boolean canceled;

	@SuppressWarnings("unchecked")
	public void setFuture(@NotNull ScheduledFuture<?> future) {
		this.future = (ScheduledFuture<V>)future;
	}

	/**
	 * 取消并 join：取本对象锁，阻塞至在飞一轮任务体结束；返回后不再启动新的一轮
	 * （mayInterruptIfRunning 只作用于底层 future，不中断任务体）。
	 * 【调用约束】不得持有任务体执行期间可能获取的任何锁，否则与在飞任务体互等（ABBA）永久挂起；
	 * 需与停机门禁互斥时，锁内只置关门标志并捕获句柄，释放锁后再 cancel。周期守护的停机
	 * 请优先用 DaemonTimer——它把"关门→cancel→限时等待在飞"内聚为组件，cancel对象为
	 * 普通JDK ScheduledFuture，本约束对其不适用。
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
