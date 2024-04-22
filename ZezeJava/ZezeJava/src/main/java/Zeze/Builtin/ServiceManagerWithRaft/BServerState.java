// auto-generated rocks @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BServerState extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = 8043107371087898459L;

    private String _ServiceName;
    private final Zeze.Raft.RocksRaft.CollMap2<Long, Zeze.Builtin.ServiceManagerWithRaft.BServiceInfosVersionRocks> _ServiceInfosVersion;
    private final Zeze.Raft.RocksRaft.CollMap2<String, Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfoRocks> _Simple;
    private long _SerialId;

    public String getServiceName() {
        if (!isManaged())
            return _ServiceName;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _ServiceName;
        var log = txn.getLog(objectId() + 1);
        if (log == null)
            return _ServiceName;
        return ((Zeze.Raft.RocksRaft.Log1.LogString)log).value;
    }

    public void setServiceName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceName = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogString(this, 1, value));
    }

    public Zeze.Raft.RocksRaft.CollMap2<Long, Zeze.Builtin.ServiceManagerWithRaft.BServiceInfosVersionRocks> getServiceInfosVersion() {
        return _ServiceInfosVersion;
    }

    public Zeze.Raft.RocksRaft.CollMap2<String, Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfoRocks> getSimple() {
        return _Simple;
    }

    public long getSerialId() {
        if (!isManaged())
            return _SerialId;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _SerialId;
        var log = txn.getLog(objectId() + 5);
        if (log == null)
            return _SerialId;
        return ((Zeze.Raft.RocksRaft.Log1.LogLong)log).value;
    }

    public void setSerialId(long value) {
        if (!isManaged()) {
            _SerialId = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogLong(this, 5, value));
    }

    public BServerState() {
        _ServiceName = "";
        _ServiceInfosVersion = new Zeze.Raft.RocksRaft.CollMap2<>(Long.class, Zeze.Builtin.ServiceManagerWithRaft.BServiceInfosVersionRocks.class);
        _ServiceInfosVersion.variableId(2);
        _Simple = new Zeze.Raft.RocksRaft.CollMap2<>(String.class, Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfoRocks.class);
        _Simple.variableId(3);
    }

    public BServerState(String _ServiceName_, long _SerialId_) {
        if (_ServiceName_ == null)
            throw new IllegalArgumentException();
        _ServiceName = _ServiceName_;
        _ServiceInfosVersion = new Zeze.Raft.RocksRaft.CollMap2<>(Long.class, Zeze.Builtin.ServiceManagerWithRaft.BServiceInfosVersionRocks.class);
        _ServiceInfosVersion.variableId(2);
        _Simple = new Zeze.Raft.RocksRaft.CollMap2<>(String.class, Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfoRocks.class);
        _Simple.variableId(3);
        _SerialId = _SerialId_;
    }

    public void assign(BServerState other) {
        setServiceName(other.getServiceName());
        _ServiceInfosVersion.clear();
        for (var e : other._ServiceInfosVersion.entrySet())
            _ServiceInfosVersion.put(e.getKey(), e.getValue());
        _Simple.clear();
        for (var e : other._Simple.entrySet())
            _Simple.put(e.getKey(), e.getValue());
        setSerialId(other.getSerialId());
    }

    public BServerState copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BServerState copy() {
        var copy = new BServerState();
        copy.assign(this);
        return copy;
    }

    public static void swap(BServerState a, BServerState b) {
        BServerState save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ServiceManagerWithRaft.BServerState: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName").append('=').append(getServiceName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceInfosVersion").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getServiceInfosVersion().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().buildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Simple").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getSimple().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().buildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SerialId").append('=').append(getSerialId()).append(System.lineSeparator());
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
            String _x_ = getServiceName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getServiceInfosVersion();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = getSimple();
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
            long _x_ = getSerialId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ServiceName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = getServiceInfosVersion();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.ServiceManagerWithRaft.BServiceInfosVersionRocks(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = getSimple();
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
        while (_t_ != 0 && _i_ < 5) {
            _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _SerialId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.RootInfo root) {
        _ServiceInfosVersion.initRootInfo(root, this);
        _Simple.initRootInfo(root, this);
    }

    @Override
    public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 1: _ServiceName = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
            case 2: _ServiceInfosVersion.leaderApplyNoRecursive(vlog); break;
            case 3: _Simple.leaderApplyNoRecursive(vlog); break;
            case 5: _SerialId = ((Zeze.Raft.RocksRaft.Log1.LogLong)vlog).value; break;
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
                case 1: _ServiceName = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
                case 2: _ServiceInfosVersion.followerApply(vlog); break;
                case 3: _Simple.followerApply(vlog); break;
                case 5: _SerialId = ((Zeze.Raft.RocksRaft.Log1.LogLong)vlog).value; break;
            }
        }
    }
}
