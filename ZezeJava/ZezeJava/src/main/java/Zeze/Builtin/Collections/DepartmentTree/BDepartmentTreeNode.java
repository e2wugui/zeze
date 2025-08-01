// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BDepartmentTreeNode extends Zeze.Transaction.Bean implements BDepartmentTreeNodeReadOnly {
    public static final long TYPEID = 2712461973987809351L;

    private long _ParentDepartment; // 0表示第一级部门
    private final Zeze.Transaction.Collections.PMap1<String, Long> _Children; // name 2 id。采用整体保存，因为需要排序和重名判断。需要加数量上限。
    private String _Name;
    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Transaction.DynamicBean> _Managers;

    private static final Zeze.Transaction.Collections.Meta2<String, Zeze.Transaction.DynamicBean> meta2_Managers
            = Zeze.Transaction.Collections.Meta2.createDynamicMapMeta(String.class, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);

    public static Zeze.Transaction.DynamicBean newDynamicBean_Managers() {
        return new Zeze.Transaction.DynamicBean(4, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_4(Zeze.Transaction.Bean _b_) {
        return Zeze.Collections.DepartmentTree.getSpecialTypeIdFromBean(_b_);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_4(long _t_) {
        return Zeze.Collections.DepartmentTree.createBeanFromSpecialTypeId(_t_);
    }

    private final Zeze.Transaction.DynamicBean _Data;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Data() {
        return new Zeze.Transaction.DynamicBean(5, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_5(Zeze.Transaction.Bean _b_) {
        return Zeze.Collections.DepartmentTree.getSpecialTypeIdFromBean(_b_);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_5(long _t_) {
        return Zeze.Collections.DepartmentTree.createBeanFromSpecialTypeId(_t_);
    }

    private static final java.lang.invoke.VarHandle vh_ParentDepartment;
    private static final java.lang.invoke.VarHandle vh_Name;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ParentDepartment = _l_.findVarHandle(BDepartmentTreeNode.class, "_ParentDepartment", long.class);
            vh_Name = _l_.findVarHandle(BDepartmentTreeNode.class, "_Name", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getParentDepartment() {
        if (!isManaged())
            return _ParentDepartment;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ParentDepartment;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ParentDepartment;
    }

    public void setParentDepartment(long _v_) {
        if (!isManaged()) {
            _ParentDepartment = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_ParentDepartment, _v_));
    }

    public Zeze.Transaction.Collections.PMap1<String, Long> getChildren() {
        return _Children;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<String, Long> getChildrenReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_Children);
    }

    @Override
    public String getName() {
        if (!isManaged())
            return _Name;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Name;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 3);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 3, vh_Name, _v_));
    }

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Transaction.DynamicBean> getManagers() {
        return _Managers;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getManagersReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Managers);
    }

    public Zeze.Transaction.DynamicBean getData() {
        return _Data;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getDataReadOnly() {
        return _Data;
    }

    @SuppressWarnings("deprecation")
    public BDepartmentTreeNode() {
        _Children = new Zeze.Transaction.Collections.PMap1<>(String.class, Long.class);
        _Children.variableId(2);
        _Name = "";
        _Managers = new Zeze.Transaction.Collections.PMap2<>(meta2_Managers);
        _Managers.variableId(4);
        _Data = newDynamicBean_Data();
    }

    @SuppressWarnings("deprecation")
    public BDepartmentTreeNode(long _ParentDepartment_, String _Name_) {
        _ParentDepartment = _ParentDepartment_;
        _Children = new Zeze.Transaction.Collections.PMap1<>(String.class, Long.class);
        _Children.variableId(2);
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
        _Managers = new Zeze.Transaction.Collections.PMap2<>(meta2_Managers);
        _Managers.variableId(4);
        _Data = newDynamicBean_Data();
    }

    @Override
    public void reset() {
        setParentDepartment(0);
        _Children.clear();
        setName("");
        _Managers.clear();
        _Data.reset();
        _unknown_ = null;
    }

    public void assign(BDepartmentTreeNode _o_) {
        setParentDepartment(_o_.getParentDepartment());
        _Children.assign(_o_._Children);
        setName(_o_.getName());
        _Managers.clear();
        for (var _e_ : _o_._Managers.entrySet())
            _Managers.put(_e_.getKey(), _e_.getValue().copy());
        _Data.assign(_o_._Data);
        _unknown_ = _o_._unknown_;
    }

    public BDepartmentTreeNode copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDepartmentTreeNode copy() {
        var _c_ = new BDepartmentTreeNode();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BDepartmentTreeNode _a_, BDepartmentTreeNode _b_) {
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
        _s_.append("Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNode: {\n");
        _s_.append(_i1_).append("ParentDepartment=").append(getParentDepartment()).append(",\n");
        _s_.append(_i1_).append("Children={");
        if (!_Children.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Children.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Children.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("Name=").append(getName()).append(",\n");
        _s_.append(_i1_).append("Managers={");
        if (!_Managers.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Managers.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Managers.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().getBean().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("Data=");
        _Data.getBean().buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
            long _x_ = getParentDepartment();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Children;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            String _x_ = getName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _Managers;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.DYNAMIC);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _Data;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
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
            setParentDepartment(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Children;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadLong(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = _Managers;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = new Zeze.Transaction.DynamicBean(0, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
                    _o_.ReadDynamic(_v_, _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _o_.ReadDynamic(_Data, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BDepartmentTreeNode))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BDepartmentTreeNode)_o_;
        if (getParentDepartment() != _b_.getParentDepartment())
            return false;
        if (!_Children.equals(_b_._Children))
            return false;
        if (!getName().equals(_b_.getName()))
            return false;
        if (!_Managers.equals(_b_._Managers))
            return false;
        if (!_Data.equals(_b_._Data))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Children.initRootInfo(_r_, this);
        _Managers.initRootInfo(_r_, this);
        _Data.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Children.initRootInfoWithRedo(_r_, this);
        _Managers.initRootInfoWithRedo(_r_, this);
        _Data.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getParentDepartment() < 0)
            return true;
        for (var _v_ : _Children.values()) {
            if (_v_ < 0)
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
                case 1: _ParentDepartment = _v_.longValue(); break;
                case 2: _Children.followerApply(_v_); break;
                case 3: _Name = _v_.stringValue(); break;
                case 4: _Managers.followerApply(_v_); break;
                case 5: _Data.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setParentDepartment(_r_.getLong(_pn_ + "ParentDepartment"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Children", _Children, _r_.getString(_pn_ + "Children"));
        setName(_r_.getString(_pn_ + "Name"));
        if (getName() == null)
            setName("");
        Zeze.Serialize.Helper.decodeJsonMap(this, "Managers", _Managers, _r_.getString(_pn_ + "Managers"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_Data, _r_.getString(_pn_ + "Data"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "ParentDepartment", getParentDepartment());
        _s_.appendString(_pn_ + "Children", Zeze.Serialize.Helper.encodeJson(_Children));
        _s_.appendString(_pn_ + "Name", getName());
        _s_.appendString(_pn_ + "Managers", Zeze.Serialize.Helper.encodeJson(_Managers));
        _s_.appendString(_pn_ + "Data", Zeze.Serialize.Helper.encodeJson(_Data));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ParentDepartment", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Children", "map", "string", "long"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Name", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Managers", "map", "string", "dynamic"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Data", "dynamic", "", ""));
        return _v_;
    }
}
