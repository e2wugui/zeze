// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

// gs to link
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BAnnounceProviderInfo extends Zeze.Transaction.Bean implements BAnnounceProviderInfoReadOnly {
    public static final long TYPEID = 4964769950995033065L;

    private String _ServiceNamePrefix;
    private String _ServiceIndentity;
    private String _ProviderDirectIp;
    private int _ProviderDirectPort;
    private int _AppVersion; // gs 版本，报告给linkd，让linkd只给最新版本的gs分发请求。

    @Override
    public String getServiceNamePrefix() {
        if (!isManaged())
            return _ServiceNamePrefix;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServiceNamePrefix;
        var log = (Log__ServiceNamePrefix)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ServiceNamePrefix;
    }

    public void setServiceNamePrefix(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceNamePrefix = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServiceNamePrefix(this, 1, value));
    }

    @Override
    public String getServiceIndentity() {
        if (!isManaged())
            return _ServiceIndentity;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServiceIndentity;
        var log = (Log__ServiceIndentity)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ServiceIndentity;
    }

    public void setServiceIndentity(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceIndentity = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServiceIndentity(this, 2, value));
    }

    @Override
    public String getProviderDirectIp() {
        if (!isManaged())
            return _ProviderDirectIp;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ProviderDirectIp;
        var log = (Log__ProviderDirectIp)txn.getLog(objectId() + 3);
        return log != null ? log.value : _ProviderDirectIp;
    }

    public void setProviderDirectIp(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ProviderDirectIp = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ProviderDirectIp(this, 3, value));
    }

    @Override
    public int getProviderDirectPort() {
        if (!isManaged())
            return _ProviderDirectPort;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ProviderDirectPort;
        var log = (Log__ProviderDirectPort)txn.getLog(objectId() + 4);
        return log != null ? log.value : _ProviderDirectPort;
    }

    public void setProviderDirectPort(int value) {
        if (!isManaged()) {
            _ProviderDirectPort = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ProviderDirectPort(this, 4, value));
    }

    @Override
    public int getAppVersion() {
        if (!isManaged())
            return _AppVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _AppVersion;
        var log = (Log__AppVersion)txn.getLog(objectId() + 5);
        return log != null ? log.value : _AppVersion;
    }

    public void setAppVersion(int value) {
        if (!isManaged()) {
            _AppVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__AppVersion(this, 5, value));
    }

    @SuppressWarnings("deprecation")
    public BAnnounceProviderInfo() {
        _ServiceNamePrefix = "";
        _ServiceIndentity = "";
        _ProviderDirectIp = "";
    }

    @SuppressWarnings("deprecation")
    public BAnnounceProviderInfo(String _ServiceNamePrefix_, String _ServiceIndentity_, String _ProviderDirectIp_, int _ProviderDirectPort_, int _AppVersion_) {
        if (_ServiceNamePrefix_ == null)
            _ServiceNamePrefix_ = "";
        _ServiceNamePrefix = _ServiceNamePrefix_;
        if (_ServiceIndentity_ == null)
            _ServiceIndentity_ = "";
        _ServiceIndentity = _ServiceIndentity_;
        if (_ProviderDirectIp_ == null)
            _ProviderDirectIp_ = "";
        _ProviderDirectIp = _ProviderDirectIp_;
        _ProviderDirectPort = _ProviderDirectPort_;
        _AppVersion = _AppVersion_;
    }

    @Override
    public void reset() {
        setServiceNamePrefix("");
        setServiceIndentity("");
        setProviderDirectIp("");
        setProviderDirectPort(0);
        setAppVersion(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BAnnounceProviderInfo.Data toData() {
        var data = new Zeze.Builtin.Provider.BAnnounceProviderInfo.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Provider.BAnnounceProviderInfo.Data)other);
    }

    public void assign(BAnnounceProviderInfo.Data other) {
        setServiceNamePrefix(other._ServiceNamePrefix);
        setServiceIndentity(other._ServiceIndentity);
        setProviderDirectIp(other._ProviderDirectIp);
        setProviderDirectPort(other._ProviderDirectPort);
        setAppVersion(other._AppVersion);
        _unknown_ = null;
    }

    public void assign(BAnnounceProviderInfo other) {
        setServiceNamePrefix(other.getServiceNamePrefix());
        setServiceIndentity(other.getServiceIndentity());
        setProviderDirectIp(other.getProviderDirectIp());
        setProviderDirectPort(other.getProviderDirectPort());
        setAppVersion(other.getAppVersion());
        _unknown_ = other._unknown_;
    }

    public BAnnounceProviderInfo copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAnnounceProviderInfo copy() {
        var copy = new BAnnounceProviderInfo();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAnnounceProviderInfo a, BAnnounceProviderInfo b) {
        BAnnounceProviderInfo save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServiceNamePrefix extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceNamePrefix(BAnnounceProviderInfo bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAnnounceProviderInfo)getBelong())._ServiceNamePrefix = value; }
    }

    private static final class Log__ServiceIndentity extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceIndentity(BAnnounceProviderInfo bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAnnounceProviderInfo)getBelong())._ServiceIndentity = value; }
    }

    private static final class Log__ProviderDirectIp extends Zeze.Transaction.Logs.LogString {
        public Log__ProviderDirectIp(BAnnounceProviderInfo bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAnnounceProviderInfo)getBelong())._ProviderDirectIp = value; }
    }

    private static final class Log__ProviderDirectPort extends Zeze.Transaction.Logs.LogInt {
        public Log__ProviderDirectPort(BAnnounceProviderInfo bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAnnounceProviderInfo)getBelong())._ProviderDirectPort = value; }
    }

    private static final class Log__AppVersion extends Zeze.Transaction.Logs.LogInt {
        public Log__AppVersion(BAnnounceProviderInfo bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAnnounceProviderInfo)getBelong())._AppVersion = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BAnnounceProviderInfo: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceNamePrefix=").append(getServiceNamePrefix()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceIndentity=").append(getServiceIndentity()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderDirectIp=").append(getProviderDirectIp()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderDirectPort=").append(getProviderDirectPort()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("AppVersion=").append(getAppVersion()).append(System.lineSeparator());
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
            String _x_ = getServiceNamePrefix();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getServiceIndentity();
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
            int _x_ = getAppVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setServiceNamePrefix(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setServiceIndentity(_o_.ReadString(_t_));
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
            setAppVersion(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _ServiceNamePrefix = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _ServiceIndentity = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _ProviderDirectIp = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _ProviderDirectPort = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 5: _AppVersion = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setServiceNamePrefix(rs.getString(_parents_name_ + "ServiceNamePrefix"));
        if (getServiceNamePrefix() == null)
            setServiceNamePrefix("");
        setServiceIndentity(rs.getString(_parents_name_ + "ServiceIndentity"));
        if (getServiceIndentity() == null)
            setServiceIndentity("");
        setProviderDirectIp(rs.getString(_parents_name_ + "ProviderDirectIp"));
        if (getProviderDirectIp() == null)
            setProviderDirectIp("");
        setProviderDirectPort(rs.getInt(_parents_name_ + "ProviderDirectPort"));
        setAppVersion(rs.getInt(_parents_name_ + "AppVersion"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "ServiceNamePrefix", getServiceNamePrefix());
        st.appendString(_parents_name_ + "ServiceIndentity", getServiceIndentity());
        st.appendString(_parents_name_ + "ProviderDirectIp", getProviderDirectIp());
        st.appendInt(_parents_name_ + "ProviderDirectPort", getProviderDirectPort());
        st.appendInt(_parents_name_ + "AppVersion", getAppVersion());
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BAnnounceProviderInfo
    }

// gs to link
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4964769950995033065L;

    private String _ServiceNamePrefix;
    private String _ServiceIndentity;
    private String _ProviderDirectIp;
    private int _ProviderDirectPort;
    private int _AppVersion; // gs 版本，报告给linkd，让linkd只给最新版本的gs分发请求。

    public String getServiceNamePrefix() {
        return _ServiceNamePrefix;
    }

    public void setServiceNamePrefix(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ServiceNamePrefix = value;
    }

    public String getServiceIndentity() {
        return _ServiceIndentity;
    }

    public void setServiceIndentity(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ServiceIndentity = value;
    }

    public String getProviderDirectIp() {
        return _ProviderDirectIp;
    }

    public void setProviderDirectIp(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ProviderDirectIp = value;
    }

    public int getProviderDirectPort() {
        return _ProviderDirectPort;
    }

    public void setProviderDirectPort(int value) {
        _ProviderDirectPort = value;
    }

    public int getAppVersion() {
        return _AppVersion;
    }

    public void setAppVersion(int value) {
        _AppVersion = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ServiceNamePrefix = "";
        _ServiceIndentity = "";
        _ProviderDirectIp = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _ServiceNamePrefix_, String _ServiceIndentity_, String _ProviderDirectIp_, int _ProviderDirectPort_, int _AppVersion_) {
        if (_ServiceNamePrefix_ == null)
            _ServiceNamePrefix_ = "";
        _ServiceNamePrefix = _ServiceNamePrefix_;
        if (_ServiceIndentity_ == null)
            _ServiceIndentity_ = "";
        _ServiceIndentity = _ServiceIndentity_;
        if (_ProviderDirectIp_ == null)
            _ProviderDirectIp_ = "";
        _ProviderDirectIp = _ProviderDirectIp_;
        _ProviderDirectPort = _ProviderDirectPort_;
        _AppVersion = _AppVersion_;
    }

    @Override
    public void reset() {
        _ServiceNamePrefix = "";
        _ServiceIndentity = "";
        _ProviderDirectIp = "";
        _ProviderDirectPort = 0;
        _AppVersion = 0;
    }

    @Override
    public Zeze.Builtin.Provider.BAnnounceProviderInfo toBean() {
        var bean = new Zeze.Builtin.Provider.BAnnounceProviderInfo();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BAnnounceProviderInfo)other);
    }

    public void assign(BAnnounceProviderInfo other) {
        _ServiceNamePrefix = other.getServiceNamePrefix();
        _ServiceIndentity = other.getServiceIndentity();
        _ProviderDirectIp = other.getProviderDirectIp();
        _ProviderDirectPort = other.getProviderDirectPort();
        _AppVersion = other.getAppVersion();
    }

    public void assign(BAnnounceProviderInfo.Data other) {
        _ServiceNamePrefix = other._ServiceNamePrefix;
        _ServiceIndentity = other._ServiceIndentity;
        _ProviderDirectIp = other._ProviderDirectIp;
        _ProviderDirectPort = other._ProviderDirectPort;
        _AppVersion = other._AppVersion;
    }

    @Override
    public BAnnounceProviderInfo.Data copy() {
        var copy = new BAnnounceProviderInfo.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAnnounceProviderInfo.Data a, BAnnounceProviderInfo.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
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
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BAnnounceProviderInfo: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceNamePrefix=").append(_ServiceNamePrefix).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceIndentity=").append(_ServiceIndentity).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderDirectIp=").append(_ProviderDirectIp).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderDirectPort=").append(_ProviderDirectPort).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("AppVersion=").append(_AppVersion).append(System.lineSeparator());
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
            String _x_ = _ServiceNamePrefix;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _ServiceIndentity;
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
            int _x_ = _AppVersion;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
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
            _ServiceNamePrefix = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ServiceIndentity = _o_.ReadString(_t_);
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
            _AppVersion = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
