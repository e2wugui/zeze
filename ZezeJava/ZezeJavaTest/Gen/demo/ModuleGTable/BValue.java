// auto-generated @formatter:off
package demo.ModuleGTable;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BValue extends Zeze.Transaction.Bean implements BValueReadOnly {
    public static final long TYPEID = 8414950011643363976L;

    private final Zeze.Transaction.GTable.GTable1<Integer, Integer, Integer> _gTable;
    private static final Zeze.Transaction.GTable.GTable1.Factory<Integer, Integer, Integer> factory_gTable
            = Zeze.Transaction.GTable.GTable1.getFactory(Integer.class, Integer.class, Integer.class);

    private final Zeze.Transaction.GTable.GTable1<Integer, String, Integer> _gTable2;
    private static final Zeze.Transaction.GTable.GTable1.Factory<Integer, String, Integer> factory_gTable2
            = Zeze.Transaction.GTable.GTable1.getFactory(Integer.class, String.class, Integer.class);

    private final Zeze.Transaction.GTable.GTable2<Integer, Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> _gTable3;
    private static final Zeze.Transaction.GTable.GTable2.Factory<Integer, Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> factory_gTable3
            = Zeze.Transaction.GTable.GTable2.getFactory(Integer.class, Integer.class, BValue::getSpecialTypeIdFromBean_3, BValue::createBeanFromSpecialTypeId_3);

    public static Zeze.Transaction.DynamicBean newDynamicBean_GTable3() {
        return new Zeze.Transaction.DynamicBean(3, BValue::getSpecialTypeIdFromBean_3, BValue::createBeanFromSpecialTypeId_3);
    }

    public static long getSpecialTypeIdFromBean_3(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == -7194090735740848088L)
            return -7194090735740848088L; // demo.ModuleGTable.Bean1
        throw new UnsupportedOperationException("Unknown Bean! dynamic@demo.ModuleGTable.BValue:gTable3:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_3(long _t_) {
        if (_t_ == -7194090735740848088L)
            return new demo.ModuleGTable.Bean1();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private final Zeze.Transaction.GTable.GTable2<Integer, String, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> _gTable4;
    private static final Zeze.Transaction.GTable.GTable2.Factory<Integer, String, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> factory_gTable4
            = Zeze.Transaction.GTable.GTable2.getFactory(Integer.class, String.class, BValue::getSpecialTypeIdFromBean_4, BValue::createBeanFromSpecialTypeId_4);

    public static Zeze.Transaction.DynamicBean newDynamicBean_GTable4() {
        return new Zeze.Transaction.DynamicBean(4, BValue::getSpecialTypeIdFromBean_4, BValue::createBeanFromSpecialTypeId_4);
    }

    public static long getSpecialTypeIdFromBean_4(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == -7194090735740848088L)
            return -7194090735740848088L; // demo.ModuleGTable.Bean1
        throw new UnsupportedOperationException("Unknown Bean! dynamic@demo.ModuleGTable.BValue:gTable4:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_4(long _t_) {
        if (_t_ == -7194090735740848088L)
            return new demo.ModuleGTable.Bean1();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    public Zeze.Transaction.GTable.GTable1<Integer, Integer, Integer> getGTable() {
        return _gTable;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap1<Integer, Integer>, Zeze.Transaction.GTable.BeanMap1ReadOnly<Integer, Integer>> getGTableReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_gTable.getPMap2());
    }

    public Zeze.Transaction.GTable.GTable1<Integer, String, Integer> getGTable2() {
        return _gTable2;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap1<String, Integer>, Zeze.Transaction.GTable.BeanMap1ReadOnly<String, Integer>> getGTable2ReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_gTable2.getPMap2());
    }

    public Zeze.Transaction.GTable.GTable2<Integer, Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getGTable3() {
        return _gTable3;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap2<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>, Zeze.Transaction.GTable.BeanMap2ReadOnly<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>> getGTable3ReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_gTable3.getPMap2());
    }

    public Zeze.Transaction.GTable.GTable2<Integer, String, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getGTable4() {
        return _gTable4;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap2<String, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>, Zeze.Transaction.GTable.BeanMap2ReadOnly<String, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>> getGTable4ReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_gTable4.getPMap2());
    }

    @SuppressWarnings("deprecation")
    public BValue() {
        _gTable = new Zeze.Transaction.GTable.GTable1<>(factory_gTable);
        _gTable.variableId(1);
        _gTable2 = new Zeze.Transaction.GTable.GTable1<>(factory_gTable2);
        _gTable2.variableId(2);
        _gTable3 = new Zeze.Transaction.GTable.GTable2<>(factory_gTable3);
        _gTable3.variableId(3);
        _gTable4 = new Zeze.Transaction.GTable.GTable2<>(factory_gTable4);
        _gTable4.variableId(4);
    }

    @Override
    public void reset() {
        _gTable.clear();
        _gTable2.clear();
        _gTable3.clear();
        _gTable4.clear();
        _unknown_ = null;
    }

    public void assign(BValue _o_) {
        _gTable.assign(_o_._gTable);
        _gTable2.assign(_o_._gTable2);
        _gTable3.assign(_o_._gTable3);
        _gTable4.assign(_o_._gTable4);
        _unknown_ = _o_._unknown_;
    }

    public BValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BValue copy() {
        var _c_ = new BValue();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BValue _a_, BValue _b_) {
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
        _s_.append("demo.ModuleGTable.BValue: {\n");
        _s_.append(_i1_).append("gTable={");
        if (!_gTable.isEmpty()) {
            _s_.append('\n');
            _gTable.buildString(_s_, _l_ + 4);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("gTable2={");
        if (!_gTable2.isEmpty()) {
            _s_.append('\n');
            _gTable2.buildString(_s_, _l_ + 4);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("gTable3={");
        if (!_gTable3.isEmpty()) {
            _s_.append('\n');
            _gTable3.buildString(_s_, _l_ + 4);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("gTable4={");
        if (!_gTable4.isEmpty()) {
            _s_.append('\n');
            _gTable4.buildString(_s_, _l_ + 4);
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
            var _x_ = _gTable;
            int _n_ = _x_.getPMap2().size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.getPMap2().entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _gTable2;
            int _n_ = _x_.getPMap2().size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.getPMap2().entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _gTable3;
            int _n_ = _x_.getPMap2().size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.getPMap2().entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _gTable4;
            int _n_ = _x_.getPMap2().size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.getPMap2().entrySet()) {
                    _o_.WriteLong(_e_.getKey());
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
            var _x_ = _gTable;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(_gTable.getPMap2().createValue(), _t_);
                    _x_.getPMap2().put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _gTable2;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(_gTable2.getPMap2().createValue(), _t_);
                    _x_.getPMap2().put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _gTable3;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(_gTable3.getPMap2().createValue(), _t_);
                    _x_.getPMap2().put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = _gTable4;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(_gTable4.getPMap2().createValue(), _t_);
                    _x_.getPMap2().put(_k_, _v_);
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
        if (!(_o_ instanceof BValue))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BValue)_o_;
        if (!_gTable.equals(_b_._gTable))
            return false;
        if (!_gTable2.equals(_b_._gTable2))
            return false;
        if (!_gTable3.equals(_b_._gTable3))
            return false;
        if (!_gTable4.equals(_b_._gTable4))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _gTable.initRootInfo(_r_, this);
        _gTable2.initRootInfo(_r_, this);
        _gTable3.initRootInfo(_r_, this);
        _gTable4.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _gTable.initRootInfoWithRedo(_r_, this);
        _gTable2.initRootInfoWithRedo(_r_, this);
        _gTable3.initRootInfoWithRedo(_r_, this);
        _gTable4.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _gTable.getPMap2().values()) {
            if (_v_.negativeCheck()) return true;
        }
        for (var _v_ : _gTable2.getPMap2().values()) {
            if (_v_.negativeCheck()) return true;
        }
        for (var _v_ : _gTable3.getPMap2().values()) {
            if (_v_.negativeCheck()) return true;
        }
        for (var _v_ : _gTable4.getPMap2().values()) {
            if (_v_.negativeCheck()) return true;
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
                case 1: _gTable.getPMap2().followerApply(_v_); break;
                case 2: _gTable2.getPMap2().followerApply(_v_); break;
                case 3: _gTable3.getPMap2().followerApply(_v_); break;
                case 4: _gTable4.getPMap2().followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonGTable1(this, "gTable", _gTable, _r_.getString(_pn_ + "gTable"));
        Zeze.Serialize.Helper.decodeJsonGTable1(this, "gTable2", _gTable2, _r_.getString(_pn_ + "gTable2"));
        Zeze.Serialize.Helper.decodeJsonGTable2(this, "gTable3", _gTable3, _r_.getString(_pn_ + "gTable3"));
        Zeze.Serialize.Helper.decodeJsonGTable2(this, "gTable4", _gTable4, _r_.getString(_pn_ + "gTable4"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "gTable", Zeze.Serialize.Helper.encodeJson(_gTable));
        _s_.appendString(_pn_ + "gTable2", Zeze.Serialize.Helper.encodeJson(_gTable2));
        _s_.appendString(_pn_ + "gTable3", Zeze.Serialize.Helper.encodeJson(_gTable3));
        _s_.appendString(_pn_ + "gTable4", Zeze.Serialize.Helper.encodeJson(_gTable4));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "gTable", "gtable", "int,int", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "gTable2", "gtable", "int,string", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "gTable3", "gtable", "int,int", "dynamic"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "gTable4", "gtable", "int,string", "dynamic"));
        return _v_;
    }
}
