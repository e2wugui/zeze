package Game.Equip;

import Zeze.Serialize.*;
import Game.*;

public final class BEquips extends Zeze.Transaction.Bean implements BEquipsReadOnly {
	private Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem> _Items; // key is equip position
	private Zeze.Transaction.Collections.PMapReadOnly<Integer,Game.Bag.BItemReadOnly,Game.Bag.BItem> _ItemsReadOnly;

	public Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem> getItems() {
		return _Items;
	}
	private System.Collections.Generic.IReadOnlyDictionary<Integer,Game.Bag.BItemReadOnly> Game.Equip.BEquipsReadOnly.Items -> _ItemsReadOnly;


	public BEquips() {
		this(0);
	}

	public BEquips(int _varId_) {
		super(_varId_);
		_Items = new Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem>(getObjectId() + 1, _v -> new Log__Items(this, _v));
		_ItemsReadOnly = new Zeze.Transaction.Collections.PMapReadOnly<Integer,Game.Bag.BItemReadOnly,Game.Bag.BItem>(_Items);
	}

	public void Assign(BEquips other) {
		getItems().Clear();
		for (var e : other.getItems()) {
			getItems().Add(e.Key, e.Value.Copy());
		}
	}

	public BEquips CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BEquips Copy() {
		var copy = new BEquips();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BEquips a, BEquips b) {
		BEquips save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 2444609496742764204;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Items extends Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem>.LogV {
		public Log__Items(BEquips host, System.Collections.Immutable.ImmutableDictionary<Integer, Game.Bag.BItem> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 1;
		}
		public BEquips getBeanTyped() {
			return (BEquips)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._Items);
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Equip.BEquips: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Items").Append("=[").Append(System.lineSeparator());
		level++;
		for (var _kv_ : getItems()) {
			sb.append("(").Append(System.lineSeparator());
			var Key = _kv_.Key;
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Key").Append("=").Append(Key).Append(",").Append(System.lineSeparator());
			var Value = _kv_.Value;
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
			_os_.WriteInt(ByteBuffer.INT);
			_os_.WriteInt(ByteBuffer.BEAN);
			_os_.WriteInt(getItems().Count);
			for (var _e_ : getItems()) {
				_os_.WriteInt(_e_.Key);
				_e_.Value.Encode(_os_);
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
						getItems().Clear();
						for (int size = _os_.ReadInt(); size > 0; --size) {
							int _k_;
							_k_ = _os_.ReadInt();
							Game.Bag.BItem _v_ = new Game.Bag.BItem();
							_v_.Decode(_os_);
							getItems().Add(_k_, _v_);
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
		_Items.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		for (var _v_ : getItems().Values) {
			if (_v_.NegativeCheck()) {
				return true;
			}
		}
		return false;
	}

}