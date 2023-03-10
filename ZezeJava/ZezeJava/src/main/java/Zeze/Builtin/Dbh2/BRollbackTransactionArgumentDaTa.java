// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BRollbackTransactionArgumentDaTa extends Zeze.Transaction.Data {
    public static final long TYPEID = -3144088224662789521L;

    private long _TransactionId;

    public long getTransactionId() {
        return _TransactionId;
    }

    public void setTransactionId(long value) {
        _TransactionId = value;
    }

    @SuppressWarnings("deprecation")
    public BRollbackTransactionArgumentDaTa() {
    }

    @SuppressWarnings("deprecation")
    public BRollbackTransactionArgumentDaTa(long _TransactionId_) {
        _TransactionId = _TransactionId_;
    }

    @Override
    public Zeze.Builtin.Dbh2.BRollbackTransactionArgument toBean() {
        var bean = new Zeze.Builtin.Dbh2.BRollbackTransactionArgument();
        bean.assign(this);
        return bean;
    }

    public void assign(Zeze.Transaction.Bean other) {
        assign((BRollbackTransactionArgument)other);
    }

    public void assign(BRollbackTransactionArgument other) {
        setTransactionId(other.getTransactionId());
    }

    public void assign(BRollbackTransactionArgumentDaTa other) {
        setTransactionId(other.getTransactionId());
    }

    @Override
    public BRollbackTransactionArgumentDaTa copy() {
        var copy = new BRollbackTransactionArgumentDaTa();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRollbackTransactionArgumentDaTa a, BRollbackTransactionArgumentDaTa b) {
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BRollbackTransactionArgument: {").append(System.lineSeparator());
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

}
