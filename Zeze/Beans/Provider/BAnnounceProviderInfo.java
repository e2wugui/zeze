// auto-generated @formatter:off
package Zeze.Beans.Provider;

import Zeze.Serialize.ByteBuffer;

public final class BAnnounceProviderInfo extends Zeze.Transaction.Bean {
    private String _ServiceNamePrefix;
    private String _ServiceIndentity;
    private String _ProviderDirectIp;
    private int _ProviderDirectPort;

    public String getServiceNamePrefix() {
        if (!isManaged())
            return _ServiceNamePrefix;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ServiceNamePrefix;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ServiceNamePrefix)txn.GetLog(this.getObjectId() + 1);
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

    public String getServiceIndentity() {
        if (!isManaged())
            return _ServiceIndentity;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ServiceIndentity;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ServiceIndentity)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _ServiceIndentity;
    }

    public void setServiceIndentity(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceIndentity = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ServiceIndentity(this, value));
    }

    public String getProviderDirectIp() {
        if (!isManaged())
            return _ProviderDirectIp;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ProviderDirectIp;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ProviderDirectIp)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _ProviderDirectIp;
    }

    public void setProviderDirectIp(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ProviderDirectIp = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ProviderDirectIp(this, value));
    }

    public int getProviderDirectPort() {
        if (!isManaged())
            return _ProviderDirectPort;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ProviderDirectPort;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ProviderDirectPort)txn.GetLog(this.getObjectId() + 4);
        return log != null ? log.getValue() : _ProviderDirectPort;
    }

    public void setProviderDirectPort(int value) {
        if (!isManaged()) {
            _ProviderDirectPort = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ProviderDirectPort(this, value));
    }

    public BAnnounceProviderInfo() {
         this(0);
    }

    public BAnnounceProviderInfo(int _varId_) {
        super(_varId_);
        _ServiceNamePrefix = "";
        _ServiceIndentity = "";
        _ProviderDirectIp = "";
    }

    public void Assign(BAnnounceProviderInfo other) {
        setServiceNamePrefix(other.getServiceNamePrefix());
        setServiceIndentity(other.getServiceIndentity());
        setProviderDirectIp(other.getProviderDirectIp());
        setProviderDirectPort(other.getProviderDirectPort());
    }

    public BAnnounceProviderInfo CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BAnnounceProviderInfo Copy() {
        var copy = new BAnnounceProviderInfo();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BAnnounceProviderInfo a, BAnnounceProviderInfo b) {
        BAnnounceProviderInfo save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -4949917838498057735L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__ServiceNamePrefix extends Zeze.Transaction.Log1<BAnnounceProviderInfo, String> {
        public Log__ServiceNamePrefix(BAnnounceProviderInfo self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._ServiceNamePrefix = this.getValue(); }
    }

    private static final class Log__ServiceIndentity extends Zeze.Transaction.Log1<BAnnounceProviderInfo, String> {
        public Log__ServiceIndentity(BAnnounceProviderInfo self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._ServiceIndentity = this.getValue(); }
    }

    private static final class Log__ProviderDirectIp extends Zeze.Transaction.Log1<BAnnounceProviderInfo, String> {
        public Log__ProviderDirectIp(BAnnounceProviderInfo self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._ProviderDirectIp = this.getValue(); }
    }

    private static final class Log__ProviderDirectPort extends Zeze.Transaction.Log1<BAnnounceProviderInfo, Integer> {
        public Log__ProviderDirectPort(BAnnounceProviderInfo self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 4; }
        @Override
        public void Commit() { this.getBeanTyped()._ProviderDirectPort = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Beans.Provider.BAnnounceProviderInfo: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceNamePrefix").append('=').append(getServiceNamePrefix()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceIndentity").append('=').append(getServiceIndentity()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderDirectIp").append('=').append(getProviderDirectIp()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderDirectPort").append('=').append(getProviderDirectPort()).append(System.lineSeparator());
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
        _o_.WriteByte(0);
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Decode(ByteBuffer _o_) {
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
        if (getProviderDirectPort() < 0)
            return true;
        return false;
    }
}
