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
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ModuleId;
        var log = (Log__ModuleId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ModuleId;
    }

    public void setModuleId(int value) {
        if (!isManaged()) {
            _ModuleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ModuleId(this, 1, value));
    }

    @Override
    public int getHashCode() {
        if (!isManaged())
            return _HashCode;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HashCode;
        var log = (Log__HashCode)txn.getLog(objectId() + 2);
        return log != null ? log.value : _HashCode;
    }

    public void setHashCode(int value) {
        if (!isManaged()) {
            _HashCode = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__HashCode(this, 2, value));
    }

    @Override
    public int getRedirectType() {
        if (!isManaged())
            return _RedirectType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _RedirectType;
        var log = (Log__RedirectType)txn.getLog(objectId() + 3);
        return log != null ? log.value : _RedirectType;
    }

    public void setRedirectType(int value) {
        if (!isManaged()) {
            _RedirectType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__RedirectType(this, 3, value));
    }

    @Override
    public String getMethodFullName() {
        if (!isManaged())
            return _MethodFullName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _MethodFullName;
        var log = (Log__MethodFullName)txn.getLog(objectId() + 4);
        return log != null ? log.value : _MethodFullName;
    }

    public void setMethodFullName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _MethodFullName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__MethodFullName(this, 4, value));
    }

    @Override
    public Zeze.Net.Binary getParams() {
        if (!isManaged())
            return _Params;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Params;
        var log = (Log__Params)txn.getLog(objectId() + 5);
        return log != null ? log.value : _Params;
    }

    public void setParams(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Params = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Params(this, 5, value));
    }

    @Override
    public String getServiceNamePrefix() {
        if (!isManaged())
            return _ServiceNamePrefix;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServiceNamePrefix;
        var log = (Log__ServiceNamePrefix)txn.getLog(objectId() + 6);
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
        txn.putLog(new Log__ServiceNamePrefix(this, 6, value));
    }

    @Override
    public int getVersion() {
        if (!isManaged())
            return _Version;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Version;
        var log = (Log__Version)txn.getLog(objectId() + 7);
        return log != null ? log.value : _Version;
    }

    public void setVersion(int value) {
        if (!isManaged()) {
            _Version = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Version(this, 7, value));
    }

    @Override
    public int getKey() {
        if (!isManaged())
            return _Key;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Key;
        var log = (Log__Key)txn.getLog(objectId() + 8);
        return log != null ? log.value : _Key;
    }

    public void setKey(int value) {
        if (!isManaged()) {
            _Key = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Key(this, 8, value));
    }

    @Override
    public boolean isNoOneByOne() {
        if (!isManaged())
            return _NoOneByOne;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NoOneByOne;
        var log = (Log__NoOneByOne)txn.getLog(objectId() + 9);
        return log != null ? log.value : _NoOneByOne;
    }

    public void setNoOneByOne(boolean value) {
        if (!isManaged()) {
            _NoOneByOne = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NoOneByOne(this, 9, value));
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
        var data = new Zeze.Builtin.ProviderDirect.BModuleRedirectArgument.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.ProviderDirect.BModuleRedirectArgument.Data)other);
    }

    public void assign(BModuleRedirectArgument.Data other) {
        setModuleId(other._ModuleId);
        setHashCode(other._HashCode);
        setRedirectType(other._RedirectType);
        setMethodFullName(other._MethodFullName);
        setParams(other._Params);
        setServiceNamePrefix(other._ServiceNamePrefix);
        setVersion(other._Version);
        setKey(other._Key);
        setNoOneByOne(other._NoOneByOne);
        _unknown_ = null;
    }

    public void assign(BModuleRedirectArgument other) {
        setModuleId(other.getModuleId());
        setHashCode(other.getHashCode());
        setRedirectType(other.getRedirectType());
        setMethodFullName(other.getMethodFullName());
        setParams(other.getParams());
        setServiceNamePrefix(other.getServiceNamePrefix());
        setVersion(other.getVersion());
        setKey(other.getKey());
        setNoOneByOne(other.isNoOneByOne());
        _unknown_ = other._unknown_;
    }

    public BModuleRedirectArgument copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BModuleRedirectArgument copy() {
        var copy = new BModuleRedirectArgument();
        copy.assign(this);
        return copy;
    }

    public static void swap(BModuleRedirectArgument a, BModuleRedirectArgument b) {
        BModuleRedirectArgument save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ModuleId extends Zeze.Transaction.Logs.LogInt {
        public Log__ModuleId(BModuleRedirectArgument bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._ModuleId = value; }
    }

    private static final class Log__HashCode extends Zeze.Transaction.Logs.LogInt {
        public Log__HashCode(BModuleRedirectArgument bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._HashCode = value; }
    }

    private static final class Log__RedirectType extends Zeze.Transaction.Logs.LogInt {
        public Log__RedirectType(BModuleRedirectArgument bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._RedirectType = value; }
    }

    private static final class Log__MethodFullName extends Zeze.Transaction.Logs.LogString {
        public Log__MethodFullName(BModuleRedirectArgument bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._MethodFullName = value; }
    }

    private static final class Log__Params extends Zeze.Transaction.Logs.LogBinary {
        public Log__Params(BModuleRedirectArgument bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._Params = value; }
    }

    private static final class Log__ServiceNamePrefix extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceNamePrefix(BModuleRedirectArgument bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._ServiceNamePrefix = value; }
    }

    private static final class Log__Version extends Zeze.Transaction.Logs.LogInt {
        public Log__Version(BModuleRedirectArgument bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._Version = value; }
    }

    private static final class Log__Key extends Zeze.Transaction.Logs.LogInt {
        public Log__Key(BModuleRedirectArgument bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._Key = value; }
    }

    private static final class Log__NoOneByOne extends Zeze.Transaction.Logs.LogBool {
        public Log__NoOneByOne(BModuleRedirectArgument bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectArgument)getBelong())._NoOneByOne = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ProviderDirect.BModuleRedirectArgument: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ModuleId=").append(getModuleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HashCode=").append(getHashCode()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RedirectType=").append(getRedirectType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("MethodFullName=").append(getMethodFullName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Params=").append(getParams()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceNamePrefix=").append(getServiceNamePrefix()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Version=").append(getVersion()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(getKey()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NoOneByOne=").append(isNoOneByOne()).append(System.lineSeparator());
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _ModuleId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _HashCode = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _RedirectType = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 4: _MethodFullName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 5: _Params = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 6: _ServiceNamePrefix = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 7: _Version = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 8: _Key = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 9: _NoOneByOne = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setModuleId(rs.getInt(_parents_name_ + "ModuleId"));
        setHashCode(rs.getInt(_parents_name_ + "HashCode"));
        setRedirectType(rs.getInt(_parents_name_ + "RedirectType"));
        setMethodFullName(rs.getString(_parents_name_ + "MethodFullName"));
        if (getMethodFullName() == null)
            setMethodFullName("");
        setParams(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Params")));
        setServiceNamePrefix(rs.getString(_parents_name_ + "ServiceNamePrefix"));
        if (getServiceNamePrefix() == null)
            setServiceNamePrefix("");
        setVersion(rs.getInt(_parents_name_ + "Version"));
        setKey(rs.getInt(_parents_name_ + "Key"));
        setNoOneByOne(rs.getBoolean(_parents_name_ + "NoOneByOne"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "ModuleId", getModuleId());
        st.appendInt(_parents_name_ + "HashCode", getHashCode());
        st.appendInt(_parents_name_ + "RedirectType", getRedirectType());
        st.appendString(_parents_name_ + "MethodFullName", getMethodFullName());
        st.appendBinary(_parents_name_ + "Params", getParams());
        st.appendString(_parents_name_ + "ServiceNamePrefix", getServiceNamePrefix());
        st.appendInt(_parents_name_ + "Version", getVersion());
        st.appendInt(_parents_name_ + "Key", getKey());
        st.appendBoolean(_parents_name_ + "NoOneByOne", isNoOneByOne());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ModuleId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "HashCode", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "RedirectType", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "MethodFullName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Params", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "ServiceNamePrefix", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "Version", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "Key", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(9, "NoOneByOne", "bool", "", ""));
        return vars;
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

    public void setModuleId(int value) {
        _ModuleId = value;
    }

    public int getHashCode() {
        return _HashCode;
    }

    public void setHashCode(int value) {
        _HashCode = value;
    }

    public int getRedirectType() {
        return _RedirectType;
    }

    public void setRedirectType(int value) {
        _RedirectType = value;
    }

    public String getMethodFullName() {
        return _MethodFullName;
    }

    public void setMethodFullName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _MethodFullName = value;
    }

    public Zeze.Net.Binary getParams() {
        return _Params;
    }

    public void setParams(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Params = value;
    }

    public String getServiceNamePrefix() {
        return _ServiceNamePrefix;
    }

    public void setServiceNamePrefix(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ServiceNamePrefix = value;
    }

    public int getVersion() {
        return _Version;
    }

    public void setVersion(int value) {
        _Version = value;
    }

    public int getKey() {
        return _Key;
    }

    public void setKey(int value) {
        _Key = value;
    }

    public boolean isNoOneByOne() {
        return _NoOneByOne;
    }

    public void setNoOneByOne(boolean value) {
        _NoOneByOne = value;
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
        var bean = new Zeze.Builtin.ProviderDirect.BModuleRedirectArgument();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BModuleRedirectArgument)other);
    }

    public void assign(BModuleRedirectArgument other) {
        _ModuleId = other.getModuleId();
        _HashCode = other.getHashCode();
        _RedirectType = other.getRedirectType();
        _MethodFullName = other.getMethodFullName();
        _Params = other.getParams();
        _ServiceNamePrefix = other.getServiceNamePrefix();
        _Version = other.getVersion();
        _Key = other.getKey();
        _NoOneByOne = other.isNoOneByOne();
    }

    public void assign(BModuleRedirectArgument.Data other) {
        _ModuleId = other._ModuleId;
        _HashCode = other._HashCode;
        _RedirectType = other._RedirectType;
        _MethodFullName = other._MethodFullName;
        _Params = other._Params;
        _ServiceNamePrefix = other._ServiceNamePrefix;
        _Version = other._Version;
        _Key = other._Key;
        _NoOneByOne = other._NoOneByOne;
    }

    @Override
    public BModuleRedirectArgument.Data copy() {
        var copy = new BModuleRedirectArgument.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BModuleRedirectArgument.Data a, BModuleRedirectArgument.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
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
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ProviderDirect.BModuleRedirectArgument: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ModuleId=").append(_ModuleId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HashCode=").append(_HashCode).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RedirectType=").append(_RedirectType).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("MethodFullName=").append(_MethodFullName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Params=").append(_Params).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceNamePrefix=").append(_ServiceNamePrefix).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Version=").append(_Version).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_Key).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NoOneByOne=").append(_NoOneByOne).append(System.lineSeparator());
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
