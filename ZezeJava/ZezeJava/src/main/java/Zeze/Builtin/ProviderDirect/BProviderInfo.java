// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BProviderInfo extends Zeze.Transaction.Bean implements BProviderInfoReadOnly {
    public static final long TYPEID = 858135112612157161L;

    private String _Ip;
    private int _Port;
    private int _ServerId;

    @Override
    public String getIp() {
        if (!isManaged())
            return _Ip;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Ip;
        var log = (Log__Ip)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Ip;
    }

    public void setIp(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Ip = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Ip(this, 1, _v_));
    }

    @Override
    public int getPort() {
        if (!isManaged())
            return _Port;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Port;
        var log = (Log__Port)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Port;
    }

    public void setPort(int _v_) {
        if (!isManaged()) {
            _Port = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Port(this, 2, _v_));
    }

    @Override
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServerId;
        var log = (Log__ServerId)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int _v_) {
        if (!isManaged()) {
            _ServerId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ServerId(this, 3, _v_));
    }

    @SuppressWarnings("deprecation")
    public BProviderInfo() {
        _Ip = "";
    }

    @SuppressWarnings("deprecation")
    public BProviderInfo(String _Ip_, int _Port_, int _ServerId_) {
        if (_Ip_ == null)
            _Ip_ = "";
        _Ip = _Ip_;
        _Port = _Port_;
        _ServerId = _ServerId_;
    }

    @Override
    public void reset() {
        setIp("");
        setPort(0);
        setServerId(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BProviderInfo.Data toData() {
        var _d_ = new Zeze.Builtin.ProviderDirect.BProviderInfo.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.ProviderDirect.BProviderInfo.Data)_o_);
    }

    public void assign(BProviderInfo.Data _o_) {
        setIp(_o_._Ip);
        setPort(_o_._Port);
        setServerId(_o_._ServerId);
        _unknown_ = null;
    }

    public void assign(BProviderInfo _o_) {
        setIp(_o_.getIp());
        setPort(_o_.getPort());
        setServerId(_o_.getServerId());
        _unknown_ = _o_._unknown_;
    }

    public BProviderInfo copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BProviderInfo copy() {
        var _c_ = new BProviderInfo();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BProviderInfo _a_, BProviderInfo _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Ip extends Zeze.Transaction.Logs.LogString {
        public Log__Ip(BProviderInfo _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BProviderInfo)getBelong())._Ip = value; }
    }

    private static final class Log__Port extends Zeze.Transaction.Logs.LogInt {
        public Log__Port(BProviderInfo _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BProviderInfo)getBelong())._Port = value; }
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BProviderInfo _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BProviderInfo)getBelong())._ServerId = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.ProviderDirect.BProviderInfo: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Ip=").append(getIp()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Port=").append(getPort()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("ServerId=").append(getServerId()).append(System.lineSeparator());
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
            String _x_ = getIp();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getPort();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            setIp(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPort(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BProviderInfo))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BProviderInfo)_o_;
        if (!getIp().equals(_b_.getIp()))
            return false;
        if (getPort() != _b_.getPort())
            return false;
        if (getServerId() != _b_.getServerId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getPort() < 0)
            return true;
        if (getServerId() < 0)
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
                case 1: _Ip = _v_.stringValue(); break;
                case 2: _Port = _v_.intValue(); break;
                case 3: _ServerId = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setIp(_r_.getString(_pn_ + "Ip"));
        if (getIp() == null)
            setIp("");
        setPort(_r_.getInt(_pn_ + "Port"));
        setServerId(_r_.getInt(_pn_ + "ServerId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Ip", getIp());
        _s_.appendInt(_pn_ + "Port", getPort());
        _s_.appendInt(_pn_ + "ServerId", getServerId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Ip", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Port", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ServerId", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 858135112612157161L;

    private String _Ip;
    private int _Port;
    private int _ServerId;

    public String getIp() {
        return _Ip;
    }

    public void setIp(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Ip = _v_;
    }

    public int getPort() {
        return _Port;
    }

    public void setPort(int _v_) {
        _Port = _v_;
    }

    public int getServerId() {
        return _ServerId;
    }

    public void setServerId(int _v_) {
        _ServerId = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Ip = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _Ip_, int _Port_, int _ServerId_) {
        if (_Ip_ == null)
            _Ip_ = "";
        _Ip = _Ip_;
        _Port = _Port_;
        _ServerId = _ServerId_;
    }

    @Override
    public void reset() {
        _Ip = "";
        _Port = 0;
        _ServerId = 0;
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BProviderInfo toBean() {
        var _b_ = new Zeze.Builtin.ProviderDirect.BProviderInfo();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BProviderInfo)_o_);
    }

    public void assign(BProviderInfo _o_) {
        _Ip = _o_.getIp();
        _Port = _o_.getPort();
        _ServerId = _o_.getServerId();
    }

    public void assign(BProviderInfo.Data _o_) {
        _Ip = _o_._Ip;
        _Port = _o_._Port;
        _ServerId = _o_._ServerId;
    }

    @Override
    public BProviderInfo.Data copy() {
        var _c_ = new BProviderInfo.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BProviderInfo.Data _a_, BProviderInfo.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BProviderInfo.Data clone() {
        return (BProviderInfo.Data)super.clone();
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.ProviderDirect.BProviderInfo: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Ip=").append(_Ip).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Port=").append(_Port).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("ServerId=").append(_ServerId).append(System.lineSeparator());
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
            String _x_ = _Ip;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = _Port;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _ServerId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Ip = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Port = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _ServerId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
