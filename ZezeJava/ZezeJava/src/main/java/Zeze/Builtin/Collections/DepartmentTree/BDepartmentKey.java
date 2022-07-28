// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "RedundantSuppression", "MethodMayBeStatic", "PatternVariableCanBeUsed"})
public final class BDepartmentKey implements Serializable, Comparable<BDepartmentKey> {
    private String _Owner;
    private long _DepartmentId;

    // for decode only
    public BDepartmentKey() {
        _Owner = "";
    }

    public BDepartmentKey(String _Owner_, long _DepartmentId_) {
        this._Owner = _Owner_;
        this._DepartmentId = _DepartmentId_;
    }

    public String getOwner() {
        return _Owner;
    }

    public long getDepartmentId() {
        return _DepartmentId;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Owner").append('=').append(getOwner()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("DepartmentId").append('=').append(getDepartmentId()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            String _x_ = getOwner();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getDepartmentId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Owner = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _DepartmentId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(Object _obj1_) {
        if (_obj1_ == this)
            return true;
        if (_obj1_ instanceof BDepartmentKey) {
            var _obj_ = (BDepartmentKey)_obj1_;
            if (!getOwner().equals(_obj_.getOwner()))
                return false;
            if (getDepartmentId() != _obj_.getDepartmentId())
                return false;
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int _prime_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _prime_ + _Owner.hashCode();
        _h_ = _h_ * _prime_ + Long.hashCode(_DepartmentId);
        return _h_;
    }

    @Override
    public int compareTo(BDepartmentKey _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = _Owner.compareTo(_o_._Owner);
            if (_c_ != 0)
                return _c_;
            _c_ = Long.compare(_DepartmentId, _o_._DepartmentId);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean NegativeCheck() {
        if (getDepartmentId() < 0)
            return true;
        return false;
    }
}
