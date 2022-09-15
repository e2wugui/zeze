// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BCustomClasses extends Zeze.Transaction.Bean {
    private final Zeze.Transaction.Collections.PSet1<String> _CustomClasses;

    public Zeze.Transaction.Collections.PSet1<String> getCustomClasses() {
        return _CustomClasses;
    }

    @SuppressWarnings("deprecation")
    public BCustomClasses() {
        _CustomClasses = new Zeze.Transaction.Collections.PSet1<>(String.class);
        _CustomClasses.variableId(1);
    }

    public void Assign(BCustomClasses other) {
        getCustomClasses().clear();
        for (var e : other.getCustomClasses())
            getCustomClasses().add(e);
    }

    public BCustomClasses CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BCustomClasses Copy() {
        var copy = new BCustomClasses();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BCustomClasses a, BCustomClasses b) {
        BCustomClasses save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BCustomClasses CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -6120785275253681446L;

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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BCustomClasses: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CustomClasses").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getCustomClasses()) {
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
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            var _x_ = getCustomClasses();
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
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = getCustomClasses();
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
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _CustomClasses.InitRootInfo(root, this);
    }

    @Override
    protected void ResetChildrenRootInfo() {
        _CustomClasses.ResetRootInfo();
    }

    @Override
    public boolean NegativeCheck() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void FollowerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _CustomClasses.FollowerApply(vlog); break;
            }
        }
    }
}
