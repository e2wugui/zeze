// auto-generated @formatter:off
package Zeze.Beans.Provider;

import Zeze.Serialize.ByteBuffer;

public final class BAnnounceProviderInfo extends Zeze.Transaction.Bean {
    private String _ServiceNamePrefix;
    private String _ServiceIndentity;

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

    public BAnnounceProviderInfo() {
         this(0);
    }

    public BAnnounceProviderInfo(int _varId_) {
        super(_varId_);
        _ServiceNamePrefix = "";
        _ServiceIndentity = "";
    }

    public void Assign(BAnnounceProviderInfo other) {
        setServiceNamePrefix(other.getServiceNamePrefix());
        setServiceIndentity(other.getServiceIndentity());
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
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceIndentity").append('=').append(getServiceIndentity()).append(System.lineSeparator());
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
        return false;
    }
}
