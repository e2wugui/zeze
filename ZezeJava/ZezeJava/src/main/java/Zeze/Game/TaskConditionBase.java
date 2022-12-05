package Zeze.Game;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.ParameterizedType;
import Zeze.Builtin.Game.TaskBase.BTaskCondition;
import Zeze.Builtin.Game.TaskBase.BTaskEvent;
import Zeze.Builtin.Game.TaskBase.BTaskPhase;
import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;

public abstract class TaskConditionBase<ConditionBean extends Bean, EventBean extends Bean> {

	public abstract boolean isDone();

	public abstract boolean accept(BTaskEvent eventBean);

	public TaskConditionBase(Class<ConditionBean> extendedBeanClass, Class<EventBean> eventBeanClass) {
		bean = new BTaskCondition();

		MethodHandle extendedBeanConstructor = beanFactory.register(extendedBeanClass);
		bean.getExtendedData().setBean(BeanFactory.invoke(extendedBeanConstructor));
	}

	/**
	 * Bean
	 */
	public BTaskCondition getBean() {
		return bean;
	}

	private final BTaskCondition bean;

	@SuppressWarnings("unchecked")
	public ConditionBean getExtendedBean() {
		return (ConditionBean)bean.getExtendedData().getBean();
	}

	private final static BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Bean bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	@SuppressWarnings("unchecked")
	public final Class<ConditionBean> getConditionBeanClass() {
		ParameterizedType parameterizedType = (ParameterizedType)this.getClass().getGenericSuperclass();
		return (Class<ConditionBean>)parameterizedType.getActualTypeArguments()[0];
	}

	@SuppressWarnings("unchecked")
	public final Class<EventBean> getEventBeanClass() {
		ParameterizedType parameterizedType = (ParameterizedType)this.getClass().getGenericSuperclass();
		return (Class<EventBean>)parameterizedType.getActualTypeArguments()[1];
	}
}
