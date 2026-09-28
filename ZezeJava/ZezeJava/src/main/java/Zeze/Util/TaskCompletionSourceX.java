package Zeze.Util;

// 带上下文附加槽的 TaskCompletionSource：getContext/setContext 携带调用方关联数据
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
