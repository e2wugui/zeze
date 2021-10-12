package Zeze.Transaction;

import Zeze.Serialize.*;

public class DynamicBean extends Bean implements DynamicBeanReadOnly {
	@Override
	public long getTypeId() {
		if (false == this.isManaged()) {
			return _TypeId;
		}
		var txn = Transaction.getCurrent();
		if (txn == null) {
			return _TypeId;
		}
		txn.VerifyRecordAccessed(this, true);
		// 不能独立设置，总是设置Bean时一起Commit，所以这里访问Bean的Log。
		var log = (LogV)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.SpecialTypeId : _TypeId;
	}

	public final Bean getBean() {
		if (false == this.isManaged()) {
			return _Bean;
		}
		var txn = Transaction.getCurrent();
		if (txn == null) {
			return _Bean;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (LogV)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _Bean;
	}

	public final void setBean(Bean value) {
		if (null == value) {
			throw new NullPointerException();
		}

		if (false == this.isManaged()) {
			_TypeId = GetSpecialTypeIdFromBean.toId(value);
			_Bean = value;
			return;
		}
		value.InitRootInfo(RootInfo, this);
		value.setVariableId(1); // 只有一个变量
		var txn = Transaction.getCurrent();
		txn.VerifyRecordAccessed(this);
		txn.PutLog(new LogV(this, value));
	}

	private long _TypeId;
	private Bean _Bean;

	private DynamicBeanToId GetSpecialTypeIdFromBean;
	public final DynamicBeanToId getGetSpecialTypeIdFromBean() {
		return GetSpecialTypeIdFromBean;
	}
	private DynamicIdToBean CreateBeanFromSpecialTypeId;
	public final DynamicIdToBean getCreateBeanFromSpecialTypeId() {
		return CreateBeanFromSpecialTypeId;
	}

	public DynamicBean(int variableId, DynamicBeanToId get, DynamicIdToBean create) {
		super(variableId);
		_Bean = new EmptyBean();
		_TypeId = EmptyBean.TYPEID;

		GetSpecialTypeIdFromBean = get;
		CreateBeanFromSpecialTypeId = create;
	}

	public final void Assign(DynamicBean other) {
		setBean(other.getBean().CopyBean());
	}

	@Override
	public boolean NegativeCheck() {
		return getBean().NegativeCheck();
	}

	@Override
	public int getCapacityHintOfByteBuffer() {
		return getBean().getCapacityHintOfByteBuffer();
	}

	@Override
	public Bean CopyBean() {
		var copy = new DynamicBean(getVariableId(), getGetSpecialTypeIdFromBean(), getCreateBeanFromSpecialTypeId());
		copy._Bean = getBean().CopyBean();
		copy._TypeId = getTypeId();
		return copy;
	}

	private void SetBeanWithSpecialTypeId(long specialTypeId, Bean bean) {
		if (false == this.isManaged()) {
			_TypeId = specialTypeId;
			_Bean = bean;
			return;
		}
		bean.InitRootInfo(RootInfo, this);
		bean.setVariableId(1); // 只有一个变量
		var txn = Transaction.getCurrent();
		txn.VerifyRecordAccessed(this);
		txn.PutLog(new LogV(specialTypeId, this, bean));
	}

	@Override
	public void Decode(ByteBuffer bb) {
		// 由于可能在事务中执行，这里仅修改Bean
		// TypeId 在 Bean 提交时才修改，但是要在事务中读到最新值，参见 TypeId 的 getter 实现。
		long typeId = bb.ReadLong8();
		Bean real = CreateBeanFromSpecialTypeId.toBean(typeId);
		if (null != real) {
			int _state_ = bb.BeginReadSegment();
			real.Decode(bb);
			bb.EndReadSegment(_state_);
			SetBeanWithSpecialTypeId(typeId, real);
		}
		else {
			bb.SkipBytes();
			SetBeanWithSpecialTypeId(EmptyBean.TYPEID, new EmptyBean());
		}
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong8(getTypeId());
		int _state_ = bb.BeginWriteSegment();
		getBean().Encode(bb);
		bb.EndWriteSegment(_state_);
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		_Bean.InitRootInfo(root, this);
	}

	private final static class LogV extends Log1<DynamicBean, Bean> {
		private long SpecialTypeId;
		public long getSpecialTypeId() {
			return SpecialTypeId;
		}

		public LogV(DynamicBean self, Zeze.Transaction.Bean value) {
			super(self, value);
			// 提前转换，如果是本Dynamic中没有配置的Bean，马上抛出异常。
			SpecialTypeId = self.GetSpecialTypeIdFromBean.toId(value);
		}

		public LogV(long specialTypeId, DynamicBean self, Zeze.Transaction.Bean value) {
			super(self, value);
			SpecialTypeId = specialTypeId;
		}

		@Override
		public long getLogKey() {
			return this.getBean().getObjectId() + 1;
		}

		@Override
		public void Commit() {
			this.getBeanTyped()._Bean = this.getValue();
			this.getBeanTyped()._TypeId = getSpecialTypeId();
		}
	}
}