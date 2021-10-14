package Game.Bag;

import Zeze.Serialize.*;
import Game.*;

public final class BChangedResult extends Zeze.Transaction.Bean implements BChangedResultReadOnly {
	public static final int ChangeTagNormalChanged = 0; // 普通增量修改。
	public static final int ChangeTagRecordIsRemoved = 1; // 整个记录删除了。
	public static final int ChangeTagRecordChanged = 2; // 整个记录发生了变更，需要先清除本地数据，再替换进去。

	private Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem> _ItemsReplace; // key is position
	private Zeze.Transaction.Collections.PMapReadOnly<Integer,Game.Bag.BItemReadOnly,Game.Bag.BItem> _ItemsReplaceReadOnly;
	private Zeze.Transaction.Collections.PSet1<Integer> _ItemsRemove; // key is position
	private int _ChangeTag;

	public Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem> getItemsReplace() {
		return _ItemsReplace;
	}
	private System.Collections.Generic.IReadOnlyDictionary<Integer,Game.Bag.BItemReadOnly> Game.Bag.BChangedResultReadOnly.ItemsReplace -> _ItemsReplaceReadOnly;

	public Zeze.Transaction.Collections.PSet1<Integer> getItemsRemove() {
		return _ItemsRemove;
	}
	private System.Collections.Generic.IReadOnlySet<Integer> Game.Bag.BChangedResultReadOnly.ItemsRemove -> _ItemsRemove;

	public int getChangeTag() {
		if (false == this.isManaged()) {
			return _ChangeTag;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ChangeTag;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ChangeTag)txn.GetLog(this.getObjectId() + 3);
		return log != null ? log.getValue() : _ChangeTag;
	}
	public void setChangeTag(int value) {
		if (false == this.isManaged()) {
			_ChangeTag = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ChangeTag(this, value));
	}


	public BChangedResult() {
		this(0);
	}

	public BChangedResult(int _varId_) {
		super(_varId_);
		_ItemsReplace = new Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem>(getObjectId() + 1, _v -> new Log__ItemsReplace(this, _v));
		_ItemsReplaceReadOnly = new Zeze.Transaction.Collections.PMapReadOnly<Integer,Game.Bag.BItemReadOnly,Game.Bag.BItem>(_ItemsReplace);
		_ItemsRemove = new Zeze.Transaction.Collections.PSet1<Integer>(getObjectId() + 2, _v -> new Log__ItemsRemove(this, _v));
	}

	public void Assign(BChangedResult other) {
		getItemsReplace().Clear();
		for (var e : other.getItemsReplace()) {
			getItemsReplace().Add(e.Key, e.Value.Copy());
		}
		getItemsRemove().Clear();
		for (var e : other.getItemsRemove()) {
			getItemsRemove().Add(e);
		}
		setChangeTag(other.getChangeTag());
	}

	public BChangedResult CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BChangedResult Copy() {
		var copy = new BChangedResult();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BChangedResult a, BChangedResult b) {
		BChangedResult save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 8376297114674457432;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__ItemsReplace extends Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem>.LogV {
		public Log__ItemsReplace(BChangedResult host, System.Collections.Immutable.ImmutableDictionary<Integer, Game.Bag.BItem> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 1;
		}
		public BChangedResult getBeanTyped() {
			return (BChangedResult)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._ItemsReplace);
		}
	}

	private final static class Log__ItemsRemove extends Zeze.Transaction.Collections.PSet1<Integer>.LogV {
		public Log__ItemsRemove(BChangedResult host, System.Collections.Immutable.ImmutableHashSet<Integer> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 2;
		}
		public BChangedResult getBeanTyped() {
			return (BChangedResult)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._ItemsRemove);
		}
	}

	private final static class Log__ChangeTag extends Zeze.Transaction.Log<BChangedResult, Integer> {
		public Log__ChangeTag(BChangedResult self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 3;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ChangeTag = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Bag.BChangedResult: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ItemsReplace").Append("=[").Append(System.lineSeparator());
		level++;
		for (var _kv_ : getItemsReplace()) {
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("],").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ItemsRemove").Append("=[").Append(System.lineSeparator());
		level++;
		for (var Item : getItemsRemove()) {
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Item").Append("=").Append(Item).Append(",").Append(System.lineSeparator());
		}
		level--;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("],").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ChangeTag").Append("=").Append(getChangeTag()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(3); // Variables.Count
		_os_.WriteInt(ByteBuffer.MAP | 1 << ByteBuffer.TAG_SHIFT); {
			int _state_;
			tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
			_os_.BeginWriteSegment(tempOut__state_);
		_state_ = tempOut__state_.outArgValue;
			_os_.WriteInt(ByteBuffer.INT);
			_os_.WriteInt(ByteBuffer.BEAN);
			_os_.WriteInt(getItemsReplace().Count);
			for (var _e_ : getItemsReplace()) {
				_os_.WriteInt(_e_.Key);
				_e_.Value.Encode(_os_);
			}
			_os_.EndWriteSegment(_state_);
		}
		_os_.WriteInt(ByteBuffer.SET | 2 << ByteBuffer.TAG_SHIFT); {
			int _state_;
			tangible.OutObject<Integer> tempOut__state_2 = new tangible.OutObject<Integer>();
			_os_.BeginWriteSegment(tempOut__state_2);
		_state_ = tempOut__state_2.outArgValue;
			_os_.WriteInt(ByteBuffer.INT);
			_os_.WriteInt(getItemsRemove().Count);
			for (var _v_ : getItemsRemove()) {
				_os_.WriteInt(_v_);
			}
			_os_.EndWriteSegment(_state_);
		}
		_os_.WriteInt(ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getChangeTag());
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
						getItemsReplace().Clear();
						for (int size = _os_.ReadInt(); size > 0; --size) {
							int _k_;
							_k_ = _os_.ReadInt();
							Game.Bag.BItem _v_ = new Game.Bag.BItem();
							_v_.Decode(_os_);
							getItemsReplace().Add(_k_, _v_);
						}
						_os_.EndReadSegment(_state_);
				}
					break;
				case ByteBuffer.SET | 2 << ByteBuffer.TAG_SHIFT: {
						int _state_;
						tangible.OutObject<Integer> tempOut__state_2 = new tangible.OutObject<Integer>();
						_os_.BeginReadSegment(tempOut__state_2);
					_state_ = tempOut__state_2.outArgValue;
						_os_.ReadInt(); // skip collection.value typetag
						getItemsRemove().Clear();
						for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
							int _v_;
							_v_ = _os_.ReadInt();
							getItemsRemove().Add(_v_);
						}
						_os_.EndReadSegment(_state_);
				}
					break;
				case ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT:
					setChangeTag(_os_.ReadInt());
					break;
				default:
					ByteBuffer.SkipUnknownField(_tagid_, _os_);
					break;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
		_ItemsReplace.InitRootInfo(root, this);
		_ItemsRemove.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		for (var _v_ : getItemsReplace().Values) {
			if (_v_.NegativeCheck()) {
				return true;
			}
		}
		for (var _v_ : getItemsRemove()) {
			if (_v_ < 0) {
				return true;
			}
		}
		if (getChangeTag() < 0) {
			return true;
		}
		return false;
	}

}