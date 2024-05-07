// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BModuleRedirectAllResult extends Zeze.Transaction.Bean implements BModuleRedirectAllResultReadOnly {
    public static final long TYPEID = -6979067915808179070L;

    private int _ModuleId;
    private int _ServerId; // 目标server的id。
    private long _SourceProvider; // 从BModuleRedirectAllRequest里面得到。
    private String _MethodFullName; // format="ModuleFullName:MethodName"
    private long _SessionId; // 发起请求者初始化，返回结果时带回。
    private final Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash> _Hashs; // 发送给具体进程时需要处理的分组hash-index（目前由linkd填写）

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
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServerId;
        var log = (Log__ServerId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int value) {
        if (!isManaged()) {
            _ServerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServerId(this, 2, value));
    }

    @Override
    public long getSourceProvider() {
        if (!isManaged())
            return _SourceProvider;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SourceProvider;
        var log = (Log__SourceProvider)txn.getLog(objectId() + 3);
        return log != null ? log.value : _SourceProvider;
    }

    public void setSourceProvider(long value) {
        if (!isManaged()) {
            _SourceProvider = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__SourceProvider(this, 3, value));
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

    public Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash> getHashs() {
        return _Hashs;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHashReadOnly> getHashsReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Hashs);
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectAllResult() {
        _MethodFullName = "";
        _Hashs = new Zeze.Transaction.Collections.PMap2<>(Integer.class, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.class);
        _Hashs.variableId(6);
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
        _Hashs = new Zeze.Transaction.Collections.PMap2<>(Integer.class, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.class);
        _Hashs.variableId(6);
    }

    @Override
    public void reset() {
        setModuleId(0);
        setServerId(0);
        setSourceProvider(0);
        setMethodFullName("");
        setSessionId(0);
        _Hashs.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult.Data toData() {
        var data = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult.Data)other);
    }

    public void assign(BModuleRedirectAllResult.Data other) {
        setModuleId(other._ModuleId);
        setServerId(other._ServerId);
        setSourceProvider(other._SourceProvider);
        setMethodFullName(other._MethodFullName);
        setSessionId(other._SessionId);
        _Hashs.clear();
        for (var e : other._Hashs.entrySet()) {
            Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash data = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash();
            data.assign(e.getValue());
            _Hashs.put(e.getKey(), data);
        }
        _unknown_ = null;
    }

    public void assign(BModuleRedirectAllResult other) {
        setModuleId(other.getModuleId());
        setServerId(other.getServerId());
        setSourceProvider(other.getSourceProvider());
        setMethodFullName(other.getMethodFullName());
        setSessionId(other.getSessionId());
        _Hashs.clear();
        for (var e : other._Hashs.entrySet())
            _Hashs.put(e.getKey(), e.getValue().copy());
        _unknown_ = other._unknown_;
    }

    public BModuleRedirectAllResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BModuleRedirectAllResult copy() {
        var copy = new BModuleRedirectAllResult();
        copy.assign(this);
        return copy;
    }

    public static void swap(BModuleRedirectAllResult a, BModuleRedirectAllResult b) {
        BModuleRedirectAllResult save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ModuleId extends Zeze.Transaction.Logs.LogInt {
        public Log__ModuleId(BModuleRedirectAllResult bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllResult)getBelong())._ModuleId = value; }
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BModuleRedirectAllResult bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllResult)getBelong())._ServerId = value; }
    }

    private static final class Log__SourceProvider extends Zeze.Transaction.Logs.LogLong {
        public Log__SourceProvider(BModuleRedirectAllResult bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllResult)getBelong())._SourceProvider = value; }
    }

    private static final class Log__MethodFullName extends Zeze.Transaction.Logs.LogString {
        public Log__MethodFullName(BModuleRedirectAllResult bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllResult)getBelong())._MethodFullName = value; }
    }

    private static final class Log__SessionId extends Zeze.Transaction.Logs.LogLong {
        public Log__SessionId(BModuleRedirectAllResult bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllResult)getBelong())._SessionId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ModuleId=").append(getModuleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId=").append(getServerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SourceProvider=").append(getSourceProvider()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("MethodFullName=").append(getMethodFullName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SessionId=").append(getSessionId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Hashs={");
        if (!_Hashs.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Hashs.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(System.lineSeparator());
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
            var _x_ = _Hashs;
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
            var _x_ = _Hashs;
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Hashs.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Hashs.initRootInfoWithRedo(root, this);
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
        for (var _v_ : _Hashs.values()) {
            if (_v_.negativeCheck())
                return true;
        }
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
                case 2: _ServerId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _SourceProvider = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _MethodFullName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 5: _SessionId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 6: _Hashs.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setModuleId(rs.getInt(_parents_name_ + "ModuleId"));
        setServerId(rs.getInt(_parents_name_ + "ServerId"));
        setSourceProvider(rs.getLong(_parents_name_ + "SourceProvider"));
        setMethodFullName(rs.getString(_parents_name_ + "MethodFullName"));
        if (getMethodFullName() == null)
            setMethodFullName("");
        setSessionId(rs.getLong(_parents_name_ + "SessionId"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Hashs", _Hashs, rs.getString(_parents_name_ + "Hashs"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "ModuleId", getModuleId());
        st.appendInt(_parents_name_ + "ServerId", getServerId());
        st.appendLong(_parents_name_ + "SourceProvider", getSourceProvider());
        st.appendString(_parents_name_ + "MethodFullName", getMethodFullName());
        st.appendLong(_parents_name_ + "SessionId", getSessionId());
        st.appendString(_parents_name_ + "Hashs", Zeze.Serialize.Helper.encodeJson(_Hashs));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ModuleId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ServerId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "SourceProvider", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "MethodFullName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "SessionId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "Hashs", "map", "int", "Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash"));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6979067915808179070L;

    private int _ModuleId;
    private int _ServerId; // 目标server的id。
    private long _SourceProvider; // 从BModuleRedirectAllRequest里面得到。
    private String _MethodFullName; // format="ModuleFullName:MethodName"
    private long _SessionId; // 发起请求者初始化，返回结果时带回。
    private java.util.HashMap<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data> _Hashs; // 发送给具体进程时需要处理的分组hash-index（目前由linkd填写）

    public int getModuleId() {
        return _ModuleId;
    }

    public void setModuleId(int value) {
        _ModuleId = value;
    }

    public int getServerId() {
        return _ServerId;
    }

    public void setServerId(int value) {
        _ServerId = value;
    }

    public long getSourceProvider() {
        return _SourceProvider;
    }

    public void setSourceProvider(long value) {
        _SourceProvider = value;
    }

    public String getMethodFullName() {
        return _MethodFullName;
    }

    public void setMethodFullName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _MethodFullName = value;
    }

    public long getSessionId() {
        return _SessionId;
    }

    public void setSessionId(long value) {
        _SessionId = value;
    }

    public java.util.HashMap<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data> getHashs() {
        return _Hashs;
    }

    public void setHashs(java.util.HashMap<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Hashs = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _MethodFullName = "";
        _Hashs = new java.util.HashMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(int _ModuleId_, int _ServerId_, long _SourceProvider_, String _MethodFullName_, long _SessionId_, java.util.HashMap<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data> _Hashs_) {
        _ModuleId = _ModuleId_;
        _ServerId = _ServerId_;
        _SourceProvider = _SourceProvider_;
        if (_MethodFullName_ == null)
            _MethodFullName_ = "";
        _MethodFullName = _MethodFullName_;
        _SessionId = _SessionId_;
        if (_Hashs_ == null)
            _Hashs_ = new java.util.HashMap<>();
        _Hashs = _Hashs_;
    }

    @Override
    public void reset() {
        _ModuleId = 0;
        _ServerId = 0;
        _SourceProvider = 0;
        _MethodFullName = "";
        _SessionId = 0;
        _Hashs.clear();
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult toBean() {
        var bean = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BModuleRedirectAllResult)other);
    }

    public void assign(BModuleRedirectAllResult other) {
        _ModuleId = other.getModuleId();
        _ServerId = other.getServerId();
        _SourceProvider = other.getSourceProvider();
        _MethodFullName = other.getMethodFullName();
        _SessionId = other.getSessionId();
        _Hashs.clear();
        for (var e : other._Hashs.entrySet()) {
            Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data data = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data();
            data.assign(e.getValue());
            _Hashs.put(e.getKey(), data);
        }
    }

    public void assign(BModuleRedirectAllResult.Data other) {
        _ModuleId = other._ModuleId;
        _ServerId = other._ServerId;
        _SourceProvider = other._SourceProvider;
        _MethodFullName = other._MethodFullName;
        _SessionId = other._SessionId;
        _Hashs.clear();
        for (var e : other._Hashs.entrySet())
            _Hashs.put(e.getKey(), e.getValue().copy());
    }

    @Override
    public BModuleRedirectAllResult.Data copy() {
        var copy = new BModuleRedirectAllResult.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BModuleRedirectAllResult.Data a, BModuleRedirectAllResult.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
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
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ModuleId=").append(_ModuleId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId=").append(_ServerId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SourceProvider=").append(_SourceProvider).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("MethodFullName=").append(_MethodFullName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SessionId=").append(_SessionId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Hashs={");
        if (!_Hashs.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Hashs.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(System.lineSeparator());
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
            var _x_ = _Hashs;
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
            var _x_ = _Hashs;
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
}
}
