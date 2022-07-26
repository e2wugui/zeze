package Zeze.Transaction;

import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;

public class DynamicBean extends Bean implements DynamicBeanReadOnly {
	private Bean _Bean;
	private long _TypeId;
	private transient final ToLongFunction<Bean> GetSpecialTypeIdFromBean;
	private transient final LongFunction<Bean> CreateBeanFromSpecialTypeId;

	public DynamicBean(int variableId, ToLongFunction<Bean> get, LongFunction<Bean> create) {
		super(variableId);
		_Bean = new EmptyBean();
		_TypeId = EmptyBean.TYPEID;
		GetSpecialTypeIdFromBean = get;
		CreateBeanFromSpecialTypeId = create;
	}

	@Override
	public final Bean getBean() {
		if (!isManaged())
			return _Bean;
		var txn = Transaction.getCurrent();
		if (txn == null)
			return _Bean;
		txn.VerifyRecordAccessed(this, true);
		var log = (LogV)txn.GetLog(getObjectId() + 1);
		return log != null ? log.getValue() : _Bean;
	}

	public final void setBean(Bean value) {
		if (value == null)
			throw new NullPointerException();
		if (!isManaged()) {
			_TypeId = GetSpecialTypeIdFromBean.applyAsLong(value);
			_Bean = value;
			return;
		}
		value.InitRootInfo(RootInfo, this);
		value.setVariableId(1); // 只有一个变量
		var txn = Transaction.getCurrent();
		assert txn != null;
		txn.VerifyRecordAccessed(this);
		txn.PutLog(new LogV(this, value));
	}

	@Override
	public long getTypeId() {
		if (!isManaged())
			return _TypeId;
		var txn = Transaction.getCurrent();
		if (txn == null)
			return _TypeId;
		txn.VerifyRecordAccessed(this, true);
		// 不能独立设置，总是设置Bean时一起Commit，所以这里访问Bean的Log。
		var log = (LogV)txn.GetLog(getObjectId() + 1);
		return log != null ? log.SpecialTypeId : _TypeId;
	}

	public final ToLongFunction<Bean> getGetSpecialTypeIdFromBean() {
		return GetSpecialTypeIdFromBean;
	}

	public final LongFunction<Bean> getCreateBeanFromSpecialTypeId() {
		return CreateBeanFromSpecialTypeId;
	}

	public final void Assign(DynamicBean other) {
		setBean(other.getBean().CopyBean());
	}

	public final boolean isEmpty() {
		return _TypeId == EmptyBean.TYPEID && _Bean.getClass() == EmptyBean.class;
	}

	@Override
	public boolean NegativeCheck() {
		return getBean().NegativeCheck();
	}

	@Override
	public Bean CopyBean() {
		var copy = new DynamicBean(getVariableId(), GetSpecialTypeIdFromBean, CreateBeanFromSpecialTypeId);
		copy._Bean = getBean().CopyBean();
		copy._TypeId = getTypeId();
		return copy;
	}

	private void SetBeanWithSpecialTypeId(long specialTypeId, Bean bean) {
		if (!isManaged()) {
			_TypeId = specialTypeId;
			_Bean = bean;
			return;
		}
		bean.InitRootInfo(RootInfo, this);
		bean.setVariableId(1); // 只有一个变量
		var txn = Transaction.getCurrent();
		assert txn != null;
		txn.VerifyRecordAccessed(this);
		txn.PutLog(new LogV(specialTypeId, this, bean));
	}

	@Override
	public void Decode(ByteBuffer bb) {
		// 由于可能在事务中执行，这里仅修改Bean
		// TypeId 在 Bean 提交时才修改，但是要在事务中读到最新值，参见 TypeId 的 getter 实现。
		long typeId = bb.ReadLong();
		Bean real = CreateBeanFromSpecialTypeId.apply(typeId);
		if (real != null) {
			real.Decode(bb);
			SetBeanWithSpecialTypeId(typeId, real);
		} else {
			bb.SkipUnknownField(ByteBuffer.BEAN);
			SetBeanWithSpecialTypeId(EmptyBean.TYPEID, new EmptyBean());
		}
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(getTypeId());
		getBean().Encode(bb);
	}

	@Override
	public int getPreAllocSize() {
		return 9 + getBean().getPreAllocSize();
	}

	@Override
	public void setPreAllocSize(int size) {
		getBean().setPreAllocSize(size - 1);
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		_Bean.InitRootInfo(root, this);
	}

	private static final class LogV extends Log1<DynamicBean, Bean> {
		private final long SpecialTypeId;

		public long getSpecialTypeId() {
			return SpecialTypeId;
		}

		public LogV(DynamicBean self, Bean value) {
			super(self, 0, value);
			// 提前转换，如果是本Dynamic中没有配置的Bean，马上抛出异常。
			SpecialTypeId = self.GetSpecialTypeIdFromBean.applyAsLong(value);
		}

		public LogV(long specialTypeId, DynamicBean self, Bean value) {
			super(self, 0, value);
			SpecialTypeId = specialTypeId;
		}

		@Override
		public long getLogKey() {
			return getBean().getObjectId() + 1;
		}

		@Override
		public void Commit() {
			getBeanTyped()._Bean = getValue();
			getBeanTyped()._TypeId = getSpecialTypeId();
		}
	}
}
