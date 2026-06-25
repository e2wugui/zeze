// auto-generated @formatter:off
package metagame.builtin.TaskModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BCondition extends Zeze.Transaction.Bean implements BConditionReadOnly {
    public static final long TYPEID = -2807495013121592461L;

    private String _ClassName;
    private Zeze.Net.Binary _Parameter;

    private static final java.lang.invoke.VarHandle vh_ClassName;
    private static final java.lang.invoke.VarHandle vh_Parameter;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ClassName = _l_.findVarHandle(BCondition.class, "_ClassName", String.class);
            vh_Parameter = _l_.findVarHandle(BCondition.class, "_Parameter", Zeze.Net.Binary.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getClassName() {
        if (!isManaged())
            return _ClassName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ClassName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.stringValue() : _ClassName;
    }

    public void setClassName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ClassName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_ClassName, _v_));
    }

    @Override
    public Zeze.Net.Binary getParameter() {
        if (!isManaged())
            return _Parameter;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Parameter;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Parameter;
    }

    public void setParameter(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Parameter = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 2, vh_Parameter, _v_));
    }

    @SuppressWarnings("deprecation")
    public BCondition() {
        _ClassName = "";
        _Parameter = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BCondition(String _ClassName_, Zeze.Net.Binary _Parameter_) {
        if (_ClassName_ == null)
            _ClassName_ = "";
        _ClassName = _ClassName_;
        if (_Parameter_ == null)
            _Parameter_ = Zeze.Net.Binary.Empty;
        _Parameter = _Parameter_;
    }

    @Override
    public void reset() {
        setClassName("");
        setParameter(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public metagame.builtin.TaskModule.BCondition.Data toData() {
        var _d_ = new metagame.builtin.TaskModule.BCondition.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((metagame.builtin.TaskModule.BCondition.Data)_o_);
    }

    public void assign(BCondition.Data _o_) {
        setClassName(_o_._ClassName);
        setParameter(_o_._Parameter);
        _unknown_ = null;
    }

    public void assign(BCondition _o_) {
        setClassName(_o_.getClassName());
        setParameter(_o_.getParameter());
        _unknown_ = _o_._unknown_;
    }

    public BCondition copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCondition copy() {
        var _c_ = new BCondition();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCondition _a_, BCondition _b_) {
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
        _s_.append("metagame.builtin.TaskModule.BCondition: {\n");
        _s_.append(_i1_).append("ClassName=").append(getClassName()).append(",\n");
        _s_.append(_i1_).append("Parameter=").append(getParameter()).append('\n');
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
            String _x_ = getClassName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getParameter();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
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
            setClassName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setParameter(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BCondition))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCondition)_o_;
        if (!getClassName().equals(_b_.getClassName()))
            return false;
        if (!getParameter().equals(_b_.getParameter()))
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
                case 1: _ClassName = _v_.stringValue(); break;
                case 2: _Parameter = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setClassName(_r_.getString(_pn_ + "ClassName"));
        if (getClassName() == null)
            setClassName("");
        setParameter(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Parameter")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "ClassName", getClassName());
        _s_.appendBinary(_pn_ + "Parameter", getParameter());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ClassName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Parameter", "binary", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2807495013121592461L;

    private String _ClassName;
    private Zeze.Net.Binary _Parameter;

    public String getClassName() {
        return _ClassName;
    }

    public void setClassName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ClassName = _v_;
    }

    public Zeze.Net.Binary getParameter() {
        return _Parameter;
    }

    public void setParameter(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Parameter = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ClassName = "";
        _Parameter = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(String _ClassName_, Zeze.Net.Binary _Parameter_) {
        if (_ClassName_ == null)
            _ClassName_ = "";
        _ClassName = _ClassName_;
        if (_Parameter_ == null)
            _Parameter_ = Zeze.Net.Binary.Empty;
        _Parameter = _Parameter_;
    }

    @Override
    public void reset() {
        _ClassName = "";
        _Parameter = Zeze.Net.Binary.Empty;
    }

    @Override
    public metagame.builtin.TaskModule.BCondition toBean() {
        var _b_ = new metagame.builtin.TaskModule.BCondition();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BCondition)_o_);
    }

    public void assign(BCondition _o_) {
        _ClassName = _o_.getClassName();
        _Parameter = _o_.getParameter();
    }

    public void assign(BCondition.Data _o_) {
        _ClassName = _o_._ClassName;
        _Parameter = _o_._Parameter;
    }

    @Override
    public BCondition.Data copy() {
        var _c_ = new BCondition.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCondition.Data _a_, BCondition.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCondition.Data clone() {
        return (BCondition.Data)super.clone();
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
        _s_.append("metagame.builtin.TaskModule.BCondition: {\n");
        _s_.append(_i1_).append("ClassName=").append(_ClassName).append(",\n");
        _s_.append(_i1_).append("Parameter=").append(_Parameter).append('\n');
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
            String _x_ = _ClassName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _Parameter;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
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
            _ClassName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Parameter = _o_.ReadBinary(_t_);
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
        if (!(_o_ instanceof BCondition.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCondition.Data)_o_;
        if (!_ClassName.equals(_b_._ClassName))
            return false;
        if (!_Parameter.equals(_b_._Parameter))
            return false;
        return true;
    }
}
}
