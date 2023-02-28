package Zeze.Component;

public interface TimerHandle {
	void onTimer(TimerContext context) throws Exception;

	void onTimerCancel() throws Exception;
}
