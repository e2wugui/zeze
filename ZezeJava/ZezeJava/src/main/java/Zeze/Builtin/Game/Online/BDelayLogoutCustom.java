// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 用于BTimer.CustomData
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BDelayLogoutCustom extends Zeze.Transaction.Bean implements BDelayLogoutCustomReadOnly {
    public static final long TYPEID = -2195913431542088885L;

    private long _RoleId;
    private long _LoginVersion;
    private String _OnlineSetName;

    private static final java.lang.invoke.VarHandle vh_RoleId;
    private static final java.lang.invoke.VarHandle vh_LoginVersion;
    private static final java.lang.invoke.VarHandle vh_OnlineSetName;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_RoleId = _l_.findVarHandle(BDelayLogoutCustom.class, "_RoleId", long.class);
            vh_LoginVersion = _l_.findVarHandle(BDelayLogoutCustom.class, "_LoginVersion", long.class);
            vh_OnlineSetName = _l_.findVarHandle(BDelayLogoutCustom.class, "_OnlineSetName", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getRoleId() {
        if (!isManaged())
            return _RoleId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RoleId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _RoleId;
    }

    public void setRoleId(long _v_) {
        if (!isManaged()) {
            _RoleId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_RoleId, _v_));
    }

    @Override
    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LoginVersion;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _LoginVersion;
    }

    public void setLoginVersion(long _v_) {
        if (!isManaged()) {
            _LoginVersion = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_LoginVersion, _v_));
    }

    @Override
    public String getOnlineSetName() {
        if (!isManaged())
            return _OnlineSetName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _OnlineSetName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 3);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 3, vh_OnlineSetName, _v_));
    }

    @SuppressWarnings("deprecation")
    public BDelayLogoutCustom() {
        _OnlineSetName = "";
    }

    @SuppressWarnings("deprecation")
    public BDelayLogoutCustom(long _RoleId_, long _LoginVersion_, String _OnlineSetName_) {
        _RoleId = _RoleId_;
        _LoginVersion = _LoginVersion_;
        if (_OnlineSetName_ == null)
            _OnlineSetName_ = "";
        _OnlineSetName = _OnlineSetName_;
    }

    @Override
    public void reset() {
        setRoleId(0);
        setLoginVersion(0);
        setOnlineSetName("");
        _unknown_ = null;
    }

    public void assign(BDelayLogoutCustom _o_) {
        setRoleId(_o_.getRoleId());
        setLoginVersion(_o_.getLoginVersion());
        setOnlineSetName(_o_.getOnlineSetName());
        _unknown_ = _o_._unknown_;
    }

    public BDelayLogoutCustom copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDelayLogoutCustom copy() {
        var _c_ = new BDelayLogoutCustom();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BDelayLogoutCustom _a_, BDelayLogoutCustom _b_) {
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
        _s_.append("Zeze.Builtin.Game.Online.BDelayLogoutCustom: {\n");
        _s_.append(_i1_).append("RoleId=").append(getRoleId()).append(",\n");
        _s_.append(_i1_).append("LoginVersion=").append(getLoginVersion()).append(",\n");
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
            long _x_ = getRoleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getOnlineSetName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            setRoleId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
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
        if (!(_o_ instanceof BDelayLogoutCustom))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BDelayLogoutCustom)_o_;
        if (getRoleId() != _b_.getRoleId())
            return false;
        if (getLoginVersion() != _b_.getLoginVersion())
            return false;
        if (!getOnlineSetName().equals(_b_.getOnlineSetName()))
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
                case 1: _RoleId = _v_.longValue(); break;
                case 2: _LoginVersion = _v_.longValue(); break;
                case 3: _OnlineSetName = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setRoleId(_r_.getLong(_pn_ + "RoleId"));
        setLoginVersion(_r_.getLong(_pn_ + "LoginVersion"));
        setOnlineSetName(_r_.getString(_pn_ + "OnlineSetName"));
        if (getOnlineSetName() == null)
            setOnlineSetName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "RoleId", getRoleId());
        _s_.appendLong(_pn_ + "LoginVersion", getLoginVersion());
        _s_.appendString(_pn_ + "OnlineSetName", getOnlineSetName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "RoleId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "LoginVersion", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "OnlineSetName", "string", "", ""));
        return _v_;
    }
}
