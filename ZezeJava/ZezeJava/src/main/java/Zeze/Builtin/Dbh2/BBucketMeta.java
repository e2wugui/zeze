// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BBucketMeta extends Zeze.Transaction.Bean implements BBucketMetaReadOnly {
    public static final long TYPEID = 8589859502117192635L;

    private String _DatabaseName;
    private String _TableName;
    private Zeze.Net.Binary _KeyFirst;
    private Zeze.Net.Binary _KeyLast;
    private String _RaftConfig;

    private static final java.lang.invoke.VarHandle vh_DatabaseName;
    private static final java.lang.invoke.VarHandle vh_TableName;
    private static final java.lang.invoke.VarHandle vh_KeyFirst;
    private static final java.lang.invoke.VarHandle vh_KeyLast;
    private static final java.lang.invoke.VarHandle vh_RaftConfig;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_DatabaseName = _l_.findVarHandle(BBucketMeta.class, "_DatabaseName", String.class);
            vh_TableName = _l_.findVarHandle(BBucketMeta.class, "_TableName", String.class);
            vh_KeyFirst = _l_.findVarHandle(BBucketMeta.class, "_KeyFirst", Zeze.Net.Binary.class);
            vh_KeyLast = _l_.findVarHandle(BBucketMeta.class, "_KeyLast", Zeze.Net.Binary.class);
            vh_RaftConfig = _l_.findVarHandle(BBucketMeta.class, "_RaftConfig", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getDatabaseName() {
        if (!isManaged())
            return _DatabaseName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _DatabaseName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _DatabaseName;
    }

    public void setDatabaseName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _DatabaseName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_DatabaseName, _v_));
    }

    @Override
    public String getTableName() {
        if (!isManaged())
            return _TableName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TableName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _TableName;
    }

    public void setTableName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TableName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_TableName, _v_));
    }

    @Override
    public Zeze.Net.Binary getKeyFirst() {
        if (!isManaged())
            return _KeyFirst;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _KeyFirst;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _KeyFirst;
    }

    public void setKeyFirst(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _KeyFirst = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 3, vh_KeyFirst, _v_));
    }

    @Override
    public Zeze.Net.Binary getKeyLast() {
        if (!isManaged())
            return _KeyLast;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _KeyLast;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _KeyLast;
    }

    public void setKeyLast(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _KeyLast = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 4, vh_KeyLast, _v_));
    }

    @Override
    public String getRaftConfig() {
        if (!isManaged())
            return _RaftConfig;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RaftConfig;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _RaftConfig;
    }

    public void setRaftConfig(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _RaftConfig = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 5, vh_RaftConfig, _v_));
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
        var _d_ = new Zeze.Builtin.Dbh2.BBucketMeta.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.BBucketMeta.Data)_o_);
    }

    public void assign(BBucketMeta.Data _o_) {
        setDatabaseName(_o_._DatabaseName);
        setTableName(_o_._TableName);
        setKeyFirst(_o_._KeyFirst);
        setKeyLast(_o_._KeyLast);
        setRaftConfig(_o_._RaftConfig);
        _unknown_ = null;
    }

    public void assign(BBucketMeta _o_) {
        setDatabaseName(_o_.getDatabaseName());
        setTableName(_o_.getTableName());
        setKeyFirst(_o_.getKeyFirst());
        setKeyLast(_o_.getKeyLast());
        setRaftConfig(_o_.getRaftConfig());
        _unknown_ = _o_._unknown_;
    }

    public BBucketMeta copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBucketMeta copy() {
        var _c_ = new BBucketMeta();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBucketMeta _a_, BBucketMeta _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.Dbh2.BBucketMeta: {\n");
        _s_.append(_i1_).append("DatabaseName=").append(getDatabaseName()).append(",\n");
        _s_.append(_i1_).append("TableName=").append(getTableName()).append(",\n");
        _s_.append(_i1_).append("KeyFirst=").append(getKeyFirst()).append(",\n");
        _s_.append(_i1_).append("KeyLast=").append(getKeyLast()).append(",\n");
        _s_.append(_i1_).append("RaftConfig=").append(getRaftConfig()).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
    public void decode(IByteBuffer _o_) {
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

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BBucketMeta))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBucketMeta)_o_;
        if (!getDatabaseName().equals(_b_.getDatabaseName()))
            return false;
        if (!getTableName().equals(_b_.getTableName()))
            return false;
        if (!getKeyFirst().equals(_b_.getKeyFirst()))
            return false;
        if (!getKeyLast().equals(_b_.getKeyLast()))
            return false;
        if (!getRaftConfig().equals(_b_.getRaftConfig()))
            return false;
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _DatabaseName = _v_.stringValue(); break;
                case 2: _TableName = _v_.stringValue(); break;
                case 3: _KeyFirst = _v_.binaryValue(); break;
                case 4: _KeyLast = _v_.binaryValue(); break;
                case 5: _RaftConfig = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setDatabaseName(_r_.getString(_pn_ + "DatabaseName"));
        if (getDatabaseName() == null)
            setDatabaseName("");
        setTableName(_r_.getString(_pn_ + "TableName"));
        if (getTableName() == null)
            setTableName("");
        setKeyFirst(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "KeyFirst")));
        setKeyLast(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "KeyLast")));
        setRaftConfig(_r_.getString(_pn_ + "RaftConfig"));
        if (getRaftConfig() == null)
            setRaftConfig("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "DatabaseName", getDatabaseName());
        _s_.appendString(_pn_ + "TableName", getTableName());
        _s_.appendBinary(_pn_ + "KeyFirst", getKeyFirst());
        _s_.appendBinary(_pn_ + "KeyLast", getKeyLast());
        _s_.appendString(_pn_ + "RaftConfig", getRaftConfig());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "DatabaseName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "TableName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "KeyFirst", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "KeyLast", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "RaftConfig", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
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

    public void setDatabaseName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _DatabaseName = _v_;
    }

    public String getTableName() {
        return _TableName;
    }

    public void setTableName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _TableName = _v_;
    }

    public Zeze.Net.Binary getKeyFirst() {
        return _KeyFirst;
    }

    public void setKeyFirst(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _KeyFirst = _v_;
    }

    public Zeze.Net.Binary getKeyLast() {
        return _KeyLast;
    }

    public void setKeyLast(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _KeyLast = _v_;
    }

    public String getRaftConfig() {
        return _RaftConfig;
    }

    public void setRaftConfig(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _RaftConfig = _v_;
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
        var _b_ = new Zeze.Builtin.Dbh2.BBucketMeta();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BBucketMeta)_o_);
    }

    public void assign(BBucketMeta _o_) {
        _DatabaseName = _o_.getDatabaseName();
        _TableName = _o_.getTableName();
        _KeyFirst = _o_.getKeyFirst();
        _KeyLast = _o_.getKeyLast();
        _RaftConfig = _o_.getRaftConfig();
    }

    public void assign(BBucketMeta.Data _o_) {
        _DatabaseName = _o_._DatabaseName;
        _TableName = _o_._TableName;
        _KeyFirst = _o_._KeyFirst;
        _KeyLast = _o_._KeyLast;
        _RaftConfig = _o_._RaftConfig;
    }

    @Override
    public BBucketMeta.Data copy() {
        var _c_ = new BBucketMeta.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBucketMeta.Data _a_, BBucketMeta.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
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
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.Dbh2.BBucketMeta: {\n");
        _s_.append(_i1_).append("DatabaseName=").append(_DatabaseName).append(",\n");
        _s_.append(_i1_).append("TableName=").append(_TableName).append(",\n");
        _s_.append(_i1_).append("KeyFirst=").append(_KeyFirst).append(",\n");
        _s_.append(_i1_).append("KeyLast=").append(_KeyLast).append(",\n");
        _s_.append(_i1_).append("RaftConfig=").append(_RaftConfig).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
    public void decode(IByteBuffer _o_) {
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
