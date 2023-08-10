// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BBucketMeta extends Zeze.Transaction.Bean implements BBucketMetaReadOnly {
    public static final long TYPEID = 8589859502117192635L;

    private String _DatabaseName;
    private String _TableName;
    private Zeze.Net.Binary _KeyFirst;
    private Zeze.Net.Binary _KeyLast;
    private String _RaftConfig;

    @Override
    public String getDatabaseName() {
        if (!isManaged())
            return _DatabaseName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _DatabaseName;
        var log = (Log__DatabaseName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _DatabaseName;
    }

    public void setDatabaseName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _DatabaseName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__DatabaseName(this, 1, value));
    }

    @Override
    public String getTableName() {
        if (!isManaged())
            return _TableName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TableName;
        var log = (Log__TableName)txn.getLog(objectId() + 2);
        return log != null ? log.value : _TableName;
    }

    public void setTableName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TableName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TableName(this, 2, value));
    }

    @Override
    public Zeze.Net.Binary getKeyFirst() {
        if (!isManaged())
            return _KeyFirst;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _KeyFirst;
        var log = (Log__KeyFirst)txn.getLog(objectId() + 3);
        return log != null ? log.value : _KeyFirst;
    }

    public void setKeyFirst(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _KeyFirst = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__KeyFirst(this, 3, value));
    }

    @Override
    public Zeze.Net.Binary getKeyLast() {
        if (!isManaged())
            return _KeyLast;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _KeyLast;
        var log = (Log__KeyLast)txn.getLog(objectId() + 4);
        return log != null ? log.value : _KeyLast;
    }

    public void setKeyLast(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _KeyLast = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__KeyLast(this, 4, value));
    }

    @Override
    public String getRaftConfig() {
        if (!isManaged())
            return _RaftConfig;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _RaftConfig;
        var log = (Log__RaftConfig)txn.getLog(objectId() + 5);
        return log != null ? log.value : _RaftConfig;
    }

    public void setRaftConfig(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _RaftConfig = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__RaftConfig(this, 5, value));
    }

    @SuppressWarnings("deprecation")
    public BBucketMeta() {
        _DatabaseName = "";
        _TableName = "";
        _KeyFirst = Zeze.Net.Binary.Empty;
        _KeyLast = Zeze.Net.Binary.Empty;
        _RaftConfig = "";
    }

    @SuppressWarnings("deprecation")
    public BBucketMeta(String _DatabaseName_, String _TableName_, Zeze.Net.Binary _KeyFirst_, Zeze.Net.Binary _KeyLast_, String _RaftConfig_) {
        if (_DatabaseName_ == null)
            _DatabaseName_ = "";
        _DatabaseName = _DatabaseName_;
        if (_TableName_ == null)
            _TableName_ = "";
        _TableName = _TableName_;
        if (_KeyFirst_ == null)
            _KeyFirst_ = Zeze.Net.Binary.Empty;
        _KeyFirst = _KeyFirst_;
        if (_KeyLast_ == null)
            _KeyLast_ = Zeze.Net.Binary.Empty;
        _KeyLast = _KeyLast_;
        if (_RaftConfig_ == null)
            _RaftConfig_ = "";
        _RaftConfig = _RaftConfig_;
    }

    @Override
    public void reset() {
        setDatabaseName("");
        setTableName("");
        setKeyFirst(Zeze.Net.Binary.Empty);
        setKeyLast(Zeze.Net.Binary.Empty);
        setRaftConfig("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.BBucketMeta.Data toData() {
        var data = new Zeze.Builtin.Dbh2.BBucketMeta.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BBucketMeta.Data)other);
    }

    public void assign(BBucketMeta.Data other) {
        setDatabaseName(other._DatabaseName);
        setTableName(other._TableName);
        setKeyFirst(other._KeyFirst);
        setKeyLast(other._KeyLast);
        setRaftConfig(other._RaftConfig);
        _unknown_ = null;
    }

    public void assign(BBucketMeta other) {
        setDatabaseName(other.getDatabaseName());
        setTableName(other.getTableName());
        setKeyFirst(other.getKeyFirst());
        setKeyLast(other.getKeyLast());
        setRaftConfig(other.getRaftConfig());
        _unknown_ = other._unknown_;
    }

    public BBucketMeta copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBucketMeta copy() {
        var copy = new BBucketMeta();
        copy.assign(this);
        return copy;
    }

    public static void swap(BBucketMeta a, BBucketMeta b) {
        BBucketMeta save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__DatabaseName extends Zeze.Transaction.Logs.LogString {
        public Log__DatabaseName(BBucketMeta bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBucketMeta)getBelong())._DatabaseName = value; }
    }

    private static final class Log__TableName extends Zeze.Transaction.Logs.LogString {
        public Log__TableName(BBucketMeta bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBucketMeta)getBelong())._TableName = value; }
    }

    private static final class Log__KeyFirst extends Zeze.Transaction.Logs.LogBinary {
        public Log__KeyFirst(BBucketMeta bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBucketMeta)getBelong())._KeyFirst = value; }
    }

    private static final class Log__KeyLast extends Zeze.Transaction.Logs.LogBinary {
        public Log__KeyLast(BBucketMeta bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBucketMeta)getBelong())._KeyLast = value; }
    }

    private static final class Log__RaftConfig extends Zeze.Transaction.Logs.LogString {
        public Log__RaftConfig(BBucketMeta bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBucketMeta)getBelong())._RaftConfig = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BBucketMeta: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("DatabaseName=").append(getDatabaseName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TableName=").append(getTableName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("KeyFirst=").append(getKeyFirst()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("KeyLast=").append(getKeyLast()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RaftConfig=").append(getRaftConfig()).append(System.lineSeparator());
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
            String _x_ = getDatabaseName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getTableName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getKeyFirst();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = getKeyLast();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = getRaftConfig();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
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
            setDatabaseName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTableName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setKeyFirst(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setKeyLast(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setRaftConfig(_o_.ReadString(_t_));
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
                case 1: _DatabaseName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _TableName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _KeyFirst = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 4: _KeyLast = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 5: _RaftConfig = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setDatabaseName(rs.getString(_parents_name_ + "DatabaseName"));
        if (getDatabaseName() == null)
            setDatabaseName("");
        setTableName(rs.getString(_parents_name_ + "TableName"));
        if (getTableName() == null)
            setTableName("");
        setKeyFirst(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "KeyFirst")));
        if (getKeyFirst() == null)
            setKeyFirst(Zeze.Net.Binary.Empty);
        setKeyLast(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "KeyLast")));
        if (getKeyLast() == null)
            setKeyLast(Zeze.Net.Binary.Empty);
        setRaftConfig(rs.getString(_parents_name_ + "RaftConfig"));
        if (getRaftConfig() == null)
            setRaftConfig("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "DatabaseName", getDatabaseName());
        st.appendString(_parents_name_ + "TableName", getTableName());
        st.appendBinary(_parents_name_ + "KeyFirst", getKeyFirst());
        st.appendBinary(_parents_name_ + "KeyLast", getKeyLast());
        st.appendString(_parents_name_ + "RaftConfig", getRaftConfig());
    }

    @Override
    public java.util.List<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "DatabaseName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "TableName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "KeyFirst", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "KeyLast", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "RaftConfig", "string", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BBucketMeta
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 8589859502117192635L;

    private String _DatabaseName;
    private String _TableName;
    private Zeze.Net.Binary _KeyFirst;
    private Zeze.Net.Binary _KeyLast;
    private String _RaftConfig;

    public String getDatabaseName() {
        return _DatabaseName;
    }

    public void setDatabaseName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _DatabaseName = value;
    }

    public String getTableName() {
        return _TableName;
    }

    public void setTableName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _TableName = value;
    }

    public Zeze.Net.Binary getKeyFirst() {
        return _KeyFirst;
    }

    public void setKeyFirst(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _KeyFirst = value;
    }

    public Zeze.Net.Binary getKeyLast() {
        return _KeyLast;
    }

    public void setKeyLast(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _KeyLast = value;
    }

    public String getRaftConfig() {
        return _RaftConfig;
    }

    public void setRaftConfig(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _RaftConfig = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _DatabaseName = "";
        _TableName = "";
        _KeyFirst = Zeze.Net.Binary.Empty;
        _KeyLast = Zeze.Net.Binary.Empty;
        _RaftConfig = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _DatabaseName_, String _TableName_, Zeze.Net.Binary _KeyFirst_, Zeze.Net.Binary _KeyLast_, String _RaftConfig_) {
        if (_DatabaseName_ == null)
            _DatabaseName_ = "";
        _DatabaseName = _DatabaseName_;
        if (_TableName_ == null)
            _TableName_ = "";
        _TableName = _TableName_;
        if (_KeyFirst_ == null)
            _KeyFirst_ = Zeze.Net.Binary.Empty;
        _KeyFirst = _KeyFirst_;
        if (_KeyLast_ == null)
            _KeyLast_ = Zeze.Net.Binary.Empty;
        _KeyLast = _KeyLast_;
        if (_RaftConfig_ == null)
            _RaftConfig_ = "";
        _RaftConfig = _RaftConfig_;
    }

    @Override
    public void reset() {
        _DatabaseName = "";
        _TableName = "";
        _KeyFirst = Zeze.Net.Binary.Empty;
        _KeyLast = Zeze.Net.Binary.Empty;
        _RaftConfig = "";
    }

    @Override
    public Zeze.Builtin.Dbh2.BBucketMeta toBean() {
        var bean = new Zeze.Builtin.Dbh2.BBucketMeta();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BBucketMeta)other);
    }

    public void assign(BBucketMeta other) {
        _DatabaseName = other.getDatabaseName();
        _TableName = other.getTableName();
        _KeyFirst = other.getKeyFirst();
        _KeyLast = other.getKeyLast();
        _RaftConfig = other.getRaftConfig();
    }

    public void assign(BBucketMeta.Data other) {
        _DatabaseName = other._DatabaseName;
        _TableName = other._TableName;
        _KeyFirst = other._KeyFirst;
        _KeyLast = other._KeyLast;
        _RaftConfig = other._RaftConfig;
    }

    @Override
    public BBucketMeta.Data copy() {
        var copy = new BBucketMeta.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BBucketMeta.Data a, BBucketMeta.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BBucketMeta.Data clone() {
        return (BBucketMeta.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BBucketMeta: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("DatabaseName=").append(_DatabaseName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TableName=").append(_TableName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("KeyFirst=").append(_KeyFirst).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("KeyLast=").append(_KeyLast).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RaftConfig=").append(_RaftConfig).append(System.lineSeparator());
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
            String _x_ = _DatabaseName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _TableName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _KeyFirst;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _KeyLast;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = _RaftConfig;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
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
            _DatabaseName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _TableName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _KeyFirst = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _KeyLast = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _RaftConfig = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
