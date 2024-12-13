// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BMQServer extends Zeze.Transaction.Bean implements BMQServerReadOnly {
    public static final long TYPEID = -5289186167189120717L;

    private String _Host;
    private int _Port;
    private int _PartitionIndex;

    private static final java.lang.invoke.VarHandle vh_Host;
    private static final java.lang.invoke.VarHandle vh_Port;
    private static final java.lang.invoke.VarHandle vh_PartitionIndex;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Host = _l_.findVarHandle(BMQServer.class, "_Host", String.class);
            vh_Port = _l_.findVarHandle(BMQServer.class, "_Port", int.class);
            vh_PartitionIndex = _l_.findVarHandle(BMQServer.class, "_PartitionIndex", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getHost() {
        if (!isManaged())
            return _Host;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Host;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Host;
    }

    public void setHost(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Host = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_Host, _v_));
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
    public int getPartitionIndex() {
        if (!isManaged())
            return _PartitionIndex;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _PartitionIndex;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _PartitionIndex;
    }

    public void setPartitionIndex(int _v_) {
        if (!isManaged()) {
            _PartitionIndex = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_PartitionIndex, _v_));
    }

    @SuppressWarnings("deprecation")
    public BMQServer() {
        _Host = "";
    }

    @SuppressWarnings("deprecation")
    public BMQServer(String _Host_, int _Port_, int _PartitionIndex_) {
        if (_Host_ == null)
            _Host_ = "";
        _Host = _Host_;
        _Port = _Port_;
        _PartitionIndex = _PartitionIndex_;
    }

    @Override
    public void reset() {
        setHost("");
        setPort(0);
        setPartitionIndex(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.MQ.Master.BMQServer.Data toData() {
        var _d_ = new Zeze.Builtin.MQ.Master.BMQServer.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.MQ.Master.BMQServer.Data)_o_);
    }

    public void assign(BMQServer.Data _o_) {
        setHost(_o_._Host);
        setPort(_o_._Port);
        setPartitionIndex(_o_._PartitionIndex);
        _unknown_ = null;
    }

    public void assign(BMQServer _o_) {
        setHost(_o_.getHost());
        setPort(_o_.getPort());
        setPartitionIndex(_o_.getPartitionIndex());
        _unknown_ = _o_._unknown_;
    }

    public BMQServer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BMQServer copy() {
        var _c_ = new BMQServer();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMQServer _a_, BMQServer _b_) {
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
        _s_.append("Zeze.Builtin.MQ.Master.BMQServer: {\n");
        _s_.append(_i1_).append("Host=").append(getHost()).append(",\n");
        _s_.append(_i1_).append("Port=").append(getPort()).append(",\n");
        _s_.append(_i1_).append("PartitionIndex=").append(getPartitionIndex()).append('\n');
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
            String _x_ = getHost();
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
            int _x_ = getPartitionIndex();
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
            setHost(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPort(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setPartitionIndex(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BMQServer))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMQServer)_o_;
        if (!getHost().equals(_b_.getHost()))
            return false;
        if (getPort() != _b_.getPort())
            return false;
        if (getPartitionIndex() != _b_.getPartitionIndex())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getPort() < 0)
            return true;
        if (getPartitionIndex() < 0)
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
                case 1: _Host = _v_.stringValue(); break;
                case 2: _Port = _v_.intValue(); break;
                case 3: _PartitionIndex = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setHost(_r_.getString(_pn_ + "Host"));
        if (getHost() == null)
            setHost("");
        setPort(_r_.getInt(_pn_ + "Port"));
        setPartitionIndex(_r_.getInt(_pn_ + "PartitionIndex"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Host", getHost());
        _s_.appendInt(_pn_ + "Port", getPort());
        _s_.appendInt(_pn_ + "PartitionIndex", getPartitionIndex());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Host", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Port", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "PartitionIndex", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5289186167189120717L;

    private String _Host;
    private int _Port;
    private int _PartitionIndex;

    public String getHost() {
        return _Host;
    }

    public void setHost(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Host = _v_;
    }

    public int getPort() {
        return _Port;
    }

    public void setPort(int _v_) {
        _Port = _v_;
    }

    public int getPartitionIndex() {
        return _PartitionIndex;
    }

    public void setPartitionIndex(int _v_) {
        _PartitionIndex = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Host = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _Host_, int _Port_, int _PartitionIndex_) {
        if (_Host_ == null)
            _Host_ = "";
        _Host = _Host_;
        _Port = _Port_;
        _PartitionIndex = _PartitionIndex_;
    }

    @Override
    public void reset() {
        _Host = "";
        _Port = 0;
        _PartitionIndex = 0;
    }

    @Override
    public Zeze.Builtin.MQ.Master.BMQServer toBean() {
        var _b_ = new Zeze.Builtin.MQ.Master.BMQServer();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BMQServer)_o_);
    }

    public void assign(BMQServer _o_) {
        _Host = _o_.getHost();
        _Port = _o_.getPort();
        _PartitionIndex = _o_.getPartitionIndex();
    }

    public void assign(BMQServer.Data _o_) {
        _Host = _o_._Host;
        _Port = _o_._Port;
        _PartitionIndex = _o_._PartitionIndex;
    }

    @Override
    public BMQServer.Data copy() {
        var _c_ = new BMQServer.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMQServer.Data _a_, BMQServer.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BMQServer.Data clone() {
        return (BMQServer.Data)super.clone();
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
        _s_.append("Zeze.Builtin.MQ.Master.BMQServer: {\n");
        _s_.append(_i1_).append("Host=").append(_Host).append(",\n");
        _s_.append(_i1_).append("Port=").append(_Port).append(",\n");
        _s_.append(_i1_).append("PartitionIndex=").append(_PartitionIndex).append('\n');
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
            String _x_ = _Host;
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
            int _x_ = _PartitionIndex;
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
            _Host = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Port = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _PartitionIndex = _o_.ReadInt(_t_);
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
        if (!(_o_ instanceof BMQServer.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMQServer.Data)_o_;
        if (!_Host.equals(_b_._Host))
            return false;
        if (_Port != _b_._Port)
            return false;
        if (_PartitionIndex != _b_._PartitionIndex)
            return false;
        return true;
    }
}
}
