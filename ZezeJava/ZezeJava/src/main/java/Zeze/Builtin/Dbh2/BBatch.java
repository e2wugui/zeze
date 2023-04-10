// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BBatch extends Zeze.Transaction.Bean implements BBatchReadOnly {
    public static final long TYPEID = -2614323448581124612L;

    private final Zeze.Transaction.Collections.PMap1<Zeze.Net.Binary, Zeze.Net.Binary> _Puts;
    private final Zeze.Transaction.Collections.PSet1<Zeze.Net.Binary> _Deletes;

    public Zeze.Transaction.Collections.PMap1<Zeze.Net.Binary, Zeze.Net.Binary> getPuts() {
        return _Puts;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<Zeze.Net.Binary, Zeze.Net.Binary> getPutsReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_Puts);
    }

    public Zeze.Transaction.Collections.PSet1<Zeze.Net.Binary> getDeletes() {
        return _Deletes;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Zeze.Net.Binary> getDeletesReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_Deletes);
    }

    @SuppressWarnings("deprecation")
    public BBatch() {
        _Puts = new Zeze.Transaction.Collections.PMap1<>(Zeze.Net.Binary.class, Zeze.Net.Binary.class);
        _Puts.variableId(1);
        _Deletes = new Zeze.Transaction.Collections.PSet1<>(Zeze.Net.Binary.class);
        _Deletes.variableId(2);
    }

    @Override
    public Zeze.Builtin.Dbh2.BBatch.Data toData() {
        var data = new Zeze.Builtin.Dbh2.BBatch.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BBatch.Data)other);
    }

    public void assign(BBatch.Data other) {
        _Puts.clear();
        _Puts.putAll(other.getPuts());
        _Deletes.clear();
        _Deletes.addAll(other.getDeletes());
    }

    public void assign(BBatch other) {
        _Puts.clear();
        _Puts.putAll(other.getPuts());
        _Deletes.clear();
        _Deletes.addAll(other.getDeletes());
    }

    public BBatch copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBatch copy() {
        var copy = new BBatch();
        copy.assign(this);
        return copy;
    }

    public static void swap(BBatch a, BBatch b) {
        BBatch save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BBatch: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Puts={");
        if (!_Puts.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Puts.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_kv_.getValue()).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Deletes={");
        if (!_Deletes.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Deletes) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
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
            var _x_ = _Puts;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteBinary(_e_.getKey());
                    _o_.WriteBinary(_e_.getValue());
                }
            }
        }
        {
            var _x_ = _Deletes;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteBinary(_v_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Puts;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBinary(_s_);
                    var _v_ = _o_.ReadBinary(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Deletes;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBinary(_t_));
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
        _Puts.initRootInfo(root, this);
        _Deletes.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Puts.initRootInfoWithRedo(root, this);
        _Deletes.initRootInfoWithRedo(root, this);
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
                case 1: _Puts.followerApply(vlog); break;
                case 2: _Deletes.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonMap(this, "Puts", getPuts(), rs.getString(_parents_name_ + "Puts"));
        Zeze.Serialize.Helper.decodeJsonSet(getDeletes(), Zeze.Net.Binary.class, rs.getString(_parents_name_ + "Deletes"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Puts", Zeze.Serialize.Helper.encodeJson(getPuts()));
        st.appendString(_parents_name_ + "Deletes", Zeze.Serialize.Helper.encodeJson(getDeletes()));
    }

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2614323448581124612L;

    private java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> _Puts;
    private java.util.HashSet<Zeze.Net.Binary> _Deletes;

    public java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> getPuts() {
        return _Puts;
    }

    public void setPuts(java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Puts = value;
    }

    public java.util.HashSet<Zeze.Net.Binary> getDeletes() {
        return _Deletes;
    }

    public void setDeletes(java.util.HashSet<Zeze.Net.Binary> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Deletes = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Puts = new java.util.HashMap<>();
        _Deletes = new java.util.HashSet<>();
    }

    @Override
    public Zeze.Builtin.Dbh2.BBatch toBean() {
        var bean = new Zeze.Builtin.Dbh2.BBatch();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BBatch)other);
    }

    public void assign(BBatch other) {
        _Puts.clear();
        _Puts.putAll(other.getPuts());
        _Deletes.clear();
        _Deletes.addAll(other.getDeletes());
    }

    public void assign(BBatch.Data other) {
        _Puts.clear();
        _Puts.putAll(other.getPuts());
        _Deletes.clear();
        _Deletes.addAll(other.getDeletes());
    }

    @Override
    public BBatch.Data copy() {
        var copy = new BBatch.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BBatch.Data a, BBatch.Data b) {
        var save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BBatch: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Puts={");
        if (!_Puts.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Puts.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_kv_.getValue()).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Deletes={");
        if (!_Deletes.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Deletes) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
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
            var _x_ = _Puts;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteBinary(_e_.getKey());
                    _o_.WriteBinary(_e_.getValue());
                }
            }
        }
        {
            var _x_ = _Deletes;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteBinary(_v_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Puts;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBinary(_s_);
                    var _v_ = _o_.ReadBinary(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Deletes;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBinary(_t_));
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
