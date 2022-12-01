package Zeze.Game;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.ParameterizedType;
import Zeze.Builtin.Game.TaskBase.BTaskCondition;
import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;

public abstract class TaskConditionBase<ConditionBean extends Bean, EventBean extends Bean> {
	private final BTaskCondition bean;
	@SuppressWarnings("unchecked")
	public ConditionBean getExtendedBean() { return (ConditionBean)bean.getTaskConditionCustomData().getBean(); }

	public TaskConditionBase(Class<ConditionBean> extendedBeanClass, Class<EventBean> eventBeanClass) {
		bean = new BTaskCondition();

		extendedBeanConstructor = beanFactory.register(extendedBeanClass);
		bean.getTaskConditionCustomData().setBean(BeanFactory.invoke(extendedBeanConstructor));
	}

	public abstract String getName();

	public abstract boolean accept(Bean eventBean);

	public abstract boolean isDone();
	@SuppressWarnings("unchecked")
	public final Class<ConditionBean> getConditionBeanClass(){
		ParameterizedType parameterizedType = (ParameterizedType) this.getClass().getGenericSuperclass();
		return (Class<ConditionBean>) parameterizedType.getActualTypeArguments()[0];
	}

	@SuppressWarnings("unchecked")
	public final Class<EventBean> getEventBeanClass(){
		ParameterizedType parameterizedType = (ParameterizedType) this.getClass().getGenericSuperclass();
		return (Class<EventBean>) parameterizedType.getActualTypeArguments()[1];
	}

	/**
	 * BeanFactory
	 */
	private final static BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Bean bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	private final MethodHandle extendedBeanConstructor;
}
