// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BClearJobState extends Zeze.Transaction.Bean implements BClearJobStateReadOnly {
    public static final long TYPEID = 8599835992466746563L;

    private long _HeadNodeId;
    private long _TailNodeId;
    private String _LinkedMapName;

    @Override
    public long getHeadNodeId() {
        if (!isManaged())
            return _HeadNodeId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HeadNodeId;
        var log = (Log__HeadNodeId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _HeadNodeId;
    }

    public void setHeadNodeId(long value) {
        if (!isManaged()) {
            _HeadNodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__HeadNodeId(this, 1, value));
    }

    @Override
    public long getTailNodeId() {
        if (!isManaged())
            return _TailNodeId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TailNodeId;
        var log = (Log__TailNodeId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _TailNodeId;
    }

    public void setTailNodeId(long value) {
        if (!isManaged()) {
            _TailNodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TailNodeId(this, 2, value));
    }

    @Override
    public String getLinkedMapName() {
        if (!isManaged())
            return _LinkedMapName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LinkedMapName;
        var log = (Log__LinkedMapName)txn.getLog(objectId() + 3);
        return log != null ? log.value : _LinkedMapName;
    }

    public void setLinkedMapName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LinkedMapName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LinkedMapName(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BClearJobState() {
        _LinkedMapName = "";
    }

    @SuppressWarnings("deprecation")
    public BClearJobState(long _HeadNodeId_, long _TailNodeId_, String _LinkedMapName_) {
        _HeadNodeId = _HeadNodeId_;
        _TailNodeId = _TailNodeId_;
        if (_LinkedMapName_ == null)
            _LinkedMapName_ = "";
        _LinkedMapName = _LinkedMapName_;
    }

    @Override
    public void reset() {
        setHeadNodeId(0);
        setTailNodeId(0);
        setLinkedMapName("");
        _unknown_ = null;
    }

    public void assign(BClearJobState other) {
        setHeadNodeId(other.getHeadNodeId());
        setTailNodeId(other.getTailNodeId());
        setLinkedMapName(other.getLinkedMapName());
        _unknown_ = other._unknown_;
    }

    public BClearJobState copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BClearJobState copy() {
        var copy = new BClearJobState();
        copy.assign(this);
        return copy;
    }

    public static void swap(BClearJobState a, BClearJobState b) {
        BClearJobState save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__HeadNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__HeadNodeId(BClearJobState bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BClearJobState)getBelong())._HeadNodeId = value; }
    }

    private static final class Log__TailNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__TailNodeId(BClearJobState bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BClearJobState)getBelong())._TailNodeId = value; }
    }

    private static final class Log__LinkedMapName extends Zeze.Transaction.Logs.LogString {
        public Log__LinkedMapName(BClearJobState bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BClearJobState)getBelong())._LinkedMapName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.LinkedMap.BClearJobState: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("HeadNodeId=").append(getHeadNodeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TailNodeId=").append(getTailNodeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LinkedMapName=").append(getLinkedMapName()).append(System.lineSeparator());
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
            long _x_ = getHeadNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getTailNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getLinkedMapName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            setHeadNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTailNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setLinkedMapName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getHeadNodeId() < 0)
            return true;
        if (getTailNodeId() < 0)
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
                case 1: _HeadNodeId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _TailNodeId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _LinkedMapName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setHeadNodeId(rs.getLong(_parents_name_ + "HeadNodeId"));
        setTailNodeId(rs.getLong(_parents_name_ + "TailNodeId"));
        setLinkedMapName(rs.getString(_parents_name_ + "LinkedMapName"));
        if (getLinkedMapName() == null)
            setLinkedMapName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "HeadNodeId", getHeadNodeId());
        st.appendLong(_parents_name_ + "TailNodeId", getTailNodeId());
        st.appendString(_parents_name_ + "LinkedMapName", getLinkedMapName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "HeadNodeId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "TailNodeId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "LinkedMapName", "string", "", ""));
        return vars;
    }
}
