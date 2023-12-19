package Zeze.Arch;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import Zeze.Util.Action1;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.jetbrains.annotations.NotNull;

public class RedirectFuture<R> extends TaskCompletionSource<R> {
	private static final @NotNull VarHandle ON_SUCCESS, ON_FAIL;
	private static final Action1<?> CALLED = __ -> {
		throw new UnsupportedOperationException();
	};

	static {
		try {
			var lookup = MethodHandles.lookup();
			ON_SUCCESS = lookup.findVarHandle(RedirectFuture.class, "onSuccess", Action1.class);
			ON_FAIL = lookup.findVarHandle(RedirectFuture.class, "onFail", Action1.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static <R> @NotNull RedirectFuture<R> finish(R r) {
		var f = new RedirectFuture<R>();
		f.setResult(r);
		return f;
	}

	private volatile @SuppressWarnings("unused") Action1<R> onSuccess;
	private volatile @SuppressWarnings("unused") Action1<RedirectException> onFail;

	@Override
	public boolean setResult(R r) {
		if (!super.setResult(r))
			return false;
		tryTriggerOnSuccess(r);
		return true;
	}

	@Override
	public boolean setException(Throwable e) {
		var ex = e instanceof RedirectException ? (RedirectException)e
				: new RedirectException(RedirectException.GENERIC, e);
		if (!super.setException(ex))
			return false;
		tryTriggerOnFail(ex);
		return true;
	}

	private void tryTriggerOnSuccess(R r) {
		if (onSuccess == null)
			return;
		@SuppressWarnings("unchecked")
		var onS = (Action1<R>)ON_SUCCESS.getAndSet(this, CALLED);
		if (onS != CALLED) {
			try {
				onS.run(r);
			} catch (Exception e) {
				Task.forceThrow(e);
			}
		}
	}

	private void tryTriggerOnFail(@NotNull RedirectException e) {
		if (onFail == null)
			return;
		@SuppressWarnings("unchecked")
		var onF = (Action1<RedirectException>)ON_FAIL.getAndSet(this, CALLED);
		if (onF != CALLED) {
			try {
				onF.run(e);
			} catch (Exception ex) {
				Task.forceThrow(ex);
			}
		}
	}

	public @NotNull RedirectFuture<R> onSuccess(@NotNull Action1<R> onSuccess) {
		//noinspection ConstantValue
		if (onSuccess == null)
			throw new NullPointerException("null onSuccess");
		if (!ON_SUCCESS.compareAndSet(this, null, onSuccess))
			throw new IllegalArgumentException("already onSuccess");
		var result = getRawResult();
		if (result != null) {
			if (result instanceof Exception) {
				var cls = result.getClass();
				if (cls == ExecutionException.class || cls == CancellationException.class)
					return this;
				if (result == NULL_RESULT)
					result = null;
			}
			@SuppressWarnings("unchecked")
			R r = (R)result;
			tryTriggerOnSuccess(r);
		}
		return this;
	}

	public @NotNull RedirectFuture<R> onFail(@NotNull Action1<RedirectException> onFail) {
		//noinspection ConstantValue
		if (onFail == null)
			throw new NullPointerException("null onFail");
		if (!ON_FAIL.compareAndSet(this, null, onFail))
			throw new IllegalArgumentException("already onFail");
		var result = getRawResult();
		if (result != null && result.getClass() == ExecutionException.class)
			tryTriggerOnFail((RedirectException)((ExecutionException)result).getCause());
		return this;
	}

	public @NotNull RedirectFuture<R> then(@NotNull Action1<R> onResult) {
		return onSuccess(onResult).onFail(__ -> onResult.run(null));
	}

	@Deprecated // use then
	public @NotNull RedirectFuture<R> Then(@NotNull Action1<R> onResult) {
		return then(onResult);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		throw new UnsupportedOperationException();
	}

	@Override
	public @NotNull RedirectFuture<R> await() {
		super.await();
		return this;
	}
}
