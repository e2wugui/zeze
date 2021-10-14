package Game.Rank;

import Zeze.Serialize.*;
import Game.*;

public final class BRankListResult extends Zeze.Transaction.Bean implements BRankListResultReadOnly {
	private int _RankType;
	private Zeze.Transaction.Collections.PList2<Game.Rank.BRankValue> _RankList;

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

	public Zeze.Transaction.Collections.PList2<Game.Rank.BRankValue> getRankList() {
		return _RankList;
	}
	private System.Collections.Generic.IReadOnlyList<Game.Rank.BRankValueReadOnly> Game.Rank.BRankListResultReadOnly.RankList -> _RankList;


	public BRankListResult() {
		this(0);
	}

	public BRankListResult(int _varId_) {
		super(_varId_);
		_RankList = new Zeze.Transaction.Collections.PList2<Game.Rank.BRankValue>(getObjectId() + 2, _v -> new Log__RankList(this, _v));
	}

	public void Assign(BRankListResult other) {
		setRankType(other.getRankType());
		getRankList().clear();
		for (var e : other.getRankList()) {
			getRankList().add(e.Copy());
		}
	}

	public BRankListResult CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BRankListResult Copy() {
		var copy = new BRankListResult();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BRankListResult a, BRankListResult b) {
		BRankListResult save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -6478045606578198186;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__RankType extends Zeze.Transaction.Log<BRankListResult, Integer> {
		public Log__RankType(BRankListResult self, int value) {
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

	private final static class Log__RankList extends Zeze.Transaction.Collections.PList2<Game.Rank.BRankValue>.LogV {
		public Log__RankList(BRankListResult host, System.Collections.Immutable.ImmutableList<Game.Rank.BRankValue> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 2;
		}
		public BRankListResult getBeanTyped() {
			return (BRankListResult)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._RankList);
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Rank.BRankListResult: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("RankType").Append("=").Append(getRankType()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("RankList").Append("=[").Append(System.lineSeparator());
		level++;
		for (var Item : getRankList()) {
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Item").Append("=").Append(System.lineSeparator());
			Item.BuildString(sb, level + 1);
			sb.append(",").Append(System.lineSeparator());
		}
		level--;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("]").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(2); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getRankType());
		_os_.WriteInt(ByteBuffer.LIST | 2 << ByteBuffer.TAG_SHIFT); {
			int _state_;
			tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
			_os_.BeginWriteSegment(tempOut__state_);
		_state_ = tempOut__state_.outArgValue;
			_os_.WriteInt(ByteBuffer.BEAN);
			_os_.WriteInt(getRankList().size());
			for (var _v_ : getRankList()) {
				_v_.Encode(_os_);
			}
			_os_.EndWriteSegment(_state_);
		}
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					setRankType(_os_.ReadInt());
					break;
				case ByteBuffer.LIST | 2 << ByteBuffer.TAG_SHIFT: {
						int _state_;
						tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
						_os_.BeginReadSegment(tempOut__state_);
					_state_ = tempOut__state_.outArgValue;
						_os_.ReadInt(); // skip collection.value typetag
						getRankList().clear();
						for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
							Game.Rank.BRankValue _v_ = new Game.Rank.BRankValue();
							_v_.Decode(_os_);
							getRankList().add(_v_);
						}
						_os_.EndReadSegment(_state_);
				}
					break;
				default:
					ByteBuffer.SkipUnknownField(_tagid_, _os_);
					break;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
		_RankList.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		if (getRankType() < 0) {
			return true;
		}
		for (var _v_ : getRankList()) {
			if (_v_.NegativeCheck()) {
				return true;
			}
		}
		return false;
	}

}