// auto-generated @formatter:off
package Zeze.Builtin.Threading;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BMutex extends Zeze.Transaction.Bean implements BMutexReadOnly {
    public static final long TYPEID = 7224301299276482451L;

    private boolean _Locked;
    private int _ServerId;
    private long _LockTime;

    @Override
    public boolean isLocked() {
        if (!isManaged())
            return _Locked;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Locked;
        var log = (Log__Locked)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Locked;
    }

    public void setLocked(boolean value) {
        if (!isManaged()) {
            _Locked = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Locked(this, 1, value));
    }

    @Override
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServerId;
        var log = (Log__ServerId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int value) {
        if (!isManaged()) {
            _ServerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServerId(this, 2, value));
    }

    @Override
    public long getLockTime() {
        if (!isManaged())
            return _LockTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LockTime;
        var log = (Log__LockTime)txn.getLog(objectId() + 3);
        return log != null ? log.value : _LockTime;
    }

    public void setLockTime(long value) {
        if (!isManaged()) {
            _LockTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LockTime(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BMutex() {
    }

    @SuppressWarnings("deprecation")
    public BMutex(boolean _Locked_, int _ServerId_, long _LockTime_) {
        _Locked = _Locked_;
        _ServerId = _ServerId_;
        _LockTime = _LockTime_;
    }

    public void assign(BMutex other) {
        setLocked(other.isLocked());
        setServerId(other.getServerId());
        setLockTime(other.getLockTime());
    }

    public BMutex copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BMutex copy() {
        var copy = new BMutex();
        copy.assign(this);
        return copy;
    }

    public static void swap(BMutex a, BMutex b) {
        BMutex save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Locked extends Zeze.Transaction.Logs.LogBool {
        public Log__Locked(BMutex bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BMutex)getBelong())._Locked = value; }
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BMutex bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BMutex)getBelong())._ServerId = value; }
    }

    private static final class Log__LockTime extends Zeze.Transaction.Logs.LogLong {
        public Log__LockTime(BMutex bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BMutex)getBelong())._LockTime = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Threading.BMutex: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Locked=").append(isLocked()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId=").append(getServerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LockTime=").append(getLockTime()).append(System.lineSeparator());
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
            boolean _x_ = isLocked();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getLockTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setLocked(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setLockTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getServerId() < 0)
            return true;
        if (getLockTime() < 0)
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
                case 1: _Locked = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
                case 2: _ServerId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _LockTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setLocked(rs.getBoolean(_parents_name_ + "Locked"));
        setServerId(rs.getInt(_parents_name_ + "ServerId"));
        setLockTime(rs.getLong(_parents_name_ + "LockTime"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBoolean(_parents_name_ + "Locked", isLocked());
        st.appendInt(_parents_name_ + "ServerId", getServerId());
        st.appendLong(_parents_name_ + "LockTime", getLockTime());
    }
}
