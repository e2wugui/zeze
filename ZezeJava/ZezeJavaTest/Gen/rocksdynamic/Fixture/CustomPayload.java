// auto-generated rocks @formatter:off
package rocksdynamic.Fixture;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "SuspiciousNameCombination"})
public final class CustomPayload extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = 7712256038246874364L;

    private final Zeze.Raft.RocksRaft.CollList1<Long> _Entries;
    private final Zeze.Raft.RocksRaft.CollList2<rocksdynamic.Fixture.Quantity> _List;
    private final Zeze.Raft.RocksRaft.CollMap2<Integer, rocksdynamic.Fixture.Quantity> _Map;
    private final Zeze.Raft.RocksRaft.CollSortedMap2<Integer, rocksdynamic.Fixture.Quantity> _SortedMap;

    public Zeze.Raft.RocksRaft.CollList1<Long> getEntries() {
        return _Entries;
    }

    public Zeze.Raft.RocksRaft.CollList2<rocksdynamic.Fixture.Quantity> getList() {
        return _List;
    }

    public Zeze.Raft.RocksRaft.CollMap2<Integer, rocksdynamic.Fixture.Quantity> getMap() {
        return _Map;
    }

    public Zeze.Raft.RocksRaft.CollSortedMap2<Integer, rocksdynamic.Fixture.Quantity> getSortedMap() {
        return _SortedMap;
    }

    public CustomPayload() {
        _Entries = new Zeze.Raft.RocksRaft.CollList1<>(Long.class);
        _Entries.variableId(1);
        _List = new Zeze.Raft.RocksRaft.CollList2<>(rocksdynamic.Fixture.Quantity.class);
        _List.variableId(2);
        _Map = new Zeze.Raft.RocksRaft.CollMap2<>(Integer.class, rocksdynamic.Fixture.Quantity.class);
        _Map.variableId(3);
        _SortedMap = new Zeze.Raft.RocksRaft.CollSortedMap2<>(Integer.class, rocksdynamic.Fixture.Quantity.class);
        _SortedMap.variableId(4);
    }

    public void assign(CustomPayload other) {
        if (other == this)
            return;
        _Entries.clear();
        for (var e : other._Entries)
            _Entries.add(e);
        _List.clear();
        for (var e : other._List)
            _List.add(e.copy());
        _Map.clear();
        for (var e : other._Map.entrySet())
            _Map.put(e.getKey(), e.getValue().copy());
        _SortedMap.clear();
        for (var e : other._SortedMap.entrySet())
            _SortedMap.put(e.getKey(), e.getValue().copy());
    }

    public CustomPayload copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public CustomPayload copy() {
        var copy = new CustomPayload();
        copy.assign(this);
        return copy;
    }

    public static void swap(CustomPayload a, CustomPayload b) {
        CustomPayload save = a.copy();
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
        _s_.append("rocksdynamic.Fixture.CustomPayload: {\n");
        _s_.append(_i1_).append("Entries=[\n");
        for (var _item_ : getEntries()) {
            _s_.append(_i2_).append("Item=").append(_item_).append(",\n");
        }
        _s_.append(_i1_).append("],\n");
        _s_.append(_i1_).append("List=[\n");
        for (var _item_ : getList()) {
            _s_.append(_i2_).append("Item=");
            _item_.buildString(_s_, _l_ + 12);
            _s_.append(",\n");
        }
        _s_.append(_i1_).append("],\n");
        _s_.append(_i1_).append("Map=[\n");
        for (var _kv_ : getMap().entrySet()) {
            _s_.append(_i2_).append("Key=").append(_kv_.getKey()).append(",\n");
            _s_.append(_i2_).append("Value=");
            _kv_.getValue().buildString(_s_, _l_ + 12);
            _s_.append(",\n");
        }
        _s_.append(_i1_).append("],\n");
        _s_.append(_i1_).append("SortedMap=[\n");
        for (var _kv_ : getSortedMap().entrySet()) {
            _s_.append(_i2_).append("Key=").append(_kv_.getKey()).append(",\n");
            _s_.append(_i2_).append("Value=");
            _kv_.getValue().buildString(_s_, _l_ + 12);
            _s_.append(",\n");
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
            var _x_ = getEntries();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = getList();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = getMap();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
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
            var _x_ = getSortedMap();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
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
            var _x_ = getEntries();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Entries.clear();
        }
        if (_i_ == 2) {
            var _x_ = getList();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new rocksdynamic.Fixture.Quantity(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _List.clear();
        }
        if (_i_ == 3) {
            var _x_ = getMap();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new rocksdynamic.Fixture.Quantity(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Map.clear();
        }
        if (_i_ == 4) {
            var _x_ = getSortedMap();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new rocksdynamic.Fixture.Quantity(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _SortedMap.clear();
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.@Nullable RootInfo root) {
        _Entries.initRootInfo(root, this);
        _List.initRootInfo(root, this);
        _Map.initRootInfo(root, this);
        _SortedMap.initRootInfo(root, this);
    }

    @Override
    public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 1: _Entries.leaderApplyNoRecursive(vlog); break;
            case 2: _List.leaderApplyNoRecursive(vlog); break;
            case 3: _Map.leaderApplyNoRecursive(vlog); break;
            case 4: _SortedMap.leaderApplyNoRecursive(vlog); break;
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
                case 1: _Entries.followerApply(vlog); break;
                case 2: _List.followerApply(vlog); break;
                case 3: _Map.followerApply(vlog); break;
                case 4: _SortedMap.followerApply(vlog); break;
            }
        }
    }
}
