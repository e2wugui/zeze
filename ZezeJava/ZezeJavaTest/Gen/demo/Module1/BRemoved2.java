// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

/*
		bean的
		多行注释
*/
@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BRemoved2 extends Zeze.Transaction.Bean implements BRemoved2ReadOnly {
    public static final long TYPEID = -3773667973386893417L;

    private int _int_1; // com aa

    private static final java.lang.invoke.VarHandle vh_int_1;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_int_1 = _l_.findVarHandle(BRemoved2.class, "_int_1", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getInt_1() {
        if (!isManaged())
            return _int_1;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _int_1;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _int_1;
    }

    public void setInt_1(int _v_) {
        if (!isManaged()) {
            _int_1 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_int_1, _v_));
    }

    @SuppressWarnings("deprecation")
    public BRemoved2() {
    }

    @SuppressWarnings("deprecation")
    public BRemoved2(int _int_1_) {
        _int_1 = _int_1_;
    }

    @Override
    public void reset() {
        setInt_1(0);
        _unknown_ = null;
    }

    @Override
    public demo.Module1.BRemoved2.Data toData() {
        var _d_ = new demo.Module1.BRemoved2.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((demo.Module1.BRemoved2.Data)_o_);
    }

    public void assign(BRemoved2.Data _o_) {
        setInt_1(_o_._int_1);
        _unknown_ = null;
    }

    public void assign(BRemoved2 _o_) {
        setInt_1(_o_.getInt_1());
        _unknown_ = _o_._unknown_;
    }

    public BRemoved2 copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRemoved2 copy() {
        var _c_ = new BRemoved2();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BRemoved2 _a_, BRemoved2 _b_) {
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
        _s_.append("demo.Module1.BRemoved2: {\n");
        _s_.append(_i1_).append("int_1=").append(getInt_1()).append('\n');
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
            int _x_ = getInt_1();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            setInt_1(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BRemoved2))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BRemoved2)_o_;
        if (getInt_1() != _b_.getInt_1())
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + Integer.hashCode(getInt_1());
        return _h_;
    }

    @Override
    public boolean negativeCheck() {
        if (getInt_1() < 0)
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
                case 1: _int_1 = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setInt_1(_r_.getInt(_pn_ + "int_1"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "int_1", getInt_1());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "int_1", "int", "", ""));
        return _v_;
    }

/*
		bean的
		多行注释
*/
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -3773667973386893417L;

    private int _int_1; // com aa

    public int getInt_1() {
        return _int_1;
    }

    public void setInt_1(int _v_) {
        _int_1 = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(int _int_1_) {
        _int_1 = _int_1_;
    }

    @Override
    public void reset() {
        _int_1 = 0;
    }

    @Override
    public demo.Module1.BRemoved2 toBean() {
        var _b_ = new demo.Module1.BRemoved2();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BRemoved2)_o_);
    }

    public void assign(BRemoved2 _o_) {
        _int_1 = _o_.getInt_1();
    }

    public void assign(BRemoved2.Data _o_) {
        _int_1 = _o_._int_1;
    }

    @Override
    public BRemoved2.Data copy() {
        var _c_ = new BRemoved2.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BRemoved2.Data _a_, BRemoved2.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BRemoved2.Data clone() {
        return (BRemoved2.Data)super.clone();
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
        _s_.append("demo.Module1.BRemoved2: {\n");
        _s_.append(_i1_).append("int_1=").append(_int_1).append('\n');
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
            int _x_ = _int_1;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _int_1 = _o_.ReadInt(_t_);
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
        if (!(_o_ instanceof BRemoved2.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BRemoved2.Data)_o_;
        if (_int_1 != _b_._int_1)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + Integer.hashCode(_int_1);
        return _h_;
    }
}
}
