// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 用于Zeze.Timer.tAccountTimers内存表的value, 只处理账号在线时的定时器
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BArchOnlineTimer extends Zeze.Transaction.Bean implements BArchOnlineTimerReadOnly {
    public static final long TYPEID = -1410268970794351805L;

    private String _Account; // 所属的账号名
    private String _ClientId; // 所属的客户端ID
    private final Zeze.Transaction.DynamicBean _TimerObj;

    public static final long DynamicTypeId_TimerObj_Zeze_Builtin_Timer_BCronTimer = -6995089347718168392L;

    public static final long DynamicTypeId_TimerObj_Zeze_Builtin_Timer_BSimpleTimer = 1832177636612857692L;

    public static Zeze.Transaction.DynamicBean newDynamicBean_TimerObj() {
        return new Zeze.Transaction.DynamicBean(3, BArchOnlineTimer::getSpecialTypeIdFromBean_3, BArchOnlineTimer::createBeanFromSpecialTypeId_3);
    }

    public static long getSpecialTypeIdFromBean_3(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == -6995089347718168392L)
            return -6995089347718168392L; // Zeze.Builtin.Timer.BCronTimer
        if (_t_ == 1832177636612857692L)
            return 1832177636612857692L; // Zeze.Builtin.Timer.BSimpleTimer
        throw new UnsupportedOperationException("Unknown Bean! dynamic@Zeze.Builtin.Timer.BArchOnlineTimer:TimerObj:" + _t_);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_3(long _t_) {
        if (_t_ == -6995089347718168392L)
            return new Zeze.Builtin.Timer.BCronTimer();
        if (_t_ == 1832177636612857692L)
            return new Zeze.Builtin.Timer.BSimpleTimer();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private long _LoginVersion; // 创建时从tlocal.LoginVersion赋值, 用于触发时再与tonline.LoginVersion验证是否一致
    private long _SerialId; // 创建时从AutoKey("Zeze.Component.Timer.SerialId")分配, 用于触发后验证是否重置了该定时器

    private static final java.lang.invoke.VarHandle vh_Account;
    private static final java.lang.invoke.VarHandle vh_ClientId;
    private static final java.lang.invoke.VarHandle vh_LoginVersion;
    private static final java.lang.invoke.VarHandle vh_SerialId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Account = _l_.findVarHandle(BArchOnlineTimer.class, "_Account", String.class);
            vh_ClientId = _l_.findVarHandle(BArchOnlineTimer.class, "_ClientId", String.class);
            vh_LoginVersion = _l_.findVarHandle(BArchOnlineTimer.class, "_LoginVersion", long.class);
            vh_SerialId = _l_.findVarHandle(BArchOnlineTimer.class, "_SerialId", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getAccount() {
        if (!isManaged())
            return _Account;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Account;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_Account, _v_));
    }

    @Override
    public String getClientId() {
        if (!isManaged())
            return _ClientId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ClientId;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_ClientId, _v_));
    }

    public Zeze.Transaction.DynamicBean getTimerObj() {
        return _TimerObj;
    }

    public Zeze.Builtin.Timer.BCronTimer getTimerObj_Zeze_Builtin_Timer_BCronTimer() {
        return (Zeze.Builtin.Timer.BCronTimer)_TimerObj.getBean();
    }

    public void setTimerObj(Zeze.Builtin.Timer.BCronTimer _v_) {
        _TimerObj.setBean(_v_);
    }

    public Zeze.Builtin.Timer.BSimpleTimer getTimerObj_Zeze_Builtin_Timer_BSimpleTimer() {
        return (Zeze.Builtin.Timer.BSimpleTimer)_TimerObj.getBean();
    }

    public void setTimerObj(Zeze.Builtin.Timer.BSimpleTimer _v_) {
        _TimerObj.setBean(_v_);
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getTimerObjReadOnly() {
        return _TimerObj;
    }

    @Override
    public Zeze.Builtin.Timer.BCronTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BCronTimerReadOnly() {
        return (Zeze.Builtin.Timer.BCronTimer)_TimerObj.getBean();
    }

    @Override
    public Zeze.Builtin.Timer.BSimpleTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BSimpleTimerReadOnly() {
        return (Zeze.Builtin.Timer.BSimpleTimer)_TimerObj.getBean();
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

    @Override
    public long getSerialId() {
        if (!isManaged())
            return _SerialId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _SerialId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _SerialId;
    }

    public void setSerialId(long _v_) {
        if (!isManaged()) {
            _SerialId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 5, vh_SerialId, _v_));
    }

    @SuppressWarnings("deprecation")
    public BArchOnlineTimer() {
        _Account = "";
        _ClientId = "";
        _TimerObj = newDynamicBean_TimerObj();
    }

    @SuppressWarnings("deprecation")
    public BArchOnlineTimer(String _Account_, String _ClientId_, long _LoginVersion_, long _SerialId_) {
        if (_Account_ == null)
            _Account_ = "";
        _Account = _Account_;
        if (_ClientId_ == null)
            _ClientId_ = "";
        _ClientId = _ClientId_;
        _TimerObj = newDynamicBean_TimerObj();
        _LoginVersion = _LoginVersion_;
        _SerialId = _SerialId_;
    }

    @Override
    public void reset() {
        setAccount("");
        setClientId("");
        _TimerObj.reset();
        setLoginVersion(0);
        setSerialId(0);
        _unknown_ = null;
    }

    public void assign(BArchOnlineTimer _o_) {
        setAccount(_o_.getAccount());
        setClientId(_o_.getClientId());
        _TimerObj.assign(_o_._TimerObj);
        setLoginVersion(_o_.getLoginVersion());
        setSerialId(_o_.getSerialId());
        _unknown_ = _o_._unknown_;
    }

    public BArchOnlineTimer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BArchOnlineTimer copy() {
        var _c_ = new BArchOnlineTimer();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BArchOnlineTimer _a_, BArchOnlineTimer _b_) {
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
        _s_.append("Zeze.Builtin.Timer.BArchOnlineTimer: {\n");
        _s_.append(_i1_).append("Account=").append(getAccount()).append(",\n");
        _s_.append(_i1_).append("ClientId=").append(getClientId()).append(",\n");
        _s_.append(_i1_).append("TimerObj=");
        _TimerObj.getBean().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("LoginVersion=").append(getLoginVersion()).append(",\n");
        _s_.append(_i1_).append("SerialId=").append(getSerialId()).append('\n');
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
            String _x_ = getAccount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getClientId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _TimerObj;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            long _x_ = getLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getSerialId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
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
            setAccount(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setClientId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadDynamic(_TimerObj, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setSerialId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BArchOnlineTimer))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BArchOnlineTimer)_o_;
        if (!getAccount().equals(_b_.getAccount()))
            return false;
        if (!getClientId().equals(_b_.getClientId()))
            return false;
        if (!_TimerObj.equals(_b_._TimerObj))
            return false;
        if (getLoginVersion() != _b_.getLoginVersion())
            return false;
        if (getSerialId() != _b_.getSerialId())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _TimerObj.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _TimerObj.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_TimerObj.negativeCheck())
            return true;
        if (getLoginVersion() < 0)
            return true;
        if (getSerialId() < 0)
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
                case 1: _Account = _v_.stringValue(); break;
                case 2: _ClientId = _v_.stringValue(); break;
                case 3: _TimerObj.followerApply(_v_); break;
                case 4: _LoginVersion = _v_.longValue(); break;
                case 5: _SerialId = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setAccount(_r_.getString(_pn_ + "Account"));
        if (getAccount() == null)
            setAccount("");
        setClientId(_r_.getString(_pn_ + "ClientId"));
        if (getClientId() == null)
            setClientId("");
        Zeze.Serialize.Helper.decodeJsonDynamic(_TimerObj, _r_.getString(_pn_ + "TimerObj"));
        setLoginVersion(_r_.getLong(_pn_ + "LoginVersion"));
        setSerialId(_r_.getLong(_pn_ + "SerialId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Account", getAccount());
        _s_.appendString(_pn_ + "ClientId", getClientId());
        _s_.appendString(_pn_ + "TimerObj", Zeze.Serialize.Helper.encodeJson(_TimerObj));
        _s_.appendLong(_pn_ + "LoginVersion", getLoginVersion());
        _s_.appendLong(_pn_ + "SerialId", getSerialId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Account", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ClientId", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "TimerObj", "dynamic", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "LoginVersion", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "SerialId", "long", "", ""));
        return _v_;
    }
}
