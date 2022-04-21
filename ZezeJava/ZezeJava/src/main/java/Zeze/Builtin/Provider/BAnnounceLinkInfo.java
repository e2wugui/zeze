// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

public final class BAnnounceLinkInfo extends Zeze.Transaction.Bean {
    private int _LinkId; // reserve
    private long _ProviderSessionId;

    public int getLinkId() {
        if (!isManaged())
            return _LinkId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _LinkId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__LinkId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _LinkId;
    }

    public void setLinkId(int value) {
        if (!isManaged()) {
            _LinkId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__LinkId(this, value));
    }

    public long getProviderSessionId() {
        if (!isManaged())
            return _ProviderSessionId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ProviderSessionId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ProviderSessionId)txn.GetLog(this.getObjectId() + 2);
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

    public BAnnounceLinkInfo() {
         this(0);
    }

    public BAnnounceLinkInfo(int _varId_) {
        super(_varId_);
    }

    public void Assign(BAnnounceLinkInfo other) {
        setLinkId(other.getLinkId());
        setProviderSessionId(other.getProviderSessionId());
    }

    public BAnnounceLinkInfo CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BAnnounceLinkInfo Copy() {
        var copy = new BAnnounceLinkInfo();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BAnnounceLinkInfo a, BAnnounceLinkInfo b) {
        BAnnounceLinkInfo save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 6291432069805514560L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__LinkId extends Zeze.Transaction.Log1<BAnnounceLinkInfo, Integer> {
        public Log__LinkId(BAnnounceLinkInfo self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._LinkId = this.getValue(); }
    }

    private static final class Log__ProviderSessionId extends Zeze.Transaction.Log1<BAnnounceLinkInfo, Long> {
        public Log__ProviderSessionId(BAnnounceLinkInfo self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BAnnounceLinkInfo: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LinkId").append('=').append(getLinkId()).append(',').append(System.lineSeparator());
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
            int _x_ = getLinkId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getProviderSessionId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setLinkId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
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
        if (getLinkId() < 0)
            return true;
        if (getProviderSessionId() < 0)
            return true;
        return false;
    }
}
