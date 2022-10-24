// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BCustomClasses extends Zeze.Transaction.Bean implements BCustomClassesReadOnly {
    public static final long TYPEID = -6120785275253681446L;

    private final Zeze.Transaction.Collections.PSet1<String> _CustomClasses;

    public Zeze.Transaction.Collections.PSet1<String> getCustomClasses() {
        return _CustomClasses;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<String> getCustomClassesReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_CustomClasses);
    }

    @SuppressWarnings("deprecation")
    public BCustomClasses() {
        _CustomClasses = new Zeze.Transaction.Collections.PSet1<>(String.class);
        _CustomClasses.variableId(1);
    }

    public void assign(BCustomClasses other) {
        _CustomClasses.clear();
        _CustomClasses.addAll(other._CustomClasses);
    }

    @Deprecated
    public void Assign(BCustomClasses other) {
        assign(other);
    }

    public BCustomClasses copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCustomClasses copy() {
        var copy = new BCustomClasses();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BCustomClasses Copy() {
        return copy();
    }

    public static void swap(BCustomClasses a, BCustomClasses b) {
        BCustomClasses save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BCustomClasses: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CustomClasses").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : _CustomClasses) {
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
            var _x_ = _CustomClasses;
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
            var _x_ = _CustomClasses;
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
        _CustomClasses.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _CustomClasses.resetRootInfo();
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
                case 1: _CustomClasses.followerApply(vlog); break;
            }
        }
    }
}
