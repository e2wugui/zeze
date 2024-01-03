// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BCondition extends Zeze.Transaction.Bean implements BConditionReadOnly {
    public static final long TYPEID = -6038954901559182436L;

    private String _ClassName;
    private Zeze.Net.Binary _Parameter;

    @Override
    public String getClassName() {
        if (!isManaged())
            return _ClassName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ClassName;
        var log = (Log__ClassName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ClassName;
    }

    public void setClassName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ClassName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ClassName(this, 1, value));
    }

    @Override
    public Zeze.Net.Binary getParameter() {
        if (!isManaged())
            return _Parameter;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Parameter;
        var log = (Log__Parameter)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Parameter;
    }

    public void setParameter(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Parameter = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Parameter(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BCondition() {
        _ClassName = "";
        _Parameter = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BCondition(String _ClassName_, Zeze.Net.Binary _Parameter_) {
        if (_ClassName_ == null)
            _ClassName_ = "";
        _ClassName = _ClassName_;
        if (_Parameter_ == null)
            _Parameter_ = Zeze.Net.Binary.Empty;
        _Parameter = _Parameter_;
    }

    @Override
    public void reset() {
        setClassName("");
        setParameter(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    public void assign(BCondition other) {
        setClassName(other.getClassName());
        setParameter(other.getParameter());
        _unknown_ = other._unknown_;
    }

    public BCondition copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCondition copy() {
        var copy = new BCondition();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCondition a, BCondition b) {
        BCondition save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ClassName extends Zeze.Transaction.Logs.LogString {
        public Log__ClassName(BCondition bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCondition)getBelong())._ClassName = value; }
    }

    private static final class Log__Parameter extends Zeze.Transaction.Logs.LogBinary {
        public Log__Parameter(BCondition bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCondition)getBelong())._Parameter = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskModule.BCondition: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ClassName=").append(getClassName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Parameter=").append(getParameter()).append(System.lineSeparator());
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
            String _x_ = getClassName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getParameter();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
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
            setClassName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setParameter(_o_.ReadBinary(_t_));
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
                case 1: _ClassName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _Parameter = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setClassName(rs.getString(_parents_name_ + "ClassName"));
        if (getClassName() == null)
            setClassName("");
        setParameter(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Parameter")));
        if (getParameter() == null)
            setParameter(Zeze.Net.Binary.Empty);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "ClassName", getClassName());
        st.appendBinary(_parents_name_ + "Parameter", getParameter());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ClassName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Parameter", "binary", "", ""));
        return vars;
    }
}
