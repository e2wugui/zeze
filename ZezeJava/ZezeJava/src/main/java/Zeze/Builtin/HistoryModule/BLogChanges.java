// auto-generated @formatter:off
package Zeze.Builtin.HistoryModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public class BLogChanges extends Zeze.Transaction.Bean implements BLogChangesReadOnly {
    public static final long TYPEID = 395935719895809559L;

    private Zeze.Util.Id128 _GlobalSerialId;
    private String _ProtocolClassName;
    private Zeze.Net.Binary _ProtocolArgument;
    private final Zeze.Transaction.Collections.PMap1<Zeze.Builtin.HistoryModule.BTableKey, Zeze.Net.Binary> _Changes;
    private long _Timestamp;

    private static final java.lang.invoke.VarHandle vh_GlobalSerialId;
    private static final java.lang.invoke.VarHandle vh_ProtocolClassName;
    private static final java.lang.invoke.VarHandle vh_ProtocolArgument;
    private static final java.lang.invoke.VarHandle vh_Timestamp;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_GlobalSerialId = _l_.findVarHandle(BLogChanges.class, "_GlobalSerialId", Zeze.Util.Id128.class);
            vh_ProtocolClassName = _l_.findVarHandle(BLogChanges.class, "_ProtocolClassName", String.class);
            vh_ProtocolArgument = _l_.findVarHandle(BLogChanges.class, "_ProtocolArgument", Zeze.Net.Binary.class);
            vh_Timestamp = _l_.findVarHandle(BLogChanges.class, "_Timestamp", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public Zeze.Util.Id128 getGlobalSerialId() {
        if (!isManaged())
            return _GlobalSerialId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _GlobalSerialId;
        @SuppressWarnings("unchecked")
        var log = (Zeze.Transaction.Logs.LogBeanKey<Zeze.Util.Id128>)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _GlobalSerialId;
    }

    public void setGlobalSerialId(Zeze.Util.Id128 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _GlobalSerialId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBeanKey<>(this, 1, vh_GlobalSerialId, _v_));
    }

    @Override
    public String getProtocolClassName() {
        if (!isManaged())
            return _ProtocolClassName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ProtocolClassName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _ProtocolClassName;
    }

    public void setProtocolClassName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ProtocolClassName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_ProtocolClassName, _v_));
    }

    @Override
    public Zeze.Net.Binary getProtocolArgument() {
        if (!isManaged())
            return _ProtocolArgument;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ProtocolArgument;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _ProtocolArgument;
    }

    public void setProtocolArgument(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ProtocolArgument = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 3, vh_ProtocolArgument, _v_));
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
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Timestamp;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _Timestamp;
    }

    public void setTimestamp(long _v_) {
        if (!isManaged()) {
            _Timestamp = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 5, vh_Timestamp, _v_));
    }

    @SuppressWarnings("deprecation")
    public BLogChanges() {
        _GlobalSerialId = new Zeze.Util.Id128();
        _ProtocolClassName = "";
        _ProtocolArgument = Zeze.Net.Binary.Empty;
        _Changes = new Zeze.Transaction.Collections.PMap1<>(Zeze.Builtin.HistoryModule.BTableKey.class, Zeze.Net.Binary.class);
        _Changes.variableId(4);
    }

    @SuppressWarnings("deprecation")
    public BLogChanges(Zeze.Util.Id128 _GlobalSerialId_, String _ProtocolClassName_, Zeze.Net.Binary _ProtocolArgument_, long _Timestamp_) {
        if (_GlobalSerialId_ == null)
            _GlobalSerialId_ = new Zeze.Util.Id128();
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
        setGlobalSerialId(new Zeze.Util.Id128());
        setProtocolClassName("");
        setProtocolArgument(Zeze.Net.Binary.Empty);
        _Changes.clear();
        setTimestamp(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.HistoryModule.BLogChanges.Data toData() {
        var _d_ = new Zeze.Builtin.HistoryModule.BLogChanges.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.HistoryModule.BLogChanges.Data)_o_);
    }

    public void assign(BLogChanges.Data _o_) {
        setGlobalSerialId(_o_._GlobalSerialId);
        setProtocolClassName(_o_._ProtocolClassName);
        setProtocolArgument(_o_._ProtocolArgument);
        _Changes.clear();
        _Changes.putAll(_o_._Changes);
        setTimestamp(_o_._Timestamp);
        _unknown_ = null;
    }

    public void assign(BLogChanges _o_) {
        setGlobalSerialId(_o_.getGlobalSerialId());
        setProtocolClassName(_o_.getProtocolClassName());
        setProtocolArgument(_o_.getProtocolArgument());
        _Changes.assign(_o_._Changes);
        setTimestamp(_o_.getTimestamp());
        _unknown_ = _o_._unknown_;
    }

    public BLogChanges copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLogChanges copy() {
        var _c_ = new BLogChanges();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLogChanges _a_, BLogChanges _b_) {
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
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.HistoryModule.BLogChanges: {\n");
        _s_.append(_i1_).append("GlobalSerialId=");
        getGlobalSerialId().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("ProtocolClassName=").append(getProtocolClassName()).append(",\n");
        _s_.append(_i1_).append("ProtocolArgument=").append(getProtocolArgument()).append(",\n");
        _s_.append(_i1_).append("Changes={");
        if (!_Changes.isEmpty()) {
            _s_.append('\n');
            for (var _e_ : _Changes.entrySet()) {
                _s_.append(_i2_).append("Key=");
                _e_.getKey().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("Timestamp=").append(getTimestamp()).append('\n');
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getGlobalSerialId().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
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
            _o_.ReadBean(getGlobalSerialId(), _t_);
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
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLogChanges))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLogChanges)_o_;
        if (!getGlobalSerialId().equals(_b_.getGlobalSerialId()))
            return false;
        if (!getProtocolClassName().equals(_b_.getProtocolClassName()))
            return false;
        if (!getProtocolArgument().equals(_b_.getProtocolArgument()))
            return false;
        if (!_Changes.equals(_b_._Changes))
            return false;
        if (getTimestamp() != _b_.getTimestamp())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Changes.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Changes.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getTimestamp() < 0)
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
                case 1: _GlobalSerialId = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Util.Id128>)_v_).value; break;
                case 2: _ProtocolClassName = _v_.stringValue(); break;
                case 3: _ProtocolArgument = _v_.binaryValue(); break;
                case 4: _Changes.followerApply(_v_); break;
                case 5: _Timestamp = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        _p_.add("GlobalSerialId");
        getGlobalSerialId().decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setProtocolClassName(_r_.getString(_pn_ + "ProtocolClassName"));
        if (getProtocolClassName() == null)
            setProtocolClassName("");
        setProtocolArgument(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "ProtocolArgument")));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Changes", _Changes, _r_.getString(_pn_ + "Changes"));
        setTimestamp(_r_.getLong(_pn_ + "Timestamp"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        _p_.add("GlobalSerialId");
        getGlobalSerialId().encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "ProtocolClassName", getProtocolClassName());
        _s_.appendBinary(_pn_ + "ProtocolArgument", getProtocolArgument());
        _s_.appendString(_pn_ + "Changes", Zeze.Serialize.Helper.encodeJson(_Changes));
        _s_.appendLong(_pn_ + "Timestamp", getTimestamp());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "GlobalSerialId", "Zeze.Util.Id128", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ProtocolClassName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ProtocolArgument", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Changes", "map", "Zeze.Builtin.HistoryModule.BTableKey", "binary"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Timestamp", "long", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 395935719895809559L;

    private Zeze.Util.Id128 _GlobalSerialId;
    private String _ProtocolClassName;
    private Zeze.Net.Binary _ProtocolArgument;
    private java.util.HashMap<Zeze.Builtin.HistoryModule.BTableKey, Zeze.Net.Binary> _Changes;
    private long _Timestamp;

    public Zeze.Util.Id128 getGlobalSerialId() {
        return _GlobalSerialId;
    }

    public void setGlobalSerialId(Zeze.Util.Id128 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _GlobalSerialId = _v_;
    }

    public String getProtocolClassName() {
        return _ProtocolClassName;
    }

    public void setProtocolClassName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ProtocolClassName = _v_;
    }

    public Zeze.Net.Binary getProtocolArgument() {
        return _ProtocolArgument;
    }

    public void setProtocolArgument(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ProtocolArgument = _v_;
    }

    public java.util.HashMap<Zeze.Builtin.HistoryModule.BTableKey, Zeze.Net.Binary> getChanges() {
        return _Changes;
    }

    public void setChanges(java.util.HashMap<Zeze.Builtin.HistoryModule.BTableKey, Zeze.Net.Binary> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Changes = _v_;
    }

    public long getTimestamp() {
        return _Timestamp;
    }

    public void setTimestamp(long _v_) {
        _Timestamp = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _GlobalSerialId = new Zeze.Util.Id128();
        _ProtocolClassName = "";
        _ProtocolArgument = Zeze.Net.Binary.Empty;
        _Changes = new java.util.HashMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Util.Id128 _GlobalSerialId_, String _ProtocolClassName_, Zeze.Net.Binary _ProtocolArgument_, java.util.HashMap<Zeze.Builtin.HistoryModule.BTableKey, Zeze.Net.Binary> _Changes_, long _Timestamp_) {
        if (_GlobalSerialId_ == null)
            _GlobalSerialId_ = new Zeze.Util.Id128();
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
        _GlobalSerialId = new Zeze.Util.Id128();
        _ProtocolClassName = "";
        _ProtocolArgument = Zeze.Net.Binary.Empty;
        _Changes.clear();
        _Timestamp = 0;
    }

    @Override
    public Zeze.Builtin.HistoryModule.BLogChanges toBean() {
        var _b_ = new Zeze.Builtin.HistoryModule.BLogChanges();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BLogChanges)_o_);
    }

    public void assign(BLogChanges _o_) {
        _GlobalSerialId = _o_.getGlobalSerialId();
        _ProtocolClassName = _o_.getProtocolClassName();
        _ProtocolArgument = _o_.getProtocolArgument();
        _Changes.clear();
        _Changes.putAll(_o_._Changes);
        _Timestamp = _o_.getTimestamp();
    }

    public void assign(BLogChanges.Data _o_) {
        _GlobalSerialId = _o_._GlobalSerialId;
        _ProtocolClassName = _o_._ProtocolClassName;
        _ProtocolArgument = _o_._ProtocolArgument;
        _Changes.clear();
        _Changes.putAll(_o_._Changes);
        _Timestamp = _o_._Timestamp;
    }

    @Override
    public BLogChanges.Data copy() {
        var _c_ = new BLogChanges.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLogChanges.Data _a_, BLogChanges.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
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
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.HistoryModule.BLogChanges: {\n");
        _s_.append(_i1_).append("GlobalSerialId=");
        _GlobalSerialId.buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("ProtocolClassName=").append(_ProtocolClassName).append(",\n");
        _s_.append(_i1_).append("ProtocolArgument=").append(_ProtocolArgument).append(",\n");
        _s_.append(_i1_).append("Changes={");
        if (!_Changes.isEmpty()) {
            _s_.append('\n');
            for (var _e_ : _Changes.entrySet()) {
                _s_.append(_i2_).append("Key=");
                _e_.getKey().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("Timestamp=").append(_Timestamp).append('\n');
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _GlobalSerialId.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
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
            _o_.ReadBean(_GlobalSerialId, _t_);
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
