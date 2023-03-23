// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BCommitTransactionArgument extends Zeze.Transaction.Bean implements BCommitTransactionArgumentReadOnly {
    public static final long TYPEID = -3031238775854345886L;

    private long _TransactionId;

    @Override
    public long getTransactionId() {
        if (!isManaged())
            return _TransactionId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TransactionId;
        var log = (Log__TransactionId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TransactionId;
    }

    public void setTransactionId(long value) {
        if (!isManaged()) {
            _TransactionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TransactionId(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BCommitTransactionArgument() {
    }

    @SuppressWarnings("deprecation")
    public BCommitTransactionArgument(long _TransactionId_) {
        _TransactionId = _TransactionId_;
    }

    @Override
    public Zeze.Builtin.Dbh2.BCommitTransactionArgument.Data toData() {
        var data = new Zeze.Builtin.Dbh2.BCommitTransactionArgument.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BCommitTransactionArgument.Data)other);
    }

    public void assign(BCommitTransactionArgument.Data other) {
        setTransactionId(other.getTransactionId());
    }

    public void assign(BCommitTransactionArgument other) {
        setTransactionId(other.getTransactionId());
    }

    public BCommitTransactionArgument copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCommitTransactionArgument copy() {
        var copy = new BCommitTransactionArgument();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCommitTransactionArgument a, BCommitTransactionArgument b) {
        BCommitTransactionArgument save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TransactionId extends Zeze.Transaction.Logs.LogLong {
        public Log__TransactionId(BCommitTransactionArgument bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCommitTransactionArgument)getBelong())._TransactionId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BCommitTransactionArgument: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TransactionId=").append(getTransactionId()).append(System.lineSeparator());
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
            long _x_ = getTransactionId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
            setTransactionId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getTransactionId() < 0)
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
                case 1: _TransactionId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTransactionId(rs.getLong(_parents_name_ + "TransactionId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "TransactionId", getTransactionId());
    }

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -3031238775854345886L;

    private long _TransactionId;

    public long getTransactionId() {
        return _TransactionId;
    }

    public void setTransactionId(long value) {
        _TransactionId = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(long _TransactionId_) {
        _TransactionId = _TransactionId_;
    }

    @Override
    public Zeze.Builtin.Dbh2.BCommitTransactionArgument toBean() {
        var bean = new Zeze.Builtin.Dbh2.BCommitTransactionArgument();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BCommitTransactionArgument)other);
    }

    public void assign(BCommitTransactionArgument other) {
        setTransactionId(other.getTransactionId());
    }

    public void assign(BCommitTransactionArgument.Data other) {
        setTransactionId(other.getTransactionId());
    }

    @Override
    public BCommitTransactionArgument.Data copy() {
        var copy = new BCommitTransactionArgument.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCommitTransactionArgument.Data a, BCommitTransactionArgument.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BCommitTransactionArgument: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TransactionId=").append(getTransactionId()).append(System.lineSeparator());
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
            long _x_ = getTransactionId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
            setTransactionId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
