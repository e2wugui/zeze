// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "ForLoopReplaceableByForEach", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "UnusedAssignment"})
public final class BModuleRedirectResult extends Zeze.Transaction.Data {
    public static final long TYPEID = 6325051164605397555L;

    private int _ModuleId;
    private int _ServerId; // 目标server的id。
    private Zeze.Net.Binary _Params;
    private boolean _NullParam; // 为了区分null和空的Binary/String,需要额外一个bool字段

    public int getModuleId() {
        return _ModuleId;
    }

    public void setModuleId(int _v_) {
        _ModuleId = _v_;
    }

    public int getServerId() {
        return _ServerId;
    }

    public void setServerId(int _v_) {
        _ServerId = _v_;
    }

    public Zeze.Net.Binary getParams() {
        return _Params;
    }

    public void setParams(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Params = _v_;
    }

    public boolean isNullParam() {
        return _NullParam;
    }

    public void setNullParam(boolean _v_) {
        _NullParam = _v_;
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectResult() {
        _Params = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectResult(int _ModuleId_, int _ServerId_, Zeze.Net.Binary _Params_, boolean _NullParam_) {
        _ModuleId = _ModuleId_;
        _ServerId = _ServerId_;
        if (_Params_ == null)
            _Params_ = Zeze.Net.Binary.Empty;
        _Params = _Params_;
        _NullParam = _NullParam_;
    }

    @Override
    public void reset() {
        _ModuleId = 0;
        _ServerId = 0;
        _Params = Zeze.Net.Binary.Empty;
        _NullParam = false;
    }

    @Override
    public Zeze.Transaction.Bean toBean() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        throw new UnsupportedOperationException();
    }

    public void assign(BModuleRedirectResult _o_) {
        _ModuleId = _o_._ModuleId;
        _ServerId = _o_._ServerId;
        _Params = _o_._Params;
        _NullParam = _o_._NullParam;
    }

    @Override
    public BModuleRedirectResult copy() {
        var _c_ = new BModuleRedirectResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BModuleRedirectResult _a_, BModuleRedirectResult _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BModuleRedirectResult clone() {
        return (BModuleRedirectResult)super.clone();
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
        _s_.append("Zeze.Builtin.ProviderDirect.BModuleRedirectResult: {\n");
        _s_.append(_i1_).append("ModuleId=").append(_ModuleId).append(",\n");
        _s_.append(_i1_).append("ServerId=").append(_ServerId).append(",\n");
        _s_.append(_i1_).append("Params=").append(_Params).append(",\n");
        _s_.append(_i1_).append("NullParam=").append(_NullParam).append('\n');
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
            int _x_ = _ServerId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Params;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            boolean _x_ = _NullParam;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
            _ServerId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Params = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _NullParam = _o_.ReadBool(_t_);
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
        if (!(_o_ instanceof BModuleRedirectResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BModuleRedirectResult)_o_;
        if (getModuleId() != _b_.getModuleId())
            return false;
        if (getServerId() != _b_.getServerId())
            return false;
        if (!getParams().equals(_b_.getParams()))
            return false;
        if (isNullParam() != _b_.isNullParam())
            return false;
        return true;
    }
}
