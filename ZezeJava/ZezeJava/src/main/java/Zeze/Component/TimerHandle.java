package Zeze.Component;

public abstract class TimerHandle {
	public abstract void onTimer(TimerContext context) throws Throwable;

	// 默认不需要实现。
	public void onCancel() throws Throwable {
	}
}
