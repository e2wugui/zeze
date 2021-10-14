package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

public final class BKick extends Zeze.Transaction.Bean implements BKickReadOnly {
	public static final int ErrorProtocolUnkown = 1;
	public static final int ErrorDecode = 2;
	public static final int ErrorProtocolException = 3;

	private long _linksid;
	private int _code;
	private String _desc; // // for debug

	public long getLinksid() {
		if (false == this.isManaged()) {
			return _linksid;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _linksid;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__linksid)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _linksid;
	}
	public void setLinksid(long value) {
		if (false == this.isManaged()) {
			_linksid = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__linksid(this, value));
	}

	public int getCode() {
		if (false == this.isManaged()) {
			return _code;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _code;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__code)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _code;
	}
	public void setCode(int value) {
		if (false == this.isManaged()) {
			_code = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__code(this, value));
	}

	public String getDesc() {
		if (false == this.isManaged()) {
			return _desc;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _desc;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__desc)txn.GetLog(this.getObjectId() + 3);
		return log != null ? log.getValue() : _desc;
	}
	public void setDesc(String value) {
		if (value.equals(null)) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_desc = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__desc(this, value));
	}


	public BKick() {
		this(0);
	}

	public BKick(int _varId_) {
		super(_varId_);
		_desc = "";
	}

	public void Assign(BKick other) {
		setLinksid(other.getLinksid());
		setCode(other.getCode());
		setDesc(other.getDesc());
	}

	public BKick CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BKick Copy() {
		var copy = new BKick();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BKick a, BKick b) {
		BKick save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 1759376791373971536;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__linksid extends Zeze.Transaction.Log<BKick, Long> {
		public Log__linksid(BKick self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._linksid = this.getValue();
		}
	}

	private final static class Log__code extends Zeze.Transaction.Log<BKick, Integer> {
		public Log__code(BKick self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._code = this.getValue();
		}
	}

	private final static class Log__desc extends Zeze.Transaction.Log<BKick, String> {
		public Log__desc(BKick self, String value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 3;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._desc = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Provider.BKick: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Linksid").Append("=").Append(getLinksid()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Code").Append("=").Append(getCode()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Desc").Append("=").Append(getDesc()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(3); // Variables.Count
		_os_.WriteInt(ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getLinksid());
		_os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getCode());
		_os_.WriteInt(ByteBuffer.STRING | 3 << ByteBuffer.TAG_SHIFT);
		_os_.WriteString(getDesc());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT:
					setLinksid(_os_.ReadLong());
					break;
				case ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT:
					setCode(_os_.ReadInt());
					break;
				case ByteBuffer.STRING | 3 << ByteBuffer.TAG_SHIFT:
					setDesc(_os_.ReadString());
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
		if (getLinksid() < 0) {
			return true;
		}
		if (getCode() < 0) {
			return true;
		}
		return false;
	}

}