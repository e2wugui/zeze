package Zeze.Util;

public class Timeout {
	private final TimeoutManager.TimeoutSet timeoutSet;
	final Action0 action0;

	Timeout(TimeoutManager.TimeoutSet timeoutSet, Action0 action0) {
		this.timeoutSet = timeoutSet;
		this.action0 = action0;
	}

	public void cancel() {
		timeoutSet.remove(this); // empty timeoutSet 不用删除，很可能马上得到利用，或者被tick消灭。
	}
}
