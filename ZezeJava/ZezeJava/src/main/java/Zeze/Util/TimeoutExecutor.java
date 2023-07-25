package Zeze.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jetbrains.annotations.NotNull;

public class TimeoutExecutor implements ExecutorService {
	private final ExecutorService executor;
	private volatile long defaultTimeout = 60 * 1000; // 1 minute

	public synchronized void setDefaultTimeout(long value) {
		defaultTimeout = value;
	}

	public long getDefaultTimeout() {
		return defaultTimeout;
	}

	/**
	 * 线程数量有限制.
	 * @param workerThreads threads count.
	 * @param prefix thread name prefix.
	 */
	public TimeoutExecutor(int workerThreads, String prefix) {
		this.executor = Task.newFixedThreadPool(workerThreads, prefix);
	}

	/**
	 * 线程数量没有限制.
	 */
	public TimeoutExecutor() {
		this.executor = Executors.newCachedThreadPool();
	}

	@Override
	public void shutdown() {
		executor.shutdown();
	}

	@NotNull
	@Override
	public List<Runnable> shutdownNow() {
		return executor.shutdownNow();
	}

	@Override
	public boolean isShutdown() {
		return executor.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return executor.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
		return executor.awaitTermination(timeout, unit);
	}

	private static <T> Callable<T> decorate(Callable<T> task, long timeout) {
		return timeout > 0 ? new TimeoutCallable<>(task, timeout) : task;
	}

	private static Runnable decorate(Runnable task, long timeout) {
		return timeout > 0 ? new TimeoutRunnable(task, timeout) : task;
	}

	private static <V> Callable<V> decorate(Runnable task, V result, long timeout) {
		var callable = Executors.callable(task, result);
		return timeout > 0 ? new TimeoutCallable<>(callable, timeout) : callable;
	}

	@NotNull
	@Override
	public <T> Future<T> submit(@NotNull Callable<T> task) {
		return executor.submit(decorate(task, defaultTimeout));
	}

	@NotNull
	@Override
	public <T> Future<T> submit(@NotNull Runnable task, T result) {
		return executor.submit(decorate(task, result, defaultTimeout));
	}

	@NotNull
	@Override
	public Future<?> submit(@NotNull Runnable task) {
		return executor.submit(decorate(task, defaultTimeout));
	}

	@NotNull
	@Override
	public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
		if (defaultTimeout > 0) {
			var decorates = new ArrayList<Callable<T>>(tasks.size());
			for (var task : tasks)
				decorates.add(decorate(task, defaultTimeout));
			return executor.invokeAll(decorates);
		}
		return executor.invokeAll(tasks);
	}

	@NotNull
	@Override
	public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException {
		if (defaultTimeout > 0) {
			var decorates = new ArrayList<Callable<T>>(tasks.size());
			for (var task : tasks)
				decorates.add(decorate(task, defaultTimeout));
			return executor.invokeAll(decorates, timeout, unit);
		}
		return executor.invokeAll(tasks, timeout, unit);
	}

	@NotNull
	@Override
	public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		if (defaultTimeout > 0) {
			var decorates = new ArrayList<Callable<T>>(tasks.size());
			for (var task : tasks)
				decorates.add(decorate(task, defaultTimeout));
			return executor.invokeAny(decorates);
		}
		return executor.invokeAny(tasks);
	}

	@Override
	public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (defaultTimeout > 0) {
			var decorates = new ArrayList<Callable<T>>(tasks.size());
			for (var task : tasks)
				decorates.add(decorate(task, defaultTimeout));
			return executor.invokeAny(decorates, timeout, unit);
		}
		return executor.invokeAny(tasks, timeout, unit);
	}

	@Override
	public void execute(@NotNull Runnable command) {
		executor.execute(decorate(command, defaultTimeout));
	}

	static class TimeoutRunnable extends TimeoutManager.Timeout implements Runnable {
		private final Runnable inner;
		private final long timeout;
		private volatile Thread runner;

		public TimeoutRunnable(Runnable task, long timeout) {
			if (null == task)
				throw new NullPointerException();
			this.inner = task;
			this.timeout = timeout;
		}

		@Override
		public void run() {
			if (timeout > 0) {
				runner = Thread.currentThread();
				TimeoutManager.instance.schedule(this, timeout);
				try {
					inner.run();
				} finally {
					TimeoutManager.instance.remove(this);
					runner = null;
				}
			} else
				inner.run();
		}

		@Override
		public void onTimeout() throws Exception {
			processThread(runner);
		}
	}

	static class TimeoutCallable<V> extends TimeoutManager.Timeout implements Callable<V> {
		private final Callable<V> inner;
		private final long timeout;
		private volatile Thread runner;

		public TimeoutCallable(Callable<V> task, long timeout) {
			if (null == task)
				throw new NullPointerException();
			this.inner = task;
			this.timeout = timeout;
		}

		@Override
		public V call() throws Exception {
			if (timeout > 0) {
				runner = Thread.currentThread();
				TimeoutManager.instance.schedule(this, timeout);
				try {
					return inner.call();
				} finally {
					TimeoutManager.instance.remove(this);
					runner = null;
				}
			} else
				return inner.call();
		}

		@Override
		public void onTimeout() throws Exception {
			processThread(runner);
		}
	}
}
