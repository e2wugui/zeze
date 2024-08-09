// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BRegister extends Zeze.Transaction.Bean implements BRegisterReadOnly {
    public static final long TYPEID = -3200018963971290421L;

    private String _Dbh2RaftAcceptorName;
    private int _Port;
    private int _BucketCount;

    private static final java.lang.invoke.VarHandle vh_Dbh2RaftAcceptorName;
    private static final java.lang.invoke.VarHandle vh_Port;
    private static final java.lang.invoke.VarHandle vh_BucketCount;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Dbh2RaftAcceptorName = _l_.findVarHandle(BRegister.class, "_Dbh2RaftAcceptorName", String.class);
            vh_Port = _l_.findVarHandle(BRegister.class, "_Port", int.class);
            vh_BucketCount = _l_.findVarHandle(BRegister.class, "_BucketCount", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getDbh2RaftAcceptorName() {
        if (!isManaged())
            return _Dbh2RaftAcceptorName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Dbh2RaftAcceptorName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Dbh2RaftAcceptorName;
    }

    public void setDbh2RaftAcceptorName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Dbh2RaftAcceptorName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_Dbh2RaftAcceptorName, _v_));
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
    public int getBucketCount() {
        if (!isManaged())
            return _BucketCount;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _BucketCount;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _BucketCount;
    }

    public void setBucketCount(int _v_) {
        if (!isManaged()) {
            _BucketCount = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_BucketCount, _v_));
    }

    @SuppressWarnings("deprecation")
    public BRegister() {
        _Dbh2RaftAcceptorName = "";
    }

    @SuppressWarnings("deprecation")
    public BRegister(String _Dbh2RaftAcceptorName_, int _Port_, int _BucketCount_) {
        if (_Dbh2RaftAcceptorName_ == null)
            _Dbh2RaftAcceptorName_ = "";
        _Dbh2RaftAcceptorName = _Dbh2RaftAcceptorName_;
        _Port = _Port_;
        _BucketCount = _BucketCount_;
    }

    @Override
    public void reset() {
        setDbh2RaftAcceptorName("");
        setPort(0);
        setBucketCount(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BRegister.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.Master.BRegister.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.Master.BRegister.Data)_o_);
    }

    public void assign(BRegister.Data _o_) {
        setDbh2RaftAcceptorName(_o_._Dbh2RaftAcceptorName);
        setPort(_o_._Port);
        setBucketCount(_o_._BucketCount);
        _unknown_ = null;
    }

    public void assign(BRegister _o_) {
        setDbh2RaftAcceptorName(_o_.getDbh2RaftAcceptorName());
        setPort(_o_.getPort());
        setBucketCount(_o_.getBucketCount());
        _unknown_ = _o_._unknown_;
    }

    public BRegister copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRegister copy() {
        var _c_ = new BRegister();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BRegister _a_, BRegister _b_) {
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
        _s_.append("Zeze.Builtin.Dbh2.Master.BRegister: {\n");
        _s_.append(_i1_).append("Dbh2RaftAcceptorName=").append(getDbh2RaftAcceptorName()).append(",\n");
        _s_.append(_i1_).append("Port=").append(getPort()).append(",\n");
        _s_.append(_i1_).append("BucketCount=").append(getBucketCount()).append('\n');
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
            String _x_ = getDbh2RaftAcceptorName();
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
            int _x_ = getBucketCount();
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
            setDbh2RaftAcceptorName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPort(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setBucketCount(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BRegister))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BRegister)_o_;
        if (!getDbh2RaftAcceptorName().equals(_b_.getDbh2RaftAcceptorName()))
            return false;
        if (getPort() != _b_.getPort())
            return false;
        if (getBucketCount() != _b_.getBucketCount())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getPort() < 0)
            return true;
        if (getBucketCount() < 0)
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
                case 1: _Dbh2RaftAcceptorName = _v_.stringValue(); break;
                case 2: _Port = _v_.intValue(); break;
                case 3: _BucketCount = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setDbh2RaftAcceptorName(_r_.getString(_pn_ + "Dbh2RaftAcceptorName"));
        if (getDbh2RaftAcceptorName() == null)
            setDbh2RaftAcceptorName("");
        setPort(_r_.getInt(_pn_ + "Port"));
        setBucketCount(_r_.getInt(_pn_ + "BucketCount"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Dbh2RaftAcceptorName", getDbh2RaftAcceptorName());
        _s_.appendInt(_pn_ + "Port", getPort());
        _s_.appendInt(_pn_ + "BucketCount", getBucketCount());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Dbh2RaftAcceptorName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Port", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "BucketCount", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -3200018963971290421L;

    private String _Dbh2RaftAcceptorName;
    private int _Port;
    private int _BucketCount;

    public String getDbh2RaftAcceptorName() {
        return _Dbh2RaftAcceptorName;
    }

    public void setDbh2RaftAcceptorName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Dbh2RaftAcceptorName = _v_;
    }

    public int getPort() {
        return _Port;
    }

    public void setPort(int _v_) {
        _Port = _v_;
    }

    public int getBucketCount() {
        return _BucketCount;
    }

    public void setBucketCount(int _v_) {
        _BucketCount = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Dbh2RaftAcceptorName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _Dbh2RaftAcceptorName_, int _Port_, int _BucketCount_) {
        if (_Dbh2RaftAcceptorName_ == null)
            _Dbh2RaftAcceptorName_ = "";
        _Dbh2RaftAcceptorName = _Dbh2RaftAcceptorName_;
        _Port = _Port_;
        _BucketCount = _BucketCount_;
    }

    @Override
    public void reset() {
        _Dbh2RaftAcceptorName = "";
        _Port = 0;
        _BucketCount = 0;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BRegister toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.Master.BRegister();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BRegister)_o_);
    }

    public void assign(BRegister _o_) {
        _Dbh2RaftAcceptorName = _o_.getDbh2RaftAcceptorName();
        _Port = _o_.getPort();
        _BucketCount = _o_.getBucketCount();
    }

    public void assign(BRegister.Data _o_) {
        _Dbh2RaftAcceptorName = _o_._Dbh2RaftAcceptorName;
        _Port = _o_._Port;
        _BucketCount = _o_._BucketCount;
    }

    @Override
    public BRegister.Data copy() {
        var _c_ = new BRegister.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BRegister.Data _a_, BRegister.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BRegister.Data clone() {
        return (BRegister.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Dbh2.Master.BRegister: {\n");
        _s_.append(_i1_).append("Dbh2RaftAcceptorName=").append(_Dbh2RaftAcceptorName).append(",\n");
        _s_.append(_i1_).append("Port=").append(_Port).append(",\n");
        _s_.append(_i1_).append("BucketCount=").append(_BucketCount).append('\n');
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
            String _x_ = _Dbh2RaftAcceptorName;
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
            int _x_ = _BucketCount;
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
            _Dbh2RaftAcceptorName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Port = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _BucketCount = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
