// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BBroadcastTaskEvent extends Zeze.Transaction.Bean implements BBroadcastTaskEventReadOnly {
    public static final long TYPEID = 2627115510834301728L;

    private boolean _isBreakIfAccepted;

    @Override
    public boolean isIsBreakIfAccepted() {
        if (!isManaged())
            return _isBreakIfAccepted;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _isBreakIfAccepted;
        var log = (Log__isBreakIfAccepted)txn.getLog(objectId() + 1);
        return log != null ? log.value : _isBreakIfAccepted;
    }

    public void setIsBreakIfAccepted(boolean value) {
        if (!isManaged()) {
            _isBreakIfAccepted = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__isBreakIfAccepted(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BBroadcastTaskEvent() {
    }

    @SuppressWarnings("deprecation")
    public BBroadcastTaskEvent(boolean _isBreakIfAccepted_) {
        _isBreakIfAccepted = _isBreakIfAccepted_;
    }

    @Override
    public void reset() {
        setIsBreakIfAccepted(false);
        _unknown_ = null;
    }

    public void assign(BBroadcastTaskEvent other) {
        setIsBreakIfAccepted(other.isIsBreakIfAccepted());
        _unknown_ = other._unknown_;
    }

    public BBroadcastTaskEvent copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBroadcastTaskEvent copy() {
        var copy = new BBroadcastTaskEvent();
        copy.assign(this);
        return copy;
    }

    public static void swap(BBroadcastTaskEvent a, BBroadcastTaskEvent b) {
        BBroadcastTaskEvent save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__isBreakIfAccepted extends Zeze.Transaction.Logs.LogBool {
        public Log__isBreakIfAccepted(BBroadcastTaskEvent bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBroadcastTaskEvent)getBelong())._isBreakIfAccepted = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("isBreakIfAccepted=").append(isIsBreakIfAccepted()).append(System.lineSeparator());
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
            boolean _x_ = isIsBreakIfAccepted();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            setIsBreakIfAccepted(_o_.ReadBool(_t_));
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
                case 1: _isBreakIfAccepted = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setIsBreakIfAccepted(rs.getBoolean(_parents_name_ + "isBreakIfAccepted"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBoolean(_parents_name_ + "isBreakIfAccepted", isIsBreakIfAccepted());
    }

    @Override
    public java.util.List<Zeze.Transaction.Bean.Variable> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Transaction.Bean.Variable(1, "isBreakIfAccepted", "bool", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BBroadcastTaskEvent
    }
}
