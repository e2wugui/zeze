// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BQuery extends Zeze.Transaction.Bean implements BQueryReadOnly {
    public static final long TYPEID = -5548513319551035665L;

    private Zeze.Net.Binary _BucketRaftSortedNames;
    private long _BucketTid;

    @Override
    public Zeze.Net.Binary getBucketRaftSortedNames() {
        if (!isManaged())
            return _BucketRaftSortedNames;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _BucketRaftSortedNames;
        var log = (Log__BucketRaftSortedNames)txn.getLog(objectId() + 1);
        return log != null ? log.value : _BucketRaftSortedNames;
    }

    public void setBucketRaftSortedNames(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _BucketRaftSortedNames = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__BucketRaftSortedNames(this, 1, value));
    }

    @Override
    public long getBucketTid() {
        if (!isManaged())
            return _BucketTid;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _BucketTid;
        var log = (Log__BucketTid)txn.getLog(objectId() + 2);
        return log != null ? log.value : _BucketTid;
    }

    public void setBucketTid(long value) {
        if (!isManaged()) {
            _BucketTid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__BucketTid(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BQuery() {
        _BucketRaftSortedNames = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BQuery(Zeze.Net.Binary _BucketRaftSortedNames_, long _BucketTid_) {
        if (_BucketRaftSortedNames_ == null)
            throw new IllegalArgumentException();
        _BucketRaftSortedNames = _BucketRaftSortedNames_;
        _BucketTid = _BucketTid_;
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
        setBucketRaftSortedNames(other.getBucketRaftSortedNames());
        setBucketTid(other.getBucketTid());
    }

    public void assign(BQuery other) {
        setBucketRaftSortedNames(other.getBucketRaftSortedNames());
        setBucketTid(other.getBucketTid());
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

    private static final class Log__BucketRaftSortedNames extends Zeze.Transaction.Logs.LogBinary {
        public Log__BucketRaftSortedNames(BQuery bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BQuery)getBelong())._BucketRaftSortedNames = value; }
    }

    private static final class Log__BucketTid extends Zeze.Transaction.Logs.LogLong {
        public Log__BucketTid(BQuery bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BQuery)getBelong())._BucketTid = value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("BucketRaftSortedNames=").append(getBucketRaftSortedNames()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("BucketTid=").append(getBucketTid()).append(System.lineSeparator());
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
            var _x_ = getBucketRaftSortedNames();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = getBucketTid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setBucketRaftSortedNames(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setBucketTid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getBucketTid() < 0)
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
                case 1: _BucketRaftSortedNames = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 2: _BucketTid = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setBucketRaftSortedNames(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "BucketRaftSortedNames")));
        if (getBucketRaftSortedNames() == null)
            setBucketRaftSortedNames(Zeze.Net.Binary.Empty);
        setBucketTid(rs.getLong(_parents_name_ + "BucketTid"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBinary(_parents_name_ + "BucketRaftSortedNames", getBucketRaftSortedNames());
        st.appendLong(_parents_name_ + "BucketTid", getBucketTid());
    }

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5548513319551035665L;

    private Zeze.Net.Binary _BucketRaftSortedNames;
    private long _BucketTid;

    public Zeze.Net.Binary getBucketRaftSortedNames() {
        return _BucketRaftSortedNames;
    }

    public void setBucketRaftSortedNames(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _BucketRaftSortedNames = value;
    }

    public long getBucketTid() {
        return _BucketTid;
    }

    public void setBucketTid(long value) {
        _BucketTid = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _BucketRaftSortedNames = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _BucketRaftSortedNames_, long _BucketTid_) {
        if (_BucketRaftSortedNames_ == null)
            throw new IllegalArgumentException();
        _BucketRaftSortedNames = _BucketRaftSortedNames_;
        _BucketTid = _BucketTid_;
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
        setBucketRaftSortedNames(other.getBucketRaftSortedNames());
        setBucketTid(other.getBucketTid());
    }

    public void assign(BQuery.Data other) {
        setBucketRaftSortedNames(other.getBucketRaftSortedNames());
        setBucketTid(other.getBucketTid());
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
        sb.append(Zeze.Util.Str.indent(level)).append("BucketRaftSortedNames=").append(getBucketRaftSortedNames()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("BucketTid=").append(getBucketTid()).append(System.lineSeparator());
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
            var _x_ = getBucketRaftSortedNames();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = getBucketTid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setBucketRaftSortedNames(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setBucketTid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
