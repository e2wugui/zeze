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
public class BeanMap1<C, V> extends Bean implements Map<C, V>, BeanMap1ReadOnly<C, V> {
	public static final long TYPEID = -105464877273786005L;

	private final Zeze.Transaction.Collections.PMap1<C, V> pMap1;
	private Object mapKey;

	@Override
	public void mapKey(Object mapKey) {
		this.mapKey = mapKey;
	}

	@Override
	public Object mapKey() {
		return mapKey;
	}

	public Zeze.Transaction.Collections.PMap1<C, V> getPMap1() {
		return pMap1;
	}

	@Override
	public Zeze.Transaction.Collections.PMap1ReadOnly<C, V> getMap1ReadOnly() {
		return new Zeze.Transaction.Collections.PMap1ReadOnly<>(pMap1);
	}

	@SuppressWarnings("deprecation")
	public BeanMap1(Class<C> colClass, Class<V> valueClass) {
		pMap1 = new Zeze.Transaction.Collections.PMap1<>(colClass, valueClass);
		pMap1.variableId(1);
	}

	@SuppressWarnings("deprecation")
	public BeanMap1(Meta2<C, V> meta) {
		pMap1 = new Zeze.Transaction.Collections.PMap1<>(meta);
		pMap1.variableId(1);
	}

	@Override
	public void reset() {
		pMap1.clear();
		_unknown_ = null;
	}

	public void assign(BeanMap1<C, V> _o_) {
		pMap1.assign(_o_.pMap1);
		_unknown_ = _o_._unknown_;
	}

	public BeanMap1<C, V> copyIfManaged() {
		return isManaged() ? copy() : this;
	}

	@Override
	public BeanMap1<C, V> copy() {
		var _c_ = new BeanMap1<>(pMap1.getMeta());
		_c_.assign(this);
		return _c_;
	}

	public static <C extends Comparable<C>, V> void swap(BeanMap1<C, V> _a_, BeanMap1<C, V> _b_) {
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
		_s_.append("Zeze.Transaction.GTable.BeanMap1: {\n");
		_s_.append(_i1_).append("Map1={");
		if (!pMap1.isEmpty()) {
			_s_.append('\n');
			for (var _e_ : pMap1.entrySet()) {
				_s_.append(_i2_).append("ColKey=").append(_e_.getKey()).append(",\n");
				_s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
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
			var meta = pMap1.getMeta();
			var _x_ = pMap1;
			int _n_ = _x_.size();
			if (_n_ != 0) {
				_i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
				_o_.WriteMapType(_n_, meta.keyEncodeType, meta.valueEncodeType);
				for (var _e_ : _x_.entrySet()) {
					meta.keyEncoder.accept(_o_, _e_.getKey());
					meta.valueEncoder.accept(_o_, _e_.getValue());
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
			var _x_ = pMap1;
			var meta = pMap1.getMeta();
			_x_.clear();
			if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
				int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
				for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
					var _k_ = meta.keyDecoderWithType.apply(_o_, _s_);
					var _v_ = meta.valueDecoderWithType.apply(_o_, _t_);
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
		return pMap1.size();
	}

	@Override
	public boolean isEmpty() {
		return pMap1.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return pMap1.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return pMap1.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return pMap1.get(key);
	}

	@Nullable
	@Override
	public V put(C key, V value) {
		return pMap1.put(key, value);
	}

	@Override
	public V remove(Object key) {
		return pMap1.remove(key);
	}

	@Override
	public void putAll(@NotNull Map<? extends C, ? extends V> m) {
		pMap1.putAll(m);
	}

	@Override
	public void clear() {
		pMap1.clear();
	}

	@NotNull
	@Override
	public Set<C> keySet() {
		return pMap1.keySet();
	}

	@NotNull
	@Override
	public Collection<V> values() {
		return pMap1.values();
	}

	@NotNull
	@Override
	public Set<Entry<C, V>> entrySet() {
		return pMap1.entrySet();
	}

	@Override
	public boolean equals(Object _o_) {
		if (_o_ == this)
			return true;
		if (!(_o_ instanceof BeanMap1))
			return false;
		@SuppressWarnings("unchecked") var _b_ = (BeanMap1<C, V>)_o_;
		if (!pMap1.equals(_b_.pMap1))
			return false;
		return true;
	}

	@Override
	protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
		pMap1.initRootInfo(_r_, this);
	}

	@Override
	protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
		pMap1.initRootInfoWithRedo(_r_, this);
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
				pMap1.followerApply(_v_);
				break;
			}
		}
	}

	@Override
	public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
		var _pn_ = Bean.parentsToName(_p_);
		Zeze.Serialize.Helper.decodeJsonMap(this, "Map1", pMap1, _r_.getString(_pn_ + "Map1"));
	}

	@Override
	public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
		var _pn_ = Bean.parentsToName(_p_);
		_s_.appendString(_pn_ + "Map1", Zeze.Serialize.Helper.encodeJson(pMap1));
	}

	@Override
	public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
		var _v_ = super.variables();
		_v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Map1", "map", "int", "int"));
		return _v_;
	}
}
