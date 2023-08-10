// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BLogin extends Zeze.Transaction.Bean implements BLoginReadOnly {
    public static final long TYPEID = -3414862908883999968L;

    private String _SessionName;

    @Override
    public String getSessionName() {
        if (!isManaged())
            return _SessionName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SessionName;
        var log = (Log__SessionName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _SessionName;
    }

    public void setSessionName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _SessionName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__SessionName(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BLogin() {
        _SessionName = "";
    }

    @SuppressWarnings("deprecation")
    public BLogin(String _SessionName_) {
        if (_SessionName_ == null)
            _SessionName_ = "";
        _SessionName = _SessionName_;
    }

    @Override
    public void reset() {
        setSessionName("");
        _unknown_ = null;
    }

    public void assign(BLogin other) {
        setSessionName(other.getSessionName());
        _unknown_ = other._unknown_;
    }

    public BLogin copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLogin copy() {
        var copy = new BLogin();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLogin a, BLogin b) {
        BLogin save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__SessionName extends Zeze.Transaction.Logs.LogString {
        public Log__SessionName(BLogin bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLogin)getBelong())._SessionName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ServiceManagerWithRaft.BLogin: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("SessionName=").append(getSessionName()).append(System.lineSeparator());
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
            String _x_ = getSessionName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            setSessionName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
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
                case 1: _SessionName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setSessionName(rs.getString(_parents_name_ + "SessionName"));
        if (getSessionName() == null)
            setSessionName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "SessionName", getSessionName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "SessionName", "string", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BLogin
    }
}
