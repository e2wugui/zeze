// auto-generated @formatter:off
package Zeze.Arch.Beans;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BSendResult extends Zeze.Transaction.Bean {
    public static final long TYPEID = -7186434891670297524L;

    private final Zeze.Transaction.Collections.PList1<Long> _ErrorLinkSids;

    public Zeze.Transaction.Collections.PList1<Long> getErrorLinkSids() {
        return _ErrorLinkSids;
    }

    @SuppressWarnings("deprecation")
    public BSendResult() {
        _ErrorLinkSids = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _ErrorLinkSids.variableId(1);
    }

    public void assign(BSendResult other) {
        _ErrorLinkSids.clear();
        _ErrorLinkSids.addAll(other._ErrorLinkSids);
    }

    @Deprecated
    public void Assign(BSendResult other) {
        assign(other);
    }

    public BSendResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSendResult copy() {
        var copy = new BSendResult();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BSendResult Copy() {
        return copy();
    }

    public static void swap(BSendResult a, BSendResult b) {
        BSendResult save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BSendResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ErrorLinkSids=[");
        if (!_ErrorLinkSids.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _ErrorLinkSids) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(System.lineSeparator());
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
            var _x_ = _ErrorLinkSids;
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
            var _x_ = _ErrorLinkSids;
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
        _ErrorLinkSids.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _ErrorLinkSids.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _ErrorLinkSids) {
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
                case 1: _ErrorLinkSids.followerApply(vlog); break;
            }
        }
    }
}
