package Zeze.Game;

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
}
