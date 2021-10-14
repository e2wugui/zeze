package Game.Rank;

import Zeze.Serialize.*;
import Game.*;

public final class BGetRankList extends Zeze.Transaction.Bean implements BGetRankListReadOnly {
	private int _RankType; // BConcurrentKey.RankTypeXXX
	private int _TimeType; // BConcurrentKey.TimeTypeXXX

	public int getRankType() {
		if (false == this.isManaged()) {
			return _RankType;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _RankType;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__RankType)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _RankType;
	}
	public void setRankType(int value) {
		if (false == this.isManaged()) {
			_RankType = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__RankType(this, value));
	}

	public int getTimeType() {
		if (false == this.isManaged()) {
			return _TimeType;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _TimeType;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__TimeType)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _TimeType;
	}
	public void setTimeType(int value) {
		if (false == this.isManaged()) {
			_TimeType = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__TimeType(this, value));
	}


	public BGetRankList() {
		this(0);
	}

	public BGetRankList(int _varId_) {
		super(_varId_);
	}

	public void Assign(BGetRankList other) {
		setRankType(other.getRankType());
		setTimeType(other.getTimeType());
	}

	public BGetRankList CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BGetRankList Copy() {
		var copy = new BGetRankList();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BGetRankList a, BGetRankList b) {
		BGetRankList save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -8033770711498528893;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__RankType extends Zeze.Transaction.Log<BGetRankList, Integer> {
		public Log__RankType(BGetRankList self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._RankType = this.getValue();
		}
	}

	private final static class Log__TimeType extends Zeze.Transaction.Log<BGetRankList, Integer> {
		public Log__TimeType(BGetRankList self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._TimeType = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Rank.BGetRankList: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("RankType").Append("=").Append(getRankType()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("TimeType").Append("=").Append(getTimeType()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(2); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getRankType());
		_os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getTimeType());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					setRankType(_os_.ReadInt());
					break;
				case ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT:
					setTimeType(_os_.ReadInt());
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
		if (getRankType() < 0) {
			return true;
		}
		if (getTimeType() < 0) {
			return true;
		}
		return false;
	}

}