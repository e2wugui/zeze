// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// link to gs
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BDispatch extends Zeze.Transaction.Bean implements BDispatchReadOnly {
    public static final long TYPEID = -496680173908943081L;

    private long _linkSid;
    private String _account;
    private long _protocolType;
    private Zeze.Net.Binary _protocolData; // 协议打包，不包括 type, size
    private String _context; // SetUserState
    private Zeze.Net.Binary _contextx; // SetUserState
    private String _onlineSetName; // SetUserState

    private static final java.lang.invoke.VarHandle vh_linkSid;
    private static final java.lang.invoke.VarHandle vh_account;
    private static final java.lang.invoke.VarHandle vh_protocolType;
    private static final java.lang.invoke.VarHandle vh_protocolData;
    private static final java.lang.invoke.VarHandle vh_context;
    private static final java.lang.invoke.VarHandle vh_contextx;
    private static final java.lang.invoke.VarHandle vh_onlineSetName;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_linkSid = _l_.findVarHandle(BDispatch.class, "_linkSid", long.class);
            vh_account = _l_.findVarHandle(BDispatch.class, "_account", String.class);
            vh_protocolType = _l_.findVarHandle(BDispatch.class, "_protocolType", long.class);
            vh_protocolData = _l_.findVarHandle(BDispatch.class, "_protocolData", Zeze.Net.Binary.class);
            vh_context = _l_.findVarHandle(BDispatch.class, "_context", String.class);
            vh_contextx = _l_.findVarHandle(BDispatch.class, "_contextx", Zeze.Net.Binary.class);
            vh_onlineSetName = _l_.findVarHandle(BDispatch.class, "_onlineSetName", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getLinkSid() {
        if (!isManaged())
            return _linkSid;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _linkSid;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _linkSid;
    }

    public void setLinkSid(long _v_) {
        if (!isManaged()) {
            _linkSid = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_linkSid, _v_));
    }

    @Override
    public String getAccount() {
        if (!isManaged())
            return _account;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _account;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.stringValue() : _account;
    }

    public void setAccount(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _account = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_account, _v_));
    }

    @Override
    public long getProtocolType() {
        if (!isManaged())
            return _protocolType;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _protocolType;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _protocolType;
    }

    public void setProtocolType(long _v_) {
        if (!isManaged()) {
            _protocolType = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_protocolType, _v_));
    }

    @Override
    public Zeze.Net.Binary getProtocolData() {
        if (!isManaged())
            return _protocolData;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _protocolData;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _protocolData;
    }

    public void setProtocolData(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _protocolData = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 4, vh_protocolData, _v_));
    }

    @Override
    public String getContext() {
        if (!isManaged())
            return _context;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _context;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 5);
        return log != null ? log.stringValue() : _context;
    }

    public void setContext(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _context = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 5, vh_context, _v_));
    }

    @Override
    public Zeze.Net.Binary getContextx() {
        if (!isManaged())
            return _contextx;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _contextx;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _contextx;
    }

    public void setContextx(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _contextx = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 6, vh_contextx, _v_));
    }

    @Override
    public String getOnlineSetName() {
        if (!isManaged())
            return _onlineSetName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _onlineSetName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 7);
        return log != null ? log.stringValue() : _onlineSetName;
    }

    public void setOnlineSetName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _onlineSetName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 7, vh_onlineSetName, _v_));
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
        var _d_ = new Zeze.Builtin.Provider.BDispatch.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Provider.BDispatch.Data)_o_);
    }

    public void assign(BDispatch.Data _o_) {
        setLinkSid(_o_._linkSid);
        setAccount(_o_._account);
        setProtocolType(_o_._protocolType);
        setProtocolData(_o_._protocolData);
        setContext(_o_._context);
        setContextx(_o_._contextx);
        setOnlineSetName(_o_._onlineSetName);
        _unknown_ = null;
    }

    public void assign(BDispatch _o_) {
        setLinkSid(_o_.getLinkSid());
        setAccount(_o_.getAccount());
        setProtocolType(_o_.getProtocolType());
        setProtocolData(_o_.getProtocolData());
        setContext(_o_.getContext());
        setContextx(_o_.getContextx());
        setOnlineSetName(_o_.getOnlineSetName());
        _unknown_ = _o_._unknown_;
    }

    public BDispatch copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDispatch copy() {
        var _c_ = new BDispatch();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BDispatch _a_, BDispatch _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.Provider.BDispatch: {\n");
        _s_.append(_i1_).append("linkSid=").append(getLinkSid()).append(",\n");
        _s_.append(_i1_).append("account=").append(getAccount()).append(",\n");
        _s_.append(_i1_).append("protocolType=").append(getProtocolType()).append(",\n");
        _s_.append(_i1_).append("protocolData=").append(getProtocolData()).append(",\n");
        _s_.append(_i1_).append("context=").append(getContext()).append(",\n");
        _s_.append(_i1_).append("contextx=").append(getContextx()).append(",\n");
        _s_.append(_i1_).append("onlineSetName=").append(getOnlineSetName()).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
    public void decode(IByteBuffer _o_) {
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
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BDispatch))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BDispatch)_o_;
        if (getLinkSid() != _b_.getLinkSid())
            return false;
        if (!getAccount().equals(_b_.getAccount()))
            return false;
        if (getProtocolType() != _b_.getProtocolType())
            return false;
        if (!getProtocolData().equals(_b_.getProtocolData()))
            return false;
        if (!getContext().equals(_b_.getContext()))
            return false;
        if (!getContextx().equals(_b_.getContextx()))
            return false;
        if (!getOnlineSetName().equals(_b_.getOnlineSetName()))
            return false;
        return true;
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
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _linkSid = _v_.longValue(); break;
                case 2: _account = _v_.stringValue(); break;
                case 3: _protocolType = _v_.longValue(); break;
                case 4: _protocolData = _v_.binaryValue(); break;
                case 5: _context = _v_.stringValue(); break;
                case 6: _contextx = _v_.binaryValue(); break;
                case 7: _onlineSetName = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setLinkSid(_r_.getLong(_pn_ + "linkSid"));
        setAccount(_r_.getString(_pn_ + "account"));
        if (getAccount() == null)
            setAccount("");
        setProtocolType(_r_.getLong(_pn_ + "protocolType"));
        setProtocolData(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "protocolData")));
        setContext(_r_.getString(_pn_ + "context"));
        if (getContext() == null)
            setContext("");
        setContextx(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "contextx")));
        setOnlineSetName(_r_.getString(_pn_ + "onlineSetName"));
        if (getOnlineSetName() == null)
            setOnlineSetName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "linkSid", getLinkSid());
        _s_.appendString(_pn_ + "account", getAccount());
        _s_.appendLong(_pn_ + "protocolType", getProtocolType());
        _s_.appendBinary(_pn_ + "protocolData", getProtocolData());
        _s_.appendString(_pn_ + "context", getContext());
        _s_.appendBinary(_pn_ + "contextx", getContextx());
        _s_.appendString(_pn_ + "onlineSetName", getOnlineSetName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "linkSid", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "account", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "protocolType", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "protocolData", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "context", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "contextx", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "onlineSetName", "string", "", ""));
        return _v_;
    }

