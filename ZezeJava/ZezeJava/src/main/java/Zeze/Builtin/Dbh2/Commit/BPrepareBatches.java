// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BPrepareBatches extends Zeze.Transaction.Bean implements BPrepareBatchesReadOnly {
    public static final long TYPEID = -2881093366329974312L;

    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Dbh2.BPrepareBatch> _Datas;

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Dbh2.BPrepareBatch> getDatas() {
        return _Datas;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Dbh2.BPrepareBatch, Zeze.Builtin.Dbh2.BPrepareBatchReadOnly> getDatasReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Datas);
    }

    @SuppressWarnings("deprecation")
    public BPrepareBatches() {
        _Datas = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Dbh2.BPrepareBatch.class);
        _Datas.variableId(1);
    }

    @Override
    public void reset() {
        _Datas.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Commit.BPrepareBatches.Data toData() {
        var data = new Zeze.Builtin.Dbh2.Commit.BPrepareBatches.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.Commit.BPrepareBatches.Data)other);
    }

    public void assign(BPrepareBatches.Data other) {
        _Datas.clear();
        for (var e : other._Datas.entrySet()) {
            Zeze.Builtin.Dbh2.BPrepareBatch data = new Zeze.Builtin.Dbh2.BPrepareBatch();
            data.assign(e.getValue());
            _Datas.put(e.getKey(), data);
        }
        _unknown_ = null;
    }

    public void assign(BPrepareBatches other) {
        _Datas.clear();
        for (var e : other._Datas.entrySet())
            _Datas.put(e.getKey(), e.getValue().copy());
        _unknown_ = other._unknown_;
    }

    public BPrepareBatches copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BPrepareBatches copy() {
        var copy = new BPrepareBatches();
        copy.assign(this);
        return copy;
    }

    public static void swap(BPrepareBatches a, BPrepareBatches b) {
        BPrepareBatches save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Commit.BPrepareBatches: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Datas={");
        if (!_Datas.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Datas.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
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

    private byte[] _unknown_;

    public byte[] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
        int _i_ = 0;
        {
            var _x_ = _Datas;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Datas;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Dbh2.BPrepareBatch(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
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
        for (var _v_ : _Datas.values()) {
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
        Zeze.Serialize.Helper.decodeJsonMap(this, "Datas", _Datas, rs.getString(_parents_name_ + "Datas"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Datas", Zeze.Serialize.Helper.encodeJson(_Datas));
    }

    @Override
    public java.util.List<Zeze.Transaction.Bean.Variable> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Transaction.Bean.Variable(1, "Datas", "map", "string", "Zeze.Builtin.Dbh2.BPrepareBatch"));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BPrepareBatches
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2881093366329974312L;

    private java.util.HashMap<String, Zeze.Builtin.Dbh2.BPrepareBatch.Data> _Datas;

    public java.util.HashMap<String, Zeze.Builtin.Dbh2.BPrepareBatch.Data> getDatas() {
        return _Datas;
    }

    public void setDatas(java.util.HashMap<String, Zeze.Builtin.Dbh2.BPrepareBatch.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Datas = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Datas = new java.util.HashMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.HashMap<String, Zeze.Builtin.Dbh2.BPrepareBatch.Data> _Datas_) {
        if (_Datas_ == null)
            _Datas_ = new java.util.HashMap<>();
        _Datas = _Datas_;
    }

    @Override
    public void reset() {
        _Datas.clear();
    }

    @Override
    public Zeze.Builtin.Dbh2.Commit.BPrepareBatches toBean() {
        var bean = new Zeze.Builtin.Dbh2.Commit.BPrepareBatches();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BPrepareBatches)other);
    }

    public void assign(BPrepareBatches other) {
        _Datas.clear();
        for (var e : other._Datas.entrySet()) {
            Zeze.Builtin.Dbh2.BPrepareBatch.Data data = new Zeze.Builtin.Dbh2.BPrepareBatch.Data();
            data.assign(e.getValue());
            _Datas.put(e.getKey(), data);
        }
    }

    public void assign(BPrepareBatches.Data other) {
        _Datas.clear();
        for (var e : other._Datas.entrySet())
            _Datas.put(e.getKey(), e.getValue().copy());
    }

    @Override
    public BPrepareBatches.Data copy() {
        var copy = new BPrepareBatches.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BPrepareBatches.Data a, BPrepareBatches.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BPrepareBatches.Data clone() {
        return (BPrepareBatches.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Commit.BPrepareBatches: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Datas={");
        if (!_Datas.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Datas.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
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
            var _x_ = _Datas;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
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
            var _x_ = _Datas;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Dbh2.BPrepareBatch.Data(), _t_);
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
