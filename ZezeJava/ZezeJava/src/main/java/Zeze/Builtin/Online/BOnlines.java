// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BOnlines extends Zeze.Transaction.Bean implements BOnlinesReadOnly {
    public static final long TYPEID = -725348871039859823L;

    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Online.BOnline> _Logins; // key is ClientId
    private long _LastLoginVersion; // 用来生成 account 登录版本号。每次递增。
    private String _Account; // 所属账号,用于登录验证

    private static final java.lang.invoke.VarHandle vh_LastLoginVersion;
    private static final java.lang.invoke.VarHandle vh_Account;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_LastLoginVersion = _l_.findVarHandle(BOnlines.class, "_LastLoginVersion", long.class);
            vh_Account = _l_.findVarHandle(BOnlines.class, "_Account", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Online.BOnline> getLogins() {
        return _Logins;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Online.BOnline, Zeze.Builtin.Online.BOnlineReadOnly> getLoginsReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Logins);
    }

    @Override
    public long getLastLoginVersion() {
        if (!isManaged())
            return _LastLoginVersion;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LastLoginVersion;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _LastLoginVersion;
    }

    public void setLastLoginVersion(long _v_) {
        if (!isManaged()) {
            _LastLoginVersion = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_LastLoginVersion, _v_));
    }

    @Override
    public String getAccount() {
        if (!isManaged())
            return _Account;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Account;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 3);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 3, vh_Account, _v_));
    }

    @SuppressWarnings("deprecation")
    public BOnlines() {
        _Logins = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Online.BOnline.class);
        _Logins.variableId(1);
        _Account = "";
    }

    @SuppressWarnings("deprecation")
    public BOnlines(long _LastLoginVersion_, String _Account_) {
        _Logins = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Online.BOnline.class);
        _Logins.variableId(1);
        _LastLoginVersion = _LastLoginVersion_;
        if (_Account_ == null)
            _Account_ = "";
        _Account = _Account_;
    }

    @Override
    public void reset() {
        _Logins.clear();
        setLastLoginVersion(0);
        setAccount("");
        _unknown_ = null;
    }

    public void assign(BOnlines _o_) {
        _Logins.clear();
        for (var _e_ : _o_._Logins.entrySet())
            _Logins.put(_e_.getKey(), _e_.getValue().copy());
        setLastLoginVersion(_o_.getLastLoginVersion());
        setAccount(_o_.getAccount());
        _unknown_ = _o_._unknown_;
    }

    public BOnlines copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOnlines copy() {
        var _c_ = new BOnlines();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BOnlines _a_, BOnlines _b_) {
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
        _s_.append("Zeze.Builtin.Online.BOnlines: {\n");
        _s_.append(_i1_).append("Logins={");
        if (!_Logins.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Logins.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Logins.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("LastLoginVersion=").append(getLastLoginVersion()).append(",\n");
        _s_.append(_i1_).append("Account=").append(getAccount()).append('\n');
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
            var _x_ = _Logins;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            long _x_ = getLastLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getAccount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            var _x_ = _Logins;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Online.BOnline(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLastLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setAccount(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BOnlines))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BOnlines)_o_;
        if (!_Logins.equals(_b_._Logins))
            return false;
        if (getLastLoginVersion() != _b_.getLastLoginVersion())
            return false;
        if (!getAccount().equals(_b_.getAccount()))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Logins.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Logins.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Logins.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        if (getLastLoginVersion() < 0)
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
                case 1: _Logins.followerApply(_v_); break;
                case 2: _LastLoginVersion = _v_.longValue(); break;
                case 3: _Account = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonMap(this, "Logins", _Logins, _r_.getString(_pn_ + "Logins"));
        setLastLoginVersion(_r_.getLong(_pn_ + "LastLoginVersion"));
        setAccount(_r_.getString(_pn_ + "Account"));
        if (getAccount() == null)
            setAccount("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Logins", Zeze.Serialize.Helper.encodeJson(_Logins));
        _s_.appendLong(_pn_ + "LastLoginVersion", getLastLoginVersion());
        _s_.appendString(_pn_ + "Account", getAccount());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Logins", "map", "string", "Zeze.Builtin.Online.BOnline"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "LastLoginVersion", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Account", "string", "", ""));
        return _v_;
    }
}
