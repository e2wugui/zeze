// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BModuleRedirectAllRequest extends Zeze.Transaction.Bean {
    private int _ModuleId;
    private int _HashCodeConcurrentLevel; // 总的并发分组数量
    private final Zeze.Transaction.Collections.PSet1<Integer> _HashCodes; // 发送给具体进程时需要处理的分组hash-index（目前由linkd填写）
    private long _SourceProvider; // linkd 转发的时候填写本地provider的sessionId。
    private long _SessionId; // 发起请求者初始化，返回结果时带回。
    private String _MethodFullName; // format="ModuleFullName:MethodName"
    private Zeze.Net.Binary _Params;
    private String _ServiceNamePrefix;

    public int getModuleId() {
        if (!isManaged())
            return _ModuleId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ModuleId;
        var log = (Log__ModuleId)txn.GetLog(objectId() + 1);
        return log != null ? log.Value : _ModuleId;
    }

    public void setModuleId(int value) {
        if (!isManaged()) {
            _ModuleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__ModuleId(this, 1, value));
    }

    public int getHashCodeConcurrentLevel() {
        if (!isManaged())
            return _HashCodeConcurrentLevel;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HashCodeConcurrentLevel;
        var log = (Log__HashCodeConcurrentLevel)txn.GetLog(objectId() + 2);
        return log != null ? log.Value : _HashCodeConcurrentLevel;
    }

    public void setHashCodeConcurrentLevel(int value) {
        if (!isManaged()) {
            _HashCodeConcurrentLevel = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__HashCodeConcurrentLevel(this, 2, value));
    }

    public Zeze.Transaction.Collections.PSet1<Integer> getHashCodes() {
        return _HashCodes;
    }

    public long getSourceProvider() {
        if (!isManaged())
            return _SourceProvider;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SourceProvider;
        var log = (Log__SourceProvider)txn.GetLog(objectId() + 4);
        return log != null ? log.Value : _SourceProvider;
    }

    public void setSourceProvider(long value) {
        if (!isManaged()) {
            _SourceProvider = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__SourceProvider(this, 4, value));
    }

    public long getSessionId() {
        if (!isManaged())
            return _SessionId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SessionId;
        var log = (Log__SessionId)txn.GetLog(objectId() + 5);
        return log != null ? log.Value : _SessionId;
    }

    public void setSessionId(long value) {
        if (!isManaged()) {
            _SessionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__SessionId(this, 5, value));
    }

    public String getMethodFullName() {
        if (!isManaged())
            return _MethodFullName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _MethodFullName;
        var log = (Log__MethodFullName)txn.GetLog(objectId() + 6);
        return log != null ? log.Value : _MethodFullName;
    }

    public void setMethodFullName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _MethodFullName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__MethodFullName(this, 6, value));
    }

    public Zeze.Net.Binary getParams() {
        if (!isManaged())
            return _Params;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Params;
        var log = (Log__Params)txn.GetLog(objectId() + 7);
        return log != null ? log.Value : _Params;
    }

    public void setParams(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Params = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__Params(this, 7, value));
    }

    public String getServiceNamePrefix() {
        if (!isManaged())
            return _ServiceNamePrefix;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServiceNamePrefix;
        var log = (Log__ServiceNamePrefix)txn.GetLog(objectId() + 8);
        return log != null ? log.Value : _ServiceNamePrefix;
    }

    public void setServiceNamePrefix(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceNamePrefix = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__ServiceNamePrefix(this, 8, value));
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
    public BModuleRedirectAllRequest(int _ModuleId_, int _HashCodeConcurrentLevel_, long _SourceProvider_, long _SessionId_, String _MethodFullName_, Zeze.Net.Binary _Params_, String _ServiceNamePrefix_) {
        _ModuleId = _ModuleId_;
        _HashCodeConcurrentLevel = _HashCodeConcurrentLevel_;
        _HashCodes = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _HashCodes.variableId(3);
        _SourceProvider = _SourceProvider_;
        _SessionId = _SessionId_;
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

    public void assign(BModuleRedirectAllRequest other) {
        setModuleId(other.getModuleId());
        setHashCodeConcurrentLevel(other.getHashCodeConcurrentLevel());
        getHashCodes().clear();
        for (var e : other.getHashCodes())
            getHashCodes().add(e);
        setSourceProvider(other.getSourceProvider());
        setSessionId(other.getSessionId());
        setMethodFullName(other.getMethodFullName());
        setParams(other.getParams());
        setServiceNamePrefix(other.getServiceNamePrefix());
    }

    @Deprecated
    public void Assign(BModuleRedirectAllRequest other) {
        assign(other);
    }

    public BModuleRedirectAllRequest copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BModuleRedirectAllRequest copy() {
        var copy = new BModuleRedirectAllRequest();
        copy.Assign(this);
        return copy;
    }

    @Deprecated
    public BModuleRedirectAllRequest Copy() {
        return copy();
    }

    public static void swap(BModuleRedirectAllRequest a, BModuleRedirectAllRequest b) {
        BModuleRedirectAllRequest save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BModuleRedirectAllRequest copyBean() {
        return Copy();
    }

    public static final long TYPEID = -1938324199607833342L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ModuleId extends Zeze.Transaction.Logs.LogInt {
        public Log__ModuleId(BModuleRedirectAllRequest bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._ModuleId = Value; }
    }

    private static final class Log__HashCodeConcurrentLevel extends Zeze.Transaction.Logs.LogInt {
        public Log__HashCodeConcurrentLevel(BModuleRedirectAllRequest bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._HashCodeConcurrentLevel = Value; }
    }

    private static final class Log__SourceProvider extends Zeze.Transaction.Logs.LogLong {
        public Log__SourceProvider(BModuleRedirectAllRequest bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._SourceProvider = Value; }
    }

    private static final class Log__SessionId extends Zeze.Transaction.Logs.LogLong {
        public Log__SessionId(BModuleRedirectAllRequest bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._SessionId = Value; }
    }

    private static final class Log__MethodFullName extends Zeze.Transaction.Logs.LogString {
        public Log__MethodFullName(BModuleRedirectAllRequest bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._MethodFullName = Value; }
    }

    private static final class Log__Params extends Zeze.Transaction.Logs.LogBinary {
        public Log__Params(BModuleRedirectAllRequest bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._Params = Value; }
    }

    private static final class Log__ServiceNamePrefix extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceNamePrefix(BModuleRedirectAllRequest bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllRequest)getBelong())._ServiceNamePrefix = Value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("ModuleId").append('=').append(getModuleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HashCodeConcurrentLevel").append('=').append(getHashCodeConcurrentLevel()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HashCodes").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getHashCodes()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(_item_).append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SourceProvider").append('=').append(getSourceProvider()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SessionId").append('=').append(getSessionId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("MethodFullName").append('=').append(getMethodFullName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Params").append('=').append(getParams()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceNamePrefix").append('=').append(getServiceNamePrefix()).append(System.lineSeparator());
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
            int _x_ = getHashCodeConcurrentLevel();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getHashCodes();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_)
                    _o_.WriteLong(_v_);
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
            setHashCodeConcurrentLevel(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = getHashCodes();
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
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _HashCodes.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _HashCodes.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getModuleId() < 0)
            return true;
        if (getHashCodeConcurrentLevel() < 0)
            return true;
        for (var _v_ : getHashCodes()) {
            if (_v_ < 0)
                return true;
        }
        if (getSourceProvider() < 0)
            return true;
        if (getSessionId() < 0)
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
                case 1: _ModuleId = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
                case 2: _HashCodeConcurrentLevel = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
                case 3: _HashCodes.followerApply(vlog); break;
                case 4: _SourceProvider = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 5: _SessionId = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 6: _MethodFullName = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 7: _Params = ((Zeze.Transaction.Logs.LogBinary)vlog).Value; break;
                case 8: _ServiceNamePrefix = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
            }
        }
    }
}
