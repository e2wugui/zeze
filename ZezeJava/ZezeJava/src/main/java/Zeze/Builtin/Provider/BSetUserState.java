// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BSetUserState extends Zeze.Transaction.Bean implements BSetUserStateReadOnly {
    public static final long TYPEID = -4860388989628287875L;

    private long _linkSid;
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Provider.BUserState> _userState;

    private static final java.lang.invoke.VarHandle vh_linkSid;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_linkSid = _l_.findVarHandle(BSetUserState.class, "_linkSid", long.class);
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
    public BSetUserState() {
        _userState = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Provider.BUserState(), Zeze.Builtin.Provider.BUserState.class);
        _userState.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BSetUserState(long _linkSid_) {
        _linkSid = _linkSid_;
        _userState = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Provider.BUserState(), Zeze.Builtin.Provider.BUserState.class);
        _userState.variableId(2);
    }

    @Override
    public void reset() {
        setLinkSid(0);
        _userState.reset();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BSetUserState.Data toData() {
        var _d_ = new Zeze.Builtin.Provider.BSetUserState.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Provider.BSetUserState.Data)_o_);
    }

    public void assign(BSetUserState.Data _o_) {
        setLinkSid(_o_._linkSid);
        var _d__userState = new Zeze.Builtin.Provider.BUserState();
        _d__userState.assign(_o_._userState);
        _userState.setValue(_d__userState);
        _unknown_ = null;
    }

    public void assign(BSetUserState _o_) {
        setLinkSid(_o_.getLinkSid());
        _userState.assign(_o_._userState);
        _unknown_ = _o_._unknown_;
    }

    public BSetUserState copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSetUserState copy() {
        var _c_ = new BSetUserState();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSetUserState _a_, BSetUserState _b_) {
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
        _s_.append("Zeze.Builtin.Provider.BSetUserState: {\n");
        _s_.append(_i1_).append("linkSid=").append(getLinkSid()).append(",\n");
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
            long _x_ = getLinkSid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
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
            setLinkSid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
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
        if (!(_o_ instanceof BSetUserState))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSetUserState)_o_;
        if (getLinkSid() != _b_.getLinkSid())
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
                case 2: _userState.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setLinkSid(_r_.getLong(_pn_ + "linkSid"));
        _p_.add("userState");
        _userState.decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "linkSid", getLinkSid());
        _p_.add("userState");
        _userState.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "linkSid", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "userState", "Zeze.Builtin.Provider.BUserState", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4860388989628287875L;

    private long _linkSid;
    private Zeze.Builtin.Provider.BUserState.Data _userState;

    public long getLinkSid() {
        return _linkSid;
    }

    public void setLinkSid(long _v_) {
        _linkSid = _v_;
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
        _userState = new Zeze.Builtin.Provider.BUserState.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(long _linkSid_, Zeze.Builtin.Provider.BUserState.Data _userState_) {
        _linkSid = _linkSid_;
        if (_userState_ == null)
            _userState_ = new Zeze.Builtin.Provider.BUserState.Data();
        _userState = _userState_;
    }

    @Override
    public void reset() {
        _linkSid = 0;
        _userState.reset();
    }

    @Override
    public Zeze.Builtin.Provider.BSetUserState toBean() {
        var _b_ = new Zeze.Builtin.Provider.BSetUserState();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BSetUserState)_o_);
    }

    public void assign(BSetUserState _o_) {
        _linkSid = _o_.getLinkSid();
        _userState.assign(_o_._userState.getValue());
    }

    public void assign(BSetUserState.Data _o_) {
        _linkSid = _o_._linkSid;
        _userState.assign(_o_._userState);
    }

    @Override
    public BSetUserState.Data copy() {
        var _c_ = new BSetUserState.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSetUserState.Data _a_, BSetUserState.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSetUserState.Data clone() {
        return (BSetUserState.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Provider.BSetUserState: {\n");
        _s_.append(_i1_).append("linkSid=").append(_linkSid).append(",\n");
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
            long _x_ = _linkSid;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
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
            _linkSid = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
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
        if (!(_o_ instanceof BSetUserState.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSetUserState.Data)_o_;
        if (_linkSid != _b_._linkSid)
            return false;
        if (!_userState.equals(_b_._userState))
            return false;
        return true;
    }
}
}
