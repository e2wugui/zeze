package Game.Buf;

import Zeze.Serialize.*;
import Game.*;

public final class BBufChanged extends Zeze.Transaction.Bean implements BBufChangedReadOnly {
	public static final int ChangeTagNormalChanged = 0; // 普通增量修改。
	public static final int ChangeTagRecordIsRemoved = 1; // 整个记录删除了。
	public static final int ChangeTagRecordChanged = 2; // 整个记录发生了变更，需要先清除本地数据，再替换进去。

	private Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf> _Replace; // key is bufId
	private Zeze.Transaction.Collections.PMapReadOnly<Integer,Game.Buf.BBufReadOnly,Game.Buf.BBuf> _ReplaceReadOnly;
	private Zeze.Transaction.Collections.PSet1<Integer> _Remove; // key is bufId
	private int _ChangeTag;

	public Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf> getReplace() {
		return _Replace;
	}
	private System.Collections.Generic.IReadOnlyDictionary<Integer,Game.Buf.BBufReadOnly> Game.Buf.BBufChangedReadOnly.Replace -> _ReplaceReadOnly;

	public Zeze.Transaction.Collections.PSet1<Integer> getRemove() {
		return _Remove;
	}
	private System.Collections.Generic.IReadOnlySet<Integer> Game.Buf.BBufChangedReadOnly.Remove -> _Remove;

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


	public BBufChanged() {
		this(0);
	}

	public BBufChanged(int _varId_) {
		super(_varId_);
		_Replace = new Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf>(getObjectId() + 1, _v -> new Log__Replace(this, _v));
		_ReplaceReadOnly = new Zeze.Transaction.Collections.PMapReadOnly<Integer,Game.Buf.BBufReadOnly,Game.Buf.BBuf>(_Replace);
		_Remove = new Zeze.Transaction.Collections.PSet1<Integer>(getObjectId() + 2, _v -> new Log__Remove(this, _v));
	}

	public void Assign(BBufChanged other) {
		getReplace().Clear();
		for (var e : other.getReplace()) {
			getReplace().Add(e.Key, e.Value.Copy());
		}
		getRemove().Clear();
		for (var e : other.getRemove()) {
			getRemove().Add(e);
		}
		setChangeTag(other.getChangeTag());
	}

	public BBufChanged CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BBufChanged Copy() {
		var copy = new BBufChanged();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BBufChanged a, BBufChanged b) {
		BBufChanged save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 8288333028989651451;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Replace extends Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf>.LogV {
		public Log__Replace(BBufChanged host, System.Collections.Immutable.ImmutableDictionary<Integer, Game.Buf.BBuf> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 1;
		}
		public BBufChanged getBeanTyped() {
			return (BBufChanged)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._Replace);
		}
	}

	private final static class Log__Remove extends Zeze.Transaction.Collections.PSet1<Integer>.LogV {
		public Log__Remove(BBufChanged host, System.Collections.Immutable.ImmutableHashSet<Integer> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 2;
		}
		public BBufChanged getBeanTyped() {
			return (BBufChanged)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._Remove);
		}
	}

	private final static class Log__ChangeTag extends Zeze.Transaction.Log<BBufChanged, Integer> {
		public Log__ChangeTag(BBufChanged self, int value) {
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Buf.BBufChanged: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Replace").Append("=[").Append(System.lineSeparator());
		level++;
		for (var _kv_ : getReplace()) {
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Remove").Append("=[").Append(System.lineSeparator());
		level++;
		for (var Item : getRemove()) {
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
			_os_.WriteInt(getReplace().Count);
			for (var _e_ : getReplace()) {
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
			_os_.WriteInt(getRemove().Count);
			for (var _v_ : getRemove()) {
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
						getReplace().Clear();
						for (int size = _os_.ReadInt(); size > 0; --size) {
							int _k_;
							_k_ = _os_.ReadInt();
							Game.Buf.BBuf _v_ = new Game.Buf.BBuf();
							_v_.Decode(_os_);
							getReplace().Add(_k_, _v_);
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
						getRemove().Clear();
						for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
							int _v_;
							_v_ = _os_.ReadInt();
							getRemove().Add(_v_);
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
		_Replace.InitRootInfo(root, this);
		_Remove.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		for (var _v_ : getReplace().Values) {
			if (_v_.NegativeCheck()) {
				return true;
			}
		}
		for (var _v_ : getRemove()) {
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