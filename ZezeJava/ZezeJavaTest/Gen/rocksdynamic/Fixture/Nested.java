// auto-generated rocks @formatter:off
package rocksdynamic.Fixture;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "SuspiciousNameCombination"})
public final class Nested extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = 6210664097565425312L;

    private static final Zeze.Raft.RocksRaft.DynamicField dynamicField_1 =
            new Zeze.Raft.RocksRaft.DynamicField(TYPEID, 1,
                    Nested::getSpecialTypeId_1, Nested::createDynamicValue_1);

    private static long getSpecialTypeId_1(Zeze.Raft.RocksRaft.Bean value) {
        var typeId = value.typeId();
        if (typeId == -8039584605842835988L) return 7L;
        throw new IllegalArgumentException("unknown type for rocksdynamic.Fixture.Nested.Payload: " + typeId);
    }

    private static Zeze.Raft.RocksRaft.@Nullable Bean createDynamicValue_1(long typeId) {
        if (typeId == 7L) return new rocksdynamic.Fixture.Quantity();
        return null;
    }

    public static void registerDynamicFields() {
        dynamicField_1.registerValueLog();
    }

    private final Zeze.Raft.RocksRaft.DynamicBean _Payload;
    private final Zeze.Raft.RocksRaft.CollSortedMap1<Integer, Long> _SortedNumbers;
    private int _ScratchNumber;

    public Zeze.Raft.RocksRaft.DynamicBean getPayload() {
        return _Payload;
    }

    public rocksdynamic.Fixture.@Nullable Quantity getPayload_rocksdynamic_Fixture_Quantity() {
        return (rocksdynamic.Fixture.Quantity)getPayload().getBean();
    }
    public void setPayload(rocksdynamic.Fixture.Quantity value) {
        getPayload().setBean(value);
    }

    public Zeze.Raft.RocksRaft.CollSortedMap1<Integer, Long> getSortedNumbers() {
        return _SortedNumbers;
    }

    public int getScratchNumber() {
        return _ScratchNumber;
    }

    public void setScratchNumber(int value) {
        _ScratchNumber = value;
    }

    public Nested() {
        _Payload = dynamicField_1.newValue();
        _SortedNumbers = new Zeze.Raft.RocksRaft.CollSortedMap1<>(Integer.class, Long.class);
        _SortedNumbers.variableId(2);
    }

    public Nested(int _ScratchNumber_) {
        _Payload = dynamicField_1.newValue();
        _SortedNumbers = new Zeze.Raft.RocksRaft.CollSortedMap1<>(Integer.class, Long.class);
        _SortedNumbers.variableId(2);
        _ScratchNumber = _ScratchNumber_;
    }

    public void assign(Nested other) {
        if (other == this)
            return;
        _Payload.assign(other.getPayload());
        _SortedNumbers.clear();
        for (var e : other._SortedNumbers.entrySet())
            _SortedNumbers.put(e.getKey(), e.getValue());
        setScratchNumber(other.getScratchNumber());
    }

    public Nested copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public Nested copy() {
        var copy = new Nested();
        copy.assign(this);
        return copy;
    }

    public static void swap(Nested a, Nested b) {
        Nested save = a.copy();
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
        _s_.append("rocksdynamic.Fixture.Nested: {\n");
        _s_.append(_i1_).append("Payload=");
        getPayload().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("SortedNumbers=[\n");
        for (var _kv_ : getSortedNumbers().entrySet()) {
            _s_.append(_i2_).append("Key=").append(_kv_.getKey()).append(",\n");
            _s_.append(_i2_).append("Value=").append(_kv_.getValue()).append(",\n");
        }
        _s_.append(_i1_).append("],\n");
        _s_.append(_i1_).append("ScratchNumber=").append(getScratchNumber()).append('\n');
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
            var _x_ = getPayload();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            var _x_ = getSortedNumbers();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
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
            _Payload.decodeTagged(_o_, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Payload.clear();
        }
        if (_i_ == 2) {
            var _x_ = getSortedNumbers();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadLong(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _SortedNumbers.clear();
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    public void restoreOmittedFields(Nested _defaults_) {
        _Payload.assign(_defaults_._Payload);
        _SortedNumbers.clear();
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.@Nullable RootInfo root) {
        _Payload.initRootInfo(root, this);
        _SortedNumbers.initRootInfo(root, this);
    }

    @Override
    public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 2: _SortedNumbers.leaderApplyNoRecursive(vlog); break;
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
                case 1: _Payload.followerApply(vlog); break;
                case 2: _SortedNumbers.followerApply(vlog); break;
            }
        }
    }
}
