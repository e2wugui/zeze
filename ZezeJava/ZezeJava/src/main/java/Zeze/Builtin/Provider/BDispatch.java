// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

// link to gs
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BDispatch extends Zeze.Transaction.Bean implements BDispatchReadOnly {
    public static final long TYPEID = -496680173908943081L;

    private long _linkSid;
    private String _account;
    private long _protocolType;
    private Zeze.Net.Binary _protocolData; // 协议打包，不包括 type, size
    private String _context; // SetUserState
    private Zeze.Net.Binary _contextx; // SetUserState
    private String _onlineSetName; // SetUserState

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public String getOnlineSetName() {
        if (!isManaged())
            return _onlineSetName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _onlineSetName;
        var log = (Log__onlineSetName)txn.getLog(objectId() + 7);
        return log != null ? log.value : _onlineSetName;
    }

    public void setOnlineSetName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _onlineSetName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__onlineSetName(this, 7, value));
    }

    @SuppressWarnings("deprecation")
    public BDispatch() {
        _account = "";
        _protocolData = Zeze.Net.Binary.Empty;
        _context = "";
        _contextx = Zeze.Net.Binary.Empty;
        _onlineSetName = "";
    }

    @SuppressWarnings("deprecation")
    public BDispatch(long _linkSid_, String _account_, long _protocolType_, Zeze.Net.Binary _protocolData_, String _context_, Zeze.Net.Binary _contextx_, String _onlineSetName_) {
        _linkSid = _linkSid_;
        if (_account_ == null)
            _account_ = "";
        _account = _account_;
        _protocolType = _protocolType_;
        if (_protocolData_ == null)
            _protocolData_ = Zeze.Net.Binary.Empty;
        _protocolData = _protocolData_;
        if (_context_ == null)
            _context_ = "";
        _context = _context_;
        if (_contextx_ == null)
            _contextx_ = Zeze.Net.Binary.Empty;
        _contextx = _contextx_;
        if (_onlineSetName_ == null)
            _onlineSetName_ = "";
        _onlineSetName = _onlineSetName_;
    }

    @Override
    public void reset() {
        setLinkSid(0);
        setAccount("");
        setProtocolType(0);
        setProtocolData(Zeze.Net.Binary.Empty);
        setContext("");
        setContextx(Zeze.Net.Binary.Empty);
        setOnlineSetName("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BDispatch.Data toData() {
        var data = new Zeze.Builtin.Provider.BDispatch.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Provider.BDispatch.Data)other);
    }

    public void assign(BDispatch.Data other) {
        setLinkSid(other._linkSid);
        setAccount(other._account);
        setProtocolType(other._protocolType);
        setProtocolData(other._protocolData);
        setContext(other._context);
        setContextx(other._contextx);
        setOnlineSetName(other._onlineSetName);
        _unknown_ = null;
    }

    public void assign(BDispatch other) {
        setLinkSid(other.getLinkSid());
        setAccount(other.getAccount());
        setProtocolType(other.getProtocolType());
        setProtocolData(other.getProtocolData());
        setContext(other.getContext());
        setContextx(other.getContextx());
        setOnlineSetName(other.getOnlineSetName());
        _unknown_ = other._unknown_;
    }

    public BDispatch copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDispatch copy() {
        var copy = new BDispatch();
        copy.assign(this);
        return copy;
    }

    public static void swap(BDispatch a, BDispatch b) {
        BDispatch save = a.copy();
        a.assign(b);
        b.assign(save);
    }

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

    private static final class Log__onlineSetName extends Zeze.Transaction.Logs.LogString {
        public Log__onlineSetName(BDispatch bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDispatch)getBelong())._onlineSetName = value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("linkSid=").append(getLinkSid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("account=").append(getAccount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("protocolType=").append(getProtocolType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("protocolData=").append(getProtocolData()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("context=").append(getContext()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("contextx=").append(getContextx()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("onlineSetName=").append(getOnlineSetName()).append(System.lineSeparator());
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
        {
            String _x_ = getOnlineSetName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
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
        if (_i_ == 7) {
            setOnlineSetName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
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
                case 7: _onlineSetName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setLinkSid(rs.getLong(_parents_name_ + "linkSid"));
        setAccount(rs.getString(_parents_name_ + "account"));
        if (getAccount() == null)
            setAccount("");
        setProtocolType(rs.getLong(_parents_name_ + "protocolType"));
        setProtocolData(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "protocolData")));
        if (getProtocolData() == null)
            setProtocolData(Zeze.Net.Binary.Empty);
        setContext(rs.getString(_parents_name_ + "context"));
        if (getContext() == null)
            setContext("");
        setContextx(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "contextx")));
        if (getContextx() == null)
            setContextx(Zeze.Net.Binary.Empty);
        setOnlineSetName(rs.getString(_parents_name_ + "onlineSetName"));
        if (getOnlineSetName() == null)
            setOnlineSetName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "linkSid", getLinkSid());
        st.appendString(_parents_name_ + "account", getAccount());
        st.appendLong(_parents_name_ + "protocolType", getProtocolType());
        st.appendBinary(_parents_name_ + "protocolData", getProtocolData());
        st.appendString(_parents_name_ + "context", getContext());
        st.appendBinary(_parents_name_ + "contextx", getContextx());
        st.appendString(_parents_name_ + "onlineSetName", getOnlineSetName());
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BDispatch
    }

// link to gs
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -496680173908943081L;

    private long _linkSid;
    private String _account;
    private long _protocolType;
    private Zeze.Net.Binary _protocolData; // 协议打包，不包括 type, size
    private String _context; // SetUserState
    private Zeze.Net.Binary _contextx; // SetUserState
    private String _onlineSetName; // SetUserState

    public long getLinkSid() {
        return _linkSid;
    }

    public void setLinkSid(long value) {
        _linkSid = value;
    }

    public String getAccount() {
        return _account;
    }

    public void setAccount(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _account = value;
    }

    public long getProtocolType() {
        return _protocolType;
    }

    public void setProtocolType(long value) {
        _protocolType = value;
    }

    public Zeze.Net.Binary getProtocolData() {
        return _protocolData;
    }

    public void setProtocolData(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _protocolData = value;
    }

    public String getContext() {
        return _context;
    }

    public void setContext(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _context = value;
    }

    public Zeze.Net.Binary getContextx() {
        return _contextx;
    }

    public void setContextx(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _contextx = value;
    }

    public String getOnlineSetName() {
        return _onlineSetName;
    }

    public void setOnlineSetName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _onlineSetName = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _account = "";
        _protocolData = Zeze.Net.Binary.Empty;
        _context = "";
        _contextx = Zeze.Net.Binary.Empty;
        _onlineSetName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(long _linkSid_, String _account_, long _protocolType_, Zeze.Net.Binary _protocolData_, String _context_, Zeze.Net.Binary _contextx_, String _onlineSetName_) {
        _linkSid = _linkSid_;
        if (_account_ == null)
            _account_ = "";
        _account = _account_;
        _protocolType = _protocolType_;
        if (_protocolData_ == null)
            _protocolData_ = Zeze.Net.Binary.Empty;
        _protocolData = _protocolData_;
        if (_context_ == null)
            _context_ = "";
        _context = _context_;
        if (_contextx_ == null)
            _contextx_ = Zeze.Net.Binary.Empty;
        _contextx = _contextx_;
        if (_onlineSetName_ == null)
            _onlineSetName_ = "";
        _onlineSetName = _onlineSetName_;
    }

    @Override
    public void reset() {
        _linkSid = 0;
        _account = "";
        _protocolType = 0;
        _protocolData = Zeze.Net.Binary.Empty;
        _context = "";
        _contextx = Zeze.Net.Binary.Empty;
        _onlineSetName = "";
    }

    @Override
    public Zeze.Builtin.Provider.BDispatch toBean() {
        var bean = new Zeze.Builtin.Provider.BDispatch();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BDispatch)other);
    }

    public void assign(BDispatch other) {
        _linkSid = other.getLinkSid();
        _account = other.getAccount();
        _protocolType = other.getProtocolType();
        _protocolData = other.getProtocolData();
        _context = other.getContext();
        _contextx = other.getContextx();
        _onlineSetName = other.getOnlineSetName();
    }

    public void assign(BDispatch.Data other) {
        _linkSid = other._linkSid;
        _account = other._account;
        _protocolType = other._protocolType;
        _protocolData = other._protocolData;
        _context = other._context;
        _contextx = other._contextx;
        _onlineSetName = other._onlineSetName;
    }

    @Override
    public BDispatch.Data copy() {
        var copy = new BDispatch.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BDispatch.Data a, BDispatch.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BDispatch.Data clone() {
        return (BDispatch.Data)super.clone();
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
        sb.append(Zeze.Util.Str.indent(level)).append("linkSid=").append(_linkSid).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("account=").append(_account).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("protocolType=").append(_protocolType).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("protocolData=").append(_protocolData).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("context=").append(_context).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("contextx=").append(_contextx).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("onlineSetName=").append(_onlineSetName).append(System.lineSeparator());
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
            long _x_ = _linkSid;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = _account;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = _protocolType;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _protocolData;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = _context;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _contextx;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = _onlineSetName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.BYTES);
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
            _linkSid = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _account = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _protocolType = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _protocolData = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _context = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _contextx = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            _onlineSetName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
