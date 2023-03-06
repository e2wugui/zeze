// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BBeginTransactionArgument extends Zeze.Transaction.Bean implements BBeginTransactionArgumentReadOnly {
    public static final long TYPEID = -7619569472530558952L;

    @SuppressWarnings("deprecation")
    public BBeginTransactionArgument() {
    }

    @Override
    public Zeze.Builtin.Dbh2.BBeginTransactionArgumentData toData() {
        var data = new Zeze.Builtin.Dbh2.BBeginTransactionArgumentData();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BBeginTransactionArgumentData)other);
    }

    public void assign(BBeginTransactionArgumentData other) {
    }

    public void assign(BBeginTransactionArgument other) {
    }

    @Deprecated
    public void Assign(BBeginTransactionArgument other) {
        assign(other);
    }

    public BBeginTransactionArgument copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBeginTransactionArgument copy() {
        var copy = new BBeginTransactionArgument();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BBeginTransactionArgument Copy() {
        return copy();
    }

    public static void swap(BBeginTransactionArgument a, BBeginTransactionArgument b) {
        BBeginTransactionArgument save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BBeginTransactionArgument: {").append(System.lineSeparator());
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
