// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
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
        var _d_ = new Zeze.Builtin.Dbh2.BRefused.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.BRefused.Data)_o_);
    }

    public void assign(BRefused.Data _o_) {
        _Refused.clear();
        for (var _e_ : _o_._Refused.entrySet()) {
            var _v_ = new Zeze.Builtin.Dbh2.BBatch();
            _v_.assign(_e_.getValue());
            _Refused.put(_e_.getKey(), _v_);
        }
        _unknown_ = null;
    }

    public void assign(BRefused _o_) {
        _Refused.clear();
        for (var _e_ : _o_._Refused.entrySet())
            _Refused.put(_e_.getKey(), _e_.getValue().copy());
        _unknown_ = _o_._unknown_;
    }

    public BRefused copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRefused copy() {
        var _c_ = new BRefused();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BRefused _a_, BRefused _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
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
        _s_.append("Zeze.Builtin.Dbh2.BRefused: {\n");
        _s_.append(_i1_).append("Refused={");
        if (!_Refused.isEmpty()) {
            _s_.append('\n');
            for (var _e_ : _Refused.entrySet()) {
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("}\n");
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BRefused))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BRefused)_o_;
        if (!_Refused.equals(_b_._Refused))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Refused.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Refused.initRootInfoWithRedo(_r_, this);
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
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _Refused.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonMap(this, "Refused", _Refused, _r_.getString(_pn_ + "Refused"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Refused", Zeze.Serialize.Helper.encodeJson(_Refused));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Refused", "map", "string", "Zeze.Builtin.Dbh2.BBatch"));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7657223106255732406L;

    private java.util.HashMap<String, Zeze.Builtin.Dbh2.BBatch.Data> _Refused;

    public java.util.HashMap<String, Zeze.Builtin.Dbh2.BBatch.Data> getRefused() {
        return _Refused;
    }

    public void setRefused(java.util.HashMap<String, Zeze.Builtin.Dbh2.BBatch.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Refused = _v_;
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
        var _b_ = new Zeze.Builtin.Dbh2.BRefused();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BRefused)_o_);
    }

    public void assign(BRefused _o_) {
        _Refused.clear();
        for (var _e_ : _o_._Refused.entrySet()) {
            var _v_ = new Zeze.Builtin.Dbh2.BBatch.Data();
            _v_.assign(_e_.getValue());
            _Refused.put(_e_.getKey(), _v_);
        }
    }

    public void assign(BRefused.Data _o_) {
        _Refused.clear();
        for (var _e_ : _o_._Refused.entrySet())
            _Refused.put(_e_.getKey(), _e_.getValue().copy());
    }

    @Override
    public BRefused.Data copy() {
        var _c_ = new BRefused.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BRefused.Data _a_, BRefused.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
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
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.Dbh2.BRefused: {\n");
        _s_.append(_i1_).append("Refused={");
        if (!_Refused.isEmpty()) {
            _s_.append('\n');
            for (var _e_ : _Refused.entrySet()) {
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("}\n");
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BRefused.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BRefused.Data)_o_;
        if (!_Refused.equals(_b_._Refused))
            return false;
        return true;
    }
}
}
