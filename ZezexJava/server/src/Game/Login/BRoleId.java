package Game.Login;

import Zeze.Serialize.*;
import Game.*;

public final class BRoleId extends Zeze.Transaction.Bean implements BRoleIdReadOnly {
	private long _Id;

	public long getId() {
		if (false == this.isManaged()) {
			return _Id;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Id;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Id)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _Id;
	}
	public void setId(long value) {
		if (false == this.isManaged()) {
			_Id = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Id(this, value));
	}


	public BRoleId() {
		this(0);
	}

	public BRoleId(int _varId_) {
		super(_varId_);
	}

	public void Assign(BRoleId other) {
		setId(other.getId());
	}

	public BRoleId CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BRoleId Copy() {
		var copy = new BRoleId();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BRoleId a, BRoleId b) {
		BRoleId save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 6390242969559454021;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Id extends Zeze.Transaction.Log<BRoleId, Long> {
		public Log__Id(BRoleId self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Id = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Login.BRoleId: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Id").Append("=").Append(getId()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(1); // Variables.Count
		_os_.WriteInt(ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getId());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT:
					setId(_os_.ReadLong());
					break;
				default:
					ByteBuffer.SkipUnknownField(_tagid_, _os_);
					break;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
	}

	@Override
	public boolean NegativeCheck() {
		if (getId() < 0) {
			return true;
		}
		return false;
	}

}