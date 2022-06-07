// auto-generated rocks @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class CacheState extends Zeze.Raft.RocksRaft.Bean {
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
        var log = txn.GetLog(getObjectId() + 2);
        if (log == null)
            return _Modify;
        return ((Zeze.Raft.RocksRaft.Log1.LogInt)log).Value;
    }

    public void setModify(int value) {
        if (!isManaged()) {
            _Modify = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        assert txn != null;
        txn.PutLog(new Zeze.Raft.RocksRaft.Log1.LogInt(this, 2, value));
    }

    public Zeze.Raft.RocksRaft.CollSet1<Integer> getShare() {
        return _Share;
    }

    public CacheState() {
         this(0);
    }

    public CacheState(int _varId_) {
        super(_varId_);
        _Modify = -1;
        _Share = new Zeze.Raft.RocksRaft.CollSet1<>(Integer.class);
        _Share.VariableId = 3;
    }

    public void Assign(CacheState other) {
        setAcquireStatePending(other.getAcquireStatePending());
        setModify(other.getModify());
        getShare().clear();
        for (var e : other.getShare())
            getShare().add(e);
    }

    public CacheState CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public CacheState Copy() {
        var copy = new CacheState();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(CacheState a, CacheState b) {
        CacheState save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Raft.RocksRaft.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 1756306680643652334L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.GlobalCacheManagerWithRaft.CacheState: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("AcquireStatePending").append('=').append(getAcquireStatePending()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Modify").append('=').append(getModify()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Share").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getShare()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(_item_).append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @Override
    public void Encode(ByteBuffer _o_) {
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
                for (var _v_ : _x_)
                    _o_.WriteLong(_v_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        while (_t_ != 0 && _i_ < 2) {
            _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Modify = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = getShare();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Raft.RocksRaft.Record.RootInfo root) {
        _Share.InitRootInfo(root, this);
    }

    @Override
    public void LeaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 2: _Modify = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).Value; break;
            case 3: _Share.LeaderApplyNoRecursive(vlog); break;
        }
    }

    @Override
    public void FollowerApply(Zeze.Raft.RocksRaft.Log log) {
        var vars = ((Zeze.Raft.RocksRaft.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 2: _Modify = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).Value; break;
                case 3: _Share.FollowerApply(vlog); break;
            }
        }
    }
}
