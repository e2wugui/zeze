package Zeze.Util;

import java.util.concurrent.*;

public class TaskCompletionSource<T> extends FutureTask<T> {

	public TaskCompletionSource(Callable<T> callable) {
		super(callable);
	}

	public TaskCompletionSource() {
		super(() -> null);
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
		} catch (TimeoutException e) {
			return false;
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
