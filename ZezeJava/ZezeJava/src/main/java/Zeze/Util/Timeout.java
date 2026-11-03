package Zeze.Util;

import Zeze.Transaction.DispatchMode;

import java.util.concurrent.locks.ReentrantLock;

public class Timeout implements AutoCloseable, Action0 {
	private final TimeoutManager.TimeoutSet timeoutSet;
	private final Action0 action0;

	Timeout(TimeoutManager.TimeoutSet timeoutSet, Action0 action0) {
		this.timeoutSet = timeoutSet;
		this.action0 = action0;
	}

	TimeoutManager.TimeoutSet getTimeoutSet() {
		return timeoutSet;
	}

	public void cancel() {
		timeoutSet.remove(this); // empty timeoutSet 不用删除，很可能马上得到利用，或者被tick消灭。
	}

	@Override
	public void run() throws Exception {
		action0.run();
	}

	@Override
	public void close() {
		cancel();
	}

	public DispatchMode getDispatchMode() {
		return DispatchMode.Normal;
	}
}
