// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;

/*
				Dbh2发现桶没找到错误时，使用GetBuckets得到完整的信息。
				因为只LocateBucket最新的桶信息虽然能用，但是出现桶没找到错误时，通常意味着前一个桶的信息也需要更新。
				不更新旧桶，桶的定位方法可以工作（只依赖桶的KeyFrist），但感觉不好。
				所以LocateBucket先不用，仅使用GetBuckets。
*/
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BLocateBucket extends Zeze.Transaction.Bean implements BLocateBucketReadOnly {
    public static final long TYPEID = 8564400157292168322L;

    private String _Database;
    private String _Table;
    private Zeze.Net.Binary _Key;

    @Override
    public String getDatabase() {
        if (!isManaged())
            return _Database;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Database;
        var log = (Log__Database)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Database;
    }

    public void setDatabase(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Database = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Database(this, 1, value));
    }

    @Override
    public String getTable() {
        if (!isManaged())
            return _Table;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Table;
        var log = (Log__Table)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Table;
    }

    public void setTable(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Table = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Table(this, 2, value));
    }

    @Override
    public Zeze.Net.Binary getKey() {
        if (!isManaged())
            return _Key;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Key;
        var log = (Log__Key)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Key;
    }

    public void setKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Key = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Key(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BLocateBucket() {
        _Database = "";
        _Table = "";
        _Key = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BLocateBucket(String _Database_, String _Table_, Zeze.Net.Binary _Key_) {
        if (_Database_ == null)
            _Database_ = "";
        _Database = _Database_;
        if (_Table_ == null)
            _Table_ = "";
        _Table = _Table_;
        if (_Key_ == null)
            _Key_ = Zeze.Net.Binary.Empty;
        _Key = _Key_;
    }

    @Override
    public void reset() {
        setDatabase("");
        setTable("");
        setKey(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BLocateBucket.Data toData() {
        var data = new Zeze.Builtin.Dbh2.Master.BLocateBucket.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.Master.BLocateBucket.Data)other);
    }

    public void assign(BLocateBucket.Data other) {
        setDatabase(other._Database);
        setTable(other._Table);
        setKey(other._Key);
        _unknown_ = null;
    }

    public void assign(BLocateBucket other) {
        setDatabase(other.getDatabase());
        setTable(other.getTable());
        setKey(other.getKey());
        _unknown_ = other._unknown_;
    }

    public BLocateBucket copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLocateBucket copy() {
        var copy = new BLocateBucket();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLocateBucket a, BLocateBucket b) {
        BLocateBucket save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Database extends Zeze.Transaction.Logs.LogString {
        public Log__Database(BLocateBucket bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLocateBucket)getBelong())._Database = value; }
    }

    private static final class Log__Table extends Zeze.Transaction.Logs.LogString {
        public Log__Table(BLocateBucket bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLocateBucket)getBelong())._Table = value; }
    }

    private static final class Log__Key extends Zeze.Transaction.Logs.LogBinary {
        public Log__Key(BLocateBucket bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLocateBucket)getBelong())._Key = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BLocateBucket: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Database=").append(getDatabase()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Table=").append(getTable()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(getKey()).append(System.lineSeparator());
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
            String _x_ = getDatabase();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getTable();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
            setDatabase(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTable(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setKey(_o_.ReadBinary(_t_));
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
                case 1: _Database = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _Table = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _Key = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setDatabase(rs.getString(_parents_name_ + "Database"));
        if (getDatabase() == null)
            setDatabase("");
        setTable(rs.getString(_parents_name_ + "Table"));
        if (getTable() == null)
            setTable("");
        setKey(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Key")));
        if (getKey() == null)
            setKey(Zeze.Net.Binary.Empty);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Database", getDatabase());
        st.appendString(_parents_name_ + "Table", getTable());
        st.appendBinary(_parents_name_ + "Key", getKey());
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BLocateBucket
    }

/*
				Dbh2发现桶没找到错误时，使用GetBuckets得到完整的信息。
				因为只LocateBucket最新的桶信息虽然能用，但是出现桶没找到错误时，通常意味着前一个桶的信息也需要更新。
				不更新旧桶，桶的定位方法可以工作（只依赖桶的KeyFrist），但感觉不好。
				所以LocateBucket先不用，仅使用GetBuckets。
*/
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 8564400157292168322L;

    private String _Database;
    private String _Table;
    private Zeze.Net.Binary _Key;

    public String getDatabase() {
        return _Database;
    }

    public void setDatabase(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Database = value;
    }

    public String getTable() {
        return _Table;
    }

    public void setTable(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Table = value;
    }

    public Zeze.Net.Binary getKey() {
        return _Key;
    }

    public void setKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Key = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Database = "";
        _Table = "";
        _Key = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(String _Database_, String _Table_, Zeze.Net.Binary _Key_) {
        if (_Database_ == null)
            _Database_ = "";
        _Database = _Database_;
        if (_Table_ == null)
            _Table_ = "";
        _Table = _Table_;
        if (_Key_ == null)
            _Key_ = Zeze.Net.Binary.Empty;
        _Key = _Key_;
    }

    @Override
    public void reset() {
        _Database = "";
        _Table = "";
        _Key = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BLocateBucket toBean() {
        var bean = new Zeze.Builtin.Dbh2.Master.BLocateBucket();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BLocateBucket)other);
    }

    public void assign(BLocateBucket other) {
        _Database = other.getDatabase();
        _Table = other.getTable();
        _Key = other.getKey();
    }

    public void assign(BLocateBucket.Data other) {
        _Database = other._Database;
        _Table = other._Table;
        _Key = other._Key;
    }

    @Override
    public BLocateBucket.Data copy() {
        var copy = new BLocateBucket.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLocateBucket.Data a, BLocateBucket.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLocateBucket.Data clone() {
        return (BLocateBucket.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BLocateBucket: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Database=").append(_Database).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Table=").append(_Table).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_Key).append(System.lineSeparator());
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
            String _x_ = _Database;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _Table;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _Key;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            _Database = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Table = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Key = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
