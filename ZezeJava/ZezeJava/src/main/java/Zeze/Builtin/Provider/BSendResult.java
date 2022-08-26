// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BSendResult extends Zeze.Transaction.Bean {
    private final Zeze.Transaction.Collections.PSet1<Long> _ErrorLinkSids;

    public Zeze.Transaction.Collections.PSet1<Long> getErrorLinkSids() {
        return _ErrorLinkSids;
    }

    public BSendResult() {
        _ErrorLinkSids = new Zeze.Transaction.Collections.PSet1<>(Long.class);
        _ErrorLinkSids.VariableId = 1;
    }

    public void Assign(BSendResult other) {
        getErrorLinkSids().clear();
        for (var e : other.getErrorLinkSids())
            getErrorLinkSids().add(e);
    }

    public BSendResult CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BSendResult Copy() {
        var copy = new BSendResult();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BSendResult a, BSendResult b) {
        BSendResult save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BSendResult CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -7186434891670297524L;

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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BSendResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ErrorLinkSids").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getErrorLinkSids()) {
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
            var _x_ = getErrorLinkSids();
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
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = getErrorLinkSids();
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
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _ErrorLinkSids.InitRootInfo(root, this);
    }

    @Override
    protected void ResetChildrenRootInfo() {
        _ErrorLinkSids.ResetRootInfo();
    }

    @Override
    public boolean NegativeCheck() {
        for (var _v_ : getErrorLinkSids()) {
            if (_v_ < 0)
                return true;
        }
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
                case 1: _ErrorLinkSids.FollowerApply(vlog); break;
            }
        }
    }
}
