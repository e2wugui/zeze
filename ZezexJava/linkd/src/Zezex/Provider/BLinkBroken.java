package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

public final class BLinkBroken extends Zeze.Transaction.Bean implements BLinkBrokenReadOnly {
	public static final int REASON_PEERCLOSE = 0;

	private String _account;
	private long _linkSid;
	private int _reason;
	private Zeze.Transaction.Collections.PList1<Long> _states; // SetUserState
	private Zeze.Net.Binary _statex; // SetUserState

	public String getAccount() {
		if (false == this.isManaged()) {
			return _account;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _account;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__account)txn.GetLog(this.getObjectId() + 1);
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

	public long getLinkSid() {
		if (false == this.isManaged()) {
			return _linkSid;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _linkSid;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__linkSid)txn.GetLog(this.getObjectId() + 2);
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

	public int getReason() {
		if (false == this.isManaged()) {
			return _reason;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _reason;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__reason)txn.GetLog(this.getObjectId() + 3);
		return log != null ? log.getValue() : _reason;
	}
	public void setReason(int value) {
		if (false == this.isManaged()) {
			_reason = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__reason(this, value));
	}

	public Zeze.Transaction.Collections.PList1<Long> getStates() {
		return _states;
	}
	private System.Collections.Generic.IReadOnlyList<Long> Zezex.Provider.BLinkBrokenReadOnly.States -> _states;

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


	public BLinkBroken() {
		this(0);
	}

	public BLinkBroken(int _varId_) {
		super(_varId_);
		_account = "";
		_states = new Zeze.Transaction.Collections.PList1<Long>(getObjectId() + 5, _v -> new Log__states(this, _v));
		_statex = Zeze.Net.Binary.Empty;
	}

	public void Assign(BLinkBroken other) {
		setAccount(other.getAccount());
		setLinkSid(other.getLinkSid());
		setReason(other.getReason());
		getStates().clear();
		for (var e : other.getStates()) {
			getStates().add(e);
		}
		setStatex(other.getStatex());
	}

	public BLinkBroken CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BLinkBroken Copy() {
		var copy = new BLinkBroken();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BLinkBroken a, BLinkBroken b) {
		BLinkBroken save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 5003488678339236183;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__account extends Zeze.Transaction.Log<BLinkBroken, String> {
		public Log__account(BLinkBroken self, String value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._account = this.getValue();
		}
	}

	private final static class Log__linkSid extends Zeze.Transaction.Log<BLinkBroken, Long> {
		public Log__linkSid(BLinkBroken self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._linkSid = this.getValue();
		}
	}

	private final static class Log__reason extends Zeze.Transaction.Log<BLinkBroken, Integer> {
		public Log__reason(BLinkBroken self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 3;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._reason = this.getValue();
		}
	}

	private final static class Log__states extends Zeze.Transaction.Collections.PList1<Long>.LogV {
		public Log__states(BLinkBroken host, System.Collections.Immutable.ImmutableList<Long> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 5;
		}
		public BLinkBroken getBeanTyped() {
			return (BLinkBroken)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._states);
		}
	}

	private final static class Log__statex extends Zeze.Transaction.Log<BLinkBroken, Zeze.Net.Binary> {
		public Log__statex(BLinkBroken self, Zeze.Net.Binary value) {
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Provider.BLinkBroken: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Account").Append("=").Append(getAccount()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("LinkSid").Append("=").Append(getLinkSid()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Reason").Append("=").Append(getReason()).Append(",").Append(System.lineSeparator());
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
		_os_.WriteInt(5); // Variables.Count
		_os_.WriteInt(ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteString(getAccount());
		_os_.WriteInt(ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getLinkSid());
		_os_.WriteInt(ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getReason());
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
				case ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT:
					setAccount(_os_.ReadString());
					break;
				case ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT:
					setLinkSid(_os_.ReadLong());
					break;
				case ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT:
					setReason(_os_.ReadInt());
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
		if (getReason() < 0) {
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