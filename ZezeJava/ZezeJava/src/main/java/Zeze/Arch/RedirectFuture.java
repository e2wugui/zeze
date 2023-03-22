package Zeze.Arch;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import Zeze.Util.Action1;
import Zeze.Util.TaskCompletionSource;

public class RedirectFuture<R> extends TaskCompletionSource<R> {
	private static final VarHandle ON_SUCCESS;
	private static final VarHandle ON_FAIL;
	private static final Action1<?> CALLED = __ -> {
		throw new UnsupportedOperationException();
	};

	static {
		try {
			ON_SUCCESS = MethodHandles.lookup().findVarHandle(RedirectFuture.class, "onSuccess", Action1.class);
			ON_FAIL = MethodHandles.lookup().findVarHandle(RedirectFuture.class, "onFail", Action1.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static <R> RedirectFuture<R> finish(R r) {
		var f = new RedirectFuture<R>();
		f.setResult(r);
		return f;
	}

	private volatile @SuppressWarnings("unused") Action1<R> onSuccess;
	private volatile @SuppressWarnings("unused") Action1<Throwable> onFail;

	@Override
	public boolean setResult(R r) {
		if (!super.setResult(r))
			return false;
		tryTriggerOnSuccess(r);
		return true;
	}

	@Override
	public boolean setException(Throwable e) {
		if (!super.setException(e))
			return false;
		tryTriggerOnFail(e);
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
			} catch (RuntimeException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	private void tryTriggerOnFail(Throwable e) {
		if (onFail == null)
			return;
		@SuppressWarnings("unchecked")
		var onF = (Action1<Throwable>)ON_FAIL.getAndSet(this, CALLED);
		if (onF != CALLED) {
			try {
				onF.run(e);
			} catch (RuntimeException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	public RedirectFuture<R> onSuccess(Action1<R> onSuccess) {
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

	public RedirectFuture<R> onFail(Action1<Throwable> onFail) {
		if (!ON_FAIL.compareAndSet(this, null, onFail))
			throw new IllegalArgumentException("already onFail");
		var result = getRawResult();
		if (result instanceof Exception) {
			var cls = result.getClass();
			Throwable e;
			if (cls == ExecutionException.class)
				e = ((ExecutionException)result).getCause();
			else if (cls == CancellationException.class)
				e = (CancellationException)result;
			else
				return this;
			tryTriggerOnFail(e);
		}
		return this;
	}

	public RedirectFuture<R> then(Action1<R> onResult) {
		return onSuccess(onResult).onFail(__ -> onResult.run(null));
	}

	@Deprecated // use then
	public RedirectFuture<R> Then(Action1<R> onResult) {
		return then(onResult);
	}

	@Override
	public RedirectFuture<R> await() {
		super.await();
		return this;
	}
}
