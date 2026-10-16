// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BFood extends Zeze.Transaction.Bean implements BFoodReadOnly {
    public static final long TYPEID = -3820308016141122965L;

    private final Zeze.Transaction.DynamicBean _Subclass;

    public static final long DynamicTypeId_Subclass_demo_Bean1 = 1L;

    public static final long DynamicTypeId_Subclass_demo_Module1_BSimple = 2L;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Subclass() {
        return new Zeze.Transaction.DynamicBean(14, BFood::getSpecialTypeIdFromBean_14, BFood::createBeanFromSpecialTypeId_14);
    }

    public static long getSpecialTypeIdFromBean_14(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == -410057899348847631L)
            return 1L; // demo.Bean1
        if (_t_ == 4513771153805810055L)
            return 2L; // demo.Module1.BSimple
        throw new UnsupportedOperationException("Unknown Bean! dynamic@demo.Module1.BFood:Subclass:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_14(long _t_) {
        if (_t_ == 1L)
            return new demo.Bean1();
        if (_t_ == 2L)
            return new demo.Module1.BSimple();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    public Zeze.Transaction.DynamicBean getSubclass() {
        return _Subclass;
    }

    public demo.Bean1 getSubclass_demo_Bean1() {
        return (demo.Bean1)_Subclass.getBean();
    }

    public void setSubclass(demo.Bean1 _v_) {
        _Subclass.setBean(_v_);
    }

    public demo.Module1.BSimple getSubclass_demo_Module1_BSimple() {
        return (demo.Module1.BSimple)_Subclass.getBean();
    }

    public void setSubclass(demo.Module1.BSimple _v_) {
        _Subclass.setBean(_v_);
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getSubclassReadOnly() {
        return _Subclass;
    }

    @Override
    public demo.Bean1ReadOnly getSubclass_demo_Bean1ReadOnly() {
        return (demo.Bean1)_Subclass.getBean();
    }

    @Override
    public demo.Module1.BSimpleReadOnly getSubclass_demo_Module1_BSimpleReadOnly() {
        return (demo.Module1.BSimple)_Subclass.getBean();
    }

    @SuppressWarnings("deprecation")
    public BFood() {
        _Subclass = newDynamicBean_Subclass();
    }

    @Override
    public void reset() {
        _Subclass.reset();
        _unknown_ = null;
    }

    @Override
    public demo.Module1.BFood.Data toData() {
        var _d_ = new demo.Module1.BFood.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((demo.Module1.BFood.Data)_o_);
    }

    public void assign(BFood.Data _o_) {
        _Subclass.assign(_o_._Subclass);
        _unknown_ = null;
    }

    public void assign(BFood _o_) {
        _Subclass.assign(_o_._Subclass);
        _unknown_ = _o_._unknown_;
    }

    public BFood copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BFood copy() {
        var _c_ = new BFood();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BFood _a_, BFood _b_) {
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
        _s_.append("demo.Module1.BFood: {\n");
        _s_.append(_i1_).append("Subclass=");
        _Subclass.getBean().buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
        while (_ui_ < 14) {
            _i_ = _o_.writeUnknownField(_i_, _ui_, _u_);
            _ui_ = _u_.readUnknownIndex();
        }
        {
            var _x_ = _Subclass;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 14, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
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
        while ((_t_ & 0xff) > 1 && _i_ < 14) {
            _u_ = _o_.readUnknownField(_i_, _t_, _u_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 14) {
            _o_.ReadDynamic(_Subclass, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BFood))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BFood)_o_;
        if (!_Subclass.equals(_b_._Subclass))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Subclass.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Subclass.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_Subclass.negativeCheck())
            return true;
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
                case 14: _Subclass.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonDynamic(_Subclass, _r_.getString(_pn_ + "Subclass"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Subclass", Zeze.Serialize.Helper.encodeJson(_Subclass));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(14, "Subclass", "dynamic", "", "demo.Bean1,demo.Module1.BSimple"));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -3820308016141122965L;

    private final DynamicData_Subclass _Subclass;

    public static final class DynamicData_Subclass extends Zeze.Transaction.DynamicData {
        public static final long DynamicTypeId_Subclass_demo_Bean1 = 1L;
        public static final long DynamicTypeId_Subclass_demo_Module1_BSimple = 2L;

        static {
            registerJsonParser(DynamicData_Subclass.class);
        }

        @Override
        public long toTypeId(Zeze.Transaction.Data _d_) {
            var _typeId_ = _d_.typeId();
            if (_typeId_ == Zeze.Transaction.EmptyBean.Data.TYPEID)
                return Zeze.Transaction.EmptyBean.Data.TYPEID;
            if (_typeId_ == -410057899348847631L)
                return 1L; // demo.Bean1
            if (_typeId_ == 4513771153805810055L)
                return 2L; // demo.Module1.BSimple
            throw new UnsupportedOperationException("Unknown Bean! dynamic@demo.Module1.BFood:Subclass:" + _typeId_);
        }

        @Override
        public Zeze.Transaction.@Nullable Data toData(long _t_) {
            if (_t_ == 1L)
                return new demo.Bean1.Data();
            if (_t_ == 2L)
                return new demo.Module1.BSimple.Data();
            if (_t_ == Zeze.Transaction.EmptyBean.Data.TYPEID)
                return Zeze.Transaction.EmptyBean.Data.instance;
            return null;
        }

        @Override
        public DynamicData_Subclass copy() {
            return (DynamicData_Subclass)super.copy();
        }
    }

    public DynamicData_Subclass getSubclass() {
        return _Subclass;
    }

    public demo.Bean1.Data getSubclass_demo_Bean1() {
        return (demo.Bean1.Data)_Subclass.getData();
    }

    public void setSubclass(demo.Bean1.Data _v_) {
        _Subclass.setData(_v_);
    }

    public demo.Module1.BSimple.Data getSubclass_demo_Module1_BSimple() {
        return (demo.Module1.BSimple.Data)_Subclass.getData();
    }

    public void setSubclass(demo.Module1.BSimple.Data _v_) {
        _Subclass.setData(_v_);
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Subclass = new DynamicData_Subclass();
    }

    @SuppressWarnings("deprecation")
    public Data(@Nullable DynamicData_Subclass _Subclass_) {
        if (_Subclass_ == null)
            _Subclass_ = new DynamicData_Subclass();
        _Subclass = _Subclass_;
    }

    @Override
    public void reset() {
        _Subclass.reset();
    }

    @Override
    public demo.Module1.BFood toBean() {
        var _b_ = new demo.Module1.BFood();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BFood)_o_);
    }

    public void assign(BFood _o_) {
        _Subclass.assign(_o_._Subclass);
    }

    public void assign(BFood.Data _o_) {
        _Subclass.assign(_o_._Subclass);
    }

    @Override
    public BFood.Data copy() {
        var _c_ = new BFood.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BFood.Data _a_, BFood.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BFood.Data clone() {
        return (BFood.Data)super.clone();
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
        _s_.append("demo.Module1.BFood: {\n");
        _s_.append(_i1_).append("Subclass=");
        _Subclass.getData().buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
            var _x_ = _Subclass;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 14, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        while ((_t_ & 0xff) > 1 && _i_ < 14) {
            _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 14) {
            _o_.ReadDynamic(_Subclass, _t_);
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
        if (!(_o_ instanceof BFood.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BFood.Data)_o_;
        if (!_Subclass.equals(_b_._Subclass))
            return false;
        return true;
    }
}
}
