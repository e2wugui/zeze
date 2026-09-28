package Zeze.Game;

/**
 * 条件事件基类：breakIfAccepted 控制事件被接受后是否中断后续处理。
 */
public abstract class ConditionEvent {
	private final boolean breakIfAccepted;

	public ConditionEvent() {
		this(false);
	}

	public ConditionEvent(boolean breakIfAccepted) {
		this.breakIfAccepted = breakIfAccepted;
	}

	public final boolean isBreakIfAccepted() {
		return breakIfAccepted;
	}
}
