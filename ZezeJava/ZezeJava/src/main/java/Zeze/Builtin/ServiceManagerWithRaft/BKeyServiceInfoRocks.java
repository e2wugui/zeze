// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "RedundantSuppression", "MethodMayBeStatic", "PatternVariableCanBeUsed"})
public final class BKeyServiceInfoRocks implements Serializable, Comparable<BKeyServiceInfoRocks> {
    private String _ServiceName;
    private String _ServiceIdentity;
    private String _PassiveIp;
    private int _PassivePort;
    private Zeze.Net.Binary _ExtraInfo;
    private Zeze.Net.Binary _Param;

    // for decode only
    public BKeyServiceInfoRocks() {
        _ServiceName = "";
        _ServiceIdentity = "";
        _PassiveIp = "";
        _ExtraInfo = Zeze.Net.Binary.Empty;
        _Param = Zeze.Net.Binary.Empty;
    }

    public BKeyServiceInfoRocks(String _ServiceName_, String _ServiceIdentity_, String _PassiveIp_, int _PassivePort_, Zeze.Net.Binary _ExtraInfo_, Zeze.Net.Binary _Param_) {
        if (_ServiceName_ == null)
            throw new IllegalArgumentException();
        this._ServiceName = _ServiceName_;
        if (_ServiceIdentity_ == null)
            throw new IllegalArgumentException();
        this._ServiceIdentity = _ServiceIdentity_;
        if (_PassiveIp_ == null)
            throw new IllegalArgumentException();
        this._PassiveIp = _PassiveIp_;
        this._PassivePort = _PassivePort_;
        if (_ExtraInfo_ == null)
            throw new IllegalArgumentException();
        this._ExtraInfo = _ExtraInfo_;
        if (_Param_ == null)
            throw new IllegalArgumentException();
        this._Param = _Param_;
    }

    public String getServiceName() {
        return _ServiceName;
    }

    public String getServiceIdentity() {
        return _ServiceIdentity;
    }

    public String getPassiveIp() {
        return _PassiveIp;
    }

    public int getPassivePort() {
        return _PassivePort;
    }

    public Zeze.Net.Binary getExtraInfo() {
        return _ExtraInfo;
    }

    public Zeze.Net.Binary getParam() {
        return _Param;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ServiceManagerWithRaft.BKeyServiceInfoRocks: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName=").append(getServiceName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceIdentity=").append(getServiceIdentity()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PassiveIp=").append(getPassiveIp()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PassivePort=").append(getPassivePort()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ExtraInfo=").append(getExtraInfo()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Param=").append(getParam()).append(System.lineSeparator());
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
            String _x_ = getServiceName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getServiceIdentity();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getPassiveIp();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getPassivePort();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getExtraInfo();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = getParam();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ServiceName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ServiceIdentity = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _PassiveIp = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _PassivePort = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _ExtraInfo = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _Param = _o_.ReadBinary(_t_);
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
        if (_obj1_ instanceof BKeyServiceInfoRocks) {
            var _obj_ = (BKeyServiceInfoRocks)_obj1_;
            if (!getServiceName().equals(_obj_.getServiceName()))
                return false;
            if (!getServiceIdentity().equals(_obj_.getServiceIdentity()))
                return false;
            if (!getPassiveIp().equals(_obj_.getPassiveIp()))
                return false;
            if (getPassivePort() != _obj_.getPassivePort())
                return false;
            if (!getExtraInfo().equals(_obj_.getExtraInfo()))
                return false;
            if (!getParam().equals(_obj_.getParam()))
                return false;
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int _prime_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _prime_ + _ServiceName.hashCode();
        _h_ = _h_ * _prime_ + _ServiceIdentity.hashCode();
        _h_ = _h_ * _prime_ + _PassiveIp.hashCode();
        _h_ = _h_ * _prime_ + Integer.hashCode(_PassivePort);
        _h_ = _h_ * _prime_ + _ExtraInfo.hashCode();
        _h_ = _h_ * _prime_ + _Param.hashCode();
        return _h_;
    }

    @Override
    public int compareTo(BKeyServiceInfoRocks _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = _ServiceName.compareTo(_o_._ServiceName);
            if (_c_ != 0)
                return _c_;
            _c_ = _ServiceIdentity.compareTo(_o_._ServiceIdentity);
            if (_c_ != 0)
                return _c_;
            _c_ = _PassiveIp.compareTo(_o_._PassiveIp);
            if (_c_ != 0)
                return _c_;
            _c_ = Integer.compare(_PassivePort, _o_._PassivePort);
            if (_c_ != 0)
                return _c_;
            _c_ = _ExtraInfo.compareTo(_o_._ExtraInfo);
            if (_c_ != 0)
                return _c_;
            _c_ = _Param.compareTo(_o_._Param);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        if (getPassivePort() < 0)
            return true;
        return false;
    }
}
