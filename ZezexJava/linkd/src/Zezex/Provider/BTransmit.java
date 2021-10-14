package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

public final class BTransmit extends Zeze.Transaction.Bean implements BTransmitReadOnly {
	private String _ActionName;
	private Zeze.Transaction.Collections.PMap2<Long, Zezex.Provider.BTransmitContext> _Roles; // 查询目标角色。
	private Zeze.Transaction.Collections.PMapReadOnly<Long,Zezex.Provider.BTransmitContextReadOnly,Zezex.Provider.BTransmitContext> _RolesReadOnly;
	private long _Sender; // 结果发送给Sender。
	private String _ServiceNamePrefix;

	public String getActionName() {
		if (false == this.isManaged()) {
			return _ActionName;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ActionName;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ActionName)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _ActionName;
	}
	public void setActionName(String value) {
		if (value.equals(null)) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_ActionName = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ActionName(this, value));
	}

	public Zeze.Transaction.Collections.PMap2<Long, Zezex.Provider.BTransmitContext> getRoles() {
		return _Roles;
	}
	private System.Collections.Generic.IReadOnlyDictionary<Long,Zezex.Provider.BTransmitContextReadOnly> Zezex.Provider.BTransmitReadOnly.Roles -> _RolesReadOnly;

	public long getSender() {
		if (false == this.isManaged()) {
			return _Sender;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Sender;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Sender)txn.GetLog(this.getObjectId() + 3);
		return log != null ? log.getValue() : _Sender;
	}
	public void setSender(long value) {
		if (false == this.isManaged()) {
			_Sender = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Sender(this, value));
	}

	public String getServiceNamePrefix() {
		if (false == this.isManaged()) {
			return _ServiceNamePrefix;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ServiceNamePrefix;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ServiceNamePrefix)txn.GetLog(this.getObjectId() + 4);
		return log != null ? log.getValue() : _ServiceNamePrefix;
	}
	public void setServiceNamePrefix(String value) {
		if (value.equals(null)) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_ServiceNamePrefix = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ServiceNamePrefix(this, value));
	}


	public BTransmit() {
		this(0);
	}

	public BTransmit(int _varId_) {
		super(_varId_);
		_ActionName = "";
		_Roles = new Zeze.Transaction.Collections.PMap2<Long, Zezex.Provider.BTransmitContext>(getObjectId() + 2, _v -> new Log__Roles(this, _v));
		_RolesReadOnly = new Zeze.Transaction.Collections.PMapReadOnly<Long,Zezex.Provider.BTransmitContextReadOnly,Zezex.Provider.BTransmitContext>(_Roles);
		_ServiceNamePrefix = "";
	}

	public void Assign(BTransmit other) {
		setActionName(other.getActionName());
		getRoles().Clear();
		for (var e : other.getRoles()) {
			getRoles().Add(e.Key, e.Value.Copy());
		}
		setSender(other.getSender());
		setServiceNamePrefix(other.getServiceNamePrefix());
	}

	public BTransmit CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BTransmit Copy() {
		var copy = new BTransmit();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BTransmit a, BTransmit b) {
		BTransmit save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 1899659324986950870;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__ActionName extends Zeze.Transaction.Log<BTransmit, String> {
		public Log__ActionName(BTransmit self, String value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ActionName = this.getValue();
		}
	}

	private final static class Log__Roles extends Zeze.Transaction.Collections.PMap2<Long, Zezex.Provider.BTransmitContext>.LogV {
		public Log__Roles(BTransmit host, System.Collections.Immutable.ImmutableDictionary<Long, Zezex.Provider.BTransmitContext> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 2;
		}
		public BTransmit getBeanTyped() {
			return (BTransmit)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._Roles);
		}
	}

	private final static class Log__Sender extends Zeze.Transaction.Log<BTransmit, Long> {
		public Log__Sender(BTransmit self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 3;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Sender = this.getValue();
		}
	}

	private final static class Log__ServiceNamePrefix extends Zeze.Transaction.Log<BTransmit, String> {
		public Log__ServiceNamePrefix(BTransmit self, String value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 4;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ServiceNamePrefix = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Provider.BTransmit: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ActionName").Append("=").Append(getActionName()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Roles").Append("=[").Append(System.lineSeparator());
		level++;
		for (var _kv_ : getRoles()) {
			sb.append("(").Append(System.lineSeparator());
			var Key = _kv_.Key;
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Key").Append("=").Append(Key).Append(",").Append(System.lineSeparator());
			var Value = _kv_.Value;
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Value").Append("=").Append(System.lineSeparator());
			Value.BuildString(sb, level + 1);
			sb.append(",").Append(System.lineSeparator());
			sb.append(")").Append(System.lineSeparator());
		}
		level--;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("],").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Sender").Append("=").Append(getSender()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ServiceNamePrefix").Append("=").Append(getServiceNamePrefix()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(4); // Variables.Count
		_os_.WriteInt(ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteString(getActionName());
		_os_.WriteInt(ByteBuffer.MAP | 2 << ByteBuffer.TAG_SHIFT); {
			int _state_;
			tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
			_os_.BeginWriteSegment(tempOut__state_);
		_state_ = tempOut__state_.outArgValue;
			_os_.WriteInt(ByteBuffer.LONG);
			_os_.WriteInt(ByteBuffer.BEAN);
			_os_.WriteInt(getRoles().Count);
			for (var _e_ : getRoles()) {
				_os_.WriteLong(_e_.Key);
				_e_.Value.Encode(_os_);
			}
			_os_.EndWriteSegment(_state_);
		}
		_os_.WriteInt(ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getSender());
		_os_.WriteInt(ByteBuffer.STRING | 4 << ByteBuffer.TAG_SHIFT);
		_os_.WriteString(getServiceNamePrefix());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT:
					setActionName(_os_.ReadString());
					break;
				case ByteBuffer.MAP | 2 << ByteBuffer.TAG_SHIFT: {
						int _state_;
						tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
						_os_.BeginReadSegment(tempOut__state_);
					_state_ = tempOut__state_.outArgValue;
						_os_.ReadInt(); // skip key typetag
						_os_.ReadInt(); // skip value typetag
						getRoles().Clear();
						for (int size = _os_.ReadInt(); size > 0; --size) {
							long _k_;
							_k_ = _os_.ReadLong();
							Zezex.Provider.BTransmitContext _v_ = new Zezex.Provider.BTransmitContext();
							_v_.Decode(_os_);
							getRoles().Add(_k_, _v_);
						}
						_os_.EndReadSegment(_state_);
				}
					break;
				case ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT:
					setSender(_os_.ReadLong());
					break;
				case ByteBuffer.STRING | 4 << ByteBuffer.TAG_SHIFT:
					setServiceNamePrefix(_os_.ReadString());
					break;
				default:
					ByteBuffer.SkipUnknownField(_tagid_, _os_);
					break;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
		_Roles.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		for (var _v_ : getRoles().Values) {
			if (_v_.NegativeCheck()) {
				return true;
			}
		}
		if (getSender() < 0) {
			return true;
		}
		return false;
	}

}