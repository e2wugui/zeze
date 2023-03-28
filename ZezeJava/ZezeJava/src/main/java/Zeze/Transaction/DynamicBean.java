package Zeze.Transaction;

import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DynamicBean extends Bean implements DynamicBeanReadOnly {
	@NotNull Bean bean;
	long typeId;
	transient final @NotNull ToLongFunction<Bean> getBean;
	transient final @NotNull LongFunction<Bean> createBean;

	public DynamicBean(int variableId, @NotNull ToLongFunction<Bean> get, @NotNull LongFunction<Bean> create) {
		super(variableId);
		bean = new EmptyBean();
		typeId = EmptyBean.TYPEID;
		this.getBean = get;
		this.createBean = create;
	}

	@Override
	public final @NotNull Bean getBean() {
		if (!isManaged())
			return bean;
		var txn = Transaction.getCurrentVerifyRead(this);
		if (txn == null)
			return bean;
		//noinspection DataFlowIssue
		var log = (LogDynamic)txn.getLog(parent().objectId() + variableId());
		return log != null ? log.value : bean;
	}

	@SuppressWarnings("deprecation")
	public final void setBean(@NotNull Bean value) {
		//noinspection ConstantValue
		if (value == null)
			throw new IllegalArgumentException("null value");
		if (!isManaged()) {
			typeId = getBean.applyAsLong(value);
			bean = value;
			return;
		}
		//noinspection DataFlowIssue
		value.initRootInfoWithRedo(rootInfo, this);
		value.variableId(1); // 只有一个变量
		var txn = Transaction.getCurrentVerifyWrite(this);
		//noinspection DataFlowIssue
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
		//noinspection DataFlowIssue
		var log = (LogDynamic)txn.getLog(parent().objectId() + variableId());
		return log != null ? log.specialTypeId : typeId;
	}

	@Override
	public long typeId() {
		return getTypeId();
	}

	public final @NotNull ToLongFunction<Bean> getGetBean() {
		return getBean;
	}

	public final @NotNull LongFunction<Bean> getCreateBean() {
		return createBean;
	}

	public @NotNull Bean newBean(long typeId) {
		bean = createBean.apply(typeId);
		if (bean == null)
			bean = new EmptyBean();
		this.typeId = typeId != 0 ? typeId : getBean.applyAsLong(bean);
		return bean;
	}

	public final void assign(@NotNull DynamicBean other) {
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
	public @NotNull DynamicBean copy() {
		var copy = new DynamicBean(variableId(), getBean, createBean);
		copy.bean = getBean().copy();
		copy.typeId = getTypeId();
		return copy;
	}

	public void assign(@NotNull DynamicBeanData other) {
		var bean = createBean.apply(other.typeId());
		bean.assign(other.getBean());
		setBean(bean);
	}

	@SuppressWarnings("deprecation")
	private void setBeanWithSpecialTypeId(long specialTypeId, @NotNull Bean bean) {
		if (!isManaged()) {
			typeId = specialTypeId;
			this.bean = bean;
			return;
		}
		//noinspection DataFlowIssue
		bean.initRootInfoWithRedo(rootInfo, this);
		bean.variableId(1); // 只有一个变量
		var txn = Transaction.getCurrentVerifyWrite(this);
		//noinspection DataFlowIssue
		var log = (LogDynamic)txn.logGetOrAdd(parent().objectId() + variableId(), this::createLogBean);
		log.setValue(bean);
	}

	@Override
	public void decode(@NotNull ByteBuffer bb) {
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
	public void encode(@NotNull ByteBuffer bb) {
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
	protected void initChildrenRootInfo(@NotNull Record.RootInfo root) {
		bean.initRootInfo(root, this);
	}

	@Override
	protected void initChildrenRootInfoWithRedo(@NotNull Record.RootInfo root) {
		bean.initRootInfoWithRedo(root, this);
	}

	@Override
	public void mapKey(@NotNull Object mapKey) {
		bean.mapKey(mapKey);
	}

	@Override
	public void followerApply(@NotNull Log log) {
		var dLog = (LogDynamic)log;
		if (null != dLog.value) {
			typeId = dLog.specialTypeId;
			bean = dLog.value;
		} else if (null != dLog.logBean) {
			bean.followerApply(dLog.logBean);
		}
	}

	@Override
	public @NotNull LogBean createLogBean() {
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
	public boolean equals(@Nullable Object o) {
		if (o == this)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		var that = (DynamicBean)o;
		return typeId == that.typeId && bean.equals(that.bean);
	}
}
