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
    }

    @Override
    public Zeze.Builtin.Dbh2.BBucketMetaData toData() {
        var data = new Zeze.Builtin.Dbh2.BBucketMetaData();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BBucketMetaData)other);
    }

    public void assign(BBucketMetaData other) {
        setDatabaseName(other.getDatabaseName());
        setTableName(other.getTableName());
        setKeyFirst(other.getKeyFirst());
        setKeyLast(other.getKeyLast());
        setRaftConfig(other.getRaftConfig());
    }

    public void assign(BBucketMeta other) {
        setDatabaseName(other.getDatabaseName());
        setTableName(other.getTableName());
        setKeyFirst(other.getKeyFirst());
        setKeyLast(other.getKeyLast());
        setRaftConfig(other.getRaftConfig());
    }

    @Deprecated
    public void Assign(BBucketMeta other) {
        assign(other);
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

    @Deprecated
    public BBucketMeta Copy() {
        return copy();
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
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
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
                case 1: _DatabaseName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _TableName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _KeyFirst = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 4: _KeyLast = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 5: _RaftConfig = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }
}
