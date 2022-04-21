// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;

public final class BTransmitContext extends Zeze.Transaction.Bean {
    private long _LinkSid;
    private int _ProviderId;
    private long _ProviderSessionId;

    public long getLinkSid() {
        if (!isManaged())
            return _LinkSid;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _LinkSid;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__LinkSid)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _LinkSid;
    }

    public void setLinkSid(long value) {
        if (!isManaged()) {
            _LinkSid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__LinkSid(this, value));
    }

    public int getProviderId() {
        if (!isManaged())
            return _ProviderId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ProviderId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ProviderId)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _ProviderId;
    }

    public void setProviderId(int value) {
        if (!isManaged()) {
            _ProviderId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ProviderId(this, value));
    }

    public long getProviderSessionId() {
        if (!isManaged())
            return _ProviderSessionId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ProviderSessionId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ProviderSessionId)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _ProviderSessionId;
    }

    public void setProviderSessionId(long value) {
        if (!isManaged()) {
            _ProviderSessionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ProviderSessionId(this, value));
    }

    public BTransmitContext() {
         this(0);
    }

    public BTransmitContext(int _varId_) {
        super(_varId_);
    }

    public void Assign(BTransmitContext other) {
        setLinkSid(other.getLinkSid());
        setProviderId(other.getProviderId());
        setProviderSessionId(other.getProviderSessionId());
    }

    public BTransmitContext CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BTransmitContext Copy() {
        var copy = new BTransmitContext();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BTransmitContext a, BTransmitContext b) {
        BTransmitContext save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 606535654553096889L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__LinkSid extends Zeze.Transaction.Log1<BTransmitContext, Long> {
        public Log__LinkSid(BTransmitContext self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._LinkSid = this.getValue(); }
    }

    private static final class Log__ProviderId extends Zeze.Transaction.Log1<BTransmitContext, Integer> {
        public Log__ProviderId(BTransmitContext self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._ProviderId = this.getValue(); }
    }

    private static final class Log__ProviderSessionId extends Zeze.Transaction.Log1<BTransmitContext, Long> {
        public Log__ProviderSessionId(BTransmitContext self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._ProviderSessionId = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ProviderDirect.BTransmitContext: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LinkSid").append('=').append(getLinkSid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderId").append('=').append(getProviderId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderSessionId").append('=').append(getProviderSessionId()).append(System.lineSeparator());
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
            long _x_ = getLinkSid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getProviderId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getProviderSessionId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            setLinkSid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setProviderId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setProviderSessionId(_o_.ReadLong(_t_));
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
        if (getLinkSid() < 0)
            return true;
        if (getProviderId() < 0)
            return true;
        if (getProviderSessionId() < 0)
            return true;
        return false;
    }
}
