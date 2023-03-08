// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BCommitTransactionResultData extends Zeze.Transaction.Data {
    public static final long TYPEID = 2317752398742334306L;

    @SuppressWarnings("deprecation")
    public BCommitTransactionResultData() {
    }

    @Override
    public Zeze.Builtin.Dbh2.BCommitTransactionResult toBean() {
        var bean = new Zeze.Builtin.Dbh2.BCommitTransactionResult();
        bean.assign(this);
        return bean;
    }

    public void assign(Zeze.Transaction.Bean other) {
        assign((BCommitTransactionResult)other);
    }

    public void assign(BCommitTransactionResult other) {
    }

    public void assign(BCommitTransactionResultData other) {
    }

    @Override
    public BCommitTransactionResultData copy() {
        var copy = new BCommitTransactionResultData();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCommitTransactionResultData a, BCommitTransactionResultData b) {
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BCommitTransactionResult: {").append(System.lineSeparator());
        level += 4;
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
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        _o_.ReadTagSize(_t_);
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(Object _o_) {
        return _o_ instanceof BCommitTransactionResult;
    }

    @Override
    public int hashCode() {
        return 0;
    }

}
