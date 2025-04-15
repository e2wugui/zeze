// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTransmitCancelRoleTimer extends Zeze.Transaction.Bean implements BTransmitCancelRoleTimerReadOnly {
    public static final long TYPEID = 6146715324091616314L;

    private String _TimerId;
    private long _RoleId;
    private long _LoginVersion;

    private static final java.lang.invoke.VarHandle vh_TimerId;
    private static final java.lang.invoke.VarHandle vh_RoleId;
    private static final java.lang.invoke.VarHandle vh_LoginVersion;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_TimerId = _l_.findVarHandle(BTransmitCancelRoleTimer.class, "_TimerId", String.class);
            vh_RoleId = _l_.findVarHandle(BTransmitCancelRoleTimer.class, "_RoleId", long.class);
            vh_LoginVersion = _l_.findVarHandle(BTransmitCancelRoleTimer.class, "_LoginVersion", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getTimerId() {
        if (!isManaged())
            return _TimerId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TimerId;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.stringValue() : _TimerId;
    }

    public void setTimerId(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TimerId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_TimerId, _v_));
    }

    @Override
    public long getRoleId() {
        if (!isManaged())
            return _RoleId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RoleId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _RoleId;
    }

    public void setRoleId(long _v_) {
        if (!isManaged()) {
            _RoleId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_RoleId, _v_));
    }

    @Override
    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LoginVersion;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _LoginVersion;
    }

    public void setLoginVersion(long _v_) {
        if (!isManaged()) {
            _LoginVersion = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_LoginVersion, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTransmitCancelRoleTimer() {
        _TimerId = "";
    }

    @SuppressWarnings("deprecation")
    public BTransmitCancelRoleTimer(String _TimerId_, long _RoleId_, long _LoginVersion_) {
        if (_TimerId_ == null)
            _TimerId_ = "";
        _TimerId = _TimerId_;
        _RoleId = _RoleId_;
        _LoginVersion = _LoginVersion_;
    }

    @Override
    public void reset() {
        setTimerId("");
        setRoleId(0);
        setLoginVersion(0);
        _unknown_ = null;
    }

    public void assign(BTransmitCancelRoleTimer _o_) {
        setTimerId(_o_.getTimerId());
        setRoleId(_o_.getRoleId());
        setLoginVersion(_o_.getLoginVersion());
        _unknown_ = _o_._unknown_;
    }

    public BTransmitCancelRoleTimer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTransmitCancelRoleTimer copy() {
        var _c_ = new BTransmitCancelRoleTimer();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTransmitCancelRoleTimer _a_, BTransmitCancelRoleTimer _b_) {
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
        _s_.append("Zeze.Builtin.Timer.BTransmitCancelRoleTimer: {\n");
        _s_.append(_i1_).append("TimerId=").append(getTimerId()).append(",\n");
        _s_.append(_i1_).append("RoleId=").append(getRoleId()).append(",\n");
        _s_.append(_i1_).append("LoginVersion=").append(getLoginVersion()).append('\n');
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
            String _x_ = getTimerId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getRoleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            setTimerId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setRoleId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTransmitCancelRoleTimer))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTransmitCancelRoleTimer)_o_;
        if (!getTimerId().equals(_b_.getTimerId()))
            return false;
        if (getRoleId() != _b_.getRoleId())
            return false;
        if (getLoginVersion() != _b_.getLoginVersion())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getRoleId() < 0)
            return true;
        if (getLoginVersion() < 0)
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
                case 1: _TimerId = _v_.stringValue(); break;
                case 2: _RoleId = _v_.longValue(); break;
                case 3: _LoginVersion = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTimerId(_r_.getString(_pn_ + "TimerId"));
        if (getTimerId() == null)
            setTimerId("");
        setRoleId(_r_.getLong(_pn_ + "RoleId"));
        setLoginVersion(_r_.getLong(_pn_ + "LoginVersion"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "TimerId", getTimerId());
        _s_.appendLong(_pn_ + "RoleId", getRoleId());
        _s_.appendLong(_pn_ + "LoginVersion", getLoginVersion());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TimerId", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "RoleId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "LoginVersion", "long", "", ""));
        return _v_;
    }
}
