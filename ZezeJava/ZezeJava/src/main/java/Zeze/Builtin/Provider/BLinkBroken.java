// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BLinkBroken extends Zeze.Transaction.Bean {
    public static final int REASON_PEERCLOSE = 0;

    private String _account;
    private long _linkSid;
    private int _reason;
    private String _context; // SetUserState
    private Zeze.Net.Binary _contextx; // SetUserState

    public String getAccount() {
        if (!isManaged())
            return _account;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _account;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__account)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.Value : _account;
    }

    public void setAccount(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _account = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__account(this, 1, value));
    }

    public long getLinkSid() {
        if (!isManaged())
            return _linkSid;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _linkSid;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__linkSid)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.Value : _linkSid;
    }

    public void setLinkSid(long value) {
        if (!isManaged()) {
            _linkSid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__linkSid(this, 2, value));
    }

    public int getReason() {
        if (!isManaged())
            return _reason;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _reason;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__reason)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.Value : _reason;
    }

    public void setReason(int value) {
        if (!isManaged()) {
            _reason = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__reason(this, 3, value));
    }

    public String getContext() {
        if (!isManaged())
            return _context;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _context;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__context)txn.GetLog(this.getObjectId() + 5);
        return log != null ? log.Value : _context;
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
        txn.PutLog(new Log__context(this, 5, value));
    }

    public Zeze.Net.Binary getContextx() {
        if (!isManaged())
            return _contextx;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _contextx;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__contextx)txn.GetLog(this.getObjectId() + 6);
        return log != null ? log.Value : _contextx;
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
        txn.PutLog(new Log__contextx(this, 6, value));
    }

    public BLinkBroken() {
         this(0);
    }

    public BLinkBroken(int _varId_) {
        super(_varId_);
        _account = "";
        _context = "";
        _contextx = Zeze.Net.Binary.Empty;
    }

    public void Assign(BLinkBroken other) {
        setAccount(other.getAccount());
        setLinkSid(other.getLinkSid());
        setReason(other.getReason());
        setContext(other.getContext());
        setContextx(other.getContextx());
    }

    public BLinkBroken CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BLinkBroken Copy() {
        var copy = new BLinkBroken();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BLinkBroken a, BLinkBroken b) {
        BLinkBroken save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 1424702393060691138L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__account extends Zeze.Transaction.Logs.LogString {
        public Log__account(BLinkBroken bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BLinkBroken)getBelong())._account = Value; }
    }

    private static final class Log__linkSid extends Zeze.Transaction.Logs.LogLong {
        public Log__linkSid(BLinkBroken bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BLinkBroken)getBelong())._linkSid = Value; }
    }

    private static final class Log__reason extends Zeze.Transaction.Logs.LogInt {
        public Log__reason(BLinkBroken bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BLinkBroken)getBelong())._reason = Value; }
    }

    private static final class Log__context extends Zeze.Transaction.Logs.LogString {
        public Log__context(BLinkBroken bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BLinkBroken)getBelong())._context = Value; }
    }

    private static final class Log__contextx extends Zeze.Transaction.Logs.LogBinary {
        public Log__contextx(BLinkBroken bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BLinkBroken)getBelong())._contextx = Value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BLinkBroken: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("account").append('=').append(getAccount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("linkSid").append('=').append(getLinkSid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("reason").append('=').append(getReason()).append(',').append(System.lineSeparator());
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
            String _x_ = getAccount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getLinkSid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getReason();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getContext();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getContextx();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
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
            setAccount(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLinkSid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setReason(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while ((_t_ & 0xff) > 1 && _i_ < 5) {
            _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setContext(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
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
        if (getReason() < 0)
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void FollowerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _account = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 2: _linkSid = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 3: _reason = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
                case 5: _context = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 6: _contextx = ((Zeze.Transaction.Logs.LogBinary)vlog).Value; break;
            }
        }
    }
}
