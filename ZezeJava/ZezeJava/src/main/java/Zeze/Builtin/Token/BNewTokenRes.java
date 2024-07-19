// auto-generated @formatter:off
package Zeze.Builtin.Token;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BNewTokenRes extends Zeze.Transaction.Bean implements BNewTokenResReadOnly {
    public static final long TYPEID = 4376622442286005260L;

    private String _token; // 新分配的token. RPC回复成功时有效,否则返回空串. 目前的设计是24个半角字符(大小写字母和数字组合)

    @Override
    public String getToken() {
        if (!isManaged())
            return _token;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _token;
        var log = (Log__token)_t_.getLog(objectId() + 1);
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
        _t_.putLog(new Log__token(this, 1, _v_));
    }

    @SuppressWarnings("deprecation")
    public BNewTokenRes() {
        _token = "";
    }

    @SuppressWarnings("deprecation")
    public BNewTokenRes(String _token_) {
        if (_token_ == null)
            _token_ = "";
        _token = _token_;
    }

    @Override
    public void reset() {
        setToken("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Token.BNewTokenRes.Data toData() {
        var _d_ = new Zeze.Builtin.Token.BNewTokenRes.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Token.BNewTokenRes.Data)_o_);
    }

    public void assign(BNewTokenRes.Data _o_) {
        setToken(_o_._token);
        _unknown_ = null;
    }

    public void assign(BNewTokenRes _o_) {
        setToken(_o_.getToken());
        _unknown_ = _o_._unknown_;
    }

    public BNewTokenRes copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BNewTokenRes copy() {
        var _c_ = new BNewTokenRes();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BNewTokenRes _a_, BNewTokenRes _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__token extends Zeze.Transaction.Logs.LogString {
        public Log__token(BNewTokenRes _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BNewTokenRes)getBelong())._token = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Token.BNewTokenRes: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("token=").append(getToken()).append(System.lineSeparator());
        _l_ -= 4;
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
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BNewTokenRes))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BNewTokenRes)_o_;
        if (!getToken().equals(_b_.getToken()))
            return false;
        return true;
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
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setToken(_r_.getString(_pn_ + "token"));
        if (getToken() == null)
            setToken("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "token", getToken());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "token", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4376622442286005260L;

    private String _token; // 新分配的token. RPC回复成功时有效,否则返回空串. 目前的设计是24个半角字符(大小写字母和数字组合)

    public String getToken() {
        return _token;
    }

    public void setToken(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _token = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _token = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _token_) {
        if (_token_ == null)
            _token_ = "";
        _token = _token_;
    }

    @Override
    public void reset() {
        _token = "";
    }

    @Override
    public Zeze.Builtin.Token.BNewTokenRes toBean() {
        var _b_ = new Zeze.Builtin.Token.BNewTokenRes();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BNewTokenRes)_o_);
    }

    public void assign(BNewTokenRes _o_) {
        _token = _o_.getToken();
    }

    public void assign(BNewTokenRes.Data _o_) {
        _token = _o_._token;
    }

    @Override
    public BNewTokenRes.Data copy() {
        var _c_ = new BNewTokenRes.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BNewTokenRes.Data _a_, BNewTokenRes.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BNewTokenRes.Data clone() {
        return (BNewTokenRes.Data)super.clone();
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Token.BNewTokenRes: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("token=").append(_token).append(System.lineSeparator());
        _l_ -= 4;
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
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
