package Zeze.Game;

public abstract class ConditionEvent {
	private boolean breakIfAccepted = false;

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
