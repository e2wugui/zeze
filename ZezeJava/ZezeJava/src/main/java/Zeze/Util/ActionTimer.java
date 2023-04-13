package Zeze.Util;

@FunctionalInterface
public interface ActionTimer {
	void run(TimerFuture timer) throws Exception;
}
