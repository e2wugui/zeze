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

    public static long getSpecialTypeIdFromBean_5(Zeze.Transaction.Bean bean) {
        return Zeze.Game.Online.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_5(long typeId) {
        return Zeze.Game.Online.createBeanFromSpecialTypeId(typeId);
    }

    @Override
    public String getAccount() {
        if (!isManaged())
            return _Account;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Account;
        var log = (Log__Account)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Account;
    }

    public void setAccount(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Account = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Account(this, 1, value));
    }

    @Override
    public Zeze.Builtin.Game.Online.BLink getLink() {
        if (!isManaged())
            return _Link;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Link;
        var log = (Log__Link)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Link;
    }

    public void setLink(Zeze.Builtin.Game.Online.BLink value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Link = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Link(this, 2, value));
    }

    @Override
    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LoginVersion;
        var log = (Log__LoginVersion)txn.getLog(objectId() + 3);
        return log != null ? log.value : _LoginVersion;
    }

    public void setLoginVersion(long value) {
        if (!isManaged()) {
            _LoginVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LoginVersion(this, 3, value));
    }

    @Override
    public long getLogoutVersion() {
        if (!isManaged())
            return _LogoutVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LogoutVersion;
        var log = (Log__LogoutVersion)txn.getLog(objectId() + 4);
        return log != null ? log.value : _LogoutVersion;
    }

    public void setLogoutVersion(long value) {
        if (!isManaged()) {
            _LogoutVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LogoutVersion(this, 4, value));
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

    public void assign(BOnlineShared other) {
        setAccount(other.getAccount());
        setLink(other.getLink());
        setLoginVersion(other.getLoginVersion());
        setLogoutVersion(other.getLogoutVersion());
        _UserData.assign(other._UserData);
        _unknown_ = other._unknown_;
    }

    public BOnlineShared copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOnlineShared copy() {
        var copy = new BOnlineShared();
        copy.assign(this);
        return copy;
    }

    public static void swap(BOnlineShared a, BOnlineShared b) {
        BOnlineShared save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Account extends Zeze.Transaction.Logs.LogString {
        public Log__Account(BOnlineShared bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOnlineShared)getBelong())._Account = value; }
    }

    private static final class Log__Link extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Game.Online.BLink> {
        public Log__Link(BOnlineShared bean, int varId, Zeze.Builtin.Game.Online.BLink value) { super(Zeze.Builtin.Game.Online.BLink.class, bean, varId, value); }

        @Override
        public void commit() { ((BOnlineShared)getBelong())._Link = value; }
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LoginVersion(BOnlineShared bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOnlineShared)getBelong())._LoginVersion = value; }
    }

    private static final class Log__LogoutVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LogoutVersion(BOnlineShared bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOnlineShared)getBelong())._LogoutVersion = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Online.BOnlineShared: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Account=").append(getAccount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Link=").append(System.lineSeparator());
        getLink().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LoginVersion=").append(getLoginVersion()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LogoutVersion=").append(getLogoutVersion()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("UserData=").append(System.lineSeparator());
        _UserData.getBean().buildString(sb, level + 4);
        sb.append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _UserData.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _UserData.initRootInfoWithRedo(root, this);
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _Account = vlog.stringValue(); break;
                case 2: _Link = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Game.Online.BLink>)vlog).value; break;
                case 3: _LoginVersion = vlog.longValue(); break;
                case 4: _LogoutVersion = vlog.longValue(); break;
                case 5: _UserData.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setAccount(rs.getString(_parents_name_ + "Account"));
        if (getAccount() == null)
            setAccount("");
        parents.add("Link");
        getLink().decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        setLoginVersion(rs.getLong(_parents_name_ + "LoginVersion"));
        setLogoutVersion(rs.getLong(_parents_name_ + "LogoutVersion"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_UserData, rs.getString(_parents_name_ + "UserData"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Account", getAccount());
        parents.add("Link");
        getLink().encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
        st.appendLong(_parents_name_ + "LoginVersion", getLoginVersion());
        st.appendLong(_parents_name_ + "LogoutVersion", getLogoutVersion());
        st.appendString(_parents_name_ + "UserData", Zeze.Serialize.Helper.encodeJson(_UserData));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Account", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Link", "Zeze.Builtin.Game.Online.BLink", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "LoginVersion", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "LogoutVersion", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "UserData", "dynamic", "", ""));
        return vars;
    }
}
