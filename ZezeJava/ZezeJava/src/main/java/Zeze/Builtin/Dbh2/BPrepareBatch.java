// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BPrepareBatch extends Zeze.Transaction.Bean implements BPrepareBatchReadOnly {
    public static final long TYPEID = 216908947802855063L;

    private String _Database; // 用来纠错
    private String _Table; // 用来纠错
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Dbh2.BBatch> _Batch;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

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

    public Zeze.Builtin.Dbh2.BBatch getBatch() {
        return _Batch.getValue();
    }

    public void setBatch(Zeze.Builtin.Dbh2.BBatch value) {
        _Batch.setValue(value);
    }

    @Override
    public Zeze.Builtin.Dbh2.BBatchReadOnly getBatchReadOnly() {
        return _Batch.getValue();
    }

    @SuppressWarnings("deprecation")
    public BPrepareBatch() {
        _Database = "";
        _Table = "";
        _Batch = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Dbh2.BBatch(), Zeze.Builtin.Dbh2.BBatch.class);
        _Batch.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BPrepareBatch(String _Database_, String _Table_) {
        if (_Database_ == null)
            throw new IllegalArgumentException();
        _Database = _Database_;
        if (_Table_ == null)
            throw new IllegalArgumentException();
        _Table = _Table_;
        _Batch = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Dbh2.BBatch(), Zeze.Builtin.Dbh2.BBatch.class);
        _Batch.variableId(3);
    }

    @Override
    public Zeze.Builtin.Dbh2.BPrepareBatch.Data toData() {
        var data = new Zeze.Builtin.Dbh2.BPrepareBatch.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BPrepareBatch.Data)other);
    }

    public void assign(BPrepareBatch.Data other) {
        setDatabase(other._Database);
        setTable(other._Table);
        Zeze.Builtin.Dbh2.BBatch data_Batch = new Zeze.Builtin.Dbh2.BBatch();
        data_Batch.assign(other._Batch);
        _Batch.setValue(data_Batch);
    }

    public void assign(BPrepareBatch other) {
        setDatabase(other.getDatabase());
        setTable(other.getTable());
        _Batch.assign(other._Batch);
    }

    public BPrepareBatch copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BPrepareBatch copy() {
        var copy = new BPrepareBatch();
        copy.assign(this);
        return copy;
    }

    public static void swap(BPrepareBatch a, BPrepareBatch b) {
        BPrepareBatch save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Database extends Zeze.Transaction.Logs.LogString {
        public Log__Database(BPrepareBatch bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BPrepareBatch)getBelong())._Database = value; }
    }

    private static final class Log__Table extends Zeze.Transaction.Logs.LogString {
        public Log__Table(BPrepareBatch bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BPrepareBatch)getBelong())._Table = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BPrepareBatch: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Database=").append(getDatabase()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Table=").append(getTable()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Batch=").append(System.lineSeparator());
        _Batch.buildString(sb, level + 4);
        sb.append(System.lineSeparator());
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 3, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Batch.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
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
            _o_.ReadBean(_Batch, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Batch.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Batch.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_Batch.negativeCheck())
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
                case 1: _Database = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _Table = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _Batch.followerApply(vlog); break;
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
        parents.add("Batch");
        _Batch.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Database", getDatabase());
        st.appendString(_parents_name_ + "Table", getTable());
        parents.add("Batch");
        _Batch.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 216908947802855063L;

    private String _Database; // 用来纠错
    private String _Table; // 用来纠错
    private Zeze.Builtin.Dbh2.BBatch.Data _Batch;

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

    public Zeze.Builtin.Dbh2.BBatch.Data getBatch() {
        return _Batch;
    }

    public void setBatch(Zeze.Builtin.Dbh2.BBatch.Data value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Batch = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Database = "";
        _Table = "";
        _Batch = new Zeze.Builtin.Dbh2.BBatch.Data();
        _Batch.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public Data(String _Database_, String _Table_) {
        if (_Database_ == null)
            throw new IllegalArgumentException();
        _Database = _Database_;
        if (_Table_ == null)
            throw new IllegalArgumentException();
        _Table = _Table_;
        _Batch = new Zeze.Builtin.Dbh2.BBatch.Data();
        _Batch.variableId(3);
    }

    @Override
    public Zeze.Builtin.Dbh2.BPrepareBatch toBean() {
        var bean = new Zeze.Builtin.Dbh2.BPrepareBatch();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BPrepareBatch)other);
    }

    public void assign(BPrepareBatch other) {
        _Database = other.getDatabase();
        _Table = other.getTable();
        _Batch.assign(other._Batch.getValue());
    }

    public void assign(BPrepareBatch.Data other) {
        _Database = other._Database;
        _Table = other._Table;
        _Batch.assign(other._Batch);
    }

    @Override
    public BPrepareBatch.Data copy() {
        var copy = new BPrepareBatch.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BPrepareBatch.Data a, BPrepareBatch.Data b) {
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BPrepareBatch: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Database=").append(_Database).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Table=").append(_Table).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Batch=").append(System.lineSeparator());
        _Batch.buildString(sb, level + 4);
        sb.append(System.lineSeparator());
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 3, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Batch.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
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
            _o_.ReadBean(_Batch, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
