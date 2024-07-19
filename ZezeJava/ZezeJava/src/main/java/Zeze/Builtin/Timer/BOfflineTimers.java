// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 记录多个定时器(timerId)的反向索引
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BOfflineTimers extends Zeze.Transaction.Bean implements BOfflineTimersReadOnly {
    public static final long TYPEID = -4429519688247847602L;

    private final Zeze.Transaction.Collections.PMap1<String, Integer> _OfflineTimers; // key是用户指定的timerId(用户指定的,或"@"+Base64编码的自动分配ID), value是注册定时器的serverId, 容量不能超过Config配置中的offlineTimerLimit

    public Zeze.Transaction.Collections.PMap1<String, Integer> getOfflineTimers() {
        return _OfflineTimers;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<String, Integer> getOfflineTimersReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_OfflineTimers);
    }

    @SuppressWarnings("deprecation")
    public BOfflineTimers() {
        _OfflineTimers = new Zeze.Transaction.Collections.PMap1<>(String.class, Integer.class);
        _OfflineTimers.variableId(1);
    }

    @Override
    public void reset() {
        _OfflineTimers.clear();
        _unknown_ = null;
    }

    public void assign(BOfflineTimers _o_) {
        _OfflineTimers.assign(_o_._OfflineTimers);
        _unknown_ = _o_._unknown_;
    }

    public BOfflineTimers copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOfflineTimers copy() {
        var _c_ = new BOfflineTimers();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BOfflineTimers _a_, BOfflineTimers _b_) {
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
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Timer.BOfflineTimers: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("OfflineTimers={");
        if (!_OfflineTimers.isEmpty()) {
            _s_.append(System.lineSeparator());
            _l_ += 4;
            for (var _e_ : _OfflineTimers.entrySet()) {
                _s_.append(Zeze.Util.Str.indent(_l_)).append("Key=").append(_e_.getKey()).append(',').append(System.lineSeparator());
                _s_.append(Zeze.Util.Str.indent(_l_)).append("Value=").append(_e_.getValue()).append(',').append(System.lineSeparator());
            }
            _l_ -= 4;
            _s_.append(Zeze.Util.Str.indent(_l_));
        }
        _s_.append('}').append(System.lineSeparator());
        _l_ -= 4;
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
            var _x_ = _OfflineTimers;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
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
            var _x_ = _OfflineTimers;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadInt(_t_);
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
        if (!(_o_ instanceof BOfflineTimers))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BOfflineTimers)_o_;
        if (!_OfflineTimers.equals(_b_._OfflineTimers))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _OfflineTimers.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _OfflineTimers.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _OfflineTimers.values()) {
            if (_v_ < 0)
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
                case 1: _OfflineTimers.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonMap(this, "OfflineTimers", _OfflineTimers, _r_.getString(_pn_ + "OfflineTimers"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "OfflineTimers", Zeze.Serialize.Helper.encodeJson(_OfflineTimers));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "OfflineTimers", "map", "string", "int"));
        return _v_;
    }
}
