// auto-generated @formatter:off
package Zeze.Builtin.Auth;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "RedundantSuppression", "MethodMayBeStatic", "PatternVariableCanBeUsed", "NullableProblems", "SuspiciousNameCombination"})
public final class BAuthKey implements Zeze.Transaction.BeanKey, Comparable<BAuthKey> {
    private int _ModuleId;
    private int _ProtocolId;

    // for decode only
    public BAuthKey() {
    }

    public BAuthKey(int _ModuleId_, int _ProtocolId_) {
        this._ModuleId = _ModuleId_;
        this._ProtocolId = _ProtocolId_;
    }

    public int getModuleId() {
        return _ModuleId;
    }

    public int getProtocolId() {
        return _ProtocolId;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Auth.BAuthKey: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ModuleId=").append(_ModuleId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProtocolId=").append(_ProtocolId).append(System.lineSeparator());
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
            int _x_ = _ModuleId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _ProtocolId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ModuleId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ProtocolId = _o_.ReadInt(_t_);
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
        if (!(_o_ instanceof BAuthKey))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAuthKey)_o_;
        if (_ModuleId != _b_._ModuleId)
            return false;
        if (_ProtocolId != _b_._ProtocolId)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + Integer.hashCode(_ModuleId);
        _h_ = _h_ * _p_ + Integer.hashCode(_ProtocolId);
        return _h_;
    }

    @Override
    public int compareTo(BAuthKey _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = Integer.compare(_ModuleId, _o_._ModuleId);
            if (_c_ != 0)
                return _c_;
            _c_ = Integer.compare(_ProtocolId, _o_._ProtocolId);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        if (getModuleId() < 0)
            return true;
        if (getProtocolId() < 0)
            return true;
        return false;
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        _ModuleId = rs.getInt(_parents_name_ + "ModuleId");
        _ProtocolId = rs.getInt(_parents_name_ + "ProtocolId");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "ModuleId", _ModuleId);
        st.appendInt(_parents_name_ + "ProtocolId", _ProtocolId);
    }
}
