// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "ForLoopReplaceableByForEach", "RedundantIfStatement", "RedundantSuppression", "UnusedAssignment"})
public final class BOnlyData extends Zeze.Transaction.Data {
    public static final long TYPEID = -8136515036661024621L;

    private int _s; // com aa

    public int getS() {
        return _s;
    }

    public void setS(int _v_) {
        _s = _v_;
    }

    @SuppressWarnings("deprecation")
    public BOnlyData() {
        _s = 1;
    }

    @SuppressWarnings("deprecation")
    public BOnlyData(int _s_) {
        _s = _s_;
    }

    @Override
    public void reset() {
        _s = 1;
    }

    @Override
    public Zeze.Transaction.Bean toBean() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        throw new UnsupportedOperationException();
    }

    public void assign(BOnlyData _o_) {
        _s = _o_._s;
    }

    @Override
    public BOnlyData copy() {
        var _c_ = new BOnlyData();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BOnlyData _a_, BOnlyData _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BOnlyData clone() {
        return (BOnlyData)super.clone();
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
        _s_.append("demo.Module1.BOnlyData: {\n");
        _s_.append(_i1_).append("s=").append(_s).append('\n');
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
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _s = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            _s = 0;
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BOnlyData))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BOnlyData)_o_;
        if (getS() != _b_.getS())
            return false;
        return true;
    }
}
