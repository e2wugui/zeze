package Zeze.Component;

import Zeze.Application;
import Zeze.Builtin.Timer.*;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public class TimerContext {
	public final Timer timer;
	public final String timerId;
	public final String timerName;
	public Bean customData;
	public final long curTimeMills;
	public final long nextExpectedTimeMills;
	public final long expectedTimeMills;

	// TimerRole Parameter
	public long roleId;

	// TimerAccount Parameter
	public String account;
	public String clientId;

	// 从数据库结构构建上下文。
	TimerContext(Timer timer, BTimer bTimer, long cur, long next, long expected) {
		this.timer = timer;
		timerId = bTimer.getTimerName();
		timerName = bTimer.getHandleName();
		customData = bTimer.getCustomData().getBean();
		if (customData instanceof EmptyBean)
			customData = null;
		curTimeMills = cur;
		nextExpectedTimeMills = next;
		expectedTimeMills = expected;
	}

	// 完全自定义构造上下文。
	TimerContext(Timer timer, String timerId, String timerName, Bean customData, long cur, long next, long expected) {
		this.timer = timer;
		this.timerId = timerId;
		this.timerName = timerName;
		this.customData = customData;
		this.curTimeMills = cur;
		this.nextExpectedTimeMills = next;
		this.expectedTimeMills = expected;
	}
}
