package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

public final class BLoad extends Zeze.Transaction.Bean implements BLoadReadOnly {
	private int _Online; // 用户数量
	private int _ProposeMaxOnline; // 建议最大用户数量
	private int _OnlineNew; // 最近上线用户数量，一般是一秒内的。用来防止短时间内给同一个gs分配太多用户。

	public int getOnline() {
		if (false == this.isManaged()) {
			return _Online;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Online;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Online)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _Online;
	}
	public void setOnline(int value) {
		if (false == this.isManaged()) {
			_Online = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Online(this, value));
	}

	public int getProposeMaxOnline() {
		if (false == this.isManaged()) {
			return _ProposeMaxOnline;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ProposeMaxOnline;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ProposeMaxOnline)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _ProposeMaxOnline;
	}
	public void setProposeMaxOnline(int value) {
		if (false == this.isManaged()) {
			_ProposeMaxOnline = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ProposeMaxOnline(this, value));
	}

	public int getOnlineNew() {
		if (false == this.isManaged()) {
			return _OnlineNew;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _OnlineNew;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__OnlineNew)txn.GetLog(this.getObjectId() + 3);
		return log != null ? log.getValue() : _OnlineNew;
	}
	public void setOnlineNew(int value) {
		if (false == this.isManaged()) {
			_OnlineNew = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__OnlineNew(this, value));
	}


	public BLoad() {
		this(0);
	}

	public BLoad(int _varId_) {
		super(_varId_);
	}

	public void Assign(BLoad other) {
		setOnline(other.getOnline());
		setProposeMaxOnline(other.getProposeMaxOnline());
		setOnlineNew(other.getOnlineNew());
	}

	public BLoad CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BLoad Copy() {
		var copy = new BLoad();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BLoad a, BLoad b) {
		BLoad save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -859605390399287264;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Online extends Zeze.Transaction.Log<BLoad, Integer> {
		public Log__Online(BLoad self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Online = this.getValue();
		}
	}

	private final static class Log__ProposeMaxOnline extends Zeze.Transaction.Log<BLoad, Integer> {
		public Log__ProposeMaxOnline(BLoad self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ProposeMaxOnline = this.getValue();
		}
	}

	private final static class Log__OnlineNew extends Zeze.Transaction.Log<BLoad, Integer> {
		public Log__OnlineNew(BLoad self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 3;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._OnlineNew = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Provider.BLoad: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Online").Append("=").Append(getOnline()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ProposeMaxOnline").Append("=").Append(getProposeMaxOnline()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("OnlineNew").Append("=").Append(getOnlineNew()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(3); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getOnline());
		_os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getProposeMaxOnline());
		_os_.WriteInt(ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getOnlineNew());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					setOnline(_os_.ReadInt());
					break;
				case ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT:
					setProposeMaxOnline(_os_.ReadInt());
					break;
				case ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT:
					setOnlineNew(_os_.ReadInt());
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
		if (getOnline() < 0) {
			return true;
		}
		if (getProposeMaxOnline() < 0) {
			return true;
		}
		if (getOnlineNew() < 0) {
			return true;
		}
		return false;
	}

}