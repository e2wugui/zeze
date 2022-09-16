// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BDepartmentTreeNode extends Zeze.Transaction.Bean {
    private long _ParentDepartment; // 0表示第一级部门
    private final Zeze.Transaction.Collections.PMap1<String, Long> _Childs; // name 2 id。采用整体保存，因为需要排序和重名判断。需要加数量上限。
    private String _Name;
    private final Zeze.Transaction.Collections.PMap1<String, Zeze.Transaction.DynamicBean> _Managers;

    public static long getSpecialTypeIdFromBean_Managers(Zeze.Transaction.Bean bean) {
        return Zeze.Collections.DepartmentTree.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_Managers(long typeId) {
        return Zeze.Collections.DepartmentTree.createBeanFromSpecialTypeId(typeId);
    }

    private final Zeze.Transaction.DynamicBean _Data;

    public static long getSpecialTypeIdFromBean_Data(Zeze.Transaction.Bean bean) {
        return Zeze.Collections.DepartmentTree.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_Data(long typeId) {
        return Zeze.Collections.DepartmentTree.createBeanFromSpecialTypeId(typeId);
    }

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

    public Zeze.Transaction.Collections.PMap1<String, Zeze.Transaction.DynamicBean> getManagers() {
        return _Managers;
    }

    public Zeze.Transaction.DynamicBean getData() {
        return _Data;
    }

    @SuppressWarnings("deprecation")
    public BDepartmentTreeNode() {
        _Childs = new Zeze.Transaction.Collections.PMap1<>(String.class, Long.class);
        _Childs.variableId(2);
        _Name = "";
        _Managers = new Zeze.Transaction.Collections.PMap1<>(String.class, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
        _Managers.variableId(4);
        _Data = new Zeze.Transaction.DynamicBean(5, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
    }

    @SuppressWarnings("deprecation")
    public BDepartmentTreeNode(long _ParentDepartment_, String _Name_) {
        _ParentDepartment = _ParentDepartment_;
        _Childs = new Zeze.Transaction.Collections.PMap1<>(String.class, Long.class);
        _Childs.variableId(2);
        if (_Name_ == null)
            throw new IllegalArgumentException();
        _Name = _Name_;
        _Managers = new Zeze.Transaction.Collections.PMap1<>(String.class, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
        _Managers.variableId(4);
        _Data = new Zeze.Transaction.DynamicBean(5, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
    }

    public void assign(BDepartmentTreeNode other) {
        setParentDepartment(other.getParentDepartment());
        getChilds().clear();
        for (var e : other.getChilds().entrySet())
            getChilds().put(e.getKey(), e.getValue());
        setName(other.getName());
        getManagers().clear();
        for (var e : other.getManagers().entrySet())
            getManagers().put(e.getKey(), e.getValue());
        getData().assign(other.getData());
    }

    @Deprecated
    public void Assign(BDepartmentTreeNode other) {
        assign(other);
    }

    public BDepartmentTreeNode copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BDepartmentTreeNode copy() {
        var copy = new BDepartmentTreeNode();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BDepartmentTreeNode Copy() {
        return copy();
    }

    public static void swap(BDepartmentTreeNode a, BDepartmentTreeNode b) {
        BDepartmentTreeNode save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public BDepartmentTreeNode copyBean() {
        return Copy();
    }

    public static final long TYPEID = 2712461973987809351L;

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
        sb.append(Zeze.Util.Str.indent(level)).append("ParentDepartment").append('=').append(getParentDepartment()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Childs").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getChilds().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(_kv_.getValue()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Name").append('=').append(getName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Managers").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getManagers().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().getBean().buildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Data").append('=').append(System.lineSeparator());
        getData().getBean().buildString(sb, level + 4);
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            long _x_ = getParentDepartment();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getChilds();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
                }
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
            var _x_ = getManagers();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.DYNAMIC);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _x_.encode(_o_);
                }
            }
        }
        {
            var _x_ = getData();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setParentDepartment(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = getChilds();
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
            var _x_ = getManagers();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    Zeze.Transaction.DynamicBean _v_ = new Zeze.Transaction.DynamicBean(0, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
                    _o_.ReadDynamic(_v_, _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _o_.ReadDynamic(getData(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Childs.initRootInfo(root, this);
        _Managers.initRootInfo(root, this);
        _Data.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _Childs.resetRootInfo();
        _Managers.resetRootInfo();
        _Data.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getParentDepartment() < 0)
            return true;
        for (var _v_ : getChilds().values()) {
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
                case 1: _ParentDepartment = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _Childs.followerApply(vlog); break;
                case 3: _Name = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _Managers.followerApply(vlog); break;
                case 5: _Data.followerApply(vlog); break;
            }
        }
    }
}
