// auto-generated @formatter:off
package Zeze.Builtin.AutoKeyOld;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"MethodMayBeStatic", "NullableProblems", "PatternVariableCanBeUsed", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "UnusedAssignment"})
public final class BSeedKey implements Zeze.Transaction.BeanKey, Comparable<BSeedKey> {
    private int _ServerId;
    private String _KeyName;

    // for decode only
    public BSeedKey() {
        _KeyName = "";
    }

    public BSeedKey(int _ServerId_, String _KeyName_) {
        this._ServerId = _ServerId_;
        if (_KeyName_ == null)
            throw new IllegalArgumentException();
        if (_KeyName_.length() > 256)
            throw new IllegalArgumentException();
        this._KeyName = _KeyName_;
    }

    public int getServerId() {
        return _ServerId;
    }

    public String getKeyName() {
        return _KeyName;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.AutoKeyOld.BSeedKey: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId=").append(_ServerId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("KeyName=").append(_KeyName).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            int _x_ = _ServerId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _KeyName;
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
            _ServerId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _KeyName = _o_.ReadString(_t_);
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
        if (!(_o_ instanceof BSeedKey))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSeedKey)_o_;
        if (_ServerId != _b_._ServerId)
            return false;
        if (!_KeyName.equals(_b_._KeyName))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + Integer.hashCode(_ServerId);
        _h_ = _h_ * _p_ + _KeyName.hashCode();
        return _h_;
    }

    @Override
    public int compareTo(BSeedKey _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = Integer.compare(_ServerId, _o_._ServerId);
            if (_c_ != 0)
                return _c_;
            _c_ = _KeyName.compareTo(_o_._KeyName);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        if (getServerId() < 0)
            return true;
        return false;
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        _ServerId = rs.getInt(_parents_name_ + "ServerId");
        _KeyName = rs.getString(_parents_name_ + "KeyName");
        if (_KeyName == null)
            _KeyName = "";
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "ServerId", _ServerId);
        st.appendString(_parents_name_ + "KeyName", _KeyName);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = new java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data>();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServerId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "KeyName", "string", "", ""));
        return vars;
    }
}
