// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

/*
					Task rpc
					所有的TaskEvent均由这个rpc驱动（仿照现serverdev的结构）
					这个rpc的参数是BTaskEvent，内部的DynamicData是各个不同的任务的不同Bean数据
*/
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskEvent extends Zeze.Transaction.Bean implements BTaskEventReadOnly {
    public static final long TYPEID = -5362050394114558969L;

    private long _roleId;
    private final Zeze.Transaction.DynamicBean _taskEventTypeDynamic;
    public static final long DynamicTypeId_TaskEventTypeDynamic_Zeze_Builtin_Game_TaskBase_BSpecificTaskEvent = 2442000638159095225L;
    public static final long DynamicTypeId_TaskEventTypeDynamic_Zeze_Builtin_Game_TaskBase_BBroadcastTaskEvent = 2627115510834301728L;

    public static Zeze.Transaction.DynamicBean newDynamicBean_TaskEventTypeDynamic() {
        return new Zeze.Transaction.DynamicBean(2, BTaskEvent::getSpecialTypeIdFromBean_2, BTaskEvent::createBeanFromSpecialTypeId_2);
    }

    public static long getSpecialTypeIdFromBean_2(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.typeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_typeId_ == 2442000638159095225L)
            return 2442000638159095225L; // Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent
        if (_typeId_ == 2627115510834301728L)
            return 2627115510834301728L; // Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent
        throw new RuntimeException("Unknown Bean! dynamic@Zeze.Builtin.Game.TaskBase.BTaskEvent:taskEventTypeDynamic");
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_2(long typeId) {
        if (typeId == 2442000638159095225L)
            return new Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent();
        if (typeId == 2627115510834301728L)
            return new Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent();
        return null;
    }

    private final Zeze.Transaction.DynamicBean _extendedData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_ExtendedData() {
        return new Zeze.Transaction.DynamicBean(3, Zeze.Game.TaskBase::getSpecialTypeIdFromBean, Zeze.Game.TaskBase::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_3(Zeze.Transaction.Bean bean) {
        return Zeze.Game.TaskBase.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_3(long typeId) {
        return Zeze.Game.TaskBase.createBeanFromSpecialTypeId(typeId);
    }

    @Override
    public long getRoleId() {
        if (!isManaged())
            return _roleId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _roleId;
        var log = (Log__roleId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _roleId;
    }

    public void setRoleId(long value) {
        if (!isManaged()) {
            _roleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__roleId(this, 1, value));
    }

    public Zeze.Transaction.DynamicBean getTaskEventTypeDynamic() {
        return _taskEventTypeDynamic;
    }

    public Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent getTaskEventTypeDynamic_Zeze_Builtin_Game_TaskBase_BSpecificTaskEvent() {
        return (Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent)_taskEventTypeDynamic.getBean();
    }

    public void setTaskEventTypeDynamic(Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent value) {
        _taskEventTypeDynamic.setBean(value);
    }

    public Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent getTaskEventTypeDynamic_Zeze_Builtin_Game_TaskBase_BBroadcastTaskEvent() {
        return (Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent)_taskEventTypeDynamic.getBean();
    }

    public void setTaskEventTypeDynamic(Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent value) {
        _taskEventTypeDynamic.setBean(value);
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getTaskEventTypeDynamicReadOnly() {
        return _taskEventTypeDynamic;
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BSpecificTaskEventReadOnly getTaskEventTypeDynamic_Zeze_Builtin_Game_TaskBase_BSpecificTaskEventReadOnly() {
        return (Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent)_taskEventTypeDynamic.getBean();
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BBroadcastTaskEventReadOnly getTaskEventTypeDynamic_Zeze_Builtin_Game_TaskBase_BBroadcastTaskEventReadOnly() {
        return (Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent)_taskEventTypeDynamic.getBean();
    }

    public Zeze.Transaction.DynamicBean getExtendedData() {
        return _extendedData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getExtendedDataReadOnly() {
        return _extendedData;
    }

    @SuppressWarnings("deprecation")
    public BTaskEvent() {
        _taskEventTypeDynamic = newDynamicBean_TaskEventTypeDynamic();
        _extendedData = newDynamicBean_ExtendedData();
    }

    @SuppressWarnings("deprecation")
    public BTaskEvent(long _roleId_) {
        _roleId = _roleId_;
        _taskEventTypeDynamic = newDynamicBean_TaskEventTypeDynamic();
        _extendedData = newDynamicBean_ExtendedData();
    }

    public void assign(BTaskEvent other) {
        setRoleId(other.getRoleId());
        _taskEventTypeDynamic.assign(other._taskEventTypeDynamic);
        _extendedData.assign(other._extendedData);
    }

    @Deprecated
    public void Assign(BTaskEvent other) {
        assign(other);
    }

    public BTaskEvent copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTaskEvent copy() {
        var copy = new BTaskEvent();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BTaskEvent Copy() {
        return copy();
    }

    public static void swap(BTaskEvent a, BTaskEvent b) {
        BTaskEvent save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__roleId extends Zeze.Transaction.Logs.LogLong {
        public Log__roleId(BTaskEvent bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskEvent)getBelong())._roleId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BTaskEvent: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("roleId=").append(getRoleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("taskEventTypeDynamic=").append(System.lineSeparator());
        _taskEventTypeDynamic.getBean().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
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
            long _x_ = getRoleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _taskEventTypeDynamic;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
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
            setRoleId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadDynamic(_taskEventTypeDynamic, _t_);
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
        _taskEventTypeDynamic.initRootInfo(root, this);
        _extendedData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _taskEventTypeDynamic.resetRootInfo();
        _extendedData.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getRoleId() < 0)
            return true;
        if (_taskEventTypeDynamic.negativeCheck())
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
                case 1: _roleId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _taskEventTypeDynamic.followerApply(vlog); break;
                case 3: _extendedData.followerApply(vlog); break;
            }
        }
    }
}
