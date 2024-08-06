// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BGetArgument extends Zeze.Transaction.Bean implements BGetArgumentReadOnly {
    public static final long TYPEID = 4922212073054736979L;

    private String _Database; // 用来纠错
    private String _Table; // 用来纠错
    private Zeze.Net.Binary _Key;

    @Override
    public String getDatabase() {
        if (!isManaged())
            return _Database;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Database;
        var log = (Log__Database)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Database;
    }

    public void setDatabase(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Database = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Database(this, 1, _v_));
    }

    @Override
    public String getTable() {
        if (!isManaged())
            return _Table;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Table;
        var log = (Log__Table)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Table;
    }

    public void setTable(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Table = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Table(this, 2, _v_));
    }

    @Override
    public Zeze.Net.Binary getKey() {
        if (!isManaged())
            return _Key;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Key;
        var log = (Log__Key)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Key;
    }

    public void setKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Key = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Key(this, 3, _v_));
    }

    @SuppressWarnings("deprecation")
    public BGetArgument() {
        _Database = "";
        _Table = "";
        _Key = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BGetArgument(String _Database_, String _Table_, Zeze.Net.Binary _Key_) {
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
    public Zeze.Builtin.Dbh2.BGetArgument.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.BGetArgument.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.BGetArgument.Data)_o_);
    }

    public void assign(BGetArgument.Data _o_) {
        setDatabase(_o_._Database);
        setTable(_o_._Table);
        setKey(_o_._Key);
        _unknown_ = null;
    }

    public void assign(BGetArgument _o_) {
        setDatabase(_o_.getDatabase());
        setTable(_o_.getTable());
        setKey(_o_.getKey());
        _unknown_ = _o_._unknown_;
    }

    public BGetArgument copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BGetArgument copy() {
        var _c_ = new BGetArgument();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BGetArgument _a_, BGetArgument _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Database extends Zeze.Transaction.Logs.LogString {
        public Log__Database(BGetArgument _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BGetArgument)getBelong())._Database = value; }
    }

    private static final class Log__Table extends Zeze.Transaction.Logs.LogString {
        public Log__Table(BGetArgument _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BGetArgument)getBelong())._Table = value; }
    }

    private static final class Log__Key extends Zeze.Transaction.Logs.LogBinary {
        public Log__Key(BGetArgument _b_, int _i_, Zeze.Net.Binary _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BGetArgument)getBelong())._Key = value; }
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
        _s_.append("Zeze.Builtin.Dbh2.BGetArgument: {\n");
        _s_.append(_i1_).append("Database=").append(getDatabase()).append(",\n");
        _s_.append(_i1_).append("Table=").append(getTable()).append(",\n");
        _s_.append(_i1_).append("Key=").append(getKey()).append('\n');
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
            setKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BGetArgument))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BGetArgument)_o_;
        if (!getDatabase().equals(_b_.getDatabase()))
            return false;
        if (!getTable().equals(_b_.getTable()))
            return false;
        if (!getKey().equals(_b_.getKey()))
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
                case 3: _Key = _v_.binaryValue(); break;
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
        setKey(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Key")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Database", getDatabase());
        _s_.appendString(_pn_ + "Table", getTable());
        _s_.appendBinary(_pn_ + "Key", getKey());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Database", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Table", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Key", "binary", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4922212073054736979L;

    private String _Database; // 用来纠错
    private String _Table; // 用来纠错
    private Zeze.Net.Binary _Key;

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

    public Zeze.Net.Binary getKey() {
        return _Key;
    }

    public void setKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Key = _v_;
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
    public Zeze.Builtin.Dbh2.BGetArgument toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.BGetArgument();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BGetArgument)_o_);
    }

    public void assign(BGetArgument _o_) {
        _Database = _o_.getDatabase();
        _Table = _o_.getTable();
        _Key = _o_.getKey();
    }

    public void assign(BGetArgument.Data _o_) {
        _Database = _o_._Database;
        _Table = _o_._Table;
        _Key = _o_._Key;
    }

    @Override
    public BGetArgument.Data copy() {
        var _c_ = new BGetArgument.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BGetArgument.Data _a_, BGetArgument.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BGetArgument.Data clone() {
        return (BGetArgument.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Dbh2.BGetArgument: {\n");
        _s_.append(_i1_).append("Database=").append(_Database).append(",\n");
        _s_.append(_i1_).append("Table=").append(_Table).append(",\n");
        _s_.append(_i1_).append("Key=").append(_Key).append('\n');
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
            var _x_ = _Key;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
