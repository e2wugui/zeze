// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BLinkBroken extends Zeze.Transaction.Bean implements BLinkBrokenReadOnly {
    public static final long TYPEID = 1424702393060691138L;

    public static final int REASON_PEERCLOSE = 0;

    private String _account;
    private long _linkSid;
    private int _reason;
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Provider.BUserState> _userState;

    private static final java.lang.invoke.VarHandle vh_account;
    private static final java.lang.invoke.VarHandle vh_linkSid;
    private static final java.lang.invoke.VarHandle vh_reason;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_account = _l_.findVarHandle(BLinkBroken.class, "_account", String.class);
            vh_linkSid = _l_.findVarHandle(BLinkBroken.class, "_linkSid", long.class);
            vh_reason = _l_.findVarHandle(BLinkBroken.class, "_reason", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getAccount() {
        if (!isManaged())
            return _account;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _account;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_account, _v_));
    }

    @Override
    public long getLinkSid() {
        if (!isManaged())
            return _linkSid;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _linkSid;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _linkSid;
    }

    public void setLinkSid(long _v_) {
        if (!isManaged()) {
            _linkSid = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_linkSid, _v_));
    }

    @Override
    public int getReason() {
        if (!isManaged())
            return _reason;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _reason;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _reason;
    }

    public void setReason(int _v_) {
        if (!isManaged()) {
            _reason = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_reason, _v_));
    }

    public Zeze.Builtin.Provider.BUserState getUserState() {
        return _userState.getValue();
    }

    public void setUserState(Zeze.Builtin.Provider.BUserState _v_) {
        _userState.setValue(_v_);
    }

    @Override
    public Zeze.Builtin.Provider.BUserStateReadOnly getUserStateReadOnly() {
        return _userState.getValue();
    }

    @SuppressWarnings("deprecation")
    public BLinkBroken() {
        _account = "";
        _userState = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Provider.BUserState(), Zeze.Builtin.Provider.BUserState.class);
        _userState.variableId(4);
    }

    @SuppressWarnings("deprecation")
    public BLinkBroken(String _account_, long _linkSid_, int _reason_) {
        if (_account_ == null)
            _account_ = "";
        _account = _account_;
        _linkSid = _linkSid_;
        _reason = _reason_;
        _userState = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Provider.BUserState(), Zeze.Builtin.Provider.BUserState.class);
        _userState.variableId(4);
    }

    @Override
    public void reset() {
        setAccount("");
        setLinkSid(0);
        setReason(0);
        _userState.reset();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BLinkBroken.Data toData() {
        var _d_ = new Zeze.Builtin.Provider.BLinkBroken.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Provider.BLinkBroken.Data)_o_);
    }

    public void assign(BLinkBroken.Data _o_) {
        setAccount(_o_._account);
        setLinkSid(_o_._linkSid);
        setReason(_o_._reason);
        var _d__userState = new Zeze.Builtin.Provider.BUserState();
        _d__userState.assign(_o_._userState);
        _userState.setValue(_d__userState);
        _unknown_ = null;
    }

    public void assign(BLinkBroken _o_) {
        setAccount(_o_.getAccount());
        setLinkSid(_o_.getLinkSid());
        setReason(_o_.getReason());
        _userState.assign(_o_._userState);
        _unknown_ = _o_._unknown_;
    }

    public BLinkBroken copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLinkBroken copy() {
        var _c_ = new BLinkBroken();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLinkBroken _a_, BLinkBroken _b_) {
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
        _s_.append("Zeze.Builtin.Provider.BLinkBroken: {\n");
        _s_.append(_i1_).append("account=").append(getAccount()).append(",\n");
        _s_.append(_i1_).append("linkSid=").append(getLinkSid()).append(",\n");
        _s_.append(_i1_).append("reason=").append(getReason()).append(",\n");
        _s_.append(_i1_).append("userState=");
        _userState.buildString(_s_, _l_ + 8);
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 4, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _userState.encode(_o_);
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
        if (_i_ == 4) {
            _o_.ReadBean(_userState, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLinkBroken))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLinkBroken)_o_;
        if (!getAccount().equals(_b_.getAccount()))
            return false;
        if (getLinkSid() != _b_.getLinkSid())
            return false;
        if (getReason() != _b_.getReason())
            return false;
        if (!_userState.equals(_b_._userState))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _userState.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _userState.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getLinkSid() < 0)
            return true;
        if (getReason() < 0)
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
                case 1: _account = _v_.stringValue(); break;
                case 2: _linkSid = _v_.longValue(); break;
                case 3: _reason = _v_.intValue(); break;
                case 4: _userState.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setAccount(_r_.getString(_pn_ + "account"));
        if (getAccount() == null)
            setAccount("");
        setLinkSid(_r_.getLong(_pn_ + "linkSid"));
        setReason(_r_.getInt(_pn_ + "reason"));
        _p_.add("userState");
        _userState.decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "account", getAccount());
        _s_.appendLong(_pn_ + "linkSid", getLinkSid());
        _s_.appendInt(_pn_ + "reason", getReason());
        _p_.add("userState");
        _userState.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "account", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "linkSid", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "reason", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "userState", "Zeze.Builtin.Provider.BUserState", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 1424702393060691138L;

    public static final int REASON_PEERCLOSE = 0;

    private String _account;
    private long _linkSid;
    private int _reason;
    private Zeze.Builtin.Provider.BUserState.Data _userState;

    public String getAccount() {
        return _account;
    }

    public void setAccount(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _account = _v_;
    }

    public long getLinkSid() {
        return _linkSid;
    }

    public void setLinkSid(long _v_) {
        _linkSid = _v_;
    }

    public int getReason() {
        return _reason;
    }

    public void setReason(int _v_) {
        _reason = _v_;
    }

    public Zeze.Builtin.Provider.BUserState.Data getUserState() {
        return _userState;
    }

    public void setUserState(Zeze.Builtin.Provider.BUserState.Data _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _userState = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _account = "";
        _userState = new Zeze.Builtin.Provider.BUserState.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(String _account_, long _linkSid_, int _reason_, Zeze.Builtin.Provider.BUserState.Data _userState_) {
        if (_account_ == null)
            _account_ = "";
        _account = _account_;
        _linkSid = _linkSid_;
        _reason = _reason_;
        if (_userState_ == null)
            _userState_ = new Zeze.Builtin.Provider.BUserState.Data();
        _userState = _userState_;
    }

    @Override
    public void reset() {
        _account = "";
        _linkSid = 0;
        _reason = 0;
        _userState.reset();
    }

    @Override
    public Zeze.Builtin.Provider.BLinkBroken toBean() {
        var _b_ = new Zeze.Builtin.Provider.BLinkBroken();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BLinkBroken)_o_);
    }

    public void assign(BLinkBroken _o_) {
        _account = _o_.getAccount();
        _linkSid = _o_.getLinkSid();
        _reason = _o_.getReason();
        _userState.assign(_o_._userState.getValue());
    }

    public void assign(BLinkBroken.Data _o_) {
        _account = _o_._account;
        _linkSid = _o_._linkSid;
        _reason = _o_._reason;
        _userState.assign(_o_._userState);
    }

    @Override
    public BLinkBroken.Data copy() {
        var _c_ = new BLinkBroken.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLinkBroken.Data _a_, BLinkBroken.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLinkBroken.Data clone() {
        return (BLinkBroken.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Provider.BLinkBroken: {\n");
        _s_.append(_i1_).append("account=").append(_account).append(",\n");
        _s_.append(_i1_).append("linkSid=").append(_linkSid).append(",\n");
        _s_.append(_i1_).append("reason=").append(_reason).append(",\n");
        _s_.append(_i1_).append("userState=");
        _userState.buildString(_s_, _l_ + 8);
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
            String _x_ = _account;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = _linkSid;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = _reason;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 4, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _userState.encode(_o_);
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
            _account = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _linkSid = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _reason = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadBean(_userState, _t_);
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
        if (!(_o_ instanceof BLinkBroken.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLinkBroken.Data)_o_;
        if (!_account.equals(_b_._account))
            return false;
        if (_linkSid != _b_._linkSid)
            return false;
        if (_reason != _b_._reason)
            return false;
        if (!_userState.equals(_b_._userState))
            return false;
        return true;
    }
}
}
