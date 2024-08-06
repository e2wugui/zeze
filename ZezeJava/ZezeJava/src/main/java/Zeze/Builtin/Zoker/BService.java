// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 服务：查询，启动，关闭
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BService extends Zeze.Transaction.Bean implements BServiceReadOnly {
    public static final long TYPEID = 8648379280162192984L;

    private String _ServiceName;
    private String _State; // Running,Stopped
    private String _Ps; // some ps result ...

    @Override
    public String getServiceName() {
        if (!isManaged())
            return _ServiceName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServiceName;
        var log = (Log__ServiceName)_t_.getLog(objectId() + 1);
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
        _t_.putLog(new Log__ServiceName(this, 1, _v_));
    }

    @Override
    public String getState() {
        if (!isManaged())
            return _State;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _State;
        var log = (Log__State)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _State;
    }

    public void setState(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _State = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__State(this, 2, _v_));
    }

    @Override
    public String getPs() {
        if (!isManaged())
            return _Ps;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Ps;
        var log = (Log__Ps)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Ps;
    }

    public void setPs(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Ps = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Ps(this, 3, _v_));
    }

    @SuppressWarnings("deprecation")
    public BService() {
        _ServiceName = "";
        _State = "";
        _Ps = "";
    }

    @SuppressWarnings("deprecation")
    public BService(String _ServiceName_, String _State_, String _Ps_) {
        if (_ServiceName_ == null)
            _ServiceName_ = "";
        _ServiceName = _ServiceName_;
        if (_State_ == null)
            _State_ = "";
        _State = _State_;
        if (_Ps_ == null)
            _Ps_ = "";
        _Ps = _Ps_;
    }

    @Override
    public void reset() {
        setServiceName("");
        setState("");
        setPs("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Zoker.BService.Data toData() {
        var _d_ = new Zeze.Builtin.Zoker.BService.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Zoker.BService.Data)_o_);
    }

    public void assign(BService.Data _o_) {
        setServiceName(_o_._ServiceName);
        setState(_o_._State);
        setPs(_o_._Ps);
        _unknown_ = null;
    }

    public void assign(BService _o_) {
        setServiceName(_o_.getServiceName());
        setState(_o_.getState());
        setPs(_o_.getPs());
        _unknown_ = _o_._unknown_;
    }

    public BService copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BService copy() {
        var _c_ = new BService();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BService _a_, BService _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServiceName extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceName(BService _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BService)getBelong())._ServiceName = value; }
    }

    private static final class Log__State extends Zeze.Transaction.Logs.LogString {
        public Log__State(BService _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BService)getBelong())._State = value; }
    }

    private static final class Log__Ps extends Zeze.Transaction.Logs.LogString {
        public Log__Ps(BService _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BService)getBelong())._Ps = value; }
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
        _s_.append("Zeze.Builtin.Zoker.BService: {\n");
        _s_.append(_i1_).append("ServiceName=").append(getServiceName()).append(",\n");
        _s_.append(_i1_).append("State=").append(getState()).append(",\n");
        _s_.append(_i1_).append("Ps=").append(getPs()).append('\n');
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
            String _x_ = getState();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getPs();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            setServiceName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setState(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setPs(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BService))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BService)_o_;
        if (!getServiceName().equals(_b_.getServiceName()))
            return false;
        if (!getState().equals(_b_.getState()))
            return false;
        if (!getPs().equals(_b_.getPs()))
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
                case 2: _State = _v_.stringValue(); break;
                case 3: _Ps = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setServiceName(_r_.getString(_pn_ + "ServiceName"));
        if (getServiceName() == null)
            setServiceName("");
        setState(_r_.getString(_pn_ + "State"));
        if (getState() == null)
            setState("");
        setPs(_r_.getString(_pn_ + "Ps"));
        if (getPs() == null)
            setPs("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "ServiceName", getServiceName());
        _s_.appendString(_pn_ + "State", getState());
        _s_.appendString(_pn_ + "Ps", getPs());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServiceName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "State", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Ps", "string", "", ""));
        return _v_;
    }

// 服务：查询，启动，关闭
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 8648379280162192984L;

    private String _ServiceName;
    private String _State; // Running,Stopped
    private String _Ps; // some ps result ...

    public String getServiceName() {
        return _ServiceName;
    }

    public void setServiceName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ServiceName = _v_;
    }

    public String getState() {
        return _State;
    }

    public void setState(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _State = _v_;
    }

    public String getPs() {
        return _Ps;
    }

    public void setPs(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Ps = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ServiceName = "";
        _State = "";
        _Ps = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _ServiceName_, String _State_, String _Ps_) {
        if (_ServiceName_ == null)
            _ServiceName_ = "";
        _ServiceName = _ServiceName_;
        if (_State_ == null)
            _State_ = "";
        _State = _State_;
        if (_Ps_ == null)
            _Ps_ = "";
        _Ps = _Ps_;
    }

    @Override
    public void reset() {
        _ServiceName = "";
        _State = "";
        _Ps = "";
    }

    @Override
    public Zeze.Builtin.Zoker.BService toBean() {
        var _b_ = new Zeze.Builtin.Zoker.BService();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BService)_o_);
    }

    public void assign(BService _o_) {
        _ServiceName = _o_.getServiceName();
        _State = _o_.getState();
        _Ps = _o_.getPs();
    }

    public void assign(BService.Data _o_) {
        _ServiceName = _o_._ServiceName;
        _State = _o_._State;
        _Ps = _o_._Ps;
    }

    @Override
    public BService.Data copy() {
        var _c_ = new BService.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BService.Data _a_, BService.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BService.Data clone() {
        return (BService.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Zoker.BService: {\n");
        _s_.append(_i1_).append("ServiceName=").append(_ServiceName).append(",\n");
        _s_.append(_i1_).append("State=").append(_State).append(",\n");
        _s_.append(_i1_).append("Ps=").append(_Ps).append('\n');
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
            String _x_ = _State;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _Ps;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            _ServiceName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _State = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Ps = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
