// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BLastVersionBeanInfo extends Zeze.Transaction.Bean implements BLastVersionBeanInfoReadOnly {
    public static final long TYPEID = -6575391224958548024L;

    private String _Name;
    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.HotDistribute.BVariable> _Variables;

    private static final java.lang.invoke.VarHandle vh_Name;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Name = _l_.findVarHandle(BLastVersionBeanInfo.class, "_Name", String.class);
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
        return log != null ? log.stringValue() : _Name;
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

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.HotDistribute.BVariable> getVariables() {
        return _Variables;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.HotDistribute.BVariable, Zeze.Builtin.HotDistribute.BVariableReadOnly> getVariablesReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Variables);
    }

    @SuppressWarnings("deprecation")
    public BLastVersionBeanInfo() {
        _Name = "";
        _Variables = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.HotDistribute.BVariable.class);
        _Variables.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BLastVersionBeanInfo(String _Name_) {
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
        _Variables = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.HotDistribute.BVariable.class);
        _Variables.variableId(2);
    }

    @Override
    public void reset() {
        setName("");
        _Variables.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BLastVersionBeanInfo.Data toData() {
        var _d_ = new Zeze.Builtin.HotDistribute.BLastVersionBeanInfo.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.HotDistribute.BLastVersionBeanInfo.Data)_o_);
    }

    public void assign(BLastVersionBeanInfo.Data _o_) {
        setName(_o_._Name);
        _Variables.clear();
        for (var _e_ : _o_._Variables) {
            var _v_ = new Zeze.Builtin.HotDistribute.BVariable();
            _v_.assign(_e_);
            _Variables.add(_v_);
        }
        _unknown_ = null;
    }

    public void assign(BLastVersionBeanInfo _o_) {
        setName(_o_.getName());
        _Variables.clear();
        for (var _e_ : _o_._Variables)
            _Variables.add(_e_.copy());
        _unknown_ = _o_._unknown_;
    }

    public BLastVersionBeanInfo copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLastVersionBeanInfo copy() {
        var _c_ = new BLastVersionBeanInfo();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLastVersionBeanInfo _a_, BLastVersionBeanInfo _b_) {
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
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.HotDistribute.BLastVersionBeanInfo: {\n");
        _s_.append(_i1_).append("Name=").append(getName()).append(",\n");
        _s_.append(_i1_).append("Variables=[");
        if (!_Variables.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _Variables) {
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("]\n");
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
        {
            var _x_ = _Variables;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
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
        if (_i_ == 2) {
            var _x_ = _Variables;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.HotDistribute.BVariable(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLastVersionBeanInfo))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLastVersionBeanInfo)_o_;
        if (!getName().equals(_b_.getName()))
            return false;
        if (!_Variables.equals(_b_._Variables))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Variables.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Variables.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Variables) {
            if (_v_.negativeCheck())
                return true;
        }
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
                case 1: _Name = _v_.stringValue(); break;
                case 2: _Variables.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setName(_r_.getString(_pn_ + "Name"));
        if (getName() == null)
            setName("");
        Zeze.Serialize.Helper.decodeJsonList(_Variables, Zeze.Builtin.HotDistribute.BVariable.class, _r_.getString(_pn_ + "Variables"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Name", getName());
        _s_.appendString(_pn_ + "Variables", Zeze.Serialize.Helper.encodeJson(_Variables));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Name", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Variables", "list", "", "Zeze.Builtin.HotDistribute.BVariable"));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6575391224958548024L;

    private String _Name;
    private java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> _Variables;

    public String getName() {
        return _Name;
    }

    public void setName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Name = _v_;
    }

    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> getVariables() {
        return _Variables;
    }

    public void setVariables(java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Variables = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Name = "";
        _Variables = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(String _Name_, java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> _Variables_) {
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
        if (_Variables_ == null)
            _Variables_ = new java.util.ArrayList<>();
        _Variables = _Variables_;
    }

    @Override
    public void reset() {
        _Name = "";
        _Variables.clear();
    }

    @Override
    public Zeze.Builtin.HotDistribute.BLastVersionBeanInfo toBean() {
        var _b_ = new Zeze.Builtin.HotDistribute.BLastVersionBeanInfo();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BLastVersionBeanInfo)_o_);
    }

    public void assign(BLastVersionBeanInfo _o_) {
        _Name = _o_.getName();
        _Variables.clear();
        for (var _e_ : _o_._Variables) {
            var _v_ = new Zeze.Builtin.HotDistribute.BVariable.Data();
            _v_.assign(_e_);
            _Variables.add(_v_);
        }
    }

    public void assign(BLastVersionBeanInfo.Data _o_) {
        _Name = _o_._Name;
        _Variables.clear();
        for (var _e_ : _o_._Variables)
            _Variables.add(_e_.copy());
    }

    @Override
    public BLastVersionBeanInfo.Data copy() {
        var _c_ = new BLastVersionBeanInfo.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLastVersionBeanInfo.Data _a_, BLastVersionBeanInfo.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLastVersionBeanInfo.Data clone() {
        return (BLastVersionBeanInfo.Data)super.clone();
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
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.HotDistribute.BLastVersionBeanInfo: {\n");
        _s_.append(_i1_).append("Name=").append(_Name).append(",\n");
        _s_.append(_i1_).append("Variables=[");
        if (!_Variables.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _Variables) {
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("]\n");
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
        {
            var _x_ = _Variables;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
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
        if (_i_ == 2) {
            var _x_ = _Variables;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.HotDistribute.BVariable.Data(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
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
        if (!(_o_ instanceof BLastVersionBeanInfo.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLastVersionBeanInfo.Data)_o_;
        if (!_Name.equals(_b_._Name))
            return false;
        if (!_Variables.equals(_b_._Variables))
            return false;
        return true;
    }
}
}
