// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
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
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    private static final java.lang.invoke.VarHandle vh_QueryIp;
    private static final java.lang.invoke.VarHandle vh_QueryPort;
    private static final java.lang.invoke.VarHandle vh_Tid;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_QueryIp = _l_.findVarHandle(BBatch.class, "_QueryIp", String.class);
            vh_QueryPort = _l_.findVarHandle(BBatch.class, "_QueryPort", int.class);
            vh_Tid = _l_.findVarHandle(BBatch.class, "_Tid", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
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
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _QueryIp;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _QueryIp;
    }

    public void setQueryIp(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _QueryIp = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 3, vh_QueryIp, _v_));
    }

    @Override
    public int getQueryPort() {
        if (!isManaged())
            return _QueryPort;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _QueryPort;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _QueryPort;
    }

    public void setQueryPort(int _v_) {
        if (!isManaged()) {
            _QueryPort = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 4, vh_QueryPort, _v_));
    }

    @Override
    public long getTid() {
        if (!isManaged())
            return _Tid;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Tid;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _Tid;
    }

    public void setTid(long _v_) {
        if (!isManaged()) {
            _Tid = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 5, vh_Tid, _v_));
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
        var _d_ = new Zeze.Builtin.Dbh2.BBatch.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.BBatch.Data)_o_);
    }

    public void assign(BBatch.Data _o_) {
        _Puts.clear();
        _Puts.putAll(_o_._Puts);
        _Deletes.clear();
        _Deletes.addAll(_o_._Deletes);
        setQueryIp(_o_._QueryIp);
        setQueryPort(_o_._QueryPort);
        setTid(_o_._Tid);
        _unknown_ = null;
    }

    public void assign(BBatch _o_) {
        _Puts.assign(_o_._Puts);
        _Deletes.assign(_o_._Deletes);
        setQueryIp(_o_.getQueryIp());
        setQueryPort(_o_.getQueryPort());
        setTid(_o_.getTid());
        _unknown_ = _o_._unknown_;
    }

    public BBatch copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBatch copy() {
        var _c_ = new BBatch();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBatch _a_, BBatch _b_) {
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
        _s_.append("Zeze.Builtin.Dbh2.BBatch: {\n");
        _s_.append(_i1_).append("Puts={");
        if (!_Puts.isEmpty()) {
            _s_.append('\n');
            for (var _e_ : _Puts.entrySet()) {
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("Deletes={");
        if (!_Deletes.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _Deletes) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("QueryIp=").append(getQueryIp()).append(",\n");
        _s_.append(_i1_).append("QueryPort=").append(getQueryPort()).append(",\n");
        _s_.append(_i1_).append("Tid=").append(getTid()).append('\n');
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Puts.initRootInfo(_r_, this);
        _Deletes.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Puts.initRootInfoWithRedo(_r_, this);
        _Deletes.initRootInfoWithRedo(_r_, this);
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
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _Puts.followerApply(_v_); break;
                case 2: _Deletes.followerApply(_v_); break;
                case 3: _QueryIp = _v_.stringValue(); break;
                case 4: _QueryPort = _v_.intValue(); break;
                case 5: _Tid = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonMap(this, "Puts", _Puts, _r_.getString(_pn_ + "Puts"));
        Zeze.Serialize.Helper.decodeJsonSet(_Deletes, Zeze.Net.Binary.class, _r_.getString(_pn_ + "Deletes"));
        setQueryIp(_r_.getString(_pn_ + "QueryIp"));
        if (getQueryIp() == null)
            setQueryIp("");
        setQueryPort(_r_.getInt(_pn_ + "QueryPort"));
        setTid(_r_.getLong(_pn_ + "Tid"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Puts", Zeze.Serialize.Helper.encodeJson(_Puts));
        _s_.appendString(_pn_ + "Deletes", Zeze.Serialize.Helper.encodeJson(_Deletes));
        _s_.appendString(_pn_ + "QueryIp", getQueryIp());
        _s_.appendInt(_pn_ + "QueryPort", getQueryPort());
        _s_.appendLong(_pn_ + "Tid", getTid());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Puts", "map", "binary", "binary"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Deletes", "set", "", "binary"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "QueryIp", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "QueryPort", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Tid", "long", "", ""));
        return _v_;
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

    public void setPuts(java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Puts = _v_;
    }

    public java.util.HashSet<Zeze.Net.Binary> getDeletes() {
        return _Deletes;
    }

    public void setDeletes(java.util.HashSet<Zeze.Net.Binary> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Deletes = _v_;
    }

    public String getQueryIp() {
        return _QueryIp;
    }

    public void setQueryIp(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _QueryIp = _v_;
    }

    public int getQueryPort() {
        return _QueryPort;
    }

    public void setQueryPort(int _v_) {
        _QueryPort = _v_;
    }

    public long getTid() {
        return _Tid;
    }

    public void setTid(long _v_) {
        _Tid = _v_;
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
        var _b_ = new Zeze.Builtin.Dbh2.BBatch();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BBatch)_o_);
    }

    public void assign(BBatch _o_) {
        _Puts.clear();
        _Puts.putAll(_o_._Puts);
        _Deletes.clear();
        _Deletes.addAll(_o_._Deletes);
        _QueryIp = _o_.getQueryIp();
        _QueryPort = _o_.getQueryPort();
        _Tid = _o_.getTid();
    }

    public void assign(BBatch.Data _o_) {
        _Puts.clear();
        _Puts.putAll(_o_._Puts);
        _Deletes.clear();
        _Deletes.addAll(_o_._Deletes);
        _QueryIp = _o_._QueryIp;
        _QueryPort = _o_._QueryPort;
        _Tid = _o_._Tid;
    }

    @Override
    public BBatch.Data copy() {
        var _c_ = new BBatch.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBatch.Data _a_, BBatch.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
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
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.Dbh2.BBatch: {\n");
        _s_.append(_i1_).append("Puts={");
        if (!_Puts.isEmpty()) {
            _s_.append('\n');
            for (var _e_ : _Puts.entrySet()) {
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("Deletes={");
        if (!_Deletes.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _Deletes) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("QueryIp=").append(_QueryIp).append(",\n");
        _s_.append(_i1_).append("QueryPort=").append(_QueryPort).append(",\n");
        _s_.append(_i1_).append("Tid=").append(_Tid).append('\n');
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

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BBatch.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBatch.Data)_o_;
        if (!_Puts.equals(_b_._Puts))
            return false;
        if (!_Deletes.equals(_b_._Deletes))
            return false;
        if (!_QueryIp.equals(_b_._QueryIp))
            return false;
        if (_QueryPort != _b_._QueryPort)
            return false;
        if (_Tid != _b_._Tid)
            return false;
        return true;
    }
}
}
