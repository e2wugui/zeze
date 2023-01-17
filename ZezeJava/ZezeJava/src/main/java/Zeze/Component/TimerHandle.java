package Zeze.Component;

public abstract class TimerHandle {
	public abstract void onTimer(TimerContext context) throws Exception;

	// 默认不需要实现。
	@SuppressWarnings("RedundantThrows")
	public void onCancel() throws Exception {
	}
}
