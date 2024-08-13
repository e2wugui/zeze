// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 用于BTimer.CustomData,关联角色的offline timer上下文数据
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BOfflineRoleCustom extends Zeze.Transaction.Bean implements BOfflineRoleCustomReadOnly {
    public static final long TYPEID = -124522910617189691L;

    private String _TimerName; // 用户指定的timerId(用户指定的,或"@"+Base64编码的自动分配ID)
    private long _RoleId; // 关联的角色ID
    private long _LoginVersion; // 创建时从tlocal.LogoutVersion赋值, 用于触发时再与tonline.LogoutVersion验证是否一致
    private String _HandleName; // 用户实现Zeze.Component.TimerHandle接口的完整类名
    private final Zeze.Transaction.DynamicBean _CustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_CustomData() {
        return new Zeze.Transaction.DynamicBean(5, Zeze.Component.Timer::getSpecialTypeIdFromBean, Zeze.Component.Timer::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_5(Zeze.Transaction.Bean _b_) {
        return Zeze.Component.Timer.getSpecialTypeIdFromBean(_b_);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_5(long _t_) {
        return Zeze.Component.Timer.createBeanFromSpecialTypeId(_t_);
    }

    private String _OnlineSetName; // 客户端名

    private static final java.lang.invoke.VarHandle vh_TimerName;
    private static final java.lang.invoke.VarHandle vh_RoleId;
    private static final java.lang.invoke.VarHandle vh_LoginVersion;
    private static final java.lang.invoke.VarHandle vh_HandleName;
    private static final java.lang.invoke.VarHandle vh_OnlineSetName;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_TimerName = _l_.findVarHandle(BOfflineRoleCustom.class, "_TimerName", String.class);
            vh_RoleId = _l_.findVarHandle(BOfflineRoleCustom.class, "_RoleId", long.class);
            vh_LoginVersion = _l_.findVarHandle(BOfflineRoleCustom.class, "_LoginVersion", long.class);
            vh_HandleName = _l_.findVarHandle(BOfflineRoleCustom.class, "_HandleName", String.class);
            vh_OnlineSetName = _l_.findVarHandle(BOfflineRoleCustom.class, "_OnlineSetName", String.class);
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

    @Override
    public String getHandleName() {
        if (!isManaged())
            return _HandleName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _HandleName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 4);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 4, vh_HandleName, _v_));
    }

    public Zeze.Transaction.DynamicBean getCustomData() {
        return _CustomData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getCustomDataReadOnly() {
        return _CustomData;
    }

    @Override
    public String getOnlineSetName() {
        if (!isManaged())
            return _OnlineSetName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _OnlineSetName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _OnlineSetName;
    }

    public void setOnlineSetName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _OnlineSetName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 6, vh_OnlineSetName, _v_));
    }

    @SuppressWarnings("deprecation")
    public BOfflineRoleCustom() {
        _TimerName = "";
        _LoginVersion = -1;
        _HandleName = "";
        _CustomData = newDynamicBean_CustomData();
        _OnlineSetName = "";
    }

    @SuppressWarnings("deprecation")
    public BOfflineRoleCustom(String _TimerName_, long _RoleId_, long _LoginVersion_, String _HandleName_, String _OnlineSetName_) {
        if (_TimerName_ == null)
            _TimerName_ = "";
        _TimerName = _TimerName_;
        _RoleId = _RoleId_;
        _LoginVersion = _LoginVersion_;
        if (_HandleName_ == null)
            _HandleName_ = "";
        _HandleName = _HandleName_;
        _CustomData = newDynamicBean_CustomData();
        if (_OnlineSetName_ == null)
            _OnlineSetName_ = "";
        _OnlineSetName = _OnlineSetName_;
    }

    @Override
    public void reset() {
        setTimerName("");
        setRoleId(0);
        setLoginVersion(-1);
        setHandleName("");
        _CustomData.reset();
        setOnlineSetName("");
        _unknown_ = null;
    }

    public void assign(BOfflineRoleCustom _o_) {
        setTimerName(_o_.getTimerName());
        setRoleId(_o_.getRoleId());
        setLoginVersion(_o_.getLoginVersion());
        setHandleName(_o_.getHandleName());
        _CustomData.assign(_o_._CustomData);
        setOnlineSetName(_o_.getOnlineSetName());
        _unknown_ = _o_._unknown_;
    }

    public BOfflineRoleCustom copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOfflineRoleCustom copy() {
        var _c_ = new BOfflineRoleCustom();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BOfflineRoleCustom _a_, BOfflineRoleCustom _b_) {
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
        _s_.append("Zeze.Builtin.Timer.BOfflineRoleCustom: {\n");
        _s_.append(_i1_).append("TimerName=").append(getTimerName()).append(",\n");
        _s_.append(_i1_).append("RoleId=").append(getRoleId()).append(",\n");
        _s_.append(_i1_).append("LoginVersion=").append(getLoginVersion()).append(",\n");
        _s_.append(_i1_).append("HandleName=").append(getHandleName()).append(",\n");
        _s_.append(_i1_).append("CustomData=");
        _CustomData.getBean().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("OnlineSetName=").append(getOnlineSetName()).append('\n');
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
        {
            String _x_ = getHandleName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _CustomData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            String _x_ = getOnlineSetName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
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
            setRoleId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            setLoginVersion(0);
        if (_i_ == 4) {
            setHandleName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _o_.ReadDynamic(_CustomData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setOnlineSetName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BOfflineRoleCustom))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BOfflineRoleCustom)_o_;
        if (!getTimerName().equals(_b_.getTimerName()))
            return false;
        if (getRoleId() != _b_.getRoleId())
            return false;
        if (getLoginVersion() != _b_.getLoginVersion())
            return false;
        if (!getHandleName().equals(_b_.getHandleName()))
            return false;
        if (!_CustomData.equals(_b_._CustomData))
            return false;
        if (!getOnlineSetName().equals(_b_.getOnlineSetName()))
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
                case 1: _TimerName = _v_.stringValue(); break;
                case 2: _RoleId = _v_.longValue(); break;
                case 3: _LoginVersion = _v_.longValue(); break;
                case 4: _HandleName = _v_.stringValue(); break;
                case 5: _CustomData.followerApply(_v_); break;
                case 6: _OnlineSetName = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTimerName(_r_.getString(_pn_ + "TimerName"));
        if (getTimerName() == null)
            setTimerName("");
        setRoleId(_r_.getLong(_pn_ + "RoleId"));
        setLoginVersion(_r_.getLong(_pn_ + "LoginVersion"));
        setHandleName(_r_.getString(_pn_ + "HandleName"));
        if (getHandleName() == null)
            setHandleName("");
        Zeze.Serialize.Helper.decodeJsonDynamic(_CustomData, _r_.getString(_pn_ + "CustomData"));
        setOnlineSetName(_r_.getString(_pn_ + "OnlineSetName"));
        if (getOnlineSetName() == null)
            setOnlineSetName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "TimerName", getTimerName());
        _s_.appendLong(_pn_ + "RoleId", getRoleId());
        _s_.appendLong(_pn_ + "LoginVersion", getLoginVersion());
        _s_.appendString(_pn_ + "HandleName", getHandleName());
        _s_.appendString(_pn_ + "CustomData", Zeze.Serialize.Helper.encodeJson(_CustomData));
        _s_.appendString(_pn_ + "OnlineSetName", getOnlineSetName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TimerName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "RoleId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "LoginVersion", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "HandleName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "CustomData", "dynamic", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "OnlineSetName", "string", "", ""));
        return _v_;
    }
}
