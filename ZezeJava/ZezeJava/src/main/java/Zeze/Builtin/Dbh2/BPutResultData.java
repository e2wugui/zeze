// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BPutResultData extends Zeze.Transaction.Data {
    public static final long TYPEID = 2501510166579002026L;

    @SuppressWarnings("deprecation")
    public BPutResultData() {
    }

    @Override
    public Zeze.Builtin.Dbh2.BPutResult toBean() {
        var bean = new Zeze.Builtin.Dbh2.BPutResult();
        bean.assign(this);
        return bean;
    }

    public void assign(Zeze.Transaction.Bean other) {
        assign((BPutResult)other);
    }

    public void assign(BPutResult other) {
    }

    public void assign(BPutResultData other) {
    }

    @Override
    public BPutResultData copy() {
        var copy = new BPutResultData();
        copy.assign(this);
        return copy;
    }

    public static void swap(BPutResultData a, BPutResultData b) {
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BPutResult: {").append(System.lineSeparator());
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
        return _o_ instanceof BPutResult;
    }

    @Override
    public int hashCode() {
        return 0;
    }

}
