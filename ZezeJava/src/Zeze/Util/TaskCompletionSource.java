package Zeze.Util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class TaskCompletionSource<T> extends FutureTask<T> {
	
	public TaskCompletionSource(Callable<T> callable) {
		super(callable);
	}
	
	public TaskCompletionSource() {
		super(new Callable<T>() {

			@Override
			public T call() throws Exception {
				return null;
			}
		});
	}
	
	public boolean TrySetException(Throwable ex) {
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
}
