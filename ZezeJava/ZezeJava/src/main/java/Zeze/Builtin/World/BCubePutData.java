// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BCubePutData extends Zeze.Transaction.Bean implements BCubePutDataReadOnly {
    public static final long TYPEID = 302569036718701960L;

    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.World.BCubeIndex> _CubeIndex;
    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.World.BOperate> _Datas;

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

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.World.BOperate> getDatas() {
        return _Datas;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.World.BOperate, Zeze.Builtin.World.BOperateReadOnly> getDatasReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Datas);
    }

    @SuppressWarnings("deprecation")
    public BCubePutData() {
        _CubeIndex = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.World.BCubeIndex(), Zeze.Builtin.World.BCubeIndex.class);
        _CubeIndex.variableId(1);
        _Datas = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.World.BOperate.class);
        _Datas.variableId(2);
    }

    @Override
    public void reset() {
        _CubeIndex.reset();
        _Datas.clear();
    }

    @Override
    public Zeze.Builtin.World.BCubePutData.Data toData() {
        var data = new Zeze.Builtin.World.BCubePutData.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BCubePutData.Data)other);
    }

    public void assign(BCubePutData.Data other) {
        Zeze.Builtin.World.BCubeIndex data_CubeIndex = new Zeze.Builtin.World.BCubeIndex();
        data_CubeIndex.assign(other._CubeIndex);
        _CubeIndex.setValue(data_CubeIndex);
        _Datas.clear();
        for (var e : other._Datas) {
            Zeze.Builtin.World.BOperate data = new Zeze.Builtin.World.BOperate();
            data.assign(e);
            _Datas.add(data);
        }
    }

    public void assign(BCubePutData other) {
        _CubeIndex.assign(other._CubeIndex);
        _Datas.clear();
        for (var e : other._Datas)
            _Datas.add(e.copy());
    }

    public BCubePutData copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCubePutData copy() {
        var copy = new BCubePutData();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCubePutData a, BCubePutData b) {
        BCubePutData save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BCubePutData: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CubeIndex=").append(System.lineSeparator());
        _CubeIndex.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Datas=[");
        if (!_Datas.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Datas) {
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
            var _x_ = _Datas;
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
            var _x_ = _Datas;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.World.BOperate(), _t_));
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
        _CubeIndex.initRootInfo(root, this);
        _Datas.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _CubeIndex.initRootInfoWithRedo(root, this);
        _Datas.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_CubeIndex.negativeCheck())
            return true;
        for (var _v_ : _Datas) {
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
                case 2: _Datas.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        parents.add("CubeIndex");
        _CubeIndex.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonList(_Datas, Zeze.Builtin.World.BOperate.class, rs.getString(_parents_name_ + "Datas"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        parents.add("CubeIndex");
        _CubeIndex.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Datas", Zeze.Serialize.Helper.encodeJson(_Datas));
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 302569036718701960L;

    private Zeze.Builtin.World.BCubeIndex.Data _CubeIndex;
    private java.util.ArrayList<Zeze.Builtin.World.BOperate.Data> _Datas;

    public Zeze.Builtin.World.BCubeIndex.Data getCubeIndex() {
        return _CubeIndex;
    }

    public void setCubeIndex(Zeze.Builtin.World.BCubeIndex.Data value) {
        if (value == null)
            throw new IllegalArgumentException();
        _CubeIndex = value;
    }

    public java.util.ArrayList<Zeze.Builtin.World.BOperate.Data> getDatas() {
        return _Datas;
    }

    public void setDatas(java.util.ArrayList<Zeze.Builtin.World.BOperate.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Datas = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _CubeIndex = new Zeze.Builtin.World.BCubeIndex.Data();
        _Datas = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Builtin.World.BCubeIndex.Data _CubeIndex_, java.util.ArrayList<Zeze.Builtin.World.BOperate.Data> _Datas_) {
        if (_CubeIndex_ == null)
            _CubeIndex_ = new Zeze.Builtin.World.BCubeIndex.Data();
        _CubeIndex = _CubeIndex_;
        if (_Datas_ == null)
            _Datas_ = new java.util.ArrayList<>();
        _Datas = _Datas_;
    }

    @Override
    public void reset() {
        _CubeIndex.reset();
        _Datas.clear();
    }

    @Override
    public Zeze.Builtin.World.BCubePutData toBean() {
        var bean = new Zeze.Builtin.World.BCubePutData();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BCubePutData)other);
    }

    public void assign(BCubePutData other) {
        _CubeIndex.assign(other._CubeIndex.getValue());
        _Datas.clear();
        for (var e : other._Datas) {
            Zeze.Builtin.World.BOperate.Data data = new Zeze.Builtin.World.BOperate.Data();
            data.assign(e);
            _Datas.add(data);
        }
    }

    public void assign(BCubePutData.Data other) {
        _CubeIndex.assign(other._CubeIndex);
        _Datas.clear();
        for (var e : other._Datas)
            _Datas.add(e.copy());
    }

    @Override
    public BCubePutData.Data copy() {
        var copy = new BCubePutData.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCubePutData.Data a, BCubePutData.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCubePutData.Data clone() {
        return (BCubePutData.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BCubePutData: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CubeIndex=").append(System.lineSeparator());
        _CubeIndex.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Datas=[");
        if (!_Datas.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Datas) {
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
            var _x_ = _Datas;
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
            var _x_ = _Datas;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.World.BOperate.Data(), _t_));
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
