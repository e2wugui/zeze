package Zeze.Arch;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import Zeze.Util.Action1;
import Zeze.Util.TaskCompletionSource;

public class RedirectFuture<T> extends TaskCompletionSource<T> {
	private static final VarHandle ON_RESULT;

	static {
		try {
			ON_RESULT = MethodHandles.lookup().findVarHandle(RedirectFuture.class, "onResult", Action1.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static <T> RedirectFuture<T> finish(T t) {
		var f = new RedirectFuture<T>();
		f.SetResult(t);
		return f;
	}

	private volatile Action1<T> onResult;

	@Override
	public boolean SetResult(T t) {
		if (!super.SetResult(t))
			return false;
		try {
			tryOnResult(t);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	private void tryOnResult(T t) throws Throwable {
		@SuppressWarnings("unchecked")
		var onR = (Action1<T>)ON_RESULT.getAndSet(this, null);
		if (onR != null) {
			onR.run(t);
		}
	}

	// 不支持同时叠多个onResult,否则可能覆盖之前没执行过的
	public RedirectFuture<T> then(Action1<T> onResult) throws Throwable {
		Object result = getRawResult();
		if (result != null)
			onResult.run(toResult(result));
		else {
			this.onResult = onResult;
			result = getRawResult(); // 再次确认,避免并发窗口问题
			if (result != null)
				tryOnResult(toResult(result));
		}
		return this;
	}
}
