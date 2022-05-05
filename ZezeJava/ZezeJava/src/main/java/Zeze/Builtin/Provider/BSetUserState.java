// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BSetUserState extends Zeze.Transaction.Bean {
    private long _linkSid;
    private String _context;
    private Zeze.Net.Binary _contextx;

    public long getLinkSid() {
        if (!isManaged())
            return _linkSid;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _linkSid;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__linkSid)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _linkSid;
    }

    public void setLinkSid(long value) {
        if (!isManaged()) {
            _linkSid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__linkSid(this, value));
    }

    public String getContext() {
        if (!isManaged())
            return _context;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _context;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__context)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _context;
    }

    public void setContext(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _context = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__context(this, value));
    }

    public Zeze.Net.Binary getContextx() {
        if (!isManaged())
            return _contextx;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _contextx;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__contextx)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _contextx;
    }

    public void setContextx(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _contextx = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__contextx(this, value));
    }

    public BSetUserState() {
         this(0);
    }

    public BSetUserState(int _varId_) {
        super(_varId_);
        _context = "";
        _contextx = Zeze.Net.Binary.Empty;
    }

    public void Assign(BSetUserState other) {
        setLinkSid(other.getLinkSid());
        setContext(other.getContext());
        setContextx(other.getContextx());
    }

    public BSetUserState CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BSetUserState Copy() {
        var copy = new BSetUserState();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BSetUserState a, BSetUserState b) {
        BSetUserState save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -4860388989628287875L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__linkSid extends Zeze.Transaction.Log1<BSetUserState, Long> {
        public Log__linkSid(BSetUserState self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._linkSid = this.getValue(); }
    }

    private static final class Log__context extends Zeze.Transaction.Log1<BSetUserState, String> {
        public Log__context(BSetUserState self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._context = this.getValue(); }
    }

    private static final class Log__contextx extends Zeze.Transaction.Log1<BSetUserState, Zeze.Net.Binary> {
        public Log__contextx(BSetUserState self, Zeze.Net.Binary value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._contextx = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BSetUserState: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("linkSid").append('=').append(getLinkSid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("context").append('=').append(getContext()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("contextx").append('=').append(getContextx()).append(System.lineSeparator());
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
            String _x_ = getContext();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getContextx();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setLinkSid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setContext(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setContextx(_o_.ReadBinary(_t_));
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

    @Override
    public boolean NegativeCheck() {
        if (getLinkSid() < 0)
            return true;
        return false;
    }
}
