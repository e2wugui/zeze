// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BRollbackTransactionResult extends Zeze.Transaction.Bean implements BRollbackTransactionResultReadOnly {
    public static final long TYPEID = 39403420976946383L;

    @SuppressWarnings("deprecation")
    public BRollbackTransactionResult() {
    }

    @Override
    public Zeze.Builtin.Dbh2.BRollbackTransactionResultData toData() {
        var data = new Zeze.Builtin.Dbh2.BRollbackTransactionResultData();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BRollbackTransactionResultData)other);
    }

    public void assign(BRollbackTransactionResultData other) {
    }

    public void assign(BRollbackTransactionResult other) {
    }

    @Deprecated
    public void Assign(BRollbackTransactionResult other) {
        assign(other);
    }

    public BRollbackTransactionResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRollbackTransactionResult copy() {
        var copy = new BRollbackTransactionResult();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BRollbackTransactionResult Copy() {
        return copy();
    }

    public static void swap(BRollbackTransactionResult a, BRollbackTransactionResult b) {
        BRollbackTransactionResult save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BRollbackTransactionResult: {").append(System.lineSeparator());
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
    public boolean negativeCheck() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log log) {
    }
}
