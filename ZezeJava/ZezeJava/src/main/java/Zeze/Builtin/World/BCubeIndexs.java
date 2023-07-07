// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BCubeIndexs extends Zeze.Transaction.Bean implements BCubeIndexsReadOnly {
    public static final long TYPEID = -2308609003851372978L;

    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.World.BCubeIndex> _CubeIndexs;

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.World.BCubeIndex> getCubeIndexs() {
        return _CubeIndexs;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.World.BCubeIndex, Zeze.Builtin.World.BCubeIndexReadOnly> getCubeIndexsReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_CubeIndexs);
    }

    @SuppressWarnings("deprecation")
    public BCubeIndexs() {
        _CubeIndexs = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.World.BCubeIndex.class);
        _CubeIndexs.variableId(1);
    }

    @Override
    public void reset() {
        _CubeIndexs.clear();
    }

    @Override
    public Zeze.Builtin.World.BCubeIndexs.Data toData() {
        var data = new Zeze.Builtin.World.BCubeIndexs.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BCubeIndexs.Data)other);
    }

    public void assign(BCubeIndexs.Data other) {
        _CubeIndexs.clear();
        for (var e : other._CubeIndexs) {
            Zeze.Builtin.World.BCubeIndex data = new Zeze.Builtin.World.BCubeIndex();
            data.assign(e);
            _CubeIndexs.add(data);
        }
    }

    public void assign(BCubeIndexs other) {
        _CubeIndexs.clear();
        for (var e : other._CubeIndexs)
            _CubeIndexs.add(e.copy());
    }

    public BCubeIndexs copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCubeIndexs copy() {
        var copy = new BCubeIndexs();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCubeIndexs a, BCubeIndexs b) {
        BCubeIndexs save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BCubeIndexs: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CubeIndexs=[");
        if (!_CubeIndexs.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _CubeIndexs) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(System.lineSeparator());
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
            var _x_ = _CubeIndexs;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
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
            var _x_ = _CubeIndexs;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.World.BCubeIndex(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _CubeIndexs.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _CubeIndexs.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _CubeIndexs) {
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
                case 1: _CubeIndexs.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonList(_CubeIndexs, Zeze.Builtin.World.BCubeIndex.class, rs.getString(_parents_name_ + "CubeIndexs"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "CubeIndexs", Zeze.Serialize.Helper.encodeJson(_CubeIndexs));
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2308609003851372978L;

    private java.util.ArrayList<Zeze.Builtin.World.BCubeIndex.Data> _CubeIndexs;

    public java.util.ArrayList<Zeze.Builtin.World.BCubeIndex.Data> getCubeIndexs() {
        return _CubeIndexs;
    }

    public void setCubeIndexs(java.util.ArrayList<Zeze.Builtin.World.BCubeIndex.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _CubeIndexs = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _CubeIndexs = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<Zeze.Builtin.World.BCubeIndex.Data> _CubeIndexs_) {
        if (_CubeIndexs_ == null)
            _CubeIndexs_ = new java.util.ArrayList<>();
        _CubeIndexs = _CubeIndexs_;
    }

    @Override
    public void reset() {
        _CubeIndexs.clear();
    }

    @Override
    public Zeze.Builtin.World.BCubeIndexs toBean() {
        var bean = new Zeze.Builtin.World.BCubeIndexs();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BCubeIndexs)other);
    }

    public void assign(BCubeIndexs other) {
        _CubeIndexs.clear();
        for (var e : other._CubeIndexs) {
            Zeze.Builtin.World.BCubeIndex.Data data = new Zeze.Builtin.World.BCubeIndex.Data();
            data.assign(e);
            _CubeIndexs.add(data);
        }
    }

    public void assign(BCubeIndexs.Data other) {
        _CubeIndexs.clear();
        for (var e : other._CubeIndexs)
            _CubeIndexs.add(e.copy());
    }

    @Override
    public BCubeIndexs.Data copy() {
        var copy = new BCubeIndexs.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCubeIndexs.Data a, BCubeIndexs.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCubeIndexs.Data clone() {
        return (BCubeIndexs.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BCubeIndexs: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CubeIndexs=[");
        if (!_CubeIndexs.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _CubeIndexs) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(System.lineSeparator());
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
            var _x_ = _CubeIndexs;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
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
            var _x_ = _CubeIndexs;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.World.BCubeIndex.Data(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
