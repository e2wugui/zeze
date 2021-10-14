package Zezex.Linkd;

import Zeze.Serialize.*;
import Zezex.*;

public final class BReportError extends Zeze.Transaction.Bean implements BReportErrorReadOnly {
	public static final int FromLink = 0;
	public static final int FromProvider = 1;
	public static final int CodeNotAuthed = 1;
	public static final int CodeNoProvider = 2;

	private int _from;
	private int _code;
	private String _desc;

	public int getFrom() {
		if (false == this.isManaged()) {
			return _from;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _from;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__from)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _from;
	}
	public void setFrom(int value) {
		if (false == this.isManaged()) {
			_from = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__from(this, value));
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


	public BReportError() {
		this(0);
	}

	public BReportError(int _varId_) {
		super(_varId_);
		_desc = "";
	}

	public void Assign(BReportError other) {
		setFrom(other.getFrom());
		setCode(other.getCode());
		setDesc(other.getDesc());
	}

	public BReportError CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BReportError Copy() {
		var copy = new BReportError();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BReportError a, BReportError b) {
		BReportError save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -1491646665364668491;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__from extends Zeze.Transaction.Log<BReportError, Integer> {
		public Log__from(BReportError self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._from = this.getValue();
		}
	}

	private final static class Log__code extends Zeze.Transaction.Log<BReportError, Integer> {
		public Log__code(BReportError self, int value) {
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

	private final static class Log__desc extends Zeze.Transaction.Log<BReportError, String> {
		public Log__desc(BReportError self, String value) {
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Linkd.BReportError: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("From").Append("=").Append(getFrom()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Code").Append("=").Append(getCode()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Desc").Append("=").Append(getDesc()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(3); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getFrom());
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
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					setFrom(_os_.ReadInt());
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
		if (getFrom() < 0) {
			return true;
		}
		if (getCode() < 0) {
			return true;
		}
		return false;
	}

}