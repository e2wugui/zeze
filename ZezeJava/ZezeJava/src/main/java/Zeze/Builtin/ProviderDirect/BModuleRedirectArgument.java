// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BModuleRedirectArgument extends Zeze.Transaction.Bean implements BModuleRedirectArgumentReadOnly {
    public static final long TYPEID = -5561456902586805165L;

    private int _ModuleId;
    private int _HashCode; // server 计算。see BBind.ChoiceType。
    private int _RedirectType; // 如果是ToServer，ServerId存在HashCode中。
    private String _MethodFullName; // format="ModuleFullName:MethodName"
    private Zeze.Net.Binary _Params;
    private String _ServiceNamePrefix;
    private int _Version; // 用于验证请求方和处理方的版本一致
    private int _Key; // 用于处理请求和回复时作为TaskOneByOne的key
    private boolean _NoOneByOne; // 是否禁用TaskOneByOne处理请求和回复

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
    public int getHashCode() {
        if (!isManaged())
            return _HashCode;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _HashCode;
        var log = (Log__HashCode)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _HashCode;
    }

    public void setHashCode(int _v_) {
        if (!isManaged()) {
            _HashCode = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__HashCode(this, 2, _v_));
    }

    @Override
    public int getRedirectType() {
        if (!isManaged())
            return _RedirectType;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RedirectType;
        var log = (Log__RedirectType)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _RedirectType;
    }

    public void setRedirectType(int _v_) {
        if (!isManaged()) {
            _RedirectType = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__RedirectType(this, 3, _v_));
    }

    @Override
    public String getMethodFullName() {
        if (!isManaged())
            return _MethodFullName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _MethodFullName;
        var log = (Log__MethodFullName)_t_.getLog(objectId() + 4);
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
        _t_.putLog(new Log__MethodFullName(this, 4, _v_));
    }

    @Override
    public Zeze.Net.Binary getParams() {
        if (!isManaged())
            return _Params;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Params;
        var log = (Log__Params)_t_.getLog(objectId() + 5);
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
        _t_.putLog(new Log__Params(this, 5, _v_));
    }

    @Override
    public String getServiceNamePrefix() {
        if (!isManaged())
            return _ServiceNamePrefix;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServiceNamePrefix;
        var log = (Log__ServiceNamePrefix)_t_.getLog(objectId() + 6);
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
        _t_.putLog(new Log__ServiceNamePrefix(this, 6, _v_));
    }

    @Override
    public int getVersion() {
        if (!isManaged())
            return _Version;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Version;
        var log = (Log__Version)_t_.getLog(objectId() + 7);
        return log != null ? log.value : _Version;
    }

    public void setVersion(int _v_) {
        if (!isManaged()) {
            _Version = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Version(this, 7, _v_));
    }

    @Override
    public int getKey() {
        if (!isManaged())
            return _Key;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Key;
        var log = (Log__Key)_t_.getLog(objectId() + 8);
        return log != null ? log.value : _Key;
    }

    public void setKey(int _v_) {
        if (!isManaged()) {
            _Key = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Key(this, 8, _v_));
    }

    @Override
    public boolean isNoOneByOne() {
        if (!isManaged())
            return _NoOneByOne;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _NoOneByOne;
        var log = (Log__NoOneByOne)_t_.getLog(objectId() + 9);
        return log != null ? log.value : _NoOneByOne;
    }

    public void setNoOneByOne(boolean _v_) {
        if (!isManaged()) {
            _NoOneByOne = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__NoOneByOne(this, 9, _v_));
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectArgument() {
        _MethodFullName = "";
        _Params = Zeze.Net.Binary.Empty;
        _ServiceNamePrefix = "";
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectArgument(int _ModuleId_, int _HashCode_, int _RedirectType_, String _MethodFullName_, Zeze.Net.Binary _Params_, String _ServiceNamePrefix_, int _Version_, int _Key_, boolean _NoOneByOne_) {
        _ModuleId = _ModuleId_;
        _HashCode = _HashCode_;
        _RedirectType = _RedirectType_;
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
        _Key = _Key_;
        _NoOneByOne = _NoOneByOne_;
    }

    @Override
    public void reset() {
        setModuleId(0);
        setHashCode(0);
        setRedirectType(0);
        setMethodFullName("");
        setParams(Zeze.Net.Binary.Empty);
        setServiceNamePrefix("");
        setVersion(0);
        setKey(0);
        setNoOneByOne(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BModuleRedirectArgument.Data toData() {
        var _d_ = new Zeze.Builtin.ProviderDirect.BModuleRedirectArgument.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.ProviderDirect.BModuleRedirectArgument.Data)_o_);
    }

    public void assign(BModuleRedirectArgument.Data _o_) {
        setModuleId(_o_._ModuleId);
        setHashCode(_o_._HashCode);
        setRedirectType(_o_._RedirectType);
        setMethodFullName(_o_._MethodFullName);
        setParams(_o_._Params);
        setServiceNamePrefix(_o_._ServiceNamePrefix);
        setVersion(_o_._Version);
        setKey(_o_._Key);
        setNoOneByOne(_o_._NoOneByOne);
        _unknown_ = null;
    }

    public void assign(BModuleRedirectArgument _o_) {
        setModuleId(_o_.getModuleId());
        setHashCode(_o_.getHashCode());
        setRedirectType(_o_.getRedirectType());
        setMethodFullName(_o_.getMethodFullName());
        setParams(_o_.getParams());
        setServiceNamePrefix(_o_.getServiceNamePrefix());
        setVersion(_o_.getVersion());
        setKey(_o_.getKey());
        setNoOneByOne(_o_.isNoOneByOne());
        _unknown_ = _o_._unknown_;
    }

    public BModuleRedirectArgument copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BModuleRedirectArgument copy() {
        var _c_ = new BModuleRedirectArgument();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BModuleRedirectArgument _a_, BModuleRedirectArgument _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ModuleId extends Zeze.Transaction.Logs.LogInt {
        public Log__ModuleId(BModuleRedirectArgument _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._ModuleId = value; }
    }

    private static final class Log__HashCode extends Zeze.Transaction.Logs.LogInt {
        public Log__HashCode(BModuleRedirectArgument _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._HashCode = value; }
    }

    private static final class Log__RedirectType extends Zeze.Transaction.Logs.LogInt {
        public Log__RedirectType(BModuleRedirectArgument _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._RedirectType = value; }
    }

    private static final class Log__MethodFullName extends Zeze.Transaction.Logs.LogString {
        public Log__MethodFullName(BModuleRedirectArgument _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._MethodFullName = value; }
    }

    private static final class Log__Params extends Zeze.Transaction.Logs.LogBinary {
        public Log__Params(BModuleRedirectArgument _b_, int _i_, Zeze.Net.Binary _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._Params = value; }
    }

    private static final class Log__ServiceNamePrefix extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceNamePrefix(BModuleRedirectArgument _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._ServiceNamePrefix = value; }
    }

    private static final class Log__Version extends Zeze.Transaction.Logs.LogInt {
        public Log__Version(BModuleRedirectArgument _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._Version = value; }
    }

    private static final class Log__Key extends Zeze.Transaction.Logs.LogInt {
        public Log__Key(BModuleRedirectArgument _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._Key = value; }
    }

    private static final class Log__NoOneByOne extends Zeze.Transaction.Logs.LogBool {
        public Log__NoOneByOne(BModuleRedirectArgument _b_, int _i_, boolean _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._NoOneByOne = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.ProviderDirect.BModuleRedirectArgument: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("ModuleId=").append(getModuleId()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("HashCode=").append(getHashCode()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("RedirectType=").append(getRedirectType()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("MethodFullName=").append(getMethodFullName()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Params=").append(getParams()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("ServiceNamePrefix=").append(getServiceNamePrefix()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Version=").append(getVersion()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Key=").append(getKey()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("NoOneByOne=").append(isNoOneByOne()).append(System.lineSeparator());
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
            int _x_ = getModuleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getHashCode();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getRedirectType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            var _x_ = getParams();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = getServiceNamePrefix();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getKey();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = isNoOneByOne();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            setHashCode(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setRedirectType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setMethodFullName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setParams(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setServiceNamePrefix(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setVersion(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            setKey(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            setNoOneByOne(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BModuleRedirectArgument))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BModuleRedirectArgument)_o_;
        if (getModuleId() != _b_.getModuleId())
            return false;
        if (getHashCode() != _b_.getHashCode())
            return false;
        if (getRedirectType() != _b_.getRedirectType())
            return false;
        if (!getMethodFullName().equals(_b_.getMethodFullName()))
            return false;
        if (!getParams().equals(_b_.getParams()))
            return false;
        if (!getServiceNamePrefix().equals(_b_.getServiceNamePrefix()))
            return false;
        if (getVersion() != _b_.getVersion())
            return false;
        if (getKey() != _b_.getKey())
            return false;
        if (isNoOneByOne() != _b_.isNoOneByOne())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getModuleId() < 0)
            return true;
        if (getHashCode() < 0)
            return true;
        if (getRedirectType() < 0)
            return true;
        if (getVersion() < 0)
            return true;
        if (getKey() < 0)
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
                case 2: _HashCode = _v_.intValue(); break;
                case 3: _RedirectType = _v_.intValue(); break;
                case 4: _MethodFullName = _v_.stringValue(); break;
                case 5: _Params = _v_.binaryValue(); break;
                case 6: _ServiceNamePrefix = _v_.stringValue(); break;
                case 7: _Version = _v_.intValue(); break;
                case 8: _Key = _v_.intValue(); break;
                case 9: _NoOneByOne = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setModuleId(_r_.getInt(_pn_ + "ModuleId"));
        setHashCode(_r_.getInt(_pn_ + "HashCode"));
        setRedirectType(_r_.getInt(_pn_ + "RedirectType"));
        setMethodFullName(_r_.getString(_pn_ + "MethodFullName"));
        if (getMethodFullName() == null)
            setMethodFullName("");
        setParams(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Params")));
        setServiceNamePrefix(_r_.getString(_pn_ + "ServiceNamePrefix"));
        if (getServiceNamePrefix() == null)
            setServiceNamePrefix("");
        setVersion(_r_.getInt(_pn_ + "Version"));
        setKey(_r_.getInt(_pn_ + "Key"));
        setNoOneByOne(_r_.getBoolean(_pn_ + "NoOneByOne"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "ModuleId", getModuleId());
        _s_.appendInt(_pn_ + "HashCode", getHashCode());
        _s_.appendInt(_pn_ + "RedirectType", getRedirectType());
        _s_.appendString(_pn_ + "MethodFullName", getMethodFullName());
        _s_.appendBinary(_pn_ + "Params", getParams());
        _s_.appendString(_pn_ + "ServiceNamePrefix", getServiceNamePrefix());
        _s_.appendInt(_pn_ + "Version", getVersion());
        _s_.appendInt(_pn_ + "Key", getKey());
        _s_.appendBoolean(_pn_ + "NoOneByOne", isNoOneByOne());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ModuleId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "HashCode", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "RedirectType", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "MethodFullName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Params", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "ServiceNamePrefix", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "Version", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "Key", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(9, "NoOneByOne", "bool", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5561456902586805165L;

    private int _ModuleId;
    private int _HashCode; // server 计算。see BBind.ChoiceType。
    private int _RedirectType; // 如果是ToServer，ServerId存在HashCode中。
    private String _MethodFullName; // format="ModuleFullName:MethodName"
    private Zeze.Net.Binary _Params;
    private String _ServiceNamePrefix;
    private int _Version; // 用于验证请求方和处理方的版本一致
    private int _Key; // 用于处理请求和回复时作为TaskOneByOne的key
    private boolean _NoOneByOne; // 是否禁用TaskOneByOne处理请求和回复

    public int getModuleId() {
        return _ModuleId;
    }

    public void setModuleId(int _v_) {
        _ModuleId = _v_;
    }

    public int getHashCode() {
        return _HashCode;
    }

    public void setHashCode(int _v_) {
        _HashCode = _v_;
    }

    public int getRedirectType() {
        return _RedirectType;
    }

    public void setRedirectType(int _v_) {
        _RedirectType = _v_;
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

    public int getKey() {
        return _Key;
    }

    public void setKey(int _v_) {
        _Key = _v_;
    }

    public boolean isNoOneByOne() {
        return _NoOneByOne;
    }

    public void setNoOneByOne(boolean _v_) {
        _NoOneByOne = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _MethodFullName = "";
        _Params = Zeze.Net.Binary.Empty;
        _ServiceNamePrefix = "";
    }

    @SuppressWarnings("deprecation")
    public Data(int _ModuleId_, int _HashCode_, int _RedirectType_, String _MethodFullName_, Zeze.Net.Binary _Params_, String _ServiceNamePrefix_, int _Version_, int _Key_, boolean _NoOneByOne_) {
        _ModuleId = _ModuleId_;
        _HashCode = _HashCode_;
        _RedirectType = _RedirectType_;
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
        _Key = _Key_;
        _NoOneByOne = _NoOneByOne_;
    }

    @Override
    public void reset() {
        _ModuleId = 0;
        _HashCode = 0;
        _RedirectType = 0;
        _MethodFullName = "";
        _Params = Zeze.Net.Binary.Empty;
        _ServiceNamePrefix = "";
        _Version = 0;
        _Key = 0;
        _NoOneByOne = false;
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BModuleRedirectArgument toBean() {
        var _b_ = new Zeze.Builtin.ProviderDirect.BModuleRedirectArgument();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BModuleRedirectArgument)_o_);
    }

    public void assign(BModuleRedirectArgument _o_) {
        _ModuleId = _o_.getModuleId();
        _HashCode = _o_.getHashCode();
        _RedirectType = _o_.getRedirectType();
        _MethodFullName = _o_.getMethodFullName();
        _Params = _o_.getParams();
        _ServiceNamePrefix = _o_.getServiceNamePrefix();
        _Version = _o_.getVersion();
        _Key = _o_.getKey();
        _NoOneByOne = _o_.isNoOneByOne();
    }

    public void assign(BModuleRedirectArgument.Data _o_) {
        _ModuleId = _o_._ModuleId;
        _HashCode = _o_._HashCode;
        _RedirectType = _o_._RedirectType;
        _MethodFullName = _o_._MethodFullName;
        _Params = _o_._Params;
        _ServiceNamePrefix = _o_._ServiceNamePrefix;
        _Version = _o_._Version;
        _Key = _o_._Key;
        _NoOneByOne = _o_._NoOneByOne;
    }

    @Override
    public BModuleRedirectArgument.Data copy() {
        var _c_ = new BModuleRedirectArgument.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BModuleRedirectArgument.Data _a_, BModuleRedirectArgument.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BModuleRedirectArgument.Data clone() {
        return (BModuleRedirectArgument.Data)super.clone();
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.ProviderDirect.BModuleRedirectArgument: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("ModuleId=").append(_ModuleId).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("HashCode=").append(_HashCode).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("RedirectType=").append(_RedirectType).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("MethodFullName=").append(_MethodFullName).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Params=").append(_Params).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("ServiceNamePrefix=").append(_ServiceNamePrefix).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Version=").append(_Version).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Key=").append(_Key).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("NoOneByOne=").append(_NoOneByOne).append(System.lineSeparator());
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
            int _x_ = _ModuleId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _HashCode;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _RedirectType;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            var _x_ = _Params;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = _ServiceNamePrefix;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = _Version;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _Key;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = _NoOneByOne;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            _HashCode = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _RedirectType = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _MethodFullName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _Params = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _ServiceNamePrefix = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            _Version = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            _Key = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            _NoOneByOne = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
