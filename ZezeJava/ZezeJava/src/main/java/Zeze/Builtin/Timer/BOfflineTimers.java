// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BOfflineTimers extends Zeze.Transaction.Bean implements BOfflineTimersReadOnly {
    public static final long TYPEID = -4429519688247847602L;

    private final Zeze.Transaction.Collections.PMap1<String, Integer> _OfflineTimers;

    public Zeze.Transaction.Collections.PMap1<String, Integer> getOfflineTimers() {
        return _OfflineTimers;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<String, Integer> getOfflineTimersReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_OfflineTimers);
    }

    @SuppressWarnings("deprecation")
    public BOfflineTimers() {
        _OfflineTimers = new Zeze.Transaction.Collections.PMap1<>(String.class, Integer.class);
        _OfflineTimers.variableId(1);
    }

    public void assign(BOfflineTimers other) {
        _OfflineTimers.clear();
        _OfflineTimers.putAll(other._OfflineTimers);
    }

    @Deprecated
    public void Assign(BOfflineTimers other) {
        assign(other);
    }

    public BOfflineTimers copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOfflineTimers copy() {
        var copy = new BOfflineTimers();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BOfflineTimers Copy() {
        return copy();
    }

    public static void swap(BOfflineTimers a, BOfflineTimers b) {
        BOfflineTimers save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BOfflineTimers: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("OfflineTimers={");
        if (!_OfflineTimers.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _OfflineTimers.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_kv_.getValue()).append(',').append(System.lineSeparator());
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
            var _x_ = _OfflineTimers;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
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
            var _x_ = _OfflineTimers;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadInt(_t_);
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
        _OfflineTimers.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _OfflineTimers.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _OfflineTimers.values()) {
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
                case 1: _OfflineTimers.followerApply(vlog); break;
            }
        }
    }
}
