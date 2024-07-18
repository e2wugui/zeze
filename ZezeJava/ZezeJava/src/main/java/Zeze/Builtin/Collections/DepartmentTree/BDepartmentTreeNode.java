// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BDepartmentTreeNode extends Zeze.Transaction.Bean implements BDepartmentTreeNodeReadOnly {
    public static final long TYPEID = 2712461973987809351L;

    private long _ParentDepartment; // 0表示第一级部门
    private final Zeze.Transaction.Collections.PMap1<String, Long> _Childs; // name 2 id。采用整体保存，因为需要排序和重名判断。需要加数量上限。
    private String _Name;
    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Transaction.DynamicBean> _Managers;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Managers() {
        return new Zeze.Transaction.DynamicBean(4, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_4(Zeze.Transaction.Bean bean) {
        return Zeze.Collections.DepartmentTree.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_4(long typeId) {
        return Zeze.Collections.DepartmentTree.createBeanFromSpecialTypeId(typeId);
    }

    private final Zeze.Transaction.DynamicBean _Data;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Data() {
        return new Zeze.Transaction.DynamicBean(5, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_5(Zeze.Transaction.Bean bean) {
        return Zeze.Collections.DepartmentTree.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_5(long typeId) {
        return Zeze.Collections.DepartmentTree.createBeanFromSpecialTypeId(typeId);
    }

    @Override
    public long getParentDepartment() {
        if (!isManaged())
            return _ParentDepartment;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ParentDepartment;
        var log = (Log__ParentDepartment)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ParentDepartment;
    }

    public void setParentDepartment(long value) {
        if (!isManaged()) {
            _ParentDepartment = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ParentDepartment(this, 1, value));
    }

    public Zeze.Transaction.Collections.PMap1<String, Long> getChilds() {
        return _Childs;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<String, Long> getChildsReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_Childs);
    }

    @Override
    public String getName() {
        if (!isManaged())
            return _Name;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Name;
        var log = (Log__Name)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Name;
    }

    public void setName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Name = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Name(this, 3, value));
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
        _Childs = new Zeze.Transaction.Collections.PMap1<>(String.class, Long.class);
        _Childs.variableId(2);
        _Name = "";
        _Managers = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
        _Managers.variableId(4);
        _Data = newDynamicBean_Data();
    }

    @SuppressWarnings("deprecation")
    public BDepartmentTreeNode(long _ParentDepartment_, String _Name_) {
        _ParentDepartment = _ParentDepartment_;
        _Childs = new Zeze.Transaction.Collections.PMap1<>(String.class, Long.class);
        _Childs.variableId(2);
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
        _Managers = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
        _Managers.variableId(4);
        _Data = newDynamicBean_Data();
    }

    @Override
    public void reset() {
        setParentDepartment(0);
        _Childs.clear();
        setName("");
        _Managers.clear();
        _Data.reset();
        _unknown_ = null;
    }

    public void assign(BDepartmentTreeNode other) {
        setParentDepartment(other.getParentDepartment());
        _Childs.assign(other._Childs);
        setName(other.getName());
        _Managers.clear();
        for (var e : other._Managers.entrySet())
            _Managers.put(e.getKey(), e.getValue().copy());
        _Data.assign(other._Data);
        _unknown_ = other._unknown_;
    }

    public BDepartmentTreeNode copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDepartmentTreeNode copy() {
        var copy = new BDepartmentTreeNode();
        copy.assign(this);
        return copy;
    }

    public static void swap(BDepartmentTreeNode a, BDepartmentTreeNode b) {
        BDepartmentTreeNode save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ParentDepartment extends Zeze.Transaction.Logs.LogLong {
        public Log__ParentDepartment(BDepartmentTreeNode bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDepartmentTreeNode)getBelong())._ParentDepartment = value; }
    }

    private static final class Log__Name extends Zeze.Transaction.Logs.LogString {
        public Log__Name(BDepartmentTreeNode bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDepartmentTreeNode)getBelong())._Name = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNode: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ParentDepartment=").append(getParentDepartment()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Childs={");
        if (!_Childs.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Childs.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_kv_.getValue()).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Name=").append(getName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Managers={");
        if (!_Managers.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Managers.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().getBean().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Data=").append(System.lineSeparator());
        _Data.getBean().buildString(sb, level + 4);
        sb.append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
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
            var _x_ = _Childs;
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
            var _x_ = _Childs;
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
        if (!_Childs.equals(_b_._Childs))
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Childs.initRootInfo(root, this);
        _Managers.initRootInfo(root, this);
        _Data.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Childs.initRootInfoWithRedo(root, this);
        _Managers.initRootInfoWithRedo(root, this);
        _Data.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getParentDepartment() < 0)
            return true;
        for (var _v_ : _Childs.values()) {
            if (_v_ < 0)
                return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _ParentDepartment = vlog.longValue(); break;
                case 2: _Childs.followerApply(vlog); break;
                case 3: _Name = vlog.stringValue(); break;
                case 4: _Managers.followerApply(vlog); break;
                case 5: _Data.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setParentDepartment(rs.getLong(_parents_name_ + "ParentDepartment"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Childs", _Childs, rs.getString(_parents_name_ + "Childs"));
        setName(rs.getString(_parents_name_ + "Name"));
        if (getName() == null)
            setName("");
        Zeze.Serialize.Helper.decodeJsonMap(this, "Managers", _Managers, rs.getString(_parents_name_ + "Managers"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_Data, rs.getString(_parents_name_ + "Data"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "ParentDepartment", getParentDepartment());
        st.appendString(_parents_name_ + "Childs", Zeze.Serialize.Helper.encodeJson(_Childs));
        st.appendString(_parents_name_ + "Name", getName());
        st.appendString(_parents_name_ + "Managers", Zeze.Serialize.Helper.encodeJson(_Managers));
        st.appendString(_parents_name_ + "Data", Zeze.Serialize.Helper.encodeJson(_Data));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ParentDepartment", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Childs", "map", "string", "long"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Name", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Managers", "map", "string", "dynamic"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Data", "dynamic", "", ""));
        return vars;
    }
}
