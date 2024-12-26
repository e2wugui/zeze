// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BeanName extends Zeze.Transaction.Bean implements BeanNameReadOnly {
    public static final long TYPEID = -1975096028535811269L;

    private String _Name;

    private static final java.lang.invoke.VarHandle vh_Name;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Name = _l_.findVarHandle(BeanName.class, "_Name", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getName() {
        if (!isManaged())
            return _Name;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Name;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Name;
    }

    public void setName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Name = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_Name, _v_));
    }

    @SuppressWarnings("deprecation")
    public BeanName() {
        _Name = "";
    }

    @SuppressWarnings("deprecation")
    public BeanName(String _Name_) {
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
    }

    @Override
    public void reset() {
        setName("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BeanName.Data toData() {
        var _d_ = new Zeze.Builtin.HotDistribute.BeanName.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.HotDistribute.BeanName.Data)_o_);
    }

    public void assign(BeanName.Data _o_) {
        setName(_o_._Name);
        _unknown_ = null;
    }

    public void assign(BeanName _o_) {
        setName(_o_.getName());
        _unknown_ = _o_._unknown_;
    }

    public BeanName copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BeanName copy() {
        var _c_ = new BeanName();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BeanName _a_, BeanName _b_) {
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
        _s_.append("Zeze.Builtin.HotDistribute.BeanName: {\n");
        _s_.append(_i1_).append("Name=").append(getName()).append('\n');
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
            String _x_ = getName();
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
            setName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BeanName))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BeanName)_o_;
        if (!getName().equals(_b_.getName()))
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
                case 1: _Name = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setName(_r_.getString(_pn_ + "Name"));
        if (getName() == null)
            setName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Name", getName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Name", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -1975096028535811269L;

    private String _Name;

    public String getName() {
        return _Name;
    }

    public void setName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Name = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Name = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _Name_) {
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
    }

    @Override
    public void reset() {
        _Name = "";
    }

    @Override
    public Zeze.Builtin.HotDistribute.BeanName toBean() {
        var _b_ = new Zeze.Builtin.HotDistribute.BeanName();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BeanName)_o_);
    }

    public void assign(BeanName _o_) {
        _Name = _o_.getName();
    }

    public void assign(BeanName.Data _o_) {
        _Name = _o_._Name;
    }

    @Override
    public BeanName.Data copy() {
        var _c_ = new BeanName.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BeanName.Data _a_, BeanName.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BeanName.Data clone() {
        return (BeanName.Data)super.clone();
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
        _s_.append("Zeze.Builtin.HotDistribute.BeanName: {\n");
        _s_.append(_i1_).append("Name=").append(_Name).append('\n');
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
            String _x_ = _Name;
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
            _Name = _o_.ReadString(_t_);
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
        if (!(_o_ instanceof BeanName.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BeanName.Data)_o_;
        if (!_Name.equals(_b_._Name))
            return false;
        return true;
    }
}
}
