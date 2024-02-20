// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BJob extends Zeze.Transaction.Bean implements BJobReadOnly {
    public static final long TYPEID = -489344497836886892L;

    private String _JobHandleName;
    private Zeze.Net.Binary _JobState;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    @Override
    public String getJobHandleName() {
        if (!isManaged())
            return _JobHandleName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _JobHandleName;
        var log = (Log__JobHandleName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _JobHandleName;
    }

    public void setJobHandleName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _JobHandleName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__JobHandleName(this, 1, value));
    }

    @Override
    public Zeze.Net.Binary getJobState() {
        if (!isManaged())
            return _JobState;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _JobState;
        var log = (Log__JobState)txn.getLog(objectId() + 2);
        return log != null ? log.value : _JobState;
    }

    public void setJobState(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _JobState = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__JobState(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BJob() {
        _JobHandleName = "";
        _JobState = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BJob(String _JobHandleName_, Zeze.Net.Binary _JobState_) {
        if (_JobHandleName_ == null)
            _JobHandleName_ = "";
        _JobHandleName = _JobHandleName_;
        if (_JobState_ == null)
            _JobState_ = Zeze.Net.Binary.Empty;
        _JobState = _JobState_;
    }

    @Override
    public void reset() {
        setJobHandleName("");
        setJobState(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    public void assign(BJob other) {
        setJobHandleName(other.getJobHandleName());
        setJobState(other.getJobState());
        _unknown_ = other._unknown_;
    }

    public BJob copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BJob copy() {
        var copy = new BJob();
        copy.assign(this);
        return copy;
    }

    public static void swap(BJob a, BJob b) {
        BJob save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__JobHandleName extends Zeze.Transaction.Logs.LogString {
        public Log__JobHandleName(BJob bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BJob)getBelong())._JobHandleName = value; }
    }

    private static final class Log__JobState extends Zeze.Transaction.Logs.LogBinary {
        public Log__JobState(BJob bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BJob)getBelong())._JobState = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.DelayRemove.BJob: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("JobHandleName=").append(getJobHandleName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("JobState=").append(getJobState()).append(System.lineSeparator());
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
            String _x_ = getJobHandleName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getJobState();
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
            setJobHandleName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setJobState(_o_.ReadBinary(_t_));
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
                case 1: _JobHandleName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _JobState = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setJobHandleName(rs.getString(_parents_name_ + "JobHandleName"));
        if (getJobHandleName() == null)
            setJobHandleName("");
        setJobState(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "JobState")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "JobHandleName", getJobHandleName());
        st.appendBinary(_parents_name_ + "JobState", getJobState());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "JobHandleName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "JobState", "binary", "", ""));
        return vars;
    }
}
