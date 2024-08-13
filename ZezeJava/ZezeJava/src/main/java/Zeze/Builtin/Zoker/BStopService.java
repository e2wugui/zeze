// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BStopService extends Zeze.Transaction.Bean implements BStopServiceReadOnly {
    public static final long TYPEID = 9090863522815062458L;

    private String _ServiceName;
    private boolean _Force; // like kill -9

    private static final java.lang.invoke.VarHandle vh_ServiceName;
    private static final java.lang.invoke.VarHandle vh_Force;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ServiceName = _l_.findVarHandle(BStopService.class, "_ServiceName", String.class);
            vh_Force = _l_.findVarHandle(BStopService.class, "_Force", boolean.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getServiceName() {
        if (!isManaged())
            return _ServiceName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServiceName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ServiceName;
    }

    public void setServiceName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_ServiceName, _v_));
    }

    @Override
    public boolean isForce() {
        if (!isManaged())
            return _Force;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Force;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Force;
    }

    public void setForce(boolean _v_) {
        if (!isManaged()) {
            _Force = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 2, vh_Force, _v_));
    }

    @SuppressWarnings("deprecation")
    public BStopService() {
        _ServiceName = "";
    }

    @SuppressWarnings("deprecation")
    public BStopService(String _ServiceName_, boolean _Force_) {
        if (_ServiceName_ == null)
            _ServiceName_ = "";
        _ServiceName = _ServiceName_;
        _Force = _Force_;
    }

    @Override
    public void reset() {
        setServiceName("");
        setForce(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Zoker.BStopService.Data toData() {
        var _d_ = new Zeze.Builtin.Zoker.BStopService.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Zoker.BStopService.Data)_o_);
    }

    public void assign(BStopService.Data _o_) {
        setServiceName(_o_._ServiceName);
        setForce(_o_._Force);
        _unknown_ = null;
    }

    public void assign(BStopService _o_) {
        setServiceName(_o_.getServiceName());
        setForce(_o_.isForce());
        _unknown_ = _o_._unknown_;
    }

    public BStopService copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BStopService copy() {
        var _c_ = new BStopService();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BStopService _a_, BStopService _b_) {
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
        _s_.append("Zeze.Builtin.Zoker.BStopService: {\n");
        _s_.append(_i1_).append("ServiceName=").append(getServiceName()).append(",\n");
        _s_.append(_i1_).append("Force=").append(isForce()).append('\n');
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
            String _x_ = getServiceName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            boolean _x_ = isForce();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setServiceName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setForce(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BStopService))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BStopService)_o_;
        if (!getServiceName().equals(_b_.getServiceName()))
            return false;
        if (isForce() != _b_.isForce())
            return false;
        return true;
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
                case 1: _ServiceName = _v_.stringValue(); break;
                case 2: _Force = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setServiceName(_r_.getString(_pn_ + "ServiceName"));
        if (getServiceName() == null)
            setServiceName("");
        setForce(_r_.getBoolean(_pn_ + "Force"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "ServiceName", getServiceName());
        _s_.appendBoolean(_pn_ + "Force", isForce());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServiceName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Force", "bool", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 9090863522815062458L;

    private String _ServiceName;
    private boolean _Force; // like kill -9

    public String getServiceName() {
        return _ServiceName;
    }

    public void setServiceName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ServiceName = _v_;
    }

    public boolean isForce() {
        return _Force;
    }

    public void setForce(boolean _v_) {
        _Force = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ServiceName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _ServiceName_, boolean _Force_) {
        if (_ServiceName_ == null)
            _ServiceName_ = "";
        _ServiceName = _ServiceName_;
        _Force = _Force_;
    }

    @Override
    public void reset() {
        _ServiceName = "";
        _Force = false;
    }

    @Override
    public Zeze.Builtin.Zoker.BStopService toBean() {
        var _b_ = new Zeze.Builtin.Zoker.BStopService();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BStopService)_o_);
    }

    public void assign(BStopService _o_) {
        _ServiceName = _o_.getServiceName();
        _Force = _o_.isForce();
    }

    public void assign(BStopService.Data _o_) {
        _ServiceName = _o_._ServiceName;
        _Force = _o_._Force;
    }

    @Override
    public BStopService.Data copy() {
        var _c_ = new BStopService.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BStopService.Data _a_, BStopService.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BStopService.Data clone() {
        return (BStopService.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Zoker.BStopService: {\n");
        _s_.append(_i1_).append("ServiceName=").append(_ServiceName).append(",\n");
        _s_.append(_i1_).append("Force=").append(_Force).append('\n');
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
            String _x_ = _ServiceName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            boolean _x_ = _Force;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            _ServiceName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Force = _o_.ReadBool(_t_);
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
        if (!(_o_ instanceof BStopService.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BStopService.Data)_o_;
        if (!_ServiceName.equals(_b_._ServiceName))
            return false;
        if (_Force != _b_._Force)
            return false;
        return true;
    }
}
}
