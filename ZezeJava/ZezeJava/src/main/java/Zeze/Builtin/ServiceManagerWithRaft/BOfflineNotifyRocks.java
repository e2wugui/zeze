// auto-generated rocks @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// session
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BOfflineNotifyRocks extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = 5623921323744247125L;

    private int _ServerId;
    private String _NotifyId;
    private long _NotifySerialId;
    private Zeze.Net.Binary _NotifyContext;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _ServerId;
        var log = txn.getLog(objectId() + 1);
        if (log == null)
            return _ServerId;
        return ((Zeze.Raft.RocksRaft.Log1.LogInt)log).value;
    }

    public void setServerId(int value) {
        if (!isManaged()) {
            _ServerId = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogInt(this, 1, value));
    }

    public String getNotifyId() {
        if (!isManaged())
            return _NotifyId;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _NotifyId;
        var log = txn.getLog(objectId() + 2);
        if (log == null)
            return _NotifyId;
        return ((Zeze.Raft.RocksRaft.Log1.LogString)log).value;
    }

    public void setNotifyId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _NotifyId = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogString(this, 2, value));
    }

    public long getNotifySerialId() {
        if (!isManaged())
            return _NotifySerialId;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _NotifySerialId;
        var log = txn.getLog(objectId() + 3);
        if (log == null)
            return _NotifySerialId;
        return ((Zeze.Raft.RocksRaft.Log1.LogLong)log).value;
    }

    public void setNotifySerialId(long value) {
        if (!isManaged()) {
            _NotifySerialId = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogLong(this, 3, value));
    }

    public Zeze.Net.Binary getNotifyContext() {
        if (!isManaged())
            return _NotifyContext;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _NotifyContext;
        var log = txn.getLog(objectId() + 4);
        if (log == null)
            return _NotifyContext;
        return ((Zeze.Raft.RocksRaft.Log1.LogBinary)log).value;
    }

    public void setNotifyContext(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _NotifyContext = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogBinary(this, 4, value));
    }

    public BOfflineNotifyRocks() {
        _NotifyId = "";
        _NotifyContext = Zeze.Net.Binary.Empty;
    }

    public BOfflineNotifyRocks(int _ServerId_, String _NotifyId_, long _NotifySerialId_, Zeze.Net.Binary _NotifyContext_) {
        _ServerId = _ServerId_;
        if (_NotifyId_ == null)
            throw new IllegalArgumentException();
        _NotifyId = _NotifyId_;
        _NotifySerialId = _NotifySerialId_;
        if (_NotifyContext_ == null)
            throw new IllegalArgumentException();
        _NotifyContext = _NotifyContext_;
    }

    public void assign(BOfflineNotifyRocks other) {
        setServerId(other.getServerId());
        setNotifyId(other.getNotifyId());
        setNotifySerialId(other.getNotifySerialId());
        setNotifyContext(other.getNotifyContext());
    }

    public BOfflineNotifyRocks copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOfflineNotifyRocks copy() {
        var copy = new BOfflineNotifyRocks();
        copy.assign(this);
        return copy;
    }

    public static void swap(BOfflineNotifyRocks a, BOfflineNotifyRocks b) {
        BOfflineNotifyRocks save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotifyRocks: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId").append('=').append(getServerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NotifyId").append('=').append(getNotifyId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NotifySerialId").append('=').append(getNotifySerialId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NotifyContext").append('=').append(getNotifyContext()).append(System.lineSeparator());
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
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getNotifyId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getNotifySerialId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getNotifyContext();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ServerId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _NotifyId = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _NotifySerialId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _NotifyContext = _o_.ReadBinary(_t_);
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
            case 1: _ServerId = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
            case 2: _NotifyId = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
            case 3: _NotifySerialId = ((Zeze.Raft.RocksRaft.Log1.LogLong)vlog).value; break;
            case 4: _NotifyContext = ((Zeze.Raft.RocksRaft.Log1.LogBinary)vlog).value; break;
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
                case 1: _ServerId = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
                case 2: _NotifyId = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
                case 3: _NotifySerialId = ((Zeze.Raft.RocksRaft.Log1.LogLong)vlog).value; break;
                case 4: _NotifyContext = ((Zeze.Raft.RocksRaft.Log1.LogBinary)vlog).value; break;
            }
        }
    }
}
