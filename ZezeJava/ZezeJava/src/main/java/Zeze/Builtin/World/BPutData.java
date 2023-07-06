// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BPutData extends Zeze.Transaction.Bean implements BPutDataReadOnly {
    public static final long TYPEID = 4767639211156820067L;

    private final Zeze.Transaction.Collections.PMap1<Zeze.Builtin.World.ObjectId, Zeze.Net.Binary> _Datas;

    public Zeze.Transaction.Collections.PMap1<Zeze.Builtin.World.ObjectId, Zeze.Net.Binary> getDatas() {
        return _Datas;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<Zeze.Builtin.World.ObjectId, Zeze.Net.Binary> getDatasReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_Datas);
    }

    @SuppressWarnings("deprecation")
    public BPutData() {
        _Datas = new Zeze.Transaction.Collections.PMap1<>(Zeze.Builtin.World.ObjectId.class, Zeze.Net.Binary.class);
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
        _Datas.putAll(other._Datas);
    }

    public void assign(BPutData other) {
        _Datas.clear();
        _Datas.putAll(other._Datas);
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
        sb.append(Zeze.Util.Str.indent(level)).append("Datas={");
        if (!_Datas.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Datas.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(System.lineSeparator());
                _kv_.getKey().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
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
            var _x_ = _Datas;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BEAN, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _e_.getKey().encode(_o_);
                    _o_.WriteBinary(_e_.getValue());
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
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBean(new Zeze.Builtin.World.ObjectId(), _s_);
                    var _v_ = _o_.ReadBinary(_t_);
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
        _Datas.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Datas.initRootInfoWithRedo(root, this);
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
        Zeze.Serialize.Helper.decodeJsonMap(this, "Datas", _Datas, rs.getString(_parents_name_ + "Datas"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Datas", Zeze.Serialize.Helper.encodeJson(_Datas));
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4767639211156820067L;

    private java.util.HashMap<Zeze.Builtin.World.ObjectId, Zeze.Net.Binary> _Datas;

    public java.util.HashMap<Zeze.Builtin.World.ObjectId, Zeze.Net.Binary> getDatas() {
        return _Datas;
    }

    public void setDatas(java.util.HashMap<Zeze.Builtin.World.ObjectId, Zeze.Net.Binary> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Datas = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Datas = new java.util.HashMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.HashMap<Zeze.Builtin.World.ObjectId, Zeze.Net.Binary> _Datas_) {
        if (_Datas_ == null)
            _Datas_ = new java.util.HashMap<>();
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
        _Datas.putAll(other._Datas);
    }

    public void assign(BPutData.Data other) {
        _Datas.clear();
        _Datas.putAll(other._Datas);
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
        sb.append(Zeze.Util.Str.indent(level)).append("Datas={");
        if (!_Datas.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Datas.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(System.lineSeparator());
                _kv_.getKey().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
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
            var _x_ = _Datas;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BEAN, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _e_.getKey().encode(_o_);
                    _o_.WriteBinary(_e_.getValue());
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
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBean(new Zeze.Builtin.World.ObjectId(), _s_);
                    var _v_ = _o_.ReadBinary(_t_);
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
