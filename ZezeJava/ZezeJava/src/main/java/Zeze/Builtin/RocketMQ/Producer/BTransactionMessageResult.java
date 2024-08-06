// auto-generated @formatter:off
package Zeze.Builtin.RocketMQ.Producer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTransactionMessageResult extends Zeze.Transaction.Bean implements BTransactionMessageResultReadOnly {
    public static final long TYPEID = 9172956284242602104L;

    private boolean _Result;
    private long _Timestamp;

    @Override
    public boolean isResult() {
        if (!isManaged())
            return _Result;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Result;
        var log = (Log__Result)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Result;
    }

    public void setResult(boolean _v_) {
        if (!isManaged()) {
            _Result = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Result(this, 1, _v_));
    }

    @Override
    public long getTimestamp() {
        if (!isManaged())
            return _Timestamp;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Timestamp;
        var log = (Log__Timestamp)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Timestamp;
    }

    public void setTimestamp(long _v_) {
        if (!isManaged()) {
            _Timestamp = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Timestamp(this, 2, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTransactionMessageResult() {
    }

    @SuppressWarnings("deprecation")
    public BTransactionMessageResult(boolean _Result_, long _Timestamp_) {
        _Result = _Result_;
        _Timestamp = _Timestamp_;
    }

    @Override
    public void reset() {
        setResult(false);
        setTimestamp(0);
        _unknown_ = null;
    }

    public void assign(BTransactionMessageResult _o_) {
        setResult(_o_.isResult());
        setTimestamp(_o_.getTimestamp());
        _unknown_ = _o_._unknown_;
    }

    public BTransactionMessageResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTransactionMessageResult copy() {
        var _c_ = new BTransactionMessageResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTransactionMessageResult _a_, BTransactionMessageResult _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Result extends Zeze.Transaction.Logs.LogBool {
        public Log__Result(BTransactionMessageResult _b_, int _i_, boolean _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BTransactionMessageResult)getBelong())._Result = value; }
    }

    private static final class Log__Timestamp extends Zeze.Transaction.Logs.LogLong {
        public Log__Timestamp(BTransactionMessageResult _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BTransactionMessageResult)getBelong())._Timestamp = value; }
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
        _s_.append("Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult: {\n");
        _s_.append(_i1_).append("Result=").append(isResult()).append(",\n");
        _s_.append(_i1_).append("Timestamp=").append(getTimestamp()).append('\n');
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
            boolean _x_ = isResult();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            long _x_ = getTimestamp();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setResult(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTimestamp(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTransactionMessageResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTransactionMessageResult)_o_;
        if (isResult() != _b_.isResult())
            return false;
        if (getTimestamp() != _b_.getTimestamp())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getTimestamp() < 0)
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
                case 1: _Result = _v_.booleanValue(); break;
                case 2: _Timestamp = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setResult(_r_.getBoolean(_pn_ + "Result"));
        setTimestamp(_r_.getLong(_pn_ + "Timestamp"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendBoolean(_pn_ + "Result", isResult());
        _s_.appendLong(_pn_ + "Timestamp", getTimestamp());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Result", "bool", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Timestamp", "long", "", ""));
        return _v_;
    }
}
