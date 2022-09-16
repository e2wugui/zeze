package Zeze.Transaction;

import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;

public class DynamicBean extends Bean implements DynamicBeanReadOnly {
	private Bean bean;
	private long typeId;
	private transient final ToLongFunction<Bean> getSpecialTypeIdFromBean;
	private transient final LongFunction<Bean> createBeanFromSpecialTypeId;

	public DynamicBean(int variableId, ToLongFunction<Bean> get, LongFunction<Bean> create) {
		super(variableId);
		bean = new EmptyBean();
		typeId = EmptyBean.TYPEID;
		getSpecialTypeIdFromBean = get;
		createBeanFromSpecialTypeId = create;
	}

	@Override
	public final Bean getBean() {
		if (!isManaged())
			return bean;
		var txn = Transaction.getCurrentVerifyRead(this);
		if (txn == null)
			return bean;
		var log = (LogV)txn.getLog(objectId() + 1);
		return log != null ? log.getValue() : bean;
	}

	@SuppressWarnings("deprecation")
	public final void setBean(Bean value) {
		if (value == null)
			throw new IllegalArgumentException("null value");
		if (!isManaged()) {
			typeId = getSpecialTypeIdFromBean.applyAsLong(value);
			bean = value;
			return;
		}
		value.initRootInfoWithRedo(rootInfo, this);
		value.variableId(1); // 只有一个变量
		var txn = Transaction.getCurrentVerifyWrite(this);
		txn.putLog(new LogV(this, value));
	}

	@Override
	public long getTypeId() {
		if (!isManaged())
			return typeId;
		var txn = Transaction.getCurrentVerifyRead(this);
		if (txn == null)
			return typeId;
		// 不能独立设置，总是设置Bean时一起Commit，所以这里访问Bean的Log。
		var log = (LogV)txn.getLog(objectId() + 1);
		return log != null ? log.specialTypeId : typeId;
	}

	@Override
	public long typeId() {
		return getTypeId();
	}

	public final ToLongFunction<Bean> getGetSpecialTypeIdFromBean() {
		return getSpecialTypeIdFromBean;
	}

	public final LongFunction<Bean> getCreateBeanFromSpecialTypeId() {
		return createBeanFromSpecialTypeId;
	}

	public final void assign(DynamicBean other) {
		setBean(other.getBean().copyBean());
	}

	public final boolean isEmpty() {
		return typeId == EmptyBean.TYPEID && bean.getClass() == EmptyBean.class;
	}

	@Override
	public boolean negativeCheck() {
		return getBean().negativeCheck();
	}

	@Override
	public DynamicBean copyBean() {
		var copy = new DynamicBean(variableId(), getSpecialTypeIdFromBean, createBeanFromSpecialTypeId);
		copy.bean = getBean().copyBean();
		copy.typeId = getTypeId();
		return copy;
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
		txn.putLog(new LogV(specialTypeId, this, bean));
	}

	@Override
	public void decode(ByteBuffer bb) {
		// 由于可能在事务中执行，这里仅修改Bean
		// TypeId 在 Bean 提交时才修改，但是要在事务中读到最新值，参见 TypeId 的 getter 实现。
		long typeId = bb.ReadLong();
		Bean real = createBeanFromSpecialTypeId.apply(typeId);
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
		return 9 + getBean().preAllocSize();
	}

	@Override
	public void preAllocSize(int size) {
		getBean().preAllocSize(size - 1);
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		bean.initRootInfo(root, this);
	}

	@Override
	protected void resetChildrenRootInfo() {
		bean.resetChildrenRootInfo();
	}

	@Override
	public void followerApply(Log log) {
		bean.followerApply(log);
	}

	private static final class LogV extends Log1<DynamicBean, Bean> {
		private final long specialTypeId;

		public long getSpecialTypeId() {
			return specialTypeId;
		}

		public LogV(DynamicBean self, Bean value) {
			super(self, 0, value);
			// 提前转换，如果是本Dynamic中没有配置的Bean，马上抛出异常。
			specialTypeId = self.getSpecialTypeIdFromBean.applyAsLong(value);
		}

		public LogV(long specialTypeId, DynamicBean self, Bean value) {
			super(self, 0, value);
			this.specialTypeId = specialTypeId;
		}

		@Override
		public long getLogKey() {
			return getBean().objectId() + 1;
		}

		@Override
		public void commit() {
			getBeanTyped().bean = getValue();
			getBeanTyped().typeId = getSpecialTypeId();
		}
	}
}