// link to gs
@SuppressWarnings("ForLoopReplaceableByForEach")
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

    public void setLinkSid(long _v_) {
        _linkSid = _v_;
    }

    public String getAccount() {
        return _account;
    }

    public void setAccount(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _account = _v_;
    }

    public long getProtocolType() {
        return _protocolType;
    }

    public void setProtocolType(long _v_) {
        _protocolType = _v_;
    }

    public Zeze.Net.Binary getProtocolData() {
        return _protocolData;
    }

    public void setProtocolData(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _protocolData = _v_;
    }

    public String getContext() {
        return _context;
    }

    public void setContext(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _context = _v_;
    }

    public Zeze.Net.Binary getContextx() {
        return _contextx;
    }

    public void setContextx(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _contextx = _v_;
    }

    public String getOnlineSetName() {
        return _onlineSetName;
    }

    public void setOnlineSetName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _onlineSetName = _v_;
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
        var _b_ = new Zeze.Builtin.Provider.BDispatch();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BDispatch)_o_);
    }

    public void assign(BDispatch _o_) {
        _linkSid = _o_.getLinkSid();
        _account = _o_.getAccount();
        _protocolType = _o_.getProtocolType();
        _protocolData = _o_.getProtocolData();
        _context = _o_.getContext();
        _contextx = _o_.getContextx();
        _onlineSetName = _o_.getOnlineSetName();
    }

    public void assign(BDispatch.Data _o_) {
        _linkSid = _o_._linkSid;
        _account = _o_._account;
        _protocolType = _o_._protocolType;
        _protocolData = _o_._protocolData;
        _context = _o_._context;
        _contextx = _o_._contextx;
        _onlineSetName = _o_._onlineSetName;
    }

    @Override
    public BDispatch.Data copy() {
        var _c_ = new BDispatch.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BDispatch.Data _a_, BDispatch.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
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
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.Provider.BDispatch: {\n");
        _s_.append(_i1_).append("linkSid=").append(_linkSid).append(",\n");
        _s_.append(_i1_).append("account=").append(_account).append(",\n");
        _s_.append(_i1_).append("protocolType=").append(_protocolType).append(",\n");
        _s_.append(_i1_).append("protocolData=").append(_protocolData).append(",\n");
        _s_.append(_i1_).append("context=").append(_context).append(",\n");
        _s_.append(_i1_).append("contextx=").append(_contextx).append(",\n");
        _s_.append(_i1_).append("onlineSetName=").append(_onlineSetName).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
    public void decode(IByteBuffer _o_) {
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

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BDispatch.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BDispatch.Data)_o_;
        if (_linkSid != _b_._linkSid)
            return false;
        if (!_account.equals(_b_._account))
            return false;
        if (_protocolType != _b_._protocolType)
            return false;
        if (!_protocolData.equals(_b_._protocolData))
            return false;
        if (!_context.equals(_b_._context))
            return false;
        if (!_contextx.equals(_b_._contextx))
            return false;
        if (!_onlineSetName.equals(_b_._onlineSetName))
            return false;
        return true;
    }
}
}
