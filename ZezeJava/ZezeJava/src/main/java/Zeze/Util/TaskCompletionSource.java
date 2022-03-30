package Zeze.Util;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TaskCompletionSource<T> extends FutureTask<T> {
	private static final Callable<?> nullCallable = () -> null;

	public TaskCompletionSource(Callable<T> callable) {
		super(callable);
	}

	@SuppressWarnings("unchecked")
	public TaskCompletionSource() {
		super((Callable<T>)nullCallable);
	}

	public boolean TrySetException(Throwable ex) {
		super.setException(ex);
		return true;
	}

	public boolean SetException(Throwable ex) {
		super.setException(ex);
		return true;
	}

	public void SetResult(T t) {
		super.set(t);
	}

	public void Wait() {
		try {
			super.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean Wait(long timeout) {
		try {
			super.get(timeout, TimeUnit.MILLISECONDS);
			return true;
		} catch (TimeoutException | CancellationException e) {
			return false;
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
