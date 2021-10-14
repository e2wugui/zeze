package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

public final class BSetUserState extends Zeze.Transaction.Bean implements BSetUserStateReadOnly {
	private long _linkSid;
	private Zeze.Transaction.Collections.PList1<Long> _states;
	private Zeze.Net.Binary _statex;

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

	public Zeze.Transaction.Collections.PList1<Long> getStates() {
		return _states;
	}
	private System.Collections.Generic.IReadOnlyList<Long> Zezex.Provider.BSetUserStateReadOnly.States -> _states;

	public Zeze.Net.Binary getStatex() {
		if (false == this.isManaged()) {
			return _statex;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _statex;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__statex)txn.GetLog(this.getObjectId() + 3);
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


	public BSetUserState() {
		this(0);
	}

	public BSetUserState(int _varId_) {
		super(_varId_);
		_states = new Zeze.Transaction.Collections.PList1<Long>(getObjectId() + 2, _v -> new Log__states(this, _v));
		_statex = Zeze.Net.Binary.Empty;
	}

	public void Assign(BSetUserState other) {
		setLinkSid(other.getLinkSid());
		getStates().clear();
		for (var e : other.getStates()) {
			getStates().add(e);
		}
		setStatex(other.getStatex());
	}

	public BSetUserState CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BSetUserState Copy() {
		var copy = new BSetUserState();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BSetUserState a, BSetUserState b) {
		BSetUserState save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 1067080601247567282;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__linkSid extends Zeze.Transaction.Log<BSetUserState, Long> {
		public Log__linkSid(BSetUserState self, long value) {
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

	private final static class Log__states extends Zeze.Transaction.Collections.PList1<Long>.LogV {
		public Log__states(BSetUserState host, System.Collections.Immutable.ImmutableList<Long> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 2;
		}
		public BSetUserState getBeanTyped() {
			return (BSetUserState)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._states);
		}
	}

	private final static class Log__statex extends Zeze.Transaction.Log<BSetUserState, Zeze.Net.Binary> {
		public Log__statex(BSetUserState self, Zeze.Net.Binary value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 3;
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Provider.BSetUserState: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("LinkSid").Append("=").Append(getLinkSid()).Append(",").Append(System.lineSeparator());
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
		_os_.WriteInt(3); // Variables.Count
		_os_.WriteInt(ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getLinkSid());
		_os_.WriteInt(ByteBuffer.LIST | 2 << ByteBuffer.TAG_SHIFT); {
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
		_os_.WriteInt(ByteBuffer.BYTES | 3 << ByteBuffer.TAG_SHIFT);
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
				case ByteBuffer.LIST | 2 << ByteBuffer.TAG_SHIFT: {
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
				case ByteBuffer.BYTES | 3 << ByteBuffer.TAG_SHIFT:
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
		for (var _v_ : getStates()) {
			if (_v_ < 0) {
				return true;
			}
		}
		return false;
	}

}