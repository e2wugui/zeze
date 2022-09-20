// auto-generated @formatter:off
package Zeze.Builtin.Game.Rank;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BRankList extends Zeze.Transaction.Bean {
    public static final long TYPEID = -1625874326687776700L;

    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.Rank.BRankValue> _RankList;

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.Rank.BRankValue> getRankList() {
        return _RankList;
    }

    @SuppressWarnings("deprecation")
    public BRankList() {
        _RankList = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.Rank.BRankValue.class);
        _RankList.variableId(1);
    }

    public void assign(BRankList other) {
        getRankList().clear();
        for (var e : other.getRankList())
            getRankList().add(e.copy());
    }

    @Deprecated
    public void Assign(BRankList other) {
        assign(other);
    }

    public BRankList copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRankList copy() {
        var copy = new BRankList();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BRankList Copy() {
        return copy();
    }

    public static void swap(BRankList a, BRankList b) {
        BRankList save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Rank.BRankList: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("RankList").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getRankList()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(System.lineSeparator());
            _item_.buildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(System.lineSeparator());
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
            var _x_ = getRankList();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_)
                    _v_.encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = getRankList();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Game.Rank.BRankValue(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _RankList.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _RankList.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : getRankList()) {
            if (_v_.negativeCheck())
                return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _RankList.followerApply(vlog); break;
            }
        }
    }
}
