// auto-generated @formatter:off
package ClientGame.Fight;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"MethodMayBeStatic", "ConstantConditions", "ConstantValue", "PatternVariableCanBeUsed", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "UnusedAssignment"})
public final class BFighterId implements Zeze.Transaction.BeanKey, Comparable<BFighterId> {
    public static final int TypeRole = 1;
    public static final int TypeMonster = 2;
    public static final int TypePet = 2; // ...

    private int _Type; // 战斗对象类型
    private long _InstanceId; // 战斗对象实例id，根据type含义不一样。

    // for decode only
    public BFighterId() {
    }

    public BFighterId(int _Type_, long _InstanceId_) {
        this._Type = _Type_;
        this._InstanceId = _InstanceId_;
    }

    public int getType() {
        return _Type;
    }

    public long getInstanceId() {
        return _InstanceId;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("ClientGame.Fight.BFighterId: {\n");
        _s_.append(_i1_).append("Type=").append(_Type).append(",\n");
        _s_.append(_i1_).append("InstanceId=").append(_InstanceId).append('\n');
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            int _x_ = _Type;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = _InstanceId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Type = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _InstanceId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BFighterId))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BFighterId)_o_;
        if (_Type != _b_._Type)
            return false;
        if (_InstanceId != _b_._InstanceId)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + Integer.hashCode(_Type);
        _h_ = _h_ * _p_ + Long.hashCode(_InstanceId);
        return _h_;
    }

    @Override
    public int compareTo(BFighterId _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = Integer.compare(_Type, _o_._Type);
            if (_c_ != 0)
                return _c_;
            _c_ = Long.compare(_InstanceId, _o_._InstanceId);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        if (getType() < 0)
            return true;
        if (getInstanceId() < 0)
            return true;
        return false;
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _Type = _r_.getInt(_pn_ + "Type");
        _InstanceId = _r_.getLong(_pn_ + "InstanceId");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "Type", _Type);
        _s_.appendLong(_pn_ + "InstanceId", _InstanceId);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = new java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data>();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Type", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "InstanceId", "long", "", ""));
        return vars;
    }
}
