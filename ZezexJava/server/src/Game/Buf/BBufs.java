package Game.Buf;

import Zeze.Serialize.*;
import Game.*;

public final class BBufs extends Zeze.Transaction.Bean implements BBufsReadOnly {
	private Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf> _Bufs;
	private Zeze.Transaction.Collections.PMapReadOnly<Integer,Game.Buf.BBufReadOnly,Game.Buf.BBuf> _BufsReadOnly;

	public Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf> getBufs() {
		return _Bufs;
	}
	private System.Collections.Generic.IReadOnlyDictionary<Integer,Game.Buf.BBufReadOnly> Game.Buf.BBufsReadOnly.Bufs -> _BufsReadOnly;


	public BBufs() {
		this(0);
	}

	public BBufs(int _varId_) {
		super(_varId_);
		_Bufs = new Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf>(getObjectId() + 1, _v -> new Log__Bufs(this, _v));
		_BufsReadOnly = new Zeze.Transaction.Collections.PMapReadOnly<Integer,Game.Buf.BBufReadOnly,Game.Buf.BBuf>(_Bufs);
	}

	public void Assign(BBufs other) {
		getBufs().Clear();
		for (var e : other.getBufs()) {
			getBufs().Add(e.Key, e.Value.Copy());
		}
	}

	public BBufs CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BBufs Copy() {
		var copy = new BBufs();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BBufs a, BBufs b) {
		BBufs save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -6095071065680829700;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Bufs extends Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf>.LogV {
		public Log__Bufs(BBufs host, System.Collections.Immutable.ImmutableDictionary<Integer, Game.Buf.BBuf> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 1;
		}
		public BBufs getBeanTyped() {
			return (BBufs)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._Bufs);
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Buf.BBufs: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Bufs").Append("=[").Append(System.lineSeparator());
		level++;
		for (var _kv_ : getBufs()) {
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
			_os_.WriteInt(getBufs().Count);
			for (var _e_ : getBufs()) {
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
						getBufs().Clear();
						for (int size = _os_.ReadInt(); size > 0; --size) {
							int _k_;
							_k_ = _os_.ReadInt();
							Game.Buf.BBuf _v_ = new Game.Buf.BBuf();
							_v_.Decode(_os_);
							getBufs().Add(_k_, _v_);
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
		_Bufs.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		for (var _v_ : getBufs().Values) {
			if (_v_.NegativeCheck()) {
				return true;
			}
		}
		return false;
	}

}