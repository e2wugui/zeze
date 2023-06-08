// auto-generated @formatter:off
package Zeze.Builtin.Threading;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BSemaphore extends Zeze.Transaction.Bean implements BSemaphoreReadOnly {
    public static final long TYPEID = -308167851404701538L;

    private int _Permits;
    private int _InitialPermits;

    @Override
    public int getPermits() {
        if (!isManaged())
            return _Permits;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Permits;
        var log = (Log__Permits)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Permits;
    }

    public void setPermits(int value) {
        if (!isManaged()) {
            _Permits = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Permits(this, 1, value));
    }

    @Override
    public int getInitialPermits() {
        if (!isManaged())
            return _InitialPermits;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _InitialPermits;
        var log = (Log__InitialPermits)txn.getLog(objectId() + 2);
        return log != null ? log.value : _InitialPermits;
    }

    public void setInitialPermits(int value) {
        if (!isManaged()) {
            _InitialPermits = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__InitialPermits(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BSemaphore() {
    }

    @SuppressWarnings("deprecation")
    public BSemaphore(int _Permits_, int _InitialPermits_) {
        _Permits = _Permits_;
        _InitialPermits = _InitialPermits_;
    }

    public void assign(BSemaphore other) {
        setPermits(other.getPermits());
        setInitialPermits(other.getInitialPermits());
    }

    public BSemaphore copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSemaphore copy() {
        var copy = new BSemaphore();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSemaphore a, BSemaphore b) {
        BSemaphore save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Permits extends Zeze.Transaction.Logs.LogInt {
        public Log__Permits(BSemaphore bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSemaphore)getBelong())._Permits = value; }
    }

    private static final class Log__InitialPermits extends Zeze.Transaction.Logs.LogInt {
        public Log__InitialPermits(BSemaphore bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSemaphore)getBelong())._InitialPermits = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Threading.BSemaphore: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Permits=").append(getPermits()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("InitialPermits=").append(getInitialPermits()).append(System.lineSeparator());
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            int _x_ = getPermits();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getInitialPermits();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setPermits(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setInitialPermits(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getPermits() < 0)
            return true;
        if (getInitialPermits() < 0)
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
                case 1: _Permits = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _InitialPermits = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setPermits(rs.getInt(_parents_name_ + "Permits"));
        setInitialPermits(rs.getInt(_parents_name_ + "InitialPermits"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "Permits", getPermits());
        st.appendInt(_parents_name_ + "InitialPermits", getInitialPermits());
    }
}
