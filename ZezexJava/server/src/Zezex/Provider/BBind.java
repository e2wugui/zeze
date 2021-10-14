package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

public final class BBind extends Zeze.Transaction.Bean implements BBindReadOnly {
	public static final int ResultSuccess = 0;
	public static final int ResultFaild = 1;

	private Zeze.Transaction.Collections.PMap2<Integer, Zezex.Provider.BModule> _modules; // moduleId -> type
	private Zeze.Transaction.Collections.PMapReadOnly<Integer,Zezex.Provider.BModuleReadOnly,Zezex.Provider.BModule> _modulesReadOnly;
	private Zeze.Transaction.Collections.PSet1<Long> _linkSids;

	public Zeze.Transaction.Collections.PMap2<Integer, Zezex.Provider.BModule> getModules() {
		return _modules;
	}
	private System.Collections.Generic.IReadOnlyDictionary<Integer,Zezex.Provider.BModuleReadOnly> Zezex.Provider.BBindReadOnly.Modules -> _modulesReadOnly;

	public Zeze.Transaction.Collections.PSet1<Long> getLinkSids() {
		return _linkSids;
	}
	private System.Collections.Generic.IReadOnlySet<Long> Zezex.Provider.BBindReadOnly.LinkSids -> _linkSids;


	public BBind() {
		this(0);
	}

	public BBind(int _varId_) {
		super(_varId_);
		_modules = new Zeze.Transaction.Collections.PMap2<Integer, Zezex.Provider.BModule>(getObjectId() + 1, _v -> new Log__modules(this, _v));
		_modulesReadOnly = new Zeze.Transaction.Collections.PMapReadOnly<Integer,Zezex.Provider.BModuleReadOnly,Zezex.Provider.BModule>(_modules);
		_linkSids = new Zeze.Transaction.Collections.PSet1<Long>(getObjectId() + 2, _v -> new Log__linkSids(this, _v));
	}

	public void Assign(BBind other) {
		getModules().Clear();
		for (var e : other.getModules()) {
			getModules().Add(e.Key, e.Value.Copy());
		}
		getLinkSids().Clear();
		for (var e : other.getLinkSids()) {
			getLinkSids().Add(e);
		}
	}

	public BBind CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BBind Copy() {
		var copy = new BBind();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BBind a, BBind b) {
		BBind save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 8933110584444310889;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__modules extends Zeze.Transaction.Collections.PMap2<Integer, Zezex.Provider.BModule>.LogV {
		public Log__modules(BBind host, System.Collections.Immutable.ImmutableDictionary<Integer, Zezex.Provider.BModule> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 1;
		}
		public BBind getBeanTyped() {
			return (BBind)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._modules);
		}
	}

	private final static class Log__linkSids extends Zeze.Transaction.Collections.PSet1<Long>.LogV {
		public Log__linkSids(BBind host, System.Collections.Immutable.ImmutableHashSet<Long> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 2;
		}
		public BBind getBeanTyped() {
			return (BBind)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._linkSids);
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Provider.BBind: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Modules").Append("=[").Append(System.lineSeparator());
		level++;
		for (var _kv_ : getModules()) {
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("LinkSids").Append("=[").Append(System.lineSeparator());
		level++;
		for (var Item : getLinkSids()) {
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Item").Append("=").Append(Item).Append(",").Append(System.lineSeparator());
		}
		level--;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("]").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(2); // Variables.Count
		_os_.WriteInt(ByteBuffer.MAP | 1 << ByteBuffer.TAG_SHIFT); {
			int _state_;
			tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
			_os_.BeginWriteSegment(tempOut__state_);
		_state_ = tempOut__state_.outArgValue;
			_os_.WriteInt(ByteBuffer.INT);
			_os_.WriteInt(ByteBuffer.BEAN);
			_os_.WriteInt(getModules().Count);
			for (var _e_ : getModules()) {
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
			_os_.WriteInt(ByteBuffer.LONG);
			_os_.WriteInt(getLinkSids().Count);
			for (var _v_ : getLinkSids()) {
				_os_.WriteLong(_v_);
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
						getModules().Clear();
						for (int size = _os_.ReadInt(); size > 0; --size) {
							int _k_;
							_k_ = _os_.ReadInt();
							Zezex.Provider.BModule _v_ = new Zezex.Provider.BModule();
							_v_.Decode(_os_);
							getModules().Add(_k_, _v_);
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
						getLinkSids().Clear();
						for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
							long _v_;
							_v_ = _os_.ReadLong();
							getLinkSids().Add(_v_);
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
		_modules.InitRootInfo(root, this);
		_linkSids.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		for (var _v_ : getModules().Values) {
			if (_v_.NegativeCheck()) {
				return true;
			}
		}
		for (var _v_ : getLinkSids()) {
			if (_v_ < 0) {
				return true;
			}
		}
		return false;
	}

}