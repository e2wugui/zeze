// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BDepartmentRoot extends Zeze.Transaction.Bean implements BDepartmentRootReadOnly {
    public static final long TYPEID = 50884757418508709L;

    private String _Root; // 群主
    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Transaction.DynamicBean> _Managers;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Managers() {
        return new Zeze.Transaction.DynamicBean(2, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_2(Zeze.Transaction.Bean bean) {
        return Zeze.Collections.DepartmentTree.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_2(long typeId) {
        return Zeze.Collections.DepartmentTree.createBeanFromSpecialTypeId(typeId);
    }

    private long _NextDepartmentId; // 部门Id种子
    private final Zeze.Transaction.Collections.PMap1<String, Long> _Childs; // name 2 id。采用整体保存，因为需要排序和重名判断。需要加数量上限。
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
    public String getRoot() {
        if (!isManaged())
            return _Root;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Root;
        var log = (Log__Root)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Root;
    }

    public void setRoot(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Root = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Root(this, 1, value));
    }

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Transaction.DynamicBean> getManagers() {
        return _Managers;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getManagersReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Managers);
    }

    @Override
    public long getNextDepartmentId() {
        if (!isManaged())
            return _NextDepartmentId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NextDepartmentId;
        var log = (Log__NextDepartmentId)txn.getLog(objectId() + 3);
        return log != null ? log.value : _NextDepartmentId;
    }

    public void setNextDepartmentId(long value) {
        if (!isManaged()) {
            _NextDepartmentId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NextDepartmentId(this, 3, value));
    }

    public Zeze.Transaction.Collections.PMap1<String, Long> getChilds() {
        return _Childs;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<String, Long> getChildsReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_Childs);
    }

    public Zeze.Transaction.DynamicBean getData() {
        return _Data;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getDataReadOnly() {
        return _Data;
    }

    @SuppressWarnings("deprecation")
    public BDepartmentRoot() {
        _Root = "";
        _Managers = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
        _Managers.variableId(2);
        _Childs = new Zeze.Transaction.Collections.PMap1<>(String.class, Long.class);
        _Childs.variableId(4);
        _Data = newDynamicBean_Data();
    }

    @SuppressWarnings("deprecation")
    public BDepartmentRoot(String _Root_, long _NextDepartmentId_) {
        if (_Root_ == null)
            throw new IllegalArgumentException();
        _Root = _Root_;
        _Managers = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
        _Managers.variableId(2);
        _NextDepartmentId = _NextDepartmentId_;
        _Childs = new Zeze.Transaction.Collections.PMap1<>(String.class, Long.class);
        _Childs.variableId(4);
        _Data = newDynamicBean_Data();
    }

    public void assign(BDepartmentRoot other) {
        setRoot(other.getRoot());
        _Managers.clear();
        for (var e : other._Managers.entrySet())
            _Managers.put(e.getKey(), e.getValue().copy());
        setNextDepartmentId(other.getNextDepartmentId());
        _Childs.clear();
        _Childs.putAll(other._Childs);
        _Data.assign(other._Data);
    }

    @Deprecated
    public void Assign(BDepartmentRoot other) {
        assign(other);
    }

    public BDepartmentRoot copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDepartmentRoot copy() {
        var copy = new BDepartmentRoot();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BDepartmentRoot Copy() {
        return copy();
    }

    public static void swap(BDepartmentRoot a, BDepartmentRoot b) {
        BDepartmentRoot save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Root extends Zeze.Transaction.Logs.LogString {
        public Log__Root(BDepartmentRoot bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDepartmentRoot)getBelong())._Root = value; }
    }

    private static final class Log__NextDepartmentId extends Zeze.Transaction.Logs.LogLong {
        public Log__NextDepartmentId(BDepartmentRoot bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDepartmentRoot)getBelong())._NextDepartmentId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.DepartmentTree.BDepartmentRoot: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Root").append('=').append(getRoot()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Managers").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : _Managers.entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().getBean().buildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NextDepartmentId").append('=').append(getNextDepartmentId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Childs").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : _Childs.entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(_kv_.getValue()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Data").append('=').append(System.lineSeparator());
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            String _x_ = getRoot();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _Managers;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.DYNAMIC);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().encode(_o_);
                }
            }
        }
        {
            long _x_ = getNextDepartmentId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Childs;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
                }
            }
        }
        {
            var _x_ = _Data;
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
            setRoot(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
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
        if (_i_ == 3) {
            setNextDepartmentId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
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
        if (_i_ == 5) {
            _o_.ReadDynamic(_Data, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Managers.initRootInfo(root, this);
        _Childs.initRootInfo(root, this);
        _Data.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _Managers.resetRootInfo();
        _Childs.resetRootInfo();
        _Data.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getNextDepartmentId() < 0)
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
                case 1: _Root = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _Managers.followerApply(vlog); break;
                case 3: _NextDepartmentId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _Childs.followerApply(vlog); break;
                case 5: _Data.followerApply(vlog); break;
            }
        }
    }
}
