// auto-generated @formatter:off
package Zeze.Builtin.Token;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BGetTokenArg extends Zeze.Transaction.Bean implements BGetTokenArgReadOnly {
    public static final long TYPEID = 3072398246496713168L;

    private String _token; // 请求token
    private long _maxCount; // 此值＞0且请求token次数(包括当前请求)≥此值时,服务器会清除此token及绑定的所有状态(当前请求仍然能正常获得)

    private static final java.lang.invoke.VarHandle vh_token;
    private static final java.lang.invoke.VarHandle vh_maxCount;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_token = _l_.findVarHandle(BGetTokenArg.class, "_token", String.class);
            vh_maxCount = _l_.findVarHandle(BGetTokenArg.class, "_maxCount", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getToken() {
        if (!isManaged())
            return _token;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _token;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _token;
    }

    public void setToken(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _token = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_token, _v_));
    }

    @Override
    public long getMaxCount() {
        if (!isManaged())
            return _maxCount;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _maxCount;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _maxCount;
    }

    public void setMaxCount(long _v_) {
        if (!isManaged()) {
            _maxCount = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_maxCount, _v_));
    }

    @SuppressWarnings("deprecation")
    public BGetTokenArg() {
        _token = "";
    }

    @SuppressWarnings("deprecation")
    public BGetTokenArg(String _token_, long _maxCount_) {
        if (_token_ == null)
            _token_ = "";
        _token = _token_;
        _maxCount = _maxCount_;
    }

    @Override
    public void reset() {
        setToken("");
        setMaxCount(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Token.BGetTokenArg.Data toData() {
        var _d_ = new Zeze.Builtin.Token.BGetTokenArg.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Token.BGetTokenArg.Data)_o_);
    }

    public void assign(BGetTokenArg.Data _o_) {
        setToken(_o_._token);
        setMaxCount(_o_._maxCount);
        _unknown_ = null;
    }

    public void assign(BGetTokenArg _o_) {
        setToken(_o_.getToken());
        setMaxCount(_o_.getMaxCount());
        _unknown_ = _o_._unknown_;
    }

    public BGetTokenArg copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BGetTokenArg copy() {
        var _c_ = new BGetTokenArg();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BGetTokenArg _a_, BGetTokenArg _b_) {
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
        _s_.append("Zeze.Builtin.Token.BGetTokenArg: {\n");
        _s_.append(_i1_).append("token=").append(getToken()).append(",\n");
        _s_.append(_i1_).append("maxCount=").append(getMaxCount()).append('\n');
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
            String _x_ = getToken();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getMaxCount();
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
            setToken(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setMaxCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BGetTokenArg))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BGetTokenArg)_o_;
        if (!getToken().equals(_b_.getToken()))
            return false;
        if (getMaxCount() != _b_.getMaxCount())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getMaxCount() < 0)
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
                case 1: _token = _v_.stringValue(); break;
                case 2: _maxCount = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setToken(_r_.getString(_pn_ + "token"));
        if (getToken() == null)
            setToken("");
        setMaxCount(_r_.getLong(_pn_ + "maxCount"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "token", getToken());
        _s_.appendLong(_pn_ + "maxCount", getMaxCount());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "token", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "maxCount", "long", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 3072398246496713168L;

    private String _token; // 请求token
    private long _maxCount; // 此值＞0且请求token次数(包括当前请求)≥此值时,服务器会清除此token及绑定的所有状态(当前请求仍然能正常获得)

    public String getToken() {
        return _token;
    }

    public void setToken(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _token = _v_;
    }

    public long getMaxCount() {
        return _maxCount;
    }

    public void setMaxCount(long _v_) {
        _maxCount = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _token = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _token_, long _maxCount_) {
        if (_token_ == null)
            _token_ = "";
        _token = _token_;
        _maxCount = _maxCount_;
    }

    @Override
    public void reset() {
        _token = "";
        _maxCount = 0;
    }

    @Override
    public Zeze.Builtin.Token.BGetTokenArg toBean() {
        var _b_ = new Zeze.Builtin.Token.BGetTokenArg();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BGetTokenArg)_o_);
    }

    public void assign(BGetTokenArg _o_) {
        _token = _o_.getToken();
        _maxCount = _o_.getMaxCount();
    }

    public void assign(BGetTokenArg.Data _o_) {
        _token = _o_._token;
        _maxCount = _o_._maxCount;
    }

    @Override
    public BGetTokenArg.Data copy() {
        var _c_ = new BGetTokenArg.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BGetTokenArg.Data _a_, BGetTokenArg.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BGetTokenArg.Data clone() {
        return (BGetTokenArg.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Token.BGetTokenArg: {\n");
        _s_.append(_i1_).append("token=").append(_token).append(",\n");
        _s_.append(_i1_).append("maxCount=").append(_maxCount).append('\n');
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
            String _x_ = _token;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = _maxCount;
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
            _token = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _maxCount = _o_.ReadLong(_t_);
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
        if (!(_o_ instanceof BGetTokenArg.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BGetTokenArg.Data)_o_;
        if (!_token.equals(_b_._token))
            return false;
        if (_maxCount != _b_._maxCount)
            return false;
        return true;
    }
}
}
