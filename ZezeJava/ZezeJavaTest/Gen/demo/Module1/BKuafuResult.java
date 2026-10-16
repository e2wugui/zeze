// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BKuafuResult extends Zeze.Transaction.Bean implements BKuafuResultReadOnly {
    public static final long TYPEID = -4375085639152583164L;

    private long _Money;

    private static final java.lang.invoke.VarHandle vh_Money;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Money = _l_.findVarHandle(BKuafuResult.class, "_Money", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getMoney() {
        if (!isManaged())
            return _Money;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Money;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Money;
    }

    public void setMoney(long _v_) {
        if (!isManaged()) {
            _Money = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_Money, _v_));
    }

    @SuppressWarnings("deprecation")
    public BKuafuResult() {
    }

    @SuppressWarnings("deprecation")
    public BKuafuResult(long _Money_) {
        _Money = _Money_;
    }

    @Override
    public void reset() {
        setMoney(0);
        _unknown_ = null;
    }

    @Override
    public demo.Module1.BKuafuResult.Data toData() {
        var _d_ = new demo.Module1.BKuafuResult.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((demo.Module1.BKuafuResult.Data)_o_);
    }

    public void assign(BKuafuResult.Data _o_) {
        setMoney(_o_._Money);
        _unknown_ = null;
    }

    public void assign(BKuafuResult _o_) {
        setMoney(_o_.getMoney());
        _unknown_ = _o_._unknown_;
    }

    public BKuafuResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BKuafuResult copy() {
        var _c_ = new BKuafuResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BKuafuResult _a_, BKuafuResult _b_) {
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
        _s_.append("demo.Module1.BKuafuResult: {\n");
        _s_.append(_i1_).append("Money=").append(getMoney()).append('\n');
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
            long _x_ = getMoney();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            setMoney(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BKuafuResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BKuafuResult)_o_;
        if (getMoney() != _b_.getMoney())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getMoney() < 0)
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
                case 1: _Money = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setMoney(_r_.getLong(_pn_ + "Money"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "Money", getMoney());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Money", "long", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4375085639152583164L;

    private long _Money;

    public long getMoney() {
        return _Money;
    }

    public void setMoney(long _v_) {
        _Money = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(long _Money_) {
        _Money = _Money_;
    }

    @Override
    public void reset() {
        _Money = 0;
    }

    @Override
    public demo.Module1.BKuafuResult toBean() {
        var _b_ = new demo.Module1.BKuafuResult();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BKuafuResult)_o_);
    }

    public void assign(BKuafuResult _o_) {
        _Money = _o_.getMoney();
    }

    public void assign(BKuafuResult.Data _o_) {
        _Money = _o_._Money;
    }

    @Override
    public BKuafuResult.Data copy() {
        var _c_ = new BKuafuResult.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BKuafuResult.Data _a_, BKuafuResult.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BKuafuResult.Data clone() {
        return (BKuafuResult.Data)super.clone();
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
        _s_.append("demo.Module1.BKuafuResult: {\n");
        _s_.append(_i1_).append("Money=").append(_Money).append('\n');
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
            long _x_ = _Money;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Money = _o_.ReadLong(_t_);
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
        if (!(_o_ instanceof BKuafuResult.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BKuafuResult.Data)_o_;
        if (_Money != _b_._Money)
            return false;
        return true;
    }
}
}
