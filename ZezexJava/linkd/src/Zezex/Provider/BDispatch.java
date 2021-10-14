package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

public final class BDispatch extends Zeze.Transaction.Bean implements BDispatchReadOnly {
	private long _linkSid;
	private String _account;
	private int _protocolType;
	private Zeze.Net.Binary _protocolData; // 协议打包，不包括 type, size
	private Zeze.Transaction.Collections.PList1<Long> _states; // SetUserState
	private Zeze.Net.Binary _statex; // SetUserState

	public long getLinkSid() {
		if (false == this.isManaged()) {
			return _linkSid;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _linkSid;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__linkSid)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _linkSid;
	}
	public void setLinkSid(long value) {
		if (false == this.isManaged()) {
			_linkSid = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__linkSid(this, value));
	}

	public String getAccount() {
		if (false == this.isManaged()) {
			return _account;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _account;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__account)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _account;
	}
	public void setAccount(String value) {
		if (value.equals(null)) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_account = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__account(this, value));
	}

	public int getProtocolType() {
		if (false == this.isManaged()) {
			return _protocolType;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _protocolType;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__protocolType)txn.GetLog(this.getObjectId() + 3);
		return log != null ? log.getValue() : _protocolType;
	}
	public void setProtocolType(int value) {
		if (false == this.isManaged()) {
			_protocolType = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__protocolType(this, value));
	}

	public Zeze.Net.Binary getProtocolData() {
		if (false == this.isManaged()) {
			return _protocolData;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _protocolData;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__protocolData)txn.GetLog(this.getObjectId() + 4);
		return log != null ? log.getValue() : _protocolData;
	}
	public void setProtocolData(Zeze.Net.Binary value) {
		if (null == value) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_protocolData = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__protocolData(this, value));
	}

	public Zeze.Transaction.Collections.PList1<Long> getStates() {
		return _states;
	}
	private System.Collections.Generic.IReadOnlyList<Long> Zezex.Provider.BDispatchReadOnly.States -> _states;

	public Zeze.Net.Binary getStatex() {
		if (false == this.isManaged()) {
			return _statex;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _statex;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__statex)txn.GetLog(this.getObjectId() + 6);
		return log != null ? log.getValue() : _statex;
	}
	public void setStatex(Zeze.Net.Binary value) {
		if (null == value) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_statex = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__statex(this, value));
	}


	public BDispatch() {
		this(0);
	}

	public BDispatch(int _varId_) {
		super(_varId_);
		_account = "";
		_protocolData = Zeze.Net.Binary.Empty;
		_states = new Zeze.Transaction.Collections.PList1<Long>(getObjectId() + 5, _v -> new Log__states(this, _v));
		_statex = Zeze.Net.Binary.Empty;
	}

	public void Assign(BDispatch other) {
		setLinkSid(other.getLinkSid());
		setAccount(other.getAccount());
		setProtocolType(other.getProtocolType());
		setProtocolData(other.getProtocolData());
		getStates().clear();
		for (var e : other.getStates()) {
			getStates().add(e);
		}
		setStatex(other.getStatex());
	}

	public BDispatch CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BDispatch Copy() {
		var copy = new BDispatch();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BDispatch a, BDispatch b) {
		BDispatch save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 5741746203543905036;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__linkSid extends Zeze.Transaction.Log<BDispatch, Long> {
		public Log__linkSid(BDispatch self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._linkSid = this.getValue();
		}
	}

	private final static class Log__account extends Zeze.Transaction.Log<BDispatch, String> {
		public Log__account(BDispatch self, String value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._account = this.getValue();
		}
	}

	private final static class Log__protocolType extends Zeze.Transaction.Log<BDispatch, Integer> {
		public Log__protocolType(BDispatch self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 3;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._protocolType = this.getValue();
		}
	}

	private final static class Log__protocolData extends Zeze.Transaction.Log<BDispatch, Zeze.Net.Binary> {
		public Log__protocolData(BDispatch self, Zeze.Net.Binary value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 4;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._protocolData = this.getValue();
		}
	}

	private final static class Log__states extends Zeze.Transaction.Collections.PList1<Long>.LogV {
		public Log__states(BDispatch host, System.Collections.Immutable.ImmutableList<Long> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 5;
		}
		public BDispatch getBeanTyped() {
			return (BDispatch)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._states);
		}
	}

	private final static class Log__statex extends Zeze.Transaction.Log<BDispatch, Zeze.Net.Binary> {
		public Log__statex(BDispatch self, Zeze.Net.Binary value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 6;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._statex = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Provider.BDispatch: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("LinkSid").Append("=").Append(getLinkSid()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Account").Append("=").Append(getAccount()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ProtocolType").Append("=").Append(getProtocolType()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ProtocolData").Append("=").Append(getProtocolData()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("States").Append("=[").Append(System.lineSeparator());
		level++;
		for (var Item : getStates()) {
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Item").Append("=").Append(Item).Append(",").Append(System.lineSeparator());
		}
		level--;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("],").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Statex").Append("=").Append(getStatex()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(6); // Variables.Count
		_os_.WriteInt(ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getLinkSid());
		_os_.WriteInt(ByteBuffer.STRING | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteString(getAccount());
		_os_.WriteInt(ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getProtocolType());
		_os_.WriteInt(ByteBuffer.BYTES | 4 << ByteBuffer.TAG_SHIFT);
		_os_.WriteBinary(getProtocolData());
		_os_.WriteInt(ByteBuffer.LIST | 5 << ByteBuffer.TAG_SHIFT); {
			int _state_;
			tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
			_os_.BeginWriteSegment(tempOut__state_);
		_state_ = tempOut__state_.outArgValue;
			_os_.WriteInt(ByteBuffer.LONG);
			_os_.WriteInt(getStates().size());
			for (var _v_ : getStates()) {
				_os_.WriteLong(_v_);
			}
			_os_.EndWriteSegment(_state_);
		}
		_os_.WriteInt(ByteBuffer.BYTES | 6 << ByteBuffer.TAG_SHIFT);
		_os_.WriteBinary(getStatex());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT:
					setLinkSid(_os_.ReadLong());
					break;
				case ByteBuffer.STRING | 2 << ByteBuffer.TAG_SHIFT:
					setAccount(_os_.ReadString());
					break;
				case ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT:
					setProtocolType(_os_.ReadInt());
					break;
				case ByteBuffer.BYTES | 4 << ByteBuffer.TAG_SHIFT:
					setProtocolData(_os_.ReadBinary());
					break;
				case ByteBuffer.LIST | 5 << ByteBuffer.TAG_SHIFT: {
						int _state_;
						tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
						_os_.BeginReadSegment(tempOut__state_);
					_state_ = tempOut__state_.outArgValue;
						_os_.ReadInt(); // skip collection.value typetag
						getStates().clear();
						for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
							long _v_;
							_v_ = _os_.ReadLong();
							getStates().add(_v_);
						}
						_os_.EndReadSegment(_state_);
				}
					break;
				case ByteBuffer.BYTES | 6 << ByteBuffer.TAG_SHIFT:
					setStatex(_os_.ReadBinary());
					break;
				default:
					ByteBuffer.SkipUnknownField(_tagid_, _os_);
					break;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
		_states.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		if (getLinkSid() < 0) {
			return true;
		}
		if (getProtocolType() < 0) {
			return true;
		}
		for (var _v_ : getStates()) {
			if (_v_ < 0) {
				return true;
			}
		}
		return false;
	}

}