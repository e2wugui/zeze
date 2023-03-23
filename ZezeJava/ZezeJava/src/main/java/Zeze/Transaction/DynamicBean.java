package Zeze.Transaction;

import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;

public class DynamicBean extends Bean implements DynamicBeanReadOnly {
	Bean bean;
	long typeId;
	transient final ToLongFunction<Bean> getBean;
	transient final LongFunction<Bean> createBean;

	public DynamicBean(int variableId, ToLongFunction<Bean> get, LongFunction<Bean> create) {
		super(variableId);
		bean = new EmptyBean();
		typeId = EmptyBean.TYPEID;
		this.getBean = get;
		this.createBean = create;
	}

	@Override
	public final Bean getBean() {
		if (!isManaged())
			return bean;
		var txn = Transaction.getCurrentVerifyRead(this);
		if (txn == null)
			return bean;
		var log = (LogDynamic)txn.getLog(parent().objectId() + variableId());
		return log != null ? log.value : bean;
	}

	@SuppressWarnings("deprecation")
	public final void setBean(Bean value) {
		if (value == null)
			throw new IllegalArgumentException("null value");
		if (!isManaged()) {
			typeId = getBean.applyAsLong(value);
			bean = value;
			return;
		}
		value.initRootInfoWithRedo(rootInfo, this);
		value.variableId(1); // 只有一个变量
		var txn = Transaction.getCurrentVerifyWrite(this);
		var log = (LogDynamic)txn.logGetOrAdd(parent().objectId() + variableId(), this::createLogBean);
		log.setValue(value);
	}

	@Override
	public long getTypeId() {
		if (!isManaged())
			return typeId;
		var txn = Transaction.getCurrentVerifyRead(this);
		if (txn == null)
			return typeId;
		// 不能独立设置，总是设置Bean时一起Commit，所以这里访问Bean的Log。
		var log = (LogDynamic)txn.getLog(parent().objectId() + variableId());
		return log != null ? log.specialTypeId : typeId;
	}

	@Override
	public long typeId() {
		return getTypeId();
	}

	public final ToLongFunction<Bean> getGetBean() {
		return getBean;
	}

	public final LongFunction<Bean> getCreateBean() {
		return createBean;
	}

	public Bean newBean(long typeId) {
		bean = createBean.apply(typeId);
		if (bean == null)
			bean = new EmptyBean();
		this.typeId = typeId != 0 ? typeId : getBean.applyAsLong(bean);
		return bean;
	}

	public final void assign(DynamicBean other) {
		setBean(other.getBean().copy());
	}

	public final boolean isEmpty() {
		return typeId == EmptyBean.TYPEID && bean.getClass() == EmptyBean.class;
	}

	public final void reset() {
		bean = new EmptyBean();
		typeId = EmptyBean.TYPEID;
	}

	@Override
	public boolean negativeCheck() {
		return getBean().negativeCheck();
	}

	@Override
	public DynamicBean copy() {
		var copy = new DynamicBean(variableId(), getBean, createBean);
		copy.bean = getBean().copy();
		copy.typeId = getTypeId();
		return copy;
	}

	public void assign(DynamicBeanData other) {
		var bean = createBean.apply(other.typeId());
		bean.assign(other.getBean());
		setBean(bean);
	}

	@SuppressWarnings("deprecation")
	private void setBeanWithSpecialTypeId(long specialTypeId, Bean bean) {
		if (!isManaged()) {
			typeId = specialTypeId;
			this.bean = bean;
			return;
		}
		bean.initRootInfoWithRedo(rootInfo, this);
		bean.variableId(1); // 只有一个变量
		var txn = Transaction.getCurrentVerifyWrite(this);
		var log = (LogDynamic)txn.logGetOrAdd(parent().objectId() + variableId(), this::createLogBean);
		log.setValue(bean);
	}

	@Override
	public void decode(ByteBuffer bb) {
		// 由于可能在事务中执行，这里仅修改Bean
		// TypeId 在 Bean 提交时才修改，但是要在事务中读到最新值，参见 TypeId 的 getter 实现。
		long typeId = bb.ReadLong();
		Bean real = createBean.apply(typeId);
		if (real != null) {
			real.decode(bb);
			setBeanWithSpecialTypeId(typeId, real);
		} else {
			bb.SkipUnknownField(ByteBuffer.BEAN);
			setBeanWithSpecialTypeId(EmptyBean.TYPEID, new EmptyBean());
		}
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(getTypeId());
		getBean().encode(bb);
	}

	@Override
	public int preAllocSize() {
		return 9 + getBean().preAllocSize(); // [9]typeId
	}

	@Override
	public void preAllocSize(int size) {
		getBean().preAllocSize(size - 1); // [1]typeId
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		bean.initRootInfo(root, this);
	}

	@Override
	protected void initChildrenRootInfoWithRedo(Record.RootInfo root) {
		bean.initRootInfoWithRedo(root, this);
	}

	@Override
	public void mapKey(Object mapKey) {
		bean.mapKey(mapKey);
	}

	@Override
	public void followerApply(Log log) {
		var dLog = (LogDynamic)log;
		if (null != dLog.value) {
			typeId = dLog.specialTypeId;
			bean = dLog.value;
		} else if (null != dLog.logBean) {
			bean.followerApply(dLog.logBean);
		}
	}

	@Override
	public LogBean createLogBean() {
		var dLog = new LogDynamic();
		dLog.setBelong(parent());
		dLog.setThis(this);
		dLog.setVariableId(variableId());
		return dLog;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(typeId) ^ bean.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		var that = (DynamicBean)o;
		return typeId == that.typeId && bean.equals(that.bean);
	}
}
