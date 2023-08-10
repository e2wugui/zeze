// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BCreateDatabase extends Zeze.Transaction.Bean implements BCreateDatabaseReadOnly {
    public static final long TYPEID = -4068258744708449065L;

    private String _Database;

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

    @SuppressWarnings("deprecation")
    public BCreateDatabase() {
        _Database = "";
    }

    @SuppressWarnings("deprecation")
    public BCreateDatabase(String _Database_) {
        if (_Database_ == null)
            _Database_ = "";
        _Database = _Database_;
    }

    @Override
    public void reset() {
        setDatabase("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BCreateDatabase.Data toData() {
        var data = new Zeze.Builtin.Dbh2.Master.BCreateDatabase.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.Master.BCreateDatabase.Data)other);
    }

    public void assign(BCreateDatabase.Data other) {
        setDatabase(other._Database);
        _unknown_ = null;
    }

    public void assign(BCreateDatabase other) {
        setDatabase(other.getDatabase());
        _unknown_ = other._unknown_;
    }

    public BCreateDatabase copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCreateDatabase copy() {
        var copy = new BCreateDatabase();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCreateDatabase a, BCreateDatabase b) {
        BCreateDatabase save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Database extends Zeze.Transaction.Logs.LogString {
        public Log__Database(BCreateDatabase bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCreateDatabase)getBelong())._Database = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BCreateDatabase: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Database=").append(getDatabase()).append(System.lineSeparator());
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
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setDatabase(rs.getString(_parents_name_ + "Database"));
        if (getDatabase() == null)
            setDatabase("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Database", getDatabase());
    }

    @Override
    public java.util.List<Zeze.Transaction.Bean.Variable> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Transaction.Bean.Variable(1, "Database", "string", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BCreateDatabase
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4068258744708449065L;

    private String _Database;

    public String getDatabase() {
        return _Database;
    }

    public void setDatabase(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Database = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Database = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _Database_) {
        if (_Database_ == null)
            _Database_ = "";
        _Database = _Database_;
    }

    @Override
    public void reset() {
        _Database = "";
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BCreateDatabase toBean() {
        var bean = new Zeze.Builtin.Dbh2.Master.BCreateDatabase();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BCreateDatabase)other);
    }

    public void assign(BCreateDatabase other) {
        _Database = other.getDatabase();
    }

    public void assign(BCreateDatabase.Data other) {
        _Database = other._Database;
    }

    @Override
    public BCreateDatabase.Data copy() {
        var copy = new BCreateDatabase.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCreateDatabase.Data a, BCreateDatabase.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCreateDatabase.Data clone() {
        return (BCreateDatabase.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BCreateDatabase: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Database=").append(_Database).append(System.lineSeparator());
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
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
