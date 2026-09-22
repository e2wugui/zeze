package Game.Timer;

import Zeze.Hot.HotService;

public interface IModuleTimer extends HotService {
	void cancel(long timerId);
	long schedule(long delay, long period, long times, String name);
}
