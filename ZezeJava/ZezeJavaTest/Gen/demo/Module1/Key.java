// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

// beankey的注释
@SuppressWarnings({"MethodMayBeStatic", "ConstantConditions", "ConstantValue", "PatternVariableCanBeUsed", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "UnusedAssignment"})
public final class Key implements Zeze.Transaction.BeanKey, Comparable<Key> {
    public static final int Enum1 = 4; // enum的注释

    private short _s; // com 2
    private String _str; // com 2

    // for decode only
    public Key() {
        _s = 1;
        _str = "";
    }

    public Key(short _s_, String _str_) {
        this._s = _s_;
        if (_str_ == null)
            throw new IllegalArgumentException();
        if (_str_.length() > 256)
            throw new IllegalArgumentException();
        this._str = _str_;
    }

    public short getS() {
        return _s;
    }

    public String getStr() {
        return _str;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("demo.Module1.Key: {\n");
        _s_.append(_i1_).append("s=").append(_s).append(",\n");
        _s_.append(_i1_).append("str=").append(_str).append('\n');
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
            int _x_ = _s;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _str;
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
            _s = _o_.ReadShort(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            _s = 0;
        if (_i_ == 2) {
            _str = _o_.ReadString(_t_);
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
        if (!(_o_ instanceof Key))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (Key)_o_;
        if (_s != _b_._s)
            return false;
        if (!_str.equals(_b_._str))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + Short.hashCode(_s);
        _h_ = _h_ * _p_ + _str.hashCode();
        return _h_;
    }

    @Override
    public int compareTo(Key _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = Short.compare(_s, _o_._s);
            if (_c_ != 0)
                return _c_;
            _c_ = _str.compareTo(_o_._str);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        if (getS() < 0)
            return true;
        return false;
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s = _r_.getShort(_pn_ + "s");
        _str = _r_.getString(_pn_ + "str");
        if (_str == null)
            _str = "";
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendShort(_pn_ + "s", _s);
        _s_.appendString(_pn_ + "str", _str);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = new java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data>();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "s", "short", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "str", "string", "", ""));
        return vars;
    }
}
