// auto-generated @formatter:off
package Zeze.Builtin.Token;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTokenStatus extends Zeze.Transaction.Bean implements BTokenStatusReadOnly {
    public static final long TYPEID = 3897751576670303080L;

    private long _newCount; // 已分配的token数量
    private long _curCount; // 当前有效的token数量
    private int _connectCount; // 当前的网络连接数量
    private String _perfLog; // 最近生成的性能日志

    @Override
    public long getNewCount() {
        if (!isManaged())
            return _newCount;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _newCount;
        var log = (Log__newCount)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _newCount;
    }

    public void setNewCount(long _v_) {
        if (!isManaged()) {
            _newCount = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__newCount(this, 1, _v_));
    }

    @Override
    public long getCurCount() {
        if (!isManaged())
            return _curCount;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _curCount;
        var log = (Log__curCount)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _curCount;
    }

    public void setCurCount(long _v_) {
        if (!isManaged()) {
            _curCount = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__curCount(this, 2, _v_));
    }

    @Override
    public int getConnectCount() {
        if (!isManaged())
            return _connectCount;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _connectCount;
        var log = (Log__connectCount)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _connectCount;
    }

    public void setConnectCount(int _v_) {
        if (!isManaged()) {
            _connectCount = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__connectCount(this, 3, _v_));
    }

    @Override
    public String getPerfLog() {
        if (!isManaged())
            return _perfLog;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _perfLog;
        var log = (Log__perfLog)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _perfLog;
    }

    public void setPerfLog(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _perfLog = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__perfLog(this, 4, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTokenStatus() {
        _perfLog = "";
    }

    @SuppressWarnings("deprecation")
    public BTokenStatus(long _newCount_, long _curCount_, int _connectCount_, String _perfLog_) {
        _newCount = _newCount_;
        _curCount = _curCount_;
        _connectCount = _connectCount_;
        if (_perfLog_ == null)
            _perfLog_ = "";
        _perfLog = _perfLog_;
    }

    @Override
    public void reset() {
        setNewCount(0);
        setCurCount(0);
        setConnectCount(0);
        setPerfLog("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Token.BTokenStatus.Data toData() {
        var _d_ = new Zeze.Builtin.Token.BTokenStatus.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Token.BTokenStatus.Data)_o_);
    }

    public void assign(BTokenStatus.Data _o_) {
        setNewCount(_o_._newCount);
        setCurCount(_o_._curCount);
        setConnectCount(_o_._connectCount);
        setPerfLog(_o_._perfLog);
        _unknown_ = null;
    }

    public void assign(BTokenStatus _o_) {
        setNewCount(_o_.getNewCount());
        setCurCount(_o_.getCurCount());
        setConnectCount(_o_.getConnectCount());
        setPerfLog(_o_.getPerfLog());
        _unknown_ = _o_._unknown_;
    }

    public BTokenStatus copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTokenStatus copy() {
        var _c_ = new BTokenStatus();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTokenStatus _a_, BTokenStatus _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__newCount extends Zeze.Transaction.Logs.LogLong {
        public Log__newCount(BTokenStatus _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BTokenStatus)getBelong())._newCount = value; }
    }

    private static final class Log__curCount extends Zeze.Transaction.Logs.LogLong {
        public Log__curCount(BTokenStatus _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BTokenStatus)getBelong())._curCount = value; }
    }

    private static final class Log__connectCount extends Zeze.Transaction.Logs.LogInt {
        public Log__connectCount(BTokenStatus _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BTokenStatus)getBelong())._connectCount = value; }
    }

    private static final class Log__perfLog extends Zeze.Transaction.Logs.LogString {
        public Log__perfLog(BTokenStatus _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BTokenStatus)getBelong())._perfLog = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Token.BTokenStatus: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("newCount=").append(getNewCount()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("curCount=").append(getCurCount()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("connectCount=").append(getConnectCount()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("perfLog=").append(getPerfLog()).append(System.lineSeparator());
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
            long _x_ = getNewCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getCurCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getConnectCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getPerfLog();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
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
            setNewCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setCurCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setConnectCount(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setPerfLog(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTokenStatus))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTokenStatus)_o_;
        if (getNewCount() != _b_.getNewCount())
            return false;
        if (getCurCount() != _b_.getCurCount())
            return false;
        if (getConnectCount() != _b_.getConnectCount())
            return false;
        if (!getPerfLog().equals(_b_.getPerfLog()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getNewCount() < 0)
            return true;
        if (getCurCount() < 0)
            return true;
        if (getConnectCount() < 0)
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
                case 1: _newCount = _v_.longValue(); break;
                case 2: _curCount = _v_.longValue(); break;
                case 3: _connectCount = _v_.intValue(); break;
                case 4: _perfLog = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setNewCount(_r_.getLong(_pn_ + "newCount"));
        setCurCount(_r_.getLong(_pn_ + "curCount"));
        setConnectCount(_r_.getInt(_pn_ + "connectCount"));
        setPerfLog(_r_.getString(_pn_ + "perfLog"));
        if (getPerfLog() == null)
            setPerfLog("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "newCount", getNewCount());
        _s_.appendLong(_pn_ + "curCount", getCurCount());
        _s_.appendInt(_pn_ + "connectCount", getConnectCount());
        _s_.appendString(_pn_ + "perfLog", getPerfLog());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "newCount", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "curCount", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "connectCount", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "perfLog", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 3897751576670303080L;

    private long _newCount; // 已分配的token数量
    private long _curCount; // 当前有效的token数量
    private int _connectCount; // 当前的网络连接数量
    private String _perfLog; // 最近生成的性能日志

    public long getNewCount() {
        return _newCount;
    }

    public void setNewCount(long _v_) {
        _newCount = _v_;
    }

    public long getCurCount() {
        return _curCount;
    }

    public void setCurCount(long _v_) {
        _curCount = _v_;
    }

    public int getConnectCount() {
        return _connectCount;
    }

    public void setConnectCount(int _v_) {
        _connectCount = _v_;
    }

    public String getPerfLog() {
        return _perfLog;
    }

    public void setPerfLog(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _perfLog = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _perfLog = "";
    }

    @SuppressWarnings("deprecation")
    public Data(long _newCount_, long _curCount_, int _connectCount_, String _perfLog_) {
        _newCount = _newCount_;
        _curCount = _curCount_;
        _connectCount = _connectCount_;
        if (_perfLog_ == null)
            _perfLog_ = "";
        _perfLog = _perfLog_;
    }

    @Override
    public void reset() {
        _newCount = 0;
        _curCount = 0;
        _connectCount = 0;
        _perfLog = "";
    }

    @Override
    public Zeze.Builtin.Token.BTokenStatus toBean() {
        var _b_ = new Zeze.Builtin.Token.BTokenStatus();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BTokenStatus)_o_);
    }

    public void assign(BTokenStatus _o_) {
        _newCount = _o_.getNewCount();
        _curCount = _o_.getCurCount();
        _connectCount = _o_.getConnectCount();
        _perfLog = _o_.getPerfLog();
    }

    public void assign(BTokenStatus.Data _o_) {
        _newCount = _o_._newCount;
        _curCount = _o_._curCount;
        _connectCount = _o_._connectCount;
        _perfLog = _o_._perfLog;
    }

    @Override
    public BTokenStatus.Data copy() {
        var _c_ = new BTokenStatus.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTokenStatus.Data _a_, BTokenStatus.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BTokenStatus.Data clone() {
        return (BTokenStatus.Data)super.clone();
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Token.BTokenStatus: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("newCount=").append(_newCount).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("curCount=").append(_curCount).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("connectCount=").append(_connectCount).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("perfLog=").append(_perfLog).append(System.lineSeparator());
        _l_ -= 4;
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
            long _x_ = _newCount;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = _curCount;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = _connectCount;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _perfLog;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
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
            _newCount = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _curCount = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _connectCount = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _perfLog = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
