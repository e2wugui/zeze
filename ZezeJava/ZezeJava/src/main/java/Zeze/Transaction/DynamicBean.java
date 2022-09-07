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
		var txn = Transaction.getCurrentVerifyRead(this);
		if (txn == null)
			return _Bean;
		var log = (LogV)txn.GetLog(objectId() + 1);
		return log != null ? log.getValue() : _Bean;
	}

	@SuppressWarnings("deprecation")
	public final void setBean(Bean value) {
		if (value == null)
			throw new NullPointerException();
		if (!isManaged()) {
			_TypeId = GetSpecialTypeIdFromBean.applyAsLong(value);
			_Bean = value;
			return;
		}
		value.InitRootInfoWithRedo(RootInfo, this);
		value.variableId(1); // 只有一个变量
		var txn = Transaction.getCurrentVerifyWrite(this);
		txn.PutLog(new LogV(this, value));
	}

	@Override
	public long getTypeId() {
		if (!isManaged())
			return _TypeId;
		var txn = Transaction.getCurrentVerifyRead(this);
		if (txn == null)
			return _TypeId;
		// 不能独立设置，总是设置Bean时一起Commit，所以这里访问Bean的Log。
		var log = (LogV)txn.GetLog(objectId() + 1);
		return log != null ? log.SpecialTypeId : _TypeId;
	}

	@Override
	public long typeId() {
		return getTypeId();
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
	public DynamicBean CopyBean() {
		var copy = new DynamicBean(variableId(), GetSpecialTypeIdFromBean, CreateBeanFromSpecialTypeId);
		copy._Bean = getBean().CopyBean();
		copy._TypeId = getTypeId();
		return copy;
	}

	@SuppressWarnings("deprecation")
	private void SetBeanWithSpecialTypeId(long specialTypeId, Bean bean) {
		if (!isManaged()) {
			_TypeId = specialTypeId;
			_Bean = bean;
			return;
		}
		bean.InitRootInfoWithRedo(RootInfo, this);
		bean.variableId(1); // 只有一个变量
		var txn = Transaction.getCurrentVerifyWrite(this);
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
	public int preAllocSize() {
		return 9 + getBean().preAllocSize();
	}

	@Override
	public void preAllocSize(int size) {
		getBean().preAllocSize(size - 1);
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		_Bean.InitRootInfo(root, this);
	}

	@Override
	protected void ResetChildrenRootInfo() {
		_Bean.ResetChildrenRootInfo();
	}

	@Override
	public void FollowerApply(Log log) {
		_Bean.FollowerApply(log);
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
			return getBean().objectId() + 1;
		}

		@Override
		public void Commit() {
			getBeanTyped()._Bean = getValue();
			getBeanTyped()._TypeId = getSpecialTypeId();
		}
	}
}
