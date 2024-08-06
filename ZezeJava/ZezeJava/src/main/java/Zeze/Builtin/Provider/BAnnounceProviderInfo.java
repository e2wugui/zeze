// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// gs to link
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BAnnounceProviderInfo extends Zeze.Transaction.Bean implements BAnnounceProviderInfoReadOnly {
    public static final long TYPEID = 4964769950995033065L;

    private String _ServiceNamePrefix;
    private String _ServiceIdentity;
    private String _ProviderDirectIp;
    private int _ProviderDirectPort;
    private long _AppVersion; // 4段版本号(a.b.c.d),从高到低依次占16位,a位不兼容,b位向后兼容,c和d前后兼容
    private boolean _DisableChoice;

    @Override
    public String getServiceNamePrefix() {
        if (!isManaged())
            return _ServiceNamePrefix;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServiceNamePrefix;
        var log = (Log__ServiceNamePrefix)_t_.getLog(objectId() + 1);
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
        _t_.putLog(new Log__ServiceNamePrefix(this, 1, _v_));
    }

    @Override
    public String getServiceIdentity() {
        if (!isManaged())
            return _ServiceIdentity;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServiceIdentity;
        var log = (Log__ServiceIdentity)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _ServiceIdentity;
    }

    public void setServiceIdentity(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceIdentity = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ServiceIdentity(this, 2, _v_));
    }

    @Override
    public String getProviderDirectIp() {
        if (!isManaged())
            return _ProviderDirectIp;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ProviderDirectIp;
        var log = (Log__ProviderDirectIp)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _ProviderDirectIp;
    }

    public void setProviderDirectIp(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ProviderDirectIp = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ProviderDirectIp(this, 3, _v_));
    }

    @Override
    public int getProviderDirectPort() {
        if (!isManaged())
            return _ProviderDirectPort;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ProviderDirectPort;
        var log = (Log__ProviderDirectPort)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _ProviderDirectPort;
    }

    public void setProviderDirectPort(int _v_) {
        if (!isManaged()) {
            _ProviderDirectPort = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ProviderDirectPort(this, 4, _v_));
    }

    @Override
    public long getAppVersion() {
        if (!isManaged())
            return _AppVersion;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _AppVersion;
        var log = (Log__AppVersion)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _AppVersion;
    }

    public void setAppVersion(long _v_) {
        if (!isManaged()) {
            _AppVersion = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__AppVersion(this, 5, _v_));
    }

    @Override
    public boolean isDisableChoice() {
        if (!isManaged())
            return _DisableChoice;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _DisableChoice;
        var log = (Log__DisableChoice)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _DisableChoice;
    }

    public void setDisableChoice(boolean _v_) {
        if (!isManaged()) {
            _DisableChoice = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__DisableChoice(this, 6, _v_));
    }

    @SuppressWarnings("deprecation")
    public BAnnounceProviderInfo() {
        _ServiceNamePrefix = "";
        _ServiceIdentity = "";
        _ProviderDirectIp = "";
    }

    @SuppressWarnings("deprecation")
    public BAnnounceProviderInfo(String _ServiceNamePrefix_, String _ServiceIdentity_, String _ProviderDirectIp_, int _ProviderDirectPort_, long _AppVersion_, boolean _DisableChoice_) {
        if (_ServiceNamePrefix_ == null)
            _ServiceNamePrefix_ = "";
        _ServiceNamePrefix = _ServiceNamePrefix_;
        if (_ServiceIdentity_ == null)
            _ServiceIdentity_ = "";
        _ServiceIdentity = _ServiceIdentity_;
        if (_ProviderDirectIp_ == null)
            _ProviderDirectIp_ = "";
        _ProviderDirectIp = _ProviderDirectIp_;
        _ProviderDirectPort = _ProviderDirectPort_;
        _AppVersion = _AppVersion_;
        _DisableChoice = _DisableChoice_;
    }

    @Override
    public void reset() {
        setServiceNamePrefix("");
        setServiceIdentity("");
        setProviderDirectIp("");
        setProviderDirectPort(0);
        setAppVersion(0);
        setDisableChoice(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BAnnounceProviderInfo.Data toData() {
        var _d_ = new Zeze.Builtin.Provider.BAnnounceProviderInfo.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Provider.BAnnounceProviderInfo.Data)_o_);
    }

    public void assign(BAnnounceProviderInfo.Data _o_) {
        setServiceNamePrefix(_o_._ServiceNamePrefix);
        setServiceIdentity(_o_._ServiceIdentity);
        setProviderDirectIp(_o_._ProviderDirectIp);
        setProviderDirectPort(_o_._ProviderDirectPort);
        setAppVersion(_o_._AppVersion);
        setDisableChoice(_o_._DisableChoice);
        _unknown_ = null;
    }

    public void assign(BAnnounceProviderInfo _o_) {
        setServiceNamePrefix(_o_.getServiceNamePrefix());
        setServiceIdentity(_o_.getServiceIdentity());
        setProviderDirectIp(_o_.getProviderDirectIp());
        setProviderDirectPort(_o_.getProviderDirectPort());
        setAppVersion(_o_.getAppVersion());
        setDisableChoice(_o_.isDisableChoice());
        _unknown_ = _o_._unknown_;
    }

    public BAnnounceProviderInfo copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAnnounceProviderInfo copy() {
        var _c_ = new BAnnounceProviderInfo();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAnnounceProviderInfo _a_, BAnnounceProviderInfo _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServiceNamePrefix extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceNamePrefix(BAnnounceProviderInfo _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BAnnounceProviderInfo)getBelong())._ServiceNamePrefix = value; }
    }

    private static final class Log__ServiceIdentity extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceIdentity(BAnnounceProviderInfo _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BAnnounceProviderInfo)getBelong())._ServiceIdentity = value; }
    }

    private static final class Log__ProviderDirectIp extends Zeze.Transaction.Logs.LogString {
        public Log__ProviderDirectIp(BAnnounceProviderInfo _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BAnnounceProviderInfo)getBelong())._ProviderDirectIp = value; }
    }

    private static final class Log__ProviderDirectPort extends Zeze.Transaction.Logs.LogInt {
        public Log__ProviderDirectPort(BAnnounceProviderInfo _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BAnnounceProviderInfo)getBelong())._ProviderDirectPort = value; }
    }

    private static final class Log__AppVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__AppVersion(BAnnounceProviderInfo _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BAnnounceProviderInfo)getBelong())._AppVersion = value; }
    }

    private static final class Log__DisableChoice extends Zeze.Transaction.Logs.LogBool {
        public Log__DisableChoice(BAnnounceProviderInfo _b_, int _i_, boolean _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BAnnounceProviderInfo)getBelong())._DisableChoice = value; }
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
        _s_.append("Zeze.Builtin.Provider.BAnnounceProviderInfo: {\n");
        _s_.append(_i1_).append("ServiceNamePrefix=").append(getServiceNamePrefix()).append(",\n");
        _s_.append(_i1_).append("ServiceIdentity=").append(getServiceIdentity()).append(",\n");
        _s_.append(_i1_).append("ProviderDirectIp=").append(getProviderDirectIp()).append(",\n");
        _s_.append(_i1_).append("ProviderDirectPort=").append(getProviderDirectPort()).append(",\n");
        _s_.append(_i1_).append("AppVersion=").append(getAppVersion()).append(",\n");
        _s_.append(_i1_).append("DisableChoice=").append(isDisableChoice()).append('\n');
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
            String _x_ = getServiceNamePrefix();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getServiceIdentity();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getProviderDirectIp();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getProviderDirectPort();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getAppVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            boolean _x_ = isDisableChoice();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
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
            setServiceNamePrefix(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setServiceIdentity(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setProviderDirectIp(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setProviderDirectPort(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setAppVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setDisableChoice(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BAnnounceProviderInfo))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAnnounceProviderInfo)_o_;
        if (!getServiceNamePrefix().equals(_b_.getServiceNamePrefix()))
            return false;
        if (!getServiceIdentity().equals(_b_.getServiceIdentity()))
            return false;
        if (!getProviderDirectIp().equals(_b_.getProviderDirectIp()))
            return false;
        if (getProviderDirectPort() != _b_.getProviderDirectPort())
            return false;
        if (getAppVersion() != _b_.getAppVersion())
            return false;
        if (isDisableChoice() != _b_.isDisableChoice())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getProviderDirectPort() < 0)
            return true;
        if (getAppVersion() < 0)
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
                case 1: _ServiceNamePrefix = _v_.stringValue(); break;
                case 2: _ServiceIdentity = _v_.stringValue(); break;
                case 3: _ProviderDirectIp = _v_.stringValue(); break;
                case 4: _ProviderDirectPort = _v_.intValue(); break;
                case 5: _AppVersion = _v_.longValue(); break;
                case 6: _DisableChoice = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setServiceNamePrefix(_r_.getString(_pn_ + "ServiceNamePrefix"));
        if (getServiceNamePrefix() == null)
            setServiceNamePrefix("");
        setServiceIdentity(_r_.getString(_pn_ + "ServiceIdentity"));
        if (getServiceIdentity() == null)
            setServiceIdentity("");
        setProviderDirectIp(_r_.getString(_pn_ + "ProviderDirectIp"));
        if (getProviderDirectIp() == null)
            setProviderDirectIp("");
        setProviderDirectPort(_r_.getInt(_pn_ + "ProviderDirectPort"));
        setAppVersion(_r_.getLong(_pn_ + "AppVersion"));
        setDisableChoice(_r_.getBoolean(_pn_ + "DisableChoice"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "ServiceNamePrefix", getServiceNamePrefix());
        _s_.appendString(_pn_ + "ServiceIdentity", getServiceIdentity());
        _s_.appendString(_pn_ + "ProviderDirectIp", getProviderDirectIp());
        _s_.appendInt(_pn_ + "ProviderDirectPort", getProviderDirectPort());
        _s_.appendLong(_pn_ + "AppVersion", getAppVersion());
        _s_.appendBoolean(_pn_ + "DisableChoice", isDisableChoice());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServiceNamePrefix", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ServiceIdentity", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ProviderDirectIp", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "ProviderDirectPort", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "AppVersion", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "DisableChoice", "bool", "", ""));
        return _v_;
    }

// gs to link
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4964769950995033065L;

    private String _ServiceNamePrefix;
    private String _ServiceIdentity;
    private String _ProviderDirectIp;
    private int _ProviderDirectPort;
    private long _AppVersion; // 4段版本号(a.b.c.d),从高到低依次占16位,a位不兼容,b位向后兼容,c和d前后兼容
    private boolean _DisableChoice;

    public String getServiceNamePrefix() {
        return _ServiceNamePrefix;
    }

    public void setServiceNamePrefix(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ServiceNamePrefix = _v_;
    }

    public String getServiceIdentity() {
        return _ServiceIdentity;
    }

    public void setServiceIdentity(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ServiceIdentity = _v_;
    }

    public String getProviderDirectIp() {
        return _ProviderDirectIp;
    }

    public void setProviderDirectIp(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ProviderDirectIp = _v_;
    }

    public int getProviderDirectPort() {
        return _ProviderDirectPort;
    }

    public void setProviderDirectPort(int _v_) {
        _ProviderDirectPort = _v_;
    }

    public long getAppVersion() {
        return _AppVersion;
    }

    public void setAppVersion(long _v_) {
        _AppVersion = _v_;
    }

    public boolean isDisableChoice() {
        return _DisableChoice;
    }

    public void setDisableChoice(boolean _v_) {
        _DisableChoice = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ServiceNamePrefix = "";
        _ServiceIdentity = "";
        _ProviderDirectIp = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _ServiceNamePrefix_, String _ServiceIdentity_, String _ProviderDirectIp_, int _ProviderDirectPort_, long _AppVersion_, boolean _DisableChoice_) {
        if (_ServiceNamePrefix_ == null)
            _ServiceNamePrefix_ = "";
        _ServiceNamePrefix = _ServiceNamePrefix_;
        if (_ServiceIdentity_ == null)
            _ServiceIdentity_ = "";
        _ServiceIdentity = _ServiceIdentity_;
        if (_ProviderDirectIp_ == null)
            _ProviderDirectIp_ = "";
        _ProviderDirectIp = _ProviderDirectIp_;
        _ProviderDirectPort = _ProviderDirectPort_;
        _AppVersion = _AppVersion_;
        _DisableChoice = _DisableChoice_;
    }

    @Override
    public void reset() {
        _ServiceNamePrefix = "";
        _ServiceIdentity = "";
        _ProviderDirectIp = "";
        _ProviderDirectPort = 0;
        _AppVersion = 0;
        _DisableChoice = false;
    }

    @Override
    public Zeze.Builtin.Provider.BAnnounceProviderInfo toBean() {
        var _b_ = new Zeze.Builtin.Provider.BAnnounceProviderInfo();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BAnnounceProviderInfo)_o_);
    }

    public void assign(BAnnounceProviderInfo _o_) {
        _ServiceNamePrefix = _o_.getServiceNamePrefix();
        _ServiceIdentity = _o_.getServiceIdentity();
        _ProviderDirectIp = _o_.getProviderDirectIp();
        _ProviderDirectPort = _o_.getProviderDirectPort();
        _AppVersion = _o_.getAppVersion();
        _DisableChoice = _o_.isDisableChoice();
    }

    public void assign(BAnnounceProviderInfo.Data _o_) {
        _ServiceNamePrefix = _o_._ServiceNamePrefix;
        _ServiceIdentity = _o_._ServiceIdentity;
        _ProviderDirectIp = _o_._ProviderDirectIp;
        _ProviderDirectPort = _o_._ProviderDirectPort;
        _AppVersion = _o_._AppVersion;
        _DisableChoice = _o_._DisableChoice;
    }

    @Override
    public BAnnounceProviderInfo.Data copy() {
        var _c_ = new BAnnounceProviderInfo.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAnnounceProviderInfo.Data _a_, BAnnounceProviderInfo.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BAnnounceProviderInfo.Data clone() {
        return (BAnnounceProviderInfo.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Provider.BAnnounceProviderInfo: {\n");
        _s_.append(_i1_).append("ServiceNamePrefix=").append(_ServiceNamePrefix).append(",\n");
        _s_.append(_i1_).append("ServiceIdentity=").append(_ServiceIdentity).append(",\n");
        _s_.append(_i1_).append("ProviderDirectIp=").append(_ProviderDirectIp).append(",\n");
        _s_.append(_i1_).append("ProviderDirectPort=").append(_ProviderDirectPort).append(",\n");
        _s_.append(_i1_).append("AppVersion=").append(_AppVersion).append(",\n");
        _s_.append(_i1_).append("DisableChoice=").append(_DisableChoice).append('\n');
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
            String _x_ = _ServiceNamePrefix;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _ServiceIdentity;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _ProviderDirectIp;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = _ProviderDirectPort;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = _AppVersion;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            boolean _x_ = _DisableChoice;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
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
            _ServiceNamePrefix = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ServiceIdentity = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _ProviderDirectIp = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _ProviderDirectPort = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _AppVersion = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _DisableChoice = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
