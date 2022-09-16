// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BOnlineTimers extends Zeze.Transaction.Bean {
    private final Zeze.Transaction.Collections.PSet1<Long> _TimerIds;

    public Zeze.Transaction.Collections.PSet1<Long> getTimerIds() {
        return _TimerIds;
    }

    @SuppressWarnings("deprecation")
    public BOnlineTimers() {
        _TimerIds = new Zeze.Transaction.Collections.PSet1<>(Long.class);
        _TimerIds.variableId(1);
    }

    public void assign(BOnlineTimers other) {
        getTimerIds().clear();
        for (var e : other.getTimerIds())
            getTimerIds().add(e);
    }

    @Deprecated
    public void Assign(BOnlineTimers other) {
        assign(other);
    }

    public BOnlineTimers copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BOnlineTimers copy() {
        var copy = new BOnlineTimers();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BOnlineTimers Copy() {
        return copy();
    }

    public static void swap(BOnlineTimers a, BOnlineTimers b) {
        BOnlineTimers save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public BOnlineTimers copyBean() {
        return Copy();
    }

    public static final long TYPEID = 5020093653412966560L;

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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BOnlineTimers: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TimerIds").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getTimerIds()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(_item_).append(',').append(System.lineSeparator());
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
            var _x_ = getTimerIds();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_)
                    _o_.WriteLong(_v_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = getTimerIds();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
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
        _TimerIds.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _TimerIds.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : getTimerIds()) {
            if (_v_ < 0)
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
                case 1: _TimerIds.followerApply(vlog); break;
            }
        }
    }
}
