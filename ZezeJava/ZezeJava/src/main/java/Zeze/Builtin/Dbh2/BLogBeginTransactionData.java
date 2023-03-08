// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BLogBeginTransactionData extends Zeze.Transaction.Data {
    public static final long TYPEID = -992863207008701875L;

    private long _TransactionId;

    public long getTransactionId() {
        return _TransactionId;
    }

    public void setTransactionId(long value) {
        _TransactionId = value;
    }

    @SuppressWarnings("deprecation")
    public BLogBeginTransactionData() {
    }

    @SuppressWarnings("deprecation")
    public BLogBeginTransactionData(long _TransactionId_) {
        _TransactionId = _TransactionId_;
    }

    @Override
    public Zeze.Builtin.Dbh2.BLogBeginTransaction toBean() {
        var bean = new Zeze.Builtin.Dbh2.BLogBeginTransaction();
        bean.assign(this);
        return bean;
    }

    public void assign(Zeze.Transaction.Bean other) {
        assign((BLogBeginTransaction)other);
    }

    public void assign(BLogBeginTransaction other) {
        setTransactionId(other.getTransactionId());
    }

    public void assign(BLogBeginTransactionData other) {
        setTransactionId(other.getTransactionId());
    }

    @Override
    public BLogBeginTransactionData copy() {
        var copy = new BLogBeginTransactionData();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLogBeginTransactionData a, BLogBeginTransactionData b) {
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BLogBeginTransaction: {").append(System.lineSeparator());
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
        if (!(_o_ instanceof BLogBeginTransaction))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLogBeginTransaction)_o_;
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
