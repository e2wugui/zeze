// auto-generated @formatter:off
package Zeze.Beans.Game.Rank;

import Zeze.Serialize.ByteBuffer;

public final class BRankList extends Zeze.Transaction.Bean {
    private final Zeze.Transaction.Collections.PList2<Zeze.Beans.Game.Rank.BRankValue> _RankList;

    public Zeze.Transaction.Collections.PList2<Zeze.Beans.Game.Rank.BRankValue> getRankList() {
        return _RankList;
    }

    public BRankList() {
         this(0);
    }

    public BRankList(int _varId_) {
        super(_varId_);
        _RankList = new Zeze.Transaction.Collections.PList2<>(getObjectId() + 1, (_v) -> new Log__RankList(this, _v));
    }

    public void Assign(BRankList other) {
        getRankList().clear();
        for (var e : other.getRankList())
            getRankList().add(e.Copy());
    }

    public BRankList CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BRankList Copy() {
        var copy = new BRankList();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BRankList a, BRankList b) {
        BRankList save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -81169960734319308L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__RankList extends Zeze.Transaction.Collections.PList.LogV<Zeze.Beans.Game.Rank.BRankValue> {
        public Log__RankList(BRankList host, org.pcollections.PVector<Zeze.Beans.Game.Rank.BRankValue> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 1; }
        public BRankList getBeanTyped() { return (BRankList)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._RankList); }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Beans.Game.Rank.BRankList: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("RankList").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getRankList()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(System.lineSeparator());
            _item_.BuildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            var _x_ = getRankList();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_)
                    _v_.Encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = getRankList();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Beans.Game.Rank.BRankValue(), _t_));
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _RankList.InitRootInfo(root, this);
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean NegativeCheck() {
        for (var _v_ : getRankList()) {
            if (_v_.NegativeCheck())
                return true;
        }
        return false;
    }
}
