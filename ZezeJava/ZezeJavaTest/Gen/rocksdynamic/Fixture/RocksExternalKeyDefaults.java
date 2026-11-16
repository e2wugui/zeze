// auto-generated rocks @formatter:off
package rocksdynamic.Fixture;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "SuspiciousNameCombination"})
public final class RocksExternalKeyDefaults extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = 1315754030835730783L;

    private Zeze.Util.Id128 _KeyValue;

    @SuppressWarnings("unchecked")
    public Zeze.Util.Id128 getKeyValue() {
        if (!isManaged())
            return _KeyValue;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _KeyValue;
        var log = txn.getLog(objectId() + 1);
        if (null == log)
            return _KeyValue;
        return ((Zeze.Raft.RocksRaft.Log1.LogBeanKey<Zeze.Util.Id128>)log).value;
    }

    public void setKeyValue(Zeze.Util.Id128 value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _KeyValue = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogBeanKey<>(Zeze.Util.Id128.class, this, 1, value));
    }

    public RocksExternalKeyDefaults() {
        _KeyValue = new Zeze.Util.Id128(1L, 2L);
    }

    public RocksExternalKeyDefaults(Zeze.Util.Id128 _KeyValue_) {
        if (_KeyValue_ == null)
            throw new IllegalArgumentException();
        _KeyValue = _KeyValue_;
    }

    public void assign(RocksExternalKeyDefaults other) {
        if (other == this)
            return;
        setKeyValue(other.getKeyValue());
    }

    public RocksExternalKeyDefaults copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public RocksExternalKeyDefaults copy() {
        var copy = new RocksExternalKeyDefaults();
        copy.assign(this);
        return copy;
    }

    public static void swap(RocksExternalKeyDefaults a, RocksExternalKeyDefaults b) {
        RocksExternalKeyDefaults save = a.copy();
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
        _s_.append("rocksdynamic.Fixture.RocksExternalKeyDefaults: {\n");
        _s_.append(_i1_).append("KeyValue=");
        getKeyValue().buildString(_s_, _l_ + 8);
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
            getKeyValue().encode(_o_);
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
            _KeyValue = _o_.ReadBeanKey(_t_, Zeze.Util.Id128.class, Zeze.Util.Id128::readFrom, () -> new Zeze.Util.Id128(1L, 2L));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _KeyValue = ByteBuffer.readEmptyBean(Zeze.Util.Id128::readFrom);
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.@Nullable RootInfo root) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 1: _KeyValue = ((Zeze.Raft.RocksRaft.Log1.LogBeanKey<Zeze.Util.Id128>)vlog).value; break;
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
                case 1: _KeyValue = ((Zeze.Raft.RocksRaft.Log1.LogBeanKey<Zeze.Util.Id128>)vlog).value; break;
            }
        }
    }
}
