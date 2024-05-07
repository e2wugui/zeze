// auto-generated @formatter:off
package Zeze.Builtin.HistoryModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BLogChanges extends Zeze.Transaction.Bean implements BLogChangesReadOnly {
    public static final long TYPEID = 395935719895809559L;

    private long _GlobalSerialId;
    private String _ProtocolClassName;
    private Zeze.Net.Binary _ProtocolArgument;
    private final Zeze.Transaction.Collections.PMap1<Zeze.Builtin.HistoryModule.BTableKey, Zeze.Net.Binary> _Changes;
    private long _Timestamp;

    @Override
    public long getGlobalSerialId() {
        if (!isManaged())
            return _GlobalSerialId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _GlobalSerialId;
        var log = (Log__GlobalSerialId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _GlobalSerialId;
    }

    public void setGlobalSerialId(long value) {
        if (!isManaged()) {
            _GlobalSerialId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__GlobalSerialId(this, 1, value));
    }

    @Override
    public String getProtocolClassName() {
        if (!isManaged())
            return _ProtocolClassName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ProtocolClassName;
        var log = (Log__ProtocolClassName)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ProtocolClassName;
    }

    public void setProtocolClassName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ProtocolClassName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ProtocolClassName(this, 2, value));
    }

    @Override
    public Zeze.Net.Binary getProtocolArgument() {
        if (!isManaged())
            return _ProtocolArgument;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ProtocolArgument;
        var log = (Log__ProtocolArgument)txn.getLog(objectId() + 3);
        return log != null ? log.value : _ProtocolArgument;
    }

    public void setProtocolArgument(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ProtocolArgument = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ProtocolArgument(this, 3, value));
    }

    public Zeze.Transaction.Collections.PMap1<Zeze.Builtin.HistoryModule.BTableKey, Zeze.Net.Binary> getChanges() {
        return _Changes;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<Zeze.Builtin.HistoryModule.BTableKey, Zeze.Net.Binary> getChangesReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_Changes);
    }

    @Override
    public long getTimestamp() {
        if (!isManaged())
            return _Timestamp;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Timestamp;
        var log = (Log__Timestamp)txn.getLog(objectId() + 5);
        return log != null ? log.value : _Timestamp;
    }

    public void setTimestamp(long value) {
        if (!isManaged()) {
            _Timestamp = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Timestamp(this, 5, value));
    }

    @SuppressWarnings("deprecation")
    public BLogChanges() {
        _ProtocolClassName = "";
        _ProtocolArgument = Zeze.Net.Binary.Empty;
        _Changes = new Zeze.Transaction.Collections.PMap1<>(Zeze.Builtin.HistoryModule.BTableKey.class, Zeze.Net.Binary.class);
        _Changes.variableId(4);
    }

    @SuppressWarnings("deprecation")
    public BLogChanges(long _GlobalSerialId_, String _ProtocolClassName_, Zeze.Net.Binary _ProtocolArgument_, long _Timestamp_) {
        _GlobalSerialId = _GlobalSerialId_;
        if (_ProtocolClassName_ == null)
            _ProtocolClassName_ = "";
        _ProtocolClassName = _ProtocolClassName_;
        if (_ProtocolArgument_ == null)
            _ProtocolArgument_ = Zeze.Net.Binary.Empty;
        _ProtocolArgument = _ProtocolArgument_;
        _Changes = new Zeze.Transaction.Collections.PMap1<>(Zeze.Builtin.HistoryModule.BTableKey.class, Zeze.Net.Binary.class);
        _Changes.variableId(4);
        _Timestamp = _Timestamp_;
    }

    @Override
    public void reset() {
        setGlobalSerialId(0);
        setProtocolClassName("");
        setProtocolArgument(Zeze.Net.Binary.Empty);
        _Changes.clear();
        setTimestamp(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.HistoryModule.BLogChanges.Data toData() {
        var data = new Zeze.Builtin.HistoryModule.BLogChanges.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.HistoryModule.BLogChanges.Data)other);
    }

    public void assign(BLogChanges.Data other) {
        setGlobalSerialId(other._GlobalSerialId);
        setProtocolClassName(other._ProtocolClassName);
        setProtocolArgument(other._ProtocolArgument);
        _Changes.clear();
        _Changes.putAll(other._Changes);
        setTimestamp(other._Timestamp);
        _unknown_ = null;
    }

    public void assign(BLogChanges other) {
        setGlobalSerialId(other.getGlobalSerialId());
        setProtocolClassName(other.getProtocolClassName());
        setProtocolArgument(other.getProtocolArgument());
        _Changes.clear();
        _Changes.putAll(other._Changes);
        setTimestamp(other.getTimestamp());
        _unknown_ = other._unknown_;
    }

    public BLogChanges copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLogChanges copy() {
        var copy = new BLogChanges();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLogChanges a, BLogChanges b) {
        BLogChanges save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__GlobalSerialId extends Zeze.Transaction.Logs.LogLong {
        public Log__GlobalSerialId(BLogChanges bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLogChanges)getBelong())._GlobalSerialId = value; }
    }

    private static final class Log__ProtocolClassName extends Zeze.Transaction.Logs.LogString {
        public Log__ProtocolClassName(BLogChanges bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLogChanges)getBelong())._ProtocolClassName = value; }
    }

    private static final class Log__ProtocolArgument extends Zeze.Transaction.Logs.LogBinary {
        public Log__ProtocolArgument(BLogChanges bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLogChanges)getBelong())._ProtocolArgument = value; }
    }

    private static final class Log__Timestamp extends Zeze.Transaction.Logs.LogLong {
        public Log__Timestamp(BLogChanges bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLogChanges)getBelong())._Timestamp = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HistoryModule.BLogChanges: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("GlobalSerialId=").append(getGlobalSerialId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProtocolClassName=").append(getProtocolClassName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProtocolArgument=").append(getProtocolArgument()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Changes={");
        if (!_Changes.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Changes.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(System.lineSeparator());
                _kv_.getKey().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_kv_.getValue()).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Timestamp=").append(getTimestamp()).append(System.lineSeparator());
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
            long _x_ = getGlobalSerialId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getProtocolClassName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getProtocolArgument();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _Changes;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BEAN, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _e_.getKey().encode(_o_);
                    _o_.WriteBinary(_e_.getValue());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            long _x_ = getTimestamp();
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
            setGlobalSerialId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setProtocolClassName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setProtocolArgument(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = _Changes;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBean(new Zeze.Builtin.HistoryModule.BTableKey(), _s_);
                    var _v_ = _o_.ReadBinary(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setTimestamp(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Changes.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Changes.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getGlobalSerialId() < 0)
            return true;
        if (getTimestamp() < 0)
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
                case 1: _GlobalSerialId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _ProtocolClassName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _ProtocolArgument = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 4: _Changes.followerApply(vlog); break;
                case 5: _Timestamp = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setGlobalSerialId(rs.getLong(_parents_name_ + "GlobalSerialId"));
        setProtocolClassName(rs.getString(_parents_name_ + "ProtocolClassName"));
        if (getProtocolClassName() == null)
            setProtocolClassName("");
        setProtocolArgument(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "ProtocolArgument")));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Changes", _Changes, rs.getString(_parents_name_ + "Changes"));
        setTimestamp(rs.getLong(_parents_name_ + "Timestamp"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "GlobalSerialId", getGlobalSerialId());
        st.appendString(_parents_name_ + "ProtocolClassName", getProtocolClassName());
        st.appendBinary(_parents_name_ + "ProtocolArgument", getProtocolArgument());
        st.appendString(_parents_name_ + "Changes", Zeze.Serialize.Helper.encodeJson(_Changes));
        st.appendLong(_parents_name_ + "Timestamp", getTimestamp());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "GlobalSerialId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ProtocolClassName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ProtocolArgument", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Changes", "map", "Zeze.Builtin.HistoryModule.BTableKey", "binary"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Timestamp", "long", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 395935719895809559L;

    private long _GlobalSerialId;
    private String _ProtocolClassName;
    private Zeze.Net.Binary _ProtocolArgument;
    private java.util.HashMap<Zeze.Builtin.HistoryModule.BTableKey, Zeze.Net.Binary> _Changes;
    private long _Timestamp;

    public long getGlobalSerialId() {
        return _GlobalSerialId;
    }

    public void setGlobalSerialId(long value) {
        _GlobalSerialId = value;
    }

    public String getProtocolClassName() {
        return _ProtocolClassName;
    }

    public void setProtocolClassName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ProtocolClassName = value;
    }

    public Zeze.Net.Binary getProtocolArgument() {
        return _ProtocolArgument;
    }

    public void setProtocolArgument(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ProtocolArgument = value;
    }

    public java.util.HashMap<Zeze.Builtin.HistoryModule.BTableKey, Zeze.Net.Binary> getChanges() {
        return _Changes;
    }

    public void setChanges(java.util.HashMap<Zeze.Builtin.HistoryModule.BTableKey, Zeze.Net.Binary> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Changes = value;
    }

    public long getTimestamp() {
        return _Timestamp;
    }

    public void setTimestamp(long value) {
        _Timestamp = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ProtocolClassName = "";
        _ProtocolArgument = Zeze.Net.Binary.Empty;
        _Changes = new java.util.HashMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(long _GlobalSerialId_, String _ProtocolClassName_, Zeze.Net.Binary _ProtocolArgument_, java.util.HashMap<Zeze.Builtin.HistoryModule.BTableKey, Zeze.Net.Binary> _Changes_, long _Timestamp_) {
        _GlobalSerialId = _GlobalSerialId_;
        if (_ProtocolClassName_ == null)
            _ProtocolClassName_ = "";
        _ProtocolClassName = _ProtocolClassName_;
        if (_ProtocolArgument_ == null)
            _ProtocolArgument_ = Zeze.Net.Binary.Empty;
        _ProtocolArgument = _ProtocolArgument_;
        if (_Changes_ == null)
            _Changes_ = new java.util.HashMap<>();
        _Changes = _Changes_;
        _Timestamp = _Timestamp_;
    }

    @Override
    public void reset() {
        _GlobalSerialId = 0;
        _ProtocolClassName = "";
        _ProtocolArgument = Zeze.Net.Binary.Empty;
        _Changes.clear();
        _Timestamp = 0;
    }

    @Override
    public Zeze.Builtin.HistoryModule.BLogChanges toBean() {
        var bean = new Zeze.Builtin.HistoryModule.BLogChanges();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BLogChanges)other);
    }

    public void assign(BLogChanges other) {
        _GlobalSerialId = other.getGlobalSerialId();
        _ProtocolClassName = other.getProtocolClassName();
        _ProtocolArgument = other.getProtocolArgument();
        _Changes.clear();
        _Changes.putAll(other._Changes);
        _Timestamp = other.getTimestamp();
    }

    public void assign(BLogChanges.Data other) {
        _GlobalSerialId = other._GlobalSerialId;
        _ProtocolClassName = other._ProtocolClassName;
        _ProtocolArgument = other._ProtocolArgument;
        _Changes.clear();
        _Changes.putAll(other._Changes);
        _Timestamp = other._Timestamp;
    }

    @Override
    public BLogChanges.Data copy() {
        var copy = new BLogChanges.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLogChanges.Data a, BLogChanges.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLogChanges.Data clone() {
        return (BLogChanges.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HistoryModule.BLogChanges: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("GlobalSerialId=").append(_GlobalSerialId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProtocolClassName=").append(_ProtocolClassName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProtocolArgument=").append(_ProtocolArgument).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Changes={");
        if (!_Changes.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Changes.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(System.lineSeparator());
                _kv_.getKey().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_kv_.getValue()).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Timestamp=").append(_Timestamp).append(System.lineSeparator());
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
            long _x_ = _GlobalSerialId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = _ProtocolClassName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _ProtocolArgument;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _Changes;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BEAN, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _e_.getKey().encode(_o_);
                    _o_.WriteBinary(_e_.getValue());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            long _x_ = _Timestamp;
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
            _GlobalSerialId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ProtocolClassName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _ProtocolArgument = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = _Changes;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBean(new Zeze.Builtin.HistoryModule.BTableKey(), _s_);
                    var _v_ = _o_.ReadBinary(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _Timestamp = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
