// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// Offline Timer
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BOfflineAccountCustom extends Zeze.Transaction.Bean implements BOfflineAccountCustomReadOnly {
    public static final long TYPEID = -8019295337231502138L;

    private String _TimerName; // 用户指定的timerId(用户指定的,或"@"+Base64编码的自动分配ID)
    private String _Account; // 关联的账号名
    private String _ClientId; // 关联的客户端ID
    private long _LoginVersion; // 创建时从tlocal.LogoutVersion赋值, 用于触发时再与tonline.LogoutVersion验证是否一致
    private String _HandleName; // 用户实现Zeze.Component.TimerHandle接口的完整类名
    private final Zeze.Transaction.DynamicBean _CustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_CustomData() {
        return new Zeze.Transaction.DynamicBean(6, Zeze.Component.Timer::getSpecialTypeIdFromBean, Zeze.Component.Timer::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_6(Zeze.Transaction.Bean _b_) {
        return Zeze.Component.Timer.getSpecialTypeIdFromBean(_b_);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_6(long _t_) {
        return Zeze.Component.Timer.createBeanFromSpecialTypeId(_t_);
    }

    private static final java.lang.invoke.VarHandle vh_TimerName;
    private static final java.lang.invoke.VarHandle vh_Account;
    private static final java.lang.invoke.VarHandle vh_ClientId;
    private static final java.lang.invoke.VarHandle vh_LoginVersion;
    private static final java.lang.invoke.VarHandle vh_HandleName;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_TimerName = _l_.findVarHandle(BOfflineAccountCustom.class, "_TimerName", String.class);
            vh_Account = _l_.findVarHandle(BOfflineAccountCustom.class, "_Account", String.class);
            vh_ClientId = _l_.findVarHandle(BOfflineAccountCustom.class, "_ClientId", String.class);
            vh_LoginVersion = _l_.findVarHandle(BOfflineAccountCustom.class, "_LoginVersion", long.class);
            vh_HandleName = _l_.findVarHandle(BOfflineAccountCustom.class, "_HandleName", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getTimerName() {
        if (!isManaged())
            return _TimerName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TimerName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _TimerName;
    }

    public void setTimerName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TimerName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_TimerName, _v_));
    }

    @Override
    public String getAccount() {
        if (!isManaged())
            return _Account;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Account;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Account;
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
        return log != null ? log.value : _ClientId;
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

    @Override
    public String getHandleName() {
        if (!isManaged())
            return _HandleName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _HandleName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _HandleName;
    }

    public void setHandleName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _HandleName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 5, vh_HandleName, _v_));
    }

    public Zeze.Transaction.DynamicBean getCustomData() {
        return _CustomData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getCustomDataReadOnly() {
        return _CustomData;
    }

    @SuppressWarnings("deprecation")
    public BOfflineAccountCustom() {
        _TimerName = "";
        _Account = "";
        _ClientId = "";
        _LoginVersion = -1;
        _HandleName = "";
        _CustomData = newDynamicBean_CustomData();
    }

    @SuppressWarnings("deprecation")
    public BOfflineAccountCustom(String _TimerName_, String _Account_, String _ClientId_, long _LoginVersion_, String _HandleName_) {
        if (_TimerName_ == null)
            _TimerName_ = "";
        _TimerName = _TimerName_;
        if (_Account_ == null)
            _Account_ = "";
        _Account = _Account_;
        if (_ClientId_ == null)
            _ClientId_ = "";
        _ClientId = _ClientId_;
        _LoginVersion = _LoginVersion_;
        if (_HandleName_ == null)
            _HandleName_ = "";
        _HandleName = _HandleName_;
        _CustomData = newDynamicBean_CustomData();
    }

    @Override
    public void reset() {
        setTimerName("");
        setAccount("");
        setClientId("");
        setLoginVersion(-1);
        setHandleName("");
        _CustomData.reset();
        _unknown_ = null;
    }

    public void assign(BOfflineAccountCustom _o_) {
        setTimerName(_o_.getTimerName());
        setAccount(_o_.getAccount());
        setClientId(_o_.getClientId());
        setLoginVersion(_o_.getLoginVersion());
        setHandleName(_o_.getHandleName());
        _CustomData.assign(_o_._CustomData);
        _unknown_ = _o_._unknown_;
    }

    public BOfflineAccountCustom copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOfflineAccountCustom copy() {
        var _c_ = new BOfflineAccountCustom();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BOfflineAccountCustom _a_, BOfflineAccountCustom _b_) {
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
        _s_.append("Zeze.Builtin.Timer.BOfflineAccountCustom: {\n");
        _s_.append(_i1_).append("TimerName=").append(getTimerName()).append(",\n");
        _s_.append(_i1_).append("Account=").append(getAccount()).append(",\n");
        _s_.append(_i1_).append("ClientId=").append(getClientId()).append(",\n");
        _s_.append(_i1_).append("LoginVersion=").append(getLoginVersion()).append(",\n");
        _s_.append(_i1_).append("HandleName=").append(getHandleName()).append(",\n");
        _s_.append(_i1_).append("CustomData=");
        _CustomData.getBean().buildString(_s_, _l_ + 8);
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
            String _x_ = getTimerName();
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
        {
            String _x_ = getHandleName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _CustomData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.DYNAMIC);
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
        if (_i_ == 1) {
            setTimerName(_o_.ReadString(_t_));
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
        } else
            setLoginVersion(0);
        if (_i_ == 5) {
            setHandleName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _o_.ReadDynamic(_CustomData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BOfflineAccountCustom))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BOfflineAccountCustom)_o_;
        if (!getTimerName().equals(_b_.getTimerName()))
            return false;
        if (!getAccount().equals(_b_.getAccount()))
            return false;
        if (!getClientId().equals(_b_.getClientId()))
            return false;
        if (getLoginVersion() != _b_.getLoginVersion())
            return false;
        if (!getHandleName().equals(_b_.getHandleName()))
            return false;
        if (!_CustomData.equals(_b_._CustomData))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _CustomData.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _CustomData.initRootInfoWithRedo(_r_, this);
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
                case 1: _TimerName = _v_.stringValue(); break;
                case 2: _Account = _v_.stringValue(); break;
                case 3: _ClientId = _v_.stringValue(); break;
                case 4: _LoginVersion = _v_.longValue(); break;
                case 5: _HandleName = _v_.stringValue(); break;
                case 6: _CustomData.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTimerName(_r_.getString(_pn_ + "TimerName"));
        if (getTimerName() == null)
            setTimerName("");
        setAccount(_r_.getString(_pn_ + "Account"));
        if (getAccount() == null)
            setAccount("");
        setClientId(_r_.getString(_pn_ + "ClientId"));
        if (getClientId() == null)
            setClientId("");
        setLoginVersion(_r_.getLong(_pn_ + "LoginVersion"));
        setHandleName(_r_.getString(_pn_ + "HandleName"));
        if (getHandleName() == null)
            setHandleName("");
        Zeze.Serialize.Helper.decodeJsonDynamic(_CustomData, _r_.getString(_pn_ + "CustomData"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "TimerName", getTimerName());
        _s_.appendString(_pn_ + "Account", getAccount());
        _s_.appendString(_pn_ + "ClientId", getClientId());
        _s_.appendLong(_pn_ + "LoginVersion", getLoginVersion());
        _s_.appendString(_pn_ + "HandleName", getHandleName());
        _s_.appendString(_pn_ + "CustomData", Zeze.Serialize.Helper.encodeJson(_CustomData));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TimerName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Account", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ClientId", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "LoginVersion", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "HandleName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "CustomData", "dynamic", "", ""));
        return _v_;
    }
}
