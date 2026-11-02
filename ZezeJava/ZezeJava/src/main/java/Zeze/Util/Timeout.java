package Zeze.Util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Timeout {
	private static final Logger logger = LogManager.getLogger(Timeout.class);
	private final TimeoutManager.TimeoutSet timeoutSet;
	private final Action0 action0;

	Timeout(TimeoutManager.TimeoutSet timeoutSet, Action0 action0) {
		this.timeoutSet = timeoutSet;
		this.action0 = action0;
	}

	public void cancel() {
		timeoutSet.remove(this);
	}

	void run() {
		try {
			action0.run();
		} catch (Throwable e) {
			logger.error("", e);
		}
	}
}
