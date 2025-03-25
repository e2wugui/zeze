// auto-generated @formatter:off
package Zeze.Builtin.AccountOnline;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BAccountLink extends Zeze.Transaction.Bean implements BAccountLinkReadOnly {
    public static final long TYPEID = -7947757090857205293L;

    private String _Account;
    private String _LinkName;
    private long _LinkSid;

    private static final java.lang.invoke.VarHandle vh_Account;
    private static final java.lang.invoke.VarHandle vh_LinkName;
    private static final java.lang.invoke.VarHandle vh_LinkSid;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Account = _l_.findVarHandle(BAccountLink.class, "_Account", String.class);
            vh_LinkName = _l_.findVarHandle(BAccountLink.class, "_LinkName", String.class);
            vh_LinkSid = _l_.findVarHandle(BAccountLink.class, "_LinkSid", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getAccount() {
        if (!isManaged())
            return _Account;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Account;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.stringValue() : _Account;
    }

    public void setAccount(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Account = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_Account, _v_));
    }

    @Override
    public String getLinkName() {
        if (!isManaged())
            return _LinkName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LinkName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.stringValue() : _LinkName;
    }

    public void setLinkName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LinkName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_LinkName, _v_));
    }

    @Override
    public long getLinkSid() {
        if (!isManaged())
            return _LinkSid;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LinkSid;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _LinkSid;
    }

    public void setLinkSid(long _v_) {
        if (!isManaged()) {
            _LinkSid = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_LinkSid, _v_));
    }

    @SuppressWarnings("deprecation")
    public BAccountLink() {
        _Account = "";
        _LinkName = "";
    }

    @SuppressWarnings("deprecation")
    public BAccountLink(String _Account_, String _LinkName_, long _LinkSid_) {
        if (_Account_ == null)
            _Account_ = "";
        _Account = _Account_;
        if (_LinkName_ == null)
            _LinkName_ = "";
        _LinkName = _LinkName_;
        _LinkSid = _LinkSid_;
    }

    @Override
    public void reset() {
        setAccount("");
        setLinkName("");
        setLinkSid(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.AccountOnline.BAccountLink.Data toData() {
        var _d_ = new Zeze.Builtin.AccountOnline.BAccountLink.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.AccountOnline.BAccountLink.Data)_o_);
    }

    public void assign(BAccountLink.Data _o_) {
        setAccount(_o_._Account);
        setLinkName(_o_._LinkName);
        setLinkSid(_o_._LinkSid);
        _unknown_ = null;
    }

    public void assign(BAccountLink _o_) {
        setAccount(_o_.getAccount());
        setLinkName(_o_.getLinkName());
        setLinkSid(_o_.getLinkSid());
        _unknown_ = _o_._unknown_;
    }

    public BAccountLink copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAccountLink copy() {
        var _c_ = new BAccountLink();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAccountLink _a_, BAccountLink _b_) {
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
        _s_.append("Zeze.Builtin.AccountOnline.BAccountLink: {\n");
        _s_.append(_i1_).append("Account=").append(getAccount()).append(",\n");
        _s_.append(_i1_).append("LinkName=").append(getLinkName()).append(",\n");
        _s_.append(_i1_).append("LinkSid=").append(getLinkSid()).append('\n');
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
            String _x_ = getLinkName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getLinkSid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            setAccount(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLinkName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setLinkSid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BAccountLink))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAccountLink)_o_;
        if (!getAccount().equals(_b_.getAccount()))
            return false;
        if (!getLinkName().equals(_b_.getLinkName()))
            return false;
        if (getLinkSid() != _b_.getLinkSid())
            return false;
        return true;
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
                case 1: _Account = _v_.stringValue(); break;
                case 2: _LinkName = _v_.stringValue(); break;
                case 3: _LinkSid = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setAccount(_r_.getString(_pn_ + "Account"));
        if (getAccount() == null)
            setAccount("");
        setLinkName(_r_.getString(_pn_ + "LinkName"));
        if (getLinkName() == null)
            setLinkName("");
        setLinkSid(_r_.getLong(_pn_ + "LinkSid"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Account", getAccount());
        _s_.appendString(_pn_ + "LinkName", getLinkName());
        _s_.appendLong(_pn_ + "LinkSid", getLinkSid());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Account", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "LinkName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "LinkSid", "long", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -7947757090857205293L;

    private String _Account;
    private String _LinkName;
    private long _LinkSid;

    public String getAccount() {
        return _Account;
    }

    public void setAccount(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Account = _v_;
    }

    public String getLinkName() {
        return _LinkName;
    }

    public void setLinkName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _LinkName = _v_;
    }

    public long getLinkSid() {
        return _LinkSid;
    }

    public void setLinkSid(long _v_) {
        _LinkSid = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Account = "";
        _LinkName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _Account_, String _LinkName_, long _LinkSid_) {
        if (_Account_ == null)
            _Account_ = "";
        _Account = _Account_;
        if (_LinkName_ == null)
            _LinkName_ = "";
        _LinkName = _LinkName_;
        _LinkSid = _LinkSid_;
    }

    @Override
    public void reset() {
        _Account = "";
        _LinkName = "";
        _LinkSid = 0;
    }

    @Override
    public Zeze.Builtin.AccountOnline.BAccountLink toBean() {
        var _b_ = new Zeze.Builtin.AccountOnline.BAccountLink();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BAccountLink)_o_);
    }

    public void assign(BAccountLink _o_) {
        _Account = _o_.getAccount();
        _LinkName = _o_.getLinkName();
        _LinkSid = _o_.getLinkSid();
    }

    public void assign(BAccountLink.Data _o_) {
        _Account = _o_._Account;
        _LinkName = _o_._LinkName;
        _LinkSid = _o_._LinkSid;
    }

    @Override
    public BAccountLink.Data copy() {
        var _c_ = new BAccountLink.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAccountLink.Data _a_, BAccountLink.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BAccountLink.Data clone() {
        return (BAccountLink.Data)super.clone();
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
        _s_.append("Zeze.Builtin.AccountOnline.BAccountLink: {\n");
        _s_.append(_i1_).append("Account=").append(_Account).append(",\n");
        _s_.append(_i1_).append("LinkName=").append(_LinkName).append(",\n");
        _s_.append(_i1_).append("LinkSid=").append(_LinkSid).append('\n');
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
            String _x_ = _Account;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _LinkName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = _LinkSid;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Account = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _LinkName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _LinkSid = _o_.ReadLong(_t_);
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
        if (!(_o_ instanceof BAccountLink.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAccountLink.Data)_o_;
        if (!_Account.equals(_b_._Account))
            return false;
        if (!_LinkName.equals(_b_._LinkName))
            return false;
        if (_LinkSid != _b_._LinkSid)
            return false;
        return true;
    }
}
}
