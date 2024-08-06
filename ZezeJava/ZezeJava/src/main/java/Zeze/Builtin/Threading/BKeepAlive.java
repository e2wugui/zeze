// auto-generated @formatter:off
package Zeze.Builtin.Threading;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BKeepAlive extends Zeze.Transaction.Bean implements BKeepAliveReadOnly {
    public static final long TYPEID = -6747942781414109078L;

    private int _ServerId;
    private long _AppSerialId;

    @Override
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServerId;
        var log = (Log__ServerId)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int _v_) {
        if (!isManaged()) {
            _ServerId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ServerId(this, 1, _v_));
    }

    @Override
    public long getAppSerialId() {
        if (!isManaged())
            return _AppSerialId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _AppSerialId;
        var log = (Log__AppSerialId)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _AppSerialId;
    }

    public void setAppSerialId(long _v_) {
        if (!isManaged()) {
            _AppSerialId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__AppSerialId(this, 2, _v_));
    }

    @SuppressWarnings("deprecation")
    public BKeepAlive() {
    }

    @SuppressWarnings("deprecation")
    public BKeepAlive(int _ServerId_, long _AppSerialId_) {
        _ServerId = _ServerId_;
        _AppSerialId = _AppSerialId_;
    }

    @Override
    public void reset() {
        setServerId(0);
        setAppSerialId(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Threading.BKeepAlive.Data toData() {
        var _d_ = new Zeze.Builtin.Threading.BKeepAlive.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Threading.BKeepAlive.Data)_o_);
    }

    public void assign(BKeepAlive.Data _o_) {
        setServerId(_o_._ServerId);
        setAppSerialId(_o_._AppSerialId);
        _unknown_ = null;
    }

    public void assign(BKeepAlive _o_) {
        setServerId(_o_.getServerId());
        setAppSerialId(_o_.getAppSerialId());
        _unknown_ = _o_._unknown_;
    }

    public BKeepAlive copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BKeepAlive copy() {
        var _c_ = new BKeepAlive();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BKeepAlive _a_, BKeepAlive _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BKeepAlive _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BKeepAlive)getBelong())._ServerId = value; }
    }

    private static final class Log__AppSerialId extends Zeze.Transaction.Logs.LogLong {
        public Log__AppSerialId(BKeepAlive _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BKeepAlive)getBelong())._AppSerialId = value; }
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
        _s_.append("Zeze.Builtin.Threading.BKeepAlive: {\n");
        _s_.append(_i1_).append("ServerId=").append(getServerId()).append(",\n");
        _s_.append(_i1_).append("AppSerialId=").append(getAppSerialId()).append('\n');
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
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getAppSerialId();
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
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setAppSerialId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BKeepAlive))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BKeepAlive)_o_;
        if (getServerId() != _b_.getServerId())
            return false;
        if (getAppSerialId() != _b_.getAppSerialId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getServerId() < 0)
            return true;
        if (getAppSerialId() < 0)
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
                case 1: _ServerId = _v_.intValue(); break;
                case 2: _AppSerialId = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setServerId(_r_.getInt(_pn_ + "ServerId"));
        setAppSerialId(_r_.getLong(_pn_ + "AppSerialId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "ServerId", getServerId());
        _s_.appendLong(_pn_ + "AppSerialId", getAppSerialId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServerId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "AppSerialId", "long", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6747942781414109078L;

    private int _ServerId;
    private long _AppSerialId;

    public int getServerId() {
        return _ServerId;
    }

    public void setServerId(int _v_) {
        _ServerId = _v_;
    }

    public long getAppSerialId() {
        return _AppSerialId;
    }

    public void setAppSerialId(long _v_) {
        _AppSerialId = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(int _ServerId_, long _AppSerialId_) {
        _ServerId = _ServerId_;
        _AppSerialId = _AppSerialId_;
    }

    @Override
    public void reset() {
        _ServerId = 0;
        _AppSerialId = 0;
    }

    @Override
    public Zeze.Builtin.Threading.BKeepAlive toBean() {
        var _b_ = new Zeze.Builtin.Threading.BKeepAlive();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BKeepAlive)_o_);
    }

    public void assign(BKeepAlive _o_) {
        _ServerId = _o_.getServerId();
        _AppSerialId = _o_.getAppSerialId();
    }

    public void assign(BKeepAlive.Data _o_) {
        _ServerId = _o_._ServerId;
        _AppSerialId = _o_._AppSerialId;
    }

    @Override
    public BKeepAlive.Data copy() {
        var _c_ = new BKeepAlive.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BKeepAlive.Data _a_, BKeepAlive.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BKeepAlive.Data clone() {
        return (BKeepAlive.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Threading.BKeepAlive: {\n");
        _s_.append(_i1_).append("ServerId=").append(_ServerId).append(",\n");
        _s_.append(_i1_).append("AppSerialId=").append(_AppSerialId).append('\n');
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
            int _x_ = _ServerId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = _AppSerialId;
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
            _ServerId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _AppSerialId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
