// auto-generated rocks @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BId128 extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = -7172533889896232547L;

    private Zeze.Util.Id128 _Current;

    @SuppressWarnings("unchecked")
    public Zeze.Util.Id128 getCurrent() {
        if (!isManaged())
            return _Current;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _Current;
        var log = txn.getLog(objectId() + 1);
        if (null == log)
            return _Current;
        return ((Zeze.Raft.RocksRaft.Log1.LogBeanKey<Zeze.Util.Id128>)log).value;
    }

    public void setCurrent(Zeze.Util.Id128 value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Current = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogBeanKey<>(Zeze.Util.Id128.class, this, 1, value));
    }

    public BId128() {
        _Current = new Zeze.Util.Id128();
    }

    public BId128(Zeze.Util.Id128 _Current_) {
        if (_Current_ == null)
            throw new IllegalArgumentException();
        _Current = _Current_;
    }

    public void assign(BId128 other) {
        setCurrent(other.getCurrent());
    }

    public BId128 copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BId128 copy() {
        var copy = new BId128();
        copy.assign(this);
        return copy;
    }

    public static void swap(BId128 a, BId128 b) {
        BId128 save = a.copy();
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
        _s_.append("Zeze.Builtin.ServiceManagerWithRaft.BId128: {\n");
        _s_.append(_i1_).append("Current=");
        getCurrent().buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getCurrent().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadBean(_Current, _t_);
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

    @SuppressWarnings("unchecked")
    @Override
    public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 1: _Current = ((Zeze.Raft.RocksRaft.Log1.LogBeanKey<Zeze.Util.Id128>)vlog).value; break;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Raft.RocksRaft.Log log) {
        var vars = ((Zeze.Raft.RocksRaft.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _Current = ((Zeze.Raft.RocksRaft.Log1.LogBeanKey<Zeze.Util.Id128>)vlog).value; break;
            }
        }
    }
}
