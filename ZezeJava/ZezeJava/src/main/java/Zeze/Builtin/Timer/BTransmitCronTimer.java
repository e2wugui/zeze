// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTransmitCronTimer extends Zeze.Transaction.Bean implements BTransmitCronTimerReadOnly {
    public static final long TYPEID = 2513103118161345176L;

    private String _TimerId;
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Timer.BCronTimer> _CronTimer;
    private String _HandleClass;
    private String _CustomClass;
    private Zeze.Net.Binary _CustomBean;
    private long _LoginVersion;
    private boolean _Hot;

    @Override
    public String getTimerId() {
        if (!isManaged())
            return _TimerId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TimerId;
        var log = (Log__TimerId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TimerId;
    }

    public void setTimerId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TimerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TimerId(this, 1, value));
    }

    public Zeze.Builtin.Timer.BCronTimer getCronTimer() {
        return _CronTimer.getValue();
    }

    public void setCronTimer(Zeze.Builtin.Timer.BCronTimer value) {
        _CronTimer.setValue(value);
    }

    @Override
    public Zeze.Builtin.Timer.BCronTimerReadOnly getCronTimerReadOnly() {
        return _CronTimer.getValue();
    }

    @Override
    public String getHandleClass() {
        if (!isManaged())
            return _HandleClass;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HandleClass;
        var log = (Log__HandleClass)txn.getLog(objectId() + 3);
        return log != null ? log.value : _HandleClass;
    }

    public void setHandleClass(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _HandleClass = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__HandleClass(this, 3, value));
    }

    @Override
    public String getCustomClass() {
        if (!isManaged())
            return _CustomClass;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _CustomClass;
        var log = (Log__CustomClass)txn.getLog(objectId() + 4);
        return log != null ? log.value : _CustomClass;
    }

    public void setCustomClass(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _CustomClass = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__CustomClass(this, 4, value));
    }

    @Override
    public Zeze.Net.Binary getCustomBean() {
        if (!isManaged())
            return _CustomBean;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _CustomBean;
        var log = (Log__CustomBean)txn.getLog(objectId() + 5);
        return log != null ? log.value : _CustomBean;
    }

    public void setCustomBean(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _CustomBean = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__CustomBean(this, 5, value));
    }

    @Override
    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LoginVersion;
        var log = (Log__LoginVersion)txn.getLog(objectId() + 6);
        return log != null ? log.value : _LoginVersion;
    }

    public void setLoginVersion(long value) {
        if (!isManaged()) {
            _LoginVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LoginVersion(this, 6, value));
    }

    @Override
    public boolean isHot() {
        if (!isManaged())
            return _Hot;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Hot;
        var log = (Log__Hot)txn.getLog(objectId() + 7);
        return log != null ? log.value : _Hot;
    }

    public void setHot(boolean value) {
        if (!isManaged()) {
            _Hot = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Hot(this, 7, value));
    }

    @SuppressWarnings("deprecation")
    public BTransmitCronTimer() {
        _TimerId = "";
        _CronTimer = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Timer.BCronTimer(), Zeze.Builtin.Timer.BCronTimer.class);
        _CronTimer.variableId(2);
        _HandleClass = "";
        _CustomClass = "";
        _CustomBean = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BTransmitCronTimer(String _TimerId_, String _HandleClass_, String _CustomClass_, Zeze.Net.Binary _CustomBean_, long _LoginVersion_, boolean _Hot_) {
        if (_TimerId_ == null)
            _TimerId_ = "";
        _TimerId = _TimerId_;
        _CronTimer = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Timer.BCronTimer(), Zeze.Builtin.Timer.BCronTimer.class);
        _CronTimer.variableId(2);
        if (_HandleClass_ == null)
            _HandleClass_ = "";
        _HandleClass = _HandleClass_;
        if (_CustomClass_ == null)
            _CustomClass_ = "";
        _CustomClass = _CustomClass_;
        if (_CustomBean_ == null)
            _CustomBean_ = Zeze.Net.Binary.Empty;
        _CustomBean = _CustomBean_;
        _LoginVersion = _LoginVersion_;
        _Hot = _Hot_;
    }

    @Override
    public void reset() {
        setTimerId("");
        _CronTimer.reset();
        setHandleClass("");
        setCustomClass("");
        setCustomBean(Zeze.Net.Binary.Empty);
        setLoginVersion(0);
        setHot(false);
        _unknown_ = null;
    }

    public void assign(BTransmitCronTimer other) {
        setTimerId(other.getTimerId());
        _CronTimer.assign(other._CronTimer);
        setHandleClass(other.getHandleClass());
        setCustomClass(other.getCustomClass());
        setCustomBean(other.getCustomBean());
        setLoginVersion(other.getLoginVersion());
        setHot(other.isHot());
        _unknown_ = other._unknown_;
    }

    public BTransmitCronTimer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTransmitCronTimer copy() {
        var copy = new BTransmitCronTimer();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTransmitCronTimer a, BTransmitCronTimer b) {
        BTransmitCronTimer save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TimerId extends Zeze.Transaction.Logs.LogString {
        public Log__TimerId(BTransmitCronTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmitCronTimer)getBelong())._TimerId = value; }
    }

    private static final class Log__HandleClass extends Zeze.Transaction.Logs.LogString {
        public Log__HandleClass(BTransmitCronTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmitCronTimer)getBelong())._HandleClass = value; }
    }

    private static final class Log__CustomClass extends Zeze.Transaction.Logs.LogString {
        public Log__CustomClass(BTransmitCronTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmitCronTimer)getBelong())._CustomClass = value; }
    }

    private static final class Log__CustomBean extends Zeze.Transaction.Logs.LogBinary {
        public Log__CustomBean(BTransmitCronTimer bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmitCronTimer)getBelong())._CustomBean = value; }
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LoginVersion(BTransmitCronTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmitCronTimer)getBelong())._LoginVersion = value; }
    }

    private static final class Log__Hot extends Zeze.Transaction.Logs.LogBool {
        public Log__Hot(BTransmitCronTimer bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmitCronTimer)getBelong())._Hot = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BTransmitCronTimer: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TimerId=").append(getTimerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CronTimer=").append(System.lineSeparator());
        _CronTimer.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HandleClass=").append(getHandleClass()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CustomClass=").append(getCustomClass()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CustomBean=").append(getCustomBean()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LoginVersion=").append(getLoginVersion()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Hot=").append(isHot()).append(System.lineSeparator());
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
            String _x_ = getTimerId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _CronTimer.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            String _x_ = getHandleClass();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getCustomClass();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getCustomBean();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = getLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            boolean _x_ = isHot();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            setTimerId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadBean(_CronTimer, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setHandleClass(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setCustomClass(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setCustomBean(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setHot(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _CronTimer.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _CronTimer.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_CronTimer.negativeCheck())
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
                case 1: _TimerId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _CronTimer.followerApply(vlog); break;
                case 3: _HandleClass = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _CustomClass = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 5: _CustomBean = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 6: _LoginVersion = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 7: _Hot = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTimerId(rs.getString(_parents_name_ + "TimerId"));
        if (getTimerId() == null)
            setTimerId("");
        parents.add("CronTimer");
        _CronTimer.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        setHandleClass(rs.getString(_parents_name_ + "HandleClass"));
        if (getHandleClass() == null)
            setHandleClass("");
        setCustomClass(rs.getString(_parents_name_ + "CustomClass"));
        if (getCustomClass() == null)
            setCustomClass("");
        setCustomBean(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "CustomBean")));
        setLoginVersion(rs.getLong(_parents_name_ + "LoginVersion"));
        setHot(rs.getBoolean(_parents_name_ + "Hot"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "TimerId", getTimerId());
        parents.add("CronTimer");
        _CronTimer.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
        st.appendString(_parents_name_ + "HandleClass", getHandleClass());
        st.appendString(_parents_name_ + "CustomClass", getCustomClass());
        st.appendBinary(_parents_name_ + "CustomBean", getCustomBean());
        st.appendLong(_parents_name_ + "LoginVersion", getLoginVersion());
        st.appendBoolean(_parents_name_ + "Hot", isHot());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TimerId", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "CronTimer", "Zeze.Builtin.Timer.BCronTimer", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "HandleClass", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "CustomClass", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "CustomBean", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "LoginVersion", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "Hot", "bool", "", ""));
        return vars;
    }
}
