// auto-generated rocks @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BSubscribeInfoRocks extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = 3024932175735380785L;

    private String _ServiceName;
    private long _Version;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

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

    public long getVersion() {
        if (!isManaged())
            return _Version;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _Version;
        var log = txn.getLog(objectId() + 2);
        if (log == null)
            return _Version;
        return ((Zeze.Raft.RocksRaft.Log1.LogLong)log).value;
    }

    public void setVersion(long value) {
        if (!isManaged()) {
            _Version = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogLong(this, 2, value));
    }

    public BSubscribeInfoRocks() {
        _ServiceName = "";
    }

    public BSubscribeInfoRocks(String _ServiceName_, long _Version_) {
        if (_ServiceName_ == null)
            throw new IllegalArgumentException();
        _ServiceName = _ServiceName_;
        _Version = _Version_;
    }

    public void assign(BSubscribeInfoRocks other) {
        setServiceName(other.getServiceName());
        setVersion(other.getVersion());
    }

    public BSubscribeInfoRocks copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSubscribeInfoRocks copy() {
        var copy = new BSubscribeInfoRocks();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSubscribeInfoRocks a, BSubscribeInfoRocks b) {
        BSubscribeInfoRocks save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfoRocks: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName").append('=').append(getServiceName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Version").append('=').append(getVersion()).append(System.lineSeparator());
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
            long _x_ = getVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            _Version = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.RootInfo root) {
    }

    @Override
    public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 1: _ServiceName = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
            case 2: _Version = ((Zeze.Raft.RocksRaft.Log1.LogLong)vlog).value; break;
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
                case 2: _Version = ((Zeze.Raft.RocksRaft.Log1.LogLong)vlog).value; break;
            }
        }
    }
}
