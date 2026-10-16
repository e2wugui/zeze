// auto-generated @formatter:off
package Game.Buf;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BBufs extends Zeze.Transaction.Bean implements BBufsReadOnly {
    public static final long TYPEID = -6095071065680829700L;

    private final Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf> _Bufs;
    private static final Zeze.Transaction.Collections.Map2Meta<Integer, Game.Buf.BBuf> meta2_Bufs
            = Zeze.Transaction.Collections.Map2Meta.get(Integer.class, Game.Buf.BBuf.class);

    public Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf> getBufs() {
        return _Bufs;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Game.Buf.BBuf, Game.Buf.BBufReadOnly> getBufsReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Bufs);
    }

    @SuppressWarnings("deprecation")
    public BBufs() {
        _Bufs = new Zeze.Transaction.Collections.PMap2<>(meta2_Bufs);
        _Bufs.variableId(1);
    }

    @Override
    public void reset() {
        _Bufs.clear();
        _unknown_ = null;
    }

    public void assign(BBufs _o_) {
        _Bufs.clear();
        for (var _e_ : _o_._Bufs.entrySet())
            _Bufs.put(_e_.getKey(), _e_.getValue().copy());
        _unknown_ = _o_._unknown_;
    }

    public BBufs copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBufs copy() {
        var _c_ = new BBufs();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBufs _a_, BBufs _b_) {
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
        _s_.append("Game.Buf.BBufs: {\n");
        _s_.append(_i1_).append("Bufs={");
        if (!_Bufs.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Bufs.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Bufs.size()).append("]\n");
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

    private byte @Nullable [] _unknown_;

    public byte @Nullable [] unknown() {
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
            var _x_ = _Bufs;
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
            var _x_ = _Bufs;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new Game.Buf.BBuf(), _t_);
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
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BBufs))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBufs)_o_;
        if (!_Bufs.equals(_b_._Bufs))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Bufs.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Bufs.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Bufs.values()) {
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
                case 1: _Bufs.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonMap(this, "Bufs", _Bufs, _r_.getString(_pn_ + "Bufs"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Bufs", Zeze.Serialize.Helper.encodeJson(_Bufs));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Bufs", "map", "int", "Game.Buf.BBuf"));
        return _v_;
    }
}
