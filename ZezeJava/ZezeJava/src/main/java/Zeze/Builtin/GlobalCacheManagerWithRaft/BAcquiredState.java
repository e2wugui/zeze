// auto-generated rocks @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BAcquiredState extends Zeze.Raft.RocksRaft.Bean {
    private int _State;

    public int getState() {
        if (!isManaged())
            return _State;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _State;
        var log = txn.GetLog(objectId() + 1);
        if (log == null)
            return _State;
        return ((Zeze.Raft.RocksRaft.Log1.LogInt)log).Value;
    }

    public void setState(int value) {
        if (!isManaged()) {
            _State = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.PutLog(new Zeze.Raft.RocksRaft.Log1.LogInt(this, 1, value));
    }

    public BAcquiredState() {
    }

    public BAcquiredState(int _State_) {
        _State = _State_;
    }

    public void assign(BAcquiredState other) {
        setState(other.getState());
    }

    @Deprecated
    public void Assign(BAcquiredState other) {
        assign(other);
    }

    public BAcquiredState copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BAcquiredState copy() {
        var copy = new BAcquiredState();
        copy.Assign(this);
        return copy;
    }

    @Deprecated
    public BAcquiredState Copy() {
        return copy();
    }

    public static void swap(BAcquiredState a, BAcquiredState b) {
        BAcquiredState save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BAcquiredState copyBean() {
        return Copy();
    }

    public static final long TYPEID = -1043402609085397620L;

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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.GlobalCacheManagerWithRaft.BAcquiredState: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("State").append('=').append(getState()).append(System.lineSeparator());
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
            int _x_ = getState();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _State = _o_.ReadInt(_t_);
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
            case 1: _State = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).Value; break;
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
                case 1: _State = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).Value; break;
            }
        }
    }
}
