// auto-generated @formatter:off
package Zeze.Builtin.Threading;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BReadWriteLock extends Zeze.Transaction.Bean implements BReadWriteLockReadOnly {
    public static final long TYPEID = 5310988726582781550L;

    private int _ReadingCount;
    private boolean _InWriting;

    @Override
    public int getReadingCount() {
        if (!isManaged())
            return _ReadingCount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ReadingCount;
        var log = (Log__ReadingCount)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ReadingCount;
    }

    public void setReadingCount(int value) {
        if (!isManaged()) {
            _ReadingCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ReadingCount(this, 1, value));
    }

    @Override
    public boolean isInWriting() {
        if (!isManaged())
            return _InWriting;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _InWriting;
        var log = (Log__InWriting)txn.getLog(objectId() + 2);
        return log != null ? log.value : _InWriting;
    }

    public void setInWriting(boolean value) {
        if (!isManaged()) {
            _InWriting = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__InWriting(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BReadWriteLock() {
    }

    @SuppressWarnings("deprecation")
    public BReadWriteLock(int _ReadingCount_, boolean _InWriting_) {
        _ReadingCount = _ReadingCount_;
        _InWriting = _InWriting_;
    }

    public void assign(BReadWriteLock other) {
        setReadingCount(other.getReadingCount());
        setInWriting(other.isInWriting());
    }

    public BReadWriteLock copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BReadWriteLock copy() {
        var copy = new BReadWriteLock();
        copy.assign(this);
        return copy;
    }

    public static void swap(BReadWriteLock a, BReadWriteLock b) {
        BReadWriteLock save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ReadingCount extends Zeze.Transaction.Logs.LogInt {
        public Log__ReadingCount(BReadWriteLock bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReadWriteLock)getBelong())._ReadingCount = value; }
    }

    private static final class Log__InWriting extends Zeze.Transaction.Logs.LogBool {
        public Log__InWriting(BReadWriteLock bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReadWriteLock)getBelong())._InWriting = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Threading.BReadWriteLock: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ReadingCount=").append(getReadingCount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("InWriting=").append(isInWriting()).append(System.lineSeparator());
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
            int _x_ = getReadingCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = isInWriting();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setReadingCount(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setInWriting(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getReadingCount() < 0)
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
                case 1: _ReadingCount = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _InWriting = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setReadingCount(rs.getInt(_parents_name_ + "ReadingCount"));
        setInWriting(rs.getBoolean(_parents_name_ + "InWriting"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "ReadingCount", getReadingCount());
        st.appendBoolean(_parents_name_ + "InWriting", isInWriting());
    }
}
