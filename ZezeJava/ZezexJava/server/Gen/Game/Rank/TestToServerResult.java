// auto-generated @formatter:off
package Game.Rank;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class TestToServerResult extends Zeze.Transaction.Bean implements TestToServerResultReadOnly {
    public static final long TYPEID = 1369012121316432420L;

    private long _resultCode; // 类型是long,名字是resultCode是特殊字段,表示远程执行的结果,0表示正常,如果是超时或异常则可能得不到正确的其它字段值. 此字段是可选的
    private int _out; // 以下是其它的自定义字段
    private int _serverId;

    private static final java.lang.invoke.VarHandle vh_resultCode;
    private static final java.lang.invoke.VarHandle vh_out;
    private static final java.lang.invoke.VarHandle vh_serverId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_resultCode = _l_.findVarHandle(TestToServerResult.class, "_resultCode", long.class);
            vh_out = _l_.findVarHandle(TestToServerResult.class, "_out", int.class);
            vh_serverId = _l_.findVarHandle(TestToServerResult.class, "_serverId", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getResultCode() {
        if (!isManaged())
            return _resultCode;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _resultCode;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _resultCode;
    }

    public void setResultCode(long _v_) {
        if (!isManaged()) {
            _resultCode = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_resultCode, _v_));
    }

    @Override
    public int getOut() {
        if (!isManaged())
            return _out;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _out;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _out;
    }

    public void setOut(int _v_) {
        if (!isManaged()) {
            _out = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_out, _v_));
    }

    @Override
    public int getServerId() {
        if (!isManaged())
            return _serverId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _serverId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _serverId;
    }

    public void setServerId(int _v_) {
        if (!isManaged()) {
            _serverId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_serverId, _v_));
    }

    @SuppressWarnings("deprecation")
    public TestToServerResult() {
    }

    @SuppressWarnings("deprecation")
    public TestToServerResult(long _resultCode_, int _out_, int _serverId_) {
        _resultCode = _resultCode_;
        _out = _out_;
        _serverId = _serverId_;
    }

    @Override
    public void reset() {
        setResultCode(0);
        setOut(0);
        setServerId(0);
        _unknown_ = null;
    }

    public void assign(TestToServerResult _o_) {
        setResultCode(_o_.getResultCode());
        setOut(_o_.getOut());
        setServerId(_o_.getServerId());
        _unknown_ = _o_._unknown_;
    }

    public TestToServerResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public TestToServerResult copy() {
        var _c_ = new TestToServerResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(TestToServerResult _a_, TestToServerResult _b_) {
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
        _s_.append("Game.Rank.TestToServerResult: {\n");
        _s_.append(_i1_).append("resultCode=").append(getResultCode()).append(",\n");
        _s_.append(_i1_).append("out=").append(getOut()).append(",\n");
        _s_.append(_i1_).append("serverId=").append(getServerId()).append('\n');
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
            long _x_ = getResultCode();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getOut();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            setResultCode(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setOut(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof TestToServerResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (TestToServerResult)_o_;
        if (getResultCode() != _b_.getResultCode())
            return false;
        if (getOut() != _b_.getOut())
            return false;
        if (getServerId() != _b_.getServerId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getResultCode() < 0)
            return true;
        if (getOut() < 0)
            return true;
        if (getServerId() < 0)
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
                case 1: _resultCode = _v_.longValue(); break;
                case 2: _out = _v_.intValue(); break;
                case 3: _serverId = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setResultCode(_r_.getLong(_pn_ + "resultCode"));
        setOut(_r_.getInt(_pn_ + "out"));
        setServerId(_r_.getInt(_pn_ + "serverId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "resultCode", getResultCode());
        _s_.appendInt(_pn_ + "out", getOut());
        _s_.appendInt(_pn_ + "serverId", getServerId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "resultCode", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "out", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "serverId", "int", "", ""));
        return _v_;
    }
}
