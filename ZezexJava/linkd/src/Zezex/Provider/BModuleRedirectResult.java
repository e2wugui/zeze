package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

public final class BModuleRedirectResult extends Zeze.Transaction.Bean implements BModuleRedirectResultReadOnly {
	private int _ModuleId;
	private int _ServerId; // 目标server的id。
	private int _ReturnCode; // 实现函数的返回。
	private Zeze.Net.Binary _Params;
	private Zeze.Transaction.Collections.PList2<Zezex.Provider.BActionParam> _Actions; // 按回调顺序。！不是定义顺序！

	public int getModuleId() {
		if (false == this.isManaged()) {
			return _ModuleId;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ModuleId;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ModuleId)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _ModuleId;
	}
	public void setModuleId(int value) {
		if (false == this.isManaged()) {
			_ModuleId = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ModuleId(this, value));
	}

	public int getServerId() {
		if (false == this.isManaged()) {
			return _ServerId;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ServerId;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ServerId)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _ServerId;
	}
	public void setServerId(int value) {
		if (false == this.isManaged()) {
			_ServerId = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ServerId(this, value));
	}

	public int getReturnCode() {
		if (false == this.isManaged()) {
			return _ReturnCode;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ReturnCode;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ReturnCode)txn.GetLog(this.getObjectId() + 4);
		return log != null ? log.getValue() : _ReturnCode;
	}
	public void setReturnCode(int value) {
		if (false == this.isManaged()) {
			_ReturnCode = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ReturnCode(this, value));
	}

	public Zeze.Net.Binary getParams() {
		if (false == this.isManaged()) {
			return _Params;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Params;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Params)txn.GetLog(this.getObjectId() + 5);
		return log != null ? log.getValue() : _Params;
	}
	public void setParams(Zeze.Net.Binary value) {
		if (null == value) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_Params = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Params(this, value));
	}

	public Zeze.Transaction.Collections.PList2<Zezex.Provider.BActionParam> getActions() {
		return _Actions;
	}
	private System.Collections.Generic.IReadOnlyList<Zezex.Provider.BActionParamReadOnly> Zezex.Provider.BModuleRedirectResultReadOnly.Actions -> _Actions;


	public BModuleRedirectResult() {
		this(0);
	}

	public BModuleRedirectResult(int _varId_) {
		super(_varId_);
		_Params = Zeze.Net.Binary.Empty;
		_Actions = new Zeze.Transaction.Collections.PList2<Zezex.Provider.BActionParam>(getObjectId() + 6, _v -> new Log__Actions(this, _v));
	}

	public void Assign(BModuleRedirectResult other) {
		setModuleId(other.getModuleId());
		setServerId(other.getServerId());
		setReturnCode(other.getReturnCode());
		setParams(other.getParams());
		getActions().clear();
		for (var e : other.getActions()) {
			getActions().add(e.Copy());
		}
	}

	public BModuleRedirectResult CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BModuleRedirectResult Copy() {
		var copy = new BModuleRedirectResult();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BModuleRedirectResult a, BModuleRedirectResult b) {
		BModuleRedirectResult save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -8455678873047944655;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__ModuleId extends Zeze.Transaction.Log<BModuleRedirectResult, Integer> {
		public Log__ModuleId(BModuleRedirectResult self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ModuleId = this.getValue();
		}
	}

	private final static class Log__ServerId extends Zeze.Transaction.Log<BModuleRedirectResult, Integer> {
		public Log__ServerId(BModuleRedirectResult self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ServerId = this.getValue();
		}
	}

	private final static class Log__ReturnCode extends Zeze.Transaction.Log<BModuleRedirectResult, Integer> {
		public Log__ReturnCode(BModuleRedirectResult self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 4;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ReturnCode = this.getValue();
		}
	}

	private final static class Log__Params extends Zeze.Transaction.Log<BModuleRedirectResult, Zeze.Net.Binary> {
		public Log__Params(BModuleRedirectResult self, Zeze.Net.Binary value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 5;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Params = this.getValue();
		}
	}

	private final static class Log__Actions extends Zeze.Transaction.Collections.PList2<Zezex.Provider.BActionParam>.LogV {
		public Log__Actions(BModuleRedirectResult host, System.Collections.Immutable.ImmutableList<Zezex.Provider.BActionParam> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 6;
		}
		public BModuleRedirectResult getBeanTyped() {
			return (BModuleRedirectResult)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._Actions);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		BuildString(sb, 0);
		sb.append(System.lineSeparator());
		return sb.toString();
	}

	@Override
	public void BuildString(StringBuilder sb, int level) {
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Provider.BModuleRedirectResult: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ModuleId").Append("=").Append(getModuleId()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ServerId").Append("=").Append(getServerId()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ReturnCode").Append("=").Append(getReturnCode()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Params").Append("=").Append(getParams()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Actions").Append("=[").Append(System.lineSeparator());
		level++;
		for (var Item : getActions()) {
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Item").Append("=").Append(System.lineSeparator());
			Item.BuildString(sb, level + 1);
			sb.append(",").Append(System.lineSeparator());
		}
		level--;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("]").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(5); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getModuleId());
		_os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getServerId());
		_os_.WriteInt(ByteBuffer.INT | 4 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getReturnCode());
		_os_.WriteInt(ByteBuffer.BYTES | 5 << ByteBuffer.TAG_SHIFT);
		_os_.WriteBinary(getParams());
		_os_.WriteInt(ByteBuffer.LIST | 6 << ByteBuffer.TAG_SHIFT); {
			int _state_;
			tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
			_os_.BeginWriteSegment(tempOut__state_);
		_state_ = tempOut__state_.outArgValue;
			_os_.WriteInt(ByteBuffer.BEAN);
			_os_.WriteInt(getActions().size());
			for (var _v_ : getActions()) {
				_v_.Encode(_os_);
			}
			_os_.EndWriteSegment(_state_);
		}
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					setModuleId(_os_.ReadInt());
					break;
				case ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT:
					setServerId(_os_.ReadInt());
					break;
				case ByteBuffer.INT | 4 << ByteBuffer.TAG_SHIFT:
					setReturnCode(_os_.ReadInt());
					break;
				case ByteBuffer.BYTES | 5 << ByteBuffer.TAG_SHIFT:
					setParams(_os_.ReadBinary());
					break;
				case ByteBuffer.LIST | 6 << ByteBuffer.TAG_SHIFT: {
						int _state_;
						tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
						_os_.BeginReadSegment(tempOut__state_);
					_state_ = tempOut__state_.outArgValue;
						_os_.ReadInt(); // skip collection.value typetag
						getActions().clear();
						for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
							Zezex.Provider.BActionParam _v_ = new Zezex.Provider.BActionParam();
							_v_.Decode(_os_);
							getActions().add(_v_);
						}
						_os_.EndReadSegment(_state_);
				}
					break;
				default:
					ByteBuffer.SkipUnknownField(_tagid_, _os_);
					break;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
		_Actions.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		if (getModuleId() < 0) {
			return true;
		}
		if (getServerId() < 0) {
			return true;
		}
		if (getReturnCode() < 0) {
			return true;
		}
		return false;
	}

}