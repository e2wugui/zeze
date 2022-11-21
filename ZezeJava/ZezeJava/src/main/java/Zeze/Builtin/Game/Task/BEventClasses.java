// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BEventClasses extends Zeze.Transaction.Bean implements BEventClassesReadOnly {
    public static final long TYPEID = -6842044204994485959L;

    private final Zeze.Transaction.Collections.PSet1<String> _EventClasses;

    public Zeze.Transaction.Collections.PSet1<String> getEventClasses() {
        return _EventClasses;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<String> getEventClassesReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_EventClasses);
    }

    @SuppressWarnings("deprecation")
    public BEventClasses() {
        _EventClasses = new Zeze.Transaction.Collections.PSet1<>(String.class);
        _EventClasses.variableId(1);
    }

    public void assign(BEventClasses other) {
        _EventClasses.clear();
        _EventClasses.addAll(other._EventClasses);
    }

    @Deprecated
    public void Assign(BEventClasses other) {
        assign(other);
    }

    public BEventClasses copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BEventClasses copy() {
        var copy = new BEventClasses();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BEventClasses Copy() {
        return copy();
    }

    public static void swap(BEventClasses a, BEventClasses b) {
        BEventClasses save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Task.BEventClasses: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("EventClasses={");
        if (!_EventClasses.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _EventClasses) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(System.lineSeparator());
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
            var _x_ = _EventClasses;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteString(_v_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _EventClasses;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
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
        _EventClasses.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _EventClasses.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
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
                case 1: _EventClasses.followerApply(vlog); break;
            }
        }
    }
}
