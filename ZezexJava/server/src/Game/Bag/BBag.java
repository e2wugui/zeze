package Game.Bag;

import Zeze.Serialize.*;
import Game.*;

public final class BBag extends Zeze.Transaction.Bean implements BBagReadOnly {
	private long _Money;
	private int _Capacity;
	private Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem> _Items; // key is bag position
	private Zeze.Transaction.Collections.PMapReadOnly<Integer,Game.Bag.BItemReadOnly,Game.Bag.BItem> _ItemsReadOnly;

	public long getMoney() {
		if (false == this.isManaged()) {
			return _Money;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Money;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Money)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _Money;
	}
	public void setMoney(long value) {
		if (false == this.isManaged()) {
			_Money = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Money(this, value));
	}

	public int getCapacity() {
		if (false == this.isManaged()) {
			return _Capacity;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Capacity;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Capacity)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _Capacity;
	}
	public void setCapacity(int value) {
		if (false == this.isManaged()) {
			_Capacity = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Capacity(this, value));
	}

	public Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem> getItems() {
		return _Items;
	}
	private System.Collections.Generic.IReadOnlyDictionary<Integer,Game.Bag.BItemReadOnly> Game.Bag.BBagReadOnly.Items -> _ItemsReadOnly;


	public BBag() {
		this(0);
	}

	public BBag(int _varId_) {
		super(_varId_);
		_Items = new Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem>(getObjectId() + 3, _v -> new Log__Items(this, _v));
		_ItemsReadOnly = new Zeze.Transaction.Collections.PMapReadOnly<Integer,Game.Bag.BItemReadOnly,Game.Bag.BItem>(_Items);
	}

	public void Assign(BBag other) {
		setMoney(other.getMoney());
		setCapacity(other.getCapacity());
		getItems().Clear();
		for (var e : other.getItems()) {
			getItems().Add(e.Key, e.Value.Copy());
		}
	}

	public BBag CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BBag Copy() {
		var copy = new BBag();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BBag a, BBag b) {
		BBag save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -7082293047368631199;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Money extends Zeze.Transaction.Log<BBag, Long> {
		public Log__Money(BBag self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Money = this.getValue();
		}
	}

	private final static class Log__Capacity extends Zeze.Transaction.Log<BBag, Integer> {
		public Log__Capacity(BBag self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Capacity = this.getValue();
		}
	}

	private final static class Log__Items extends Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem>.LogV {
		public Log__Items(BBag host, System.Collections.Immutable.ImmutableDictionary<Integer, Game.Bag.BItem> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 3;
		}
		public BBag getBeanTyped() {
			return (BBag)Bean;
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Bag.BBag: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Money").Append("=").Append(getMoney()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Capacity").Append("=").Append(getCapacity()).Append(",").Append(System.lineSeparator());
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
		_os_.WriteInt(3); // Variables.Count
		_os_.WriteInt(ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getMoney());
		_os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getCapacity());
		_os_.WriteInt(ByteBuffer.MAP | 3 << ByteBuffer.TAG_SHIFT); {
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
				case ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT:
					setMoney(_os_.ReadLong());
					break;
				case ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT:
					setCapacity(_os_.ReadInt());
					break;
				case ByteBuffer.MAP | 3 << ByteBuffer.TAG_SHIFT: {
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
		if (getMoney() < 0) {
			return true;
		}
		if (getCapacity() < 0) {
			return true;
		}
		for (var _v_ : getItems().Values) {
			if (_v_.NegativeCheck()) {
				return true;
			}
		}
		return false;
	}

}