// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BNode extends Zeze.Transaction.Bean {
    private long _PrevNodeId;
    private long _NextNodeId;
    private final Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Timer.BTimer> _Timers;

    public long getPrevNodeId() {
        if (!isManaged())
            return _PrevNodeId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PrevNodeId;
        var log = (Log__PrevNodeId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _PrevNodeId;
    }

    public void setPrevNodeId(long value) {
        if (!isManaged()) {
            _PrevNodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__PrevNodeId(this, 1, value));
    }

    public long getNextNodeId() {
        if (!isManaged())
            return _NextNodeId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NextNodeId;
        var log = (Log__NextNodeId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _NextNodeId;
    }

    public void setNextNodeId(long value) {
        if (!isManaged()) {
            _NextNodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NextNodeId(this, 2, value));
    }

    public Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Timer.BTimer> getTimers() {
        return _Timers;
    }

    @SuppressWarnings("deprecation")
    public BNode() {
        _Timers = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Timer.BTimer.class);
        _Timers.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BNode(long _PrevNodeId_, long _NextNodeId_) {
        _PrevNodeId = _PrevNodeId_;
        _NextNodeId = _NextNodeId_;
        _Timers = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Timer.BTimer.class);
        _Timers.variableId(3);
    }

    public void assign(BNode other) {
        setPrevNodeId(other.getPrevNodeId());
        setNextNodeId(other.getNextNodeId());
        getTimers().clear();
        for (var e : other.getTimers().entrySet())
            getTimers().put(e.getKey(), e.getValue().copy());
    }

    @Deprecated
    public void Assign(BNode other) {
        assign(other);
    }

    public BNode copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BNode copy() {
        var copy = new BNode();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BNode Copy() {
        return copy();
    }

    public static void swap(BNode a, BNode b) {
        BNode save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public BNode copyBean() {
        return Copy();
    }

    public static final long TYPEID = -44647384323818353L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__PrevNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__PrevNodeId(BNode bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BNode)getBelong())._PrevNodeId = value; }
    }

    private static final class Log__NextNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__NextNodeId(BNode bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BNode)getBelong())._NextNodeId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BNode: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("PrevNodeId").append('=').append(getPrevNodeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NextNodeId").append('=').append(getNextNodeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Timers").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getTimers().entrySet()) {
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
            long _x_ = getPrevNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getNextNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getTimers();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
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
            setPrevNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setNextNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = getTimers();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Timer.BTimer(), _t_);
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
        _Timers.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _Timers.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getPrevNodeId() < 0)
            return true;
        if (getNextNodeId() < 0)
            return true;
        for (var _v_ : getTimers().values()) {
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
                case 1: _PrevNodeId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _NextNodeId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _Timers.followerApply(vlog); break;
            }
        }
    }
}
