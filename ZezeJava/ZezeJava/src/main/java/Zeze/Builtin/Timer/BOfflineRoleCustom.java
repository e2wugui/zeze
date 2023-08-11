// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BOfflineRoleCustom extends Zeze.Transaction.Bean implements BOfflineRoleCustomReadOnly {
    public static final long TYPEID = -124522910617189691L;

    private String _TimerName;
    private long _RoleId;
    private long _LoginVersion;
    private String _HandleName;
    private final Zeze.Transaction.DynamicBean _CustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_CustomData() {
        return new Zeze.Transaction.DynamicBean(5, Zeze.Component.Timer::getSpecialTypeIdFromBean, Zeze.Component.Timer::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_5(Zeze.Transaction.Bean bean) {
        return Zeze.Component.Timer.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_5(long typeId) {
        return Zeze.Component.Timer.createBeanFromSpecialTypeId(typeId);
    }

    private String _OnlineSetName;

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
    public long getRoleId() {
        if (!isManaged())
            return _RoleId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _RoleId;
        var log = (Log__RoleId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _RoleId;
    }

    public void setRoleId(long value) {
        if (!isManaged()) {
            _RoleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__RoleId(this, 2, value));
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
    public String getHandleName() {
        if (!isManaged())
            return _HandleName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HandleName;
        var log = (Log__HandleName)txn.getLog(objectId() + 4);
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
        txn.putLog(new Log__HandleName(this, 4, value));
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
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _OnlineSetName;
        var log = (Log__OnlineSetName)txn.getLog(objectId() + 6);
        return log != null ? log.value : _OnlineSetName;
    }

    public void setOnlineSetName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _OnlineSetName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__OnlineSetName(this, 6, value));
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

    public void assign(BOfflineRoleCustom other) {
        setTimerName(other.getTimerName());
        setRoleId(other.getRoleId());
        setLoginVersion(other.getLoginVersion());
        setHandleName(other.getHandleName());
        _CustomData.assign(other._CustomData);
        setOnlineSetName(other.getOnlineSetName());
        _unknown_ = other._unknown_;
    }

    public BOfflineRoleCustom copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOfflineRoleCustom copy() {
        var copy = new BOfflineRoleCustom();
        copy.assign(this);
        return copy;
    }

    public static void swap(BOfflineRoleCustom a, BOfflineRoleCustom b) {
        BOfflineRoleCustom save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TimerName extends Zeze.Transaction.Logs.LogString {
        public Log__TimerName(BOfflineRoleCustom bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineRoleCustom)getBelong())._TimerName = value; }
    }

    private static final class Log__RoleId extends Zeze.Transaction.Logs.LogLong {
        public Log__RoleId(BOfflineRoleCustom bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineRoleCustom)getBelong())._RoleId = value; }
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LoginVersion(BOfflineRoleCustom bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineRoleCustom)getBelong())._LoginVersion = value; }
    }

    private static final class Log__HandleName extends Zeze.Transaction.Logs.LogString {
        public Log__HandleName(BOfflineRoleCustom bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineRoleCustom)getBelong())._HandleName = value; }
    }

    private static final class Log__OnlineSetName extends Zeze.Transaction.Logs.LogString {
        public Log__OnlineSetName(BOfflineRoleCustom bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineRoleCustom)getBelong())._OnlineSetName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BOfflineRoleCustom: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TimerName=").append(getTimerName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RoleId=").append(getRoleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LoginVersion=").append(getLoginVersion()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HandleName=").append(getHandleName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CustomData=").append(System.lineSeparator());
        _CustomData.getBean().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("OnlineSetName=").append(getOnlineSetName()).append(System.lineSeparator());
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
    public void decode(ByteBuffer _o_) {
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _CustomData.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _CustomData.initRootInfoWithRedo(root, this);
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _TimerName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _RoleId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _LoginVersion = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _HandleName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 5: _CustomData.followerApply(vlog); break;
                case 6: _OnlineSetName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTimerName(rs.getString(_parents_name_ + "TimerName"));
        if (getTimerName() == null)
            setTimerName("");
        setRoleId(rs.getLong(_parents_name_ + "RoleId"));
        setLoginVersion(rs.getLong(_parents_name_ + "LoginVersion"));
        setHandleName(rs.getString(_parents_name_ + "HandleName"));
        if (getHandleName() == null)
            setHandleName("");
        Zeze.Serialize.Helper.decodeJsonDynamic(_CustomData, rs.getString(_parents_name_ + "CustomData"));
        setOnlineSetName(rs.getString(_parents_name_ + "OnlineSetName"));
        if (getOnlineSetName() == null)
            setOnlineSetName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "TimerName", getTimerName());
        st.appendLong(_parents_name_ + "RoleId", getRoleId());
        st.appendLong(_parents_name_ + "LoginVersion", getLoginVersion());
        st.appendString(_parents_name_ + "HandleName", getHandleName());
        st.appendString(_parents_name_ + "CustomData", Zeze.Serialize.Helper.encodeJson(_CustomData));
        st.appendString(_parents_name_ + "OnlineSetName", getOnlineSetName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TimerName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "RoleId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "LoginVersion", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "HandleName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "CustomData", "dynamic", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "OnlineSetName", "string", "", ""));
        return vars;
    }
}
