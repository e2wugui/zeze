// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BBucketMeta extends Zeze.Transaction.Bean implements BBucketMetaReadOnly {
    public static final long TYPEID = 8589859502117192635L;

    private String _DatabaseName;
    private String _TableName;
    private Zeze.Net.Binary _KeyFirst;
    private Zeze.Net.Binary _KeyLast;
    private String _RaftConfig;
    private boolean _Moving; // 正在迁移中
    private Zeze.Net.Binary _KeyMoving; // 正在迁移中的key

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

    @Override
    public boolean isMoving() {
        if (!isManaged())
            return _Moving;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Moving;
        var log = (Log__Moving)txn.getLog(objectId() + 6);
        return log != null ? log.value : _Moving;
    }

    public void setMoving(boolean value) {
        if (!isManaged()) {
            _Moving = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Moving(this, 6, value));
    }

    @Override
    public Zeze.Net.Binary getKeyMoving() {
        if (!isManaged())
            return _KeyMoving;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _KeyMoving;
        var log = (Log__KeyMoving)txn.getLog(objectId() + 7);
        return log != null ? log.value : _KeyMoving;
    }

    public void setKeyMoving(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _KeyMoving = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__KeyMoving(this, 7, value));
    }

    @SuppressWarnings("deprecation")
    public BBucketMeta() {
        _DatabaseName = "";
        _TableName = "";
        _KeyFirst = Zeze.Net.Binary.Empty;
        _KeyLast = Zeze.Net.Binary.Empty;
        _RaftConfig = "";
        _KeyMoving = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BBucketMeta(String _DatabaseName_, String _TableName_, Zeze.Net.Binary _KeyFirst_, Zeze.Net.Binary _KeyLast_, String _RaftConfig_, boolean _Moving_, Zeze.Net.Binary _KeyMoving_) {
        if (_DatabaseName_ == null)
            throw new IllegalArgumentException();
        _DatabaseName = _DatabaseName_;
        if (_TableName_ == null)
            throw new IllegalArgumentException();
        _TableName = _TableName_;
        if (_KeyFirst_ == null)
            throw new IllegalArgumentException();
        _KeyFirst = _KeyFirst_;
        if (_KeyLast_ == null)
            throw new IllegalArgumentException();
        _KeyLast = _KeyLast_;
        if (_RaftConfig_ == null)
            throw new IllegalArgumentException();
        _RaftConfig = _RaftConfig_;
        _Moving = _Moving_;
        if (_KeyMoving_ == null)
            throw new IllegalArgumentException();
        _KeyMoving = _KeyMoving_;
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
        setDatabaseName(other.getDatabaseName());
        setTableName(other.getTableName());
        setKeyFirst(other.getKeyFirst());
        setKeyLast(other.getKeyLast());
        setRaftConfig(other.getRaftConfig());
        setMoving(other.isMoving());
        setKeyMoving(other.getKeyMoving());
    }

    public void assign(BBucketMeta other) {
        setDatabaseName(other.getDatabaseName());
        setTableName(other.getTableName());
        setKeyFirst(other.getKeyFirst());
        setKeyLast(other.getKeyLast());
        setRaftConfig(other.getRaftConfig());
        setMoving(other.isMoving());
        setKeyMoving(other.getKeyMoving());
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

    private static final class Log__Moving extends Zeze.Transaction.Logs.LogBool {
        public Log__Moving(BBucketMeta bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBucketMeta)getBelong())._Moving = value; }
    }

    private static final class Log__KeyMoving extends Zeze.Transaction.Logs.LogBinary {
        public Log__KeyMoving(BBucketMeta bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBucketMeta)getBelong())._KeyMoving = value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("RaftConfig=").append(getRaftConfig()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Moving=").append(isMoving()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("KeyMoving=").append(getKeyMoving()).append(System.lineSeparator());
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
        {
            boolean _x_ = isMoving();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            var _x_ = getKeyMoving();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.BYTES);
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
        if (_i_ == 6) {
            setMoving(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setKeyMoving(_o_.ReadBinary(_t_));
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
                case 1: _DatabaseName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _TableName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _KeyFirst = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 4: _KeyLast = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 5: _RaftConfig = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 6: _Moving = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
                case 7: _KeyMoving = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setDatabaseName(rs.getString(_parents_name_ + "DatabaseName"));
        setTableName(rs.getString(_parents_name_ + "TableName"));
        setKeyFirst(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "KeyFirst")));
        setKeyLast(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "KeyLast")));
        setRaftConfig(rs.getString(_parents_name_ + "RaftConfig"));
        setMoving(rs.getBoolean(_parents_name_ + "Moving"));
        setKeyMoving(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "KeyMoving")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "DatabaseName", getDatabaseName());
        st.appendString(_parents_name_ + "TableName", getTableName());
        st.appendBinary(_parents_name_ + "KeyFirst", getKeyFirst());
        st.appendBinary(_parents_name_ + "KeyLast", getKeyLast());
        st.appendString(_parents_name_ + "RaftConfig", getRaftConfig());
        st.appendBoolean(_parents_name_ + "Moving", isMoving());
        st.appendBinary(_parents_name_ + "KeyMoving", getKeyMoving());
    }

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 8589859502117192635L;

    private String _DatabaseName;
    private String _TableName;
    private Zeze.Net.Binary _KeyFirst;
    private Zeze.Net.Binary _KeyLast;
    private String _RaftConfig;
    private boolean _Moving; // 正在迁移中
    private Zeze.Net.Binary _KeyMoving; // 正在迁移中的key

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

    public boolean isMoving() {
        return _Moving;
    }

    public void setMoving(boolean value) {
        _Moving = value;
    }

    public Zeze.Net.Binary getKeyMoving() {
        return _KeyMoving;
    }

    public void setKeyMoving(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _KeyMoving = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _DatabaseName = "";
        _TableName = "";
        _KeyFirst = Zeze.Net.Binary.Empty;
        _KeyLast = Zeze.Net.Binary.Empty;
        _RaftConfig = "";
        _KeyMoving = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(String _DatabaseName_, String _TableName_, Zeze.Net.Binary _KeyFirst_, Zeze.Net.Binary _KeyLast_, String _RaftConfig_, boolean _Moving_, Zeze.Net.Binary _KeyMoving_) {
        if (_DatabaseName_ == null)
            throw new IllegalArgumentException();
        _DatabaseName = _DatabaseName_;
        if (_TableName_ == null)
            throw new IllegalArgumentException();
        _TableName = _TableName_;
        if (_KeyFirst_ == null)
            throw new IllegalArgumentException();
        _KeyFirst = _KeyFirst_;
        if (_KeyLast_ == null)
            throw new IllegalArgumentException();
        _KeyLast = _KeyLast_;
        if (_RaftConfig_ == null)
            throw new IllegalArgumentException();
        _RaftConfig = _RaftConfig_;
        _Moving = _Moving_;
        if (_KeyMoving_ == null)
            throw new IllegalArgumentException();
        _KeyMoving = _KeyMoving_;
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
        setDatabaseName(other.getDatabaseName());
        setTableName(other.getTableName());
        setKeyFirst(other.getKeyFirst());
        setKeyLast(other.getKeyLast());
        setRaftConfig(other.getRaftConfig());
        setMoving(other.isMoving());
        setKeyMoving(other.getKeyMoving());
    }

    public void assign(BBucketMeta.Data other) {
        setDatabaseName(other.getDatabaseName());
        setTableName(other.getTableName());
        setKeyFirst(other.getKeyFirst());
        setKeyLast(other.getKeyLast());
        setRaftConfig(other.getRaftConfig());
        setMoving(other.isMoving());
        setKeyMoving(other.getKeyMoving());
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
        sb.append(Zeze.Util.Str.indent(level)).append("RaftConfig=").append(getRaftConfig()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Moving=").append(isMoving()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("KeyMoving=").append(getKeyMoving()).append(System.lineSeparator());
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
        {
            boolean _x_ = isMoving();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            var _x_ = getKeyMoving();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.BYTES);
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
        if (_i_ == 6) {
            setMoving(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setKeyMoving(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
