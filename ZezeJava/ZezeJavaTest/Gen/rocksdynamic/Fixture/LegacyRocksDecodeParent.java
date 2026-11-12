// auto-generated rocks @formatter:off
package rocksdynamic.Fixture;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "SuspiciousNameCombination"})
public final class LegacyRocksDecodeParent extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = -8313851343223027172L;

    private final rocksdynamic.Fixture.LegacyRocksDecodeChild _Child;
    private rocksdynamic.Fixture.LegacyDecodeKey _KeyValue;

    public rocksdynamic.Fixture.LegacyRocksDecodeChild getChild() {
        return _Child;
    }

    @SuppressWarnings("unchecked")
    public rocksdynamic.Fixture.LegacyDecodeKey getKeyValue() {
        if (!isManaged())
            return _KeyValue;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _KeyValue;
        var log = txn.getLog(objectId() + 2);
        if (null == log)
            return _KeyValue;
        return ((Zeze.Raft.RocksRaft.Log1.LogBeanKey<rocksdynamic.Fixture.LegacyDecodeKey>)log).value;
    }

    public void setKeyValue(rocksdynamic.Fixture.LegacyDecodeKey value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _KeyValue = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogBeanKey<>(rocksdynamic.Fixture.LegacyDecodeKey.class, this, 2, value));
    }

    public LegacyRocksDecodeParent() {
        _Child = new rocksdynamic.Fixture.LegacyRocksDecodeChild();
        _Child.variableId(1);
        _KeyValue = new rocksdynamic.Fixture.LegacyDecodeKey();
    }

    public LegacyRocksDecodeParent(rocksdynamic.Fixture.LegacyDecodeKey _KeyValue_) {
        _Child = new rocksdynamic.Fixture.LegacyRocksDecodeChild();
        _Child.variableId(1);
        if (_KeyValue_ == null)
            throw new IllegalArgumentException();
        _KeyValue = _KeyValue_;
    }

    public void assign(LegacyRocksDecodeParent other) {
        if (other == this)
            return;
        _Child.assign(other._Child);
        setKeyValue(other.getKeyValue());
    }

    public LegacyRocksDecodeParent copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public LegacyRocksDecodeParent copy() {
        var copy = new LegacyRocksDecodeParent();
        copy.assign(this);
        return copy;
    }

    public static void swap(LegacyRocksDecodeParent a, LegacyRocksDecodeParent b) {
        LegacyRocksDecodeParent save = a.copy();
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
        _s_.append("rocksdynamic.Fixture.LegacyRocksDecodeParent: {\n");
        _s_.append(_i1_).append("Child=");
        getChild().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
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
            getChild().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
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
            _o_.ReadBean(_Child, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            ByteBuffer.decodeEmptyBean(_Child);
        }
        if (_i_ == 2) {
            _KeyValue = _o_.ReadBean(new rocksdynamic.Fixture.LegacyDecodeKey(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _KeyValue = ByteBuffer.decodeEmptyBean(new rocksdynamic.Fixture.LegacyDecodeKey());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.@Nullable RootInfo root) {
        _Child.initRootInfo(root, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 2: _KeyValue = ((Zeze.Raft.RocksRaft.Log1.LogBeanKey<rocksdynamic.Fixture.LegacyDecodeKey>)vlog).value; break;
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
                case 1: _Child.followerApply(vlog); break;
                case 2: _KeyValue = ((Zeze.Raft.RocksRaft.Log1.LogBeanKey<rocksdynamic.Fixture.LegacyDecodeKey>)vlog).value; break;
            }
        }
    }
}
