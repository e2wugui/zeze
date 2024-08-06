// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BModuleRedirectAllRequest extends Zeze.Transaction.Bean implements BModuleRedirectAllRequestReadOnly {
    public static final long TYPEID = -1938324199607833342L;

    private int _ModuleId;
    private int _HashCodeConcurrentLevel; // 总的并发分组数量
    private final Zeze.Transaction.Collections.PSet1<Integer> _HashCodes; // 发送给具体进程时需要处理的分组hash-index（目前由linkd填写）
    private long _SourceProvider; // linkd 转发的时候填写本地provider的sessionId。
    private long _SessionId; // 发起请求者初始化，返回结果时带回。
    private String _MethodFullName; // format="ModuleFullName:MethodName"
    private Zeze.Net.Binary _Params;
    private String _ServiceNamePrefix;
    private int _Version; // 用于验证请求方和处理方的版本一致

    @Override
    public int getModuleId() {
        if (!isManaged())
            return _ModuleId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ModuleId;
        var log = (Log__ModuleId)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ModuleId;
    }

    public void setModuleId(int _v_) {
        if (!isManaged()) {
            _ModuleId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ModuleId(this, 1, _v_));
    }

    @Override
    public int getHashCodeConcurrentLevel() {
        if (!isManaged())
            return _HashCodeConcurrentLevel;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _HashCodeConcurrentLevel;
        var log = (Log__HashCodeConcurrentLevel)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _HashCodeConcurrentLevel;
    }

    public void setHashCodeConcurrentLevel(int _v_) {
        if (!isManaged()) {
            _HashCodeConcurrentLevel = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__HashCodeConcurrentLevel(this, 2, _v_));
    }

    public Zeze.Transaction.Collections.PSet1<Integer> getHashCodes() {
        return _HashCodes;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getHashCodesReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_HashCodes);
    }

    @Override
    public long getSourceProvider() {
        if (!isManaged())
            return _SourceProvider;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _SourceProvider;
        var log = (Log__SourceProvider)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _SourceProvider;
    }

    public void setSourceProvider(long _v_) {
        if (!isManaged()) {
            _SourceProvider = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__SourceProvider(this, 4, _v_));
    }

    @Override
    public long getSessionId() {
        if (!isManaged())
            return _SessionId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _SessionId;
        var log = (Log__SessionId)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _SessionId;
    }

    public void setSessionId(long _v_) {
        if (!isManaged()) {
            _SessionId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__SessionId(this, 5, _v_));
    }

    @Override
    public String getMethodFullName() {
        if (!isManaged())
            return _MethodFullName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _MethodFullName;
        var log = (Log__MethodFullName)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _MethodFullName;
    }

    public void setMethodFullName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _MethodFullName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__MethodFullName(this, 6, _v_));
    }

    @Override
    public Zeze.Net.Binary getParams() {
        if (!isManaged())
            return _Params;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Params;
        var log = (Log__Params)_t_.getLog(objectId() + 7);
        return log != null ? log.value : _Params;
    }

    public void setParams(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Params = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Params(this, 7, _v_));
    }

    @Override
    public String getServiceNamePrefix() {
        if (!isManaged())
            return _ServiceNamePrefix;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServiceNamePrefix;
        var log = (Log__ServiceNamePrefix)_t_.getLog(objectId() + 8);
        return log != null ? log.value : _ServiceNamePrefix;
    }

    public void setServiceNamePrefix(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceNamePrefix = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ServiceNamePrefix(this, 8, _v_));
    }

    @Override
    public int getVersion() {
        if (!isManaged())
            return _Version;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Version;
        var log = (Log__Version)_t_.getLog(objectId() + 9);
        return log != null ? log.value : _Version;
    }

    public void setVersion(int _v_) {
        if (!isManaged()) {
            _Version = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Version(this, 9, _v_));
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectAllRequest() {
        _HashCodes = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _HashCodes.variableId(3);
        _MethodFullName = "";
        _Params = Zeze.Net.Binary.Empty;
        _ServiceNamePrefix = "";
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectAllRequest(int _ModuleId_, int _HashCodeConcurrentLevel_, long _SourceProvider_, long _SessionId_, String _MethodFullName_, Zeze.Net.Binary _Params_, String _ServiceNamePrefix_, int _Version_) {
        _ModuleId = _ModuleId_;
        _HashCodeConcurrentLevel = _HashCodeConcurrentLevel_;
        _HashCodes = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _HashCodes.variableId(3);
        _SourceProvider = _SourceProvider_;
        _SessionId = _SessionId_;
        if (_MethodFullName_ == null)
            _MethodFullName_ = "";
        _MethodFullName = _MethodFullName_;
        if (_Params_ == null)
            _Params_ = Zeze.Net.Binary.Empty;
        _Params = _Params_;
        if (_ServiceNamePrefix_ == null)
            _ServiceNamePrefix_ = "";
        _ServiceNamePrefix = _ServiceNamePrefix_;
        _Version = _Version_;
    }

    @Override
    public void reset() {
        setModuleId(0);
        setHashCodeConcurrentLevel(0);
        _HashCodes.clear();
        setSourceProvider(0);
        setSessionId(0);
        setMethodFullName("");
        setParams(Zeze.Net.Binary.Empty);
        setServiceNamePrefix("");
        setVersion(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest.Data toData() {
        var _d_ = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest.Data)_o_);
    }

    public void assign(BModuleRedirectAllRequest.Data _o_) {
        setModuleId(_o_._ModuleId);
        setHashCodeConcurrentLevel(_o_._HashCodeConcurrentLevel);
        _HashCodes.clear();
        _HashCodes.addAll(_o_._HashCodes);
        setSourceProvider(_o_._SourceProvider);
        setSessionId(_o_._SessionId);
        setMethodFullName(_o_._MethodFullName);
        setParams(_o_._Params);
        setServiceNamePrefix(_o_._ServiceNamePrefix);
        setVersion(_o_._Version);
        _unknown_ = null;
    }

    public void assign(BModuleRedirectAllRequest _o_) {
        setModuleId(_o_.getModuleId());
        setHashCodeConcurrentLevel(_o_.getHashCodeConcurrentLevel());
        _HashCodes.assign(_o_._HashCodes);
        setSourceProvider(_o_.getSourceProvider());
        setSessionId(_o_.getSessionId());
        setMethodFullName(_o_.getMethodFullName());
        setParams(_o_.getParams());
        setServiceNamePrefix(_o_.getServiceNamePrefix());
        setVersion(_o_.getVersion());
        _unknown_ = _o_._unknown_;
    }

    public BModuleRedirectAllRequest copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BModuleRedirectAllRequest copy() {
        var _c_ = new BModuleRedirectAllRequest();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BModuleRedirectAllRequest _a_, BModuleRedirectAllRequest _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ModuleId extends Zeze.Transaction.Logs.LogInt {
        public Log__ModuleId(BModuleRedirectAllRequest _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._ModuleId = value; }
    }

    private static final class Log__HashCodeConcurrentLevel extends Zeze.Transaction.Logs.LogInt {
        public Log__HashCodeConcurrentLevel(BModuleRedirectAllRequest _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._HashCodeConcurrentLevel = value; }
    }

    private static final class Log__SourceProvider extends Zeze.Transaction.Logs.LogLong {
        public Log__SourceProvider(BModuleRedirectAllRequest _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._SourceProvider = value; }
    }

    private static final class Log__SessionId extends Zeze.Transaction.Logs.LogLong {
        public Log__SessionId(BModuleRedirectAllRequest _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._SessionId = value; }
    }

    private static final class Log__MethodFullName extends Zeze.Transaction.Logs.LogString {
        public Log__MethodFullName(BModuleRedirectAllRequest _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._MethodFullName = value; }
    }

    private static final class Log__Params extends Zeze.Transaction.Logs.LogBinary {
        public Log__Params(BModuleRedirectAllRequest _b_, int _i_, Zeze.Net.Binary _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._Params = value; }
    }

    private static final class Log__ServiceNamePrefix extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceNamePrefix(BModuleRedirectAllRequest _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._ServiceNamePrefix = value; }
    }

    private static final class Log__Version extends Zeze.Transaction.Logs.LogInt {
        public Log__Version(BModuleRedirectAllRequest _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._Version = value; }
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
        _s_.append("Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest: {\n");
        _s_.append(_i1_).append("ModuleId=").append(getModuleId()).append(",\n");
        _s_.append(_i1_).append("HashCodeConcurrentLevel=").append(getHashCodeConcurrentLevel()).append(",\n");
        _s_.append(_i1_).append("HashCodes={");
        if (!_HashCodes.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _HashCodes) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("SourceProvider=").append(getSourceProvider()).append(",\n");
        _s_.append(_i1_).append("SessionId=").append(getSessionId()).append(",\n");
        _s_.append(_i1_).append("MethodFullName=").append(getMethodFullName()).append(",\n");
        _s_.append(_i1_).append("Params=").append(getParams()).append(",\n");
        _s_.append(_i1_).append("ServiceNamePrefix=").append(getServiceNamePrefix()).append(",\n");
        _s_.append(_i1_).append("Version=").append(getVersion()).append('\n');
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
            int _x_ = getHashCodeConcurrentLevel();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _HashCodes;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            long _x_ = getSourceProvider();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            String _x_ = getMethodFullName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getParams();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = getServiceNamePrefix();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            setHashCodeConcurrentLevel(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _HashCodes;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setSourceProvider(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setSessionId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setMethodFullName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setParams(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            setServiceNamePrefix(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            setVersion(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BModuleRedirectAllRequest))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BModuleRedirectAllRequest)_o_;
        if (getModuleId() != _b_.getModuleId())
            return false;
        if (getHashCodeConcurrentLevel() != _b_.getHashCodeConcurrentLevel())
            return false;
        if (!_HashCodes.equals(_b_._HashCodes))
            return false;
        if (getSourceProvider() != _b_.getSourceProvider())
            return false;
        if (getSessionId() != _b_.getSessionId())
            return false;
        if (!getMethodFullName().equals(_b_.getMethodFullName()))
            return false;
        if (!getParams().equals(_b_.getParams()))
            return false;
        if (!getServiceNamePrefix().equals(_b_.getServiceNamePrefix()))
            return false;
        if (getVersion() != _b_.getVersion())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _HashCodes.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _HashCodes.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getModuleId() < 0)
            return true;
        if (getHashCodeConcurrentLevel() < 0)
            return true;
        for (var _v_ : _HashCodes) {
            if (_v_ < 0)
                return true;
        }
        if (getSourceProvider() < 0)
            return true;
        if (getSessionId() < 0)
            return true;
        if (getVersion() < 0)
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
                case 1: _ModuleId = _v_.intValue(); break;
                case 2: _HashCodeConcurrentLevel = _v_.intValue(); break;
                case 3: _HashCodes.followerApply(_v_); break;
                case 4: _SourceProvider = _v_.longValue(); break;
                case 5: _SessionId = _v_.longValue(); break;
                case 6: _MethodFullName = _v_.stringValue(); break;
                case 7: _Params = _v_.binaryValue(); break;
                case 8: _ServiceNamePrefix = _v_.stringValue(); break;
                case 9: _Version = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setModuleId(_r_.getInt(_pn_ + "ModuleId"));
        setHashCodeConcurrentLevel(_r_.getInt(_pn_ + "HashCodeConcurrentLevel"));
        Zeze.Serialize.Helper.decodeJsonSet(_HashCodes, Integer.class, _r_.getString(_pn_ + "HashCodes"));
        setSourceProvider(_r_.getLong(_pn_ + "SourceProvider"));
        setSessionId(_r_.getLong(_pn_ + "SessionId"));
        setMethodFullName(_r_.getString(_pn_ + "MethodFullName"));
        if (getMethodFullName() == null)
            setMethodFullName("");
        setParams(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Params")));
        setServiceNamePrefix(_r_.getString(_pn_ + "ServiceNamePrefix"));
        if (getServiceNamePrefix() == null)
            setServiceNamePrefix("");
        setVersion(_r_.getInt(_pn_ + "Version"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "ModuleId", getModuleId());
        _s_.appendInt(_pn_ + "HashCodeConcurrentLevel", getHashCodeConcurrentLevel());
        _s_.appendString(_pn_ + "HashCodes", Zeze.Serialize.Helper.encodeJson(_HashCodes));
        _s_.appendLong(_pn_ + "SourceProvider", getSourceProvider());
        _s_.appendLong(_pn_ + "SessionId", getSessionId());
        _s_.appendString(_pn_ + "MethodFullName", getMethodFullName());
        _s_.appendBinary(_pn_ + "Params", getParams());
        _s_.appendString(_pn_ + "ServiceNamePrefix", getServiceNamePrefix());
        _s_.appendInt(_pn_ + "Version", getVersion());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ModuleId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "HashCodeConcurrentLevel", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "HashCodes", "set", "", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "SourceProvider", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "SessionId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "MethodFullName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "Params", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "ServiceNamePrefix", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(9, "Version", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -1938324199607833342L;

    private int _ModuleId;
    private int _HashCodeConcurrentLevel; // 总的并发分组数量
    private java.util.HashSet<Integer> _HashCodes; // 发送给具体进程时需要处理的分组hash-index（目前由linkd填写）
    private long _SourceProvider; // linkd 转发的时候填写本地provider的sessionId。
    private long _SessionId; // 发起请求者初始化，返回结果时带回。
    private String _MethodFullName; // format="ModuleFullName:MethodName"
    private Zeze.Net.Binary _Params;
    private String _ServiceNamePrefix;
    private int _Version; // 用于验证请求方和处理方的版本一致

    public int getModuleId() {
        return _ModuleId;
    }

    public void setModuleId(int _v_) {
        _ModuleId = _v_;
    }

    public int getHashCodeConcurrentLevel() {
        return _HashCodeConcurrentLevel;
    }

    public void setHashCodeConcurrentLevel(int _v_) {
        _HashCodeConcurrentLevel = _v_;
    }

    public java.util.HashSet<Integer> getHashCodes() {
        return _HashCodes;
    }

    public void setHashCodes(java.util.HashSet<Integer> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _HashCodes = _v_;
    }

    public long getSourceProvider() {
        return _SourceProvider;
    }

    public void setSourceProvider(long _v_) {
        _SourceProvider = _v_;
    }

    public long getSessionId() {
        return _SessionId;
    }

    public void setSessionId(long _v_) {
        _SessionId = _v_;
    }

    public String getMethodFullName() {
        return _MethodFullName;
    }

    public void setMethodFullName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _MethodFullName = _v_;
    }

    public Zeze.Net.Binary getParams() {
        return _Params;
    }

    public void setParams(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Params = _v_;
    }

    public String getServiceNamePrefix() {
        return _ServiceNamePrefix;
    }

    public void setServiceNamePrefix(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ServiceNamePrefix = _v_;
    }

    public int getVersion() {
        return _Version;
    }

    public void setVersion(int _v_) {
        _Version = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _HashCodes = new java.util.HashSet<>();
        _MethodFullName = "";
        _Params = Zeze.Net.Binary.Empty;
        _ServiceNamePrefix = "";
    }

    @SuppressWarnings("deprecation")
    public Data(int _ModuleId_, int _HashCodeConcurrentLevel_, java.util.HashSet<Integer> _HashCodes_, long _SourceProvider_, long _SessionId_, String _MethodFullName_, Zeze.Net.Binary _Params_, String _ServiceNamePrefix_, int _Version_) {
        _ModuleId = _ModuleId_;
        _HashCodeConcurrentLevel = _HashCodeConcurrentLevel_;
        if (_HashCodes_ == null)
            _HashCodes_ = new java.util.HashSet<>();
        _HashCodes = _HashCodes_;
        _SourceProvider = _SourceProvider_;
        _SessionId = _SessionId_;
        if (_MethodFullName_ == null)
            _MethodFullName_ = "";
        _MethodFullName = _MethodFullName_;
        if (_Params_ == null)
            _Params_ = Zeze.Net.Binary.Empty;
        _Params = _Params_;
        if (_ServiceNamePrefix_ == null)
            _ServiceNamePrefix_ = "";
        _ServiceNamePrefix = _ServiceNamePrefix_;
        _Version = _Version_;
    }

    @Override
    public void reset() {
        _ModuleId = 0;
        _HashCodeConcurrentLevel = 0;
        _HashCodes.clear();
        _SourceProvider = 0;
        _SessionId = 0;
        _MethodFullName = "";
        _Params = Zeze.Net.Binary.Empty;
        _ServiceNamePrefix = "";
        _Version = 0;
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest toBean() {
        var _b_ = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BModuleRedirectAllRequest)_o_);
    }

    public void assign(BModuleRedirectAllRequest _o_) {
        _ModuleId = _o_.getModuleId();
        _HashCodeConcurrentLevel = _o_.getHashCodeConcurrentLevel();
        _HashCodes.clear();
        _HashCodes.addAll(_o_._HashCodes);
        _SourceProvider = _o_.getSourceProvider();
        _SessionId = _o_.getSessionId();
        _MethodFullName = _o_.getMethodFullName();
        _Params = _o_.getParams();
        _ServiceNamePrefix = _o_.getServiceNamePrefix();
        _Version = _o_.getVersion();
    }

    public void assign(BModuleRedirectAllRequest.Data _o_) {
        _ModuleId = _o_._ModuleId;
        _HashCodeConcurrentLevel = _o_._HashCodeConcurrentLevel;
        _HashCodes.clear();
        _HashCodes.addAll(_o_._HashCodes);
        _SourceProvider = _o_._SourceProvider;
        _SessionId = _o_._SessionId;
        _MethodFullName = _o_._MethodFullName;
        _Params = _o_._Params;
        _ServiceNamePrefix = _o_._ServiceNamePrefix;
        _Version = _o_._Version;
    }

    @Override
    public BModuleRedirectAllRequest.Data copy() {
        var _c_ = new BModuleRedirectAllRequest.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BModuleRedirectAllRequest.Data _a_, BModuleRedirectAllRequest.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BModuleRedirectAllRequest.Data clone() {
        return (BModuleRedirectAllRequest.Data)super.clone();
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
        _s_.append("Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest: {\n");
        _s_.append(_i1_).append("ModuleId=").append(_ModuleId).append(",\n");
        _s_.append(_i1_).append("HashCodeConcurrentLevel=").append(_HashCodeConcurrentLevel).append(",\n");
        _s_.append(_i1_).append("HashCodes={");
        if (!_HashCodes.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _HashCodes) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("SourceProvider=").append(_SourceProvider).append(",\n");
        _s_.append(_i1_).append("SessionId=").append(_SessionId).append(",\n");
        _s_.append(_i1_).append("MethodFullName=").append(_MethodFullName).append(",\n");
        _s_.append(_i1_).append("Params=").append(_Params).append(",\n");
        _s_.append(_i1_).append("ServiceNamePrefix=").append(_ServiceNamePrefix).append(",\n");
        _s_.append(_i1_).append("Version=").append(_Version).append('\n');
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
            int _x_ = _HashCodeConcurrentLevel;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _HashCodes;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            long _x_ = _SourceProvider;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            String _x_ = _MethodFullName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _Params;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = _ServiceNamePrefix;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = _Version;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            _HashCodeConcurrentLevel = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _HashCodes;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _SourceProvider = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _SessionId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _MethodFullName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            _Params = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            _ServiceNamePrefix = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            _Version = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
