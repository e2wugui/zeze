// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BModuleRedirectAllResult extends Zeze.Transaction.Bean implements BModuleRedirectAllResultReadOnly {
    public static final long TYPEID = -6979067915808179070L;

    private int _ModuleId;
    private int _ServerId; // 目标server的id。
    private long _SourceProvider; // 从BModuleRedirectAllRequest里面得到。
    private String _MethodFullName; // format="ModuleFullName:MethodName"
    private long _SessionId; // 发起请求者初始化，返回结果时带回。
    private final Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash> _Hashes; // 发送给具体进程时需要处理的分组hash-index（目前由linkd填写）

    private static final java.lang.invoke.VarHandle vh_ModuleId;
    private static final java.lang.invoke.VarHandle vh_ServerId;
    private static final java.lang.invoke.VarHandle vh_SourceProvider;
    private static final java.lang.invoke.VarHandle vh_MethodFullName;
    private static final java.lang.invoke.VarHandle vh_SessionId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ModuleId = _l_.findVarHandle(BModuleRedirectAllResult.class, "_ModuleId", int.class);
            vh_ServerId = _l_.findVarHandle(BModuleRedirectAllResult.class, "_ServerId", int.class);
            vh_SourceProvider = _l_.findVarHandle(BModuleRedirectAllResult.class, "_SourceProvider", long.class);
            vh_MethodFullName = _l_.findVarHandle(BModuleRedirectAllResult.class, "_MethodFullName", String.class);
            vh_SessionId = _l_.findVarHandle(BModuleRedirectAllResult.class, "_SessionId", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getModuleId() {
        if (!isManaged())
            return _ModuleId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ModuleId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ModuleId;
    }

    public void setModuleId(int _v_) {
        if (!isManaged()) {
            _ModuleId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_ModuleId, _v_));
    }

    @Override
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServerId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int _v_) {
        if (!isManaged()) {
            _ServerId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_ServerId, _v_));
    }

    @Override
    public long getSourceProvider() {
        if (!isManaged())
            return _SourceProvider;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _SourceProvider;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _SourceProvider;
    }

    public void setSourceProvider(long _v_) {
        if (!isManaged()) {
            _SourceProvider = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_SourceProvider, _v_));
    }

    @Override
    public String getMethodFullName() {
        if (!isManaged())
            return _MethodFullName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _MethodFullName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 4);
        return log != null ? log.stringValue() : _MethodFullName;
    }

    public void setMethodFullName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _MethodFullName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 4, vh_MethodFullName, _v_));
    }

    @Override
    public long getSessionId() {
        if (!isManaged())
            return _SessionId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _SessionId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _SessionId;
    }

    public void setSessionId(long _v_) {
        if (!isManaged()) {
            _SessionId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 5, vh_SessionId, _v_));
    }

    public Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash> getHashes() {
        return _Hashes;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHashReadOnly> getHashesReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Hashes);
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectAllResult() {
        _MethodFullName = "";
        _Hashes = new Zeze.Transaction.Collections.PMap2<>(Integer.class, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.class);
        _Hashes.variableId(6);
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectAllResult(int _ModuleId_, int _ServerId_, long _SourceProvider_, String _MethodFullName_, long _SessionId_) {
        _ModuleId = _ModuleId_;
        _ServerId = _ServerId_;
        _SourceProvider = _SourceProvider_;
        if (_MethodFullName_ == null)
            _MethodFullName_ = "";
        _MethodFullName = _MethodFullName_;
        _SessionId = _SessionId_;
        _Hashes = new Zeze.Transaction.Collections.PMap2<>(Integer.class, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.class);
        _Hashes.variableId(6);
    }

    @Override
    public void reset() {
        setModuleId(0);
        setServerId(0);
        setSourceProvider(0);
        setMethodFullName("");
        setSessionId(0);
        _Hashes.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult.Data toData() {
        var _d_ = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult.Data)_o_);
    }

    public void assign(BModuleRedirectAllResult.Data _o_) {
        setModuleId(_o_._ModuleId);
        setServerId(_o_._ServerId);
        setSourceProvider(_o_._SourceProvider);
        setMethodFullName(_o_._MethodFullName);
        setSessionId(_o_._SessionId);
        _Hashes.clear();
        for (var _e_ : _o_._Hashes.entrySet()) {
            var _v_ = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash();
            _v_.assign(_e_.getValue());
            _Hashes.put(_e_.getKey(), _v_);
        }
        _unknown_ = null;
    }

    public void assign(BModuleRedirectAllResult _o_) {
        setModuleId(_o_.getModuleId());
        setServerId(_o_.getServerId());
        setSourceProvider(_o_.getSourceProvider());
        setMethodFullName(_o_.getMethodFullName());
        setSessionId(_o_.getSessionId());
        _Hashes.clear();
        for (var _e_ : _o_._Hashes.entrySet())
            _Hashes.put(_e_.getKey(), _e_.getValue().copy());
        _unknown_ = _o_._unknown_;
    }

    public BModuleRedirectAllResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BModuleRedirectAllResult copy() {
        var _c_ = new BModuleRedirectAllResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BModuleRedirectAllResult _a_, BModuleRedirectAllResult _b_) {
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
        _s_.append("Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult: {\n");
        _s_.append(_i1_).append("ModuleId=").append(getModuleId()).append(",\n");
        _s_.append(_i1_).append("ServerId=").append(getServerId()).append(",\n");
        _s_.append(_i1_).append("SourceProvider=").append(getSourceProvider()).append(",\n");
        _s_.append(_i1_).append("MethodFullName=").append(getMethodFullName()).append(",\n");
        _s_.append(_i1_).append("SessionId=").append(getSessionId()).append(",\n");
        _s_.append(_i1_).append("Hashes={");
        if (!_Hashes.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Hashes.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Hashes.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("}\n");
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
            int _x_ = getModuleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getSourceProvider();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getMethodFullName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getSessionId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Hashes;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
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
            setModuleId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setSourceProvider(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setMethodFullName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setSessionId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            var _x_ = _Hashes;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BModuleRedirectAllResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BModuleRedirectAllResult)_o_;
        if (getModuleId() != _b_.getModuleId())
            return false;
        if (getServerId() != _b_.getServerId())
            return false;
        if (getSourceProvider() != _b_.getSourceProvider())
            return false;
        if (!getMethodFullName().equals(_b_.getMethodFullName()))
            return false;
        if (getSessionId() != _b_.getSessionId())
            return false;
        if (!_Hashes.equals(_b_._Hashes))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Hashes.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Hashes.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getModuleId() < 0)
            return true;
        if (getServerId() < 0)
            return true;
        if (getSourceProvider() < 0)
            return true;
        if (getSessionId() < 0)
            return true;
        for (var _v_ : _Hashes.values()) {
            if (_v_.negativeCheck())
                return true;
        }
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
                case 1: _ModuleId = _v_.intValue(); break;
                case 2: _ServerId = _v_.intValue(); break;
                case 3: _SourceProvider = _v_.longValue(); break;
                case 4: _MethodFullName = _v_.stringValue(); break;
                case 5: _SessionId = _v_.longValue(); break;
                case 6: _Hashes.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setModuleId(_r_.getInt(_pn_ + "ModuleId"));
        setServerId(_r_.getInt(_pn_ + "ServerId"));
        setSourceProvider(_r_.getLong(_pn_ + "SourceProvider"));
        setMethodFullName(_r_.getString(_pn_ + "MethodFullName"));
        if (getMethodFullName() == null)
            setMethodFullName("");
        setSessionId(_r_.getLong(_pn_ + "SessionId"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Hashes", _Hashes, _r_.getString(_pn_ + "Hashes"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "ModuleId", getModuleId());
        _s_.appendInt(_pn_ + "ServerId", getServerId());
        _s_.appendLong(_pn_ + "SourceProvider", getSourceProvider());
        _s_.appendString(_pn_ + "MethodFullName", getMethodFullName());
        _s_.appendLong(_pn_ + "SessionId", getSessionId());
        _s_.appendString(_pn_ + "Hashes", Zeze.Serialize.Helper.encodeJson(_Hashes));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ModuleId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ServerId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "SourceProvider", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "MethodFullName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "SessionId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "Hashes", "map", "int", "Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash"));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6979067915808179070L;

    private int _ModuleId;
    private int _ServerId; // 目标server的id。
    private long _SourceProvider; // 从BModuleRedirectAllRequest里面得到。
    private String _MethodFullName; // format="ModuleFullName:MethodName"
    private long _SessionId; // 发起请求者初始化，返回结果时带回。
    private java.util.HashMap<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data> _Hashes; // 发送给具体进程时需要处理的分组hash-index（目前由linkd填写）

    public int getModuleId() {
        return _ModuleId;
    }

    public void setModuleId(int _v_) {
        _ModuleId = _v_;
    }

    public int getServerId() {
        return _ServerId;
    }

    public void setServerId(int _v_) {
        _ServerId = _v_;
    }

    public long getSourceProvider() {
        return _SourceProvider;
    }

    public void setSourceProvider(long _v_) {
        _SourceProvider = _v_;
    }

    public String getMethodFullName() {
        return _MethodFullName;
    }

    public void setMethodFullName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _MethodFullName = _v_;
    }

    public long getSessionId() {
        return _SessionId;
    }

    public void setSessionId(long _v_) {
        _SessionId = _v_;
    }

    public java.util.HashMap<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data> getHashes() {
        return _Hashes;
    }

    public void setHashes(java.util.HashMap<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Hashes = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _MethodFullName = "";
        _Hashes = new java.util.HashMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(int _ModuleId_, int _ServerId_, long _SourceProvider_, String _MethodFullName_, long _SessionId_, java.util.HashMap<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data> _Hashes_) {
        _ModuleId = _ModuleId_;
        _ServerId = _ServerId_;
        _SourceProvider = _SourceProvider_;
        if (_MethodFullName_ == null)
            _MethodFullName_ = "";
        _MethodFullName = _MethodFullName_;
        _SessionId = _SessionId_;
        if (_Hashes_ == null)
            _Hashes_ = new java.util.HashMap<>();
        _Hashes = _Hashes_;
    }

    @Override
    public void reset() {
        _ModuleId = 0;
        _ServerId = 0;
        _SourceProvider = 0;
        _MethodFullName = "";
        _SessionId = 0;
        _Hashes.clear();
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult toBean() {
        var _b_ = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BModuleRedirectAllResult)_o_);
    }

    public void assign(BModuleRedirectAllResult _o_) {
        _ModuleId = _o_.getModuleId();
        _ServerId = _o_.getServerId();
        _SourceProvider = _o_.getSourceProvider();
        _MethodFullName = _o_.getMethodFullName();
        _SessionId = _o_.getSessionId();
        _Hashes.clear();
        for (var _e_ : _o_._Hashes.entrySet()) {
            var _v_ = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data();
            _v_.assign(_e_.getValue());
            _Hashes.put(_e_.getKey(), _v_);
        }
    }

    public void assign(BModuleRedirectAllResult.Data _o_) {
        _ModuleId = _o_._ModuleId;
        _ServerId = _o_._ServerId;
        _SourceProvider = _o_._SourceProvider;
        _MethodFullName = _o_._MethodFullName;
        _SessionId = _o_._SessionId;
        _Hashes.clear();
        for (var _e_ : _o_._Hashes.entrySet())
            _Hashes.put(_e_.getKey(), _e_.getValue().copy());
    }

    @Override
    public BModuleRedirectAllResult.Data copy() {
        var _c_ = new BModuleRedirectAllResult.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BModuleRedirectAllResult.Data _a_, BModuleRedirectAllResult.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BModuleRedirectAllResult.Data clone() {
        return (BModuleRedirectAllResult.Data)super.clone();
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
        _s_.append("Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult: {\n");
        _s_.append(_i1_).append("ModuleId=").append(_ModuleId).append(",\n");
        _s_.append(_i1_).append("ServerId=").append(_ServerId).append(",\n");
        _s_.append(_i1_).append("SourceProvider=").append(_SourceProvider).append(",\n");
        _s_.append(_i1_).append("MethodFullName=").append(_MethodFullName).append(",\n");
        _s_.append(_i1_).append("SessionId=").append(_SessionId).append(",\n");
        _s_.append(_i1_).append("Hashes={");
        if (!_Hashes.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Hashes.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Hashes.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("}\n");
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
            int _x_ = _ModuleId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _ServerId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = _SourceProvider;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = _MethodFullName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = _SessionId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Hashes;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ModuleId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ServerId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _SourceProvider = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _MethodFullName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _SessionId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            var _x_ = _Hashes;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
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
        if (!(_o_ instanceof BModuleRedirectAllResult.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BModuleRedirectAllResult.Data)_o_;
        if (_ModuleId != _b_._ModuleId)
            return false;
        if (_ServerId != _b_._ServerId)
            return false;
        if (_SourceProvider != _b_._SourceProvider)
            return false;
        if (!_MethodFullName.equals(_b_._MethodFullName))
            return false;
        if (_SessionId != _b_._SessionId)
            return false;
        if (!_Hashes.equals(_b_._Hashes))
            return false;
        return true;
    }
}
}
