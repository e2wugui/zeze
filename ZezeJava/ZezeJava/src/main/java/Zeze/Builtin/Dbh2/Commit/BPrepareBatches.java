// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BPrepareBatches extends Zeze.Transaction.Bean implements BPrepareBatchesReadOnly {
    public static final long TYPEID = -2881093366329974312L;

    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Dbh2.BPrepareBatch> _Datas;
    private String _QueryIp; // 仅在独立CommitServer时设置，收到方直接使用，不再查询配置。
    private int _QueryPort;

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Dbh2.BPrepareBatch> getDatas() {
        return _Datas;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Dbh2.BPrepareBatch, Zeze.Builtin.Dbh2.BPrepareBatchReadOnly> getDatasReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Datas);
    }

    @Override
    public String getQueryIp() {
        if (!isManaged())
            return _QueryIp;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _QueryIp;
        var log = (Log__QueryIp)txn.getLog(objectId() + 2);
        return log != null ? log.value : _QueryIp;
    }

    public void setQueryIp(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _QueryIp = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__QueryIp(this, 2, value));
    }

    @Override
    public int getQueryPort() {
        if (!isManaged())
            return _QueryPort;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _QueryPort;
        var log = (Log__QueryPort)txn.getLog(objectId() + 3);
        return log != null ? log.value : _QueryPort;
    }

    public void setQueryPort(int value) {
        if (!isManaged()) {
            _QueryPort = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__QueryPort(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BPrepareBatches() {
        _Datas = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Dbh2.BPrepareBatch.class);
        _Datas.variableId(1);
        _QueryIp = "";
    }

    @SuppressWarnings("deprecation")
    public BPrepareBatches(String _QueryIp_, int _QueryPort_) {
        _Datas = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Dbh2.BPrepareBatch.class);
        _Datas.variableId(1);
        if (_QueryIp_ == null)
            throw new IllegalArgumentException();
        _QueryIp = _QueryIp_;
        _QueryPort = _QueryPort_;
    }

    @Override
    public Zeze.Builtin.Dbh2.Commit.BPrepareBatches.Data toData() {
        var data = new Zeze.Builtin.Dbh2.Commit.BPrepareBatches.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.Commit.BPrepareBatches.Data)other);
    }

    public void assign(BPrepareBatches.Data other) {
        _Datas.clear();
        for (var e : other.getDatas().entrySet()) {
            Zeze.Builtin.Dbh2.BPrepareBatch data = new Zeze.Builtin.Dbh2.BPrepareBatch();
            data.assign(e.getValue());
            _Datas.put(e.getKey(), data);
        }
        setQueryIp(other.getQueryIp());
        setQueryPort(other.getQueryPort());
    }

    public void assign(BPrepareBatches other) {
        _Datas.clear();
        for (var e : other.getDatas().entrySet())
            _Datas.put(e.getKey(), e.getValue().copy());
        setQueryIp(other.getQueryIp());
        setQueryPort(other.getQueryPort());
    }

    public BPrepareBatches copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BPrepareBatches copy() {
        var copy = new BPrepareBatches();
        copy.assign(this);
        return copy;
    }

    public static void swap(BPrepareBatches a, BPrepareBatches b) {
        BPrepareBatches save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__QueryIp extends Zeze.Transaction.Logs.LogString {
        public Log__QueryIp(BPrepareBatches bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BPrepareBatches)getBelong())._QueryIp = value; }
    }

    private static final class Log__QueryPort extends Zeze.Transaction.Logs.LogInt {
        public Log__QueryPort(BPrepareBatches bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BPrepareBatches)getBelong())._QueryPort = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Commit.BPrepareBatches: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Datas={");
        if (!_Datas.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Datas.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("QueryIp=").append(getQueryIp()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("QueryPort=").append(getQueryPort()).append(System.lineSeparator());
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
            var _x_ = _Datas;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().encode(_o_);
                }
            }
        }
        {
            String _x_ = getQueryIp();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getQueryPort();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Datas;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Dbh2.BPrepareBatch(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setQueryIp(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setQueryPort(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Datas.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Datas.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Datas.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        if (getQueryPort() < 0)
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
                case 1: _Datas.followerApply(vlog); break;
                case 2: _QueryIp = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _QueryPort = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonMap(this, "Datas", getDatas(), rs.getString(_parents_name_ + "Datas"));
        setQueryIp(rs.getString(_parents_name_ + "QueryIp"));
        if (getQueryIp() == null)
            setQueryIp("");
        setQueryPort(rs.getInt(_parents_name_ + "QueryPort"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Datas", Zeze.Serialize.Helper.encodeJson(getDatas()));
        st.appendString(_parents_name_ + "QueryIp", getQueryIp());
        st.appendInt(_parents_name_ + "QueryPort", getQueryPort());
    }

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2881093366329974312L;

    private java.util.HashMap<String, Zeze.Builtin.Dbh2.BPrepareBatch.Data> _Datas;
    private String _QueryIp; // 仅在独立CommitServer时设置，收到方直接使用，不再查询配置。
    private int _QueryPort;

    public java.util.HashMap<String, Zeze.Builtin.Dbh2.BPrepareBatch.Data> getDatas() {
        return _Datas;
    }

    public void setDatas(java.util.HashMap<String, Zeze.Builtin.Dbh2.BPrepareBatch.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Datas = value;
    }

    public String getQueryIp() {
        return _QueryIp;
    }

    public void setQueryIp(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _QueryIp = value;
    }

    public int getQueryPort() {
        return _QueryPort;
    }

    public void setQueryPort(int value) {
        _QueryPort = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Datas = new java.util.HashMap<>();
        _QueryIp = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _QueryIp_, int _QueryPort_) {
        _Datas = new java.util.HashMap<>();
        if (_QueryIp_ == null)
            throw new IllegalArgumentException();
        _QueryIp = _QueryIp_;
        _QueryPort = _QueryPort_;
    }

    @Override
    public Zeze.Builtin.Dbh2.Commit.BPrepareBatches toBean() {
        var bean = new Zeze.Builtin.Dbh2.Commit.BPrepareBatches();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BPrepareBatches)other);
    }

    public void assign(BPrepareBatches other) {
        _Datas.clear();
        for (var e : other.getDatas().entrySet()) {
            Zeze.Builtin.Dbh2.BPrepareBatch.Data data = new Zeze.Builtin.Dbh2.BPrepareBatch.Data();
            data.assign(e.getValue());
            _Datas.put(e.getKey(), data);
        }
        setQueryIp(other.getQueryIp());
        setQueryPort(other.getQueryPort());
    }

    public void assign(BPrepareBatches.Data other) {
        _Datas.clear();
        for (var e : other.getDatas().entrySet())
            _Datas.put(e.getKey(), e.getValue().copy());
        setQueryIp(other.getQueryIp());
        setQueryPort(other.getQueryPort());
    }

    @Override
    public BPrepareBatches.Data copy() {
        var copy = new BPrepareBatches.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BPrepareBatches.Data a, BPrepareBatches.Data b) {
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Commit.BPrepareBatches: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Datas={");
        if (!_Datas.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Datas.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("QueryIp=").append(getQueryIp()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("QueryPort=").append(getQueryPort()).append(System.lineSeparator());
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
            var _x_ = _Datas;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().encode(_o_);
                }
            }
        }
        {
            String _x_ = getQueryIp();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getQueryPort();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Datas;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Dbh2.BPrepareBatch.Data(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setQueryIp(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setQueryPort(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
