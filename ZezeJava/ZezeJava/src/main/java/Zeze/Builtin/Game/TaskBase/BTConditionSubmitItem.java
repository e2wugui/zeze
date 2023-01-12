// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

// 内置条件类型：提交物品
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTConditionSubmitItem extends Zeze.Transaction.Bean implements BTConditionSubmitItemReadOnly {
    public static final long TYPEID = 5009432016461298914L;

    private final Zeze.Transaction.Collections.PMap1<Long, Integer> _items; // key：itemId，value：itemCount
    private final Zeze.Transaction.Collections.PMap1<Long, Integer> _itemsSubmitted; // key：itemId，value：itemCount

    public Zeze.Transaction.Collections.PMap1<Long, Integer> getItems() {
        return _items;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<Long, Integer> getItemsReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_items);
    }

    public Zeze.Transaction.Collections.PMap1<Long, Integer> getItemsSubmitted() {
        return _itemsSubmitted;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<Long, Integer> getItemsSubmittedReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_itemsSubmitted);
    }

    @SuppressWarnings("deprecation")
    public BTConditionSubmitItem() {
        _items = new Zeze.Transaction.Collections.PMap1<>(Long.class, Integer.class);
        _items.variableId(1);
        _itemsSubmitted = new Zeze.Transaction.Collections.PMap1<>(Long.class, Integer.class);
        _itemsSubmitted.variableId(2);
    }

    public void assign(BTConditionSubmitItem other) {
        _items.clear();
        _items.putAll(other._items);
        _itemsSubmitted.clear();
        _itemsSubmitted.putAll(other._itemsSubmitted);
    }

    @Deprecated
    public void Assign(BTConditionSubmitItem other) {
        assign(other);
    }

    public BTConditionSubmitItem copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTConditionSubmitItem copy() {
        var copy = new BTConditionSubmitItem();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BTConditionSubmitItem Copy() {
        return copy();
    }

    public static void swap(BTConditionSubmitItem a, BTConditionSubmitItem b) {
        BTConditionSubmitItem save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BTConditionSubmitItem: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("items={");
        if (!_items.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _items.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_kv_.getValue()).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("itemsSubmitted={");
        if (!_itemsSubmitted.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _itemsSubmitted.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_kv_.getValue()).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
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
            var _x_ = _items;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
                }
            }
        }
        {
            var _x_ = _itemsSubmitted;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
                }
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _items;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadInt(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _itemsSubmitted;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadInt(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _items.initRootInfo(root, this);
        _itemsSubmitted.initRootInfo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _items.values()) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _itemsSubmitted.values()) {
            if (_v_ < 0)
                return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _items.followerApply(vlog); break;
                case 2: _itemsSubmitted.followerApply(vlog); break;
            }
        }
    }
}
