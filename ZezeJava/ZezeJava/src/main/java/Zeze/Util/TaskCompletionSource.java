package Zeze.Util;

import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @param <R> 为了性能优化考虑,R不能是ExecutionException,也不能是CancellationException
 */
public class TaskCompletionSource<R> implements Future<R> {
	private static final VarHandle RESULT;
	private static final Exception NULL_RESULT = new LambdaConversionException(null, null, false, false);

	@SuppressWarnings("unused")
	private volatile Object result;

	static {
		try {
			RESULT = MethodHandles.lookup().findVarHandle(TaskCompletionSource.class, "result", Object.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	protected Object getRawResult() {
		return result;
	}

	private boolean setResult(Object r) {
		if (r == null)
			r = NULL_RESULT;
		if (RESULT.compareAndSet(this, null, r)) {
			synchronized (this) {
				notifyAll();
			}
			return true;
		}
		return false;
	}

	public boolean SetResult(R r) {
		return setResult(r);
	}

	public boolean TrySetException(Throwable e) {
		return setResult(new ExecutionException(e));
	}

	public boolean SetException(Throwable e) {
		return setResult(new ExecutionException(e));
	}

	public boolean isCompletedExceptionally() {
		Object r = result;
		return r != null && r.getClass() == ExecutionException.class;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return setResult(new CancellationException());
	}

	@Override
	public boolean isCancelled() {
		Object r = result;
		return r != null && r.getClass() == CancellationException.class;
	}

	@Override
	public boolean isDone() {
		return result != null;
	}

	@Override
	public R get() throws InterruptedException, ExecutionException {
		Object r = result;
		if (r == null) {
			synchronized (this) {
				while ((r = result) == null)
					wait();
			}
		}
		return toResult(r);
	}

	@Override
	public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		Object r = result;
		if (r == null) {
			timeout = unit.toMillis(timeout);
			if (timeout <= 0) // wait(0) == wait(), but get(0) != get()
				throw new TimeoutException();
			synchronized (this) {
				if ((r = result) == null) {
					wait(timeout);
					if ((r = result) == null)
						throw new TimeoutException();
				}
			}
		}
		return toResult(r);
	}

	protected R toResult(Object o) throws ExecutionException {
		if (o instanceof Exception) {
			Class<?> cls = o.getClass();
			if (cls == ExecutionException.class)
				throw (ExecutionException)o;
			if (cls == CancellationException.class)
				throw (CancellationException)o;
			if (o == NULL_RESULT)
				o = null;
		}
		@SuppressWarnings("unchecked")
		R r = (R)o;
		return r;
	}

	public R getNow() throws ExecutionException {
		Object r = result;
		return r != null ? toResult(r) : null;
	}

	public R getNow(R valueIfAbsent) throws ExecutionException {
		Object r = result;
		return r != null ? toResult(r) : valueIfAbsent;
	}

	public R join() {
		try {
			return get();
		} catch (InterruptedException | ExecutionException e) {
			throw new CompletionException(e);
		}
	}

	public TaskCompletionSource<R> await() {
		try {
			get();
		} catch (InterruptedException | ExecutionException e) {
			throw new CompletionException(e);
		}
		return this;
	}

	public boolean await(long timeout) {
		try {
			get(timeout, TimeUnit.MILLISECONDS);
			return true;
		} catch (TimeoutException | CancellationException e) {
			return false;
		} catch (InterruptedException | ExecutionException e) {
			throw new CompletionException(e);
		}
	}

	@Deprecated // 这个方法跟Object.wait太像了,容易混淆用错,还是改用await吧
	public void Wait() {
		await();
	}

	@Deprecated // 这个方法跟Object.wait太像了,容易混淆用错,还是改用await吧
	public boolean Wait(long timeout) {
		return await(timeout);
	}
}
