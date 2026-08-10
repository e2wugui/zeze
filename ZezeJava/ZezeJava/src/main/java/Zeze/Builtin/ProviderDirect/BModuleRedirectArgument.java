// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "ForLoopReplaceableByForEach", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "UnusedAssignment"})
public final class BModuleRedirectArgument extends Zeze.Transaction.Data {
    public static final long TYPEID = -5561456902586805165L;

    private int _ModuleId;
    private int _HashCode; // server 计算。see BBind.ChoiceType。
    private int _RedirectType; // 如果是ToServer，ServerId存在HashCode中。
    private String _MethodFullName; // format="ModuleFullName:MethodName"
    private Zeze.Net.Binary _Params;
    private String _ServiceNamePrefix;
    private int _Version; // 用于验证请求方和处理方的版本一致
    private int _Key; // 用于处理请求和回复时作为TaskOneByOne的key
    private boolean _NoOneByOne; // 是否禁用TaskOneByOne处理请求和回复

    public int getModuleId() {
        return _ModuleId;
    }

    public void setModuleId(int _v_) {
        _ModuleId = _v_;
    }

    public int getHashCode() {
        return _HashCode;
    }

    public void setHashCode(int _v_) {
        _HashCode = _v_;
    }

    public int getRedirectType() {
        return _RedirectType;
    }

    public void setRedirectType(int _v_) {
        _RedirectType = _v_;
    }

    public String getMethodFullName() {
        return _MethodFullName;
    }

    public void setMethodFullName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _MethodFullName = _v_;
    }

    public Zeze.Net.Binary getParams() {
        return _Params;
    }

    public void setParams(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Params = _v_;
    }

    public String getServiceNamePrefix() {
        return _ServiceNamePrefix;
    }

    public void setServiceNamePrefix(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ServiceNamePrefix = _v_;
    }

    public int getVersion() {
        return _Version;
    }

    public void setVersion(int _v_) {
        _Version = _v_;
    }

    public int getKey() {
        return _Key;
    }

    public void setKey(int _v_) {
        _Key = _v_;
    }

    public boolean isNoOneByOne() {
        return _NoOneByOne;
    }

    public void setNoOneByOne(boolean _v_) {
        _NoOneByOne = _v_;
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectArgument() {
        _MethodFullName = "";
        _Params = Zeze.Net.Binary.Empty;
        _ServiceNamePrefix = "";
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectArgument(int _ModuleId_, int _HashCode_, int _RedirectType_, String _MethodFullName_, Zeze.Net.Binary _Params_, String _ServiceNamePrefix_, int _Version_, int _Key_, boolean _NoOneByOne_) {
        _ModuleId = _ModuleId_;
        _HashCode = _HashCode_;
        _RedirectType = _RedirectType_;
        if (_MethodFullName_ == null)
            _MethodFullName_ = "";
        _MethodFullName = _MethodFullName_;
        if (_Params_ == null)
            _Params_ = Zeze.Net.Binary.Empty;
        _Params = _Params_;
        if (_ServiceNamePrefix_ == null)
            _ServiceNamePrefix_ = "";
        _ServiceNamePrefix = _ServiceNamePrefix_;
        _Version = _Version_;
        _Key = _Key_;
        _NoOneByOne = _NoOneByOne_;
    }

    @Override
    public void reset() {
        _ModuleId = 0;
        _HashCode = 0;
        _RedirectType = 0;
        _MethodFullName = "";
        _Params = Zeze.Net.Binary.Empty;
        _ServiceNamePrefix = "";
        _Version = 0;
        _Key = 0;
        _NoOneByOne = false;
    }

    @Override
    public Zeze.Transaction.Bean toBean() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        throw new UnsupportedOperationException();
    }

    public void assign(BModuleRedirectArgument _o_) {
        _ModuleId = _o_._ModuleId;
        _HashCode = _o_._HashCode;
        _RedirectType = _o_._RedirectType;
        _MethodFullName = _o_._MethodFullName;
        _Params = _o_._Params;
        _ServiceNamePrefix = _o_._ServiceNamePrefix;
        _Version = _o_._Version;
        _Key = _o_._Key;
        _NoOneByOne = _o_._NoOneByOne;
    }

    @Override
    public BModuleRedirectArgument copy() {
        var _c_ = new BModuleRedirectArgument();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BModuleRedirectArgument _a_, BModuleRedirectArgument _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BModuleRedirectArgument clone() {
        return (BModuleRedirectArgument)super.clone();
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
        _s_.append("Zeze.Builtin.ProviderDirect.BModuleRedirectArgument: {\n");
        _s_.append(_i1_).append("ModuleId=").append(_ModuleId).append(",\n");
        _s_.append(_i1_).append("HashCode=").append(_HashCode).append(",\n");
        _s_.append(_i1_).append("RedirectType=").append(_RedirectType).append(",\n");
        _s_.append(_i1_).append("MethodFullName=").append(_MethodFullName).append(",\n");
        _s_.append(_i1_).append("Params=").append(_Params).append(",\n");
        _s_.append(_i1_).append("ServiceNamePrefix=").append(_ServiceNamePrefix).append(",\n");
        _s_.append(_i1_).append("Version=").append(_Version).append(",\n");
        _s_.append(_i1_).append("Key=").append(_Key).append(",\n");
        _s_.append(_i1_).append("NoOneByOne=").append(_NoOneByOne).append('\n');
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
            int _x_ = _ModuleId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _HashCode;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _RedirectType;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _MethodFullName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _Params;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = _ServiceNamePrefix;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = _Version;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _Key;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = _NoOneByOne;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ModuleId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _HashCode = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _RedirectType = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _MethodFullName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _Params = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _ServiceNamePrefix = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            _Version = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            _Key = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            _NoOneByOne = _o_.ReadBool(_t_);
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
        if (!(_o_ instanceof BModuleRedirectArgument))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BModuleRedirectArgument)_o_;
        if (getModuleId() != _b_.getModuleId())
            return false;
        if (getHashCode() != _b_.getHashCode())
            return false;
        if (getRedirectType() != _b_.getRedirectType())
            return false;
        if (!getMethodFullName().equals(_b_.getMethodFullName()))
            return false;
        if (!getParams().equals(_b_.getParams()))
            return false;
        if (!getServiceNamePrefix().equals(_b_.getServiceNamePrefix()))
            return false;
        if (getVersion() != _b_.getVersion())
            return false;
        if (getKey() != _b_.getKey())
            return false;
        if (isNoOneByOne() != _b_.isNoOneByOne())
            return false;
        return true;
    }
}
