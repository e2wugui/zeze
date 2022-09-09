// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BAnnounceLinkInfo extends Zeze.Transaction.Bean {
    @SuppressWarnings("deprecation")
    public BAnnounceLinkInfo() {
    }

    public void Assign(BAnnounceLinkInfo other) {
    }

    public BAnnounceLinkInfo CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BAnnounceLinkInfo Copy() {
        var copy = new BAnnounceLinkInfo();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BAnnounceLinkInfo a, BAnnounceLinkInfo b) {
        BAnnounceLinkInfo save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BAnnounceLinkInfo CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 6291432069805514560L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BAnnounceLinkInfo: {").append(System.lineSeparator());
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
    public void Encode(ByteBuffer _o_) {
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        _o_.ReadTagSize(_t_);
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void ResetChildrenRootInfo() {
    }

    @Override
    public boolean NegativeCheck() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void FollowerApply(Zeze.Transaction.Log log) {
    }
}
