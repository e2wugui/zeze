package Zeze.Game;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.ParameterizedType;
import Zeze.Builtin.Game.TaskBase.BTaskCondition;
import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;
import Zeze.Util.Action1;

public abstract class TaskConditionBase<ConditionBean extends Bean, EventBean extends Bean> {

	// @formatter:off
	public abstract boolean accept(Bean eventBean) throws Throwable;
	public abstract boolean isCompleted();

	protected static class Opt {}
	public TaskConditionBase(TaskPhase phase, Opt opt) {
		this.phase = phase;
		bean = new BTaskCondition();

		MethodHandle extendedBeanConstructor = beanFactory.register(getConditionBeanClass());
		bean.getExtendedData().setBean(BeanFactory.invoke(extendedBeanConstructor));
	}

	/**
	 * Condition Info:
	 */
	public long getConditionId() { return bean.getConditionId(); }
	public final BTaskCondition getBean() { return bean; }
	private final BTaskCondition bean;
	public TaskPhase getPhase() { return phase; }
	private final TaskPhase phase;
	public final void setOnComplete(Action1<TaskConditionBase<ConditionBean, EventBean>> callback) { onCompleteUserCallback = callback; }
	private Action1<TaskConditionBase<ConditionBean, EventBean>> onCompleteUserCallback;
	public void onComplete() throws Throwable {
		if (isCompleted() && null != onCompleteUserCallback) {
			onCompleteUserCallback.run(this);
		}
	}


	// ======================================== Private方法和一些不需要被注意的方法 ========================================
	@SuppressWarnings("unchecked")
	public ConditionBean getExtendedBean() { return (ConditionBean)bean.getExtendedData().getBean(); }
	private final static BeanFactory beanFactory = new BeanFactory();
	public static long getSpecialTypeIdFromBean(Bean bean) { return BeanFactory.getSpecialTypeIdFromBean(bean); }
	public static Bean createBeanFromSpecialTypeId(long typeId) { return beanFactory.createBeanFromSpecialTypeId(typeId); }
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
	// @formatter:on
}
