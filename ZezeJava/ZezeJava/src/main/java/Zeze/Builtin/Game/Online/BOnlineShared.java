// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// tables
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BOnlineShared extends Zeze.Transaction.Bean implements BOnlineSharedReadOnly {
    public static final long TYPEID = -281067677727319713L;

    private String _Account; // 所属账号, 用于登录验证
    private Zeze.Builtin.Game.Online.BLink _Link; // link相关状态, 登录和下线时会更新
    private long _LoginVersion; // 角色登录(包括重登录)时自增
    private long _LogoutVersion; // 登录和下线前会赋值为LoginVersion
    private final Zeze.Transaction.DynamicBean _UserData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_UserData() {
        return new Zeze.Transaction.DynamicBean(5, Zeze.Game.Online::getSpecialTypeIdFromBean, Zeze.Game.Online::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_5(Zeze.Transaction.Bean _b_) {
        return Zeze.Game.Online.getSpecialTypeIdFromBean(_b_);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_5(long _t_) {
        return Zeze.Game.Online.createBeanFromSpecialTypeId(_t_);
    }

    @Override
    public String getAccount() {
        if (!isManaged())
            return _Account;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Account;
        var log = (Log__Account)_t_.getLog(objectId() + 1);
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
        _t_.putLog(new Log__Account(this, 1, _v_));
    }

    @Override
    public Zeze.Builtin.Game.Online.BLink getLink() {
        if (!isManaged())
            return _Link;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Link;
        var log = (Log__Link)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Link;
    }

    public void setLink(Zeze.Builtin.Game.Online.BLink _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Link = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Link(this, 2, _v_));
    }

    @Override
    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LoginVersion;
        var log = (Log__LoginVersion)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _LoginVersion;
    }

    public void setLoginVersion(long _v_) {
        if (!isManaged()) {
            _LoginVersion = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__LoginVersion(this, 3, _v_));
    }

    @Override
    public long getLogoutVersion() {
        if (!isManaged())
            return _LogoutVersion;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LogoutVersion;
        var log = (Log__LogoutVersion)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _LogoutVersion;
    }

    public void setLogoutVersion(long _v_) {
        if (!isManaged()) {
            _LogoutVersion = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__LogoutVersion(this, 4, _v_));
    }

    public Zeze.Transaction.DynamicBean getUserData() {
        return _UserData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getUserDataReadOnly() {
        return _UserData;
    }

    @SuppressWarnings("deprecation")
    public BOnlineShared() {
        _Account = "";
        _Link = new Zeze.Builtin.Game.Online.BLink();
        _UserData = newDynamicBean_UserData();
    }

    @SuppressWarnings("deprecation")
    public BOnlineShared(String _Account_, Zeze.Builtin.Game.Online.BLink _Link_, long _LoginVersion_, long _LogoutVersion_) {
        if (_Account_ == null)
            _Account_ = "";
        _Account = _Account_;
        if (_Link_ == null)
            _Link_ = new Zeze.Builtin.Game.Online.BLink();
        _Link = _Link_;
        _LoginVersion = _LoginVersion_;
        _LogoutVersion = _LogoutVersion_;
        _UserData = newDynamicBean_UserData();
    }

    @Override
    public void reset() {
        setAccount("");
        setLink(new Zeze.Builtin.Game.Online.BLink());
        setLoginVersion(0);
        setLogoutVersion(0);
        _UserData.reset();
        _unknown_ = null;
    }

    public void assign(BOnlineShared _o_) {
        setAccount(_o_.getAccount());
        setLink(_o_.getLink());
        setLoginVersion(_o_.getLoginVersion());
        setLogoutVersion(_o_.getLogoutVersion());
        _UserData.assign(_o_._UserData);
        _unknown_ = _o_._unknown_;
    }

    public BOnlineShared copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOnlineShared copy() {
        var _c_ = new BOnlineShared();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BOnlineShared _a_, BOnlineShared _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Account extends Zeze.Transaction.Logs.LogString {
        public Log__Account(BOnlineShared _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BOnlineShared)getBelong())._Account = value; }
    }

    private static final class Log__Link extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Game.Online.BLink> {
        public Log__Link(BOnlineShared _b_, int _i_, Zeze.Builtin.Game.Online.BLink _v_) { super(Zeze.Builtin.Game.Online.BLink.class, _b_, _i_, _v_); }

        @Override
        public void commit() { ((BOnlineShared)getBelong())._Link = value; }
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LoginVersion(BOnlineShared _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BOnlineShared)getBelong())._LoginVersion = value; }
    }

    private static final class Log__LogoutVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LogoutVersion(BOnlineShared _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BOnlineShared)getBelong())._LogoutVersion = value; }
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
        _s_.append("Zeze.Builtin.Game.Online.BOnlineShared: {\n");
        _s_.append(_i1_).append("Account=").append(getAccount()).append(",\n");
        _s_.append(_i1_).append("Link=");
        getLink().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("LoginVersion=").append(getLoginVersion()).append(",\n");
        _s_.append(_i1_).append("LogoutVersion=").append(getLogoutVersion()).append(",\n");
        _s_.append(_i1_).append("UserData=");
        _UserData.getBean().buildString(_s_, _l_ + 8);
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
            String _x_ = getAccount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getLink().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            long _x_ = getLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getLogoutVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _UserData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.DYNAMIC);
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
            setAccount(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadBean(getLink(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setLogoutVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _o_.ReadDynamic(_UserData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BOnlineShared))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BOnlineShared)_o_;
        if (!getAccount().equals(_b_.getAccount()))
            return false;
        if (!getLink().equals(_b_.getLink()))
            return false;
        if (getLoginVersion() != _b_.getLoginVersion())
            return false;
        if (getLogoutVersion() != _b_.getLogoutVersion())
            return false;
        if (!_UserData.equals(_b_._UserData))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _UserData.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _UserData.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getLink().negativeCheck())
            return true;
        if (getLoginVersion() < 0)
            return true;
        if (getLogoutVersion() < 0)
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
                case 2: _Link = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Game.Online.BLink>)_v_).value; break;
                case 3: _LoginVersion = _v_.longValue(); break;
                case 4: _LogoutVersion = _v_.longValue(); break;
                case 5: _UserData.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setAccount(_r_.getString(_pn_ + "Account"));
        if (getAccount() == null)
            setAccount("");
        _p_.add("Link");
        getLink().decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
        setLoginVersion(_r_.getLong(_pn_ + "LoginVersion"));
        setLogoutVersion(_r_.getLong(_pn_ + "LogoutVersion"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_UserData, _r_.getString(_pn_ + "UserData"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Account", getAccount());
        _p_.add("Link");
        getLink().encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        _s_.appendLong(_pn_ + "LoginVersion", getLoginVersion());
        _s_.appendLong(_pn_ + "LogoutVersion", getLogoutVersion());
        _s_.appendString(_pn_ + "UserData", Zeze.Serialize.Helper.encodeJson(_UserData));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Account", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Link", "Zeze.Builtin.Game.Online.BLink", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "LoginVersion", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "LogoutVersion", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "UserData", "dynamic", "", ""));
        return _v_;
    }
}
