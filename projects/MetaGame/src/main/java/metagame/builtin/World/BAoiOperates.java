// auto-generated @formatter:off
package metagame.builtin.World;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 命令 eAoiEnter,eAoiOperate的参数。
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BAoiOperates extends Zeze.Transaction.Bean implements BAoiOperatesReadOnly {
    public static final long TYPEID = 381025645242791091L;

    private final Zeze.Transaction.Collections.PMap2<Long, metagame.builtin.World.BAoiOperate> _Operates;

    public Zeze.Transaction.Collections.PMap2<Long, metagame.builtin.World.BAoiOperate> getOperates() {
        return _Operates;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, metagame.builtin.World.BAoiOperate, metagame.builtin.World.BAoiOperateReadOnly> getOperatesReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Operates);
    }

    @SuppressWarnings("deprecation")
    public BAoiOperates() {
        _Operates = new Zeze.Transaction.Collections.PMap2<>(Long.class, metagame.builtin.World.BAoiOperate.class);
        _Operates.variableId(1);
    }

    @Override
    public void reset() {
        _Operates.clear();
        _unknown_ = null;
    }

    @Override
    public metagame.builtin.World.BAoiOperates.Data toData() {
        var _d_ = new metagame.builtin.World.BAoiOperates.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((metagame.builtin.World.BAoiOperates.Data)_o_);
    }

    public void assign(BAoiOperates.Data _o_) {
        _Operates.clear();
        for (var _e_ : _o_._Operates.entrySet()) {
            var _v_ = new metagame.builtin.World.BAoiOperate();
            _v_.assign(_e_.getValue());
            _Operates.put(_e_.getKey(), _v_);
        }
        _unknown_ = null;
    }

    public void assign(BAoiOperates _o_) {
        _Operates.clear();
        for (var _e_ : _o_._Operates.entrySet())
            _Operates.put(_e_.getKey(), _e_.getValue().copy());
        _unknown_ = _o_._unknown_;
    }

    public BAoiOperates copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAoiOperates copy() {
        var _c_ = new BAoiOperates();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAoiOperates _a_, BAoiOperates _b_) {
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
        _s_.append("metagame.builtin.World.BAoiOperates: {\n");
        _s_.append(_i1_).append("Operates={");
        if (!_Operates.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Operates.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Operates.size()).append("]\n");
                    break;
                }
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
            var _x_ = _Operates;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
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
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Operates;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new metagame.builtin.World.BAoiOperate(), _t_);
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
        if (!(_o_ instanceof BAoiOperates))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAoiOperates)_o_;
        if (!_Operates.equals(_b_._Operates))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Operates.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Operates.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Operates.values()) {
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
                case 1: _Operates.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonMap(this, "Operates", _Operates, _r_.getString(_pn_ + "Operates"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Operates", Zeze.Serialize.Helper.encodeJson(_Operates));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Operates", "map", "long", "metagame.builtin.World.BAoiOperate"));
        return _v_;
    }

// 命令 eAoiEnter,eAoiOperate的参数。
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 381025645242791091L;

    private java.util.HashMap<Long, metagame.builtin.World.BAoiOperate.Data> _Operates;

    public java.util.HashMap<Long, metagame.builtin.World.BAoiOperate.Data> getOperates() {
        return _Operates;
    }

    public void setOperates(java.util.HashMap<Long, metagame.builtin.World.BAoiOperate.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Operates = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Operates = new java.util.HashMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.HashMap<Long, metagame.builtin.World.BAoiOperate.Data> _Operates_) {
        if (_Operates_ == null)
            _Operates_ = new java.util.HashMap<>();
        _Operates = _Operates_;
    }

    @Override
    public void reset() {
        _Operates.clear();
    }

    @Override
    public metagame.builtin.World.BAoiOperates toBean() {
        var _b_ = new metagame.builtin.World.BAoiOperates();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BAoiOperates)_o_);
    }

    public void assign(BAoiOperates _o_) {
        _Operates.clear();
        for (var _e_ : _o_._Operates.entrySet()) {
            var _v_ = new metagame.builtin.World.BAoiOperate.Data();
            _v_.assign(_e_.getValue());
            _Operates.put(_e_.getKey(), _v_);
        }
    }

    public void assign(BAoiOperates.Data _o_) {
        _Operates.clear();
        for (var _e_ : _o_._Operates.entrySet())
            _Operates.put(_e_.getKey(), _e_.getValue().copy());
    }

    @Override
    public BAoiOperates.Data copy() {
        var _c_ = new BAoiOperates.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAoiOperates.Data _a_, BAoiOperates.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
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
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("metagame.builtin.World.BAoiOperates: {\n");
        _s_.append(_i1_).append("Operates={");
        if (!_Operates.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Operates.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Operates.size()).append("]\n");
                    break;
                }
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
            var _x_ = _Operates;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
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
            var _x_ = _Operates;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new metagame.builtin.World.BAoiOperate.Data(), _t_);
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
        if (!(_o_ instanceof BAoiOperates.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAoiOperates.Data)_o_;
        if (!_Operates.equals(_b_._Operates))
            return false;
        return true;
    }
}
}
