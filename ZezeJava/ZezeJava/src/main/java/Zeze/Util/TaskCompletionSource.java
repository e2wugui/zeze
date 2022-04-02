package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TaskCompletionSource<T> implements Future<T> {
	private static final VarHandle RESULT;

	@SuppressWarnings("unused")
	private volatile Object result;

	static {
		try {
			RESULT = MethodHandles.lookup().findVarHandle(TaskCompletionSource.class, "result", Object.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private boolean setResult(Object r) {
		if (r == null)
			r = RESULT;
		if (RESULT.compareAndSet(this, null, r)) {
			synchronized (this) {
				notifyAll();
			}
			return true;
		}
		return false;
	}

	public boolean SetResult(T t) {
		return setResult(t);
	}

	public boolean TrySetException(Throwable e) {
		return setResult(new ExecutionException(e));
	}

	public boolean SetException(Throwable e) {
		return setResult(new ExecutionException(e));
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCancelled() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDone() {
		return result != null;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		Object r = result;
		if (r == null) {
			synchronized (this) {
				while ((r = result) == null)
					wait();
			}
		}
		if (r instanceof ExecutionException)
			throw (ExecutionException)r;
		if (r == RESULT)
			r = null;
		@SuppressWarnings("unchecked")
		T t = (T)r;
		return t;
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		Object r = result;
		if (r == null) {
			synchronized (this) {
				if ((r = result) == null) {
					wait((unit.toNanos(timeout) + 999_999) / 1_000_000);
					if ((r = result) == null)
						throw new TimeoutException();
				}
			}
		}
		if (r instanceof ExecutionException)
			throw (ExecutionException)r;
		if (r == RESULT)
			r = null;
		@SuppressWarnings("unchecked")
		T t = (T)r;
		return t;
	}

	public void Wait() {
		try {
			get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean Wait(long timeout) {
		try {
			get(timeout, TimeUnit.MILLISECONDS);
			return true;
		} catch (TimeoutException | CancellationException e) {
			return false;
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
