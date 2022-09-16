// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BDispatch extends Zeze.Transaction.Bean {
    private long _linkSid;
    private String _account;
    private long _protocolType;
    private Zeze.Net.Binary _protocolData; // 协议打包，不包括 type, size
    private String _context; // SetUserState
    private Zeze.Net.Binary _contextx; // SetUserState

    public long getLinkSid() {
        if (!isManaged())
            return _linkSid;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _linkSid;
        var log = (Log__linkSid)txn.getLog(objectId() + 1);
        return log != null ? log.value : _linkSid;
    }

    public void setLinkSid(long value) {
        if (!isManaged()) {
            _linkSid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__linkSid(this, 1, value));
    }

    public String getAccount() {
        if (!isManaged())
            return _account;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _account;
        var log = (Log__account)txn.getLog(objectId() + 2);
        return log != null ? log.value : _account;
    }

    public void setAccount(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _account = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__account(this, 2, value));
    }

    public long getProtocolType() {
        if (!isManaged())
            return _protocolType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _protocolType;
        var log = (Log__protocolType)txn.getLog(objectId() + 3);
        return log != null ? log.value : _protocolType;
    }

    public void setProtocolType(long value) {
        if (!isManaged()) {
            _protocolType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__protocolType(this, 3, value));
    }

    public Zeze.Net.Binary getProtocolData() {
        if (!isManaged())
            return _protocolData;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _protocolData;
        var log = (Log__protocolData)txn.getLog(objectId() + 4);
        return log != null ? log.value : _protocolData;
    }

    public void setProtocolData(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _protocolData = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__protocolData(this, 4, value));
    }

    public String getContext() {
        if (!isManaged())
            return _context;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _context;
        var log = (Log__context)txn.getLog(objectId() + 5);
        return log != null ? log.value : _context;
    }

    public void setContext(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _context = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__context(this, 5, value));
    }

    public Zeze.Net.Binary getContextx() {
        if (!isManaged())
            return _contextx;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _contextx;
        var log = (Log__contextx)txn.getLog(objectId() + 6);
        return log != null ? log.value : _contextx;
    }

    public void setContextx(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _contextx = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__contextx(this, 6, value));
    }

    @SuppressWarnings("deprecation")
    public BDispatch() {
        _account = "";
        _protocolData = Zeze.Net.Binary.Empty;
        _context = "";
        _contextx = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BDispatch(long _linkSid_, String _account_, long _protocolType_, Zeze.Net.Binary _protocolData_, String _context_, Zeze.Net.Binary _contextx_) {
        _linkSid = _linkSid_;
        if (_account_ == null)
            throw new IllegalArgumentException();
        _account = _account_;
        _protocolType = _protocolType_;
        if (_protocolData_ == null)
            throw new IllegalArgumentException();
        _protocolData = _protocolData_;
        if (_context_ == null)
            throw new IllegalArgumentException();
        _context = _context_;
        if (_contextx_ == null)
            throw new IllegalArgumentException();
        _contextx = _contextx_;
    }

    public void assign(BDispatch other) {
        setLinkSid(other.getLinkSid());
        setAccount(other.getAccount());
        setProtocolType(other.getProtocolType());
        setProtocolData(other.getProtocolData());
        setContext(other.getContext());
        setContextx(other.getContextx());
    }

    @Deprecated
    public void Assign(BDispatch other) {
        assign(other);
    }

    public BDispatch copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    public BDispatch copy() {
        var copy = new BDispatch();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BDispatch Copy() {
        return copy();
    }

    public static void swap(BDispatch a, BDispatch b) {
        BDispatch save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public BDispatch copyBean() {
        return copy();
    }

    public static final long TYPEID = -496680173908943081L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__linkSid extends Zeze.Transaction.Logs.LogLong {
        public Log__linkSid(BDispatch bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDispatch)getBelong())._linkSid = value; }
    }

    private static final class Log__account extends Zeze.Transaction.Logs.LogString {
        public Log__account(BDispatch bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDispatch)getBelong())._account = value; }
    }

    private static final class Log__protocolType extends Zeze.Transaction.Logs.LogLong {
        public Log__protocolType(BDispatch bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDispatch)getBelong())._protocolType = value; }
    }

    private static final class Log__protocolData extends Zeze.Transaction.Logs.LogBinary {
        public Log__protocolData(BDispatch bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDispatch)getBelong())._protocolData = value; }
    }

    private static final class Log__context extends Zeze.Transaction.Logs.LogString {
        public Log__context(BDispatch bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDispatch)getBelong())._context = value; }
    }

    private static final class Log__contextx extends Zeze.Transaction.Logs.LogBinary {
        public Log__contextx(BDispatch bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDispatch)getBelong())._contextx = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BDispatch: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("linkSid").append('=').append(getLinkSid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("account").append('=').append(getAccount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("protocolType").append('=').append(getProtocolType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("protocolData").append('=').append(getProtocolData()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("context").append('=').append(getContext()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("contextx").append('=').append(getContextx()).append(System.lineSeparator());
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
            long _x_ = getLinkSid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getAccount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getProtocolType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getProtocolData();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setLinkSid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setAccount(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setProtocolType(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setProtocolData(_o_.ReadBinary(_t_));
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void resetChildrenRootInfo() {
    }

    @Override
    public boolean negativeCheck() {
        if (getLinkSid() < 0)
            return true;
        if (getProtocolType() < 0)
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
                case 1: _linkSid = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _account = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _protocolType = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _protocolData = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 5: _context = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 6: _contextx = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }
}
