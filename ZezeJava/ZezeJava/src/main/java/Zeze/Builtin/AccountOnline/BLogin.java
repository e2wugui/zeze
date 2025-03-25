// auto-generated @formatter:off
package Zeze.Builtin.AccountOnline;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BLogin extends Zeze.Transaction.Bean implements BLoginReadOnly {
    public static final long TYPEID = 3455789962318553073L;

    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.AccountOnline.BAccountLink> _AccountLink;
    private boolean _KickOld;

    private static final java.lang.invoke.VarHandle vh_KickOld;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_KickOld = _l_.findVarHandle(BLogin.class, "_KickOld", boolean.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    public Zeze.Builtin.AccountOnline.BAccountLink getAccountLink() {
        return _AccountLink.getValue();
    }

    public void setAccountLink(Zeze.Builtin.AccountOnline.BAccountLink _v_) {
        _AccountLink.setValue(_v_);
    }

    @Override
    public Zeze.Builtin.AccountOnline.BAccountLinkReadOnly getAccountLinkReadOnly() {
        return _AccountLink.getValue();
    }

    @Override
    public boolean isKickOld() {
        if (!isManaged())
            return _KickOld;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _KickOld;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _KickOld;
    }

    public void setKickOld(boolean _v_) {
        if (!isManaged()) {
            _KickOld = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 2, vh_KickOld, _v_));
    }

    @SuppressWarnings("deprecation")
    public BLogin() {
        _AccountLink = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.AccountOnline.BAccountLink(), Zeze.Builtin.AccountOnline.BAccountLink.class);
        _AccountLink.variableId(1);
    }

    @SuppressWarnings("deprecation")
    public BLogin(boolean _KickOld_) {
        _AccountLink = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.AccountOnline.BAccountLink(), Zeze.Builtin.AccountOnline.BAccountLink.class);
        _AccountLink.variableId(1);
        _KickOld = _KickOld_;
    }

    @Override
    public void reset() {
        _AccountLink.reset();
        setKickOld(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.AccountOnline.BLogin.Data toData() {
        var _d_ = new Zeze.Builtin.AccountOnline.BLogin.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.AccountOnline.BLogin.Data)_o_);
    }

    public void assign(BLogin.Data _o_) {
        var _d__AccountLink = new Zeze.Builtin.AccountOnline.BAccountLink();
        _d__AccountLink.assign(_o_._AccountLink);
        _AccountLink.setValue(_d__AccountLink);
        setKickOld(_o_._KickOld);
        _unknown_ = null;
    }

    public void assign(BLogin _o_) {
        _AccountLink.assign(_o_._AccountLink);
        setKickOld(_o_.isKickOld());
        _unknown_ = _o_._unknown_;
    }

    public BLogin copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLogin copy() {
        var _c_ = new BLogin();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLogin _a_, BLogin _b_) {
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
        _s_.append("Zeze.Builtin.AccountOnline.BLogin: {\n");
        _s_.append(_i1_).append("AccountLink=");
        _AccountLink.buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("KickOld=").append(isKickOld()).append('\n');
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _AccountLink.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            boolean _x_ = isKickOld();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            _o_.ReadBean(_AccountLink, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setKickOld(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLogin))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLogin)_o_;
        if (!_AccountLink.equals(_b_._AccountLink))
            return false;
        if (isKickOld() != _b_.isKickOld())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _AccountLink.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _AccountLink.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_AccountLink.negativeCheck())
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
                case 1: _AccountLink.followerApply(_v_); break;
                case 2: _KickOld = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        _p_.add("AccountLink");
        _AccountLink.decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setKickOld(_r_.getBoolean(_pn_ + "KickOld"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        _p_.add("AccountLink");
        _AccountLink.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendBoolean(_pn_ + "KickOld", isKickOld());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "AccountLink", "Zeze.Builtin.AccountOnline.BAccountLink", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "KickOld", "bool", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 3455789962318553073L;

    private Zeze.Builtin.AccountOnline.BAccountLink.Data _AccountLink;
    private boolean _KickOld;

    public Zeze.Builtin.AccountOnline.BAccountLink.Data getAccountLink() {
        return _AccountLink;
    }

    public void setAccountLink(Zeze.Builtin.AccountOnline.BAccountLink.Data _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _AccountLink = _v_;
    }

    public boolean isKickOld() {
        return _KickOld;
    }

    public void setKickOld(boolean _v_) {
        _KickOld = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _AccountLink = new Zeze.Builtin.AccountOnline.BAccountLink.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Builtin.AccountOnline.BAccountLink.Data _AccountLink_, boolean _KickOld_) {
        if (_AccountLink_ == null)
            _AccountLink_ = new Zeze.Builtin.AccountOnline.BAccountLink.Data();
        _AccountLink = _AccountLink_;
        _KickOld = _KickOld_;
    }

    @Override
    public void reset() {
        _AccountLink.reset();
        _KickOld = false;
    }

    @Override
    public Zeze.Builtin.AccountOnline.BLogin toBean() {
        var _b_ = new Zeze.Builtin.AccountOnline.BLogin();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BLogin)_o_);
    }

    public void assign(BLogin _o_) {
        _AccountLink.assign(_o_._AccountLink.getValue());
        _KickOld = _o_.isKickOld();
    }

    public void assign(BLogin.Data _o_) {
        _AccountLink.assign(_o_._AccountLink);
        _KickOld = _o_._KickOld;
    }

    @Override
    public BLogin.Data copy() {
        var _c_ = new BLogin.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLogin.Data _a_, BLogin.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLogin.Data clone() {
        return (BLogin.Data)super.clone();
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
        _s_.append("Zeze.Builtin.AccountOnline.BLogin: {\n");
        _s_.append(_i1_).append("AccountLink=");
        _AccountLink.buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("KickOld=").append(_KickOld).append('\n');
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _AccountLink.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            boolean _x_ = _KickOld;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadBean(_AccountLink, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _KickOld = _o_.ReadBool(_t_);
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
        if (!(_o_ instanceof BLogin.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLogin.Data)_o_;
        if (!_AccountLink.equals(_b_._AccountLink))
            return false;
        if (_KickOld != _b_._KickOld)
            return false;
        return true;
    }
}
}
