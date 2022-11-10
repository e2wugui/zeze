package Zeze.Game;

import Zeze.Builtin.Game.Task.TriggerTaskEvent;
import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;

public abstract class Condition {
	private final static BeanFactory beanFactory = new BeanFactory();
	public abstract boolean accept(ConditionEvent event);
	public abstract boolean isDone();
	public static long getSpecialTypeIdFromBean(Bean bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}
	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

//	public final boolean accept(TriggerTaskEvent event) {
//		var taskId = event.Argument.getTaskId();
//		var taskPhaseId = event.Argument.getTaskPhaseId();
//		var taskConditionId = event.Argument.getTaskConditionId();
//
//		var bean = event.Argument.getDynamicData().getBean();
//		return accept(bean);
//	}
//
//	public abstract boolean accept(Bean bean);
}
