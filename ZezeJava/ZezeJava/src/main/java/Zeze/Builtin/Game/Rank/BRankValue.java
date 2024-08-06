// auto-generated @formatter:off
package Zeze.Builtin.Game.Rank;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BRankValue extends Zeze.Transaction.Bean implements BRankValueReadOnly {
    public static final long TYPEID = 2276228832088785165L;

    private long _RoleId;
    private long _Value; // 含义由 BConcurrentKey.RankType 决定
    private Zeze.Net.Binary _ValueEx; // 自定义数据。

    @Override
    public long getRoleId() {
        if (!isManaged())
            return _RoleId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RoleId;
        var log = (Log__RoleId)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _RoleId;
    }

    public void setRoleId(long _v_) {
        if (!isManaged()) {
            _RoleId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__RoleId(this, 1, _v_));
    }

    @Override
    public long getValue() {
        if (!isManaged())
            return _Value;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Value;
        var log = (Log__Value)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Value;
    }

    public void setValue(long _v_) {
        if (!isManaged()) {
            _Value = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Value(this, 2, _v_));
    }

    @Override
    public Zeze.Net.Binary getValueEx() {
        if (!isManaged())
            return _ValueEx;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ValueEx;
        var log = (Log__ValueEx)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _ValueEx;
    }

    public void setValueEx(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ValueEx = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ValueEx(this, 3, _v_));
    }

    @SuppressWarnings("deprecation")
    public BRankValue() {
        _ValueEx = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BRankValue(long _RoleId_, long _Value_, Zeze.Net.Binary _ValueEx_) {
        _RoleId = _RoleId_;
        _Value = _Value_;
        if (_ValueEx_ == null)
            _ValueEx_ = Zeze.Net.Binary.Empty;
        _ValueEx = _ValueEx_;
    }

    @Override
    public void reset() {
        setRoleId(0);
        setValue(0);
        setValueEx(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    public void assign(BRankValue _o_) {
        setRoleId(_o_.getRoleId());
        setValue(_o_.getValue());
        setValueEx(_o_.getValueEx());
        _unknown_ = _o_._unknown_;
    }

    public BRankValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRankValue copy() {
        var _c_ = new BRankValue();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BRankValue _a_, BRankValue _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__RoleId extends Zeze.Transaction.Logs.LogLong {
        public Log__RoleId(BRankValue _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BRankValue)getBelong())._RoleId = value; }
    }

    private static final class Log__Value extends Zeze.Transaction.Logs.LogLong {
        public Log__Value(BRankValue _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BRankValue)getBelong())._Value = value; }
    }

    private static final class Log__ValueEx extends Zeze.Transaction.Logs.LogBinary {
        public Log__ValueEx(BRankValue _b_, int _i_, Zeze.Net.Binary _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BRankValue)getBelong())._ValueEx = value; }
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
        _s_.append("Zeze.Builtin.Game.Rank.BRankValue: {\n");
        _s_.append(_i1_).append("RoleId=").append(getRoleId()).append(",\n");
        _s_.append(_i1_).append("Value=").append(getValue()).append(",\n");
        _s_.append(_i1_).append("ValueEx=").append(getValueEx()).append('\n');
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
            long _x_ = getRoleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getValue();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getValueEx();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
            setRoleId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setValue(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setValueEx(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BRankValue))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BRankValue)_o_;
        if (getRoleId() != _b_.getRoleId())
            return false;
        if (getValue() != _b_.getValue())
            return false;
        if (!getValueEx().equals(_b_.getValueEx()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getRoleId() < 0)
            return true;
        if (getValue() < 0)
            return true;
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
                case 1: _RoleId = _v_.longValue(); break;
                case 2: _Value = _v_.longValue(); break;
                case 3: _ValueEx = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setRoleId(_r_.getLong(_pn_ + "RoleId"));
        setValue(_r_.getLong(_pn_ + "Value"));
        setValueEx(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "ValueEx")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "RoleId", getRoleId());
        _s_.appendLong(_pn_ + "Value", getValue());
        _s_.appendBinary(_pn_ + "ValueEx", getValueEx());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "RoleId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Value", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ValueEx", "binary", "", ""));
        return _v_;
    }
}
