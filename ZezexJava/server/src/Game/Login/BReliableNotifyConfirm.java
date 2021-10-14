package Game.Login;

import Zeze.Serialize.*;
import Game.*;

public final class BReliableNotifyConfirm extends Zeze.Transaction.Bean implements BReliableNotifyConfirmReadOnly {
	private long _ReliableNotifyConfirmCount;

	public long getReliableNotifyConfirmCount() {
		if (false == this.isManaged()) {
			return _ReliableNotifyConfirmCount;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ReliableNotifyConfirmCount;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ReliableNotifyConfirmCount)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _ReliableNotifyConfirmCount;
	}
	public void setReliableNotifyConfirmCount(long value) {
		if (false == this.isManaged()) {
			_ReliableNotifyConfirmCount = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ReliableNotifyConfirmCount(this, value));
	}


	public BReliableNotifyConfirm() {
		this(0);
	}

	public BReliableNotifyConfirm(int _varId_) {
		super(_varId_);
	}

	public void Assign(BReliableNotifyConfirm other) {
		setReliableNotifyConfirmCount(other.getReliableNotifyConfirmCount());
	}

	public BReliableNotifyConfirm CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BReliableNotifyConfirm Copy() {
		var copy = new BReliableNotifyConfirm();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BReliableNotifyConfirm a, BReliableNotifyConfirm b) {
		BReliableNotifyConfirm save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -4569350877179575427;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__ReliableNotifyConfirmCount extends Zeze.Transaction.Log<BReliableNotifyConfirm, Long> {
		public Log__ReliableNotifyConfirmCount(BReliableNotifyConfirm self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ReliableNotifyConfirmCount = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Login.BReliableNotifyConfirm: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ReliableNotifyConfirmCount").Append("=").Append(getReliableNotifyConfirmCount()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(1); // Variables.Count
		_os_.WriteInt(ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getReliableNotifyConfirmCount());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT:
					setReliableNotifyConfirmCount(_os_.ReadLong());
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
		if (getReliableNotifyConfirmCount() < 0) {
			return true;
		}
		return false;
	}

}