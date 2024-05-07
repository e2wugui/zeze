// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BRefused extends Zeze.Transaction.Bean implements BRefusedReadOnly {
    public static final long TYPEID = 7657223106255732406L;

    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Dbh2.BBatch> _Refused;

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Dbh2.BBatch> getRefused() {
        return _Refused;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Dbh2.BBatch, Zeze.Builtin.Dbh2.BBatchReadOnly> getRefusedReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Refused);
    }

    @SuppressWarnings("deprecation")
    public BRefused() {
        _Refused = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Dbh2.BBatch.class);
        _Refused.variableId(1);
    }

    @Override
    public void reset() {
        _Refused.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.BRefused.Data toData() {
        var data = new Zeze.Builtin.Dbh2.BRefused.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BRefused.Data)other);
    }

    public void assign(BRefused.Data other) {
        _Refused.clear();
        for (var e : other._Refused.entrySet()) {
            Zeze.Builtin.Dbh2.BBatch data = new Zeze.Builtin.Dbh2.BBatch();
            data.assign(e.getValue());
            _Refused.put(e.getKey(), data);
        }
        _unknown_ = null;
    }

    public void assign(BRefused other) {
        _Refused.clear();
        for (var e : other._Refused.entrySet())
            _Refused.put(e.getKey(), e.getValue().copy());
        _unknown_ = other._unknown_;
    }

    public BRefused copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRefused copy() {
        var copy = new BRefused();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRefused a, BRefused b) {
        BRefused save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BRefused: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Refused={");
        if (!_Refused.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Refused.entrySet()) {
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
            var _x_ = _Refused;
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
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Refused;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Dbh2.BBatch(), _t_);
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
        _Refused.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Refused.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Refused.values()) {
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
                case 1: _Refused.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonMap(this, "Refused", _Refused, rs.getString(_parents_name_ + "Refused"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Refused", Zeze.Serialize.Helper.encodeJson(_Refused));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Refused", "map", "string", "Zeze.Builtin.Dbh2.BBatch"));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7657223106255732406L;

    private java.util.HashMap<String, Zeze.Builtin.Dbh2.BBatch.Data> _Refused;

    public java.util.HashMap<String, Zeze.Builtin.Dbh2.BBatch.Data> getRefused() {
        return _Refused;
    }

    public void setRefused(java.util.HashMap<String, Zeze.Builtin.Dbh2.BBatch.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Refused = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Refused = new java.util.HashMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.HashMap<String, Zeze.Builtin.Dbh2.BBatch.Data> _Refused_) {
        if (_Refused_ == null)
            _Refused_ = new java.util.HashMap<>();
        _Refused = _Refused_;
    }

    @Override
    public void reset() {
        _Refused.clear();
    }

    @Override
    public Zeze.Builtin.Dbh2.BRefused toBean() {
        var bean = new Zeze.Builtin.Dbh2.BRefused();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BRefused)other);
    }

    public void assign(BRefused other) {
        _Refused.clear();
        for (var e : other._Refused.entrySet()) {
            Zeze.Builtin.Dbh2.BBatch.Data data = new Zeze.Builtin.Dbh2.BBatch.Data();
            data.assign(e.getValue());
            _Refused.put(e.getKey(), data);
        }
    }

    public void assign(BRefused.Data other) {
        _Refused.clear();
        for (var e : other._Refused.entrySet())
            _Refused.put(e.getKey(), e.getValue().copy());
    }

    @Override
    public BRefused.Data copy() {
        var copy = new BRefused.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRefused.Data a, BRefused.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BRefused.Data clone() {
        return (BRefused.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BRefused: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Refused={");
        if (!_Refused.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Refused.entrySet()) {
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
            var _x_ = _Refused;
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
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Refused;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Dbh2.BBatch.Data(), _t_);
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
