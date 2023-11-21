// auto-generated rocks @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BSession extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = -1467883983153477901L;

    private long _SessionId;
    private int _OfflineRegisterServerId;
    private final Zeze.Raft.RocksRaft.CollMap2<String, Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotifyRocks> _OfflineRegisterNotifies;
    private final Zeze.Raft.RocksRaft.CollMap2<Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoKeyRocks, Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoRocks> _Registers;
    private final Zeze.Raft.RocksRaft.CollMap2<String, Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfoRocks> _Subscribes;

    public long getSessionId() {
        if (!isManaged())
            return _SessionId;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _SessionId;
        var log = txn.getLog(objectId() + 1);
        if (log == null)
            return _SessionId;
        return ((Zeze.Raft.RocksRaft.Log1.LogLong)log).value;
    }

    public void setSessionId(long value) {
        if (!isManaged()) {
            _SessionId = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogLong(this, 1, value));
    }

    public int getOfflineRegisterServerId() {
        if (!isManaged())
            return _OfflineRegisterServerId;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _OfflineRegisterServerId;
        var log = txn.getLog(objectId() + 2);
        if (log == null)
            return _OfflineRegisterServerId;
        return ((Zeze.Raft.RocksRaft.Log1.LogInt)log).value;
    }

    public void setOfflineRegisterServerId(int value) {
        if (!isManaged()) {
            _OfflineRegisterServerId = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogInt(this, 2, value));
    }

    public Zeze.Raft.RocksRaft.CollMap2<String, Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotifyRocks> getOfflineRegisterNotifies() {
        return _OfflineRegisterNotifies;
    }

    public Zeze.Raft.RocksRaft.CollMap2<Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoKeyRocks, Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoRocks> getRegisters() {
        return _Registers;
    }

    public Zeze.Raft.RocksRaft.CollMap2<String, Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfoRocks> getSubscribes() {
        return _Subscribes;
    }

    public BSession() {
        _OfflineRegisterNotifies = new Zeze.Raft.RocksRaft.CollMap2<>(String.class, Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotifyRocks.class);
        _OfflineRegisterNotifies.variableId(3);
        _Registers = new Zeze.Raft.RocksRaft.CollMap2<>(Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoKeyRocks.class, Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoRocks.class);
        _Registers.variableId(4);
        _Subscribes = new Zeze.Raft.RocksRaft.CollMap2<>(String.class, Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfoRocks.class);
        _Subscribes.variableId(5);
    }

    public BSession(long _SessionId_, int _OfflineRegisterServerId_) {
        _SessionId = _SessionId_;
        _OfflineRegisterServerId = _OfflineRegisterServerId_;
        _OfflineRegisterNotifies = new Zeze.Raft.RocksRaft.CollMap2<>(String.class, Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotifyRocks.class);
        _OfflineRegisterNotifies.variableId(3);
        _Registers = new Zeze.Raft.RocksRaft.CollMap2<>(Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoKeyRocks.class, Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoRocks.class);
        _Registers.variableId(4);
        _Subscribes = new Zeze.Raft.RocksRaft.CollMap2<>(String.class, Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfoRocks.class);
        _Subscribes.variableId(5);
    }

    public void assign(BSession other) {
        setSessionId(other.getSessionId());
        setOfflineRegisterServerId(other.getOfflineRegisterServerId());
        _OfflineRegisterNotifies.clear();
        for (var e : other._OfflineRegisterNotifies.entrySet())
            _OfflineRegisterNotifies.put(e.getKey(), e.getValue());
        _Registers.clear();
        for (var e : other._Registers.entrySet())
            _Registers.put(e.getKey(), e.getValue());
        _Subscribes.clear();
        for (var e : other._Subscribes.entrySet())
            _Subscribes.put(e.getKey(), e.getValue());
    }

    public BSession copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSession copy() {
        var copy = new BSession();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSession a, BSession b) {
        BSession save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ServiceManagerWithRaft.BSession: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("SessionId").append('=').append(getSessionId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("OfflineRegisterServerId").append('=').append(getOfflineRegisterServerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("OfflineRegisterNotifies").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getOfflineRegisterNotifies().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().buildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Registers").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getRegisters().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(System.lineSeparator());
            _kv_.getKey().buildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().buildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Subscribes").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getSubscribes().entrySet()) {
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
            long _x_ = getSessionId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getOfflineRegisterServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getOfflineRegisterNotifies();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.MAP);
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
        {
            var _x_ = getRegisters();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BEAN, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _e_.getKey().encode(_o_);
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = getSubscribes();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.MAP);
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
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _SessionId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _OfflineRegisterServerId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = getOfflineRegisterNotifies();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotifyRocks(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = getRegisters();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBean(new Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoKeyRocks(), _s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoRocks(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            var _x_ = getSubscribes();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfoRocks(), _t_);
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
    protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.RootInfo root) {
        _OfflineRegisterNotifies.initRootInfo(root, this);
        _Registers.initRootInfo(root, this);
        _Subscribes.initRootInfo(root, this);
    }

    @Override
    public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 1: _SessionId = ((Zeze.Raft.RocksRaft.Log1.LogLong)vlog).value; break;
            case 2: _OfflineRegisterServerId = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
            case 3: _OfflineRegisterNotifies.leaderApplyNoRecursive(vlog); break;
            case 4: _Registers.leaderApplyNoRecursive(vlog); break;
            case 5: _Subscribes.leaderApplyNoRecursive(vlog); break;
        }
    }

    @Override
    public void followerApply(Zeze.Raft.RocksRaft.Log log) {
        var vars = ((Zeze.Raft.RocksRaft.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _SessionId = ((Zeze.Raft.RocksRaft.Log1.LogLong)vlog).value; break;
                case 2: _OfflineRegisterServerId = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
                case 3: _OfflineRegisterNotifies.followerApply(vlog); break;
                case 4: _Registers.followerApply(vlog); break;
                case 5: _Subscribes.followerApply(vlog); break;
            }
        }
    }
}
