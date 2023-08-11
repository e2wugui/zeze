// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

// 保存在内存Map中
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BArchOnlineTimer extends Zeze.Transaction.Bean implements BArchOnlineTimerReadOnly {
    public static final long TYPEID = -1410268970794351805L;

    private String _Account;
    private String _ClientId;
    private final Zeze.Transaction.DynamicBean _TimerObj;
    public static final long DynamicTypeId_TimerObj_Zeze_Builtin_Timer_BCronTimer = -6995089347718168392L;
    public static final long DynamicTypeId_TimerObj_Zeze_Builtin_Timer_BSimpleTimer = 1832177636612857692L;

    public static Zeze.Transaction.DynamicBean newDynamicBean_TimerObj() {
        return new Zeze.Transaction.DynamicBean(3, BArchOnlineTimer::getSpecialTypeIdFromBean_3, BArchOnlineTimer::createBeanFromSpecialTypeId_3);
    }

    public static long getSpecialTypeIdFromBean_3(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.typeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_typeId_ == -6995089347718168392L)
            return -6995089347718168392L; // Zeze.Builtin.Timer.BCronTimer
        if (_typeId_ == 1832177636612857692L)
            return 1832177636612857692L; // Zeze.Builtin.Timer.BSimpleTimer
        throw new UnsupportedOperationException("Unknown Bean! dynamic@Zeze.Builtin.Timer.BArchOnlineTimer:TimerObj");
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_3(long typeId) {
        if (typeId == -6995089347718168392L)
            return new Zeze.Builtin.Timer.BCronTimer();
        if (typeId == 1832177636612857692L)
            return new Zeze.Builtin.Timer.BSimpleTimer();
        if (typeId == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private long _LoginVersion;
    private long _SerialId;

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
    public String getClientId() {
        if (!isManaged())
            return _ClientId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ClientId;
        var log = (Log__ClientId)txn.getLog(objectId() + 2);
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
        txn.putLog(new Log__ClientId(this, 2, value));
    }

    public Zeze.Transaction.DynamicBean getTimerObj() {
        return _TimerObj;
    }

    public Zeze.Builtin.Timer.BCronTimer getTimerObj_Zeze_Builtin_Timer_BCronTimer() {
        return (Zeze.Builtin.Timer.BCronTimer)_TimerObj.getBean();
    }

    public void setTimerObj(Zeze.Builtin.Timer.BCronTimer value) {
        _TimerObj.setBean(value);
    }

    public Zeze.Builtin.Timer.BSimpleTimer getTimerObj_Zeze_Builtin_Timer_BSimpleTimer() {
        return (Zeze.Builtin.Timer.BSimpleTimer)_TimerObj.getBean();
    }

    public void setTimerObj(Zeze.Builtin.Timer.BSimpleTimer value) {
        _TimerObj.setBean(value);
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
    public long getSerialId() {
        if (!isManaged())
            return _SerialId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SerialId;
        var log = (Log__SerialId)txn.getLog(objectId() + 5);
        return log != null ? log.value : _SerialId;
    }

    public void setSerialId(long value) {
        if (!isManaged()) {
            _SerialId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__SerialId(this, 5, value));
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

    public void assign(BArchOnlineTimer other) {
        setAccount(other.getAccount());
        setClientId(other.getClientId());
        _TimerObj.assign(other._TimerObj);
        setLoginVersion(other.getLoginVersion());
        setSerialId(other.getSerialId());
        _unknown_ = other._unknown_;
    }

    public BArchOnlineTimer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BArchOnlineTimer copy() {
        var copy = new BArchOnlineTimer();
        copy.assign(this);
        return copy;
    }

    public static void swap(BArchOnlineTimer a, BArchOnlineTimer b) {
        BArchOnlineTimer save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Account extends Zeze.Transaction.Logs.LogString {
        public Log__Account(BArchOnlineTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BArchOnlineTimer)getBelong())._Account = value; }
    }

    private static final class Log__ClientId extends Zeze.Transaction.Logs.LogString {
        public Log__ClientId(BArchOnlineTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BArchOnlineTimer)getBelong())._ClientId = value; }
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LoginVersion(BArchOnlineTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BArchOnlineTimer)getBelong())._LoginVersion = value; }
    }

    private static final class Log__SerialId extends Zeze.Transaction.Logs.LogLong {
        public Log__SerialId(BArchOnlineTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BArchOnlineTimer)getBelong())._SerialId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BArchOnlineTimer: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Account=").append(getAccount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ClientId=").append(getClientId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TimerObj=").append(System.lineSeparator());
        _TimerObj.getBean().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LoginVersion=").append(getLoginVersion()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SerialId=").append(getSerialId()).append(System.lineSeparator());
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
    public void decode(ByteBuffer _o_) {
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _TimerObj.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _TimerObj.initRootInfoWithRedo(root, this);
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _Account = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _ClientId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _TimerObj.followerApply(vlog); break;
                case 4: _LoginVersion = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 5: _SerialId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setAccount(rs.getString(_parents_name_ + "Account"));
        if (getAccount() == null)
            setAccount("");
        setClientId(rs.getString(_parents_name_ + "ClientId"));
        if (getClientId() == null)
            setClientId("");
        Zeze.Serialize.Helper.decodeJsonDynamic(_TimerObj, rs.getString(_parents_name_ + "TimerObj"));
        setLoginVersion(rs.getLong(_parents_name_ + "LoginVersion"));
        setSerialId(rs.getLong(_parents_name_ + "SerialId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Account", getAccount());
        st.appendString(_parents_name_ + "ClientId", getClientId());
        st.appendString(_parents_name_ + "TimerObj", Zeze.Serialize.Helper.encodeJson(_TimerObj));
        st.appendLong(_parents_name_ + "LoginVersion", getLoginVersion());
        st.appendLong(_parents_name_ + "SerialId", getSerialId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Account", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ClientId", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "TimerObj", "dynamic", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "LoginVersion", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "SerialId", "long", "", ""));
        return vars;
    }
}
