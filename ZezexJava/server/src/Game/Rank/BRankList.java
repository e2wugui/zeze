package Game.Rank;

import Zeze.Serialize.*;
import Game.*;

public final class BRankList extends Zeze.Transaction.Bean implements BRankListReadOnly {
	private Zeze.Transaction.Collections.PList2<Game.Rank.BRankValue> _RankList;

	public Zeze.Transaction.Collections.PList2<Game.Rank.BRankValue> getRankList() {
		return _RankList;
	}
	private System.Collections.Generic.IReadOnlyList<Game.Rank.BRankValueReadOnly> Game.Rank.BRankListReadOnly.RankList -> _RankList;


	public BRankList() {
		this(0);
	}

	public BRankList(int _varId_) {
		super(_varId_);
		_RankList = new Zeze.Transaction.Collections.PList2<Game.Rank.BRankValue>(getObjectId() + 1, _v -> new Log__RankList(this, _v));
	}

	public void Assign(BRankList other) {
		getRankList().clear();
		for (var e : other.getRankList()) {
			getRankList().add(e.Copy());
		}
	}

	public BRankList CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BRankList Copy() {
		var copy = new BRankList();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BRankList a, BRankList b) {
		BRankList save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 7673159184186841875;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__RankList extends Zeze.Transaction.Collections.PList2<Game.Rank.BRankValue>.LogV {
		public Log__RankList(BRankList host, System.Collections.Immutable.ImmutableList<Game.Rank.BRankValue> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 1;
		}
		public BRankList getBeanTyped() {
			return (BRankList)Bean;
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Rank.BRankList: {").Append(System.lineSeparator());
		level++;
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
		_os_.WriteInt(1); // Variables.Count
		_os_.WriteInt(ByteBuffer.LIST | 1 << ByteBuffer.TAG_SHIFT); {
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
				case ByteBuffer.LIST | 1 << ByteBuffer.TAG_SHIFT: {
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
		for (var _v_ : getRankList()) {
			if (_v_.NegativeCheck()) {
				return true;
			}
		}
		return false;
	}

}