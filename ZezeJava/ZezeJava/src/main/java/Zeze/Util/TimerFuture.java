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
	private boolean canceled;

	@SuppressWarnings("unchecked")
	public void setFuture(@NotNull ScheduledFuture<?> future) {
		this.future = (ScheduledFuture<V>)future;
	}

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
