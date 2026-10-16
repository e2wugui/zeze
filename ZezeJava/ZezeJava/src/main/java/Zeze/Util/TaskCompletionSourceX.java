package Zeze.Util;

import org.jspecify.annotations.Nullable;

// 带上下文附加槽的 TaskCompletionSource：getContext/setContext 携带调用方关联数据
public class TaskCompletionSourceX<R extends @Nullable Object> extends TaskCompletionSource<R> {
	private @Nullable Object context;

	public @Nullable Object getContext() {
		return context;
	}

	public TaskCompletionSourceX<R> setContext(@Nullable Object context) {
		this.context = context;
		return this;
	}
}
