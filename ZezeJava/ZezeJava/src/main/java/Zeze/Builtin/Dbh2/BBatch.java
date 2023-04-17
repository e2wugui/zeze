// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BBatch extends Zeze.Transaction.Bean implements BBatchReadOnly {
    public static final long TYPEID = -2614323448581124612L;

    private final Zeze.Transaction.Collections.PMap1<Zeze.Net.Binary, Zeze.Net.Binary> _Puts;
    private final Zeze.Transaction.Collections.PSet1<Zeze.Net.Binary> _Deletes;
    private String _QueryIp;
    private int _QueryPort;

    public Zeze.Transaction.Collections.PMap1<Zeze.Net.Binary, Zeze.Net.Binary> getPuts() {
        return _Puts;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<Zeze.Net.Binary, Zeze.Net.Binary> getPutsReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_Puts);
    }

    public Zeze.Transaction.Collections.PSet1<Zeze.Net.Binary> getDeletes() {
        return _Deletes;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Zeze.Net.Binary> getDeletesReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_Deletes);
    }

    @Override
    public String getQueryIp() {
        if (!isManaged())
            return _QueryIp;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _QueryIp;
        var log = (Log__QueryIp)txn.getLog(objectId() + 3);
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
        txn.putLog(new Log__QueryIp(this, 3, value));
    }

    @Override
    public int getQueryPort() {
        if (!isManaged())
            return _QueryPort;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _QueryPort;
        var log = (Log__QueryPort)txn.getLog(objectId() + 4);
        return log != null ? log.value : _QueryPort;
    }

    public void setQueryPort(int value) {
        if (!isManaged()) {
            _QueryPort = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__QueryPort(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BBatch() {
        _Puts = new Zeze.Transaction.Collections.PMap1<>(Zeze.Net.Binary.class, Zeze.Net.Binary.class);
        _Puts.variableId(1);
        _Deletes = new Zeze.Transaction.Collections.PSet1<>(Zeze.Net.Binary.class);
        _Deletes.variableId(2);
        _QueryIp = "";
    }

    @SuppressWarnings("deprecation")
    public BBatch(String _QueryIp_, int _QueryPort_) {
        _Puts = new Zeze.Transaction.Collections.PMap1<>(Zeze.Net.Binary.class, Zeze.Net.Binary.class);
        _Puts.variableId(1);
        _Deletes = new Zeze.Transaction.Collections.PSet1<>(Zeze.Net.Binary.class);
        _Deletes.variableId(2);
        if (_QueryIp_ == null)
            throw new IllegalArgumentException();
        _QueryIp = _QueryIp_;
        _QueryPort = _QueryPort_;
    }

    @Override
    public Zeze.Builtin.Dbh2.BBatch.Data toData() {
        var data = new Zeze.Builtin.Dbh2.BBatch.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BBatch.Data)other);
    }

    public void assign(BBatch.Data other) {
        _Puts.clear();
        _Puts.putAll(other.getPuts());
        _Deletes.clear();
        _Deletes.addAll(other.getDeletes());
        setQueryIp(other.getQueryIp());
        setQueryPort(other.getQueryPort());
    }

    public void assign(BBatch other) {
        _Puts.clear();
        _Puts.putAll(other.getPuts());
        _Deletes.clear();
        _Deletes.addAll(other.getDeletes());
        setQueryIp(other.getQueryIp());
        setQueryPort(other.getQueryPort());
    }

    public BBatch copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBatch copy() {
        var copy = new BBatch();
        copy.assign(this);
        return copy;
    }

    public static void swap(BBatch a, BBatch b) {
        BBatch save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__QueryIp extends Zeze.Transaction.Logs.LogString {
        public Log__QueryIp(BBatch bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBatch)getBelong())._QueryIp = value; }
    }

    private static final class Log__QueryPort extends Zeze.Transaction.Logs.LogInt {
        public Log__QueryPort(BBatch bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBatch)getBelong())._QueryPort = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BBatch: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Puts={");
        if (!_Puts.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Puts.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_kv_.getValue()).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Deletes={");
        if (!_Deletes.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Deletes) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
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
            var _x_ = _Puts;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteBinary(_e_.getKey());
                    _o_.WriteBinary(_e_.getValue());
                }
            }
        }
        {
            var _x_ = _Deletes;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteBinary(_v_);
            }
        }
        {
            String _x_ = getQueryIp();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getQueryPort();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
            var _x_ = _Puts;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBinary(_s_);
                    var _v_ = _o_.ReadBinary(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Deletes;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBinary(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setQueryIp(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
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
        _Puts.initRootInfo(root, this);
        _Deletes.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Puts.initRootInfoWithRedo(root, this);
        _Deletes.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
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
                case 1: _Puts.followerApply(vlog); break;
                case 2: _Deletes.followerApply(vlog); break;
                case 3: _QueryIp = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _QueryPort = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonMap(this, "Puts", getPuts(), rs.getString(_parents_name_ + "Puts"));
        Zeze.Serialize.Helper.decodeJsonSet(getDeletes(), Zeze.Net.Binary.class, rs.getString(_parents_name_ + "Deletes"));
        setQueryIp(rs.getString(_parents_name_ + "QueryIp"));
        if (getQueryIp() == null)
            setQueryIp("");
        setQueryPort(rs.getInt(_parents_name_ + "QueryPort"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Puts", Zeze.Serialize.Helper.encodeJson(getPuts()));
        st.appendString(_parents_name_ + "Deletes", Zeze.Serialize.Helper.encodeJson(getDeletes()));
        st.appendString(_parents_name_ + "QueryIp", getQueryIp());
        st.appendInt(_parents_name_ + "QueryPort", getQueryPort());
    }

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2614323448581124612L;

    private java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> _Puts;
    private java.util.HashSet<Zeze.Net.Binary> _Deletes;
    private String _QueryIp;
    private int _QueryPort;

    public java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> getPuts() {
        return _Puts;
    }

    public void setPuts(java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Puts = value;
    }

    public java.util.HashSet<Zeze.Net.Binary> getDeletes() {
        return _Deletes;
    }

    public void setDeletes(java.util.HashSet<Zeze.Net.Binary> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Deletes = value;
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
        _Puts = new java.util.HashMap<>();
        _Deletes = new java.util.HashSet<>();
        _QueryIp = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _QueryIp_, int _QueryPort_) {
        _Puts = new java.util.HashMap<>();
        _Deletes = new java.util.HashSet<>();
        if (_QueryIp_ == null)
            throw new IllegalArgumentException();
        _QueryIp = _QueryIp_;
        _QueryPort = _QueryPort_;
    }

    @Override
    public Zeze.Builtin.Dbh2.BBatch toBean() {
        var bean = new Zeze.Builtin.Dbh2.BBatch();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BBatch)other);
    }

    public void assign(BBatch other) {
        _Puts.clear();
        _Puts.putAll(other.getPuts());
        _Deletes.clear();
        _Deletes.addAll(other.getDeletes());
        setQueryIp(other.getQueryIp());
        setQueryPort(other.getQueryPort());
    }

    public void assign(BBatch.Data other) {
        _Puts.clear();
        _Puts.putAll(other.getPuts());
        _Deletes.clear();
        _Deletes.addAll(other.getDeletes());
        setQueryIp(other.getQueryIp());
        setQueryPort(other.getQueryPort());
    }

    @Override
    public BBatch.Data copy() {
        var copy = new BBatch.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BBatch.Data a, BBatch.Data b) {
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BBatch: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Puts={");
        if (!_Puts.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Puts.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_kv_.getValue()).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Deletes={");
        if (!_Deletes.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Deletes) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
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
            var _x_ = _Puts;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteBinary(_e_.getKey());
                    _o_.WriteBinary(_e_.getValue());
                }
            }
        }
        {
            var _x_ = _Deletes;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteBinary(_v_);
            }
        }
        {
            String _x_ = getQueryIp();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getQueryPort();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
            var _x_ = _Puts;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBinary(_s_);
                    var _v_ = _o_.ReadBinary(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Deletes;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBinary(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setQueryIp(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
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
