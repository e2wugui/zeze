package Zeze.Transaction.GTable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.Meta2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BeanMap2<C, V extends Bean, VReadOnly> extends Bean implements Map<C, V>, BeanMap2ReadOnly<C, V, VReadOnly> {
	public static final long TYPEID = 2968992468344472794L;

	private final Zeze.Transaction.Collections.PMap2<C, V> _Map2;

	public Zeze.Transaction.Collections.PMap2<C, V> getMap2() {
		return _Map2;
	}

	@Override
	public Zeze.Transaction.Collections.PMap2ReadOnly<C, V, VReadOnly> getMap2ReadOnly() {
		return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Map2);
	}

	public BeanMap2(Class<C> colClass, Class<V> valueClass) {
		_Map2 = new Zeze.Transaction.Collections.PMap2<>(colClass, valueClass);
		_Map2.variableId(1);
	}

	public BeanMap2(Meta2<C, V> meta) {
		_Map2 = new Zeze.Transaction.Collections.PMap2<>(meta);
		_Map2.variableId(1);
	}

	@Override
	public void reset() {
		_Map2.clear();
		_unknown_ = null;
	}

	public void assign(BeanMap2<C, V, VReadOnly> _o_) {
		_Map2.clear();
		for (var _e_ : _o_._Map2.entrySet()) {
			@SuppressWarnings("unchecked") var v = (V)_e_.getValue().copy();
			_Map2.put(_e_.getKey(), v);
		}
		_unknown_ = _o_._unknown_;
	}

	public BeanMap2<C, V, VReadOnly> copyIfManaged() {
		return isManaged() ? copy() : this;
	}

	@Override
	public BeanMap2<C, V, VReadOnly> copy() {
		var _c_ = new BeanMap2<C, V, VReadOnly>(_Map2.getMeta());
		_c_.assign(this);
		return _c_;
	}

	public static <C extends Comparable<C>, V extends Bean, VReadOnly>
	void swap(BeanMap2<C, V, VReadOnly> _a_, BeanMap2<C, V, VReadOnly> _b_) {
		var _s_ = _a_.copy();
		_a_.assign(_b_);
		_b_.assign(_s_);
	}

	@Override
	public long typeId() {
		return TYPEID;
	}

	@Override
	public String toString() {
		var _s_ = new StringBuilder();
		buildString(_s_, 0);
		return _s_.toString();
	}

	@Override
	public void buildString(StringBuilder _s_, int _l_) {
		var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
		var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
		_s_.append("Zeze.Transaction.GTable.BeanMap2: {\n");
		_s_.append(_i1_).append("Map2={");
		if (!_Map2.isEmpty()) {
			_s_.append('\n');
			for (var _e_ : _Map2.entrySet()) {
				_s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
				_s_.append(_i2_).append("Value=");
				_e_.getValue().buildString(_s_, _l_ + 12);
				_s_.append(",\n");
			}
			_s_.append(_i1_);
		}
		_s_.append("}\n");
		_s_.append(Zeze.Util.Str.indent(_l_)).append('}');
	}

	private static int _PRE_ALLOC_SIZE_ = 16;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int _s_) {
		_PRE_ALLOC_SIZE_ = _s_;
	}

	private byte[] _unknown_;

	public byte[] unknown() {
		return _unknown_;
	}

	public void clearUnknown() {
		_unknown_ = null;
	}

	@Override
	public void encode(ByteBuffer _o_) {
		ByteBuffer _u_ = null;
		var _ua_ = _unknown_;
		var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
		int _i_ = 0;
		{
			var meta = _Map2.getMeta();
			var _x_ = _Map2;
			int _n_ = _x_.size();
			if (_n_ != 0) {
				_i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
				_o_.WriteMapType(_n_, meta.keyEncodeType, ByteBuffer.BEAN);
				for (var _e_ : _x_.entrySet()) {
					meta.keyEncoder.accept(_o_, _e_.getKey());
					_e_.getValue().encode(_o_);
					_n_--;
				}
				if (_n_ != 0)
					throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
			}
		}
		_o_.writeAllUnknownFields(_i_, _ui_, _u_);
		_o_.WriteByte(0);
	}

	@Override
	public void decode(IByteBuffer _o_) {
		ByteBuffer _u_ = null;
		int _t_ = _o_.ReadByte();
		int _i_ = _o_.ReadTagSize(_t_);
		if (_i_ == 1) {
			var meta = _Map2.getMeta();
			var _x_ = _Map2;
			_x_.clear();
			if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
				int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
				for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
					var _k_ = meta.keyDecoderWithType.apply(_o_, _s_);
					var _v_ = _o_.ReadBean(_Map2.createValue(), _t_);
					_x_.put(_k_, _v_);
				}
			} else
				_o_.SkipUnknownFieldOrThrow(_t_, "Map");
			_i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
		}
		//noinspection ConstantValue
		_unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
	}

	@Override
	public int size() {
		return _Map2.size();
	}

	@Override
	public boolean isEmpty() {
		return _Map2.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return _Map2.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return _Map2.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return _Map2.get(key);
	}

	@Nullable
	@Override
	public V put(C key, V value) {
		return _Map2.put(key, value);
	}

	@Override
	public V remove(Object key) {
		return _Map2.remove(key);
	}

	@Override
	public void putAll(@NotNull Map<? extends C, ? extends V> m) {
		_Map2.putAll(m);
	}

	@Override
	public void clear() {
		_Map2.clear();
	}

	@NotNull
	@Override
	public Set<C> keySet() {
		return _Map2.keySet();
	}

	@NotNull
	@Override
	public Collection<V> values() {
		return _Map2.values();
	}

	@NotNull
	@Override
	public Set<Entry<C, V>> entrySet() {
		return _Map2.entrySet();
	}

	@Override
	public boolean equals(Object _o_) {
		if (_o_ == this)
			return true;
		if (!(_o_ instanceof BeanMap2))
			return false;
		@SuppressWarnings("unchecked") var _b_ = (BeanMap2<C, V, VReadOnly>)_o_;
		if (!_Map2.equals(_b_._Map2))
			return false;
		return true;
	}

	@Override
	protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
		_Map2.initRootInfo(_r_, this);
	}

	@Override
	protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
		_Map2.initRootInfoWithRedo(_r_, this);
	}

	@Override
	public boolean negativeCheck() {
		for (var _v_ : _Map2.values()) {
			if (_v_.negativeCheck())
				return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void followerApply(Zeze.Transaction.Log _l_) {
		var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
		if (_vs_ == null)
			return;
		for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
			var _v_ = _i_.value();
			switch (_v_.getVariableId()) {
			case 1:
				_Map2.followerApply(_v_);
				break;
			}
		}
	}

	@Override
	public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
		var _pn_ = Bean.parentsToName(_p_);
		Zeze.Serialize.Helper.decodeJsonMap(this, "Map2", _Map2, _r_.getString(_pn_ + "Map2"));
	}

	@Override
	public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
		var _pn_ = Bean.parentsToName(_p_);
		_s_.appendString(_pn_ + "Map2", Zeze.Serialize.Helper.encodeJson(_Map2));
	}

	@Override
	public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
		var _v_ = super.variables();
		_v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Map2", "map", "int", "Zeze.Builtin.MQ.BOptions"));
		return _v_;
	}
}
