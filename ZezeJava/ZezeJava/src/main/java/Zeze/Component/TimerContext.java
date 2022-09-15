package Zeze.Component;

import Zeze.Builtin.Timer.*;
import Zeze.Transaction.Bean;

public class TimerContext {
	public long timerId;
	public String timerName;
	public Bean customData;
	public long curTimeMills;
	public long nextExpectedTimeMills;
	public long expectedTimeMills;

	// 从数据库结构构建上下文。
	TimerContext(BTimer timer, long cur, long next, long expected) {
		timerId = timer.getTimerId();
		timerName = timer.getName();
		customData = timer.getCustomData().getBean();
		curTimeMills = cur;
		nextExpectedTimeMills = next;
		expectedTimeMills = expected;
	}

	// 完全自定义构造上下文。
	TimerContext(long timerId, String timerName, Bean customData, long cur, long next, long expected) {
		this.timerId = timerId;
		this.timerName = timerName;
		this.customData = customData;
		this.curTimeMills = cur;
		this.nextExpectedTimeMills = next;
		this.expectedTimeMills = expected;
	}
}
