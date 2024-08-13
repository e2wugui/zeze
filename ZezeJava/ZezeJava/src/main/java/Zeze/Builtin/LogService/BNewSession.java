// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BNewSession extends Zeze.Transaction.Bean implements BNewSessionReadOnly {
    public static final long TYPEID = 4447234831022031083L;

    private String _LogName;

    private static final java.lang.invoke.VarHandle vh_LogName;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_LogName = _l_.findVarHandle(BNewSession.class, "_LogName", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getLogName() {
        if (!isManaged())
            return _LogName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LogName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _LogName;
    }

    public void setLogName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LogName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_LogName, _v_));
    }

    @SuppressWarnings("deprecation")
    public BNewSession() {
        _LogName = "";
    }

    @SuppressWarnings("deprecation")
    public BNewSession(String _LogName_) {
        if (_LogName_ == null)
            _LogName_ = "";
        _LogName = _LogName_;
    }

    @Override
    public void reset() {
        setLogName("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BNewSession.Data toData() {
        var _d_ = new Zeze.Builtin.LogService.BNewSession.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.LogService.BNewSession.Data)_o_);
    }

    public void assign(BNewSession.Data _o_) {
        setLogName(_o_._LogName);
        _unknown_ = null;
    }

    public void assign(BNewSession _o_) {
        setLogName(_o_.getLogName());
        _unknown_ = _o_._unknown_;
    }

    public BNewSession copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BNewSession copy() {
        var _c_ = new BNewSession();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BNewSession _a_, BNewSession _b_) {
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
        _s_.append("Zeze.Builtin.LogService.BNewSession: {\n");
        _s_.append(_i1_).append("LogName=").append(getLogName()).append('\n');
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
            String _x_ = getLogName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            setLogName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BNewSession))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BNewSession)_o_;
        if (!getLogName().equals(_b_.getLogName()))
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
                case 1: _LogName = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setLogName(_r_.getString(_pn_ + "LogName"));
        if (getLogName() == null)
            setLogName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "LogName", getLogName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "LogName", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4447234831022031083L;

    private String _LogName;

    public String getLogName() {
        return _LogName;
    }

    public void setLogName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _LogName = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _LogName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _LogName_) {
        if (_LogName_ == null)
            _LogName_ = "";
        _LogName = _LogName_;
    }

    @Override
    public void reset() {
        _LogName = "";
    }

    @Override
    public Zeze.Builtin.LogService.BNewSession toBean() {
        var _b_ = new Zeze.Builtin.LogService.BNewSession();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BNewSession)_o_);
    }

    public void assign(BNewSession _o_) {
        _LogName = _o_.getLogName();
    }

    public void assign(BNewSession.Data _o_) {
        _LogName = _o_._LogName;
    }

    @Override
    public BNewSession.Data copy() {
        var _c_ = new BNewSession.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BNewSession.Data _a_, BNewSession.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BNewSession.Data clone() {
        return (BNewSession.Data)super.clone();
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
        _s_.append("Zeze.Builtin.LogService.BNewSession: {\n");
        _s_.append(_i1_).append("LogName=").append(_LogName).append('\n');
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
            String _x_ = _LogName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            _LogName = _o_.ReadString(_t_);
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
        if (!(_o_ instanceof BNewSession.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BNewSession.Data)_o_;
        if (!_LogName.equals(_b_._LogName))
            return false;
        return true;
    }
}
}
