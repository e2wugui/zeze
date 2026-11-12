// auto-generated rocks @formatter:off
package rocksdynamic.Fixture;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "SuspiciousNameCombination"})
public final class Quantity extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = -8039584605842835988L;

    private long _Number;

    private transient @Nullable Object __zeze_map_key__;

    @Override
    public @Nullable Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    public long getNumber() {
        if (!isManaged())
            return _Number;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _Number;
        var log = txn.getLog(objectId() + 1);
        if (log == null)
            return _Number;
        return ((Zeze.Raft.RocksRaft.Log1.LogLong)log).value;
    }

    public void setNumber(long value) {
        if (!isManaged()) {
            _Number = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogLong(this, 1, value));
    }

    public Quantity() {
    }

    public Quantity(long _Number_) {
        _Number = _Number_;
    }

    public void assign(Quantity other) {
        if (other == this)
            return;
        setNumber(other.getNumber());
    }

    public Quantity copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public Quantity copy() {
        var copy = new Quantity();
        copy.assign(this);
        return copy;
    }

    public static void swap(Quantity a, Quantity b) {
        Quantity save = a.copy();
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
        _s_.append("rocksdynamic.Fixture.Quantity: {\n");
        _s_.append(_i1_).append("Number=").append(getNumber()).append('\n');
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
            long _x_ = getNumber();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
            _Number = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Number = 0;
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.@Nullable RootInfo root) {
    }

    @Override
    public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 1: _Number = ((Zeze.Raft.RocksRaft.Log1.LogLong)vlog).value; break;
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
                case 1: _Number = ((Zeze.Raft.RocksRaft.Log1.LogLong)vlog).value; break;
            }
        }
    }
}
