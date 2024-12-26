// auto-generated @formatter:off
package Zeze.Builtin.Onz;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// <enum name="eFlushPeriod" value="3"/>
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BFuncProcedure extends Zeze.Transaction.Bean implements BFuncProcedureReadOnly {
    public static final long TYPEID = 2028535493874213798L;

    private long _OnzTid;
    private String _FuncName;
    private Zeze.Net.Binary _FuncArgument;
    private int _FlushMode;
    private int _FlushTimeout;

    private static final java.lang.invoke.VarHandle vh_OnzTid;
    private static final java.lang.invoke.VarHandle vh_FuncName;
    private static final java.lang.invoke.VarHandle vh_FuncArgument;
    private static final java.lang.invoke.VarHandle vh_FlushMode;
    private static final java.lang.invoke.VarHandle vh_FlushTimeout;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_OnzTid = _l_.findVarHandle(BFuncProcedure.class, "_OnzTid", long.class);
            vh_FuncName = _l_.findVarHandle(BFuncProcedure.class, "_FuncName", String.class);
            vh_FuncArgument = _l_.findVarHandle(BFuncProcedure.class, "_FuncArgument", Zeze.Net.Binary.class);
            vh_FlushMode = _l_.findVarHandle(BFuncProcedure.class, "_FlushMode", int.class);
            vh_FlushTimeout = _l_.findVarHandle(BFuncProcedure.class, "_FlushTimeout", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getOnzTid() {
        if (!isManaged())
            return _OnzTid;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _OnzTid;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _OnzTid;
    }

    public void setOnzTid(long _v_) {
        if (!isManaged()) {
            _OnzTid = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_OnzTid, _v_));
    }

    @Override
    public String getFuncName() {
        if (!isManaged())
            return _FuncName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _FuncName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _FuncName;
    }

    public void setFuncName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _FuncName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_FuncName, _v_));
    }

    @Override
    public Zeze.Net.Binary getFuncArgument() {
        if (!isManaged())
            return _FuncArgument;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _FuncArgument;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _FuncArgument;
    }

    public void setFuncArgument(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _FuncArgument = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 3, vh_FuncArgument, _v_));
    }

    @Override
    public int getFlushMode() {
        if (!isManaged())
            return _FlushMode;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _FlushMode;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _FlushMode;
    }

    public void setFlushMode(int _v_) {
        if (!isManaged()) {
            _FlushMode = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 4, vh_FlushMode, _v_));
    }

    @Override
    public int getFlushTimeout() {
        if (!isManaged())
            return _FlushTimeout;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _FlushTimeout;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _FlushTimeout;
    }

    public void setFlushTimeout(int _v_) {
        if (!isManaged()) {
            _FlushTimeout = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 5, vh_FlushTimeout, _v_));
    }

    @SuppressWarnings("deprecation")
    public BFuncProcedure() {
        _FuncName = "";
        _FuncArgument = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BFuncProcedure(long _OnzTid_, String _FuncName_, Zeze.Net.Binary _FuncArgument_, int _FlushMode_, int _FlushTimeout_) {
        _OnzTid = _OnzTid_;
        if (_FuncName_ == null)
            _FuncName_ = "";
        _FuncName = _FuncName_;
        if (_FuncArgument_ == null)
            _FuncArgument_ = Zeze.Net.Binary.Empty;
        _FuncArgument = _FuncArgument_;
        _FlushMode = _FlushMode_;
        _FlushTimeout = _FlushTimeout_;
    }

    @Override
    public void reset() {
        setOnzTid(0);
        setFuncName("");
        setFuncArgument(Zeze.Net.Binary.Empty);
        setFlushMode(0);
        setFlushTimeout(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Onz.BFuncProcedure.Data toData() {
        var _d_ = new Zeze.Builtin.Onz.BFuncProcedure.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Onz.BFuncProcedure.Data)_o_);
    }

    public void assign(BFuncProcedure.Data _o_) {
        setOnzTid(_o_._OnzTid);
        setFuncName(_o_._FuncName);
        setFuncArgument(_o_._FuncArgument);
        setFlushMode(_o_._FlushMode);
        setFlushTimeout(_o_._FlushTimeout);
        _unknown_ = null;
    }

    public void assign(BFuncProcedure _o_) {
        setOnzTid(_o_.getOnzTid());
        setFuncName(_o_.getFuncName());
        setFuncArgument(_o_.getFuncArgument());
        setFlushMode(_o_.getFlushMode());
        setFlushTimeout(_o_.getFlushTimeout());
        _unknown_ = _o_._unknown_;
    }

    public BFuncProcedure copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BFuncProcedure copy() {
        var _c_ = new BFuncProcedure();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BFuncProcedure _a_, BFuncProcedure _b_) {
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
        _s_.append("Zeze.Builtin.Onz.BFuncProcedure: {\n");
        _s_.append(_i1_).append("OnzTid=").append(getOnzTid()).append(",\n");
        _s_.append(_i1_).append("FuncName=").append(getFuncName()).append(",\n");
        _s_.append(_i1_).append("FuncArgument=").append(getFuncArgument()).append(",\n");
        _s_.append(_i1_).append("FlushMode=").append(getFlushMode()).append(",\n");
        _s_.append(_i1_).append("FlushTimeout=").append(getFlushTimeout()).append('\n');
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
            long _x_ = getOnzTid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getFuncName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getFuncArgument();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = getFlushMode();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getFlushTimeout();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
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
            setOnzTid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setFuncName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setFuncArgument(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setFlushMode(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setFlushTimeout(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BFuncProcedure))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BFuncProcedure)_o_;
        if (getOnzTid() != _b_.getOnzTid())
            return false;
        if (!getFuncName().equals(_b_.getFuncName()))
            return false;
        if (!getFuncArgument().equals(_b_.getFuncArgument()))
            return false;
        if (getFlushMode() != _b_.getFlushMode())
            return false;
        if (getFlushTimeout() != _b_.getFlushTimeout())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getOnzTid() < 0)
            return true;
        if (getFlushMode() < 0)
            return true;
        if (getFlushTimeout() < 0)
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
                case 1: _OnzTid = _v_.longValue(); break;
                case 2: _FuncName = _v_.stringValue(); break;
                case 3: _FuncArgument = _v_.binaryValue(); break;
                case 4: _FlushMode = _v_.intValue(); break;
                case 5: _FlushTimeout = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setOnzTid(_r_.getLong(_pn_ + "OnzTid"));
        setFuncName(_r_.getString(_pn_ + "FuncName"));
        if (getFuncName() == null)
            setFuncName("");
        setFuncArgument(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "FuncArgument")));
        setFlushMode(_r_.getInt(_pn_ + "FlushMode"));
        setFlushTimeout(_r_.getInt(_pn_ + "FlushTimeout"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "OnzTid", getOnzTid());
        _s_.appendString(_pn_ + "FuncName", getFuncName());
        _s_.appendBinary(_pn_ + "FuncArgument", getFuncArgument());
        _s_.appendInt(_pn_ + "FlushMode", getFlushMode());
        _s_.appendInt(_pn_ + "FlushTimeout", getFlushTimeout());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "OnzTid", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "FuncName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "FuncArgument", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "FlushMode", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "FlushTimeout", "int", "", ""));
        return _v_;
    }

