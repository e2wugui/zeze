// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTestSchemas extends Zeze.Transaction.Bean implements BTestSchemasReadOnly {
    public static final long TYPEID = -3970310617082156489L;

    private long _Var1;
    private long _Var2;

    private static final java.lang.invoke.VarHandle vh_Var1;
    private static final java.lang.invoke.VarHandle vh_Var2;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Var1 = _l_.findVarHandle(BTestSchemas.class, "_Var1", long.class);
            vh_Var2 = _l_.findVarHandle(BTestSchemas.class, "_Var2", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getVar1() {
        if (!isManaged())
            return _Var1;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Var1;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Var1;
    }

    public void setVar1(long _v_) {
        if (!isManaged()) {
            _Var1 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_Var1, _v_));
    }

    @Override
    public long getVar2() {
        if (!isManaged())
            return _Var2;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Var2;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Var2;
    }

    public void setVar2(long _v_) {
        if (!isManaged()) {
            _Var2 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_Var2, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTestSchemas() {
    }

    @SuppressWarnings("deprecation")
    public BTestSchemas(long _Var1_, long _Var2_) {
        _Var1 = _Var1_;
        _Var2 = _Var2_;
    }

    @Override
    public void reset() {
        setVar1(0);
        setVar2(0);
        _unknown_ = null;
    }

    public void assign(BTestSchemas _o_) {
        setVar1(_o_.getVar1());
        setVar2(_o_.getVar2());
        _unknown_ = _o_._unknown_;
    }

    public BTestSchemas copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTestSchemas copy() {
        var _c_ = new BTestSchemas();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTestSchemas _a_, BTestSchemas _b_) {
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
        _s_.append("demo.Module1.BTestSchemas: {\n");
        _s_.append(_i1_).append("Var1=").append(getVar1()).append(",\n");
        _s_.append(_i1_).append("Var2=").append(getVar2()).append('\n');
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
            long _x_ = getVar1();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getVar2();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setVar1(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setVar2(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTestSchemas))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTestSchemas)_o_;
        if (getVar1() != _b_.getVar1())
            return false;
        if (getVar2() != _b_.getVar2())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getVar1() < 0)
            return true;
        if (getVar2() < 0)
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
                case 1: _Var1 = _v_.longValue(); break;
                case 2: _Var2 = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setVar1(_r_.getLong(_pn_ + "Var1"));
        setVar2(_r_.getLong(_pn_ + "Var2"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "Var1", getVar1());
        _s_.appendLong(_pn_ + "Var2", getVar2());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Var1", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Var2", "long", "", ""));
        return _v_;
    }
}
