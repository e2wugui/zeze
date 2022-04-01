// auto-generated @formatter:off
package Zeze.Beans.Provider2;

import Zeze.Serialize.ByteBuffer;

public final class BModuleRedirectArgument extends Zeze.Transaction.Bean {
    private int _ModuleId;
    private int _HashCode; // server 计算。see BBind.ChoiceType。
    private int _RedirectType; // 如果是ToServer，ServerId存在HashCode中。
    private String _MethodFullName; // format="ModuleFullName:MethodName"
    private Zeze.Net.Binary _Params;
    private String _ServiceNamePrefix;

    public int getModuleId() {
        if (!isManaged())
            return _ModuleId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ModuleId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ModuleId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _ModuleId;
    }

    public void setModuleId(int value) {
        if (!isManaged()) {
            _ModuleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ModuleId(this, value));
    }

    public int getHashCode() {
        if (!isManaged())
            return _HashCode;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _HashCode;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__HashCode)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _HashCode;
    }

    public void setHashCode(int value) {
        if (!isManaged()) {
            _HashCode = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__HashCode(this, value));
    }

    public int getRedirectType() {
        if (!isManaged())
            return _RedirectType;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _RedirectType;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__RedirectType)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _RedirectType;
    }

    public void setRedirectType(int value) {
        if (!isManaged()) {
            _RedirectType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__RedirectType(this, value));
    }

    public String getMethodFullName() {
        if (!isManaged())
            return _MethodFullName;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _MethodFullName;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__MethodFullName)txn.GetLog(this.getObjectId() + 4);
        return log != null ? log.getValue() : _MethodFullName;
    }

    public void setMethodFullName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _MethodFullName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__MethodFullName(this, value));
    }

    public Zeze.Net.Binary getParams() {
        if (!isManaged())
            return _Params;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Params;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Params)txn.GetLog(this.getObjectId() + 5);
        return log != null ? log.getValue() : _Params;
    }

    public void setParams(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Params = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Params(this, value));
    }

    public String getServiceNamePrefix() {
        if (!isManaged())
            return _ServiceNamePrefix;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ServiceNamePrefix;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ServiceNamePrefix)txn.GetLog(this.getObjectId() + 6);
        return log != null ? log.getValue() : _ServiceNamePrefix;
    }

    public void setServiceNamePrefix(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceNamePrefix = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ServiceNamePrefix(this, value));
    }

    public BModuleRedirectArgument() {
         this(0);
    }

    public BModuleRedirectArgument(int _varId_) {
        super(_varId_);
        _MethodFullName = "";
        _Params = Zeze.Net.Binary.Empty;
        _ServiceNamePrefix = "";
    }

    public void Assign(BModuleRedirectArgument other) {
        setModuleId(other.getModuleId());
        setHashCode(other.getHashCode());
        setRedirectType(other.getRedirectType());
        setMethodFullName(other.getMethodFullName());
        setParams(other.getParams());
        setServiceNamePrefix(other.getServiceNamePrefix());
    }

    public BModuleRedirectArgument CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BModuleRedirectArgument Copy() {
        var copy = new BModuleRedirectArgument();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BModuleRedirectArgument a, BModuleRedirectArgument b) {
        BModuleRedirectArgument save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 4310056222124983648L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__ModuleId extends Zeze.Transaction.Log1<BModuleRedirectArgument, Integer> {
        public Log__ModuleId(BModuleRedirectArgument self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._ModuleId = this.getValue(); }
    }

    private static final class Log__HashCode extends Zeze.Transaction.Log1<BModuleRedirectArgument, Integer> {
        public Log__HashCode(BModuleRedirectArgument self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._HashCode = this.getValue(); }
    }

    private static final class Log__RedirectType extends Zeze.Transaction.Log1<BModuleRedirectArgument, Integer> {
        public Log__RedirectType(BModuleRedirectArgument self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._RedirectType = this.getValue(); }
    }

    private static final class Log__MethodFullName extends Zeze.Transaction.Log1<BModuleRedirectArgument, String> {
        public Log__MethodFullName(BModuleRedirectArgument self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 4; }
        @Override
        public void Commit() { this.getBeanTyped()._MethodFullName = this.getValue(); }
    }

    private static final class Log__Params extends Zeze.Transaction.Log1<BModuleRedirectArgument, Zeze.Net.Binary> {
        public Log__Params(BModuleRedirectArgument self, Zeze.Net.Binary value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 5; }
        @Override
        public void Commit() { this.getBeanTyped()._Params = this.getValue(); }
    }

    private static final class Log__ServiceNamePrefix extends Zeze.Transaction.Log1<BModuleRedirectArgument, String> {
        public Log__ServiceNamePrefix(BModuleRedirectArgument self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 6; }
        @Override
        public void Commit() { this.getBeanTyped()._ServiceNamePrefix = this.getValue(); }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Beans.Provider2.BModuleRedirectArgument: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ModuleId").append('=').append(getModuleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HashCode").append('=').append(getHashCode()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RedirectType").append('=').append(getRedirectType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("MethodFullName").append('=').append(getMethodFullName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Params").append('=').append(getParams()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceNamePrefix").append('=').append(getServiceNamePrefix()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Encode(ByteBuffer _o_) {
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

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Decode(ByteBuffer _o_) {
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
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean NegativeCheck() {
        if (getModuleId() < 0)
            return true;
        if (getHashCode() < 0)
            return true;
        if (getRedirectType() < 0)
            return true;
        return false;
    }
}
