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
    public String getServiceIdentity() {
        if (!isManaged())
            return _ServiceIdentity;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServiceIdentity;
        var log = (Log__ServiceIdentity)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ServiceIdentity;
    }

    public void setServiceIdentity(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceIdentity = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServiceIdentity(this, 2, value));
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
    public long getAppVersion() {
        if (!isManaged())
            return _AppVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _AppVersion;
        var log = (Log__AppVersion)txn.getLog(objectId() + 5);
        return log != null ? log.value : _AppVersion;
    }

    public void setAppVersion(long value) {
        if (!isManaged()) {
            _AppVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__AppVersion(this, 5, value));
    }

    @Override
    public boolean isDisableChoice() {
        if (!isManaged())
            return _DisableChoice;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _DisableChoice;
        var log = (Log__DisableChoice)txn.getLog(objectId() + 6);
        return log != null ? log.value : _DisableChoice;
    }

    public void setDisableChoice(boolean value) {
        if (!isManaged()) {
            _DisableChoice = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__DisableChoice(this, 6, value));
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
        setServiceIdentity(other._ServiceIdentity);
        setProviderDirectIp(other._ProviderDirectIp);
        setProviderDirectPort(other._ProviderDirectPort);
        setAppVersion(other._AppVersion);
        setDisableChoice(other._DisableChoice);
        _unknown_ = null;
    }

    public void assign(BAnnounceProviderInfo other) {
        setServiceNamePrefix(other.getServiceNamePrefix());
        setServiceIdentity(other.getServiceIdentity());
        setProviderDirectIp(other.getProviderDirectIp());
        setProviderDirectPort(other.getProviderDirectPort());
        setAppVersion(other.getAppVersion());
        setDisableChoice(other.isDisableChoice());
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

    private static final class Log__ServiceIdentity extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceIdentity(BAnnounceProviderInfo bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAnnounceProviderInfo)getBelong())._ServiceIdentity = value; }
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

    private static final class Log__AppVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__AppVersion(BAnnounceProviderInfo bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAnnounceProviderInfo)getBelong())._AppVersion = value; }
    }

    private static final class Log__DisableChoice extends Zeze.Transaction.Logs.LogBool {
        public Log__DisableChoice(BAnnounceProviderInfo bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAnnounceProviderInfo)getBelong())._DisableChoice = value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceIdentity=").append(getServiceIdentity()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderDirectIp=").append(getProviderDirectIp()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderDirectPort=").append(getProviderDirectPort()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("AppVersion=").append(getAppVersion()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("DisableChoice=").append(isDisableChoice()).append(System.lineSeparator());
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _ServiceNamePrefix = vlog.stringValue(); break;
                case 2: _ServiceIdentity = vlog.stringValue(); break;
                case 3: _ProviderDirectIp = vlog.stringValue(); break;
                case 4: _ProviderDirectPort = vlog.intValue(); break;
                case 5: _AppVersion = vlog.longValue(); break;
                case 6: _DisableChoice = vlog.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setServiceNamePrefix(rs.getString(_parents_name_ + "ServiceNamePrefix"));
        if (getServiceNamePrefix() == null)
            setServiceNamePrefix("");
        setServiceIdentity(rs.getString(_parents_name_ + "ServiceIdentity"));
        if (getServiceIdentity() == null)
            setServiceIdentity("");
        setProviderDirectIp(rs.getString(_parents_name_ + "ProviderDirectIp"));
        if (getProviderDirectIp() == null)
            setProviderDirectIp("");
        setProviderDirectPort(rs.getInt(_parents_name_ + "ProviderDirectPort"));
        setAppVersion(rs.getLong(_parents_name_ + "AppVersion"));
        setDisableChoice(rs.getBoolean(_parents_name_ + "DisableChoice"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "ServiceNamePrefix", getServiceNamePrefix());
        st.appendString(_parents_name_ + "ServiceIdentity", getServiceIdentity());
        st.appendString(_parents_name_ + "ProviderDirectIp", getProviderDirectIp());
        st.appendInt(_parents_name_ + "ProviderDirectPort", getProviderDirectPort());
        st.appendLong(_parents_name_ + "AppVersion", getAppVersion());
        st.appendBoolean(_parents_name_ + "DisableChoice", isDisableChoice());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServiceNamePrefix", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ServiceIdentity", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ProviderDirectIp", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "ProviderDirectPort", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "AppVersion", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "DisableChoice", "bool", "", ""));
        return vars;
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

    public void setServiceNamePrefix(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ServiceNamePrefix = value;
    }

    public String getServiceIdentity() {
        return _ServiceIdentity;
    }

    public void setServiceIdentity(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ServiceIdentity = value;
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

    public long getAppVersion() {
        return _AppVersion;
    }

    public void setAppVersion(long value) {
        _AppVersion = value;
    }

    public boolean isDisableChoice() {
        return _DisableChoice;
    }

    public void setDisableChoice(boolean value) {
        _DisableChoice = value;
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
        _ServiceIdentity = other.getServiceIdentity();
        _ProviderDirectIp = other.getProviderDirectIp();
        _ProviderDirectPort = other.getProviderDirectPort();
        _AppVersion = other.getAppVersion();
        _DisableChoice = other.isDisableChoice();
    }

    public void assign(BAnnounceProviderInfo.Data other) {
        _ServiceNamePrefix = other._ServiceNamePrefix;
        _ServiceIdentity = other._ServiceIdentity;
        _ProviderDirectIp = other._ProviderDirectIp;
        _ProviderDirectPort = other._ProviderDirectPort;
        _AppVersion = other._AppVersion;
        _DisableChoice = other._DisableChoice;
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
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceIdentity=").append(_ServiceIdentity).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderDirectIp=").append(_ProviderDirectIp).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderDirectPort=").append(_ProviderDirectPort).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("AppVersion=").append(_AppVersion).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("DisableChoice=").append(_DisableChoice).append(System.lineSeparator());
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
