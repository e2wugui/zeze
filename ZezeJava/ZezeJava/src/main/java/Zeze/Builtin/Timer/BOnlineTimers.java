// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 这个Bean作为Online.Local.Any存储
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BOnlineTimers extends Zeze.Transaction.Bean implements BOnlineTimersReadOnly {
    public static final long TYPEID = 5020093653412966560L;

    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Timer.BOnlineCustom> _TimerIds;

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Timer.BOnlineCustom> getTimerIds() {
        return _TimerIds;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Timer.BOnlineCustom, Zeze.Builtin.Timer.BOnlineCustomReadOnly> getTimerIdsReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_TimerIds);
    }

    @SuppressWarnings("deprecation")
    public BOnlineTimers() {
        _TimerIds = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Timer.BOnlineCustom.class);
        _TimerIds.variableId(1);
    }

    @Override
    public void reset() {
        _TimerIds.clear();
        _unknown_ = null;
    }

    public void assign(BOnlineTimers other) {
        _TimerIds.clear();
        for (var e : other._TimerIds.entrySet())
            _TimerIds.put(e.getKey(), e.getValue().copy());
        _unknown_ = other._unknown_;
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
        sb.append(Zeze.Util.Str.indent(level)).append("TimerIds={");
        if (!_TimerIds.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _TimerIds.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
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

    private byte[] _unknown_;

    public byte[] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
        int _i_ = 0;
        {
            var _x_ = _TimerIds;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _TimerIds;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Timer.BOnlineCustom(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _TimerIds.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _TimerIds.initRootInfoWithRedo(root, this);
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

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonMap(this, "TimerIds", _TimerIds, rs.getString(_parents_name_ + "TimerIds"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "TimerIds", Zeze.Serialize.Helper.encodeJson(_TimerIds));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TimerIds", "map", "string", "Zeze.Builtin.Timer.BOnlineCustom"));
        return vars;
    }
}
