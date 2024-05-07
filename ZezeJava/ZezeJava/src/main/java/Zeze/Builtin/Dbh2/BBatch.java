// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BBatch extends Zeze.Transaction.Bean implements BBatchReadOnly {
    public static final long TYPEID = -2614323448581124612L;

    private final Zeze.Transaction.Collections.PMap1<Zeze.Net.Binary, Zeze.Net.Binary> _Puts;
    private final Zeze.Transaction.Collections.PSet1<Zeze.Net.Binary> _Deletes;
    private String _QueryIp;
    private int _QueryPort;
    private long _Tid;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

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

    @Override
    public long getTid() {
        if (!isManaged())
            return _Tid;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Tid;
        var log = (Log__Tid)txn.getLog(objectId() + 5);
        return log != null ? log.value : _Tid;
    }

    public void setTid(long value) {
        if (!isManaged()) {
            _Tid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Tid(this, 5, value));
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
    public BBatch(String _QueryIp_, int _QueryPort_, long _Tid_) {
        _Puts = new Zeze.Transaction.Collections.PMap1<>(Zeze.Net.Binary.class, Zeze.Net.Binary.class);
        _Puts.variableId(1);
        _Deletes = new Zeze.Transaction.Collections.PSet1<>(Zeze.Net.Binary.class);
        _Deletes.variableId(2);
        if (_QueryIp_ == null)
            _QueryIp_ = "";
        _QueryIp = _QueryIp_;
        _QueryPort = _QueryPort_;
        _Tid = _Tid_;
    }

    @Override
    public void reset() {
        _Puts.clear();
        _Deletes.clear();
        setQueryIp("");
        setQueryPort(0);
        setTid(0);
        _unknown_ = null;
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
        _Puts.putAll(other._Puts);
        _Deletes.clear();
        _Deletes.addAll(other._Deletes);
        setQueryIp(other._QueryIp);
        setQueryPort(other._QueryPort);
        setTid(other._Tid);
        _unknown_ = null;
    }

    public void assign(BBatch other) {
        _Puts.clear();
        _Puts.putAll(other._Puts);
        _Deletes.clear();
        _Deletes.addAll(other._Deletes);
        setQueryIp(other.getQueryIp());
        setQueryPort(other.getQueryPort());
        setTid(other.getTid());
        _unknown_ = other._unknown_;
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

    private static final class Log__Tid extends Zeze.Transaction.Logs.LogLong {
        public Log__Tid(BBatch bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBatch)getBelong())._Tid = value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("QueryPort=").append(getQueryPort()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Tid=").append(getTid()).append(System.lineSeparator());
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
            var _x_ = _Puts;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteBinary(_e_.getKey());
                    _o_.WriteBinary(_e_.getValue());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _Deletes;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteBinary(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
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
        {
            long _x_ = getTid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
        if (_i_ == 5) {
            setTid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BBatch))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBatch)_o_;
        if (!_Puts.equals(_b_._Puts))
            return false;
        if (!_Deletes.equals(_b_._Deletes))
            return false;
        if (!getQueryIp().equals(_b_.getQueryIp()))
            return false;
        if (getQueryPort() != _b_.getQueryPort())
            return false;
        if (getTid() != _b_.getTid())
            return false;
        return true;
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
        if (getTid() < 0)
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
                case 5: _Tid = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonMap(this, "Puts", _Puts, rs.getString(_parents_name_ + "Puts"));
        Zeze.Serialize.Helper.decodeJsonSet(_Deletes, Zeze.Net.Binary.class, rs.getString(_parents_name_ + "Deletes"));
        setQueryIp(rs.getString(_parents_name_ + "QueryIp"));
        if (getQueryIp() == null)
            setQueryIp("");
        setQueryPort(rs.getInt(_parents_name_ + "QueryPort"));
        setTid(rs.getLong(_parents_name_ + "Tid"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Puts", Zeze.Serialize.Helper.encodeJson(_Puts));
        st.appendString(_parents_name_ + "Deletes", Zeze.Serialize.Helper.encodeJson(_Deletes));
        st.appendString(_parents_name_ + "QueryIp", getQueryIp());
        st.appendInt(_parents_name_ + "QueryPort", getQueryPort());
        st.appendLong(_parents_name_ + "Tid", getTid());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Puts", "map", "binary", "binary"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Deletes", "set", "", "binary"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "QueryIp", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "QueryPort", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Tid", "long", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2614323448581124612L;

    private java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> _Puts;
    private java.util.HashSet<Zeze.Net.Binary> _Deletes;
    private String _QueryIp;
    private int _QueryPort;
    private long _Tid;

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

    public long getTid() {
        return _Tid;
    }

    public void setTid(long value) {
        _Tid = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Puts = new java.util.HashMap<>();
        _Deletes = new java.util.HashSet<>();
        _QueryIp = "";
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> _Puts_, java.util.HashSet<Zeze.Net.Binary> _Deletes_, String _QueryIp_, int _QueryPort_, long _Tid_) {
        if (_Puts_ == null)
            _Puts_ = new java.util.HashMap<>();
        _Puts = _Puts_;
        if (_Deletes_ == null)
            _Deletes_ = new java.util.HashSet<>();
        _Deletes = _Deletes_;
        if (_QueryIp_ == null)
            _QueryIp_ = "";
        _QueryIp = _QueryIp_;
        _QueryPort = _QueryPort_;
        _Tid = _Tid_;
    }

    @Override
    public void reset() {
        _Puts.clear();
        _Deletes.clear();
        _QueryIp = "";
        _QueryPort = 0;
        _Tid = 0;
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
        _Puts.putAll(other._Puts);
        _Deletes.clear();
        _Deletes.addAll(other._Deletes);
        _QueryIp = other.getQueryIp();
        _QueryPort = other.getQueryPort();
        _Tid = other.getTid();
    }

    public void assign(BBatch.Data other) {
        _Puts.clear();
        _Puts.putAll(other._Puts);
        _Deletes.clear();
        _Deletes.addAll(other._Deletes);
        _QueryIp = other._QueryIp;
        _QueryPort = other._QueryPort;
        _Tid = other._Tid;
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
    public BBatch.Data clone() {
        return (BBatch.Data)super.clone();
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
        sb.append(Zeze.Util.Str.indent(level)).append("QueryIp=").append(_QueryIp).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("QueryPort=").append(_QueryPort).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Tid=").append(_Tid).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

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
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _Deletes;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteBinary(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            String _x_ = _QueryIp;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = _QueryPort;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = _Tid;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
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
            _QueryIp = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _QueryPort = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _Tid = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
