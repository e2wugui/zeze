// auto-generated @formatter:off
package Zeze.Builtin.AutoKeyOld;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BAutoKey extends Zeze.Transaction.Bean implements BAutoKeyReadOnly {
    public static final long TYPEID = 1874176782463244405L;

    private long _NextId;

    @Override
    public long getNextId() {
        if (!isManaged())
            return _NextId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _NextId;
        var log = (Log__NextId)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _NextId;
    }

    public void setNextId(long _v_) {
        if (!isManaged()) {
            _NextId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__NextId(this, 1, _v_));
    }

    @SuppressWarnings("deprecation")
    public BAutoKey() {
    }

    @SuppressWarnings("deprecation")
    public BAutoKey(long _NextId_) {
        _NextId = _NextId_;
    }

    @Override
    public void reset() {
        setNextId(0);
        _unknown_ = null;
    }

    public void assign(BAutoKey _o_) {
        setNextId(_o_.getNextId());
        _unknown_ = _o_._unknown_;
    }

    public BAutoKey copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAutoKey copy() {
        var _c_ = new BAutoKey();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAutoKey _a_, BAutoKey _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__NextId extends Zeze.Transaction.Logs.LogLong {
        public Log__NextId(BAutoKey _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BAutoKey)getBelong())._NextId = value; }
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
        _s_.append("Zeze.Builtin.AutoKeyOld.BAutoKey: {\n");
        _s_.append(_i1_).append("NextId=").append(getNextId()).append('\n');
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
            long _x_ = getNextId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            setNextId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BAutoKey))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAutoKey)_o_;
        if (getNextId() != _b_.getNextId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getNextId() < 0)
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
                case 1: _NextId = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setNextId(_r_.getLong(_pn_ + "NextId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "NextId", getNextId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "NextId", "long", "", ""));
        return _v_;
    }
}
