package Zeze.Component;

import Zeze.Builtin.Timer.BTimer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TimerContext {
	public final @NotNull Timer timer; // 所属的Timer模块
	public final @NotNull String timerId; // 用户指定的timerId, 或"@"+Base64编码的自动分配ID
	public final @NotNull String timerName; // 用户实现Zeze.Component.TimerHandle接口的完整类名
	public @Nullable Bean customData; // 创建timer时用户指定的上下文数据(持久化过的)
	public final long happenTimes; // 已经触发过的次数, 触发前自增, 即首次触发时是1
	public final long expectedTimeMills; // 本次应该触发的时间(unix毫秒时间戳)
	public long nextExpectedTimeMills; // 下次计划触发的时间(unix毫秒时间戳), SimpleTimer可以修改并生效下次触发时间

	public long roleId; // 所属的角色ID. 只用于TimerRole

	public String account; // 所属的账号名. 只用于TimerAccount
	public String clientId; // 所属的客户端名. 只用于TimerAccount

	// 从数据库结构构建上下文
	TimerContext(@NotNull Timer timer, @NotNull BTimer bTimer,
				 long happenTimes, long expectedTimeMills, long nextExpectedTimeMills) {
		this.timer = timer;
		timerId = bTimer.getTimerName();
		timerName = bTimer.getHandleName();
		customData = bTimer.getCustomData().getBean();
		if (customData instanceof EmptyBean)
			customData = null;
		this.happenTimes = happenTimes;
		this.expectedTimeMills = expectedTimeMills;
		this.nextExpectedTimeMills = nextExpectedTimeMills;
	}

	// 完全自定义构造上下文
	TimerContext(@NotNull Timer timer, @NotNull String timerId, @NotNull String timerName, @Nullable Bean customData,
				 long happenTimes, long expectedTimeMills, long nextExpectedTimeMills) {
		this.timer = timer;
		this.timerId = timerId;
		this.timerName = timerName;
		this.customData = customData;
		this.happenTimes = happenTimes;
		this.expectedTimeMills = expectedTimeMills;
		this.nextExpectedTimeMills = nextExpectedTimeMills;
	}
}
