// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BBroadcast extends Zeze.Transaction.Bean implements BBroadcastReadOnly {
    public static final long TYPEID = -6926497733546172658L;

    private long _protocolType;
    private Zeze.Net.Binary _protocolWholeData; // 完整的协议打包，包括了 type, size
    private int _time;
    private boolean _onlySameVersion; // 是否仅广播给匹配该provider版本的客户端

    @Override
    public long getProtocolType() {
        if (!isManaged())
            return _protocolType;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _protocolType;
        var log = (Log__protocolType)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _protocolType;
    }

    public void setProtocolType(long _v_) {
        if (!isManaged()) {
            _protocolType = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__protocolType(this, 1, _v_));
    }

    @Override
    public Zeze.Net.Binary getProtocolWholeData() {
        if (!isManaged())
            return _protocolWholeData;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _protocolWholeData;
        var log = (Log__protocolWholeData)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _protocolWholeData;
    }

    public void setProtocolWholeData(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _protocolWholeData = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__protocolWholeData(this, 2, _v_));
    }

    @Override
    public int getTime() {
        if (!isManaged())
            return _time;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _time;
        var log = (Log__time)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _time;
    }

    public void setTime(int _v_) {
        if (!isManaged()) {
            _time = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__time(this, 3, _v_));
    }

    @Override
    public boolean isOnlySameVersion() {
        if (!isManaged())
            return _onlySameVersion;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _onlySameVersion;
        var log = (Log__onlySameVersion)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _onlySameVersion;
    }

    public void setOnlySameVersion(boolean _v_) {
        if (!isManaged()) {
            _onlySameVersion = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__onlySameVersion(this, 4, _v_));
    }

    @SuppressWarnings("deprecation")
    public BBroadcast() {
        _protocolWholeData = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BBroadcast(long _protocolType_, Zeze.Net.Binary _protocolWholeData_, int _time_, boolean _onlySameVersion_) {
        _protocolType = _protocolType_;
        if (_protocolWholeData_ == null)
            _protocolWholeData_ = Zeze.Net.Binary.Empty;
        _protocolWholeData = _protocolWholeData_;
        _time = _time_;
        _onlySameVersion = _onlySameVersion_;
    }

    @Override
    public void reset() {
        setProtocolType(0);
        setProtocolWholeData(Zeze.Net.Binary.Empty);
        setTime(0);
        setOnlySameVersion(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BBroadcast.Data toData() {
        var _d_ = new Zeze.Builtin.Provider.BBroadcast.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Provider.BBroadcast.Data)_o_);
    }

    public void assign(BBroadcast.Data _o_) {
        setProtocolType(_o_._protocolType);
        setProtocolWholeData(_o_._protocolWholeData);
        setTime(_o_._time);
        setOnlySameVersion(_o_._onlySameVersion);
        _unknown_ = null;
    }

    public void assign(BBroadcast _o_) {
        setProtocolType(_o_.getProtocolType());
        setProtocolWholeData(_o_.getProtocolWholeData());
        setTime(_o_.getTime());
        setOnlySameVersion(_o_.isOnlySameVersion());
        _unknown_ = _o_._unknown_;
    }

    public BBroadcast copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBroadcast copy() {
        var _c_ = new BBroadcast();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBroadcast _a_, BBroadcast _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__protocolType extends Zeze.Transaction.Logs.LogLong {
        public Log__protocolType(BBroadcast _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BBroadcast)getBelong())._protocolType = value; }
    }

    private static final class Log__protocolWholeData extends Zeze.Transaction.Logs.LogBinary {
        public Log__protocolWholeData(BBroadcast _b_, int _i_, Zeze.Net.Binary _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BBroadcast)getBelong())._protocolWholeData = value; }
    }

    private static final class Log__time extends Zeze.Transaction.Logs.LogInt {
        public Log__time(BBroadcast _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BBroadcast)getBelong())._time = value; }
    }

    private static final class Log__onlySameVersion extends Zeze.Transaction.Logs.LogBool {
        public Log__onlySameVersion(BBroadcast _b_, int _i_, boolean _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BBroadcast)getBelong())._onlySameVersion = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Provider.BBroadcast: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("protocolType=").append(getProtocolType()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("protocolWholeData=").append(getProtocolWholeData()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("time=").append(getTime()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("onlySameVersion=").append(isOnlySameVersion()).append(System.lineSeparator());
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
            long _x_ = getProtocolType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getProtocolWholeData();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = getTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = isOnlySameVersion();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            setProtocolType(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setProtocolWholeData(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setTime(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setOnlySameVersion(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BBroadcast))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBroadcast)_o_;
        if (getProtocolType() != _b_.getProtocolType())
            return false;
        if (!getProtocolWholeData().equals(_b_.getProtocolWholeData()))
            return false;
        if (getTime() != _b_.getTime())
            return false;
        if (isOnlySameVersion() != _b_.isOnlySameVersion())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getProtocolType() < 0)
            return true;
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
                case 1: _protocolType = _v_.longValue(); break;
                case 2: _protocolWholeData = _v_.binaryValue(); break;
                case 3: _time = _v_.intValue(); break;
                case 4: _onlySameVersion = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setProtocolType(_r_.getLong(_pn_ + "protocolType"));
        setProtocolWholeData(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "protocolWholeData")));
        setTime(_r_.getInt(_pn_ + "time"));
        setOnlySameVersion(_r_.getBoolean(_pn_ + "onlySameVersion"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "protocolType", getProtocolType());
        _s_.appendBinary(_pn_ + "protocolWholeData", getProtocolWholeData());
        _s_.appendInt(_pn_ + "time", getTime());
        _s_.appendBoolean(_pn_ + "onlySameVersion", isOnlySameVersion());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "protocolType", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "protocolWholeData", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "time", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "onlySameVersion", "bool", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6926497733546172658L;

    private long _protocolType;
    private Zeze.Net.Binary _protocolWholeData; // 完整的协议打包，包括了 type, size
    private int _time;
    private boolean _onlySameVersion; // 是否仅广播给匹配该provider版本的客户端

    public long getProtocolType() {
        return _protocolType;
    }

    public void setProtocolType(long _v_) {
        _protocolType = _v_;
    }

    public Zeze.Net.Binary getProtocolWholeData() {
        return _protocolWholeData;
    }

    public void setProtocolWholeData(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _protocolWholeData = _v_;
    }

    public int getTime() {
        return _time;
    }

    public void setTime(int _v_) {
        _time = _v_;
    }

    public boolean isOnlySameVersion() {
        return _onlySameVersion;
    }

    public void setOnlySameVersion(boolean _v_) {
        _onlySameVersion = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _protocolWholeData = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(long _protocolType_, Zeze.Net.Binary _protocolWholeData_, int _time_, boolean _onlySameVersion_) {
        _protocolType = _protocolType_;
        if (_protocolWholeData_ == null)
            _protocolWholeData_ = Zeze.Net.Binary.Empty;
        _protocolWholeData = _protocolWholeData_;
        _time = _time_;
        _onlySameVersion = _onlySameVersion_;
    }

    @Override
    public void reset() {
        _protocolType = 0;
        _protocolWholeData = Zeze.Net.Binary.Empty;
        _time = 0;
        _onlySameVersion = false;
    }

    @Override
    public Zeze.Builtin.Provider.BBroadcast toBean() {
        var _b_ = new Zeze.Builtin.Provider.BBroadcast();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BBroadcast)_o_);
    }

    public void assign(BBroadcast _o_) {
        _protocolType = _o_.getProtocolType();
        _protocolWholeData = _o_.getProtocolWholeData();
        _time = _o_.getTime();
        _onlySameVersion = _o_.isOnlySameVersion();
    }

    public void assign(BBroadcast.Data _o_) {
        _protocolType = _o_._protocolType;
        _protocolWholeData = _o_._protocolWholeData;
        _time = _o_._time;
        _onlySameVersion = _o_._onlySameVersion;
    }

    @Override
    public BBroadcast.Data copy() {
        var _c_ = new BBroadcast.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBroadcast.Data _a_, BBroadcast.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BBroadcast.Data clone() {
        return (BBroadcast.Data)super.clone();
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Provider.BBroadcast: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("protocolType=").append(_protocolType).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("protocolWholeData=").append(_protocolWholeData).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("time=").append(_time).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("onlySameVersion=").append(_onlySameVersion).append(System.lineSeparator());
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
            long _x_ = _protocolType;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _protocolWholeData;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = _time;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = _onlySameVersion;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _protocolType = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _protocolWholeData = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _time = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _onlySameVersion = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
