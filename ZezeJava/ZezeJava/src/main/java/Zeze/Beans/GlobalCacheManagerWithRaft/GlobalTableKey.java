// auto-generated @formatter:off
package Zeze.Beans.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

public final class GlobalTableKey implements Serializable, Comparable<GlobalTableKey> {
    private String _TableName;
    private Zeze.Net.Binary _Key;

    // for decode only
    public GlobalTableKey() {
        _TableName = "";
        _Key = Zeze.Net.Binary.Empty;
    }

    public GlobalTableKey(String _TableName_, Zeze.Net.Binary _Key_) {
        this._TableName = _TableName_;
        this._Key = _Key_;
    }

    public String getTableName() {
        return _TableName;
    }

    public Zeze.Net.Binary getKey() {
        return _Key;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TableName").append('=').append(getTableName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(getKey()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            String _x_ = getTableName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _TableName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Key = _o_.ReadBinary(_t_);
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
        if (_obj1_ instanceof GlobalTableKey) {
            var _obj_ = (GlobalTableKey)_obj1_;
            if (!getTableName().equals(_obj_.getTableName()))
                return false;
            if (!getKey().equals(_obj_.getKey()))
                return false;
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int _prime_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _prime_ + _TableName.hashCode();
        _h_ = _h_ * _prime_ + _Key.hashCode();
        return _h_;
    }

    @Override
    public int compareTo(GlobalTableKey _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = _TableName.compareTo(_o_._TableName);
            if (_c_ != 0)
                return _c_;
            _c_ = _Key.compareTo(_o_._Key);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean NegativeCheck() {
        return false;
    }
}
