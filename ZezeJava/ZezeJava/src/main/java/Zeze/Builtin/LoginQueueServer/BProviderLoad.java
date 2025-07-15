// auto-generated @formatter:off
package Zeze.Builtin.LoginQueueServer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BProviderLoad extends Zeze.Transaction.Bean implements BProviderLoadReadOnly {
    public static final long TYPEID = -4725395130921506944L;

    private int _ServerId;
    private String _ServiceIp; // LinkdService公开给客户端的Ip或者ProviderIp(Redirect)。
    private int _ServicePort;
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Provider.BLoad> _Load;

    private static final java.lang.invoke.VarHandle vh_ServerId;
    private static final java.lang.invoke.VarHandle vh_ServiceIp;
    private static final java.lang.invoke.VarHandle vh_ServicePort;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ServerId = _l_.findVarHandle(BProviderLoad.class, "_ServerId", int.class);
            vh_ServiceIp = _l_.findVarHandle(BProviderLoad.class, "_ServiceIp", String.class);
            vh_ServicePort = _l_.findVarHandle(BProviderLoad.class, "_ServicePort", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServerId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int _v_) {
        if (!isManaged()) {
            _ServerId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_ServerId, _v_));
    }

    @Override
    public String getServiceIp() {
        if (!isManaged())
            return _ServiceIp;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServiceIp;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.stringValue() : _ServiceIp;
    }

    public void setServiceIp(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceIp = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_ServiceIp, _v_));
    }

    @Override
    public int getServicePort() {
        if (!isManaged())
            return _ServicePort;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServicePort;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _ServicePort;
    }

    public void setServicePort(int _v_) {
        if (!isManaged()) {
            _ServicePort = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_ServicePort, _v_));
    }

    public Zeze.Builtin.Provider.BLoad getLoad() {
        return _Load.getValue();
    }

    public void setLoad(Zeze.Builtin.Provider.BLoad _v_) {
        _Load.setValue(_v_);
    }

    @Override
    public Zeze.Builtin.Provider.BLoadReadOnly getLoadReadOnly() {
        return _Load.getValue();
    }

    @SuppressWarnings("deprecation")
    public BProviderLoad() {
        _ServiceIp = "";
        _Load = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Provider.BLoad(), Zeze.Builtin.Provider.BLoad.class);
        _Load.variableId(4);
    }

    @SuppressWarnings("deprecation")
    public BProviderLoad(int _ServerId_, String _ServiceIp_, int _ServicePort_) {
        _ServerId = _ServerId_;
        if (_ServiceIp_ == null)
            _ServiceIp_ = "";
        _ServiceIp = _ServiceIp_;
        _ServicePort = _ServicePort_;
        _Load = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Provider.BLoad(), Zeze.Builtin.Provider.BLoad.class);
        _Load.variableId(4);
    }

    @Override
    public void reset() {
        setServerId(0);
        setServiceIp("");
        setServicePort(0);
        _Load.reset();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LoginQueueServer.BProviderLoad.Data toData() {
        var _d_ = new Zeze.Builtin.LoginQueueServer.BProviderLoad.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.LoginQueueServer.BProviderLoad.Data)_o_);
    }

    public void assign(BProviderLoad.Data _o_) {
        setServerId(_o_._ServerId);
        setServiceIp(_o_._ServiceIp);
        setServicePort(_o_._ServicePort);
        var _d__Load = new Zeze.Builtin.Provider.BLoad();
        _d__Load.assign(_o_._Load);
        _Load.setValue(_d__Load);
        _unknown_ = null;
    }

    public void assign(BProviderLoad _o_) {
        setServerId(_o_.getServerId());
        setServiceIp(_o_.getServiceIp());
        setServicePort(_o_.getServicePort());
        _Load.assign(_o_._Load);
        _unknown_ = _o_._unknown_;
    }

    public BProviderLoad copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BProviderLoad copy() {
        var _c_ = new BProviderLoad();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BProviderLoad _a_, BProviderLoad _b_) {
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
        _s_.append("Zeze.Builtin.LoginQueueServer.BProviderLoad: {\n");
        _s_.append(_i1_).append("ServerId=").append(getServerId()).append(",\n");
        _s_.append(_i1_).append("ServiceIp=").append(getServiceIp()).append(",\n");
        _s_.append(_i1_).append("ServicePort=").append(getServicePort()).append(",\n");
        _s_.append(_i1_).append("Load=");
        _Load.buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getServiceIp();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getServicePort();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 4, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Load.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
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
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setServiceIp(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setServicePort(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadBean(_Load, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BProviderLoad))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BProviderLoad)_o_;
        if (getServerId() != _b_.getServerId())
            return false;
        if (!getServiceIp().equals(_b_.getServiceIp()))
            return false;
        if (getServicePort() != _b_.getServicePort())
            return false;
        if (!_Load.equals(_b_._Load))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Load.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Load.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getServerId() < 0)
            return true;
        if (getServicePort() < 0)
            return true;
        if (_Load.negativeCheck())
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
                case 1: _ServerId = _v_.intValue(); break;
                case 2: _ServiceIp = _v_.stringValue(); break;
                case 3: _ServicePort = _v_.intValue(); break;
                case 4: _Load.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setServerId(_r_.getInt(_pn_ + "ServerId"));
        setServiceIp(_r_.getString(_pn_ + "ServiceIp"));
        if (getServiceIp() == null)
            setServiceIp("");
        setServicePort(_r_.getInt(_pn_ + "ServicePort"));
        _p_.add("Load");
        _Load.decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "ServerId", getServerId());
        _s_.appendString(_pn_ + "ServiceIp", getServiceIp());
        _s_.appendInt(_pn_ + "ServicePort", getServicePort());
        _p_.add("Load");
        _Load.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServerId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ServiceIp", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ServicePort", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Load", "Zeze.Builtin.Provider.BLoad", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4725395130921506944L;

    private int _ServerId;
    private String _ServiceIp; // LinkdService公开给客户端的Ip或者ProviderIp(Redirect)。
    private int _ServicePort;
    private Zeze.Builtin.Provider.BLoad.Data _Load;

    public int getServerId() {
        return _ServerId;
    }

    public void setServerId(int _v_) {
        _ServerId = _v_;
    }

    public String getServiceIp() {
        return _ServiceIp;
    }

    public void setServiceIp(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ServiceIp = _v_;
    }

    public int getServicePort() {
        return _ServicePort;
    }

    public void setServicePort(int _v_) {
        _ServicePort = _v_;
    }

    public Zeze.Builtin.Provider.BLoad.Data getLoad() {
        return _Load;
    }

    public void setLoad(Zeze.Builtin.Provider.BLoad.Data _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Load = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ServiceIp = "";
        _Load = new Zeze.Builtin.Provider.BLoad.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(int _ServerId_, String _ServiceIp_, int _ServicePort_, Zeze.Builtin.Provider.BLoad.Data _Load_) {
        _ServerId = _ServerId_;
        if (_ServiceIp_ == null)
            _ServiceIp_ = "";
        _ServiceIp = _ServiceIp_;
        _ServicePort = _ServicePort_;
        if (_Load_ == null)
            _Load_ = new Zeze.Builtin.Provider.BLoad.Data();
        _Load = _Load_;
    }

    @Override
    public void reset() {
        _ServerId = 0;
        _ServiceIp = "";
        _ServicePort = 0;
        _Load.reset();
    }

    @Override
    public Zeze.Builtin.LoginQueueServer.BProviderLoad toBean() {
        var _b_ = new Zeze.Builtin.LoginQueueServer.BProviderLoad();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BProviderLoad)_o_);
    }

    public void assign(BProviderLoad _o_) {
        _ServerId = _o_.getServerId();
        _ServiceIp = _o_.getServiceIp();
        _ServicePort = _o_.getServicePort();
        _Load.assign(_o_._Load.getValue());
    }

    public void assign(BProviderLoad.Data _o_) {
        _ServerId = _o_._ServerId;
        _ServiceIp = _o_._ServiceIp;
        _ServicePort = _o_._ServicePort;
        _Load.assign(_o_._Load);
    }

    @Override
    public BProviderLoad.Data copy() {
        var _c_ = new BProviderLoad.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BProviderLoad.Data _a_, BProviderLoad.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BProviderLoad.Data clone() {
        return (BProviderLoad.Data)super.clone();
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
        _s_.append("Zeze.Builtin.LoginQueueServer.BProviderLoad: {\n");
        _s_.append(_i1_).append("ServerId=").append(_ServerId).append(",\n");
        _s_.append(_i1_).append("ServiceIp=").append(_ServiceIp).append(",\n");
        _s_.append(_i1_).append("ServicePort=").append(_ServicePort).append(",\n");
        _s_.append(_i1_).append("Load=");
        _Load.buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
            int _x_ = _ServerId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _ServiceIp;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = _ServicePort;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 4, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Load.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ServerId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ServiceIp = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _ServicePort = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadBean(_Load, _t_);
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
        if (!(_o_ instanceof BProviderLoad.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BProviderLoad.Data)_o_;
        if (_ServerId != _b_._ServerId)
            return false;
        if (!_ServiceIp.equals(_b_._ServiceIp))
            return false;
        if (_ServicePort != _b_._ServicePort)
            return false;
        if (!_Load.equals(_b_._Load))
            return false;
        return true;
    }
}
}
