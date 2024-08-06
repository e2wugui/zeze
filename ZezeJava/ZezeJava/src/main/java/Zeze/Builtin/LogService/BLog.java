// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BLog extends Zeze.Transaction.Bean implements BLogReadOnly {
    public static final long TYPEID = 3900400357954919579L;

    private long _Time;
    private String _Log;

    @Override
    public long getTime() {
        if (!isManaged())
            return _Time;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Time;
        var log = (Log__Time)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Time;
    }

    public void setTime(long _v_) {
        if (!isManaged()) {
            _Time = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Time(this, 1, _v_));
    }

    @Override
    public String getLog() {
        if (!isManaged())
            return _Log;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Log;
        var log = (Log__Log)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Log;
    }

    public void setLog(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Log = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Log(this, 2, _v_));
    }

    @SuppressWarnings("deprecation")
    public BLog() {
        _Log = "";
    }

    @SuppressWarnings("deprecation")
    public BLog(long _Time_, String _Log_) {
        _Time = _Time_;
        if (_Log_ == null)
            _Log_ = "";
        _Log = _Log_;
    }

    @Override
    public void reset() {
        setTime(0);
        setLog("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BLog.Data toData() {
        var _d_ = new Zeze.Builtin.LogService.BLog.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.LogService.BLog.Data)_o_);
    }

    public void assign(BLog.Data _o_) {
        setTime(_o_._Time);
        setLog(_o_._Log);
        _unknown_ = null;
    }

    public void assign(BLog _o_) {
        setTime(_o_.getTime());
        setLog(_o_.getLog());
        _unknown_ = _o_._unknown_;
    }

    public BLog copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLog copy() {
        var _c_ = new BLog();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLog _a_, BLog _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Time extends Zeze.Transaction.Logs.LogLong {
        public Log__Time(BLog _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BLog)getBelong())._Time = value; }
    }

    private static final class Log__Log extends Zeze.Transaction.Logs.LogString {
        public Log__Log(BLog _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BLog)getBelong())._Log = value; }
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
        _s_.append("Zeze.Builtin.LogService.BLog: {\n");
        _s_.append(_i1_).append("Time=").append(getTime()).append(",\n");
        _s_.append(_i1_).append("Log=").append(getLog()).append('\n');
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
            long _x_ = getTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getLog();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
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
            setTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLog(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLog))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLog)_o_;
        if (getTime() != _b_.getTime())
            return false;
        if (!getLog().equals(_b_.getLog()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getTime() < 0)
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
                case 1: _Time = _v_.longValue(); break;
                case 2: _Log = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTime(_r_.getLong(_pn_ + "Time"));
        setLog(_r_.getString(_pn_ + "Log"));
        if (getLog() == null)
            setLog("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "Time", getTime());
        _s_.appendString(_pn_ + "Log", getLog());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Time", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Log", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 3900400357954919579L;

    private long _Time;
    private String _Log;

    public long getTime() {
        return _Time;
    }

    public void setTime(long _v_) {
        _Time = _v_;
    }

    public String getLog() {
        return _Log;
    }

    public void setLog(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Log = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Log = "";
    }

    @SuppressWarnings("deprecation")
    public Data(long _Time_, String _Log_) {
        _Time = _Time_;
        if (_Log_ == null)
            _Log_ = "";
        _Log = _Log_;
    }

    @Override
    public void reset() {
        _Time = 0;
        _Log = "";
    }

    @Override
    public Zeze.Builtin.LogService.BLog toBean() {
        var _b_ = new Zeze.Builtin.LogService.BLog();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BLog)_o_);
    }

    public void assign(BLog _o_) {
        _Time = _o_.getTime();
        _Log = _o_.getLog();
    }

    public void assign(BLog.Data _o_) {
        _Time = _o_._Time;
        _Log = _o_._Log;
    }

    @Override
    public BLog.Data copy() {
        var _c_ = new BLog.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLog.Data _a_, BLog.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLog.Data clone() {
        return (BLog.Data)super.clone();
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
        _s_.append("Zeze.Builtin.LogService.BLog: {\n");
        _s_.append(_i1_).append("Time=").append(_Time).append(",\n");
        _s_.append(_i1_).append("Log=").append(_Log).append('\n');
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
            long _x_ = _Time;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = _Log;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Time = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Log = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
