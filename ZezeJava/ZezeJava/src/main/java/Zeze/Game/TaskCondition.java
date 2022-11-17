package Zeze.Game;

import Zeze.Transaction.Bean;

public abstract class TaskCondition {
	public abstract boolean accept(Bean eventBean);
	public abstract boolean isDone();
	public abstract Class<? extends Bean> getBeanClass();
	public abstract Class<? extends Bean> getEventBeanClass();
}
