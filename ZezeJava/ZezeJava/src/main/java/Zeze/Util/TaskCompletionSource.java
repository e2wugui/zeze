package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import Zeze.Transaction.Profiler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 可手动完成（setResult/setException）的 Future 实现：CAS 单槽结果 + 无锁等待线程栈
public class TaskCompletionSource<R> implements Future<R> {
	private static final @NotNull VarHandle RESULT, WAIT_HEAD;
	protected static final AltResult NULL_RESULT = new AltResult(null);

	private volatile @SuppressWarnings("unused") Object result;
	private volatile @SuppressWarnings("unused") Node waitHead;

	private static final class Node {
		volatile @Nullable Thread thread;
		volatile @Nullable Node next;

		Node(@NotNull Thread thread) {
			this.thread = thread;
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
			WAIT_HEAD = lookup.findVarHandle(TaskCompletionSource.class, "waitHead", Node.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private @NotNull Node push(@NotNull Thread t) {
		var node = new Node(t);
		for (; ; ) {
			var h = waitHead;
			node.next = h;
			if (WAIT_HEAD.compareAndSet(this, h, node))
				return node;
		}
	}

	private void removeWaiter(@NotNull Node node) {
		node.thread = null;
		retry:
		for (; ; ) {
			Node predecessor = null;
			for (var current = waitHead; current != null; ) {
				var next = current.next;
				if (current.thread != null)
					predecessor = current;
				else if (predecessor != null) {
					predecessor.next = next;
					if (predecessor.thread == null)
						continue retry;
				} else if (!WAIT_HEAD.compareAndSet(this, current, next))
					continue retry;
				current = next;
			}
			return;
		}
	}

	private void unparkAll() {
		for (; ; ) {
			var h = waitHead;
			if (h == null)
				return;
			if (WAIT_HEAD.compareAndSet(this, h, null)) {
				while (h != null) {
					var thread = h.thread;
					h.thread = null;
					if (thread != null)
						LockSupport.unpark(thread);
					h = h.next;
				}
				return;
			}
		}
	}

	protected @Nullable Object getRawResult() {
		return result;
	}

	public boolean setRawResult(@NotNull Object r) {
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
	public R get() { // throws InterruptedException, CompletionException, CancellationException
		var r = result;
		if (r == null) {
			var ct = Thread.currentThread();
			assert !ct.getName().startsWith("Selector");
			var waiter = push(ct);
			try {
				if ((r = result) == null) {
					try (var ignored = Profiler.begin("TaskCompletionSource")) {
						do {
							LockSupport.park();
							if (Thread.interrupted())
								throw Task.forceThrow(new InterruptedException());
						} while ((r = result) == null);
					}
				}
			} finally {
				removeWaiter(waiter);
			}
		}
		return toResult(r);
	}

	@Override
	public R get(long timeout, @NotNull TimeUnit unit) { // throws InterruptedException, TimeoutException, CompletionException, CancellationException
		var r = result;
		if (r == null) {
			var ct = Thread.currentThread();
			assert !ct.getName().startsWith("Selector");
			var waiter = push(ct);
			try {
				if ((r = result) == null) {
					timeout = unit.toNanos(timeout);
					// toNanos 的饱和值与 nanoTime 相加会溢出为负的 deadline（不变式破坏，
					// j.u.c 对饱和超时值有"不超时"特判）。检测饱和（now>0 时 MAX-now 不溢出，now<=0 时
					// now+timeout 不可能溢出）钳制 deadline 为 MAX_VALUE，使"deadline-now 恒为大正数、
					// 循环等到结果为止"的循环不变式显式成立，不再依赖补码双重回绕的偶然自愈。
					var now = System.nanoTime();
					var deadline = timeout >= Long.MAX_VALUE - now ? Long.MAX_VALUE : now + timeout;
					try (var ignored = Profiler.begin("TaskCompletionSource")) {
						do {
							if (timeout <= 0) // wait(0) == wait(), but get(0) != get()
								throw Task.forceThrow(new TimeoutException());
							LockSupport.parkNanos(timeout);
							if (Thread.interrupted())
								throw Task.forceThrow(new InterruptedException());
							timeout = deadline - System.nanoTime();
						} while ((r = result) == null);
					}
				}
			} finally {
				removeWaiter(waiter);
			}
		}
		return toResult(r);
	}


	protected @Nullable R toResult(@NotNull Object o) { // throws CompletionException
		if (o instanceof AltResult) {
			var e = ((AltResult)o).e;
			if (e == null)
				return null;
			if (e instanceof CancellationException)
				throw (CancellationException)e;
			throw Task.forceThrow(new CompletionException(e));
		}
		@SuppressWarnings("unchecked")
		R r = (R)o;
		return r;
	}

	public @Nullable R getNow() { // throws CompletionException, CancellationException
		Object r = result;
		return r != null ? toResult(r) : null;
	}

	public R getNow(R valueIfAbsent) { // throws CompletionException, CancellationException
		Object r = result;
		return r != null ? toResult(r) : valueIfAbsent;
	}

	public R join() { // throws InterruptedException, CompletionException, CancellationException
		return get();
	}

	public @NotNull TaskCompletionSource<R> await() { // throws InterruptedException, CompletionException, CancellationException
		get();
		return this;
	}

	// 带超时的await（FND29 dbh2-02）：超时抛TimeoutException（同get(timeout,unit)的既有形态，
	// 经forceThrow以未检查异常传播）。rpc等待等"future完成路径可能整体失效"（如发送容器已关闭）
	// 的场景必须用带超时版本兜底，不得使用无参await无界悬挂。返回this保持链式（await()同款）。
	public @NotNull TaskCompletionSource<R> await(long timeout, @NotNull TimeUnit unit) { // throws InterruptedException, TimeoutException, CompletionException, CancellationException
		get(timeout, unit);
		return this;
	}

	/**
	 * @return 是否得到结果, 取消或超时会返回false
	 */
	public boolean await(long timeout) { // throws InterruptedException, CompletionException
		try {
			get(timeout, TimeUnit.MILLISECONDS);
			return true;
		} catch (Exception e) {
			//noinspection ConstantValue
			if (e instanceof TimeoutException || e instanceof CancellationException)
				return false;
			throw e;
		}
	}
}
