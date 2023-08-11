// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

// Offline Timer
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BOfflineAccountCustom extends Zeze.Transaction.Bean implements BOfflineAccountCustomReadOnly {
    public static final long TYPEID = -8019295337231502138L;

    private String _TimerName;
    private String _Account;
    private String _ClientId;
    private long _LoginVersion;
    private String _HandleName;
    private final Zeze.Transaction.DynamicBean _CustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_CustomData() {
        return new Zeze.Transaction.DynamicBean(6, Zeze.Component.Timer::getSpecialTypeIdFromBean, Zeze.Component.Timer::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_6(Zeze.Transaction.Bean bean) {
        return Zeze.Component.Timer.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_6(long typeId) {
        return Zeze.Component.Timer.createBeanFromSpecialTypeId(typeId);
    }

    @Override
    public String getTimerName() {
        if (!isManaged())
            return _TimerName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TimerName;
        var log = (Log__TimerName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TimerName;
    }

    public void setTimerName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TimerName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TimerName(this, 1, value));
    }

    @Override
    public String getAccount() {
        if (!isManaged())
            return _Account;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Account;
        var log = (Log__Account)txn.getLog(objectId() + 2);
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
        txn.putLog(new Log__Account(this, 2, value));
    }

    @Override
    public String getClientId() {
        if (!isManaged())
            return _ClientId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ClientId;
        var log = (Log__ClientId)txn.getLog(objectId() + 3);
        return log != null ? log.value : _ClientId;
    }

    public void setClientId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ClientId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ClientId(this, 3, value));
    }

    @Override
    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LoginVersion;
        var log = (Log__LoginVersion)txn.getLog(objectId() + 4);
        return log != null ? log.value : _LoginVersion;
    }

    public void setLoginVersion(long value) {
        if (!isManaged()) {
            _LoginVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LoginVersion(this, 4, value));
    }

    @Override
    public String getHandleName() {
        if (!isManaged())
            return _HandleName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HandleName;
        var log = (Log__HandleName)txn.getLog(objectId() + 5);
        return log != null ? log.value : _HandleName;
    }

    public void setHandleName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _HandleName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__HandleName(this, 5, value));
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

    public void assign(BOfflineAccountCustom other) {
        setTimerName(other.getTimerName());
        setAccount(other.getAccount());
        setClientId(other.getClientId());
        setLoginVersion(other.getLoginVersion());
        setHandleName(other.getHandleName());
        _CustomData.assign(other._CustomData);
        _unknown_ = other._unknown_;
    }

    public BOfflineAccountCustom copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOfflineAccountCustom copy() {
        var copy = new BOfflineAccountCustom();
        copy.assign(this);
        return copy;
    }

    public static void swap(BOfflineAccountCustom a, BOfflineAccountCustom b) {
        BOfflineAccountCustom save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TimerName extends Zeze.Transaction.Logs.LogString {
        public Log__TimerName(BOfflineAccountCustom bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineAccountCustom)getBelong())._TimerName = value; }
    }

    private static final class Log__Account extends Zeze.Transaction.Logs.LogString {
        public Log__Account(BOfflineAccountCustom bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineAccountCustom)getBelong())._Account = value; }
    }

    private static final class Log__ClientId extends Zeze.Transaction.Logs.LogString {
        public Log__ClientId(BOfflineAccountCustom bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineAccountCustom)getBelong())._ClientId = value; }
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LoginVersion(BOfflineAccountCustom bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineAccountCustom)getBelong())._LoginVersion = value; }
    }

    private static final class Log__HandleName extends Zeze.Transaction.Logs.LogString {
        public Log__HandleName(BOfflineAccountCustom bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineAccountCustom)getBelong())._HandleName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BOfflineAccountCustom: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TimerName=").append(getTimerName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Account=").append(getAccount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ClientId=").append(getClientId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LoginVersion=").append(getLoginVersion()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HandleName=").append(getHandleName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CustomData=").append(System.lineSeparator());
        _CustomData.getBean().buildString(sb, level + 4);
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
    public void decode(ByteBuffer _o_) {
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _CustomData.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _CustomData.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getLoginVersion() < 0)
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
                case 1: _TimerName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _Account = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _ClientId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _LoginVersion = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 5: _HandleName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 6: _CustomData.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTimerName(rs.getString(_parents_name_ + "TimerName"));
        if (getTimerName() == null)
            setTimerName("");
        setAccount(rs.getString(_parents_name_ + "Account"));
        if (getAccount() == null)
            setAccount("");
        setClientId(rs.getString(_parents_name_ + "ClientId"));
        if (getClientId() == null)
            setClientId("");
        setLoginVersion(rs.getLong(_parents_name_ + "LoginVersion"));
        setHandleName(rs.getString(_parents_name_ + "HandleName"));
        if (getHandleName() == null)
            setHandleName("");
        Zeze.Serialize.Helper.decodeJsonDynamic(_CustomData, rs.getString(_parents_name_ + "CustomData"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "TimerName", getTimerName());
        st.appendString(_parents_name_ + "Account", getAccount());
        st.appendString(_parents_name_ + "ClientId", getClientId());
        st.appendLong(_parents_name_ + "LoginVersion", getLoginVersion());
        st.appendString(_parents_name_ + "HandleName", getHandleName());
        st.appendString(_parents_name_ + "CustomData", Zeze.Serialize.Helper.encodeJson(_CustomData));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TimerName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Account", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ClientId", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "LoginVersion", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "HandleName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "CustomData", "dynamic", "", ""));
        return vars;
    }
}
