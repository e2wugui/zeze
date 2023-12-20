package Zeze.Util;

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

public class TaskCompletionSource<R> implements Future<R> {
	private static final @NotNull VarHandle RESULT, WAIT_HEAD;
	protected static final AltResult NULL_RESULT = new AltResult(null);

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

	protected static final class AltResult {
		public final @Nullable Throwable e;

		AltResult(@Nullable Throwable e) {
			this.e = e;
		}
	}

	static {
		try {
			var lookup = MethodHandles.lookup();
			RESULT = lookup.findVarHandle(TaskCompletionSource.class, "result", Object.class);
			WAIT_HEAD = lookup.findVarHandle(TaskCompletionSource.class, "waitHead", Object.class);
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

	protected @Nullable Object getRawResult() {
		return result;
	}

	private boolean setRawResult(@NotNull Object r) {
		if (!RESULT.compareAndSet(this, null, r))
			return false;
		unparkAll();
		return true;
	}

	public boolean setResult(@Nullable R r) {
		return setRawResult(r != null ? r : NULL_RESULT);
	}

	public boolean setException(@NotNull Throwable e) {
		//noinspection ConstantValue
		if (e == null)
			throw new NullPointerException();
		return setRawResult(new AltResult(e));
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return setRawResult(new AltResult(new CancellationException()));
	}

	@Override
	public boolean isCancelled() {
		Object r = result;
		return r instanceof AltResult && ((AltResult)r).e instanceof CancellationException;
	}

	public boolean isCompletedExceptionally() {
		Object r = result;
		return r instanceof AltResult && ((AltResult)r).e != null;
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
			if ((r = result) != null)
				unparkAll();
			else {
				do {
					LockSupport.park();
					if (Thread.interrupted())
						throw new InterruptedException();
				} while ((r = result) == null);
			}
		}
		return toResult(r);
	}

	@Override
	public R get(long timeout, @NotNull TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		var r = result;
		if (r == null) {
			var ct = Thread.currentThread();
			assert !ct.getName().startsWith("Selector");
			push(ct);
			if ((r = result) != null)
				unparkAll();
			else {
				timeout = unit.toNanos(timeout);
				var deadline = System.nanoTime() + timeout;
				do {
					if (timeout <= 0) // wait(0) == wait(), but get(0) != get()
						throw new TimeoutException();
					LockSupport.parkNanos(timeout);
					if (Thread.interrupted())
						throw new InterruptedException();
					timeout = deadline - System.nanoTime();
				} while ((r = result) == null);
			}
		}
		return toResult(r);
	}

	protected @Nullable R toResult(@NotNull Object o) throws ExecutionException {
		if (o instanceof AltResult) {
			var e = ((AltResult)o).e;
			if (e == null)
				return null;
			if (e instanceof CancellationException)
				throw (CancellationException)e;
			Throwable c;
			if ((e instanceof CompletionException) && (c = e.getCause()) != null)
				e = c;
			throw new ExecutionException(e);
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
