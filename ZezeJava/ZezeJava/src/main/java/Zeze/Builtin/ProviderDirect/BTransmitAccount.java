// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BTransmitAccount extends Zeze.Transaction.Bean implements BTransmitAccountReadOnly {
    public static final long TYPEID = 2637210793748287339L;

    private String _ActionName;
    private Zeze.Net.Binary _Parameter; // encoded bean
    private final Zeze.Transaction.Collections.PSet1<String> _TargetAccounts; // 查询目标角色。
    private String _SenderAccount; // 结果发送给Sender。
    private String _SenderClientId; // 结果发送给Sender。

    private static final java.lang.invoke.VarHandle vh_ActionName;
    private static final java.lang.invoke.VarHandle vh_Parameter;
    private static final java.lang.invoke.VarHandle vh_SenderAccount;
    private static final java.lang.invoke.VarHandle vh_SenderClientId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ActionName = _l_.findVarHandle(BTransmitAccount.class, "_ActionName", String.class);
            vh_Parameter = _l_.findVarHandle(BTransmitAccount.class, "_Parameter", Zeze.Net.Binary.class);
            vh_SenderAccount = _l_.findVarHandle(BTransmitAccount.class, "_SenderAccount", String.class);
            vh_SenderClientId = _l_.findVarHandle(BTransmitAccount.class, "_SenderClientId", String.class);
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

    @Override
    public Zeze.Net.Binary getParameter() {
        if (!isManaged())
            return _Parameter;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Parameter;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 2);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 2, vh_Parameter, _v_));
    }

    public Zeze.Transaction.Collections.PSet1<String> getTargetAccounts() {
        return _TargetAccounts;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<String> getTargetAccountsReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_TargetAccounts);
    }

    @Override
    public String getSenderAccount() {
        if (!isManaged())
            return _SenderAccount;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _SenderAccount;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _SenderAccount;
    }

    public void setSenderAccount(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _SenderAccount = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 4, vh_SenderAccount, _v_));
    }

    @Override
    public String getSenderClientId() {
        if (!isManaged())
            return _SenderClientId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _SenderClientId;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _SenderClientId;
    }

    public void setSenderClientId(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _SenderClientId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 5, vh_SenderClientId, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTransmitAccount() {
        _ActionName = "";
        _Parameter = Zeze.Net.Binary.Empty;
        _TargetAccounts = new Zeze.Transaction.Collections.PSet1<>(String.class);
        _TargetAccounts.variableId(3);
        _SenderAccount = "";
        _SenderClientId = "";
    }

    @SuppressWarnings("deprecation")
    public BTransmitAccount(String _ActionName_, Zeze.Net.Binary _Parameter_, String _SenderAccount_, String _SenderClientId_) {
        if (_ActionName_ == null)
            _ActionName_ = "";
        _ActionName = _ActionName_;
        if (_Parameter_ == null)
            _Parameter_ = Zeze.Net.Binary.Empty;
        _Parameter = _Parameter_;
        _TargetAccounts = new Zeze.Transaction.Collections.PSet1<>(String.class);
        _TargetAccounts.variableId(3);
        if (_SenderAccount_ == null)
            _SenderAccount_ = "";
        _SenderAccount = _SenderAccount_;
        if (_SenderClientId_ == null)
            _SenderClientId_ = "";
        _SenderClientId = _SenderClientId_;
    }

    @Override
    public void reset() {
        setActionName("");
        setParameter(Zeze.Net.Binary.Empty);
        _TargetAccounts.clear();
        setSenderAccount("");
        setSenderClientId("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BTransmitAccount.Data toData() {
        var _d_ = new Zeze.Builtin.ProviderDirect.BTransmitAccount.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.ProviderDirect.BTransmitAccount.Data)_o_);
    }

    public void assign(BTransmitAccount.Data _o_) {
        setActionName(_o_._ActionName);
        setParameter(_o_._Parameter);
        _TargetAccounts.clear();
        _TargetAccounts.addAll(_o_._TargetAccounts);
        setSenderAccount(_o_._SenderAccount);
        setSenderClientId(_o_._SenderClientId);
        _unknown_ = null;
    }

    public void assign(BTransmitAccount _o_) {
        setActionName(_o_.getActionName());
        setParameter(_o_.getParameter());
        _TargetAccounts.assign(_o_._TargetAccounts);
        setSenderAccount(_o_.getSenderAccount());
        setSenderClientId(_o_.getSenderClientId());
        _unknown_ = _o_._unknown_;
    }

    public BTransmitAccount copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTransmitAccount copy() {
        var _c_ = new BTransmitAccount();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTransmitAccount _a_, BTransmitAccount _b_) {
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
        _s_.append("Zeze.Builtin.ProviderDirect.BTransmitAccount: {\n");
        _s_.append(_i1_).append("ActionName=").append(getActionName()).append(",\n");
        _s_.append(_i1_).append("Parameter=").append(getParameter()).append(",\n");
        _s_.append(_i1_).append("TargetAccounts={");
        if (!_TargetAccounts.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _TargetAccounts) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("SenderAccount=").append(getSenderAccount()).append(",\n");
        _s_.append(_i1_).append("SenderClientId=").append(getSenderClientId()).append('\n');
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
            var _x_ = getParameter();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _TargetAccounts;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteString(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            String _x_ = getSenderAccount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getSenderClientId();
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
            setParameter(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _TargetAccounts;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setSenderAccount(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setSenderClientId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTransmitAccount))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTransmitAccount)_o_;
        if (!getActionName().equals(_b_.getActionName()))
            return false;
        if (!getParameter().equals(_b_.getParameter()))
            return false;
        if (!_TargetAccounts.equals(_b_._TargetAccounts))
            return false;
        if (!getSenderAccount().equals(_b_.getSenderAccount()))
            return false;
        if (!getSenderClientId().equals(_b_.getSenderClientId()))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _TargetAccounts.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _TargetAccounts.initRootInfoWithRedo(_r_, this);
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
                case 2: _Parameter = _v_.binaryValue(); break;
                case 3: _TargetAccounts.followerApply(_v_); break;
                case 4: _SenderAccount = _v_.stringValue(); break;
                case 5: _SenderClientId = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setActionName(_r_.getString(_pn_ + "ActionName"));
        if (getActionName() == null)
            setActionName("");
        setParameter(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Parameter")));
        Zeze.Serialize.Helper.decodeJsonSet(_TargetAccounts, String.class, _r_.getString(_pn_ + "TargetAccounts"));
        setSenderAccount(_r_.getString(_pn_ + "SenderAccount"));
        if (getSenderAccount() == null)
            setSenderAccount("");
        setSenderClientId(_r_.getString(_pn_ + "SenderClientId"));
        if (getSenderClientId() == null)
            setSenderClientId("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "ActionName", getActionName());
        _s_.appendBinary(_pn_ + "Parameter", getParameter());
        _s_.appendString(_pn_ + "TargetAccounts", Zeze.Serialize.Helper.encodeJson(_TargetAccounts));
        _s_.appendString(_pn_ + "SenderAccount", getSenderAccount());
        _s_.appendString(_pn_ + "SenderClientId", getSenderClientId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ActionName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Parameter", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "TargetAccounts", "set", "", "string"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "SenderAccount", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "SenderClientId", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 2637210793748287339L;

    private String _ActionName;
    private Zeze.Net.Binary _Parameter; // encoded bean
    private java.util.HashSet<String> _TargetAccounts; // 查询目标角色。
    private String _SenderAccount; // 结果发送给Sender。
    private String _SenderClientId; // 结果发送给Sender。

    public String getActionName() {
        return _ActionName;
    }

    public void setActionName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ActionName = _v_;
    }

    public Zeze.Net.Binary getParameter() {
        return _Parameter;
    }

    public void setParameter(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Parameter = _v_;
    }

    public java.util.HashSet<String> getTargetAccounts() {
        return _TargetAccounts;
    }

    public void setTargetAccounts(java.util.HashSet<String> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _TargetAccounts = _v_;
    }

    public String getSenderAccount() {
        return _SenderAccount;
    }

    public void setSenderAccount(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _SenderAccount = _v_;
    }

    public String getSenderClientId() {
        return _SenderClientId;
    }

    public void setSenderClientId(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _SenderClientId = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ActionName = "";
        _Parameter = Zeze.Net.Binary.Empty;
        _TargetAccounts = new java.util.HashSet<>();
        _SenderAccount = "";
        _SenderClientId = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _ActionName_, Zeze.Net.Binary _Parameter_, java.util.HashSet<String> _TargetAccounts_, String _SenderAccount_, String _SenderClientId_) {
        if (_ActionName_ == null)
            _ActionName_ = "";
        _ActionName = _ActionName_;
        if (_Parameter_ == null)
            _Parameter_ = Zeze.Net.Binary.Empty;
        _Parameter = _Parameter_;
        if (_TargetAccounts_ == null)
            _TargetAccounts_ = new java.util.HashSet<>();
        _TargetAccounts = _TargetAccounts_;
        if (_SenderAccount_ == null)
            _SenderAccount_ = "";
        _SenderAccount = _SenderAccount_;
        if (_SenderClientId_ == null)
            _SenderClientId_ = "";
        _SenderClientId = _SenderClientId_;
    }

    @Override
    public void reset() {
        _ActionName = "";
        _Parameter = Zeze.Net.Binary.Empty;
        _TargetAccounts.clear();
        _SenderAccount = "";
        _SenderClientId = "";
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BTransmitAccount toBean() {
        var _b_ = new Zeze.Builtin.ProviderDirect.BTransmitAccount();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BTransmitAccount)_o_);
    }

    public void assign(BTransmitAccount _o_) {
        _ActionName = _o_.getActionName();
        _Parameter = _o_.getParameter();
        _TargetAccounts.clear();
        _TargetAccounts.addAll(_o_._TargetAccounts);
        _SenderAccount = _o_.getSenderAccount();
        _SenderClientId = _o_.getSenderClientId();
    }

    public void assign(BTransmitAccount.Data _o_) {
        _ActionName = _o_._ActionName;
        _Parameter = _o_._Parameter;
        _TargetAccounts.clear();
        _TargetAccounts.addAll(_o_._TargetAccounts);
        _SenderAccount = _o_._SenderAccount;
        _SenderClientId = _o_._SenderClientId;
    }

    @Override
    public BTransmitAccount.Data copy() {
        var _c_ = new BTransmitAccount.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTransmitAccount.Data _a_, BTransmitAccount.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BTransmitAccount.Data clone() {
        return (BTransmitAccount.Data)super.clone();
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
        _s_.append("Zeze.Builtin.ProviderDirect.BTransmitAccount: {\n");
        _s_.append(_i1_).append("ActionName=").append(_ActionName).append(",\n");
        _s_.append(_i1_).append("Parameter=").append(_Parameter).append(",\n");
        _s_.append(_i1_).append("TargetAccounts={");
        if (!_TargetAccounts.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _TargetAccounts) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("SenderAccount=").append(_SenderAccount).append(",\n");
        _s_.append(_i1_).append("SenderClientId=").append(_SenderClientId).append('\n');
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
            var _x_ = _Parameter;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _TargetAccounts;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteString(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            String _x_ = _SenderAccount;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _SenderClientId;
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
            _Parameter = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _TargetAccounts;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _SenderAccount = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _SenderClientId = _o_.ReadString(_t_);
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
        if (!(_o_ instanceof BTransmitAccount.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTransmitAccount.Data)_o_;
        if (!_ActionName.equals(_b_._ActionName))
            return false;
        if (!_Parameter.equals(_b_._Parameter))
            return false;
        if (!_TargetAccounts.equals(_b_._TargetAccounts))
            return false;
        if (!_SenderAccount.equals(_b_._SenderAccount))
            return false;
        if (!_SenderClientId.equals(_b_._SenderClientId))
            return false;
        return true;
    }
}
}
