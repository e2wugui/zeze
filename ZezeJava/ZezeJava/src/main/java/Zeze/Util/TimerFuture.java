package Zeze.Util;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;

public class TimerFuture<V> implements ScheduledFuture<V> {
	private final @NotNull ReentrantLock lock;
	private final @NotNull OutInt canceled;
	private final @NotNull ScheduledFuture<V> future;

	public TimerFuture(@NotNull ReentrantLock lock, @NotNull OutInt canceled, @NotNull ScheduledFuture<V> future) {
		this.lock = lock;
		this.canceled = canceled;
		this.future = future;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		lock.lock();
		try {
			canceled.value = 1;
			return future.cancel(mayInterruptIfRunning);
		} finally {
			lock.unlock();
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
		return future.isCancelled();
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
