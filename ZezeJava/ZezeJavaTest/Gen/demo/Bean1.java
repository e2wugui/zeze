// auto-generated @formatter:off
package demo;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

// bean的注释
@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class Bean1 extends Zeze.Transaction.Bean implements Bean1ReadOnly {
    public static final long TYPEID = -410057899348847631L;

    public static final int Enum1 = 4; // enum的注释

    private int _V1; // bean1comm		bean1line2
    private final Zeze.Transaction.Collections.PMap1<Integer, Integer> _V2; // bean1v2		bean1v2line2
    private static final Zeze.Transaction.Collections.Map1Meta<Integer, Integer> meta2_V2
            = Zeze.Transaction.Collections.Map1Meta.get(Integer.class, Integer.class);

    private transient @Nullable Object __zeze_map_key__;

    @Override
    public @Nullable Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    private static final java.lang.invoke.VarHandle vh_V1;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_V1 = _l_.findVarHandle(Bean1.class, "_V1", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getV1() {
        if (!isManaged())
            return _V1;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _V1;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _V1;
    }

    public void setV1(int _v_) {
        if (!isManaged()) {
            _V1 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_V1, _v_));
    }

    public Zeze.Transaction.Collections.PMap1<Integer, Integer> getV2() {
        return _V2;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<Integer, Integer> getV2ReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_V2);
    }

    @SuppressWarnings("deprecation")
    public Bean1() {
        _V1 = 1;
        _V2 = new Zeze.Transaction.Collections.PMap1<>(meta2_V2);
        _V2.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public Bean1(int _V1_) {
        _V1 = _V1_;
        _V2 = new Zeze.Transaction.Collections.PMap1<>(meta2_V2);
        _V2.variableId(2);
    }

    @Override
    public void reset() {
        setV1(1);
        _V2.clear();
        _unknown_ = null;
    }

    @Override
    public demo.Bean1.Data toData() {
        var _d_ = new demo.Bean1.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((demo.Bean1.Data)_o_);
    }

    public void assign(Bean1.Data _o_) {
        setV1(_o_._V1);
        _V2.clear();
        _V2.putAll(_o_._V2);
        _unknown_ = null;
    }

    public void assign(Bean1 _o_) {
        setV1(_o_.getV1());
        _V2.assign(_o_._V2);
        _unknown_ = _o_._unknown_;
    }

    public Bean1 copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public Bean1 copy() {
        var _c_ = new Bean1();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(Bean1 _a_, Bean1 _b_) {
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
        _s_.append("demo.Bean1: {\n");
        _s_.append(_i1_).append("V1=").append(getV1()).append(",\n");
        _s_.append(_i1_).append("V2={");
        if (!_V2.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _V2.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_V2.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
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

    private byte @Nullable [] _unknown_;

    public byte @Nullable [] unknown() {
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
            int _x_ = getV1();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _V2;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
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
            setV1(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            setV1(0);
        if (_i_ == 2) {
            var _x_ = _V2;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadInt(_t_);
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
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof Bean1))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (Bean1)_o_;
        if (getV1() != _b_.getV1())
            return false;
        if (!_V2.equals(_b_._V2))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _V2.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _V2.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getV1() < 0)
            return true;
        for (var _v_ : _V2.values()) {
            if (_v_ < 0)
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
                case 1: _V1 = _v_.intValue(); break;
                case 2: _V2.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setV1(_r_.getInt(_pn_ + "V1"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "V2", _V2, _r_.getString(_pn_ + "V2"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "V1", getV1());
        _s_.appendString(_pn_ + "V2", Zeze.Serialize.Helper.encodeJson(_V2));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "V1", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "V2", "map", "int", "int"));
        return _v_;
    }

// bean的注释
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -410057899348847631L;

    public static final int Enum1 = 4; // enum的注释

    private int _V1; // bean1comm		bean1line2
    private java.util.HashMap<Integer, Integer> _V2; // bean1v2		bean1v2line2

    public int getV1() {
        return _V1;
    }

    public void setV1(int _v_) {
        _V1 = _v_;
    }

    public java.util.HashMap<Integer, Integer> getV2() {
        return _V2;
    }

    public void setV2(java.util.HashMap<Integer, Integer> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _V2 = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _V1 = 1;
        _V2 = new java.util.HashMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(int _V1_, java.util.@Nullable HashMap<Integer, Integer> _V2_) {
        _V1 = _V1_;
        if (_V2_ == null)
            _V2_ = new java.util.HashMap<>();
        _V2 = _V2_;
    }

    @Override
    public void reset() {
        _V1 = 1;
        _V2.clear();
    }

    @Override
    public demo.Bean1 toBean() {
        var _b_ = new demo.Bean1();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((Bean1)_o_);
    }

    public void assign(Bean1 _o_) {
        _V1 = _o_.getV1();
        _V2.clear();
        _V2.putAll(_o_._V2);
    }

    public void assign(Bean1.Data _o_) {
        _V1 = _o_._V1;
        _V2.clear();
        _V2.putAll(_o_._V2);
    }

    @Override
    public Bean1.Data copy() {
        var _c_ = new Bean1.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(Bean1.Data _a_, Bean1.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public Bean1.Data clone() {
        return (Bean1.Data)super.clone();
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
        _s_.append("demo.Bean1: {\n");
        _s_.append(_i1_).append("V1=").append(_V1).append(",\n");
        _s_.append(_i1_).append("V2={");
        if (!_V2.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _V2.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_V2.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("}\n");
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            int _x_ = _V1;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _V2;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _V1 = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            _V1 = 0;
        if (_i_ == 2) {
            var _x_ = _V2;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadInt(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof Bean1.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (Bean1.Data)_o_;
        if (_V1 != _b_._V1)
            return false;
        if (!_V2.equals(_b_._V2))
            return false;
        return true;
    }
}
}
