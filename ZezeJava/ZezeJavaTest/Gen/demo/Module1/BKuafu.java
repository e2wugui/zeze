// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BKuafu extends Zeze.Transaction.Bean implements BKuafuReadOnly {
    public static final long TYPEID = -8805935299408250239L;

    private long _Account;
    private long _Money;

    private static final java.lang.invoke.VarHandle vh_Account;
    private static final java.lang.invoke.VarHandle vh_Money;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Account = _l_.findVarHandle(BKuafu.class, "_Account", long.class);
            vh_Money = _l_.findVarHandle(BKuafu.class, "_Money", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getAccount() {
        if (!isManaged())
            return _Account;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Account;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Account;
    }

    public void setAccount(long _v_) {
        if (!isManaged()) {
            _Account = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_Account, _v_));
    }

    @Override
    public long getMoney() {
        if (!isManaged())
            return _Money;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Money;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Money;
    }

    public void setMoney(long _v_) {
        if (!isManaged()) {
            _Money = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_Money, _v_));
    }

    @SuppressWarnings("deprecation")
    public BKuafu() {
    }

    @SuppressWarnings("deprecation")
    public BKuafu(long _Account_, long _Money_) {
        _Account = _Account_;
        _Money = _Money_;
    }

    @Override
    public void reset() {
        setAccount(0);
        setMoney(0);
        _unknown_ = null;
    }

    @Override
    public demo.Module1.BKuafu.Data toData() {
        var _d_ = new demo.Module1.BKuafu.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((demo.Module1.BKuafu.Data)_o_);
    }

    public void assign(BKuafu.Data _o_) {
        setAccount(_o_._Account);
        setMoney(_o_._Money);
        _unknown_ = null;
    }

    public void assign(BKuafu _o_) {
        setAccount(_o_.getAccount());
        setMoney(_o_.getMoney());
        _unknown_ = _o_._unknown_;
    }

    public BKuafu copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BKuafu copy() {
        var _c_ = new BKuafu();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BKuafu _a_, BKuafu _b_) {
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
        _s_.append("demo.Module1.BKuafu: {\n");
        _s_.append(_i1_).append("Account=").append(getAccount()).append(",\n");
        _s_.append(_i1_).append("Money=").append(getMoney()).append('\n');
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

    private byte @Nullable [] _unknown_;

    public byte @Nullable [] unknown() {
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
            long _x_ = getAccount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getMoney();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setAccount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setMoney(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BKuafu))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BKuafu)_o_;
        if (getAccount() != _b_.getAccount())
            return false;
        if (getMoney() != _b_.getMoney())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getAccount() < 0)
            return true;
        if (getMoney() < 0)
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
                case 1: _Account = _v_.longValue(); break;
                case 2: _Money = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setAccount(_r_.getLong(_pn_ + "Account"));
        setMoney(_r_.getLong(_pn_ + "Money"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "Account", getAccount());
        _s_.appendLong(_pn_ + "Money", getMoney());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Account", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Money", "long", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -8805935299408250239L;

    private long _Account;
    private long _Money;

    public long getAccount() {
        return _Account;
    }

    public void setAccount(long _v_) {
        _Account = _v_;
    }

    public long getMoney() {
        return _Money;
    }

    public void setMoney(long _v_) {
        _Money = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(long _Account_, long _Money_) {
        _Account = _Account_;
        _Money = _Money_;
    }

    @Override
    public void reset() {
        _Account = 0;
        _Money = 0;
    }

    @Override
    public demo.Module1.BKuafu toBean() {
        var _b_ = new demo.Module1.BKuafu();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BKuafu)_o_);
    }

    public void assign(BKuafu _o_) {
        _Account = _o_.getAccount();
        _Money = _o_.getMoney();
    }

    public void assign(BKuafu.Data _o_) {
        _Account = _o_._Account;
        _Money = _o_._Money;
    }

    @Override
    public BKuafu.Data copy() {
        var _c_ = new BKuafu.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BKuafu.Data _a_, BKuafu.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BKuafu.Data clone() {
        return (BKuafu.Data)super.clone();
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
        _s_.append("demo.Module1.BKuafu: {\n");
        _s_.append(_i1_).append("Account=").append(_Account).append(",\n");
        _s_.append(_i1_).append("Money=").append(_Money).append('\n');
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
            long _x_ = _Account;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = _Money;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            _Account = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Money = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BKuafu.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BKuafu.Data)_o_;
        if (_Account != _b_._Account)
            return false;
        if (_Money != _b_._Money)
            return false;
        return true;
    }
}
}
