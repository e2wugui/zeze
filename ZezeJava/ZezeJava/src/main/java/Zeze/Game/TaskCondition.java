package Zeze.Game;

import java.lang.reflect.ParameterizedType;
import Zeze.Transaction.Bean;

public abstract class TaskCondition<ConditionBean extends Bean, EventBean extends Bean> {
	public abstract String getName();

	public abstract boolean accept(Bean eventBean);

	public abstract boolean isDone();

	public abstract ConditionBean getConditionBean();
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
}
