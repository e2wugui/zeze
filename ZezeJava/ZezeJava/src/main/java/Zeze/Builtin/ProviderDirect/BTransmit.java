// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTransmit extends Zeze.Transaction.Bean implements BTransmitReadOnly {
    public static final long TYPEID = 7395081565293443928L;

    private String _ActionName;
    private final Zeze.Transaction.Collections.PSet1<Long> _Roles; // 查询目标角色。
    private long _Sender; // 结果发送给Sender。
    private Zeze.Net.Binary _Parameter; // encoded bean
    private String _OnlineSetName;

    private static final java.lang.invoke.VarHandle vh_ActionName;
    private static final java.lang.invoke.VarHandle vh_Sender;
    private static final java.lang.invoke.VarHandle vh_Parameter;
    private static final java.lang.invoke.VarHandle vh_OnlineSetName;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ActionName = _l_.findVarHandle(BTransmit.class, "_ActionName", String.class);
            vh_Sender = _l_.findVarHandle(BTransmit.class, "_Sender", long.class);
            vh_Parameter = _l_.findVarHandle(BTransmit.class, "_Parameter", Zeze.Net.Binary.class);
            vh_OnlineSetName = _l_.findVarHandle(BTransmit.class, "_OnlineSetName", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getActionName() {
        if (!isManaged())
            return _ActionName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ActionName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ActionName;
    }

    public void setActionName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ActionName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_ActionName, _v_));
    }

    public Zeze.Transaction.Collections.PSet1<Long> getRoles() {
        return _Roles;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Long> getRolesReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_Roles);
    }

    @Override
    public long getSender() {
        if (!isManaged())
            return _Sender;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Sender;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Sender;
    }

    public void setSender(long _v_) {
        if (!isManaged()) {
            _Sender = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_Sender, _v_));
    }

    @Override
    public Zeze.Net.Binary getParameter() {
        if (!isManaged())
            return _Parameter;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Parameter;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Parameter;
    }

    public void setParameter(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Parameter = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 4, vh_Parameter, _v_));
    }

    @Override
    public String getOnlineSetName() {
        if (!isManaged())
            return _OnlineSetName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _OnlineSetName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _OnlineSetName;
    }

    public void setOnlineSetName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _OnlineSetName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 5, vh_OnlineSetName, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTransmit() {
        _ActionName = "";
        _Roles = new Zeze.Transaction.Collections.PSet1<>(Long.class);
        _Roles.variableId(2);
        _Parameter = Zeze.Net.Binary.Empty;
        _OnlineSetName = "";
    }

    @SuppressWarnings("deprecation")
    public BTransmit(String _ActionName_, long _Sender_, Zeze.Net.Binary _Parameter_, String _OnlineSetName_) {
        if (_ActionName_ == null)
            _ActionName_ = "";
        _ActionName = _ActionName_;
        _Roles = new Zeze.Transaction.Collections.PSet1<>(Long.class);
        _Roles.variableId(2);
        _Sender = _Sender_;
        if (_Parameter_ == null)
            _Parameter_ = Zeze.Net.Binary.Empty;
        _Parameter = _Parameter_;
        if (_OnlineSetName_ == null)
            _OnlineSetName_ = "";
        _OnlineSetName = _OnlineSetName_;
    }

    @Override
    public void reset() {
        setActionName("");
        _Roles.clear();
        setSender(0);
        setParameter(Zeze.Net.Binary.Empty);
        setOnlineSetName("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BTransmit.Data toData() {
        var _d_ = new Zeze.Builtin.ProviderDirect.BTransmit.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.ProviderDirect.BTransmit.Data)_o_);
    }

    public void assign(BTransmit.Data _o_) {
        setActionName(_o_._ActionName);
        _Roles.clear();
        _Roles.addAll(_o_._Roles);
        setSender(_o_._Sender);
        setParameter(_o_._Parameter);
        setOnlineSetName(_o_._OnlineSetName);
        _unknown_ = null;
    }

    public void assign(BTransmit _o_) {
        setActionName(_o_.getActionName());
        _Roles.assign(_o_._Roles);
        setSender(_o_.getSender());
        setParameter(_o_.getParameter());
        setOnlineSetName(_o_.getOnlineSetName());
        _unknown_ = _o_._unknown_;
    }

    public BTransmit copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTransmit copy() {
        var _c_ = new BTransmit();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTransmit _a_, BTransmit _b_) {
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
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.ProviderDirect.BTransmit: {\n");
        _s_.append(_i1_).append("ActionName=").append(getActionName()).append(",\n");
        _s_.append(_i1_).append("Roles={");
        if (!_Roles.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _Roles) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("Sender=").append(getSender()).append(",\n");
        _s_.append(_i1_).append("Parameter=").append(getParameter()).append(",\n");
        _s_.append(_i1_).append("OnlineSetName=").append(getOnlineSetName()).append('\n');
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
            String _x_ = getActionName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _Roles;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            long _x_ = getSender();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getParameter();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = getOnlineSetName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
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
            setActionName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Roles;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setSender(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setParameter(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
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
        if (!(_o_ instanceof BTransmit))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTransmit)_o_;
        if (!getActionName().equals(_b_.getActionName()))
            return false;
        if (!_Roles.equals(_b_._Roles))
            return false;
        if (getSender() != _b_.getSender())
            return false;
        if (!getParameter().equals(_b_.getParameter()))
            return false;
        if (!getOnlineSetName().equals(_b_.getOnlineSetName()))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Roles.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Roles.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Roles) {
            if (_v_ < 0)
                return true;
        }
        if (getSender() < 0)
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
                case 1: _ActionName = _v_.stringValue(); break;
                case 2: _Roles.followerApply(_v_); break;
                case 3: _Sender = _v_.longValue(); break;
                case 4: _Parameter = _v_.binaryValue(); break;
                case 5: _OnlineSetName = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setActionName(_r_.getString(_pn_ + "ActionName"));
        if (getActionName() == null)
            setActionName("");
        Zeze.Serialize.Helper.decodeJsonSet(_Roles, Long.class, _r_.getString(_pn_ + "Roles"));
        setSender(_r_.getLong(_pn_ + "Sender"));
        setParameter(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Parameter")));
        setOnlineSetName(_r_.getString(_pn_ + "OnlineSetName"));
        if (getOnlineSetName() == null)
            setOnlineSetName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "ActionName", getActionName());
        _s_.appendString(_pn_ + "Roles", Zeze.Serialize.Helper.encodeJson(_Roles));
        _s_.appendLong(_pn_ + "Sender", getSender());
        _s_.appendBinary(_pn_ + "Parameter", getParameter());
        _s_.appendString(_pn_ + "OnlineSetName", getOnlineSetName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ActionName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Roles", "set", "", "long"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Sender", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Parameter", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "OnlineSetName", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7395081565293443928L;

    private String _ActionName;
    private java.util.HashSet<Long> _Roles; // 查询目标角色。
    private long _Sender; // 结果发送给Sender。
    private Zeze.Net.Binary _Parameter; // encoded bean
    private String _OnlineSetName;

    public String getActionName() {
        return _ActionName;
    }

    public void setActionName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ActionName = _v_;
    }

    public java.util.HashSet<Long> getRoles() {
        return _Roles;
    }

    public void setRoles(java.util.HashSet<Long> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Roles = _v_;
    }

    public long getSender() {
        return _Sender;
    }

    public void setSender(long _v_) {
        _Sender = _v_;
    }

    public Zeze.Net.Binary getParameter() {
        return _Parameter;
    }

    public void setParameter(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Parameter = _v_;
    }

    public String getOnlineSetName() {
        return _OnlineSetName;
    }

    public void setOnlineSetName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _OnlineSetName = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ActionName = "";
        _Roles = new java.util.HashSet<>();
        _Parameter = Zeze.Net.Binary.Empty;
        _OnlineSetName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _ActionName_, java.util.HashSet<Long> _Roles_, long _Sender_, Zeze.Net.Binary _Parameter_, String _OnlineSetName_) {
        if (_ActionName_ == null)
            _ActionName_ = "";
        _ActionName = _ActionName_;
        if (_Roles_ == null)
            _Roles_ = new java.util.HashSet<>();
        _Roles = _Roles_;
        _Sender = _Sender_;
        if (_Parameter_ == null)
            _Parameter_ = Zeze.Net.Binary.Empty;
        _Parameter = _Parameter_;
        if (_OnlineSetName_ == null)
            _OnlineSetName_ = "";
        _OnlineSetName = _OnlineSetName_;
    }

    @Override
    public void reset() {
        _ActionName = "";
        _Roles.clear();
        _Sender = 0;
        _Parameter = Zeze.Net.Binary.Empty;
        _OnlineSetName = "";
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BTransmit toBean() {
        var _b_ = new Zeze.Builtin.ProviderDirect.BTransmit();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BTransmit)_o_);
    }

    public void assign(BTransmit _o_) {
        _ActionName = _o_.getActionName();
        _Roles.clear();
        _Roles.addAll(_o_._Roles);
        _Sender = _o_.getSender();
        _Parameter = _o_.getParameter();
        _OnlineSetName = _o_.getOnlineSetName();
    }

    public void assign(BTransmit.Data _o_) {
        _ActionName = _o_._ActionName;
        _Roles.clear();
        _Roles.addAll(_o_._Roles);
        _Sender = _o_._Sender;
        _Parameter = _o_._Parameter;
        _OnlineSetName = _o_._OnlineSetName;
    }

    @Override
    public BTransmit.Data copy() {
        var _c_ = new BTransmit.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTransmit.Data _a_, BTransmit.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BTransmit.Data clone() {
        return (BTransmit.Data)super.clone();
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
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.ProviderDirect.BTransmit: {\n");
        _s_.append(_i1_).append("ActionName=").append(_ActionName).append(",\n");
        _s_.append(_i1_).append("Roles={");
        if (!_Roles.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _Roles) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("Sender=").append(_Sender).append(",\n");
        _s_.append(_i1_).append("Parameter=").append(_Parameter).append(",\n");
        _s_.append(_i1_).append("OnlineSetName=").append(_OnlineSetName).append('\n');
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
            String _x_ = _ActionName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _Roles;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            long _x_ = _Sender;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Parameter;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = _OnlineSetName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
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
            _ActionName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Roles;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Sender = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Parameter = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _OnlineSetName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
