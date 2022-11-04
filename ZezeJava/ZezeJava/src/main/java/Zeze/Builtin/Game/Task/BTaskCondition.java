// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskCondition extends Zeze.Transaction.Bean implements BTaskConditionReadOnly {
    public static final long TYPEID = 3233174055754866965L;

    private String _ConditionId;
    private String _ConditionName;
    private final Zeze.Transaction.DynamicBean _ConditionCustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_ConditionCustomData() {
        return new Zeze.Transaction.DynamicBean(3, Zeze.Game.Condition::getSpecialTypeIdFromBean, Zeze.Game.Condition::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_ConditionCustomData(Zeze.Transaction.Bean bean) {
        return Zeze.Game.Condition.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_ConditionCustomData(long typeId) {
        return Zeze.Game.Condition.createBeanFromSpecialTypeId(typeId);
    }

    @Override
    public String getConditionId() {
        if (!isManaged())
            return _ConditionId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ConditionId;
        var log = (Log__ConditionId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ConditionId;
    }

    public void setConditionId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ConditionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ConditionId(this, 1, value));
    }

    @Override
    public String getConditionName() {
        if (!isManaged())
            return _ConditionName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ConditionName;
        var log = (Log__ConditionName)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ConditionName;
    }

    public void setConditionName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ConditionName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ConditionName(this, 2, value));
    }

    public Zeze.Transaction.DynamicBean getConditionCustomData() {
        return _ConditionCustomData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getConditionCustomDataReadOnly() {
        return _ConditionCustomData;
    }

    @SuppressWarnings("deprecation")
    public BTaskCondition() {
        _ConditionId = "";
        _ConditionName = "";
        _ConditionCustomData = newDynamicBean_ConditionCustomData();
    }

    @SuppressWarnings("deprecation")
    public BTaskCondition(String _ConditionId_, String _ConditionName_) {
        if (_ConditionId_ == null)
            throw new IllegalArgumentException();
        _ConditionId = _ConditionId_;
        if (_ConditionName_ == null)
            throw new IllegalArgumentException();
        _ConditionName = _ConditionName_;
        _ConditionCustomData = newDynamicBean_ConditionCustomData();
    }

    public void assign(BTaskCondition other) {
        setConditionId(other.getConditionId());
        setConditionName(other.getConditionName());
        _ConditionCustomData.assign(other._ConditionCustomData);
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

    private static final class Log__ConditionId extends Zeze.Transaction.Logs.LogString {
        public Log__ConditionId(BTaskCondition bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskCondition)getBelong())._ConditionId = value; }
    }

    private static final class Log__ConditionName extends Zeze.Transaction.Logs.LogString {
        public Log__ConditionName(BTaskCondition bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskCondition)getBelong())._ConditionName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Task.BTaskCondition: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ConditionId").append('=').append(getConditionId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ConditionName").append('=').append(getConditionName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ConditionCustomData").append('=').append(System.lineSeparator());
        _ConditionCustomData.getBean().buildString(sb, level + 4);
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
            String _x_ = getConditionId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getConditionName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _ConditionCustomData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DYNAMIC);
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
            setConditionId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setConditionName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadDynamic(_ConditionCustomData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _ConditionCustomData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _ConditionCustomData.resetRootInfo();
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
                case 1: _ConditionId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _ConditionName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _ConditionCustomData.followerApply(vlog); break;
            }
        }
    }
}
