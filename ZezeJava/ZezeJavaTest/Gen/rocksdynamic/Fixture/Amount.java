// auto-generated rocks @formatter:off
package rocksdynamic.Fixture;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "SuspiciousNameCombination"})
public final class Amount extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = -5493259402015202929L;

    private int _Number;
    private final Zeze.Raft.RocksRaft.CollList1<Integer> _Entries;

    public int getNumber() {
        if (!isManaged())
            return _Number;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _Number;
        var log = txn.getLog(objectId() + 1);
        if (log == null)
            return _Number;
        return ((Zeze.Raft.RocksRaft.Log1.LogInt)log).value;
    }

    public void setNumber(int value) {
        if (!isManaged()) {
            _Number = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogInt(this, 1, value));
    }

    public Zeze.Raft.RocksRaft.CollList1<Integer> getEntries() {
        return _Entries;
    }

    public Amount() {
        _Entries = new Zeze.Raft.RocksRaft.CollList1<>(Integer.class);
        _Entries.variableId(2);
    }

    public Amount(int _Number_) {
        _Number = _Number_;
        _Entries = new Zeze.Raft.RocksRaft.CollList1<>(Integer.class);
        _Entries.variableId(2);
    }

    public void assign(Amount other) {
        if (other == this)
            return;
        setNumber(other.getNumber());
        _Entries.clear();
        for (var e : other._Entries)
            _Entries.add(e);
    }

    public Amount copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public Amount copy() {
        var copy = new Amount();
        copy.assign(this);
        return copy;
    }

    public static void swap(Amount a, Amount b) {
        Amount save = a.copy();
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
        _s_.append("rocksdynamic.Fixture.Amount: {\n");
        _s_.append(_i1_).append("Number=").append(getNumber()).append(",\n");
        _s_.append(_i1_).append("Entries=[\n");
        for (var _item_ : getEntries()) {
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
            int _x_ = getNumber();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getEntries();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
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
        if (_i_ == 1) {
            _Number = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Number = 0;
        }
        if (_i_ == 2) {
            var _x_ = getEntries();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Entries.clear();
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.@Nullable RootInfo root) {
        _Entries.initRootInfo(root, this);
    }

    @Override
    public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 1: _Number = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
            case 2: _Entries.leaderApplyNoRecursive(vlog); break;
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
                case 1: _Number = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
                case 2: _Entries.followerApply(vlog); break;
            }
        }
    }
}
