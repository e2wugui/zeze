// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

// Task rpc
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskEvent extends Zeze.Transaction.Bean implements BTaskEventReadOnly {
    public static final long TYPEID = -4434634468626847386L;

    private long _TaskId;
    private long _TaskPhaseId;
    private long _TaskConditionId;
    private final Zeze.Transaction.DynamicBean _DynamicData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_DynamicData() {
        return new Zeze.Transaction.DynamicBean(4, Zeze.Game.Task::getSpecialTypeIdFromBean, Zeze.Game.Task::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_DynamicData(Zeze.Transaction.Bean bean) {
        return Zeze.Game.Task.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_DynamicData(long typeId) {
        return Zeze.Game.Task.createBeanFromSpecialTypeId(typeId);
    }

    @Override
    public long getTaskId() {
        if (!isManaged())
            return _TaskId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskId;
        var log = (Log__TaskId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TaskId;
    }

    public void setTaskId(long value) {
        if (!isManaged()) {
            _TaskId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskId(this, 1, value));
    }

    @Override
    public long getTaskPhaseId() {
        if (!isManaged())
            return _TaskPhaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskPhaseId;
        var log = (Log__TaskPhaseId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _TaskPhaseId;
    }

    public void setTaskPhaseId(long value) {
        if (!isManaged()) {
            _TaskPhaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskPhaseId(this, 2, value));
    }

    @Override
    public long getTaskConditionId() {
        if (!isManaged())
            return _TaskConditionId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskConditionId;
        var log = (Log__TaskConditionId)txn.getLog(objectId() + 3);
        return log != null ? log.value : _TaskConditionId;
    }

    public void setTaskConditionId(long value) {
        if (!isManaged()) {
            _TaskConditionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskConditionId(this, 3, value));
    }

    public Zeze.Transaction.DynamicBean getDynamicData() {
        return _DynamicData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getDynamicDataReadOnly() {
        return _DynamicData;
    }

    @SuppressWarnings("deprecation")
    public BTaskEvent() {
        _DynamicData = newDynamicBean_DynamicData();
    }

    @SuppressWarnings("deprecation")
    public BTaskEvent(long _TaskId_, long _TaskPhaseId_, long _TaskConditionId_) {
        _TaskId = _TaskId_;
        _TaskPhaseId = _TaskPhaseId_;
        _TaskConditionId = _TaskConditionId_;
        _DynamicData = newDynamicBean_DynamicData();
    }

    public void assign(BTaskEvent other) {
        setTaskId(other.getTaskId());
        setTaskPhaseId(other.getTaskPhaseId());
        setTaskConditionId(other.getTaskConditionId());
        _DynamicData.assign(other._DynamicData);
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

    private static final class Log__TaskId extends Zeze.Transaction.Logs.LogLong {
        public Log__TaskId(BTaskEvent bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskEvent)getBelong())._TaskId = value; }
    }

    private static final class Log__TaskPhaseId extends Zeze.Transaction.Logs.LogLong {
        public Log__TaskPhaseId(BTaskEvent bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskEvent)getBelong())._TaskPhaseId = value; }
    }

    private static final class Log__TaskConditionId extends Zeze.Transaction.Logs.LogLong {
        public Log__TaskConditionId(BTaskEvent bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskEvent)getBelong())._TaskConditionId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Task.BTaskEvent: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TaskId").append('=').append(getTaskId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskPhaseId").append('=').append(getTaskPhaseId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskConditionId").append('=').append(getTaskConditionId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("DynamicData").append('=').append(System.lineSeparator());
        _DynamicData.getBean().buildString(sb, level + 4);
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
            long _x_ = getTaskId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getTaskPhaseId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getTaskConditionId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _DynamicData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.DYNAMIC);
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
            setTaskId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTaskPhaseId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setTaskConditionId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadDynamic(_DynamicData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _DynamicData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _DynamicData.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getTaskId() < 0)
            return true;
        if (getTaskPhaseId() < 0)
            return true;
        if (getTaskConditionId() < 0)
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
                case 1: _TaskId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _TaskPhaseId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _TaskConditionId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _DynamicData.followerApply(vlog); break;
            }
        }
    }
}
