// auto-generated rocks @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// table
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BCacheState extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = 2494905368101749954L;

    private int _AcquireStatePending;
    private int _Modify; // ServerId, default MUST BE -1.
    private final Zeze.Raft.RocksRaft.CollSet1<Integer> _Share;

    public int getAcquireStatePending() {
        return _AcquireStatePending;
    }

    public void setAcquireStatePending(int value) {
        _AcquireStatePending = value;
    }

    public int getModify() {
        if (!isManaged())
            return _Modify;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _Modify;
        var log = txn.getLog(objectId() + 2);
        if (log == null)
            return _Modify;
        return ((Zeze.Raft.RocksRaft.Log1.LogInt)log).value;
    }

    public void setModify(int value) {
        if (!isManaged()) {
            _Modify = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogInt(this, 2, value));
    }

    public Zeze.Raft.RocksRaft.CollSet1<Integer> getShare() {
        return _Share;
    }

    public BCacheState() {
        _Modify = -1;
        _Share = new Zeze.Raft.RocksRaft.CollSet1<>(Integer.class);
        _Share.variableId(3);
    }

    public BCacheState(int _AcquireStatePending_, int _Modify_) {
        _AcquireStatePending = _AcquireStatePending_;
        _Modify = _Modify_;
        _Share = new Zeze.Raft.RocksRaft.CollSet1<>(Integer.class);
        _Share.variableId(3);
    }

    public void assign(BCacheState other) {
        setAcquireStatePending(other.getAcquireStatePending());
        setModify(other.getModify());
        _Share.clear();
        for (var e : other._Share)
            _Share.add(e);
    }

    public BCacheState copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCacheState copy() {
        var copy = new BCacheState();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCacheState a, BCacheState b) {
        BCacheState save = a.copy();
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
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.GlobalCacheManagerWithRaft.BCacheState: {\n");
        _s_.append(_i1_).append("AcquireStatePending=").append(getAcquireStatePending()).append(",\n");
        _s_.append(_i1_).append("Modify=").append(getModify()).append(",\n");
        _s_.append(_i1_).append("Share=[\n");
        for (var _item_ : getShare()) {
            _s_.append(_i2_).append("Item=").append(_item_).append(",\n");
        }
        _s_.append(_i1_).append("]\n");
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
            int _x_ = getModify();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getShare();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
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
        while (_t_ != 0 && _i_ < 2) {
            _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Modify = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            _Modify = 0;
        if (_i_ == 3) {
            var _x_ = getShare();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
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
    protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.RootInfo root) {
        _Share.initRootInfo(root, this);
    }

    @Override
    public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 2: _Modify = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
            case 3: _Share.leaderApplyNoRecursive(vlog); break;
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
                case 2: _Modify = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
                case 3: _Share.followerApply(vlog); break;
            }
        }
    }
}
