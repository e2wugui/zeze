// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BBeginTransactionResultData extends Zeze.Transaction.Data {
    public static final long TYPEID = 2362617513595826008L;

    private long _TransactionId;

    public long getTransactionId() {
        return _TransactionId;
    }

    public void setTransactionId(long value) {
        _TransactionId = value;
    }

    @SuppressWarnings("deprecation")
    public BBeginTransactionResultData() {
    }

    @SuppressWarnings("deprecation")
    public BBeginTransactionResultData(long _TransactionId_) {
        _TransactionId = _TransactionId_;
    }

    @Override
    public Zeze.Builtin.Dbh2.BBeginTransactionResult toBean() {
        var bean = new Zeze.Builtin.Dbh2.BBeginTransactionResult();
        bean.assign(this);
        return bean;
    }

    public void assign(Zeze.Transaction.Bean other) {
        assign((BBeginTransactionResult)other);
    }

    public void assign(BBeginTransactionResult other) {
        setTransactionId(other.getTransactionId());
    }

    public void assign(BBeginTransactionResultData other) {
        setTransactionId(other.getTransactionId());
    }

    @Override
    public BBeginTransactionResultData copy() {
        var copy = new BBeginTransactionResultData();
        copy.assign(this);
        return copy;
    }

    public static void swap(BBeginTransactionResultData a, BBeginTransactionResultData b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BBeginTransactionResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TransactionId=").append(getTransactionId()).append(System.lineSeparator());
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
            long _x_ = getTransactionId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
            setTransactionId(_o_.ReadLong(_t_));
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
        if (!(_o_ instanceof BBeginTransactionResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBeginTransactionResult)_o_;
        if (getTransactionId() != _b_.getTransactionId())
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + Long.hashCode(_TransactionId);
        return _h_;
    }

}
