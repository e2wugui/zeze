// auto-generated @formatter:off
package dynamicred.Demo;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class Line extends Zeze.Transaction.Bean implements LineReadOnly {
    public static final long TYPEID = 5260758296393585156L;

    private final Zeze.Transaction.Collections.PList2<Zeze.Transaction.DynamicBean> _Amounts;
    private static final Zeze.Transaction.DynamicField dynamicField_Amounts
            = Zeze.Transaction.DynamicField.declare(Line.class, TYPEID, 2, Line::getSpecialTypeIdFromBean_2, Line::createBeanFromSpecialTypeId_2, dynamicred.Demo.Amount.class);
    private static final Zeze.Transaction.Collections.List2Meta<Zeze.Transaction.DynamicBean> meta1_Amounts
            = Zeze.Transaction.Collections.List2Meta.createDynamic(dynamicField_Amounts);

    public static Zeze.Transaction.DynamicBean newDynamicBean_Amounts() {
        return new Zeze.Transaction.DynamicBean(dynamicField_Amounts);
    }

    public static long getSpecialTypeIdFromBean_2(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == 5587247785002748608L)
            return 7L; // dynamicred.Demo.Amount
        throw new UnsupportedOperationException("Unknown Bean! dynamic@dynamicred.Demo.Line:Amounts:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_2(long _t_) {
        if (_t_ == 7L)
            return new dynamicred.Demo.Amount();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Transaction.DynamicBean> getAmounts() {
        return _Amounts;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getAmountsReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Amounts);
    }

    @SuppressWarnings("deprecation")
    public Line() {
        _Amounts = new Zeze.Transaction.Collections.PList2<>(meta1_Amounts);
        _Amounts.variableId(2);
    }

    @Override
    public void reset() {
        _Amounts.clear();
        _unknown_ = null;
    }

    public void assign(Line _o_) {
        _Amounts.clear();
        for (var _e_ : _o_._Amounts)
            _Amounts.add(_e_.copy());
        _unknown_ = _o_._unknown_;
    }

    public Line copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public Line copy() {
        var _c_ = new Line();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(Line _a_, Line _b_) {
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
        _s_.append("dynamicred.Demo.Line: {\n");
        _s_.append(_i1_).append("Amounts=[");
        if (!_Amounts.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _Amounts) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Amounts.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.getBean().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("]\n");
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
        while (_ui_ < 2) {
            _i_ = _o_.writeUnknownField(_i_, _ui_, _u_);
            _ui_ = _u_.readUnknownIndex();
        }
        {
            var _x_ = _Amounts;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.DYNAMIC);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
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
        while ((_t_ & 0xff) > 1 && _i_ < 2) {
            _u_ = _o_.readUnknownField(_i_, _t_, _u_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Amounts;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                    var _e_ = Line.newDynamicBean_Amounts();
                    _o_.ReadDynamic(_e_, _t_);
                    _x_.add(_e_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            _Amounts.clear();
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof Line))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (Line)_o_;
        if (!_Amounts.equals(_b_._Amounts))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Amounts.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Amounts.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Amounts) {
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
                case 2: _Amounts.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonList(_Amounts, Zeze.Transaction.DynamicBean.class, _r_.getString(_pn_ + "Amounts"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Amounts", Zeze.Serialize.Helper.encodeJson(_Amounts));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Amounts", "list", "", "dynamic"));
        return _v_;
    }
}
