// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTransmitCancelAccountTimer extends Zeze.Transaction.Bean implements BTransmitCancelAccountTimerReadOnly {
    public static final long TYPEID = -4957261049626394033L;

    private String _TimerId;
    private String _Account;
    private String _ClientId;
    private long _LoginVersion;

    private static final java.lang.invoke.VarHandle vh_TimerId;
    private static final java.lang.invoke.VarHandle vh_Account;
    private static final java.lang.invoke.VarHandle vh_ClientId;
    private static final java.lang.invoke.VarHandle vh_LoginVersion;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_TimerId = _l_.findVarHandle(BTransmitCancelAccountTimer.class, "_TimerId", String.class);
            vh_Account = _l_.findVarHandle(BTransmitCancelAccountTimer.class, "_Account", String.class);
            vh_ClientId = _l_.findVarHandle(BTransmitCancelAccountTimer.class, "_ClientId", String.class);
            vh_LoginVersion = _l_.findVarHandle(BTransmitCancelAccountTimer.class, "_LoginVersion", long.class);
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
    public String getAccount() {
        if (!isManaged())
            return _Account;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Account;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.stringValue() : _Account;
    }

    public void setAccount(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Account = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_Account, _v_));
    }

    @Override
    public String getClientId() {
        if (!isManaged())
            return _ClientId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ClientId;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 3);
        return log != null ? log.stringValue() : _ClientId;
    }

    public void setClientId(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ClientId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 3, vh_ClientId, _v_));
    }

    @Override
    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LoginVersion;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _LoginVersion;
    }

    public void setLoginVersion(long _v_) {
        if (!isManaged()) {
            _LoginVersion = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 4, vh_LoginVersion, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTransmitCancelAccountTimer() {
        _TimerId = "";
        _Account = "";
        _ClientId = "";
    }

    @SuppressWarnings("deprecation")
    public BTransmitCancelAccountTimer(String _TimerId_, String _Account_, String _ClientId_, long _LoginVersion_) {
        if (_TimerId_ == null)
            _TimerId_ = "";
        _TimerId = _TimerId_;
        if (_Account_ == null)
            _Account_ = "";
        _Account = _Account_;
        if (_ClientId_ == null)
            _ClientId_ = "";
        _ClientId = _ClientId_;
        _LoginVersion = _LoginVersion_;
    }

    @Override
    public void reset() {
        setTimerId("");
        setAccount("");
        setClientId("");
        setLoginVersion(0);
        _unknown_ = null;
    }

    public void assign(BTransmitCancelAccountTimer _o_) {
        setTimerId(_o_.getTimerId());
        setAccount(_o_.getAccount());
        setClientId(_o_.getClientId());
        setLoginVersion(_o_.getLoginVersion());
        _unknown_ = _o_._unknown_;
    }

    public BTransmitCancelAccountTimer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTransmitCancelAccountTimer copy() {
        var _c_ = new BTransmitCancelAccountTimer();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTransmitCancelAccountTimer _a_, BTransmitCancelAccountTimer _b_) {
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
        _s_.append("Zeze.Builtin.Timer.BTransmitCancelAccountTimer: {\n");
        _s_.append(_i1_).append("TimerId=").append(getTimerId()).append(",\n");
        _s_.append(_i1_).append("Account=").append(getAccount()).append(",\n");
        _s_.append(_i1_).append("ClientId=").append(getClientId()).append(",\n");
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
            String _x_ = getAccount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getClientId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
            setAccount(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setClientId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
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
        if (!(_o_ instanceof BTransmitCancelAccountTimer))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTransmitCancelAccountTimer)_o_;
        if (!getTimerId().equals(_b_.getTimerId()))
            return false;
        if (!getAccount().equals(_b_.getAccount()))
            return false;
        if (!getClientId().equals(_b_.getClientId()))
            return false;
        if (getLoginVersion() != _b_.getLoginVersion())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
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
                case 2: _Account = _v_.stringValue(); break;
                case 3: _ClientId = _v_.stringValue(); break;
                case 4: _LoginVersion = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTimerId(_r_.getString(_pn_ + "TimerId"));
        if (getTimerId() == null)
            setTimerId("");
        setAccount(_r_.getString(_pn_ + "Account"));
        if (getAccount() == null)
            setAccount("");
        setClientId(_r_.getString(_pn_ + "ClientId"));
        if (getClientId() == null)
            setClientId("");
        setLoginVersion(_r_.getLong(_pn_ + "LoginVersion"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "TimerId", getTimerId());
        _s_.appendString(_pn_ + "Account", getAccount());
        _s_.appendString(_pn_ + "ClientId", getClientId());
        _s_.appendLong(_pn_ + "LoginVersion", getLoginVersion());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TimerId", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Account", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ClientId", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "LoginVersion", "long", "", ""));
        return _v_;
    }
}
