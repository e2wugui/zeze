// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BDbh2Config extends Zeze.Transaction.Bean implements BDbh2ConfigReadOnly {
    public static final long TYPEID = 1761638729449220740L;

    private String _Database;
    private String _Table;
    private String _RaftConfig;

    private static final java.lang.invoke.VarHandle vh_Database;
    private static final java.lang.invoke.VarHandle vh_Table;
    private static final java.lang.invoke.VarHandle vh_RaftConfig;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Database = _l_.findVarHandle(BDbh2Config.class, "_Database", String.class);
            vh_Table = _l_.findVarHandle(BDbh2Config.class, "_Table", String.class);
            vh_RaftConfig = _l_.findVarHandle(BDbh2Config.class, "_RaftConfig", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getDatabase() {
        if (!isManaged())
            return _Database;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Database;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.stringValue() : _Database;
    }

    public void setDatabase(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Database = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_Database, _v_));
    }

    @Override
    public String getTable() {
        if (!isManaged())
            return _Table;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Table;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.stringValue() : _Table;
    }

    public void setTable(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Table = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_Table, _v_));
    }

    @Override
    public String getRaftConfig() {
        if (!isManaged())
            return _RaftConfig;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RaftConfig;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 3);
        return log != null ? log.stringValue() : _RaftConfig;
    }

    public void setRaftConfig(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _RaftConfig = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 3, vh_RaftConfig, _v_));
    }

    @SuppressWarnings("deprecation")
    public BDbh2Config() {
        _Database = "";
        _Table = "";
        _RaftConfig = "";
    }

    @SuppressWarnings("deprecation")
    public BDbh2Config(String _Database_, String _Table_, String _RaftConfig_) {
        if (_Database_ == null)
            _Database_ = "";
        _Database = _Database_;
        if (_Table_ == null)
            _Table_ = "";
        _Table = _Table_;
        if (_RaftConfig_ == null)
            _RaftConfig_ = "";
        _RaftConfig = _RaftConfig_;
    }

    @Override
    public void reset() {
        setDatabase("");
        setTable("");
        setRaftConfig("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BDbh2Config.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.Master.BDbh2Config.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.Master.BDbh2Config.Data)_o_);
    }

    public void assign(BDbh2Config.Data _o_) {
        setDatabase(_o_._Database);
        setTable(_o_._Table);
        setRaftConfig(_o_._RaftConfig);
        _unknown_ = null;
    }

    public void assign(BDbh2Config _o_) {
        setDatabase(_o_.getDatabase());
        setTable(_o_.getTable());
        setRaftConfig(_o_.getRaftConfig());
        _unknown_ = _o_._unknown_;
    }

    public BDbh2Config copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDbh2Config copy() {
        var _c_ = new BDbh2Config();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BDbh2Config _a_, BDbh2Config _b_) {
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
        _s_.append("Zeze.Builtin.Dbh2.Master.BDbh2Config: {\n");
        _s_.append(_i1_).append("Database=").append(getDatabase()).append(",\n");
        _s_.append(_i1_).append("Table=").append(getTable()).append(",\n");
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
            String _x_ = getRaftConfig();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            setDatabase(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTable(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
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
        if (!(_o_ instanceof BDbh2Config))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BDbh2Config)_o_;
        if (!getDatabase().equals(_b_.getDatabase()))
            return false;
        if (!getTable().equals(_b_.getTable()))
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
                case 1: _Database = _v_.stringValue(); break;
                case 2: _Table = _v_.stringValue(); break;
                case 3: _RaftConfig = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setDatabase(_r_.getString(_pn_ + "Database"));
        if (getDatabase() == null)
            setDatabase("");
        setTable(_r_.getString(_pn_ + "Table"));
        if (getTable() == null)
            setTable("");
        setRaftConfig(_r_.getString(_pn_ + "RaftConfig"));
        if (getRaftConfig() == null)
            setRaftConfig("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Database", getDatabase());
        _s_.appendString(_pn_ + "Table", getTable());
        _s_.appendString(_pn_ + "RaftConfig", getRaftConfig());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Database", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Table", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "RaftConfig", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 1761638729449220740L;

    private String _Database;
    private String _Table;
    private String _RaftConfig;

    public String getDatabase() {
        return _Database;
    }

    public void setDatabase(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Database = _v_;
    }

    public String getTable() {
        return _Table;
    }

    public void setTable(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Table = _v_;
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
        _Database = "";
        _Table = "";
        _RaftConfig = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _Database_, String _Table_, String _RaftConfig_) {
        if (_Database_ == null)
            _Database_ = "";
        _Database = _Database_;
        if (_Table_ == null)
            _Table_ = "";
        _Table = _Table_;
        if (_RaftConfig_ == null)
            _RaftConfig_ = "";
        _RaftConfig = _RaftConfig_;
    }

    @Override
    public void reset() {
        _Database = "";
        _Table = "";
        _RaftConfig = "";
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BDbh2Config toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.Master.BDbh2Config();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BDbh2Config)_o_);
    }

    public void assign(BDbh2Config _o_) {
        _Database = _o_.getDatabase();
        _Table = _o_.getTable();
        _RaftConfig = _o_.getRaftConfig();
    }

    public void assign(BDbh2Config.Data _o_) {
        _Database = _o_._Database;
        _Table = _o_._Table;
        _RaftConfig = _o_._RaftConfig;
    }

    @Override
    public BDbh2Config.Data copy() {
        var _c_ = new BDbh2Config.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BDbh2Config.Data _a_, BDbh2Config.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BDbh2Config.Data clone() {
        return (BDbh2Config.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Dbh2.Master.BDbh2Config: {\n");
        _s_.append(_i1_).append("Database=").append(_Database).append(",\n");
        _s_.append(_i1_).append("Table=").append(_Table).append(",\n");
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
            String _x_ = _RaftConfig;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            _Database = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Table = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _RaftConfig = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BDbh2Config.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BDbh2Config.Data)_o_;
        if (!_Database.equals(_b_._Database))
            return false;
        if (!_Table.equals(_b_._Table))
            return false;
        if (!_RaftConfig.equals(_b_._RaftConfig))
            return false;
        return true;
    }
}
}
