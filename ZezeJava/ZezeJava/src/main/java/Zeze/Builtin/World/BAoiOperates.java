// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

// 命令 eAoiEnter,eAoiOperate的参数。
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BAoiOperates extends Zeze.Transaction.Bean implements BAoiOperatesReadOnly {
    public static final long TYPEID = 8731977537695035170L;

    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.World.BCubeIndex> _CubeIndex;
    private final Zeze.Transaction.Collections.PMap2<Zeze.Builtin.World.BObjectId, Zeze.Builtin.World.BAoiOperate> _Operates;

    public Zeze.Builtin.World.BCubeIndex getCubeIndex() {
        return _CubeIndex.getValue();
    }

    public void setCubeIndex(Zeze.Builtin.World.BCubeIndex value) {
        _CubeIndex.setValue(value);
    }

    @Override
    public Zeze.Builtin.World.BCubeIndexReadOnly getCubeIndexReadOnly() {
        return _CubeIndex.getValue();
    }

    public Zeze.Transaction.Collections.PMap2<Zeze.Builtin.World.BObjectId, Zeze.Builtin.World.BAoiOperate> getOperates() {
        return _Operates;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Zeze.Builtin.World.BObjectId, Zeze.Builtin.World.BAoiOperate, Zeze.Builtin.World.BAoiOperateReadOnly> getOperatesReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Operates);
    }

    @SuppressWarnings("deprecation")
    public BAoiOperates() {
        _CubeIndex = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.World.BCubeIndex(), Zeze.Builtin.World.BCubeIndex.class);
        _CubeIndex.variableId(1);
        _Operates = new Zeze.Transaction.Collections.PMap2<>(Zeze.Builtin.World.BObjectId.class, Zeze.Builtin.World.BAoiOperate.class);
        _Operates.variableId(2);
    }

    @Override
    public void reset() {
        _CubeIndex.reset();
        _Operates.clear();
    }

    @Override
    public Zeze.Builtin.World.BAoiOperates.Data toData() {
        var data = new Zeze.Builtin.World.BAoiOperates.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BAoiOperates.Data)other);
    }

    public void assign(BAoiOperates.Data other) {
        Zeze.Builtin.World.BCubeIndex data_CubeIndex = new Zeze.Builtin.World.BCubeIndex();
        data_CubeIndex.assign(other._CubeIndex);
        _CubeIndex.setValue(data_CubeIndex);
        _Operates.clear();
        for (var e : other._Operates.entrySet()) {
            Zeze.Builtin.World.BAoiOperate data = new Zeze.Builtin.World.BAoiOperate();
            data.assign(e.getValue());
            _Operates.put(e.getKey(), data);
        }
    }

    public void assign(BAoiOperates other) {
        _CubeIndex.assign(other._CubeIndex);
        _Operates.clear();
        for (var e : other._Operates.entrySet())
            _Operates.put(e.getKey(), e.getValue().copy());
    }

    public BAoiOperates copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAoiOperates copy() {
        var copy = new BAoiOperates();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAoiOperates a, BAoiOperates b) {
        BAoiOperates save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BAoiOperates: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CubeIndex=").append(System.lineSeparator());
        _CubeIndex.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Operates={");
        if (!_Operates.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Operates.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(System.lineSeparator());
                _kv_.getKey().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _CubeIndex.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            var _x_ = _Operates;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BEAN, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _e_.getKey().encode(_o_);
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
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadBean(_CubeIndex, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Operates;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBean(new Zeze.Builtin.World.BObjectId(), _s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.World.BAoiOperate(), _t_);
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
        _CubeIndex.initRootInfo(root, this);
        _Operates.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _CubeIndex.initRootInfoWithRedo(root, this);
        _Operates.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_CubeIndex.negativeCheck())
            return true;
        for (var _v_ : _Operates.values()) {
            if (_v_.negativeCheck())
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
                case 1: _CubeIndex.followerApply(vlog); break;
                case 2: _Operates.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        parents.add("CubeIndex");
        _CubeIndex.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonMap(this, "Operates", _Operates, rs.getString(_parents_name_ + "Operates"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        parents.add("CubeIndex");
        _CubeIndex.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Operates", Zeze.Serialize.Helper.encodeJson(_Operates));
    }

// 命令 eAoiEnter,eAoiOperate的参数。
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 8731977537695035170L;

    private Zeze.Builtin.World.BCubeIndex.Data _CubeIndex;
    private java.util.HashMap<Zeze.Builtin.World.BObjectId, Zeze.Builtin.World.BAoiOperate.Data> _Operates;

    public Zeze.Builtin.World.BCubeIndex.Data getCubeIndex() {
        return _CubeIndex;
    }

    public void setCubeIndex(Zeze.Builtin.World.BCubeIndex.Data value) {
        if (value == null)
            throw new IllegalArgumentException();
        _CubeIndex = value;
    }

    public java.util.HashMap<Zeze.Builtin.World.BObjectId, Zeze.Builtin.World.BAoiOperate.Data> getOperates() {
        return _Operates;
    }

    public void setOperates(java.util.HashMap<Zeze.Builtin.World.BObjectId, Zeze.Builtin.World.BAoiOperate.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Operates = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _CubeIndex = new Zeze.Builtin.World.BCubeIndex.Data();
        _Operates = new java.util.HashMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Builtin.World.BCubeIndex.Data _CubeIndex_, java.util.HashMap<Zeze.Builtin.World.BObjectId, Zeze.Builtin.World.BAoiOperate.Data> _Operates_) {
        if (_CubeIndex_ == null)
            _CubeIndex_ = new Zeze.Builtin.World.BCubeIndex.Data();
        _CubeIndex = _CubeIndex_;
        if (_Operates_ == null)
            _Operates_ = new java.util.HashMap<>();
        _Operates = _Operates_;
    }

    @Override
    public void reset() {
        _CubeIndex.reset();
        _Operates.clear();
    }

    @Override
    public Zeze.Builtin.World.BAoiOperates toBean() {
        var bean = new Zeze.Builtin.World.BAoiOperates();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BAoiOperates)other);
    }

    public void assign(BAoiOperates other) {
        _CubeIndex.assign(other._CubeIndex.getValue());
        _Operates.clear();
        for (var e : other._Operates.entrySet()) {
            Zeze.Builtin.World.BAoiOperate.Data data = new Zeze.Builtin.World.BAoiOperate.Data();
            data.assign(e.getValue());
            _Operates.put(e.getKey(), data);
        }
    }

    public void assign(BAoiOperates.Data other) {
        _CubeIndex.assign(other._CubeIndex);
        _Operates.clear();
        for (var e : other._Operates.entrySet())
            _Operates.put(e.getKey(), e.getValue().copy());
    }

    @Override
    public BAoiOperates.Data copy() {
        var copy = new BAoiOperates.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAoiOperates.Data a, BAoiOperates.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BAoiOperates.Data clone() {
        return (BAoiOperates.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BAoiOperates: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CubeIndex=").append(System.lineSeparator());
        _CubeIndex.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Operates={");
        if (!_Operates.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Operates.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(System.lineSeparator());
                _kv_.getKey().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _CubeIndex.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            var _x_ = _Operates;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BEAN, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _e_.getKey().encode(_o_);
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
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadBean(_CubeIndex, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Operates;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBean(new Zeze.Builtin.World.BObjectId(), _s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.World.BAoiOperate.Data(), _t_);
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
}
}
