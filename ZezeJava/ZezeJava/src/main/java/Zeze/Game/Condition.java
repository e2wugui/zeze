package Zeze.Game;

public abstract class Condition {
	public abstract boolean accept(ConditionEvent event);

	public abstract boolean isDone();
}
