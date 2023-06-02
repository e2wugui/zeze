package Zeze.Util;

public class TaskCompletionSourceX<R> extends TaskCompletionSource<R> {
	private Object context;

	public Object getContext() {
		return context;
	}

	public TaskCompletionSourceX<R> setContext(Object context) {
		this.context = context;
		return this;
	}
}
