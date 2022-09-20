// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BOnlineTimers extends Zeze.Transaction.Bean {
    public static final long TYPEID = 5020093653412966560L;

    private final Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Timer.BOnlineCustom> _TimerIds;
    private final Zeze.Transaction.Collections.PSet1<String> _NamedNames;

    public Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Timer.BOnlineCustom> getTimerIds() {
        return _TimerIds;
    }

    public Zeze.Transaction.Collections.PSet1<String> getNamedNames() {
        return _NamedNames;
    }

    @SuppressWarnings("deprecation")
    public BOnlineTimers() {
        _TimerIds = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Timer.BOnlineCustom.class);
        _TimerIds.variableId(1);
        _NamedNames = new Zeze.Transaction.Collections.PSet1<>(String.class);
        _NamedNames.variableId(2);
    }

    public void assign(BOnlineTimers other) {
        getTimerIds().clear();
        for (var e : other.getTimerIds().entrySet())
            getTimerIds().put(e.getKey(), e.getValue().copy());
        getNamedNames().clear();
        for (var e : other.getNamedNames())
            getNamedNames().add(e);
    }

    @Deprecated
    public void Assign(BOnlineTimers other) {
        assign(other);
    }

    public BOnlineTimers copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
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
        for (var _kv_ : getTimerIds().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().buildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NamedNames").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getNamedNames()) {
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
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                }
            }
        }
        {
            var _x_ = getNamedNames();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
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
            var _x_ = getTimerIds();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Timer.BOnlineCustom(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = getNamedNames();
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
        _TimerIds.initRootInfo(root, this);
        _NamedNames.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _TimerIds.resetRootInfo();
        _NamedNames.resetRootInfo();
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
                case 1: _TimerIds.followerApply(vlog); break;
                case 2: _NamedNames.followerApply(vlog); break;
            }
        }
    }
}
