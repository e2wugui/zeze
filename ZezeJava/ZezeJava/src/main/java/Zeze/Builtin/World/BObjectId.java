// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "RedundantSuppression", "MethodMayBeStatic", "PatternVariableCanBeUsed", "NullableProblems", "SuspiciousNameCombination"})
public final class BObjectId implements Serializable, Comparable<BObjectId> {
    private int _Type;
    private int _ConfigId;
    private long _InstanceId;

    // for decode only
    public BObjectId() {
    }

    public BObjectId(int _Type_, int _ConfigId_, long _InstanceId_) {
        this._Type = _Type_;
        this._ConfigId = _ConfigId_;
        this._InstanceId = _InstanceId_;
    }

    public int getType() {
        return _Type;
    }

    public int getConfigId() {
        return _ConfigId;
    }

    public long getInstanceId() {
        return _InstanceId;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BObjectId: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Type=").append(_Type).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ConfigId=").append(_ConfigId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("InstanceId=").append(_InstanceId).append(System.lineSeparator());
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
            int _x_ = _Type;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _ConfigId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = _InstanceId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Type = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ConfigId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _InstanceId = _o_.ReadLong(_t_);
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
        if (!(_o_ instanceof BObjectId))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BObjectId)_o_;
        if (_Type != _b_._Type)
            return false;
        if (_ConfigId != _b_._ConfigId)
            return false;
        if (_InstanceId != _b_._InstanceId)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + Integer.hashCode(_Type);
        _h_ = _h_ * _p_ + Integer.hashCode(_ConfigId);
        _h_ = _h_ * _p_ + Long.hashCode(_InstanceId);
        return _h_;
    }

    @Override
    public int compareTo(BObjectId _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = Integer.compare(_Type, _o_._Type);
            if (_c_ != 0)
                return _c_;
            _c_ = Integer.compare(_ConfigId, _o_._ConfigId);
            if (_c_ != 0)
                return _c_;
            _c_ = Long.compare(_InstanceId, _o_._InstanceId);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        if (getType() < 0)
            return true;
        if (getConfigId() < 0)
            return true;
        if (getInstanceId() < 0)
            return true;
        return false;
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        _Type = rs.getInt(_parents_name_ + "Type");
        _ConfigId = rs.getInt(_parents_name_ + "ConfigId");
        _InstanceId = rs.getLong(_parents_name_ + "InstanceId");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "Type", _Type);
        st.appendInt(_parents_name_ + "ConfigId", _ConfigId);
        st.appendLong(_parents_name_ + "InstanceId", _InstanceId);
    }
}
