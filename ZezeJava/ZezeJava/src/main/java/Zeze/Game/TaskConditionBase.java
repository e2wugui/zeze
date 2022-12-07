package Zeze.Game;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.ParameterizedType;
import Zeze.Builtin.Game.TaskBase.BTaskCondition;
import Zeze.Builtin.Game.TaskBase.BTaskEvent;
import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;
import Zeze.Util.Action1;

public abstract class TaskConditionBase<ConditionBean extends Bean, EventBean extends Bean> {

	// @formatter:off
	public TaskPhase getPhase() { return phase; }
	private final TaskPhase phase;
	public final void setOnComplete(Action1<TaskConditionBase<ConditionBean, EventBean>> callback) { onCompleteUserCallback = callback; }
	private Action1<TaskConditionBase<ConditionBean, EventBean>> onCompleteUserCallback;
	public abstract boolean accept(BTaskEvent eventBean) throws Throwable;
	public abstract boolean isDone();

	public final void onComplete() throws Throwable {
		if (isDone() && null != onCompleteUserCallback) {
			onCompleteUserCallback.run(this);
		}
	}

	public TaskConditionBase(TaskPhase phase, Class<ConditionBean> extendedBeanClass, Class<EventBean> eventBeanClass) {
		this.phase = phase;
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
