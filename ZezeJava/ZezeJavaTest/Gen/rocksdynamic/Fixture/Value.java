// auto-generated rocks @formatter:off
package rocksdynamic.Fixture;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "SuspiciousNameCombination"})
public final class Value extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = -2264750572828986776L;

    private static final Zeze.Raft.RocksRaft.DynamicField dynamicField_1 =
            new Zeze.Raft.RocksRaft.DynamicField(TYPEID, 1,
                    Value::getSpecialTypeId_1, Value::createDynamicValue_1);

    private static long getSpecialTypeId_1(Zeze.Raft.RocksRaft.Bean value) {
        var typeId = value.typeId();
        if (typeId == -5493259402015202929L) return 7L;
        if (typeId == -8039584605842835988L) return 8L;
        throw new IllegalArgumentException("unknown type for rocksdynamic.Fixture.Value.Amount: " + typeId);
    }

    private static Zeze.Raft.RocksRaft.@Nullable Bean createDynamicValue_1(long typeId) {
        if (typeId == 7L) return new rocksdynamic.Fixture.Amount();
        if (typeId == 8L) return new rocksdynamic.Fixture.Quantity();
        return null;
    }

    private static final Zeze.Raft.RocksRaft.DynamicField dynamicField_2 =
            new Zeze.Raft.RocksRaft.DynamicField(TYPEID, 2,
                    Value::getSpecialTypeId_2, Value::createDynamicValue_2);

    private static long getSpecialTypeId_2(Zeze.Raft.RocksRaft.Bean value) {
        var typeId = value.typeId();
        if (typeId == -8039584605842835988L) return 7L;
        throw new IllegalArgumentException("unknown type for rocksdynamic.Fixture.Value.Quantity: " + typeId);
    }

    private static Zeze.Raft.RocksRaft.@Nullable Bean createDynamicValue_2(long typeId) {
        if (typeId == 7L) return new rocksdynamic.Fixture.Quantity();
        return null;
    }

    private static final Zeze.Raft.RocksRaft.DynamicField dynamicField_3 =
            new Zeze.Raft.RocksRaft.DynamicField(TYPEID, 3,
                    Value::getSpecialTypeId_3, Value::createDynamicValue_3);

    public static Zeze.Raft.RocksRaft.DynamicBean newDynamicBean_Amounts() {
        return dynamicField_3.newValue();
    }

    private static long getSpecialTypeId_3(Zeze.Raft.RocksRaft.Bean value) {
        var typeId = value.typeId();
        if (typeId == -5493259402015202929L) return 7L;
        if (typeId == 7712256038246874364L) return 8L;
        throw new IllegalArgumentException("unknown type for rocksdynamic.Fixture.Value.Amounts: " + typeId);
    }

    private static Zeze.Raft.RocksRaft.@Nullable Bean createDynamicValue_3(long typeId) {
        if (typeId == 7L) return new rocksdynamic.Fixture.Amount();
        if (typeId == 8L) return new rocksdynamic.Fixture.CustomPayload();
        return null;
    }

    private static final Zeze.Raft.RocksRaft.DynamicField dynamicField_4 =
            new Zeze.Raft.RocksRaft.DynamicField(TYPEID, 4,
                    Value::getSpecialTypeId_4, Value::createDynamicValue_4);

    public static Zeze.Raft.RocksRaft.DynamicBean newDynamicBean_Quantities() {
        return dynamicField_4.newValue();
    }

    private static long getSpecialTypeId_4(Zeze.Raft.RocksRaft.Bean value) {
        var typeId = value.typeId();
        if (typeId == -8039584605842835988L) return 7L;
        throw new IllegalArgumentException("unknown type for rocksdynamic.Fixture.Value.Quantities: " + typeId);
    }

    private static Zeze.Raft.RocksRaft.@Nullable Bean createDynamicValue_4(long typeId) {
        if (typeId == 7L) return new rocksdynamic.Fixture.Quantity();
        return null;
    }

    private static final Zeze.Raft.RocksRaft.DynamicField dynamicField_5 =
            new Zeze.Raft.RocksRaft.DynamicField(TYPEID, 5,
                    Value::getSpecialTypeId_5, Value::createDynamicValue_5);

    public static Zeze.Raft.RocksRaft.DynamicBean newDynamicBean_SortedAmounts() {
        return dynamicField_5.newValue();
    }

    private static long getSpecialTypeId_5(Zeze.Raft.RocksRaft.Bean value) {
        var typeId = value.typeId();
        if (typeId == -5493259402015202929L) return 7L;
        throw new IllegalArgumentException("unknown type for rocksdynamic.Fixture.Value.SortedAmounts: " + typeId);
    }

    private static Zeze.Raft.RocksRaft.@Nullable Bean createDynamicValue_5(long typeId) {
        if (typeId == 7L) return new rocksdynamic.Fixture.Amount();
        return null;
    }

    private static final Zeze.Raft.RocksRaft.DynamicField dynamicField_7 =
            new Zeze.Raft.RocksRaft.DynamicField(TYPEID, 7,
                    Value::getSpecialTypeId_7, Value::createDynamicValue_7);

    private static long getSpecialTypeId_7(Zeze.Raft.RocksRaft.Bean value) {
        var typeId = value.typeId();
        if (typeId == -5493259402015202929L) return 7L;
        throw new IllegalArgumentException("unknown type for rocksdynamic.Fixture.Value.Scratch: " + typeId);
    }

    private static Zeze.Raft.RocksRaft.@Nullable Bean createDynamicValue_7(long typeId) {
        if (typeId == 7L) return new rocksdynamic.Fixture.Amount();
        return null;
    }

    private static final Zeze.Raft.RocksRaft.DynamicField dynamicField_8 =
            new Zeze.Raft.RocksRaft.DynamicField(TYPEID, 8,
                    Value::getSpecialTypeId_8, Value::createDynamicValue_8);

    private static long getSpecialTypeId_8(Zeze.Raft.RocksRaft.Bean value) {
        return rocksdynamic.Fixture.DynamicFixture.identifyCustomPayload(value);
    }

    private static Zeze.Raft.RocksRaft.@Nullable Bean createDynamicValue_8(long typeId) {
        Zeze.Raft.RocksRaft.@Nullable Bean value = rocksdynamic.Fixture.DynamicFixture.createCustomPayload(typeId);
        if (value == null) return null;
        if (value.getClass() == rocksdynamic.Fixture.CustomPayload.class) return value;
        throw new IllegalArgumentException("undeclared RocksRaft dynamic payload for rocksdynamic.Fixture.Value.Custom: " + value.getClass().getName());
    }

    public static void registerDynamicFields() {
        dynamicField_1.registerValueLog();
        dynamicField_2.registerValueLog();
        dynamicField_3.registerListLog();
        dynamicField_4.registerMapLog(Integer.class);
        dynamicField_5.registerSortedMapLog(Integer.class);
        dynamicField_8.registerValueLog();
    }

    private final Zeze.Raft.RocksRaft.DynamicBean _Amount;
    private final Zeze.Raft.RocksRaft.DynamicBean _Quantity;
    private final Zeze.Raft.RocksRaft.CollList2<Zeze.Raft.RocksRaft.DynamicBean> _Amounts;
    private final Zeze.Raft.RocksRaft.CollMap2<Integer, Zeze.Raft.RocksRaft.DynamicBean> _Quantities;
    private final Zeze.Raft.RocksRaft.CollSortedMap2<Integer, Zeze.Raft.RocksRaft.DynamicBean> _SortedAmounts;
    private final rocksdynamic.Fixture.Nested _Nested;
    private final Zeze.Raft.RocksRaft.DynamicBean _Scratch;
    private final Zeze.Raft.RocksRaft.DynamicBean _Custom;
    private final rocksdynamic.Fixture.Defaults _Defaults;

    public Zeze.Raft.RocksRaft.DynamicBean getAmount() {
        return _Amount;
    }

    public rocksdynamic.Fixture.@Nullable Amount getAmount_rocksdynamic_Fixture_Amount() {
        return (rocksdynamic.Fixture.Amount)getAmount().getBean();
    }
    public void setAmount(rocksdynamic.Fixture.Amount value) {
        getAmount().setBean(value);
    }

    public rocksdynamic.Fixture.@Nullable Quantity getAmount_rocksdynamic_Fixture_Quantity() {
        return (rocksdynamic.Fixture.Quantity)getAmount().getBean();
    }
    public void setAmount(rocksdynamic.Fixture.Quantity value) {
        getAmount().setBean(value);
    }

    public Zeze.Raft.RocksRaft.DynamicBean getQuantity() {
        return _Quantity;
    }

    public rocksdynamic.Fixture.@Nullable Quantity getQuantity_rocksdynamic_Fixture_Quantity() {
        return (rocksdynamic.Fixture.Quantity)getQuantity().getBean();
    }
    public void setQuantity(rocksdynamic.Fixture.Quantity value) {
        getQuantity().setBean(value);
    }

    public Zeze.Raft.RocksRaft.CollList2<Zeze.Raft.RocksRaft.DynamicBean> getAmounts() {
        return _Amounts;
    }

    public Zeze.Raft.RocksRaft.CollMap2<Integer, Zeze.Raft.RocksRaft.DynamicBean> getQuantities() {
        return _Quantities;
    }

    public Zeze.Raft.RocksRaft.CollSortedMap2<Integer, Zeze.Raft.RocksRaft.DynamicBean> getSortedAmounts() {
        return _SortedAmounts;
    }

    public rocksdynamic.Fixture.Nested getNested() {
        return _Nested;
    }

    public Zeze.Raft.RocksRaft.DynamicBean getScratch() {
        return _Scratch;
    }

    public rocksdynamic.Fixture.@Nullable Amount getScratch_rocksdynamic_Fixture_Amount() {
        return (rocksdynamic.Fixture.Amount)getScratch().getBean();
    }
    public void setScratch(rocksdynamic.Fixture.Amount value) {
        getScratch().setBean(value);
    }

    public Zeze.Raft.RocksRaft.DynamicBean getCustom() {
        return _Custom;
    }

    public rocksdynamic.Fixture.@Nullable CustomPayload getCustom_rocksdynamic_Fixture_CustomPayload() {
        return (rocksdynamic.Fixture.CustomPayload)getCustom().getBean();
    }
    public void setCustom(rocksdynamic.Fixture.CustomPayload value) {
        getCustom().setBean(value);
    }

    public rocksdynamic.Fixture.Defaults getDefaults() {
        return _Defaults;
    }

    public Value() {
        _Amount = dynamicField_1.newValue();
        _Quantity = dynamicField_2.newValue();
        _Amounts = Zeze.Raft.RocksRaft.CollList2.dynamic(dynamicField_3);
        _Amounts.variableId(3);
        _Quantities = Zeze.Raft.RocksRaft.CollMap2.dynamic(Integer.class, dynamicField_4);
        _Quantities.variableId(4);
        _SortedAmounts = Zeze.Raft.RocksRaft.CollSortedMap2.dynamic(Integer.class, dynamicField_5);
        _SortedAmounts.variableId(5);
        _Nested = new rocksdynamic.Fixture.Nested();
        _Nested.variableId(6);
        _Scratch = dynamicField_7.newValue();
        _Custom = dynamicField_8.newValue();
        _Defaults = new rocksdynamic.Fixture.Defaults();
        _Defaults.variableId(9);
    }

    public void assign(Value other) {
        if (other == this)
            return;
        _Amount.assign(other.getAmount());
        _Quantity.assign(other.getQuantity());
        _Amounts.clear();
        for (var e : other._Amounts)
            _Amounts.add(e.copy());
        _Quantities.clear();
        for (var e : other._Quantities.entrySet())
            _Quantities.put(e.getKey(), e.getValue().copy());
        _SortedAmounts.clear();
        for (var e : other._SortedAmounts.entrySet())
            _SortedAmounts.put(e.getKey(), e.getValue().copy());
        _Nested.assign(other._Nested);
        _Scratch.assign(other.getScratch());
        _Custom.assign(other.getCustom());
        _Defaults.assign(other._Defaults);
    }

    public Value copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public Value copy() {
        var copy = new Value();
        copy.assign(this);
        return copy;
    }

    public static void swap(Value a, Value b) {
        Value save = a.copy();
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
        _s_.append("rocksdynamic.Fixture.Value: {\n");
        _s_.append(_i1_).append("Amount=");
        getAmount().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("Quantity=");
        getQuantity().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("Amounts=[\n");
        for (var _item_ : getAmounts()) {
            _s_.append(_i2_).append("Item=");
            _item_.buildString(_s_, _l_ + 12);
            _s_.append(",\n");
        }
        _s_.append(_i1_).append("],\n");
        _s_.append(_i1_).append("Quantities=[\n");
        for (var _kv_ : getQuantities().entrySet()) {
            _s_.append(_i2_).append("Key=").append(_kv_.getKey()).append(",\n");
            _s_.append(_i2_).append("Value=");
            _kv_.getValue().buildString(_s_, _l_ + 12);
            _s_.append(",\n");
        }
        _s_.append(_i1_).append("],\n");
        _s_.append(_i1_).append("SortedAmounts=[\n");
        for (var _kv_ : getSortedAmounts().entrySet()) {
            _s_.append(_i2_).append("Key=").append(_kv_.getKey()).append(",\n");
            _s_.append(_i2_).append("Value=");
            _kv_.getValue().buildString(_s_, _l_ + 12);
            _s_.append(",\n");
        }
        _s_.append(_i1_).append("],\n");
        _s_.append(_i1_).append("Nested=");
        getNested().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("Scratch=");
        getScratch().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("Custom=");
        getCustom().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("Defaults=");
        getDefaults().buildString(_s_, _l_ + 8);
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
            var _x_ = getAmount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            var _x_ = getQuantity();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            var _x_ = getAmounts();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.DYNAMIC);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = getQuantities();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.DYNAMIC);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = getSortedAmounts();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.DYNAMIC);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 6, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getNested().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            var _x_ = getCustom();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 9, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getDefaults().encode(_o_);
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
            _Amount.decodeTagged(_o_, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Amount.clear();
        }
        if (_i_ == 2) {
            _Quantity.decodeTagged(_o_, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Quantity.clear();
        }
        if (_i_ == 3) {
            var _x_ = getAmounts();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(newDynamicBean_Amounts().decodeTagged(_o_, _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Amounts.clear();
        }
        if (_i_ == 4) {
            var _x_ = getQuantities();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = newDynamicBean_Quantities().decodeTagged(_o_, _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Quantities.clear();
        }
        if (_i_ == 5) {
            var _x_ = getSortedAmounts();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = newDynamicBean_SortedAmounts().decodeTagged(_o_, _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _SortedAmounts.clear();
        }
        if (_i_ == 6) {
            _o_.ReadBean(_Nested, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Nested.restoreOmittedFields(new rocksdynamic.Fixture.Nested());
        }
        while (_t_ != 0 && _i_ < 8) {
            _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            _Custom.decodeTagged(_o_, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Custom.clear();
        }
        if (_i_ == 9) {
            _o_.ReadBean(_Defaults, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Defaults.restoreOmittedFields(new rocksdynamic.Fixture.Defaults());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.@Nullable RootInfo root) {
        _Amount.initRootInfo(root, this);
        _Quantity.initRootInfo(root, this);
        _Amounts.initRootInfo(root, this);
        _Quantities.initRootInfo(root, this);
        _SortedAmounts.initRootInfo(root, this);
        _Nested.initRootInfo(root, this);
        _Custom.initRootInfo(root, this);
        _Defaults.initRootInfo(root, this);
    }

    @Override
    public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 3: _Amounts.leaderApplyNoRecursive(vlog); break;
            case 4: _Quantities.leaderApplyNoRecursive(vlog); break;
            case 5: _SortedAmounts.leaderApplyNoRecursive(vlog); break;
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
                case 1: _Amount.followerApply(vlog); break;
                case 2: _Quantity.followerApply(vlog); break;
                case 3: _Amounts.followerApply(vlog); break;
                case 4: _Quantities.followerApply(vlog); break;
                case 5: _SortedAmounts.followerApply(vlog); break;
                case 6: _Nested.followerApply(vlog); break;
                case 8: _Custom.followerApply(vlog); break;
                case 9: _Defaults.followerApply(vlog); break;
            }
        }
    }
}