// <enum name="eFlushPeriod" value="3"/>
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 2028535493874213798L;

    private long _OnzTid;
    private String _FuncName;
    private Zeze.Net.Binary _FuncArgument;
    private int _FlushMode;
    private int _FlushTimeout;

    public long getOnzTid() {
        return _OnzTid;
    }

    public void setOnzTid(long _v_) {
        _OnzTid = _v_;
    }

    public String getFuncName() {
        return _FuncName;
    }

    public void setFuncName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _FuncName = _v_;
    }

    public Zeze.Net.Binary getFuncArgument() {
        return _FuncArgument;
    }

    public void setFuncArgument(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _FuncArgument = _v_;
    }

    public int getFlushMode() {
        return _FlushMode;
    }

    public void setFlushMode(int _v_) {
        _FlushMode = _v_;
    }

    public int getFlushTimeout() {
        return _FlushTimeout;
    }

    public void setFlushTimeout(int _v_) {
        _FlushTimeout = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _FuncName = "";
        _FuncArgument = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(long _OnzTid_, String _FuncName_, Zeze.Net.Binary _FuncArgument_, int _FlushMode_, int _FlushTimeout_) {
        _OnzTid = _OnzTid_;
        if (_FuncName_ == null)
            _FuncName_ = "";
        _FuncName = _FuncName_;
        if (_FuncArgument_ == null)
            _FuncArgument_ = Zeze.Net.Binary.Empty;
        _FuncArgument = _FuncArgument_;
        _FlushMode = _FlushMode_;
        _FlushTimeout = _FlushTimeout_;
    }

    @Override
    public void reset() {
        _OnzTid = 0;
        _FuncName = "";
        _FuncArgument = Zeze.Net.Binary.Empty;
        _FlushMode = 0;
        _FlushTimeout = 0;
    }

    @Override
    public Zeze.Builtin.Onz.BFuncProcedure toBean() {
        var _b_ = new Zeze.Builtin.Onz.BFuncProcedure();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BFuncProcedure)_o_);
    }

    public void assign(BFuncProcedure _o_) {
        _OnzTid = _o_.getOnzTid();
        _FuncName = _o_.getFuncName();
        _FuncArgument = _o_.getFuncArgument();
        _FlushMode = _o_.getFlushMode();
        _FlushTimeout = _o_.getFlushTimeout();
    }

    public void assign(BFuncProcedure.Data _o_) {
        _OnzTid = _o_._OnzTid;
        _FuncName = _o_._FuncName;
        _FuncArgument = _o_._FuncArgument;
        _FlushMode = _o_._FlushMode;
        _FlushTimeout = _o_._FlushTimeout;
    }

    @Override
    public BFuncProcedure.Data copy() {
        var _c_ = new BFuncProcedure.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BFuncProcedure.Data _a_, BFuncProcedure.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BFuncProcedure.Data clone() {
        return (BFuncProcedure.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Onz.BFuncProcedure: {\n");
        _s_.append(_i1_).append("OnzTid=").append(_OnzTid).append(",\n");
        _s_.append(_i1_).append("FuncName=").append(_FuncName).append(",\n");
        _s_.append(_i1_).append("FuncArgument=").append(_FuncArgument).append(",\n");
        _s_.append(_i1_).append("FlushMode=").append(_FlushMode).append(",\n");
        _s_.append(_i1_).append("FlushTimeout=").append(_FlushTimeout).append('\n');
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
            long _x_ = _OnzTid;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = _FuncName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _FuncArgument;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = _FlushMode;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _FlushTimeout;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
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
            _OnzTid = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _FuncName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _FuncArgument = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _FlushMode = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _FlushTimeout = _o_.ReadInt(_t_);
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
        if (!(_o_ instanceof BFuncProcedure.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BFuncProcedure.Data)_o_;
        if (_OnzTid != _b_._OnzTid)
            return false;
        if (!_FuncName.equals(_b_._FuncName))
            return false;
        if (!_FuncArgument.equals(_b_._FuncArgument))
            return false;
        if (_FlushMode != _b_._FlushMode)
            return false;
        if (_FlushTimeout != _b_._FlushTimeout)
            return false;
        return true;
    }
}
}
