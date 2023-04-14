// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BQuery extends Zeze.Transaction.Bean implements BQueryReadOnly {
    public static final long TYPEID = -5548513319551035665L;

    private Zeze.Net.Binary _TransactionKey;

    @Override
    public Zeze.Net.Binary getTransactionKey() {
        if (!isManaged())
            return _TransactionKey;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TransactionKey;
        var log = (Log__TransactionKey)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TransactionKey;
    }

    public void setTransactionKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TransactionKey = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TransactionKey(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BQuery() {
        _TransactionKey = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BQuery(Zeze.Net.Binary _TransactionKey_) {
        if (_TransactionKey_ == null)
            throw new IllegalArgumentException();
        _TransactionKey = _TransactionKey_;
    }

    @Override
    public Zeze.Builtin.Dbh2.Commit.BQuery.Data toData() {
        var data = new Zeze.Builtin.Dbh2.Commit.BQuery.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.Commit.BQuery.Data)other);
    }

    public void assign(BQuery.Data other) {
        setTransactionKey(other.getTransactionKey());
    }

    public void assign(BQuery other) {
        setTransactionKey(other.getTransactionKey());
    }

    public BQuery copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BQuery copy() {
        var copy = new BQuery();
        copy.assign(this);
        return copy;
    }

    public static void swap(BQuery a, BQuery b) {
        BQuery save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TransactionKey extends Zeze.Transaction.Logs.LogBinary {
        public Log__TransactionKey(BQuery bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BQuery)getBelong())._TransactionKey = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Commit.BQuery: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TransactionKey=").append(getTransactionKey()).append(System.lineSeparator());
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
            var _x_ = getTransactionKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setTransactionKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
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
                case 1: _TransactionKey = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTransactionKey(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "TransactionKey")));
        if (getTransactionKey() == null)
            setTransactionKey(Zeze.Net.Binary.Empty);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBinary(_parents_name_ + "TransactionKey", getTransactionKey());
    }

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5548513319551035665L;

    private Zeze.Net.Binary _TransactionKey;

    public Zeze.Net.Binary getTransactionKey() {
        return _TransactionKey;
    }

    public void setTransactionKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _TransactionKey = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _TransactionKey = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _TransactionKey_) {
        if (_TransactionKey_ == null)
            throw new IllegalArgumentException();
        _TransactionKey = _TransactionKey_;
    }

    @Override
    public Zeze.Builtin.Dbh2.Commit.BQuery toBean() {
        var bean = new Zeze.Builtin.Dbh2.Commit.BQuery();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BQuery)other);
    }

    public void assign(BQuery other) {
        setTransactionKey(other.getTransactionKey());
    }

    public void assign(BQuery.Data other) {
        setTransactionKey(other.getTransactionKey());
    }

    @Override
    public BQuery.Data copy() {
        var copy = new BQuery.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BQuery.Data a, BQuery.Data b) {
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Commit.BQuery: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TransactionKey=").append(getTransactionKey()).append(System.lineSeparator());
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
            var _x_ = getTransactionKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setTransactionKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
