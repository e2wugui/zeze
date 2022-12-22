package Zeze.Game;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.ParameterizedType;
import javax.json.JsonObject;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Builtin.Game.TaskBase.BTaskCondition;
import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;
import Zeze.Util.Action1;

public abstract class TaskConditionBase<ConditionBean extends Bean, EventBean extends Bean> {

	// @formatter:off
	private BTaskCondition bean;
	public final BTaskCondition getBean() {return bean;}
	public void loadBean(BTaskCondition bean) { this.bean = bean; loadBeanExtended(bean); }
	protected abstract void loadBeanExtended(BTaskCondition bean);
	public void loadJson(JsonObject json) {  }
	public abstract void loadJsonExtended(JsonObject json);
	public TaskPhase getPhase() { return phase; }
	private final TaskPhase phase;
	public abstract String getType();
	public abstract boolean accept(Bean eventBean) throws Throwable;
	public abstract boolean isCompleted();

	public TaskConditionBase(TaskPhase phase) {
		this.phase = phase;
		beanFactory.register(getConditionBeanClass());
		beanFactory.register(getEventBeanClass());
	}

	// ======================================== Private方法和一些不需要被注意的方法 ========================================
	@SuppressWarnings("unchecked")
	public ConditionBean getExtendedBean() { return (ConditionBean)bean.getExtendedData().getBean(); }
	private final static BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Bean bean) { return BeanFactory.getSpecialTypeIdFromBean(bean); }

	public static Bean createBeanFromSpecialTypeId(long typeId) { return beanFactory.createBeanFromSpecialTypeId(typeId); }

	@SuppressWarnings("unchecked")
	private Class<ConditionBean> getConditionBeanClass() {
		ParameterizedType parameterizedType = (ParameterizedType)this.getClass().getGenericSuperclass();
		return (Class<ConditionBean>)parameterizedType.getActualTypeArguments()[0];
	}

	@SuppressWarnings("unchecked")
	private Class<EventBean> getEventBeanClass() {
		ParameterizedType parameterizedType = (ParameterizedType)this.getClass().getGenericSuperclass();
		return (Class<EventBean>)parameterizedType.getActualTypeArguments()[1];
	}
	// @formatter:on
}
