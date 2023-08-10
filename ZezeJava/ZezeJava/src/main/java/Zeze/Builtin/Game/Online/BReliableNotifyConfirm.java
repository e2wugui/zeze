// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BReliableNotifyConfirm extends Zeze.Transaction.Bean implements BReliableNotifyConfirmReadOnly {
    public static final long TYPEID = -6588057877320371892L;

    private long _ReliableNotifyConfirmIndex;
    private boolean _Sync;

    @Override
    public long getReliableNotifyConfirmIndex() {
        if (!isManaged())
            return _ReliableNotifyConfirmIndex;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ReliableNotifyConfirmIndex;
        var log = (Log__ReliableNotifyConfirmIndex)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ReliableNotifyConfirmIndex;
    }

    public void setReliableNotifyConfirmIndex(long value) {
        if (!isManaged()) {
            _ReliableNotifyConfirmIndex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ReliableNotifyConfirmIndex(this, 1, value));
    }

    @Override
    public boolean isSync() {
        if (!isManaged())
            return _Sync;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Sync;
        var log = (Log__Sync)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Sync;
    }

    public void setSync(boolean value) {
        if (!isManaged()) {
            _Sync = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Sync(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BReliableNotifyConfirm() {
    }

    @SuppressWarnings("deprecation")
    public BReliableNotifyConfirm(long _ReliableNotifyConfirmIndex_, boolean _Sync_) {
        _ReliableNotifyConfirmIndex = _ReliableNotifyConfirmIndex_;
        _Sync = _Sync_;
    }

    @Override
    public void reset() {
        setReliableNotifyConfirmIndex(0);
        setSync(false);
        _unknown_ = null;
    }

    public void assign(BReliableNotifyConfirm other) {
        setReliableNotifyConfirmIndex(other.getReliableNotifyConfirmIndex());
        setSync(other.isSync());
        _unknown_ = other._unknown_;
    }

    public BReliableNotifyConfirm copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BReliableNotifyConfirm copy() {
        var copy = new BReliableNotifyConfirm();
        copy.assign(this);
        return copy;
    }

    public static void swap(BReliableNotifyConfirm a, BReliableNotifyConfirm b) {
        BReliableNotifyConfirm save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ReliableNotifyConfirmIndex extends Zeze.Transaction.Logs.LogLong {
        public Log__ReliableNotifyConfirmIndex(BReliableNotifyConfirm bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReliableNotifyConfirm)getBelong())._ReliableNotifyConfirmIndex = value; }
    }

    private static final class Log__Sync extends Zeze.Transaction.Logs.LogBool {
        public Log__Sync(BReliableNotifyConfirm bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReliableNotifyConfirm)getBelong())._Sync = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Online.BReliableNotifyConfirm: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyConfirmIndex=").append(getReliableNotifyConfirmIndex()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Sync=").append(isSync()).append(System.lineSeparator());
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
            long _x_ = getReliableNotifyConfirmIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            boolean _x_ = isSync();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setReliableNotifyConfirmIndex(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setSync(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getReliableNotifyConfirmIndex() < 0)
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
                case 1: _ReliableNotifyConfirmIndex = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _Sync = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setReliableNotifyConfirmIndex(rs.getLong(_parents_name_ + "ReliableNotifyConfirmIndex"));
        setSync(rs.getBoolean(_parents_name_ + "Sync"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "ReliableNotifyConfirmIndex", getReliableNotifyConfirmIndex());
        st.appendBoolean(_parents_name_ + "Sync", isSync());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ReliableNotifyConfirmIndex", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Sync", "bool", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BReliableNotifyConfirm
    }
}
