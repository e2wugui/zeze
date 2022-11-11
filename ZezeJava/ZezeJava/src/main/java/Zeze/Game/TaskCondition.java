package Zeze.Game;

import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;

public abstract class TaskCondition {
	public abstract boolean accept(Bean eventBean);
	public abstract boolean isDone();
}
