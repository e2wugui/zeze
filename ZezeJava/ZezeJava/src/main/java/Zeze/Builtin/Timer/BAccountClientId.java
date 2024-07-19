// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"MethodMayBeStatic", "NullableProblems", "PatternVariableCanBeUsed", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "UnusedAssignment"})
public final class BAccountClientId implements Zeze.Transaction.BeanKey, Comparable<BAccountClientId> {
    private String _Account; // 账号名
    private String _ClientId; // 客户端ID

    // for decode only
    public BAccountClientId() {
        _Account = "";
        _ClientId = "";
    }

    public BAccountClientId(String _Account_, String _ClientId_) {
        if (_Account_ == null)
            throw new IllegalArgumentException();
        if (_Account_.length() > 256)
            throw new IllegalArgumentException();
        this._Account = _Account_;
        if (_ClientId_ == null)
            throw new IllegalArgumentException();
        if (_ClientId_.length() > 256)
            throw new IllegalArgumentException();
        this._ClientId = _ClientId_;
    }

    public String getAccount() {
        return _Account;
    }

    public String getClientId() {
        return _ClientId;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        _s_.append(System.lineSeparator());
        return _s_.toString();
    }

    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Timer.BAccountClientId: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Account=").append(_Account).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("ClientId=").append(_ClientId).append(System.lineSeparator());
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
            String _x_ = _ClientId;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
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
            _Account = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ClientId = _o_.ReadString(_t_);
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
        if (!(_o_ instanceof BAccountClientId))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAccountClientId)_o_;
        if (!_Account.equals(_b_._Account))
            return false;
        if (!_ClientId.equals(_b_._ClientId))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + _Account.hashCode();
        _h_ = _h_ * _p_ + _ClientId.hashCode();
        return _h_;
    }

    @Override
    public int compareTo(BAccountClientId _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = _Account.compareTo(_o_._Account);
            if (_c_ != 0)
                return _c_;
            _c_ = _ClientId.compareTo(_o_._ClientId);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        return false;
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _Account = _r_.getString(_pn_ + "Account");
        if (_Account == null)
            _Account = "";
        _ClientId = _r_.getString(_pn_ + "ClientId");
        if (_ClientId == null)
            _ClientId = "";
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Account", _Account);
        _s_.appendString(_pn_ + "ClientId", _ClientId);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = new java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data>();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Account", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ClientId", "string", "", ""));
        return vars;
    }
}
