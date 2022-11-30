// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

// TaskCondition的Bean数据，只存在在BTaskPhase之内
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskCondition extends Zeze.Transaction.Bean implements BTaskConditionReadOnly {
    public static final long TYPEID = 4477615436284693494L;

    private String _TaskConditionName; // Condition的名字（这个名字主要需要在下面这个dynamic中动态指定）
    private final Zeze.Transaction.DynamicBean _TaskConditionCustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_TaskConditionCustomData() {
        return new Zeze.Transaction.DynamicBean(2, Zeze.Game.TaskBase::getSpecialTypeIdFromBean, Zeze.Game.TaskBase::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_2(Zeze.Transaction.Bean bean) {
        return Zeze.Game.TaskBase.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_2(long typeId) {
        return Zeze.Game.TaskBase.createBeanFromSpecialTypeId(typeId);
    }

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    @Override
    public String getTaskConditionName() {
        if (!isManaged())
            return _TaskConditionName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskConditionName;
        var log = (Log__TaskConditionName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TaskConditionName;
    }

    public void setTaskConditionName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TaskConditionName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskConditionName(this, 1, value));
    }

    public Zeze.Transaction.DynamicBean getTaskConditionCustomData() {
        return _TaskConditionCustomData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getTaskConditionCustomDataReadOnly() {
        return _TaskConditionCustomData;
    }

    @SuppressWarnings("deprecation")
    public BTaskCondition() {
        _TaskConditionName = "";
        _TaskConditionCustomData = newDynamicBean_TaskConditionCustomData();
    }

    @SuppressWarnings("deprecation")
    public BTaskCondition(String _TaskConditionName_) {
        if (_TaskConditionName_ == null)
            throw new IllegalArgumentException();
        _TaskConditionName = _TaskConditionName_;
        _TaskConditionCustomData = newDynamicBean_TaskConditionCustomData();
    }

    public void assign(BTaskCondition other) {
        setTaskConditionName(other.getTaskConditionName());
        _TaskConditionCustomData.assign(other._TaskConditionCustomData);
    }

    @Deprecated
    public void Assign(BTaskCondition other) {
        assign(other);
    }

    public BTaskCondition copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTaskCondition copy() {
        var copy = new BTaskCondition();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BTaskCondition Copy() {
        return copy();
    }

    public static void swap(BTaskCondition a, BTaskCondition b) {
        BTaskCondition save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TaskConditionName extends Zeze.Transaction.Logs.LogString {
        public Log__TaskConditionName(BTaskCondition bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskCondition)getBelong())._TaskConditionName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BTaskCondition: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TaskConditionName=").append(getTaskConditionName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskConditionCustomData=").append(System.lineSeparator());
        _TaskConditionCustomData.getBean().buildString(sb, level + 4);
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
            String _x_ = getTaskConditionName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _TaskConditionCustomData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.DYNAMIC);
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
            setTaskConditionName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadDynamic(_TaskConditionCustomData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _TaskConditionCustomData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _TaskConditionCustomData.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
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
                case 1: _TaskConditionName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _TaskConditionCustomData.followerApply(vlog); break;
            }
        }
    }
}
