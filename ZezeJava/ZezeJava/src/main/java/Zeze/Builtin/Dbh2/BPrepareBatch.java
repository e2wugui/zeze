// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BPrepareBatch extends Zeze.Transaction.Bean implements BPrepareBatchReadOnly {
    public static final long TYPEID = 216908947802855063L;

    private String _Master; // 用来纠错
    private String _Database; // 用来纠错
    private String _Table; // 用来纠错
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Dbh2.BBatch> _Batch;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    private static final java.lang.invoke.VarHandle vh_Master;
    private static final java.lang.invoke.VarHandle vh_Database;
    private static final java.lang.invoke.VarHandle vh_Table;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Master = _l_.findVarHandle(BPrepareBatch.class, "_Master", String.class);
            vh_Database = _l_.findVarHandle(BPrepareBatch.class, "_Database", String.class);
            vh_Table = _l_.findVarHandle(BPrepareBatch.class, "_Table", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getMaster() {
        if (!isManaged())
            return _Master;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Master;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Master;
    }

    public void setMaster(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Master = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_Master, _v_));
    }

    @Override
    public String getDatabase() {
        if (!isManaged())
            return _Database;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Database;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_Database, _v_));
    }

    @Override
    public String getTable() {
        if (!isManaged())
            return _Table;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Table;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 3);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 3, vh_Table, _v_));
    }

    public Zeze.Builtin.Dbh2.BBatch getBatch() {
        return _Batch.getValue();
    }

    public void setBatch(Zeze.Builtin.Dbh2.BBatch _v_) {
        _Batch.setValue(_v_);
    }

    @Override
    public Zeze.Builtin.Dbh2.BBatchReadOnly getBatchReadOnly() {
        return _Batch.getValue();
    }

    @SuppressWarnings("deprecation")
    public BPrepareBatch() {
        _Master = "";
        _Database = "";
        _Table = "";
        _Batch = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Dbh2.BBatch(), Zeze.Builtin.Dbh2.BBatch.class);
        _Batch.variableId(4);
    }

    @SuppressWarnings("deprecation")
    public BPrepareBatch(String _Master_, String _Database_, String _Table_) {
        if (_Master_ == null)
            _Master_ = "";
        _Master = _Master_;
        if (_Database_ == null)
            _Database_ = "";
        _Database = _Database_;
        if (_Table_ == null)
            _Table_ = "";
        _Table = _Table_;
        _Batch = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Dbh2.BBatch(), Zeze.Builtin.Dbh2.BBatch.class);
        _Batch.variableId(4);
    }

    @Override
    public void reset() {
        setMaster("");
        setDatabase("");
        setTable("");
        _Batch.reset();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.BPrepareBatch.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.BPrepareBatch.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.BPrepareBatch.Data)_o_);
    }

    public void assign(BPrepareBatch.Data _o_) {
        setMaster(_o_._Master);
        setDatabase(_o_._Database);
        setTable(_o_._Table);
        var _d__Batch = new Zeze.Builtin.Dbh2.BBatch();
        _d__Batch.assign(_o_._Batch);
        _Batch.setValue(_d__Batch);
        _unknown_ = null;
    }

    public void assign(BPrepareBatch _o_) {
        setMaster(_o_.getMaster());
        setDatabase(_o_.getDatabase());
        setTable(_o_.getTable());
        _Batch.assign(_o_._Batch);
        _unknown_ = _o_._unknown_;
    }

    public BPrepareBatch copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BPrepareBatch copy() {
        var _c_ = new BPrepareBatch();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BPrepareBatch _a_, BPrepareBatch _b_) {
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
        _s_.append("Zeze.Builtin.Dbh2.BPrepareBatch: {\n");
        _s_.append(_i1_).append("Master=").append(getMaster()).append(",\n");
        _s_.append(_i1_).append("Database=").append(getDatabase()).append(",\n");
        _s_.append(_i1_).append("Table=").append(getTable()).append(",\n");
        _s_.append(_i1_).append("Batch=");
        _Batch.buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
            String _x_ = getMaster();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getDatabase();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getTable();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 4, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Batch.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
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
            setMaster(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setDatabase(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setTable(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadBean(_Batch, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BPrepareBatch))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BPrepareBatch)_o_;
        if (!getMaster().equals(_b_.getMaster()))
            return false;
        if (!getDatabase().equals(_b_.getDatabase()))
            return false;
        if (!getTable().equals(_b_.getTable()))
            return false;
        if (!_Batch.equals(_b_._Batch))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Batch.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Batch.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_Batch.negativeCheck())
            return true;
        return false;
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
                case 1: _Master = _v_.stringValue(); break;
                case 2: _Database = _v_.stringValue(); break;
                case 3: _Table = _v_.stringValue(); break;
                case 4: _Batch.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setMaster(_r_.getString(_pn_ + "Master"));
        if (getMaster() == null)
            setMaster("");
        setDatabase(_r_.getString(_pn_ + "Database"));
        if (getDatabase() == null)
            setDatabase("");
        setTable(_r_.getString(_pn_ + "Table"));
        if (getTable() == null)
            setTable("");
        _p_.add("Batch");
        _Batch.decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Master", getMaster());
        _s_.appendString(_pn_ + "Database", getDatabase());
        _s_.appendString(_pn_ + "Table", getTable());
        _p_.add("Batch");
        _Batch.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Master", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Database", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Table", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Batch", "Zeze.Builtin.Dbh2.BBatch", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 216908947802855063L;

    private String _Master; // 用来纠错
    private String _Database; // 用来纠错
    private String _Table; // 用来纠错
    private Zeze.Builtin.Dbh2.BBatch.Data _Batch;

    public String getMaster() {
        return _Master;
    }

    public void setMaster(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Master = _v_;
    }

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

    public Zeze.Builtin.Dbh2.BBatch.Data getBatch() {
        return _Batch;
    }

    public void setBatch(Zeze.Builtin.Dbh2.BBatch.Data _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Batch = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Master = "";
        _Database = "";
        _Table = "";
        _Batch = new Zeze.Builtin.Dbh2.BBatch.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(String _Master_, String _Database_, String _Table_, Zeze.Builtin.Dbh2.BBatch.Data _Batch_) {
        if (_Master_ == null)
            _Master_ = "";
        _Master = _Master_;
        if (_Database_ == null)
            _Database_ = "";
        _Database = _Database_;
        if (_Table_ == null)
            _Table_ = "";
        _Table = _Table_;
        if (_Batch_ == null)
            _Batch_ = new Zeze.Builtin.Dbh2.BBatch.Data();
        _Batch = _Batch_;
    }

    @Override
    public void reset() {
        _Master = "";
        _Database = "";
        _Table = "";
        _Batch.reset();
    }

    @Override
    public Zeze.Builtin.Dbh2.BPrepareBatch toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.BPrepareBatch();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BPrepareBatch)_o_);
    }

    public void assign(BPrepareBatch _o_) {
        _Master = _o_.getMaster();
        _Database = _o_.getDatabase();
        _Table = _o_.getTable();
        _Batch.assign(_o_._Batch.getValue());
    }

    public void assign(BPrepareBatch.Data _o_) {
        _Master = _o_._Master;
        _Database = _o_._Database;
        _Table = _o_._Table;
        _Batch.assign(_o_._Batch);
    }

    @Override
    public BPrepareBatch.Data copy() {
        var _c_ = new BPrepareBatch.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BPrepareBatch.Data _a_, BPrepareBatch.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BPrepareBatch.Data clone() {
        return (BPrepareBatch.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Dbh2.BPrepareBatch: {\n");
        _s_.append(_i1_).append("Master=").append(_Master).append(",\n");
        _s_.append(_i1_).append("Database=").append(_Database).append(",\n");
        _s_.append(_i1_).append("Table=").append(_Table).append(",\n");
        _s_.append(_i1_).append("Batch=");
        _Batch.buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
            String _x_ = _Master;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _Database;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _Table;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 4, ByteBuffer.BEAN);
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
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Master = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Database = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Table = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadBean(_Batch, _t_);
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
        if (!(_o_ instanceof BPrepareBatch.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BPrepareBatch.Data)_o_;
        if (!_Master.equals(_b_._Master))
            return false;
        if (!_Database.equals(_b_._Database))
            return false;
        if (!_Table.equals(_b_._Table))
            return false;
        if (!_Batch.equals(_b_._Batch))
            return false;
        return true;
    }
}
}
