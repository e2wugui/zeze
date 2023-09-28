// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
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
    public int getHashCodeConcurrentLevel() {
        if (!isManaged())
            return _HashCodeConcurrentLevel;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HashCodeConcurrentLevel;
        var log = (Log__HashCodeConcurrentLevel)txn.getLog(objectId() + 2);
        return log != null ? log.value : _HashCodeConcurrentLevel;
    }

    public void setHashCodeConcurrentLevel(int value) {
        if (!isManaged()) {
            _HashCodeConcurrentLevel = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__HashCodeConcurrentLevel(this, 2, value));
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
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SourceProvider;
        var log = (Log__SourceProvider)txn.getLog(objectId() + 4);
        return log != null ? log.value : _SourceProvider;
    }

    public void setSourceProvider(long value) {
        if (!isManaged()) {
            _SourceProvider = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__SourceProvider(this, 4, value));
    }

    @Override
    public long getSessionId() {
        if (!isManaged())
            return _SessionId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SessionId;
        var log = (Log__SessionId)txn.getLog(objectId() + 5);
        return log != null ? log.value : _SessionId;
    }

    public void setSessionId(long value) {
        if (!isManaged()) {
            _SessionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__SessionId(this, 5, value));
    }

    @Override
    public String getMethodFullName() {
        if (!isManaged())
            return _MethodFullName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _MethodFullName;
        var log = (Log__MethodFullName)txn.getLog(objectId() + 6);
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
        txn.putLog(new Log__MethodFullName(this, 6, value));
    }

    @Override
    public Zeze.Net.Binary getParams() {
        if (!isManaged())
            return _Params;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Params;
        var log = (Log__Params)txn.getLog(objectId() + 7);
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
        txn.putLog(new Log__Params(this, 7, value));
    }

    @Override
    public String getServiceNamePrefix() {
        if (!isManaged())
            return _ServiceNamePrefix;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServiceNamePrefix;
        var log = (Log__ServiceNamePrefix)txn.getLog(objectId() + 8);
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
        txn.putLog(new Log__ServiceNamePrefix(this, 8, value));
    }

    @Override
    public int getVersion() {
        if (!isManaged())
            return _Version;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Version;
        var log = (Log__Version)txn.getLog(objectId() + 9);
        return log != null ? log.value : _Version;
    }

    public void setVersion(int value) {
        if (!isManaged()) {
            _Version = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Version(this, 9, value));
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
        var data = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest.Data)other);
    }

    public void assign(BModuleRedirectAllRequest.Data other) {
        setModuleId(other._ModuleId);
        setHashCodeConcurrentLevel(other._HashCodeConcurrentLevel);
        _HashCodes.clear();
        _HashCodes.addAll(other._HashCodes);
        setSourceProvider(other._SourceProvider);
        setSessionId(other._SessionId);
        setMethodFullName(other._MethodFullName);
        setParams(other._Params);
        setServiceNamePrefix(other._ServiceNamePrefix);
        setVersion(other._Version);
        _unknown_ = null;
    }

    public void assign(BModuleRedirectAllRequest other) {
        setModuleId(other.getModuleId());
        setHashCodeConcurrentLevel(other.getHashCodeConcurrentLevel());
        _HashCodes.clear();
        _HashCodes.addAll(other._HashCodes);
        setSourceProvider(other.getSourceProvider());
        setSessionId(other.getSessionId());
        setMethodFullName(other.getMethodFullName());
        setParams(other.getParams());
        setServiceNamePrefix(other.getServiceNamePrefix());
        setVersion(other.getVersion());
        _unknown_ = other._unknown_;
    }

    public BModuleRedirectAllRequest copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BModuleRedirectAllRequest copy() {
        var copy = new BModuleRedirectAllRequest();
        copy.assign(this);
        return copy;
    }

    public static void swap(BModuleRedirectAllRequest a, BModuleRedirectAllRequest b) {
        BModuleRedirectAllRequest save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ModuleId extends Zeze.Transaction.Logs.LogInt {
        public Log__ModuleId(BModuleRedirectAllRequest bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._ModuleId = value; }
    }

    private static final class Log__HashCodeConcurrentLevel extends Zeze.Transaction.Logs.LogInt {
        public Log__HashCodeConcurrentLevel(BModuleRedirectAllRequest bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._HashCodeConcurrentLevel = value; }
    }

    private static final class Log__SourceProvider extends Zeze.Transaction.Logs.LogLong {
        public Log__SourceProvider(BModuleRedirectAllRequest bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._SourceProvider = value; }
    }

    private static final class Log__SessionId extends Zeze.Transaction.Logs.LogLong {
        public Log__SessionId(BModuleRedirectAllRequest bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._SessionId = value; }
    }

    private static final class Log__MethodFullName extends Zeze.Transaction.Logs.LogString {
        public Log__MethodFullName(BModuleRedirectAllRequest bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._MethodFullName = value; }
    }

    private static final class Log__Params extends Zeze.Transaction.Logs.LogBinary {
        public Log__Params(BModuleRedirectAllRequest bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._Params = value; }
    }

    private static final class Log__ServiceNamePrefix extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceNamePrefix(BModuleRedirectAllRequest bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._ServiceNamePrefix = value; }
    }

    private static final class Log__Version extends Zeze.Transaction.Logs.LogInt {
        public Log__Version(BModuleRedirectAllRequest bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._Version = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ModuleId=").append(getModuleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HashCodeConcurrentLevel=").append(getHashCodeConcurrentLevel()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HashCodes={");
        if (!_HashCodes.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _HashCodes) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SourceProvider=").append(getSourceProvider()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SessionId=").append(getSessionId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("MethodFullName=").append(getMethodFullName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Params=").append(getParams()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceNamePrefix=").append(getServiceNamePrefix()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Version=").append(getVersion()).append(System.lineSeparator());
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
    public void decode(ByteBuffer _o_) {
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _HashCodes.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _HashCodes.initRootInfoWithRedo(root, this);
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _ModuleId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _HashCodeConcurrentLevel = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _HashCodes.followerApply(vlog); break;
                case 4: _SourceProvider = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 5: _SessionId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 6: _MethodFullName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 7: _Params = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 8: _ServiceNamePrefix = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 9: _Version = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setModuleId(rs.getInt(_parents_name_ + "ModuleId"));
        setHashCodeConcurrentLevel(rs.getInt(_parents_name_ + "HashCodeConcurrentLevel"));
        Zeze.Serialize.Helper.decodeJsonSet(_HashCodes, Integer.class, rs.getString(_parents_name_ + "HashCodes"));
        setSourceProvider(rs.getLong(_parents_name_ + "SourceProvider"));
        setSessionId(rs.getLong(_parents_name_ + "SessionId"));
        setMethodFullName(rs.getString(_parents_name_ + "MethodFullName"));
        if (getMethodFullName() == null)
            setMethodFullName("");
        setParams(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Params")));
        if (getParams() == null)
            setParams(Zeze.Net.Binary.Empty);
        setServiceNamePrefix(rs.getString(_parents_name_ + "ServiceNamePrefix"));
        if (getServiceNamePrefix() == null)
            setServiceNamePrefix("");
        setVersion(rs.getInt(_parents_name_ + "Version"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "ModuleId", getModuleId());
        st.appendInt(_parents_name_ + "HashCodeConcurrentLevel", getHashCodeConcurrentLevel());
        st.appendString(_parents_name_ + "HashCodes", Zeze.Serialize.Helper.encodeJson(_HashCodes));
        st.appendLong(_parents_name_ + "SourceProvider", getSourceProvider());
        st.appendLong(_parents_name_ + "SessionId", getSessionId());
        st.appendString(_parents_name_ + "MethodFullName", getMethodFullName());
        st.appendBinary(_parents_name_ + "Params", getParams());
        st.appendString(_parents_name_ + "ServiceNamePrefix", getServiceNamePrefix());
        st.appendInt(_parents_name_ + "Version", getVersion());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ModuleId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "HashCodeConcurrentLevel", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "HashCodes", "set", "", "int"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "SourceProvider", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "SessionId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "MethodFullName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "Params", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "ServiceNamePrefix", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(9, "Version", "int", "", ""));
        return vars;
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

    public void setModuleId(int value) {
        _ModuleId = value;
    }

    public int getHashCodeConcurrentLevel() {
        return _HashCodeConcurrentLevel;
    }

    public void setHashCodeConcurrentLevel(int value) {
        _HashCodeConcurrentLevel = value;
    }

    public java.util.HashSet<Integer> getHashCodes() {
        return _HashCodes;
    }

    public void setHashCodes(java.util.HashSet<Integer> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _HashCodes = value;
    }

    public long getSourceProvider() {
        return _SourceProvider;
    }

    public void setSourceProvider(long value) {
        _SourceProvider = value;
    }

    public long getSessionId() {
        return _SessionId;
    }

    public void setSessionId(long value) {
        _SessionId = value;
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
        var bean = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BModuleRedirectAllRequest)other);
    }

    public void assign(BModuleRedirectAllRequest other) {
        _ModuleId = other.getModuleId();
        _HashCodeConcurrentLevel = other.getHashCodeConcurrentLevel();
        _HashCodes.clear();
        _HashCodes.addAll(other._HashCodes);
        _SourceProvider = other.getSourceProvider();
        _SessionId = other.getSessionId();
        _MethodFullName = other.getMethodFullName();
        _Params = other.getParams();
        _ServiceNamePrefix = other.getServiceNamePrefix();
        _Version = other.getVersion();
    }

    public void assign(BModuleRedirectAllRequest.Data other) {
        _ModuleId = other._ModuleId;
        _HashCodeConcurrentLevel = other._HashCodeConcurrentLevel;
        _HashCodes.clear();
        _HashCodes.addAll(other._HashCodes);
        _SourceProvider = other._SourceProvider;
        _SessionId = other._SessionId;
        _MethodFullName = other._MethodFullName;
        _Params = other._Params;
        _ServiceNamePrefix = other._ServiceNamePrefix;
        _Version = other._Version;
    }

    @Override
    public BModuleRedirectAllRequest.Data copy() {
        var copy = new BModuleRedirectAllRequest.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BModuleRedirectAllRequest.Data a, BModuleRedirectAllRequest.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
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
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ModuleId=").append(_ModuleId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HashCodeConcurrentLevel=").append(_HashCodeConcurrentLevel).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HashCodes={");
        if (!_HashCodes.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _HashCodes) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SourceProvider=").append(_SourceProvider).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SessionId=").append(_SessionId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("MethodFullName=").append(_MethodFullName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Params=").append(_Params).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceNamePrefix=").append(_ServiceNamePrefix).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Version=").append(_Version).append(System.lineSeparator());
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
    public void decode(ByteBuffer _o_) {
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
