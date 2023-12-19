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
import java.util.concurrent.locks.LockSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @param <R> 为了性能优化考虑,R不能是ExecutionException,也不能是CancellationException
 */
public class TaskCompletionSource<R> implements Future<R> {
	private static final @NotNull VarHandle RESULT, WAIT_HEAD;
	protected static final Exception NULL_RESULT = new LambdaConversionException(null, null, false, false);

	private volatile @SuppressWarnings("unused") Object result;
	private volatile @SuppressWarnings("unused") Object waitHead; // Node -> Node -> ... -> Thread

	private static final class Node {
		final @NotNull Thread thread;
		final @NotNull Object next; // Node or Thread

		Node(@NotNull Thread thread, @NotNull Object next) {
			this.thread = thread;
			this.next = next;
		}
	}

	static {
		try {
			RESULT = MethodHandles.lookup().findVarHandle(TaskCompletionSource.class, "result", Object.class);
			WAIT_HEAD = MethodHandles.lookup().findVarHandle(TaskCompletionSource.class, "waitHead", Object.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private void push(@NotNull Thread t) {
		for (; ; ) {
			var h = waitHead;
			if (h == null) {
				if (WAIT_HEAD.compareAndSet(this, null, t))
					return;
			} else if (WAIT_HEAD.compareAndSet(this, h, new Node(t, h)))
				return;
		}
	}

	private void unparkAll() {
		for (; ; ) {
			var h = waitHead;
			if (h == null)
				return;
			if (WAIT_HEAD.compareAndSet(this, h, null)) {
				for (; ; ) {
					if (h instanceof Thread) {
						LockSupport.unpark((Thread)h);
						return;
					}
					var n = (Node)h;
					LockSupport.unpark(n.thread);
					h = n.next;
				}
			}
		}
	}

	/**
	 * @return found param thread
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean unparkAllExcept(@NotNull Thread thread) {
		for (; ; ) {
			var h = waitHead;
			if (h == null)
				return false;
			if (WAIT_HEAD.compareAndSet(this, h, null)) {
				for (var found = false; ; ) {
					if (h instanceof Thread) {
						if (thread == h)
							return true;
						LockSupport.unpark((Thread)h);
						return found;
					}
					var n = (Node)h;
					var t = n.thread;
					h = n.next;
					if (thread == t)
						found = true;
					else
						LockSupport.unpark(t);
				}
			}
		}
	}

	protected @Nullable Object getRawResult() {
		return result;
	}

	private boolean setRawResult(@Nullable Object r) {
		if (r == null)
			r = NULL_RESULT;
		if (RESULT.compareAndSet(this, null, r)) {
			unparkAll();
			return true;
		}
		return false;
	}

	public boolean setResult(@Nullable R r) {
		return setRawResult(r);
	}

	public boolean setException(@Nullable Throwable e) {
		return setRawResult(new ExecutionException(e));
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return setRawResult(new CancellationException());
	}

	@Override
	public boolean isCancelled() {
		Object r = result;
		return r != null && r.getClass() == CancellationException.class;
	}

	public boolean isCompletedExceptionally() {
		Object r = result;
		return r != null && r.getClass() == ExecutionException.class;
	}

	@Override
	public boolean isDone() {
		return result != null;
	}

	@Override
	public R get() throws InterruptedException, ExecutionException {
		var r = result;
		if (r == null) {
			var ct = Thread.currentThread();
			assert !ct.getName().startsWith("Selector");
			push(ct);
			if ((r = result) == null || !unparkAllExcept(ct)) {
				LockSupport.park();
				if (Thread.interrupted())
					throw new InterruptedException();
				r = result;
			}
		}
		return toResult(r);
	}

	@Override
	public R get(long timeout, @NotNull TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		var r = result;
		if (r == null) {
			timeout = unit.toNanos(timeout);
			if (timeout <= 0) // wait(0) == wait(), but get(0) != get()
				throw new TimeoutException();
			var ct = Thread.currentThread();
			assert !ct.getName().startsWith("Selector");
			push(ct);
			if ((r = result) == null || !unparkAllExcept(ct)) {
				LockSupport.parkNanos(timeout);
				if (Thread.interrupted())
					throw new InterruptedException();
				if ((r = result) == null)
					throw new TimeoutException();
			}
		}
		return toResult(r);
	}

	protected R toResult(Object o) throws ExecutionException {
		if (o instanceof Exception) {
			var cls = o.getClass();
			if (cls == ExecutionException.class || cls == CancellationException.class)
				throw new ExecutionException((Throwable)o);
			if (o == NULL_RESULT)
				o = null;
		}
		@SuppressWarnings("unchecked")
		R r = (R)o;
		return r;
	}

	public @Nullable R getNow() throws ExecutionException {
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

	public @NotNull TaskCompletionSource<R> await() {
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
}
