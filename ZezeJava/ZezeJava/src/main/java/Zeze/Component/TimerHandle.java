package Zeze.Component;

import org.jetbrains.annotations.NotNull;

public interface TimerHandle {
	void onTimer(@NotNull TimerContext context) throws Exception;

	void onTimerCancel() throws Exception;
}
