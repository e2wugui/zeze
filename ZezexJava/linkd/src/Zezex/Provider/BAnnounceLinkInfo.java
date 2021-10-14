package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

public final class BAnnounceLinkInfo extends Zeze.Transaction.Bean implements BAnnounceLinkInfoReadOnly {
	private int _LinkId; // reserve
	private long _ProviderSessionId;

	public int getLinkId() {
		if (false == this.isManaged()) {
			return _LinkId;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _LinkId;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__LinkId)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _LinkId;
	}
	public void setLinkId(int value) {
		if (false == this.isManaged()) {
			_LinkId = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__LinkId(this, value));
	}

	public long getProviderSessionId() {
		if (false == this.isManaged()) {
			return _ProviderSessionId;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ProviderSessionId;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ProviderSessionId)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _ProviderSessionId;
	}
	public void setProviderSessionId(long value) {
		if (false == this.isManaged()) {
			_ProviderSessionId = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ProviderSessionId(this, value));
	}


	public BAnnounceLinkInfo() {
		this(0);
	}

	public BAnnounceLinkInfo(int _varId_) {
		super(_varId_);
	}

	public void Assign(BAnnounceLinkInfo other) {
		setLinkId(other.getLinkId());
		setProviderSessionId(other.getProviderSessionId());
	}

	public BAnnounceLinkInfo CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BAnnounceLinkInfo Copy() {
		var copy = new BAnnounceLinkInfo();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BAnnounceLinkInfo a, BAnnounceLinkInfo b) {
		BAnnounceLinkInfo save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 5825166529181261237;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__LinkId extends Zeze.Transaction.Log<BAnnounceLinkInfo, Integer> {
		public Log__LinkId(BAnnounceLinkInfo self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._LinkId = this.getValue();
		}
	}

	private final static class Log__ProviderSessionId extends Zeze.Transaction.Log<BAnnounceLinkInfo, Long> {
		public Log__ProviderSessionId(BAnnounceLinkInfo self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ProviderSessionId = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Provider.BAnnounceLinkInfo: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("LinkId").Append("=").Append(getLinkId()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ProviderSessionId").Append("=").Append(getProviderSessionId()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(2); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getLinkId());
		_os_.WriteInt(ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getProviderSessionId());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					setLinkId(_os_.ReadInt());
					break;
				case ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT:
					setProviderSessionId(_os_.ReadLong());
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
		if (getLinkId() < 0) {
			return true;
		}
		if (getProviderSessionId() < 0) {
			return true;
		}
		return false;
	}

}