// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BModuleRedirectArgument extends Zeze.Transaction.Bean implements BModuleRedirectArgumentReadOnly {
    public static final long TYPEID = -5561456902586805165L;

    private int _ModuleId;
    private int _HashCode; // server 计算。see BBind.ChoiceType。
    private int _RedirectType; // 如果是ToServer，ServerId存在HashCode中。
    private String _MethodFullName; // format="ModuleFullName:MethodName"
    private Zeze.Net.Binary _Params;
    private String _ServiceNamePrefix;

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

    @SuppressWarnings("deprecation")
    public BModuleRedirectArgument() {
        _MethodFullName = "";
        _Params = Zeze.Net.Binary.Empty;
        _ServiceNamePrefix = "";
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectArgument(int _ModuleId_, int _HashCode_, int _RedirectType_, String _MethodFullName_, Zeze.Net.Binary _Params_, String _ServiceNamePrefix_) {
        _ModuleId = _ModuleId_;
        _HashCode = _HashCode_;
        _RedirectType = _RedirectType_;
        if (_MethodFullName_ == null)
            throw new IllegalArgumentException();
        _MethodFullName = _MethodFullName_;
        if (_Params_ == null)
            throw new IllegalArgumentException();
        _Params = _Params_;
        if (_ServiceNamePrefix_ == null)
            throw new IllegalArgumentException();
        _ServiceNamePrefix = _ServiceNamePrefix_;
    }

    public void assign(BModuleRedirectArgument other) {
        setModuleId(other.getModuleId());
        setHashCode(other.getHashCode());
        setRedirectType(other.getRedirectType());
        setMethodFullName(other.getMethodFullName());
        setParams(other.getParams());
        setServiceNamePrefix(other.getServiceNamePrefix());
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
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceNamePrefix=").append(getServiceNamePrefix()).append(System.lineSeparator());
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
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
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
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getModuleId() < 0)
            return true;
        if (getHashCode() < 0)
            return true;
        if (getRedirectType() < 0)
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
        if (getParams() == null)
            setParams(Zeze.Net.Binary.Empty);
        setServiceNamePrefix(rs.getString(_parents_name_ + "ServiceNamePrefix"));
        if (getServiceNamePrefix() == null)
            setServiceNamePrefix("");
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
    }
}
