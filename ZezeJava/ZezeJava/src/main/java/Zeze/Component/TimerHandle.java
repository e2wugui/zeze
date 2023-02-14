package Zeze.Component;

public interface TimerHandle {
	public void onTimer(TimerContext context) throws Exception;
	public void onTimerCancel() throws Exception;
}
