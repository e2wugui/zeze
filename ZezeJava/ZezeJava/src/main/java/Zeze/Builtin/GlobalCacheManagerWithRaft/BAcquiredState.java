// auto-generated rocks @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BAcquiredState extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = -1043402609085397620L;

    private int _State;

    public int getState() {
        if (!isManaged())
            return _State;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _State;
        var log = txn.getLog(objectId() + 1);
        if (log == null)
            return _State;
        return ((Zeze.Raft.RocksRaft.Log1.LogInt)log).value;
    }

    public void setState(int value) {
        if (!isManaged()) {
            _State = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogInt(this, 1, value));
    }

    public BAcquiredState() {
    }

    public BAcquiredState(int _State_) {
        _State = _State_;
    }

    public void assign(BAcquiredState other) {
        setState(other.getState());
    }

    public BAcquiredState copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAcquiredState copy() {
        var copy = new BAcquiredState();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAcquiredState a, BAcquiredState b) {
        BAcquiredState save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.GlobalCacheManagerWithRaft.BAcquiredState: {\n");
        _s_.append(_i1_).append("State=").append(getState()).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
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
    public void decode(IByteBuffer _o_) {
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
            case 1: _State = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
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
                case 1: _State = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
            }
        }
    }
}
