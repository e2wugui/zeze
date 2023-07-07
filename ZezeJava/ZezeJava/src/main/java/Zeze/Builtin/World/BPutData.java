// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BPutData extends Zeze.Transaction.Bean implements BPutDataReadOnly {
    public static final long TYPEID = 4767639211156820067L;

    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.World.BEditData> _Datas;

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.World.BEditData> getDatas() {
        return _Datas;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.World.BEditData, Zeze.Builtin.World.BEditDataReadOnly> getDatasReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Datas);
    }

    @SuppressWarnings("deprecation")
    public BPutData() {
        _Datas = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.World.BEditData.class);
        _Datas.variableId(1);
    }

    @Override
    public void reset() {
        _Datas.clear();
    }

    @Override
    public Zeze.Builtin.World.BPutData.Data toData() {
        var data = new Zeze.Builtin.World.BPutData.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BPutData.Data)other);
    }

    public void assign(BPutData.Data other) {
        _Datas.clear();
        for (var e : other._Datas) {
            Zeze.Builtin.World.BEditData data = new Zeze.Builtin.World.BEditData();
            data.assign(e);
            _Datas.add(data);
        }
    }

    public void assign(BPutData other) {
        _Datas.clear();
        for (var e : other._Datas)
            _Datas.add(e.copy());
    }

    public BPutData copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BPutData copy() {
        var copy = new BPutData();
        copy.assign(this);
        return copy;
    }

    public static void swap(BPutData a, BPutData b) {
        BPutData save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BPutData: {").append(System.lineSeparator());
        level += 4;
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
            var _x_ = _Datas;
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
            var _x_ = _Datas;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.World.BEditData(), _t_));
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
        _Datas.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Datas.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
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
                case 1: _Datas.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonList(_Datas, Zeze.Builtin.World.BEditData.class, rs.getString(_parents_name_ + "Datas"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Datas", Zeze.Serialize.Helper.encodeJson(_Datas));
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4767639211156820067L;

    private java.util.ArrayList<Zeze.Builtin.World.BEditData.Data> _Datas;

    public java.util.ArrayList<Zeze.Builtin.World.BEditData.Data> getDatas() {
        return _Datas;
    }

    public void setDatas(java.util.ArrayList<Zeze.Builtin.World.BEditData.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Datas = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Datas = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<Zeze.Builtin.World.BEditData.Data> _Datas_) {
        if (_Datas_ == null)
            _Datas_ = new java.util.ArrayList<>();
        _Datas = _Datas_;
    }

    @Override
    public void reset() {
        _Datas.clear();
    }

    @Override
    public Zeze.Builtin.World.BPutData toBean() {
        var bean = new Zeze.Builtin.World.BPutData();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BPutData)other);
    }

    public void assign(BPutData other) {
        _Datas.clear();
        for (var e : other._Datas) {
            Zeze.Builtin.World.BEditData.Data data = new Zeze.Builtin.World.BEditData.Data();
            data.assign(e);
            _Datas.add(data);
        }
    }

    public void assign(BPutData.Data other) {
        _Datas.clear();
        for (var e : other._Datas)
            _Datas.add(e.copy());
    }

    @Override
    public BPutData.Data copy() {
        var copy = new BPutData.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BPutData.Data a, BPutData.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BPutData.Data clone() {
        return (BPutData.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BPutData: {").append(System.lineSeparator());
        level += 4;
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
            var _x_ = _Datas;
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
            var _x_ = _Datas;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.World.BEditData.Data(), _t_));
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
