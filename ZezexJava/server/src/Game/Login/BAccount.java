package Game.Login;

import Zeze.Serialize.*;
import Game.*;

public final class BAccount extends Zeze.Transaction.Bean implements BAccountReadOnly {
	private String _Name;
	private Zeze.Transaction.Collections.PList1<Long> _Roles; // roleid list
	private long _LastLoginRoleId;

	public String getName() {
		if (false == this.isManaged()) {
			return _Name;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Name;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Name)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _Name;
	}
	public void setName(String value) {
		if (value.equals(null)) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_Name = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Name(this, value));
	}

	public Zeze.Transaction.Collections.PList1<Long> getRoles() {
		return _Roles;
	}
	private System.Collections.Generic.IReadOnlyList<Long> Game.Login.BAccountReadOnly.Roles -> _Roles;

	public long getLastLoginRoleId() {
		if (false == this.isManaged()) {
			return _LastLoginRoleId;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _LastLoginRoleId;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__LastLoginRoleId)txn.GetLog(this.getObjectId() + 3);
		return log != null ? log.getValue() : _LastLoginRoleId;
	}
	public void setLastLoginRoleId(long value) {
		if (false == this.isManaged()) {
			_LastLoginRoleId = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__LastLoginRoleId(this, value));
	}


	public BAccount() {
		this(0);
	}

	public BAccount(int _varId_) {
		super(_varId_);
		_Name = "";
		_Roles = new Zeze.Transaction.Collections.PList1<Long>(getObjectId() + 2, _v -> new Log__Roles(this, _v));
	}

	public void Assign(BAccount other) {
		setName(other.getName());
		getRoles().clear();
		for (var e : other.getRoles()) {
			getRoles().add(e);
		}
		setLastLoginRoleId(other.getLastLoginRoleId());
	}

	public BAccount CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BAccount Copy() {
		var copy = new BAccount();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BAccount a, BAccount b) {
		BAccount save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -3313986381974074771;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Name extends Zeze.Transaction.Log<BAccount, String> {
		public Log__Name(BAccount self, String value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Name = this.getValue();
		}
	}

	private final static class Log__Roles extends Zeze.Transaction.Collections.PList1<Long>.LogV {
		public Log__Roles(BAccount host, System.Collections.Immutable.ImmutableList<Long> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 2;
		}
		public BAccount getBeanTyped() {
			return (BAccount)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._Roles);
		}
	}

	private final static class Log__LastLoginRoleId extends Zeze.Transaction.Log<BAccount, Long> {
		public Log__LastLoginRoleId(BAccount self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 3;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._LastLoginRoleId = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Login.BAccount: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Name").Append("=").Append(getName()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Roles").Append("=[").Append(System.lineSeparator());
		level++;
		for (var Item : getRoles()) {
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Item").Append("=").Append(Item).Append(",").Append(System.lineSeparator());
		}
		level--;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("],").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("LastLoginRoleId").Append("=").Append(getLastLoginRoleId()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(3); // Variables.Count
		_os_.WriteInt(ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteString(getName());
		_os_.WriteInt(ByteBuffer.LIST | 2 << ByteBuffer.TAG_SHIFT); {
			int _state_;
			tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
			_os_.BeginWriteSegment(tempOut__state_);
		_state_ = tempOut__state_.outArgValue;
			_os_.WriteInt(ByteBuffer.LONG);
			_os_.WriteInt(getRoles().size());
			for (var _v_ : getRoles()) {
				_os_.WriteLong(_v_);
			}
			_os_.EndWriteSegment(_state_);
		}
		_os_.WriteInt(ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getLastLoginRoleId());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT:
					setName(_os_.ReadString());
					break;
				case ByteBuffer.LIST | 2 << ByteBuffer.TAG_SHIFT: {
						int _state_;
						tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
						_os_.BeginReadSegment(tempOut__state_);
					_state_ = tempOut__state_.outArgValue;
						_os_.ReadInt(); // skip collection.value typetag
						getRoles().clear();
						for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
							long _v_;
							_v_ = _os_.ReadLong();
							getRoles().add(_v_);
						}
						_os_.EndReadSegment(_state_);
				}
					break;
				case ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT:
					setLastLoginRoleId(_os_.ReadLong());
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
		for (var _v_ : getRoles()) {
			if (_v_ < 0) {
				return true;
			}
		}
		if (getLastLoginRoleId() < 0) {
			return true;
		}
		return false;
	}

}