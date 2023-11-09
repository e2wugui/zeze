package Zeze.Game;

import java.lang.reflect.ParameterizedType;
import javax.json.JsonObject;
import Zeze.Builtin.Game.TaskBase.BTaskCondition;
import Zeze.Collections.BeanFactory;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;

public abstract class TaskConditionBase<ConditionBean extends Bean, EventBean extends Bean> {

	// @formatter:off
	private final TaskPhase phase;
	private BTaskCondition bean;
	public TaskConditionBase(TaskPhase phase) {
		this.phase = phase;
	}
	public final BTaskCondition getBean() { return bean; }
	public void loadBean(BTaskCondition bean) { this.bean = bean; loadBeanExtended(this.bean); }
	public void loadJson(JsonObject json) {
		bean = new BTaskCondition();
		bean.setConditionType(getType());
		loadJsonExtended(json);
	}
	public TaskPhase getPhase() { return phase; }
	public abstract String getType();
	protected abstract void loadJsonExtended(JsonObject json);
	protected abstract void loadBeanExtended(BTaskCondition bean);
	public abstract boolean accept(Bean eventBean) throws Exception;
	public abstract boolean isCompleted();


	// ======================================== Private方法和一些不需要被注意的方法 ========================================
	@SuppressWarnings("unchecked")
	public ConditionBean getExtendedBean() { return (ConditionBean)bean.getExtendedData().getBean(); }
	private static final BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Serializable bean) { return BeanFactory.getSpecialTypeIdFromBean(bean); }

	public static Bean createBeanFromSpecialTypeId(long typeId) { return beanFactory.createBeanFromSpecialTypeId(typeId); }

	@SuppressWarnings("unchecked")
	public Class<ConditionBean> getConditionBeanClass() {
		ParameterizedType parameterizedType = (ParameterizedType)this.getClass().getGenericSuperclass();
		return (Class<ConditionBean>)parameterizedType.getActualTypeArguments()[0];
	}

	@SuppressWarnings("unchecked")
	public Class<EventBean> getEventBeanClass() {
		ParameterizedType parameterizedType = (ParameterizedType)this.getClass().getGenericSuperclass();
		return (Class<EventBean>)parameterizedType.getActualTypeArguments()[1];
	}
	// @formatter:on
}
