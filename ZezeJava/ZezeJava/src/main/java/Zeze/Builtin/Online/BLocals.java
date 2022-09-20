// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BLocals extends Zeze.Transaction.Bean {
    public static final long TYPEID = -3801167864121993305L;

    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Online.BLocal> _Logins; // key is ClientId

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Online.BLocal> getLogins() {
        return _Logins;
    }

    @SuppressWarnings("deprecation")
    public BLocals() {
        _Logins = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Online.BLocal.class);
        _Logins.variableId(1);
    }

    public void assign(BLocals other) {
        getLogins().clear();
        for (var e : other.getLogins().entrySet())
            getLogins().put(e.getKey(), e.getValue().copy());
    }

    @Deprecated
    public void Assign(BLocals other) {
        assign(other);
    }

    public BLocals copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLocals copy() {
        var copy = new BLocals();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BLocals Copy() {
        return copy();
    }

    public static void swap(BLocals a, BLocals b) {
        BLocals save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Online.BLocals: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Logins").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getLogins().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().buildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
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
            var _x_ = getLogins();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().encode(_o_);
                }
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = getLogins();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Online.BLocal(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Logins.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _Logins.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : getLogins().values()) {
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
                case 1: _Logins.followerApply(vlog); break;
            }
        }
    }
}
