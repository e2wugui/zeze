// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

// TODO: 允许广播
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskCondition extends Zeze.Transaction.Bean implements BTaskConditionReadOnly {
    public static final long TYPEID = 4477615436284693494L;

    private int _conditionType; // Condition的完成类型
    private long _conditionId; // Condition的Id
    private final Zeze.Transaction.DynamicBean _extendedData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_ExtendedData() {
        return new Zeze.Transaction.DynamicBean(3, Zeze.Game.TaskConditionBase::getSpecialTypeIdFromBean, Zeze.Game.TaskConditionBase::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_3(Zeze.Transaction.Bean bean) {
        return Zeze.Game.TaskConditionBase.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_3(long typeId) {
        return Zeze.Game.TaskConditionBase.createBeanFromSpecialTypeId(typeId);
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
    public int getConditionType() {
        if (!isManaged())
            return _conditionType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _conditionType;
        var log = (Log__conditionType)txn.getLog(objectId() + 1);
        return log != null ? log.value : _conditionType;
    }

    public void setConditionType(int value) {
        if (!isManaged()) {
            _conditionType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__conditionType(this, 1, value));
    }

    @Override
    public long getConditionId() {
        if (!isManaged())
            return _conditionId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _conditionId;
        var log = (Log__conditionId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _conditionId;
    }

    public void setConditionId(long value) {
        if (!isManaged()) {
            _conditionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__conditionId(this, 2, value));
    }

    public Zeze.Transaction.DynamicBean getExtendedData() {
        return _extendedData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getExtendedDataReadOnly() {
        return _extendedData;
    }

    @SuppressWarnings("deprecation")
    public BTaskCondition() {
        _extendedData = newDynamicBean_ExtendedData();
    }

    @SuppressWarnings("deprecation")
    public BTaskCondition(int _conditionType_, long _conditionId_) {
        _conditionType = _conditionType_;
        _conditionId = _conditionId_;
        _extendedData = newDynamicBean_ExtendedData();
    }

    public void assign(BTaskCondition other) {
        setConditionType(other.getConditionType());
        setConditionId(other.getConditionId());
        _extendedData.assign(other._extendedData);
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

    private static final class Log__conditionType extends Zeze.Transaction.Logs.LogInt {
        public Log__conditionType(BTaskCondition bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskCondition)getBelong())._conditionType = value; }
    }

    private static final class Log__conditionId extends Zeze.Transaction.Logs.LogLong {
        public Log__conditionId(BTaskCondition bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskCondition)getBelong())._conditionId = value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("conditionType=").append(getConditionType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("conditionId=").append(getConditionId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("extendedData=").append(System.lineSeparator());
        _extendedData.getBean().buildString(sb, level + 4);
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
            int _x_ = getConditionType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getConditionId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _extendedData;
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
            setConditionType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setConditionId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadDynamic(_extendedData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _extendedData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _extendedData.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getConditionType() < 0)
            return true;
        if (getConditionId() < 0)
            return true;
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
                case 1: _conditionType = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _conditionId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _extendedData.followerApply(vlog); break;
            }
        }
    }
}
