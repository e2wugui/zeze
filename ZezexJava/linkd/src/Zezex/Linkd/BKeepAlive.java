package Zezex.Linkd;

import Zeze.Serialize.*;
import Zezex.*;

public final class BKeepAlive extends Zeze.Transaction.Bean implements BKeepAliveReadOnly {
	private long _timestamp; // 客户端发上来，服务器原样放回。

	public long getTimestamp() {
		if (false == this.isManaged()) {
			return _timestamp;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _timestamp;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__timestamp)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _timestamp;
	}
	public void setTimestamp(long value) {
		if (false == this.isManaged()) {
			_timestamp = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__timestamp(this, value));
	}


	public BKeepAlive() {
		this(0);
	}

	public BKeepAlive(int _varId_) {
		super(_varId_);
	}

	public void Assign(BKeepAlive other) {
		setTimestamp(other.getTimestamp());
	}

	public BKeepAlive CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BKeepAlive Copy() {
		var copy = new BKeepAlive();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BKeepAlive a, BKeepAlive b) {
		BKeepAlive save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 8485573885308318241;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__timestamp extends Zeze.Transaction.Log<BKeepAlive, Long> {
		public Log__timestamp(BKeepAlive self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._timestamp = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Linkd.BKeepAlive: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Timestamp").Append("=").Append(getTimestamp()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(1); // Variables.Count
		_os_.WriteInt(ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getTimestamp());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT:
					setTimestamp(_os_.ReadLong());
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
		if (getTimestamp() < 0) {
			return true;
		}
		return false;
	}

}