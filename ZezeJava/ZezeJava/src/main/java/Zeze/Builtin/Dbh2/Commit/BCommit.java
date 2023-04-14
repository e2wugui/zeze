// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BCommit extends Zeze.Transaction.Bean implements BCommitReadOnly {
    public static final long TYPEID = 3148866502298196978L;

    private Zeze.Net.Binary _TransactionData;

    @Override
    public Zeze.Net.Binary getTransactionData() {
        if (!isManaged())
            return _TransactionData;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TransactionData;
        var log = (Log__TransactionData)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TransactionData;
    }

    public void setTransactionData(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TransactionData = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TransactionData(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BCommit() {
        _TransactionData = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BCommit(Zeze.Net.Binary _TransactionData_) {
        if (_TransactionData_ == null)
            throw new IllegalArgumentException();
        _TransactionData = _TransactionData_;
    }

    @Override
    public Zeze.Builtin.Dbh2.Commit.BCommit.Data toData() {
        var data = new Zeze.Builtin.Dbh2.Commit.BCommit.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.Commit.BCommit.Data)other);
    }

    public void assign(BCommit.Data other) {
        setTransactionData(other.getTransactionData());
    }

    public void assign(BCommit other) {
        setTransactionData(other.getTransactionData());
    }

    public BCommit copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCommit copy() {
        var copy = new BCommit();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCommit a, BCommit b) {
        BCommit save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TransactionData extends Zeze.Transaction.Logs.LogBinary {
        public Log__TransactionData(BCommit bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCommit)getBelong())._TransactionData = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Commit.BCommit: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TransactionData=").append(getTransactionData()).append(System.lineSeparator());
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
            var _x_ = getTransactionData();
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
            setTransactionData(_o_.ReadBinary(_t_));
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
                case 1: _TransactionData = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTransactionData(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "TransactionData")));
        if (getTransactionData() == null)
            setTransactionData(Zeze.Net.Binary.Empty);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBinary(_parents_name_ + "TransactionData", getTransactionData());
    }

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 3148866502298196978L;

    private Zeze.Net.Binary _TransactionData;

    public Zeze.Net.Binary getTransactionData() {
        return _TransactionData;
    }

    public void setTransactionData(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _TransactionData = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _TransactionData = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _TransactionData_) {
        if (_TransactionData_ == null)
            throw new IllegalArgumentException();
        _TransactionData = _TransactionData_;
    }

    @Override
    public Zeze.Builtin.Dbh2.Commit.BCommit toBean() {
        var bean = new Zeze.Builtin.Dbh2.Commit.BCommit();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BCommit)other);
    }

    public void assign(BCommit other) {
        setTransactionData(other.getTransactionData());
    }

    public void assign(BCommit.Data other) {
        setTransactionData(other.getTransactionData());
    }

    @Override
    public BCommit.Data copy() {
        var copy = new BCommit.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCommit.Data a, BCommit.Data b) {
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Commit.BCommit: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TransactionData=").append(getTransactionData()).append(System.lineSeparator());
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
            var _x_ = getTransactionData();
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
            setTransactionData(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
