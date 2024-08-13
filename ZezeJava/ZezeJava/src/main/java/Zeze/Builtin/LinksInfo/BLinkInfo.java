// auto-generated @formatter:off
package Zeze.Builtin.LinksInfo;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BLinkInfo extends Zeze.Transaction.Bean implements BLinkInfoReadOnly {
    public static final long TYPEID = -4351959562089154457L;

    private String _Ip;
    private int _Port;
    private Zeze.Net.Binary _Extra;

    private static final java.lang.invoke.VarHandle vh_Ip;
    private static final java.lang.invoke.VarHandle vh_Port;
    private static final java.lang.invoke.VarHandle vh_Extra;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Ip = _l_.findVarHandle(BLinkInfo.class, "_Ip", String.class);
            vh_Port = _l_.findVarHandle(BLinkInfo.class, "_Port", int.class);
            vh_Extra = _l_.findVarHandle(BLinkInfo.class, "_Extra", Zeze.Net.Binary.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getIp() {
        if (!isManaged())
            return _Ip;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Ip;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_Ip, _v_));
    }

    @Override
    public int getPort() {
        if (!isManaged())
            return _Port;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Port;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Port;
    }

    public void setPort(int _v_) {
        if (!isManaged()) {
            _Port = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_Port, _v_));
    }

    @Override
    public Zeze.Net.Binary getExtra() {
        if (!isManaged())
            return _Extra;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Extra;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Extra;
    }

    public void setExtra(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Extra = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 3, vh_Extra, _v_));
    }

    @SuppressWarnings("deprecation")
    public BLinkInfo() {
        _Ip = "";
        _Extra = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BLinkInfo(String _Ip_, int _Port_, Zeze.Net.Binary _Extra_) {
        if (_Ip_ == null)
            _Ip_ = "";
        _Ip = _Ip_;
        _Port = _Port_;
        if (_Extra_ == null)
            _Extra_ = Zeze.Net.Binary.Empty;
        _Extra = _Extra_;
    }

    @Override
    public void reset() {
        setIp("");
        setPort(0);
        setExtra(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LinksInfo.BLinkInfo.Data toData() {
        var _d_ = new Zeze.Builtin.LinksInfo.BLinkInfo.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.LinksInfo.BLinkInfo.Data)_o_);
    }

    public void assign(BLinkInfo.Data _o_) {
        setIp(_o_._Ip);
        setPort(_o_._Port);
        setExtra(_o_._Extra);
        _unknown_ = null;
    }

    public void assign(BLinkInfo _o_) {
        setIp(_o_.getIp());
        setPort(_o_.getPort());
        setExtra(_o_.getExtra());
        _unknown_ = _o_._unknown_;
    }

    public BLinkInfo copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLinkInfo copy() {
        var _c_ = new BLinkInfo();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLinkInfo _a_, BLinkInfo _b_) {
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
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.LinksInfo.BLinkInfo: {\n");
        _s_.append(_i1_).append("Ip=").append(getIp()).append(",\n");
        _s_.append(_i1_).append("Port=").append(getPort()).append(",\n");
        _s_.append(_i1_).append("Extra=").append(getExtra()).append('\n');
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
            var _x_ = getExtra();
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
            setIp(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPort(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setExtra(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLinkInfo))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLinkInfo)_o_;
        if (!getIp().equals(_b_.getIp()))
            return false;
        if (getPort() != _b_.getPort())
            return false;
        if (!getExtra().equals(_b_.getExtra()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getPort() < 0)
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
                case 3: _Extra = _v_.binaryValue(); break;
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
        setExtra(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Extra")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Ip", getIp());
        _s_.appendInt(_pn_ + "Port", getPort());
        _s_.appendBinary(_pn_ + "Extra", getExtra());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Ip", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Port", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Extra", "binary", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4351959562089154457L;

    private String _Ip;
    private int _Port;
    private Zeze.Net.Binary _Extra;

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

    public Zeze.Net.Binary getExtra() {
        return _Extra;
    }

    public void setExtra(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Extra = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Ip = "";
        _Extra = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(String _Ip_, int _Port_, Zeze.Net.Binary _Extra_) {
        if (_Ip_ == null)
            _Ip_ = "";
        _Ip = _Ip_;
        _Port = _Port_;
        if (_Extra_ == null)
            _Extra_ = Zeze.Net.Binary.Empty;
        _Extra = _Extra_;
    }

    @Override
    public void reset() {
        _Ip = "";
        _Port = 0;
        _Extra = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.LinksInfo.BLinkInfo toBean() {
        var _b_ = new Zeze.Builtin.LinksInfo.BLinkInfo();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BLinkInfo)_o_);
    }

    public void assign(BLinkInfo _o_) {
        _Ip = _o_.getIp();
        _Port = _o_.getPort();
        _Extra = _o_.getExtra();
    }

    public void assign(BLinkInfo.Data _o_) {
        _Ip = _o_._Ip;
        _Port = _o_._Port;
        _Extra = _o_._Extra;
    }

    @Override
    public BLinkInfo.Data copy() {
        var _c_ = new BLinkInfo.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLinkInfo.Data _a_, BLinkInfo.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLinkInfo.Data clone() {
        return (BLinkInfo.Data)super.clone();
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
        _s_.append("Zeze.Builtin.LinksInfo.BLinkInfo: {\n");
        _s_.append(_i1_).append("Ip=").append(_Ip).append(",\n");
        _s_.append(_i1_).append("Port=").append(_Port).append(",\n");
        _s_.append(_i1_).append("Extra=").append(_Extra).append('\n');
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
            var _x_ = _Extra;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
            _Extra = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLinkInfo.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLinkInfo.Data)_o_;
        if (!_Ip.equals(_b_._Ip))
            return false;
        if (_Port != _b_._Port)
            return false;
        if (!_Extra.equals(_b_._Extra))
            return false;
        return true;
    }
}
}
