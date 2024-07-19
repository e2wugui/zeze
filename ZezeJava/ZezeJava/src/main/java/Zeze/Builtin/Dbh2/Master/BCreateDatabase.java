// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BCreateDatabase extends Zeze.Transaction.Bean implements BCreateDatabaseReadOnly {
    public static final long TYPEID = -4068258744708449065L;

    private String _Database;

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
        var _d_ = new Zeze.Builtin.Dbh2.Master.BCreateDatabase.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.Master.BCreateDatabase.Data)_o_);
    }

    public void assign(BCreateDatabase.Data _o_) {
        setDatabase(_o_._Database);
        _unknown_ = null;
    }

    public void assign(BCreateDatabase _o_) {
        setDatabase(_o_.getDatabase());
        _unknown_ = _o_._unknown_;
    }

    public BCreateDatabase copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCreateDatabase copy() {
        var _c_ = new BCreateDatabase();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCreateDatabase _a_, BCreateDatabase _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Database extends Zeze.Transaction.Logs.LogString {
        public Log__Database(BCreateDatabase _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCreateDatabase)getBelong())._Database = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Dbh2.Master.BCreateDatabase: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Database=").append(getDatabase()).append(System.lineSeparator());
        _l_ -= 4;
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
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BCreateDatabase))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCreateDatabase)_o_;
        if (!getDatabase().equals(_b_.getDatabase()))
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
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setDatabase(_r_.getString(_pn_ + "Database"));
        if (getDatabase() == null)
            setDatabase("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Database", getDatabase());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Database", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4068258744708449065L;

    private String _Database;

    public String getDatabase() {
        return _Database;
    }

    public void setDatabase(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Database = _v_;
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
        var _b_ = new Zeze.Builtin.Dbh2.Master.BCreateDatabase();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BCreateDatabase)_o_);
    }

    public void assign(BCreateDatabase _o_) {
        _Database = _o_.getDatabase();
    }

    public void assign(BCreateDatabase.Data _o_) {
        _Database = _o_._Database;
    }

    @Override
    public BCreateDatabase.Data copy() {
        var _c_ = new BCreateDatabase.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCreateDatabase.Data _a_, BCreateDatabase.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
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
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Dbh2.Master.BCreateDatabase: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Database=").append(_Database).append(System.lineSeparator());
        _l_ -= 4;
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
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
