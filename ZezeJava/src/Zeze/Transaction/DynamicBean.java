package Zeze.Transaction;

import Zeze.Serialize.*;
import Zeze.*;
import java.util.*;

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
		var log = (Log)txn.GetLog(this.getObjectId() + 1);
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
		var log = (Log)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.Value : _Bean;
	}

	public final void setBean(Bean value) {
		if (null == value) {
			throw new NullPointerException();
		}

		if (false == this.isManaged()) {
			_TypeId = GetSpecialTypeIdFromBean(value);
			_Bean = value;
			return;
		}
		value.InitRootInfo(RootInfo, this);
		value.VariableId = 1; // 只有一个变量
		var txn = Transaction.getCurrent();
		txn.VerifyRecordAccessed(this);
		txn.PutLog(new Log(this, value));
	}

	private long _TypeId;
	private Bean _Bean;

	private tangible.Func1Param<Bean, Long> GetSpecialTypeIdFromBean;
	public final tangible.Func1Param<Bean, Long> getGetSpecialTypeIdFromBean() {
		return GetSpecialTypeIdFromBean;
	}
	private tangible.Func1Param<Long, Bean> CreateBeanFromSpecialTypeId;
	public final tangible.Func1Param<Long, Bean> getCreateBeanFromSpecialTypeId() {
		return CreateBeanFromSpecialTypeId;
	}

	public DynamicBean(int variableId, tangible.Func1Param<Bean, Long> get, tangible.Func1Param<Long, Bean> create) {
		super(variableId);
		_Bean = new EmptyBean();
		_TypeId = EmptyBean.TYPEID;

		GetSpecialTypeIdFromBean = ::get;
		CreateBeanFromSpecialTypeId = ::create;
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
		return getBean().CapacityHintOfByteBuffer;
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
		bean.VariableId = 1; // 只有一个变量
		var txn = Transaction.getCurrent();
		txn.VerifyRecordAccessed(this);
		txn.PutLog(new Log(specialTypeId, this, bean));
	}

	@Override
	public void Decode(ByteBuffer bb) {
		// 由于可能在事务中执行，这里仅修改Bean
		// TypeId 在 Bean 提交时才修改，但是要在事务中读到最新值，参见 TypeId 的 getter 实现。
		long typeId = bb.ReadLong8();
		Bean real = CreateBeanFromSpecialTypeId(typeId);
		if (null != real) {
			int _state_;
			tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
			bb.BeginReadSegment(tempOut__state_);
		_state_ = tempOut__state_.outArgValue;
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
		int _state_;
		tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
		bb.BeginWriteSegment(tempOut__state_);
	_state_ = tempOut__state_.outArgValue;
		getBean().Encode(bb);
		bb.EndWriteSegment(_state_);
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		_Bean.InitRootInfo(root, this);
	}

	private final static class Log extends Zeze.Transaction.Log<DynamicBean, Zeze.Transaction.Bean> {
		private long SpecialTypeId;
		public long getSpecialTypeId() {
			return SpecialTypeId;
		}

		public Log(DynamicBean self, Zeze.Transaction.Bean value) {
			super(self, value);
			// 提前转换，如果是本Dynamic中没有配置的Bean，马上抛出异常。
			SpecialTypeId = self.GetSpecialTypeIdFromBean(value);
		}

		public Log(long specialTypeId, DynamicBean self, Zeze.Transaction.Bean value) {
			super(self, value);
			SpecialTypeId = specialTypeId;
		}

		@Override
		public long getLogKey() {
			return this.getBean().ObjectId + 1;
		}

		@Override
		public void Commit() {
			this.getBeanTyped()._Bean = this.getValue();
			this.getBeanTyped()._TypeId = getSpecialTypeId();
		}
	}
}