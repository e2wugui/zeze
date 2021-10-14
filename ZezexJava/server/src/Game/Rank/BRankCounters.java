package Game.Rank;

import Zeze.Serialize.*;
import Game.*;

public final class BRankCounters extends Zeze.Transaction.Bean implements BRankCountersReadOnly {
	private Zeze.Transaction.Collections.PMap2<Game.Rank.BConcurrentKey, Game.Rank.BRankCounter> _Counters;
	private Zeze.Transaction.Collections.PMapReadOnly<Game.Rank.BConcurrentKey,Game.Rank.BRankCounterReadOnly,Game.Rank.BRankCounter> _CountersReadOnly;

	public Zeze.Transaction.Collections.PMap2<Game.Rank.BConcurrentKey, Game.Rank.BRankCounter> getCounters() {
		return _Counters;
	}
	private System.Collections.Generic.IReadOnlyDictionary<Game.Rank.BConcurrentKey,Game.Rank.BRankCounterReadOnly> Game.Rank.BRankCountersReadOnly.Counters -> _CountersReadOnly;


	public BRankCounters() {
		this(0);
	}

	public BRankCounters(int _varId_) {
		super(_varId_);
		_Counters = new Zeze.Transaction.Collections.PMap2<Game.Rank.BConcurrentKey, Game.Rank.BRankCounter>(getObjectId() + 1, _v -> new Log__Counters(this, _v));
		_CountersReadOnly = new Zeze.Transaction.Collections.PMapReadOnly<Game.Rank.BConcurrentKey,Game.Rank.BRankCounterReadOnly,Game.Rank.BRankCounter>(_Counters);
	}

	public void Assign(BRankCounters other) {
		getCounters().Clear();
		for (var e : other.getCounters()) {
			getCounters().Add(e.Key, e.getValue().Copy());
		}
	}

	public BRankCounters CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BRankCounters Copy() {
		var copy = new BRankCounters();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BRankCounters a, BRankCounters b) {
		BRankCounters save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -7316366693928035206;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Counters extends Zeze.Transaction.Collections.PMap2<Game.Rank.BConcurrentKey, Game.Rank.BRankCounter>.LogV {
		public Log__Counters(BRankCounters host, System.Collections.Immutable.ImmutableDictionary<Game.Rank.BConcurrentKey, Game.Rank.BRankCounter> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 1;
		}
		public BRankCounters getBeanTyped() {
			return (BRankCounters)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._Counters);
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Rank.BRankCounters: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Counters").Append("=[").Append(System.lineSeparator());
		level++;
		for (var _kv_ : getCounters()) {
			sb.append("(").Append(System.lineSeparator());
			var Key = _kv_.Key;
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Key").Append("=").Append(System.lineSeparator());
			Key.BuildString(sb, level + 1);
			sb.append(",").Append(System.lineSeparator());
			var Value = _kv_.getValue();
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Value").Append("=").Append(System.lineSeparator());
			Value.BuildString(sb, level + 1);
			sb.append(",").Append(System.lineSeparator());
			sb.append(")").Append(System.lineSeparator());
		}
		level--;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("]").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(1); // Variables.Count
		_os_.WriteInt(ByteBuffer.MAP | 1 << ByteBuffer.TAG_SHIFT); {
			int _state_;
			tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
			_os_.BeginWriteSegment(tempOut__state_);
		_state_ = tempOut__state_.outArgValue;
			_os_.WriteInt(ByteBuffer.BEAN);
			_os_.WriteInt(ByteBuffer.BEAN);
			_os_.WriteInt(getCounters().Count);
			for (var _e_ : getCounters()) {
				_e_.Key.Encode(_os_);
				_e_.getValue().Encode(_os_);
			}
			_os_.EndWriteSegment(_state_);
		}
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.MAP | 1 << ByteBuffer.TAG_SHIFT: {
						int _state_;
						tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
						_os_.BeginReadSegment(tempOut__state_);
					_state_ = tempOut__state_.outArgValue;
						_os_.ReadInt(); // skip key typetag
						_os_.ReadInt(); // skip value typetag
						getCounters().Clear();
						for (int size = _os_.ReadInt(); size > 0; --size) {
							Game.Rank.BConcurrentKey _k_ = new Game.Rank.BConcurrentKey();
							_k_.Decode(_os_);
							Game.Rank.BRankCounter _v_ = new Game.Rank.BRankCounter();
							_v_.Decode(_os_);
							getCounters().Add(_k_, _v_);
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
		_Counters.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		for (var _v_ : getCounters().Values) {
			if (_v_.NegativeCheck()) {
				return true;
			}
		}
		return false;
	}

}