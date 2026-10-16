// auto-generated @formatter:off
package ClientZezex.Linkd;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BAuth extends Zeze.Transaction.Bean implements BAuthReadOnly {
    public static final long TYPEID = -4648907590533302561L;

    private String _Account;
    private String _Token; // security. maybe password
    private String _AppVersion; // "1.2.3.4"
    private Zeze.Net.Binary _LoginQueueToken;

    private static final java.lang.invoke.VarHandle vh_Account;
    private static final java.lang.invoke.VarHandle vh_Token;
    private static final java.lang.invoke.VarHandle vh_AppVersion;
    private static final java.lang.invoke.VarHandle vh_LoginQueueToken;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Account = _l_.findVarHandle(BAuth.class, "_Account", String.class);
            vh_Token = _l_.findVarHandle(BAuth.class, "_Token", String.class);
            vh_AppVersion = _l_.findVarHandle(BAuth.class, "_AppVersion", String.class);
            vh_LoginQueueToken = _l_.findVarHandle(BAuth.class, "_LoginQueueToken", Zeze.Net.Binary.class);
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
    public String getToken() {
        if (!isManaged())
            return _Token;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Token;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.stringValue() : _Token;
    }

    public void setToken(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Token = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_Token, _v_));
    }

    @Override
    public String getAppVersion() {
        if (!isManaged())
            return _AppVersion;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _AppVersion;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 3);
        return log != null ? log.stringValue() : _AppVersion;
    }

    public void setAppVersion(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _AppVersion = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 3, vh_AppVersion, _v_));
    }

    @Override
    public Zeze.Net.Binary getLoginQueueToken() {
        if (!isManaged())
            return _LoginQueueToken;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LoginQueueToken;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _LoginQueueToken;
    }

    public void setLoginQueueToken(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LoginQueueToken = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 4, vh_LoginQueueToken, _v_));
    }

    @SuppressWarnings("deprecation")
    public BAuth() {
        _Account = "";
        _Token = "";
        _AppVersion = "";
        _LoginQueueToken = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BAuth(@Nullable String _Account_, @Nullable String _Token_, @Nullable String _AppVersion_, Zeze.Net.@Nullable Binary _LoginQueueToken_) {
        if (_Account_ == null)
            _Account_ = "";
        _Account = _Account_;
        if (_Token_ == null)
            _Token_ = "";
        _Token = _Token_;
        if (_AppVersion_ == null)
            _AppVersion_ = "";
        _AppVersion = _AppVersion_;
        if (_LoginQueueToken_ == null)
            _LoginQueueToken_ = Zeze.Net.Binary.Empty;
        _LoginQueueToken = _LoginQueueToken_;
    }

    @Override
    public void reset() {
        setAccount("");
        setToken("");
        setAppVersion("");
        setLoginQueueToken(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    public void assign(BAuth _o_) {
        setAccount(_o_.getAccount());
        setToken(_o_.getToken());
        setAppVersion(_o_.getAppVersion());
        setLoginQueueToken(_o_.getLoginQueueToken());
        _unknown_ = _o_._unknown_;
    }

    public BAuth copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAuth copy() {
        var _c_ = new BAuth();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAuth _a_, BAuth _b_) {
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
        _s_.append("ClientZezex.Linkd.BAuth: {\n");
        _s_.append(_i1_).append("Account=").append(getAccount()).append(",\n");
        _s_.append(_i1_).append("Token=").append(getToken()).append(",\n");
        _s_.append(_i1_).append("AppVersion=").append(getAppVersion()).append(",\n");
        _s_.append(_i1_).append("LoginQueueToken=").append(getLoginQueueToken()).append('\n');
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
            String _x_ = getAccount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getToken();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getAppVersion();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getLoginQueueToken();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
            setToken(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setAppVersion(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setLoginQueueToken(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BAuth))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAuth)_o_;
        if (!getAccount().equals(_b_.getAccount()))
            return false;
        if (!getToken().equals(_b_.getToken()))
            return false;
        if (!getAppVersion().equals(_b_.getAppVersion()))
            return false;
        if (!getLoginQueueToken().equals(_b_.getLoginQueueToken()))
            return false;
        return true;
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
                case 2: _Token = _v_.stringValue(); break;
                case 3: _AppVersion = _v_.stringValue(); break;
                case 4: _LoginQueueToken = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setAccount(_r_.getString(_pn_ + "Account"));
        if (getAccount() == null)
            setAccount("");
        setToken(_r_.getString(_pn_ + "Token"));
        if (getToken() == null)
            setToken("");
        setAppVersion(_r_.getString(_pn_ + "AppVersion"));
        if (getAppVersion() == null)
            setAppVersion("");
        setLoginQueueToken(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "LoginQueueToken")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Account", getAccount());
        _s_.appendString(_pn_ + "Token", getToken());
        _s_.appendString(_pn_ + "AppVersion", getAppVersion());
        _s_.appendBinary(_pn_ + "LoginQueueToken", getLoginQueueToken());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Account", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Token", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "AppVersion", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "LoginQueueToken", "binary", "", ""));
        return _v_;
    }
}
