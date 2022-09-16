package Zeze.Arch;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import Zeze.Util.Action1;
import Zeze.Util.TaskCompletionSource;

public class RedirectFuture<R> extends TaskCompletionSource<R> {
	private static final VarHandle ON_RESULT;

	static {
		try {
			ON_RESULT = MethodHandles.lookup().findVarHandle(RedirectFuture.class, "onResult", Action1.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static <R> RedirectFuture<R> finish(R r) {
		var f = new RedirectFuture<R>();
		f.setResult(r);
		return f;
	}

	private volatile Action1<R> onResult;

	@Override
	public boolean setResult(R r) {
		if (!super.setResult(r))
			return false;
		try {
			tryOnResult(r);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	private void tryOnResult(R r) throws Throwable {
		@SuppressWarnings("unchecked")
		var onR = (Action1<R>)ON_RESULT.getAndSet(this, null);
		if (onR != null)
			onR.run(r);
	}

	// 不支持同时叠多个onResult,否则可能覆盖之前没执行过的
	public RedirectFuture<R> then(Action1<R> onResult) throws Throwable {
		Object result = getRawResult();
		if (result != null)
			onResult.run(toResult(result));
		else {
			this.onResult = onResult;
			if ((result = getRawResult()) != null) // 再次确认,避免并发窗口问题
				tryOnResult(toResult(result));
		}
		return this;
	}

	public RedirectFuture<R> Then(Action1<R> onResult) {
		try {
			return then(onResult);
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RedirectFuture<R> await() {
		super.await();
		return this;
	}
}
